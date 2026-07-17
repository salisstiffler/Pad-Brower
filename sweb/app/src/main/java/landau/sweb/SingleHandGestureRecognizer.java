package landau.sweb;

/**
 * Simple recognizer for single-hand gestures based on A-zone pointer movement.
 * Detects quick vertical swipes (page turn) and short scrubs (thumb-avoid nudges).
 */
public class SingleHandGestureRecognizer {

    public interface Listener {
        void onPageTurnUp();   // user swiped up -> next page / pageDown
        void onPageTurnDown(); // user swiped down -> previous page / pageUp
        void onSmallNudge(float dyPx); // small scrub for thumb-avoid scrolling
    }

    private Listener listener;
    private int activePointerId = -1;
    private float startY;
    private float startX;
    private long startTime;

    // thresholds (px) - tuned via PinchZoneConfig mm->px externally
    private float pageTurnThresholdPx = -1f; // if negative, use 30mm
    private float nudgeThresholdPx = -1f; // 5mm

    public SingleHandGestureRecognizer() {
    }

    public void setListener(Listener l) { this.listener = l; }

    public void setPageTurnThresholdPx(float px) { this.pageTurnThresholdPx = px; }
    public void setNudgeThresholdPx(float px) { this.nudgeThresholdPx = px; }

    public void onDown(int pointerId, float x, float y) {
        activePointerId = pointerId;
        startY = y; startX = x; startTime = System.currentTimeMillis();
    }

    public void onMove(int pointerId, float x, float y) {
        if (pointerId != activePointerId) return;
        float dy = y - startY;
        float ady = Math.abs(dy);
        if (nudgeThresholdPx > 0 && ady >= nudgeThresholdPx && ady < getPageTurnThresholdPx()) {
            if (listener != null) listener.onSmallNudge(dy);
        }
    }

    public void onUp(int pointerId, float x, float y) {
        if (pointerId != activePointerId) return;
        float dy = y - startY;
        long dt = System.currentTimeMillis() - startTime;
        // Quick swipe heuristic
        if (Math.abs(dy) >= getPageTurnThresholdPx() && dt < 800) {
            if (dy < 0) {
                if (listener != null) listener.onPageTurnUp();
            } else {
                if (listener != null) listener.onPageTurnDown();
            }
        }
        activePointerId = -1;
    }

    private float getPageTurnThresholdPx() {
        if (pageTurnThresholdPx > 0) return pageTurnThresholdPx;
        // Caller must set threshold via setPageTurnThresholdPx(context-derived value)
        return 99999f; // effectively disable until configured
    }
}
