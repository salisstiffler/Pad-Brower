package landau.sweb;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Choreographer;
import android.webkit.WebView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PinchGestureManagerTest {

    @Mock
    private Context mockContext;
    @Mock
    private Resources mockResources;
    private DisplayMetrics displayMetrics;

    @Mock
    private PinchOverlayView mockOverlayView;
    @Mock
    private PinchGestureManager.Listener mockListener;
    @Mock
    private WebView mockWebView;

    @Mock
    private Choreographer mockChoreographer;

    private PinchGestureManager gestureManager;

    private MockedStatic<Log> mockedLog;
    private MockedStatic<Choreographer> mockedChoreographer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockedLog = mockStatic(Log.class);
        mockedLog.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
        mockedLog.when(() -> Log.i(anyString(), anyString())).thenReturn(0);
        mockedLog.when(() -> Log.w(anyString(), anyString())).thenReturn(0);
        mockedLog.when(() -> Log.e(anyString(), anyString())).thenReturn(0);

        mockedChoreographer = mockStatic(Choreographer.class);
        mockedChoreographer.when(Choreographer::getInstance).thenReturn(mockChoreographer);

        displayMetrics = new DisplayMetrics();
        // Set ydpi so that 1 mm = 10 pixels (since mmToPx does: mm * ydpi / 25.4f)
        // 25.4f * 10 = 254f
        displayMetrics.ydpi = 254f;

        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getDisplayMetrics()).thenReturn(displayMetrics);

        gestureManager = new PinchGestureManager(mockContext, mockOverlayView, mockListener);
        gestureManager.setWebView(mockWebView);
    }

    @After
    public void tearDown() {
        mockedLog.close();
        mockedChoreographer.close();
    }

    @Test
    public void testOnBZoneDown() {
        gestureManager.onBZoneDown(100f);
        assertTrue(gestureManager.isGestureActive());
        verify(mockListener).onBZoneGestureStarted();
    }

    @Test
    public void testOnBZoneMove_DeadZone() {
        // Init gesture down at Y = 100f
        gestureManager.onBZoneDown(100f);
        reset(mockListener);

        // Move to Y = 110f (since ydpi=254, 1mm = 10px, so 10px = 1mm, which is within DEAD_ZONE_MM = 2.0mm)
        gestureManager.onBZoneMove(110f);
        verify(mockListener).onBZoneScrollProgress(0f);
        verify(mockOverlayView).setScrollIndicator(0f, false);
    }

    @Test
    public void testOnBZoneMove_StepScroll() {
        gestureManager.onBZoneDown(100f);
        reset(mockListener);
        reset(mockWebView);

        // Move to Y = 130f (30px delta = 3mm, which is between 2mm and 4mm)
        gestureManager.onBZoneMove(130f);

        verify(mockWebView).scrollBy(0, -300); // 30mm * 10 = 300px

        ArgumentCaptor<Float> captor = ArgumentCaptor.forClass(Float.class);
        verify(mockListener).onBZoneScrollProgress(captor.capture());
        // Compute expected eased progress dynamically to match easing in implementation
        float absdy = 30f; // 30px in test
        float stepMinPx = PinchZoneConfig.mmToPx(mockContext, PinchZoneConfig.STEP_SCROLL_MIN_MM);
        float stepMaxPx = PinchZoneConfig.mmToPx(mockContext, PinchZoneConfig.STEP_SCROLL_MAX_MM);
        float rawProgress = (absdy - stepMinPx) / (stepMaxPx - stepMinPx) * 0.33f;
        float expected = (float) Math.pow(Math.max(0f, Math.min(1f, rawProgress)), 0.92);
        assertEquals(expected, captor.getValue(), 0.001f);
    }

    @Test
    public void testOnBZoneUp() {
        gestureManager.onBZoneDown(100f);
        reset(mockListener);

        gestureManager.onBZoneUp();
        assertFalse(gestureManager.isGestureActive());
        verify(mockListener).onBZoneGestureEnded();
    }
}
