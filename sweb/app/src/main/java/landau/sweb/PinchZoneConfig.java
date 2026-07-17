package landau.sweb;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Configuration for the Pinch-Hold (B-Zone) gesture system.
 * All spatial values are defined in mm and converted to pixels at runtime.
 */
public class PinchZoneConfig {

    // --- Edge Detection Zone (triggers B-zone creation) ---
    /** Width of the edge detection strip on left/right sides, in mm */
    public static float EDGE_STRIP_MM = 6.0f;

    // --- B-Zone Timing ---
    /** Time in ms the user must hold in the edge zone to activate B-zone */
    public static long B_ZONE_ACTIVATE_DELAY_MS = 500;

    /** Time in ms after leaving B-zone before it auto-dismisses */
    public static final long B_ZONE_DISMISS_DELAY_MS = 1500;

    // --- B-Zone Size ---
    /** Radius of the circular B-zone button area, in mm */
    public static final float B_ZONE_RADIUS_MM = 10.0f;

    // --- Edge hint display ---
    /** Alpha of the edge hint strip when shown (0..255) */
    public static final int EDGE_HINT_ALPHA = 204; // ~80%
    /** How long the edge hint is shown at startup, in ms */
    public static final long EDGE_HINT_SHOW_DURATION_MS = 1000;
    /** Fade-out duration of edge hint, in ms */
    public static final long EDGE_HINT_FADE_MS = 200;

    // --- Visual & Haptic tuning ---
    /** Pulse animation duration for the B-zone visual (ms) */
    public static final long B_ZONE_PULSE_MS = 400;
    /** Haptic feedback duration on activation/dismiss (ms) */
    public static final int HAPTIC_FEEDBACK_MS = 20;

    // --- Module 2: B-Zone Gesture Thresholds ---
    /** Vertical movement <= this: no gesture (dead zone), in mm */
    public static final float DEAD_ZONE_MM = 2.0f;
    /** Vertical movement 2-4mm: single page step scroll */
    public static final float STEP_SCROLL_MIN_MM = 2.0f;
    public static final float STEP_SCROLL_MAX_MM = 4.0f;
    /** Step scroll distance injected into WebView, in mm */
    public static final float STEP_SCROLL_DISTANCE_MM = 30.0f; // ~3cm

    /** Vertical movement 4-6mm: slow continuous scroll */
    public static final float SLOW_SCROLL_MIN_MM = 4.0f;
    public static final float SLOW_SCROLL_MAX_MM = 6.0f;
    /** Slow continuous scroll step per frame (Choreographer), in mm */
    public static final float SLOW_SCROLL_STEP_MM = 3.0f;

    /** Vertical movement > 6mm: fast continuous scroll */
    public static final float FAST_SCROLL_THRESHOLD_MM = 6.0f;
    /** Fast scroll step per frame, in mm — tuned to readable speed */
    public static final float FAST_SCROLL_STEP_MM = 8.0f;

    // --- Direction: upward movement = positive Y delta = scroll DOWN (content up) ---
    // (User pushes finger up → page scrolls up → content moves down visually)

    // ---- Utility ----

    /** Convert mm to pixels using the screen density */
    public static float mmToPx(Context context, float mm) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        // dm.xdpi or dm.ydpi could differ; use ydpi for vertical measurements
        return mm * dm.ydpi / 25.4f;
    }

    /** Convert mm to pixels (integer) */
    public static int mmToPxInt(Context context, float mm) {
        return Math.round(mmToPx(context, mm));
    }
}
