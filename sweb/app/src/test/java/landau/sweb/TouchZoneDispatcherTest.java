package landau.sweb;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.WebView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TouchZoneDispatcherTest {

    @Mock
    private Context mockContext;
    @Mock
    private Resources mockResources;
    private DisplayMetrics displayMetrics;

    @Mock
    private PinchGestureManager mockGestureManager;
    @Mock
    private PinchOverlayView mockOverlayView;
    @Mock
    private WebView mockWebView;

    private TouchZoneDispatcher dispatcher;
    private MockedStatic<Log> mockedLog;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        mockedLog = mockStatic(Log.class);
        mockedLog.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
        mockedLog.when(() -> Log.i(anyString(), anyString())).thenReturn(0);
        mockedLog.when(() -> Log.w(anyString(), anyString())).thenReturn(0);
        mockedLog.when(() -> Log.e(anyString(), anyString())).thenReturn(0);

        displayMetrics = new DisplayMetrics();
        // Set ydpi so that 1 mm = 10 pixels (since mmToPx does: mm * ydpi / 25.4f)
        // 25.4f * 10 = 254f
        displayMetrics.ydpi = 254f;

        when(mockContext.getResources()).thenReturn(mockResources);
        when(mockResources.getDisplayMetrics()).thenReturn(displayMetrics);

        dispatcher = new TouchZoneDispatcher(mockContext, mockGestureManager, mockOverlayView);
        dispatcher.setWebView(mockWebView);
    }

    @After
    public void tearDown() {
        mockedLog.close();
    }

    @Test
    public void testBZoneInactiveByDefault() {
        assertFalse(dispatcher.isBZoneActive());
    }

    @Test
    public void testDispatchTouchEvent_BypassMode() {
        MotionEvent mockEvent = mock(MotionEvent.class);
        when(mockEvent.getActionMasked()).thenReturn(MotionEvent.ACTION_DOWN);
        when(mockEvent.getActionIndex()).thenReturn(0);
        when(mockEvent.getPointerId(0)).thenReturn(0);

        // Coordinates in A-zone (x = 100, far from edge strip width of 60px)
        when(mockEvent.getX(0)).thenReturn(100f);
        when(mockEvent.getY(0)).thenReturn(100f);
        when(mockWebView.getWidth()).thenReturn(1000);

        boolean consumed = dispatcher.dispatchTouchEvent(mockEvent);
        assertFalse(consumed);
        assertFalse(dispatcher.isBZoneActive());
    }
}
