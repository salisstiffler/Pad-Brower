package landau.sweb;

/**
 * Lightweight pointer event model used by the touch preprocessor pipeline.
 */
public class PointerEvent {
    public final int pointerId;
    public final long eventTimeMs;
    public final float x;
    public final float y;
    public final float pressure;

    public PointerEvent(int pointerId, long eventTimeMs, float x, float y, float pressure) {
        this.pointerId = pointerId;
        this.eventTimeMs = eventTimeMs;
        this.x = x;
        this.y = y;
        this.pressure = pressure;
    }

    @Override
    public String toString() {
        return "PointerEvent{" +
                "id=" + pointerId +
                ", t=" + eventTimeMs +
                ", x=" + x +
                ", y=" + y +
                ", p=" + pressure +
                '}';
    }
}
