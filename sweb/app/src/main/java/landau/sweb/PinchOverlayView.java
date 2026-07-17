package landau.sweb;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.animation.ValueAnimator;
import android.animation.Animator;

/**
 * PinchOverlayView — a transparent overlay drawn on top of the WebView.
 *
 * Responsibilities:
 *  1. Display edge hint strips at startup (left/right edges, fading after 1s).
 *  2. Render the B-zone circular button area when it is active.
 *  3. Provide a visual indicator showing B-zone scroll gesture direction.
 */
public class PinchOverlayView extends View {

    private static final String TAG = "PinchOverlayView";

    // --- State ---
    private boolean edgeHintVisible = false;
    private float edgeHintAlpha = 0f;

    private boolean bZoneActive = false;
    private PointF bZoneCenter = new PointF();
    private float bZoneRadius = 0f;
    private float bZoneAlpha = 0f;  // 0f = fully transparent, 1f = visible
    // base radius used for pulse scaling
    private float baseBZoneRadius = 0f;
    // Animator for subtle pulse when B-zone activates
    private ValueAnimator bZonePulseAnimator;

    // Scroll direction indicator
    private boolean scrollIndicatorVisible = false;
    private float scrollIndicatorProgress = 0f; // 0..1 maps to gesture thresholds

    // --- Paint objects ---
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bZonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bZoneRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Handler handler = new Handler(Looper.getMainLooper());

    // Edge strip dimensions (set from config in pixels)
    private float edgeStripWidthPx = 0f;

    public PinchOverlayView(Context context) {
        this(context, null);
    }

