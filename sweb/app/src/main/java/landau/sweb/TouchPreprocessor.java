package landau.sweb;

import android.content.Context;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * TouchPreprocessor — composes TouchCapture + TouchClassifier to turn
 * MotionEvent -> labeled PointerEvents, ready for routing by the coordinator.
 */
public class TouchPreprocessor {
    private final TouchCapture capture;
    private final TouchClassifier classifier;

    public static class LabeledPointer {
        public final PointerEvent event;
        public final TouchClassifier.Label label;
        public LabeledPointer(PointerEvent event, TouchClassifier.Label label) {
            this.event = event;
            this.label = label;
        }
    }

    public TouchPreprocessor(Context context) {
        this.capture = new TouchCapture(context);
        this.classifier = new TouchClassifier(context);
    }

    // For injection/testing
    public TouchPreprocessor(TouchCapture capture, TouchClassifier classifier) {
        this.capture = capture;
        this.classifier = classifier;
    }

    /**
     * Process a MotionEvent: capture pointer events and classify each one.
     */
    public List<LabeledPointer> process(MotionEvent ev) {
        List<PointerEvent> captured = capture.capture(ev);
        List<LabeledPointer> out = new ArrayList<>(captured.size());
        for (PointerEvent pe : captured) {
            TouchClassifier.Label lbl = classifier.classify(pe);
            out.add(new LabeledPointer(pe, lbl));
        }
        return out;
    }

    /**
     * Convenience: return true if any pointer in the MotionEvent was labeled GRIP
     */
    public boolean anyGrip(MotionEvent ev) {
        for (LabeledPointer lp : process(ev)) {
            if (lp.label == TouchClassifier.Label.GRIP) return true;
        }
        return false;
    }
}
