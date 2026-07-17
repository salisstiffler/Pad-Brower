Grip SDK Integration

This document describes how to integrate the Pad-Brower grip (pinch-hold) features into a host Android app.

Quickstart

1. Add the `sweb` module to your project and include the built AAR or module dependency.
2. From your Activity (after setContentView), call:

```java
FrameLayout container = findViewById(R.id.webviews); // host container for WebView
WebView webView = ...; // your active WebView
GripSdk.init(this, container, webView);
// Optionally switch active webview later:
GripSdk.setWebView(newWebView);
```

API

- GripSdk.init(Activity activity, FrameLayout containerView, WebView webView)
  - Initializes overlay, dispatcher and gesture manager.
- GripSdk.setWebView(WebView)
  - Switches the active WebView the dispatcher will scroll.
- GripSdk.enable()/GripSdk.disable()
  - Show/hide overlay UI.
- GripSdk.getDispatcher()/getOverlayView()
  - Advanced: access internal modules for custom integrations.

Notes

- The SDK requires the host Activity to use a full-screen FrameLayout container where the overlay can be added on top of WebView.
- The overlay is non-intercepting; touch event arbitration is performed by TouchZoneDispatcher and the host container should forward dispatchTouchEvent to it (PinchWebViewContainer is provided).
- Calibration and configuration values are in PinchZoneConfig and can be tuned via SharedPreferences (see MainActivity for example keys).

If you want the integration exported as an AAR or packaged module, tell me and I'll add Gradle packaging files and a sample integration app.