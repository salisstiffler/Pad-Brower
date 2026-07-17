Chromium Integration Notes

Purpose

Provide a clear integration plan for feeding preprocessed touch events from the Grip preprocessor
into the Chromium-based rendering/input pipeline used by the app's WebView or a native Chromium build.

Current status

- ChromiumEventAdapter.java added as an abstraction layer. It currently forwards events to
  WebView.dispatchTouchEvent but centralizes the integration point for future replacement.

Integration strategies

1) WebView forwarding (current)
   - Simple: adapter.injectTouchEvent(event) forwards to webView.dispatchTouchEvent(event).
   - Low engineering cost, higher latency, uses standard Android path.

2) Chromium embedders / native injection (future)
   - Use JNI to call into Chromium's input thread or InputInjector to deliver events with preserved
     timestamps and pointer ids. This requires building Chromium upstream with a small bridge and
     shipping a native library (increases build complexity).
   - Benefits: lower latency, precise control over gesture synthesis, avoid WebView-level gesture
     synthesis conflicts.

3) Event batching and timestamp preservation
   - Batch preprocessed events and inject them in order with preserved eventTime/downTime.
   - Ensure adapter exposes an API for batched injection.

Recommended next steps

- Keep ChromiumEventAdapter as the single integration location.
- Implement a native JNI path when moving from prototype to production: build a thin JNI layer that
  receives MotionEvent-like structs and calls Chromium's InputInjector on the compositor/input thread.
- Add tests that verify ordering, timestamps, and that no synthetic long-presses are triggered after injection.

Files added

- sweb/app/src/main/java/landau/sweb/ChromiumEventAdapter.java
- docs/CHROMIUM_INTEGRATION.md

Notes on testing

- Instrumented tests should assert that adapter.injectTouchEvent returns true and that the WebView (or
  Chromium) responds as expected in smoke scenarios. Device lab testing is recommended for latency and
  reliability measurements.
