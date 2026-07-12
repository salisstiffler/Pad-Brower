package landau.sweb;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * TouchZoneDispatcher — the central event dispatcher that implements the full
 * three-module architecture described in the requirements.
 *
 * Architecture:
 *   Module 1 (A-Zone): Standard touch events from the large area. Forwarded
 *                       transparently to WebView, with Module 3 override.
 *   Module 2 (B-Zone): Handled entirely by PinchGestureManager.
 *   Module 3 (Coordinator): This class coordinates A/B zone priority.
 *
 * Lifecycle of the B-zone:
 *   1. Edge detection strip (left/right, 5-7mm) watches for long-press > 500ms.
 *   2. On activation, B-zone circular area appears at touch point.
 *   3. Pointer held in B-zone drives scroll via Module 2.
 *   4. When B-zone pointer lifts, dismiss timer starts (1.5s default).
 *   5. A-zone touch (Module 1) has HIGHEST priority: pauses B-zone scroll,
 *      then resumes when A-zone pointer lifts.
 *   6. B-zone dismissal resets everything to bypass mode.
 *
 * Thread safety: All operations must be called from the UI thread.
 */
public class TouchZoneDispatcher {

    private static final String TAG = "TouchZoneDispatcher";

    // ---- B-Zone State ----
    private enum BZoneState {
        INACTIVE,           // B-zone does not exist; bypass mode
        PENDING,            // Pointer is in edge strip, waiting for long-press timeout
        ACTIVE,             // B-zone is active, tracking B-zone pointer
        DISMISSING          // B-zone pointer lifted, dismiss timer running
    }

    private BZoneState bZoneState = BZoneState.INACTIVE;

    // Pointer tracking
    private int bZonePointerId = -1;         // pointer ID locked to B-zone
    private float bZoneCenterX, bZoneCenterY; // B-zone circle center on screen
    private float bZoneActivationY;          // Y where pointer initially landed

    // A-zone active pointer tracking (Module 1 priority for Module 3)
    private final SparseArray<float[]> aZonePointers = new SparseArray<>(); // id -> [x, y]
    private boolean aZoneHasActivePointer = false;

    // Thresholds (px)
    private float edgeStripWidthPx;
    private float bZoneRadiusPx;

    // Timers
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingActivateRunnable;
    private Runnable pendingDismissRunnable;

    // Dependencies
    private final Context context;
    private WebView webView;
    private final PinchGestureManager gestureManager;
    private final PinchOverlayView overlayView;

    public TouchZoneDispatcher(Context context,
                               PinchGestureManager gestureManager,
                               PinchOverlayView overlayView) {
        this.context = context;
        this.gestureManager = gestureManager;
        this.overlayView = overlayView;
        computeThresholds();
    }

