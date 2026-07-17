package landau.sweb;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TouchClassifierTest {
    @Mock
    private Context mockContext;
    @Mock
    private Resources mockResources;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        DisplayMetrics dm = new DisplayMetrics();
        dm.widthPixels = 2560;
        dm.xdpi = 254f; dm.ydpi = 254f; // 1mm = 10px
        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getDisplayMetrics()).thenReturn(dm);
    }

    @Test
    public void testEdgeGrip() {
        RegionPartition rp = new RegionPartition(mockContext);
        TouchRegionCalibration calib = new TouchRegionCalibration(mockContext);
        TouchClassifier c = new TouchClassifier(rp, calib, 0.9f);
        PointerEvent p = new PointerEvent(0, System.currentTimeMillis(), 2f, 100f, 0.2f);
        assertEquals(TouchClassifier.Label.GRIP, c.classify(p));
    }

    @Test
    public void testCenterInteraction() {
        RegionPartition rp = new RegionPartition(mockContext);
        TouchRegionCalibration calib = new TouchRegionCalibration(mockContext);
        TouchClassifier c = new TouchClassifier(rp, calib, 0.5f);
        PointerEvent p = new PointerEvent(0, System.currentTimeMillis(), 500f, 500f, 0.3f);
        assertEquals(TouchClassifier.Label.INTERACTION, c.classify(p));
    }

    @Test
    public void testPressureGrip() {
        RegionPartition rp = new RegionPartition(mockContext);
        TouchRegionCalibration calib = new TouchRegionCalibration(mockContext);
        TouchClassifier c = new TouchClassifier(rp, calib, 0.4f);
        PointerEvent p = new PointerEvent(0, System.currentTimeMillis(), 500f, 500f, 0.6f);
        assertEquals(TouchClassifier.Label.GRIP, c.classify(p));
    }
}
