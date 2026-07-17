package landau.sweb;

import android.view.MotionEvent;
import android.util.Log;

/**
 * TouchMappingProxy — handles suppression/remapping of grip events and forwards
 * processed (A-zone) events to the ChromiumEventAdapter for injection.
 */
public class TouchMappingProxy {
    private static final String TAG = "TouchMappingProxy";

    private final ChromiumEventAdapter adapter;

    public TouchMappingProxy(ChromiumEventAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Forward a synthesized A-zone event to the engine.
     * Returns true if the underlying adapter consumed it.
     */
    public boolean forwardAzoneEvent(MotionEvent ev) {
        if (ev == null) return false;
        try {
            if (adapter != null) {
                return adapter.injectTouchEvent(ev);
            }
        } catch (Throwable t) {
            Log.w(TAG, "forwardAzoneEvent failed", t);
        }
        return false;
    }

    /**
     * Suppress or remap a grip (B-zone) event. By default, consume it (suppress),
     * but provide a hook for selective pass-through in the future.
     */
    public boolean suppressGripEvent(MotionEvent ev) {
        // Default behavior: swallow grip events so they don't reach WebView.
        return true;
    }

    /**
     * Convenience: synthesize and forward a simple single-pointer event.
     */
    public boolean forwardSimpleEvent(long downTime, long eventTime, int action, float x, float y, int metaState) {
        if (adapter == null) return false;
        return adapter.injectSimpleEvent(downTime, eventTime, action, x, y, metaState);
    }
}
