package landau.sweb;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Simple calibration holder for B-zone center/radius. If context provided,
 * values are persisted to SharedPreferences under key "pinch_calib".
 */
public class TouchRegionCalibration {
    private static final String PREF = "pinch_calib";
    private static final String KEY_CX = "cx";
    private static final String KEY_CY = "cy";
    private static final String KEY_R = "r";

    private final Context context;
    private float centerX = -1f;
    private float centerY = -1f;
    private float radiusPx = -1f;

    public TouchRegionCalibration(Context context) {
        this.context = context;
        load();
    }

    public void setCalibration(float cx, float cy, float radiusPx) {
        this.centerX = cx;
        this.centerY = cy;
        this.radiusPx = radiusPx;
        save();
    }

    public float getCenterX() { return centerX; }
    public float getCenterY() { return centerY; }
    public float getRadiusPx() { return radiusPx; }

    private void save() {
        if (context == null) return;
        try {
            SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            p.edit().putFloat(KEY_CX, centerX).putFloat(KEY_CY, centerY).putFloat(KEY_R, radiusPx).apply();
        } catch (Throwable ignored) {}
    }

    private void load() {
        if (context == null) return;
        try {
            SharedPreferences p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            if (p.contains(KEY_CX)) {
                centerX = p.getFloat(KEY_CX, -1f);
                centerY = p.getFloat(KEY_CY, -1f);
                radiusPx = p.getFloat(KEY_R, -1f);
            }
        } catch (Throwable ignored) {}
    }
}
