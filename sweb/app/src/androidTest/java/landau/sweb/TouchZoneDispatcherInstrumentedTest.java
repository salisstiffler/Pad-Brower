package landau.sweb;

import android.content.Context;
import android.webkit.WebView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Basic instrumentation test to verify that TouchZoneDispatcher can be instantiated
 * and that initial state transitions are stable. This is a smoke test and does not
 * attempt to simulate complex multi-pointer gestures (which should be covered in
 * dedicated device lab tests).
 */
@RunWith(AndroidJUnit4.class)
public class TouchZoneDispatcherInstrumentedTest {

    private Context appContext;

    @Before
    public void setup() {
        appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void instantiateDispatcher_noCrash() {
        PinchOverlayView overlay = new PinchOverlayView(appContext);
        PinchGestureManager gm = new PinchGestureManager(appContext, overlay, new PinchGestureManager.Listener() {
            @Override public void onBZoneGestureStarted() {}
            @Override public void onBZoneGestureEnded() {}
            @Override public void onBZoneScrollProgress(float progress) {}
        });

        TouchZoneDispatcher dispatcher = new TouchZoneDispatcher(appContext, gm, overlay);
        assertNotNull(dispatcher);

        WebView webView = new WebView(appContext);
        dispatcher.setWebView(webView);
        // Initial state should be inactive
        assertFalse(dispatcher.isBZoneActive());
    }
}
