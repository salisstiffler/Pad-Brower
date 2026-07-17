package landau.sweb;

import android.content.Context;

/**
 * TouchClassifier — simple heuristic classifier for labeling pointer events
 * as GRIP (B-zone) or INTERACTION (A-zone).
 *
 * Heuristics used (configurable):
 * - If pointer lies within the edge strip -> GRIP
 * - If pointer is within configured B-zone radius around calibrated center -> GRIP
 * - If pressure exceeds threshold -> GRIP
 * - Otherwise -> INTERACTION
 */
public class TouchClassifier {
    public enum Label { GRIP, INTERACTION }

    private final RegionPartition partition;
    private final TouchRegionCalibration calibration;
    private final float pressureThreshold;

    public TouchClassifier(Context context) {
        this.partition = new RegionPartition(context);
        this.calibration = new TouchRegionCalibration(context);
        this.pressureThreshold = 0.7f; // conservative default
    }

    // For tests / injection
    public TouchClassifier(RegionPartition partition, TouchRegionCalibration calib, float pressureThreshold) {
        this.partition = partition;
        this.calibration = calib;
        this.pressureThreshold = pressureThreshold;
    }

    public Label classify(PointerEvent pe) {
        if (pe == null) return Label.INTERACTION;
        // Edge strip wins
        if (partition.isInEdgeStrip(pe.x)) return Label.GRIP;
        // Calibrated B-zone
        if (calibration != null && calibration.getRadiusPx() > 0) {
            if (partition.isInBZone(pe.x, pe.y, calibration.getCenterX(), calibration.getCenterY())) return Label.GRIP;
        }
        // Pressure heuristic
        if (pe.pressure >= pressureThreshold) return Label.GRIP;
        return Label.INTERACTION;
    }
}
