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

public class TouchRegionPartitionTest {
    @Mock
    private Context mockContext;
    @Mock
    private Resources mockResources;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        DisplayMetrics dm = new DisplayMetrics();
        // Simulate 2560px width and ydpi such that mm->px conversion known
        dm.widthPixels = 2560;
        dm.xdpi = 254f; dm.ydpi = 254f; // 1mm = 10px
        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getDisplayMetrics()).thenReturn(dm);
    }

    @Test
    public void testEdgeStripDetection() {
        RegionPartition rp = new RegionPartition(mockContext);
        float edgePx = rp.getEdgeStripPx();
        // left edge
        assertTrue(rp.isInEdgeStrip(1f));
        // inside edge width
        assertTrue(rp.isInEdgeStrip(edgePx - 1f));
        // just outside left edge
        assertFalse(rp.isInEdgeStrip(edgePx + 10f));
        // right edge
        assertTrue(rp.isInEdgeStrip(2560 - 1));
    }

    @Test
    public void testBZoneDetection() {
        RegionPartition rp = new RegionPartition(mockContext);
        float cx = 200f, cy = 300f;
        float rad = rp.getBZoneRadiusPx();
        // center is inside
        assertTrue(rp.isInBZone(cx, cy, cx, cy));
        // just inside radius
        assertTrue(rp.isInBZone(cx + rad - 1f, cy, cx, cy));
        // outside radius
        assertFalse(rp.isInBZone(cx + rad + 10f, cy, cx, cy));
    }
}
