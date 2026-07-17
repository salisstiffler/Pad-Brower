package landau.sweb;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * PinchGestureManager — Module 2: B-Zone Touch Processor.
 *
 * Tracks the B-zone pointer and translates its vertical micro-movements
 * into WebView scroll commands. The scroll speed tiers are:
 *
 *   |Δy| ≤ DEAD_ZONE_MM          → no scroll
 *   STEP_SCROLL_MIN..MAX_MM      → single step scroll (one-shot, 3cm equivalent)
 *   SLOW_SCROLL_MIN..MAX_MM      → continuous slow scroll (via Choreographer)
 *   > FAST_SCROLL_THRESHOLD_MM   → continuous fast scroll (readable speed)
 *
 * Direction: upward finger movement → scroll page content upward (positive scrollBy Y).
 */
public class PinchGestureManager {

    private static final String TAG = "PinchGestureManager";

    // ---- Scroll state machine ----
    private static final int SCROLL_NONE = 0;
    private static final int SCROLL_STEP_SENT = 1;   // one-shot step was sent
    private static final int SCROLL_CONTINUOUS = 2;  // continuous scroll running

    private int scrollState = SCROLL_NONE;
    private float downY = 0f;              // Y coordinate where B-zone pointer first pressed
    private boolean gestureActive = false;

    // Threshold pixels (computed from mm on init)
    private float deadZonePx;
    private float stepMinPx, stepMaxPx;
    private float slowMinPx, slowMaxPx;
    private float fastThresholdPx;
    private float stepScrollDistancePx;
    private float slowScrollStepPx;
    private float fastScrollStepPx;

    // Continuous scroll
    private Choreographer.FrameCallback frameCallback;
    private boolean continuousScrollRunning = false;
    private int continuousScrollStepPx = 0; // pixels to scroll per frame

    private final Context context;
    private WebView webView;
    private final PinchOverlayView overlayView;
    private final Listener listener;

    public interface Listener {
        /** Called when B-zone gesture begins (for Module 3 awareness) */
        void onBZoneGestureStarted();
        /** Called when B-zone gesture ends (for Module 3) */
        void onBZoneGestureEnded();
        /** Current gesture scroll indicator 0..1 (for overlay) */
        void onBZoneScrollProgress(float progress);
    }

    public PinchGestureManager(Context context, PinchOverlayView overlayView, Listener listener) {
        this.context = context;
        this.overlayView = overlayView;
        this.listener = listener;
        computeThresholds();
    }

