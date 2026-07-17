package landau.sweb;

import org.junit.Test;
import static org.junit.Assert.*;

public class PinchZoneConfigTest {

    @Test
    public void testConfigConstants() {
        assertEquals(6.0f, PinchZoneConfig.EDGE_STRIP_MM, 0.001f);
        assertEquals(500L, PinchZoneConfig.B_ZONE_ACTIVATE_DELAY_MS);
        assertEquals(1500L, PinchZoneConfig.B_ZONE_DISMISS_DELAY_MS);
        assertEquals(10.0f, PinchZoneConfig.B_ZONE_RADIUS_MM, 0.001f);
        
        assertEquals(2.0f, PinchZoneConfig.DEAD_ZONE_MM, 0.001f);
        assertEquals(2.0f, PinchZoneConfig.STEP_SCROLL_MIN_MM, 0.001f);
        assertEquals(4.0f, PinchZoneConfig.STEP_SCROLL_MAX_MM, 0.001f);
        assertEquals(30.0f, PinchZoneConfig.STEP_SCROLL_DISTANCE_MM, 0.001f);
        
        assertEquals(4.0f, PinchZoneConfig.SLOW_SCROLL_MIN_MM, 0.001f);
        assertEquals(6.0f, PinchZoneConfig.SLOW_SCROLL_MAX_MM, 0.001f);
        assertEquals(6.0f, PinchZoneConfig.FAST_SCROLL_THRESHOLD_MM, 0.001f);
    }
}