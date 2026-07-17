package landau.sweb;

import android.content.Context;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * TouchCapture — captures MotionEvent and normalizes into PointerEvent list.
 * Provides simple in-memory queue for downstream modules.
 */
public class TouchCapture {
    private final Context context;
    private final List<PointerEvent> queue = new ArrayList<>();

    public TouchCapture(Context context) {
        this.context = context;
    }

    /**
     * Convert MotionEvent to a list of PointerEvent (one per pointer index)
     */
    public List<PointerEvent> capture(MotionEvent ev) {
        if (ev == null) return new ArrayList<>();
        long t = ev.getEventTime();
        List<PointerEvent> out = new ArrayList<>();
        for (int i = 0; i < ev.getPointerCount(); i++) {
            int id = ev.getPointerId(i);
            float x = ev.getX(i);
            float y = ev.getY(i);
            float p = ev.getPressure(i);
            PointerEvent pe = new PointerEvent(id, t, x, y, p);
            out.add(pe);
            synchronized (queue) {
                queue.add(pe);
                // keep queue bounded to last 256 events
                if (queue.size() > 256) queue.remove(0);
            }
        }
        return out;
    }

    public List<PointerEvent> drainQueue() {
        synchronized (queue) {
            List<PointerEvent> copy = new ArrayList<>(queue);
            queue.clear();
            return copy;
        }
    }
}
