package landau.sweb;

import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * ChromiumEventAdapter — abstraction layer for injecting preprocessed touch events
 * into the rendering/input pipeline. Current implementation forwards events to
 * WebView.dispatchTouchEvent(), but this class intentionally centralizes the
 * integration points so it can later be replaced by a native Chromium event
 * injector (via JNI) or by using engine-specific APIs.
 */
public class ChromiumEventAdapter {

    private final WebView webView;

    public ChromiumEventAdapter(WebView webView) {
        this.webView = webView;
    }

    /**
     * Inject a MotionEvent into the underlying engine. Returns true if consumed.
     * Current behavior: forward to WebView.dispatchTouchEvent and return its result.
     */
    public boolean injectTouchEvent(MotionEvent event) {
        if (webView == null) return false;
        try {
            // Future hook: replace with Chromium-specific input injection for lower latency
            return webView.dispatchTouchEvent(event);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Convenience: synthesize and inject a simple single-pointer event
     */
    public boolean injectSimpleEvent(long downTime, long eventTime, int action, float x, float y, int metaState) {
        MotionEvent ev = MotionEvent.obtain(downTime, eventTime, action, x, y, metaState);
        boolean res = injectTouchEvent(ev);
        ev.recycle();
        return res;
    }

    // TODO: Add native hooks, batching, and timestamp preservation for high-fidelity injection.
}