    private void computeThresholds() {
        deadZonePx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.DEAD_ZONE_MM);
        stepMinPx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.STEP_SCROLL_MIN_MM);
        stepMaxPx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.STEP_SCROLL_MAX_MM);
        slowMinPx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.SLOW_SCROLL_MIN_MM);
        slowMaxPx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.SLOW_SCROLL_MAX_MM);
        fastThresholdPx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.FAST_SCROLL_THRESHOLD_MM);
        stepScrollDistancePx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.STEP_SCROLL_DISTANCE_MM);
        slowScrollStepPx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.SLOW_SCROLL_STEP_MM);
        fastScrollStepPx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.FAST_SCROLL_STEP_MM);
    }

    public void setWebView(WebView webView) {
        this.webView = webView;
    }

    // ---- Event Entry Points ----

    /**
     * Called by the touch dispatcher when the B-zone pointer first presses down.
     * @param y the Y coordinate of the pointer
     */
    public void onBZoneDown(float y) {
        downY = y;
        gestureActive = true;
        scrollState = SCROLL_NONE;
        stopContinuousScroll();
        listener.onBZoneGestureStarted();
        Log.d(TAG, "B-Zone DOWN at y=" + y);
    }

    private float lastKnownY = 0f;

    /**
     * Called on MOVE events for the B-zone pointer.
     * @param currentY current Y of the B-zone pointer
     */
    public void onBZoneMove(float currentY) {
        lastKnownY = currentY;
        if (!gestureActive) return;

        float dy = currentY - downY; // positive = finger moved down; negative = finger moved up
        float absdy = Math.abs(dy);
        // Finger moving UP (negative dy) means user wants to scroll content UP
        // WebView.scrollBy(0, positive) scrolls page down (content up) — that's what we want

        if (absdy <= deadZonePx) {
            // Dead zone — stop any continuous scroll
            if (continuousScrollRunning) {
                stopContinuousScroll();
            }
            scrollState = SCROLL_NONE;
            listener.onBZoneScrollProgress(0f);
            overlayView.setScrollIndicator(0f, false);

        } else if (absdy > stepMinPx && absdy <= stepMaxPx) {
            // Tier 2: step scroll (one-shot, 3cm)
            if (scrollState != SCROLL_STEP_SENT) {
                stopContinuousScroll();
                scrollState = SCROLL_STEP_SENT;
                int scrollDir = dy < 0 ? 1 : -1; // up movement → positive (scroll up)
                doScrollBy((int) (scrollDir * stepScrollDistancePx));
                float progress = (absdy - stepMinPx) / (stepMaxPx - stepMinPx) * 0.33f;
                listener.onBZoneScrollProgress(progress);
                overlayView.setScrollIndicator(progress, true);
            }

        } else if (absdy > slowMinPx && absdy <= slowMaxPx) {
            // Tier 3: slow continuous scroll
            int stepPx = (int) slowScrollStepPx;
            int scrollDir = dy < 0 ? 1 : -1;
            startOrUpdateContinuousScroll(scrollDir * stepPx);
            float progress = 0.33f + (absdy - slowMinPx) / (slowMaxPx - slowMinPx) * 0.34f;
            listener.onBZoneScrollProgress(progress);
            overlayView.setScrollIndicator(progress, true);

        } else if (absdy > fastThresholdPx) {
            // Tier 4: fast continuous scroll (readable speed)
            int stepPx = (int) fastScrollStepPx;
            int scrollDir = dy < 0 ? 1 : -1;
            startOrUpdateContinuousScroll(scrollDir * stepPx);
            float progress = Math.min(1f, 0.67f + (absdy - fastThresholdPx) / fastThresholdPx * 0.33f);
            listener.onBZoneScrollProgress(progress);
            overlayView.setScrollIndicator(progress, true);
        }
    }

    /**
     * Called when the B-zone pointer is lifted.
     */
    public void onBZoneUp() {
        if (!gestureActive) return;
        Log.d(TAG, "B-Zone UP");
        gestureActive = false;
        // Don't stop continuous scroll immediately — Module 3 may resume it
        // Continuous scroll will be stopped by stopGesture() when B-zone dismisses
        listener.onBZoneGestureEnded();
    }

    /**
     * Immediately stop all scrolling from Module 2.
     * Called by Module 3 when A-zone takes priority.
     */
    public void pauseScroll() {
        Log.d(TAG, "B-Zone scroll PAUSED (A-zone priority)");
        stopContinuousScroll();
        scrollState = SCROLL_NONE;
    }

    /**
     * Resume continuous scroll after A-zone gesture ends (if B-zone pointer still in range).
     * Called by Module 3 when A-zone gesture finishes.
     */
    public void resumeScrollIfActive() {
        if (!gestureActive) return;
        Log.d(TAG, "B-Zone scroll RESUMED");
        // Re-trigger move logic with the last known position to restart continuous scroll if needed
        onBZoneMove(lastKnownY);
    }

    /**
     * Fully stop and reset the gesture state. Called on B-zone dismiss.
     */
    public void stopGesture() {
        gestureActive = false;
        stopContinuousScroll();
        scrollState = SCROLL_NONE;
        overlayView.setScrollIndicator(0f, false);
    }

    // ---- Continuous Scroll Internals ----

    private void startOrUpdateContinuousScroll(int stepPx) {
        continuousScrollStepPx = stepPx;
        if (!continuousScrollRunning) {
            continuousScrollRunning = true;
            scheduleNextFrame();
        }
    }

    private void scheduleNextFrame() {
        if (!continuousScrollRunning) return;
        frameCallback = frameTimeNanos -> {
            if (!continuousScrollRunning || webView == null) return;
            doScrollBy(continuousScrollStepPx);
            scheduleNextFrame();
        };
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    private void stopContinuousScroll() {
        continuousScrollRunning = false;
        if (frameCallback != null) {
            Choreographer.getInstance().removeFrameCallback(frameCallback);
            frameCallback = null;
        }
    }

    private void doScrollBy(int dy) {
        if (webView == null) return;
        webView.scrollBy(0, dy);
    }

    public boolean isGestureActive() {
        return gestureActive;
    }

    public boolean isContinuousScrollRunning() {
        return continuousScrollRunning;
    }
}
