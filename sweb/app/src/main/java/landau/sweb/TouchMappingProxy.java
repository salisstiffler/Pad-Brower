package landau.sweb;

import android.view.MotionEvent;
import android.util.Log;

/**
 * TouchMappingProxy — handles suppression/remapping of grip events and forwards
 * processed (A-zone) events to the ChromiumEventAdapter for injection.
 */
import android.os.Handler;
import android.os.Looper;

public class TouchMappingProxy {
    private static final String TAG = "TouchMappingProxy";

    private final ChromiumEventAdapter adapter;

    // Simple batch buffer to reduce frequent dispatch calls and MotionEvent churn
    private final java.util.List<MotionEvent> batchBuffer = new java.util.ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();
    private final int FLUSH_MS = 16; // ~1 frame
    private final int MAX_BATCH = 8;
    private final Runnable flushRunnable = this::flushBatch;

    public TouchMappingProxy(ChromiumEventAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Forward a synthesized A-zone event to the engine.
     * Returns true if the underlying adapter consumed it.
     * This implementation batches events briefly to reduce overhead.
     */
    public boolean forwardAzoneEvent(MotionEvent ev) {
        if (ev == null) return false;
        if (adapter == null) return false;

        // Add a copy to the batch buffer; schedule a flush
        synchronized (lock) {
            batchBuffer.add(MotionEvent.obtain(ev));
            if (batchBuffer.size() >= MAX_BATCH) {
                // immediate flush
                handler.removeCallbacks(flushRunnable);
                flushBatch();
            } else {
                // debounce flush to group events within a frame
                handler.removeCallbacks(flushRunnable);
                handler.postDelayed(flushRunnable, FLUSH_MS);
            }
        }
        return true; // we consumed it locally (forwarding asynchronously)
    }

    private void flushBatch() {
        java.util.List<MotionEvent> toSend;
        synchronized (lock) {
            if (batchBuffer.isEmpty()) return;
            toSend = new java.util.ArrayList<>(batchBuffer);
            batchBuffer.clear();
        }
        try {
            adapter.injectBatch(toSend.toArray(new MotionEvent[0]));
        } catch (Throwable t) {
            Log.w(TAG, "batch forward failed", t);
        } finally {
            for (MotionEvent e : toSend) {
                try { e.recycle(); } catch (Throwable ignored) {}
            }
        }
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
