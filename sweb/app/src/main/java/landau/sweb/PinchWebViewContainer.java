package landau.sweb;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.FrameLayout;

/**
 * PinchWebViewContainer — FrameLayout that holds the WebView + PinchOverlayView.
 *
 * This is the outer container. It intercepts touch events BEFORE the WebView
 * sees them, passes them through the TouchZoneDispatcher, and only forwards
 * non-consumed events to the WebView children.
 *
 * Layout (z-order, bottom to top):
 *   [WebView(s)]
 *   [PinchOverlayView]  ← transparent, always on top, does not intercept clicks
 *
 * The PinchOverlayView has clickable=false, so it does not block input itself.
 * Event interception is done at this container level via onInterceptTouchEvent.
 */
public class PinchWebViewContainer extends FrameLayout {

    private TouchZoneDispatcher dispatcher;
    private PinchOverlayView overlayView;

    public PinchWebViewContainer(Context context) {
        super(context);
    }

    public PinchWebViewContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PinchWebViewContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(TouchZoneDispatcher dispatcher, PinchOverlayView overlayView) {
        this.dispatcher = dispatcher;
        this.overlayView = overlayView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (dispatcher == null) return false;

        // Ask the dispatcher: should we steal this event from children?
        // We intercept if:
        //  - B-zone is active and the pointer is in B-zone area
        //  - A pointer is in the edge detection strip (pending activation)
        // The dispatcher handles this decision internally.
        // We return true only when we want to take control away from WebView.

        // To keep things simple: we do NOT intercept here. Instead, we let
        // dispatchTouchEvent handle everything. The WebView child will receive
        // events only when the dispatcher returns false.
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (dispatcher != null) {
            boolean consumed = dispatcher.dispatchTouchEvent(ev);
            if (consumed) {
                // Event handled by our gesture system — don't pass to children
                return true;
            }
        }
        // Pass event through to children (WebView, etc.) normally
        return super.dispatchTouchEvent(ev);
    }
}
