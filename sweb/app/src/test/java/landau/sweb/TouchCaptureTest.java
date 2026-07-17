package landau.sweb;

import android.content.Context;
import android.view.MotionEvent;
import android.util.DisplayMetrics;
import android.content.res.Resources;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TouchCaptureTest {
    @Mock
    private Context mockContext;
    @Mock
    private Resources mockResources;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        DisplayMetrics dm = new DisplayMetrics();
        dm.xdpi = 254f; dm.ydpi = 254f; // 1mm = 10px
        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getDisplayMetrics()).thenReturn(dm);
    }

    @Test
    public void testCapture_singlePointer() {
        TouchCapture tc = new TouchCapture(mockContext);
        // Mock MotionEvent because MotionEvent.obtain may not behave in unit test JVM
        long now = System.currentTimeMillis();
        MotionEvent ev = mock(MotionEvent.class);
        when(ev.getEventTime()).thenReturn(now);
        when(ev.getPointerCount()).thenReturn(1);
        when(ev.getPointerId(0)).thenReturn(0);
        when(ev.getX(0)).thenReturn(100f);
        when(ev.getY(0)).thenReturn(200f);
        when(ev.getPressure(0)).thenReturn(1.0f);
        List<PointerEvent> out = tc.capture(ev);
        assertEquals(1, out.size());
        PointerEvent p = out.get(0);
        assertEquals(0, p.pointerId);
        assertEquals(100f, p.x, 0.001f);
        assertEquals(200f, p.y, 0.001f);
    }

    @Test
    public void testCapture_multiPointer() {
        TouchCapture tc = new TouchCapture(mockContext);
        long now = System.currentTimeMillis();
        MotionEvent ev = mock(MotionEvent.class);
        when(ev.getEventTime()).thenReturn(now);
        when(ev.getPointerCount()).thenReturn(2);
        when(ev.getPointerId(0)).thenReturn(0);
        when(ev.getPointerId(1)).thenReturn(1);
        when(ev.getX(0)).thenReturn(50f);
        when(ev.getY(0)).thenReturn(60f);
        when(ev.getPressure(0)).thenReturn(0.5f);
        when(ev.getX(1)).thenReturn(60f);
        when(ev.getY(1)).thenReturn(80f);
        when(ev.getPressure(1)).thenReturn(0.6f);
        List<PointerEvent> out = tc.capture(ev);
        assertEquals(2, out.size());
        assertEquals(50f, out.get(0).x, 0.001f);
        assertEquals(80f, out.get(1).y, 0.001f); // 60 + 20
    }
}