    public PinchOverlayView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);
        setClickable(false);

        edgePaint.setColor(Color.argb(180, 100, 149, 237)); // cornflower blue
        edgePaint.setStyle(Paint.Style.FILL);

        bZonePaint.setColor(Color.argb(40, 100, 149, 237));
        bZonePaint.setStyle(Paint.Style.FILL);

        bZoneRingPaint.setColor(Color.argb(120, 100, 149, 237));
        bZoneRingPaint.setStyle(Paint.Style.STROKE);
        bZoneRingPaint.setStrokeWidth(3f);

        arrowPaint.setColor(Color.argb(180, 255, 255, 255));
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setStrokeWidth(4f);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void init(float edgeStripWidthPx, float bZoneRadiusPx) {
        this.edgeStripWidthPx = edgeStripWidthPx;
        this.bZoneRadius = bZoneRadiusPx;
        this.baseBZoneRadius = bZoneRadiusPx;
    }

    // ---- Edge Hint ----

    /**
     * Show the edge hint strips briefly, then fade them out.
     * Called once when the app starts.
     */
    public void showEdgeHintBriefly() {
        // Requirement: "在刚打开浏览器1秒后，显示此特殊识别区...0.2秒后变成透明"
        handler.postDelayed(() -> {
            edgeHintVisible = true;
            edgeHintAlpha = PinchZoneConfig.EDGE_HINT_ALPHA / 255f;
            invalidate();

            // Show for 0.2s then fade out
            handler.postDelayed(this::fadeOutEdgeHint, 200);
        }, 1000);
    }

    private void fadeOutEdgeHint() {
        // Animate alpha from current to 0 over EDGE_HINT_FADE_MS
        final long startTime = System.currentTimeMillis();
        final float startAlpha = edgeHintAlpha;
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                float fraction = Math.min(1f, (float) elapsed / PinchZoneConfig.EDGE_HINT_FADE_MS);
                edgeHintAlpha = startAlpha * (1f - fraction);
                invalidate();
                if (fraction < 1f) {
                    handler.post(this);
                } else {
                    edgeHintVisible = false;
                }
            }
        });
    }

    // ---- B-Zone ----

    /**
     * Activate the B-zone visual at the given screen coordinates.
     */
    public void showBZone(float centerX, float centerY) {
        bZoneCenter.set(centerX, centerY);
        bZoneActive = true;
        bZoneAlpha = 1f;
        // Start a subtle pulse to signal activation
        try {
            if (bZonePulseAnimator != null && bZonePulseAnimator.isRunning()) {
                bZonePulseAnimator.cancel();
            }
            bZonePulseAnimator = ValueAnimator.ofFloat(1f, 1.08f, 1f);
            bZonePulseAnimator.setDuration((int) PinchZoneConfig.B_ZONE_PULSE_MS);
            bZonePulseAnimator.addUpdateListener(anim -> {
                float scale = (float) anim.getAnimatedValue();
                bZoneRadius = baseBZoneRadius * scale;
                invalidate();
            });
            bZonePulseAnimator.start();
        } catch (Throwable t) {
            // Fallback: ignore animation failures
            bZoneRadius = baseBZoneRadius;
        }

        invalidate();
        handler.postDelayed(this::fadeBZoneToTransparent, 200);
    }

    private void fadeBZoneToTransparent() {
        // Stop pulse animation if running
        if (bZonePulseAnimator != null) {
            try { bZonePulseAnimator.cancel(); } catch (Throwable ignored) {}
            bZonePulseAnimator = null;
            bZoneRadius = baseBZoneRadius;
        }

        ValueAnimator va = ValueAnimator.ofFloat(bZoneAlpha, 0f);
        va.setDuration((int) PinchZoneConfig.EDGE_HINT_FADE_MS);
        va.addUpdateListener(anim -> {
            bZoneAlpha = (float) anim.getAnimatedValue();
            invalidate();
        });
        va.start();
    }

    /**
     * Hide the B-zone visual entirely.
     */
    public void hideBZone() {
        bZoneActive = false;
        scrollIndicatorVisible = false;
        if (bZonePulseAnimator != null) {
            try { bZonePulseAnimator.cancel(); } catch (Throwable ignored) {}
            bZonePulseAnimator = null;
            bZoneRadius = baseBZoneRadius;
        }
        bZoneAlpha = 0f;
        invalidate();
    }

    /**
     * Update the scroll indicator based on gesture progress (0 = dead zone, 1 = fast scroll).
     */
    public void setScrollIndicator(float progress, boolean visible) {
        scrollIndicatorProgress = progress;
        scrollIndicatorVisible = visible;
        invalidate();
    }

    // ---- Drawing ----

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (edgeHintVisible && edgeHintAlpha > 0.01f) {
            drawEdgeHints(canvas);
        }

        if (bZoneActive) {
            drawBZone(canvas);
        }
    }

    private void drawEdgeHints(Canvas canvas) {
        int alpha = (int) (edgeHintAlpha * 255);
        edgePaint.setAlpha(alpha);

        // Left edge
        canvas.drawRect(0, 0, edgeStripWidthPx, getHeight(), edgePaint);
        // Right edge
        canvas.drawRect(getWidth() - edgeStripWidthPx, 0, getWidth(), getHeight(), edgePaint);
    }

    private void drawBZone(Canvas canvas) {
        int baseAlpha = (int) (bZoneAlpha * 255);

        if (baseAlpha > 5) {
            bZonePaint.setAlpha(baseAlpha);
            bZoneRingPaint.setAlpha(baseAlpha + 80);
            canvas.drawCircle(bZoneCenter.x, bZoneCenter.y, bZoneRadius, bZonePaint);
            canvas.drawCircle(bZoneCenter.x, bZoneCenter.y, bZoneRadius, bZoneRingPaint);
        }

        // Always draw the scroll indicator when B-zone is active and a gesture is happening
        if (scrollIndicatorVisible && scrollIndicatorProgress > 0.01f) {
            drawScrollArrow(canvas);
        }
    }

    private void drawScrollArrow(Canvas canvas) {
        // Draw upward arrows at the B-zone center to indicate scrolling
        float cx = bZoneCenter.x;
        float cy = bZoneCenter.y;
        float size = bZoneRadius * 0.4f;
        int arrowAlpha = Math.min(255, (int) (scrollIndicatorProgress * 200 + 55));
        arrowPaint.setAlpha(arrowAlpha);

        // Draw simple upward-pointing arrow
        float arrowHalfW = size * 0.5f;
        float arrowTop = cy - size;
        float arrowBottom = cy + size * 0.3f;

        // Stem
        arrowPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(cx, arrowTop, cx, arrowBottom, arrowPaint);

        // Head
        arrowPaint.setStyle(Paint.Style.FILL);
        float[] headPath = {
                cx, arrowTop,
                cx - arrowHalfW, arrowTop + size * 0.5f,
                cx + arrowHalfW, arrowTop + size * 0.5f
        };
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(headPath[0], headPath[1]);
        path.lineTo(headPath[2], headPath[3]);
        path.lineTo(headPath[4], headPath[5]);
        path.close();
        canvas.drawPath(path, arrowPaint);
    }
}
