package landau.sweb;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

/**
 * GripSdk — lightweight integration wrapper exposing the pinch-hold (grip) features
 * to host apps. Provides a simple init() and enable/disable API so other apps
 * can adopt the overlay and dispatcher without touching internal classes.
 */
public class GripSdk {

    private static TouchZoneDispatcher dispatcher;
    private static PinchGestureManager gestureManager;
    private static PinchOverlayView overlayView;
    private static PinchWebViewContainer container;

    /**
     * Initialize the Grip SDK and wire it into the given container and WebView.
     * Must be called from the UI thread after the activity layout is inflated.
     *
     * @param activity host activity (used for resources and mm→px conversion)
     * @param containerView the FrameLayout that contains the WebView(s) and overlay
     * @param webView the initial WebView to route events/scrolls to
     */
    public static void init(Activity activity, FrameLayout containerView, WebView webView) {
        if (activity == null || containerView == null || webView == null) return;

        Context ctx = activity.getApplicationContext();

        // Create overlay and add to container if not already present
        overlayView = new PinchOverlayView(activity);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        overlayView.setLayoutParams(lp);
        containerView.addView(overlayView);
        overlayView.bringToFront();

        // Create gesture manager and dispatcher
        gestureManager = new PinchGestureManager(activity, overlayView, new PinchGestureManager.Listener() {
            @Override
            public void onBZoneGestureStarted() { }

            @Override
            public void onBZoneGestureEnded() { }

            @Override
            public void onBZoneScrollProgress(float progress) {
                overlayView.setScrollIndicator(progress, progress > 0.01f);
            }
        });

        dispatcher = new TouchZoneDispatcher(activity, gestureManager, overlayView);
        dispatcher.setWebView(webView);

        // Create a container wrapper so host app can route touch events through dispatcher
        container = new PinchWebViewContainer(activity);
        container.init(dispatcher, overlayView);

        // Initialize overlay sizing from config
        float edgeStripPx = PinchZoneConfig.mmToPx(activity, PinchZoneConfig.EDGE_STRIP_MM);
        float bZoneRadiusPx = PinchZoneConfig.mmToPx(activity, PinchZoneConfig.B_ZONE_RADIUS_MM);
        overlayView.init(edgeStripPx, bZoneRadiusPx);

        // Show startup hint
        overlayView.showEdgeHintBriefly();
    }

    public static void setWebView(WebView webView) {
        if (dispatcher != null) dispatcher.setWebView(webView);
    }

    public static void enable() {
        if (overlayView != null) overlayView.setVisibility(android.view.View.VISIBLE);
    }

    public static void disable() {
        if (overlayView != null) overlayView.setVisibility(android.view.View.GONE);
    }

    public static TouchZoneDispatcher getDispatcher() { return dispatcher; }
    public static PinchOverlayView getOverlayView() { return overlayView; }
}
