package landau.sweb;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    private EditText edgeWidthEt, activateDelayEt, pulseMsEt;
    private Switch hapticSwitch;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        edgeWidthEt = findViewById(R.id.edge_width);
        activateDelayEt = findViewById(R.id.activate_delay);
        pulseMsEt = findViewById(R.id.pulse_ms);
        hapticSwitch = findViewById(R.id.haptic_switch);

        float edge = prefs.getFloat("pinch_edge_width", 6.0f);
        long delay = prefs.getLong("pinch_activate_delay", 500L);
        int pulse = prefs.getInt("pinch_pulse_ms", (int) PinchZoneConfig.B_ZONE_PULSE_MS);
        boolean haptic = prefs.getBoolean("pinch_haptic", true);

        edgeWidthEt.setText(String.valueOf(edge));
        activateDelayEt.setText(String.valueOf(delay));
        pulseMsEt.setText(String.valueOf(pulse));
        hapticSwitch.setChecked(haptic);

        Button save = findViewById(R.id.save_btn);
        save.setOnClickListener(v -> {
            try {
                float e = Float.parseFloat(edgeWidthEt.getText().toString());
                long d = Long.parseLong(activateDelayEt.getText().toString());
                int p = Integer.parseInt(pulseMsEt.getText().toString());
                boolean h = hapticSwitch.isChecked();
                prefs.edit()
                        .putFloat("pinch_edge_width", e)
                        .putLong("pinch_activate_delay", d)
                        .putInt("pinch_pulse_ms", p)
                        .putBoolean("pinch_haptic", h)
                        .apply();
                // update runtime config
                PinchZoneConfig.EDGE_STRIP_MM = e;
                PinchZoneConfig.B_ZONE_ACTIVATE_DELAY_MS = d;
                // Pulse & haptic stored in prefs; PinchOverlayView reads pulse ms on init
                Toast.makeText(this, "Pinch settings saved", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception ex) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
