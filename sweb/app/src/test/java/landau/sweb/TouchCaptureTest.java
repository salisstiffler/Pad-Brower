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
        // Create MotionEvent via obtain
        long now = System.currentTimeMillis();
        MotionEvent ev = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 100f, 200f, 0);
        List<PointerEvent> out = tc.capture(ev);
        assertEquals(1, out.size());
        PointerEvent p = out.get(0);
        assertEquals(0, p.pointerId);
        assertEquals(100f, p.x, 0.001f);
        assertEquals(200f, p.y, 0.001f);
        ev.recycle();
    }

    @Test
    public void testCapture_multiPointer() {
        TouchCapture tc = new TouchCapture(mockContext);
        long now = System.currentTimeMillis();
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[2];
        for (int i=0;i<2;i++){
            props[i]= new MotionEvent.PointerProperties();
            props[i].id = i;
            coords[i]= new MotionEvent.PointerCoords();
            coords[i].x = 50f + i*10f;
            coords[i].y = 60f + i*20f;
            coords[i].pressure = 0.5f + i*0.1f;
        }
        MotionEvent ev = MotionEvent.obtain(0, now, MotionEvent.ACTION_MOVE, 2, props, coords, 0, 0, 1.0f, 1.0f, 0, 0, 0, 0);
        List<PointerEvent> out = tc.capture(ev);
        assertEquals(2, out.size());
        assertEquals(50f, out.get(0).x, 0.001f);
        assertEquals(80f, out.get(1).y, 0.001f); // 60 + 20
        ev.recycle();
    }
}