    private void computeThresholds() {
        edgeStripWidthPx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.EDGE_STRIP_MM);
        bZoneRadiusPx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.B_ZONE_RADIUS_MM);
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
        gestureManager.setWebView(webView);
    }

    // ========================================================================
    // Main entry point: called from the WebView container's dispatchTouchEvent
    // Returns true if the event was FULLY consumed (do not pass to WebView).
    // Returns false if the event should be passed through to WebView normally.
    // ========================================================================
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int actionIndex = event.getActionIndex();
        int pointerId = event.getPointerId(actionIndex);

        boolean consumedInternal = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                consumedInternal = handlePointerDown(event, actionIndex, pointerId);
                break;

            case MotionEvent.ACTION_MOVE:
                consumedInternal = handleMove(event);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                consumedInternal = handlePointerUp(event, actionIndex, pointerId);
                break;

            case MotionEvent.ACTION_CANCEL:
                handleCancel();
                consumedInternal = false;
                break;

            default:
                consumedInternal = false;
                break;
        }

        // Module 3 coordination & splitting logic
        if (isBZoneActive()) {
            // Check if there are active A-zone pointers, or if we are currently releasing an A-zone pointer
            boolean isReleasingAZone = (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP)
                    && (pointerId != bZonePointerId);

            if (aZonePointers.size() > 0 || isReleasingAZone) {
                forwardAZoneEventsToWebView(event, action, actionIndex, pointerId, isReleasingAZone);
            }
            // Always return true to consume the original event at the container level
            return true;
        }

        return consumedInternal;
    }

    /**
     * Manually forward A-zone pointer events to the WebView as simple single-pointer events,
     * avoiding the need for MotionEvent.split() which is only publicly available from API 34+.
     *
     * Strategy:
     *  - On MOVE: dispatch ACTION_MOVE for the primary A-zone pointer, with its current coordinates.
     *  - On POINTER_DOWN: if it is an A-zone new pointer, dispatch ACTION_DOWN to WebView.
     *  - On UP/POINTER_UP: if it is an A-zone pointer being released, dispatch ACTION_UP to WebView.
     *
     * This produces clean single-pointer events that prevent spurious pinch-zoom in WebView.
     */
    private void forwardAZoneEventsToWebView(MotionEvent event, int action, int actionIndex,
                                              int releasedPointerId, boolean isReleasingAZone) {
        if (webView == null) return;

        long downTime = event.getDownTime();
        long eventTime = event.getEventTime();
        int metaState = event.getMetaState();

        if (action == MotionEvent.ACTION_MOVE) {
            // For MOVE, forward each active A-zone pointer as its own ACTION_MOVE.
            // In practice, dispatch only the primary (first) A-zone pointer to avoid confusion.
            if (aZonePointers.size() > 0) {
                float[] pos = aZonePointers.valueAt(0);
                MotionEvent synth = MotionEvent.obtain(
                        downTime, eventTime, MotionEvent.ACTION_MOVE,
                        pos[0], pos[1], metaState);
                webView.dispatchTouchEvent(synth);
                synth.recycle();
            }
        } else if (action == MotionEvent.ACTION_POINTER_DOWN && releasedPointerId != bZonePointerId) {
            // New A-zone pointer down — send ACTION_DOWN to WebView
            float x = event.getX(actionIndex);
            float y = event.getY(actionIndex);
            MotionEvent synth = MotionEvent.obtain(
                    eventTime, eventTime, MotionEvent.ACTION_DOWN,
                    x, y, metaState);
            webView.dispatchTouchEvent(synth);
            synth.recycle();
        } else if (isReleasingAZone) {
            // A-zone pointer lifting — find the current position and send ACTION_UP
            float[] pos = aZonePointers.get(releasedPointerId);
            if (pos == null) {
                // Fallback: use the event's reported coordinates for the lifting pointer
                pos = new float[]{event.getX(actionIndex), event.getY(actionIndex)};
            }
            MotionEvent synth = MotionEvent.obtain(
                    downTime, eventTime, MotionEvent.ACTION_UP,
                    pos[0], pos[1], metaState);
            webView.dispatchTouchEvent(synth);
            synth.recycle();
        }
    }



    // ---- Handler: POINTER DOWN ----

    private boolean handlePointerDown(MotionEvent event, int actionIndex, int pointerId) {
        float x = event.getX(actionIndex);
        float y = event.getY(actionIndex);

        // Is this pointer in the B-zone area (when active)?
        if (bZoneState == BZoneState.ACTIVE || bZoneState == BZoneState.DISMISSING) {
            if (isInBZone(x, y)) {
                // This is the B-zone pointer being re-pressed or another finger in B-zone
                handleBZonePointerDown(pointerId, x, y);
                return true; // consumed — do not pass to WebView
            }
        }

        // Is this pointer in the edge detection strip (when B-zone is inactive)?
        if (bZoneState == BZoneState.INACTIVE && isInEdgeStrip(x)) {
            // Start long-press timer to activate B-zone
            startPendingActivation(pointerId, x, y);
            return true; // consumed — block this event from WebView during detection
        }

        // If B-zone is already pending and the same pointer moves away, handled in MOVE

        // Module 1: A-zone pointer
        registerAZonePointer(pointerId, x, y);

        // Module 3: A-zone has priority — if B-zone continuous scroll is running, pause it
        if (bZoneState == BZoneState.ACTIVE) {
            onAZoneGestureStarted();
        }

        return false; // let WebView handle
    }

    // ---- Handler: MOVE ----

    private boolean handleMove(MotionEvent event) {
        boolean bZoneHandled = false;
        for (int i = 0; i < event.getPointerCount(); i++) {
            int pId = event.getPointerId(i);
            float x = event.getX(i);
            float y = event.getY(i);

            if (pId == bZonePointerId) {
                // This is the B-zone pointer
                switch (bZoneState) {
                    case PENDING:
                        // Check if finger has moved too far from start (cancel pending)
                        float dy = Math.abs(y - bZoneActivationY);
                        if (dy > PinchZoneConfig.mmToPx(context, 5f)) {
                            cancelPendingActivation();
                            // Let WebView know about this movement
                            registerAZonePointer(pId, x, y);
                        } else {
                            bZoneHandled = true;
                        }
                        break;

                    case ACTIVE:
                        gestureManager.onBZoneMove(y);
                        bZoneHandled = true;
                        break;

                    case DISMISSING:
                        // Finger re-entered during dismiss — reactivate
                        if (isInBZone(x, y)) {
                            cancelDismiss();
                            bZoneState = BZoneState.ACTIVE;
                            bZonePointerId = pId;
                            gestureManager.onBZoneDown(y);
                        }
                        bZoneHandled = true;
                        break;
                }
            } else {
                // A-zone pointer
                updateAZonePointer(pId, x, y);
            }
        }
        return bZoneHandled;
    }

    // ---- Handler: POINTER UP ----

    private boolean handlePointerUp(MotionEvent event, int actionIndex, int pointerId) {
        if (pointerId == bZonePointerId) {
            switch (bZoneState) {
                case PENDING:
                    cancelPendingActivation();
                    return false; // Let WebView see this as a normal tap

                case ACTIVE:
                    bZoneState = BZoneState.DISMISSING;
                    gestureManager.onBZoneUp();
                    scheduleDismiss();
                    return true;

                case DISMISSING:
                    // Pointer lifted while already dismissing — just let dismiss complete
                    return true;
            }
        }

        // A-zone pointer up
        removeAZonePointer(pointerId);
        boolean hadAZone = aZoneHasActivePointer;
        aZoneHasActivePointer = aZonePointers.size() > 0;

        // Module 3: A-zone ended — resume B-zone scroll if it was paused
        if (hadAZone && !aZoneHasActivePointer && bZoneState == BZoneState.ACTIVE) {
            onAZoneGestureEnded();
        }

        return false;
    }

    // ---- Handler: CANCEL ----

    private void handleCancel() {
        cancelPendingActivation();
        aZonePointers.clear();
        aZoneHasActivePointer = false;
    }

    // ========================================================================
    // B-Zone Lifecycle
    // ========================================================================

    private void startPendingActivation(int pointerId, float x, float y) {
        bZonePointerId = pointerId;
        bZoneActivationY = y;
        bZoneCenterX = x;
        bZoneCenterY = y;
        bZoneState = BZoneState.PENDING;
        Log.d(TAG, "B-Zone PENDING at (" + x + ", " + y + ")");

        pendingActivateRunnable = () -> activateBZone();
        handler.postDelayed(pendingActivateRunnable,
                PinchZoneConfig.B_ZONE_ACTIVATE_DELAY_MS);
    }

    private void activateBZone() {
        if (bZoneState != BZoneState.PENDING) return;
        bZoneState = BZoneState.ACTIVE;
        Log.d(TAG, "B-Zone ACTIVATED at (" + bZoneCenterX + ", " + bZoneCenterY + ")");

        // Show the visual
        overlayView.showBZone(bZoneCenterX, bZoneCenterY);
        // Notify gesture manager
        gestureManager.onBZoneDown(bZoneCenterY);
    }

    private void cancelPendingActivation() {
        if (pendingActivateRunnable != null) {
            handler.removeCallbacks(pendingActivateRunnable);
            pendingActivateRunnable = null;
        }
        bZonePointerId = -1;
        bZoneState = BZoneState.INACTIVE;
    }

    private void scheduleDismiss() {
        cancelDismiss();
        pendingDismissRunnable = this::dismissBZone;
        handler.postDelayed(pendingDismissRunnable,
                PinchZoneConfig.B_ZONE_DISMISS_DELAY_MS);
    }

    private void cancelDismiss() {
        if (pendingDismissRunnable != null) {
            handler.removeCallbacks(pendingDismissRunnable);
            pendingDismissRunnable = null;
        }
    }

    private void dismissBZone() {
        Log.d(TAG, "B-Zone DISMISSED");
        bZoneState = BZoneState.INACTIVE;
        bZonePointerId = -1;
        gestureManager.stopGesture();
        overlayView.hideBZone();
    }

    private void handleBZonePointerDown(int pointerId, float x, float y) {
        cancelDismiss();
        bZoneState = BZoneState.ACTIVE;
        bZonePointerId = pointerId;
        gestureManager.onBZoneDown(y);
        overlayView.showBZone(bZoneCenterX, bZoneCenterY);
        Log.d(TAG, "B-Zone pointer re-acquired: " + pointerId);
    }

    // ========================================================================
    // Module 3: A/B Coordination
    // ========================================================================

    /**
     * Called when A-zone gets a new pointer DOWN while B-zone is active.
     * Per spec: A-zone has highest priority — pause B-zone scroll.
     */
    private void onAZoneGestureStarted() {
        Log.d(TAG, "Module3: A-zone started → pausing B-zone scroll");
        gestureManager.pauseScroll();
        aZoneHasActivePointer = true;
    }

    /**
     * Called when the last A-zone pointer lifts.
     * Per spec: resume B-zone scroll if B-zone pointer is still held.
     */
    private void onAZoneGestureEnded() {
        Log.d(TAG, "Module3: A-zone ended → resuming B-zone scroll");
        gestureManager.resumeScrollIfActive();
    }

    // ========================================================================
    // Geometry helpers
    // ========================================================================

    private boolean isInEdgeStrip(float x) {
        if (webView == null) return false;
        int width = webView.getWidth();
        return x < edgeStripWidthPx || x > (width - edgeStripWidthPx);
    }

    private boolean isInBZone(float x, float y) {
        float dx = x - bZoneCenterX;
        float dy = y - bZoneCenterY;
        return Math.sqrt(dx * dx + dy * dy) <= bZoneRadiusPx;
    }

    // ========================================================================
    // A-Zone pointer tracking
    // ========================================================================

    private void registerAZonePointer(int pointerId, float x, float y) {
        aZonePointers.put(pointerId, new float[]{x, y});
        aZoneHasActivePointer = true;
    }

    private void updateAZonePointer(int pointerId, float x, float y) {
        float[] pos = aZonePointers.get(pointerId);
        if (pos != null) {
            pos[0] = x;
            pos[1] = y;
        }
    }

    private void removeAZonePointer(int pointerId) {
        aZonePointers.remove(pointerId);
    }

    // ---- Public accessors ----

    public boolean isBZoneActive() {
        return bZoneState == BZoneState.ACTIVE || bZoneState == BZoneState.DISMISSING;
    }
}
