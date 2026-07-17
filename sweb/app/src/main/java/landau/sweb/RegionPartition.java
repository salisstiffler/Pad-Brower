package landau.sweb;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * RegionPartition: decides whether a pointer lies in edge strips or B-zone.
 */
public class RegionPartition {
    private final Context context;
    private final int screenWidthPx;
    private final float edgeStripPx;
    private final float bZoneRadiusPx;

    public RegionPartition(Context context) {
        this.context = context;
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        this.screenWidthPx = dm.widthPixels;
        this.edgeStripPx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.EDGE_STRIP_MM);
        this.bZoneRadiusPx = PinchZoneConfig.mmToPx(context, PinchZoneConfig.B_ZONE_RADIUS_MM);
    }

    public boolean isInEdgeStrip(float x) {
        if (screenWidthPx <= 0) return false;
        return x <= edgeStripPx || x >= (screenWidthPx - edgeStripPx);
    }

    public boolean isInBZone(float x, float y, float centerX, float centerY) {
        double dx = x - centerX;
        double dy = y - centerY;
        double dist = Math.hypot(dx, dy);
        return dist <= bZoneRadiusPx;
    }

    public float getEdgeStripPx() { return edgeStripPx; }
    public float getBZoneRadiusPx() { return bZoneRadiusPx; }
}
