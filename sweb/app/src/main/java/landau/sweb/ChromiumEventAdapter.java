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

    /**
     * Inject a batch of events in order while preserving event timestamps.
     * This helps preserve temporal spacing when forwarding synthesized sequences.
     */
    public boolean injectBatch(MotionEvent[] events) {
        if (webView == null || events == null) return false;
        boolean anyConsumed = false;
        for (MotionEvent e : events) {
            if (e == null) continue;
            // Clone to avoid recycling side-effects from callers
            MotionEvent clone = MotionEvent.obtain(e);
            try {
                anyConsumed |= webView.dispatchTouchEvent(clone);
            } catch (Throwable ignored) {
            } finally {
                clone.recycle();
            }
        }
        return anyConsumed;
    }

    // NOTE: Future improvement: replace dispatchTouchEvent with a JNI/native
    // injection to Chromium's InputInjector for lower latency and timestamp fidelity.
}

