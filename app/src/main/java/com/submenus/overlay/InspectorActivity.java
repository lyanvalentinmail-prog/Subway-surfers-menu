package com.submenus.overlay;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.submenus.overlay.bridge.BridgeContract;
import com.submenus.overlay.bridge.ModMenuBridge;
import com.submenus.overlay.data.MenuRepository;
import com.submenus.overlay.ui.Theme;

/** Shows the exact JSON an integration receives, plus the live heartbeat status. */
public class InspectorActivity extends Activity {

    private MenuRepository repository;
    private TextView availabilityValue;
    private TextView heartbeatValue;
    private TextView jsonValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspector);

        repository = MenuRepository.get(this);
        ((TextView) findViewById(R.id.uri_value)).setText(BridgeContract.URI_SETTINGS);
        availabilityValue = findViewById(R.id.availability_value);
        heartbeatValue = findViewById(R.id.heartbeat_value);
        jsonValue = findViewById(R.id.json_value);

        findViewById(R.id.btn_copy).setOnClickListener(v -> copyJson());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean available = ModMenuBridge.isAvailable(this);
        availabilityValue.setText(available
                ? getString(R.string.inspector_available)
                : getString(R.string.inspector_unavailable));
        availabilityValue.setTextColor(available ? Theme.GREEN_LIGHT : Theme.ORANGE);

        long age = ModMenuBridge.lastHeartbeatAge(this);
        if (age < 0) {
            heartbeatValue.setText(getString(R.string.inspector_heartbeat) + ": "
                    + getString(R.string.inspector_none));
        } else {
            long seconds = age / 1000L;
            String relative = seconds < 60
                    ? getString(R.string.inspector_seconds_ago, seconds)
                    : String.format(java.util.Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
            String label = repository.prefs().getClientLabel();
            String suffix = label == null || label.isEmpty() ? "" : " · " + label;
            heartbeatValue.setText(getString(R.string.inspector_heartbeat) + ": " + relative + suffix);
        }

        jsonValue.setText(ModMenuBridge.read(this).toPrettyJson());
    }

    private void copyJson() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("mod-menu-snapshot",
                    ModMenuBridge.read(this).toPrettyJson()));
            Toast.makeText(this, R.string.inspector_copied, Toast.LENGTH_SHORT).show();
        }
    }
}
