package com.submenus.overlay;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.submenus.overlay.bridge.BridgeContract;
import com.submenus.overlay.bridge.MenuLimits;
import com.submenus.overlay.bridge.MenuSnapshot;
import com.submenus.overlay.data.MenuRepository;
import com.submenus.overlay.ui.SliderRowView;
import com.submenus.overlay.ui.SwitchRowView;
import com.submenus.overlay.ui.Theme;

/** Launcher screen: permissions, service control, quick options and appearance. */
public class MainActivity extends Activity implements MenuRepository.Listener {

    private static final int REQ_OVERLAY = 1201;
    private static final int REQ_NOTIFICATIONS = 1202;

    private MenuRepository repository;

    private LinearLayout statusContainer;
    private LinearLayout optionsContainer;
    private LinearLayout prefsContainer;
    private TextView toggleButton;
    private TextView bridgeUri;

    private TextView permissionValue;
    private TextView serviceValue;
    private TextView bridgeValue;
    private View permissionDot;
    private View serviceDot;
    private View bridgeDot;

    private SwitchRowView speedSwitch;
    private SliderRowView speedSlider;
    private SwitchRowView jumpSwitch;
    private SliderRowView jumpSlider;
    private SwitchRowView invincibilitySwitch;
    private SwitchRowView coinSwitch;
    private SliderRowView coinSlider;
    private SwitchRowView keysSwitch;

    private SwitchRowView hapticsSwitch;
    private SwitchRowView animationsSwitch;
    private SwitchRowView snapSwitch;
    private SliderRowView opacitySlider;
    private SliderRowView sizeSlider;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable statusPoll = new Runnable() {
        @Override
        public void run() {
            refreshStatus();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = MenuRepository.get(this);

        statusContainer = findViewById(R.id.status_container);
        optionsContainer = findViewById(R.id.options_container);
        prefsContainer = findViewById(R.id.prefs_container);
        toggleButton = findViewById(R.id.btn_toggle_service);
        bridgeUri = findViewById(R.id.bridge_uri);

        bridgeUri.setText(BridgeContract.URI_SETTINGS);

        buildStatusRows();
        buildOptionRows();
        buildPreferenceRows();

        toggleButton.setOnClickListener(v -> onToggleServiceClicked());
        findViewById(R.id.btn_inspector).setOnClickListener(v ->
                startActivity(new Intent(this, InspectorActivity.class)));
        findViewById(R.id.btn_reset).setOnClickListener(v -> confirmReset());

        // Make sure the provider always has a snapshot to serve.
        if (repository.prefs().getSnapshotJson() == null) {
            repository.publish();
        }

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
    }

    // ------------------------------------------------------------------- rows

    private void buildStatusRows() {
        View permissionRow = createStatusRow(getString(R.string.permission_label));
        permissionDot = (View) permissionRow.getTag(R.id.tag_dot);
        permissionValue = (TextView) permissionRow.getTag(R.id.tag_value);
        permissionRow.setOnClickListener(v -> requestOverlayPermission());
        statusContainer.addView(permissionRow);

        View serviceRow = createStatusRow(getString(R.string.service_label));
        serviceDot = (View) serviceRow.getTag(R.id.tag_dot);
        serviceValue = (TextView) serviceRow.getTag(R.id.tag_value);
        statusContainer.addView(serviceRow);

        View bridgeRow = createStatusRow(getString(R.string.bridge_label));
        bridgeDot = (View) bridgeRow.getTag(R.id.tag_dot);
        bridgeValue = (TextView) bridgeRow.getTag(R.id.tag_value);
        bridgeRow.setOnClickListener(v ->
                startActivity(new Intent(this, InspectorActivity.class)));
        statusContainer.addView(bridgeRow);
    }

    private View createStatusRow(String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padV = Theme.dp(this, 9f);
        row.setPadding(0, padV, 0, padV);

        View dot = new View(this);
        int dotSize = Theme.dp(this, 8f);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
        dotParams.setMarginEnd(Theme.dp(this, 10f));
        row.addView(dot, dotParams);

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(13f);
        labelView.setTextColor(Theme.TEXT_DIM);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(labelView, labelParams);

        TextView valueView = new TextView(this);
        valueView.setTextSize(13f);
        valueView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        valueView.setTextColor(Theme.TEXT);
        row.addView(valueView);

        row.setTag(R.id.tag_dot, dot);
        row.setTag(R.id.tag_value, valueView);
        return row;
    }

    private void buildOptionRows() {
        speedSwitch = addSwitchRow(optionsContainer, R.string.opt_speed, R.string.opt_speed_sub,
                repository.isSpeedEnabled(), checked -> repository.setSpeedEnabled(checked));
        speedSlider = addSliderRow(optionsContainer, getString(R.string.opt_speed),
                MenuLimits.SPEED_MIN, MenuLimits.SPEED_MAX, MenuLimits.SPEED_STEP,
                false, "%.2f\u00d7", repository.getSpeed(),
                value -> repository.setSpeedMultiplier(value));

        jumpSwitch = addSwitchRow(optionsContainer, R.string.opt_high_jump, R.string.opt_high_jump_sub,
                repository.isHighJumpEnabled(), checked -> repository.setHighJumpEnabled(checked));
        jumpSlider = addSliderRow(optionsContainer, getString(R.string.opt_high_jump),
                MenuLimits.HIGH_JUMP_MIN, MenuLimits.HIGH_JUMP_MAX, MenuLimits.HIGH_JUMP_STEP,
                false, "%.2f\u00d7", repository.getHighJump(),
                value -> repository.setHighJumpMultiplier(value));

        invincibilitySwitch = addSwitchRow(optionsContainer, R.string.opt_invincibility,
                R.string.opt_invincibility_sub, repository.isInvincibilityEnabled(),
                checked -> repository.setInvincibilityEnabled(checked));

        coinSwitch = addSwitchRow(optionsContainer, R.string.opt_coin, R.string.opt_coin_sub,
                repository.isCoinMultiplierEnabled(), checked -> repository.setCoinMultiplierEnabled(checked));
        coinSlider = addSliderRow(optionsContainer, getString(R.string.opt_coin),
                MenuLimits.COIN_MIN, MenuLimits.COIN_MAX, 1f, true, "%d\u00d7",
                repository.getCoinMultiplier(),
                value -> repository.setCoinMultiplier(Math.round(value)));

        keysSwitch = addSwitchRow(optionsContainer, R.string.opt_keys, R.string.opt_keys_sub,
                repository.isKeysCounterEnabled(), checked -> repository.setKeysCounterEnabled(checked));
    }

    private void buildPreferenceRows() {
        hapticsSwitch = addSwitchRow(prefsContainer, R.string.pref_haptics, R.string.pref_haptics_sub,
                repository.prefs().isHapticsEnabled(), checked -> repository.setHapticsEnabled(checked));
        animationsSwitch = addSwitchRow(prefsContainer, R.string.pref_animations,
                R.string.pref_animations_sub, repository.prefs().isAnimationsEnabled(),
                checked -> repository.setAnimationsEnabled(checked));
        snapSwitch = addSwitchRow(prefsContainer, R.string.pref_snap, R.string.pref_snap_sub,
                repository.prefs().isSnapToEdgeEnabled(),
                checked -> repository.setSnapToEdgeEnabled(checked));

        opacitySlider = addSliderRow(prefsContainer, getString(R.string.pref_opacity),
                40f, 100f, 5f, true, "%d%%",
                Math.round(repository.prefs().getOpacity() * 100f),
                value -> repository.setOpacity(value / 100f));

        sizeSlider = addSliderRow(prefsContainer, getString(R.string.pref_size),
                MenuLimits.BUTTON_SIZE_MIN_DP, MenuLimits.BUTTON_SIZE_MAX_DP, 4f, true, "%d dp",
                repository.prefs().getButtonSizeDp(),
                value -> repository.setButtonSizeDp(Math.round(value)));
    }

    private interface OnSwitch {
        void onChecked(boolean checked);
    }

    private interface OnSlider {
        void onValue(float value);
    }

    private SwitchRowView addSwitchRow(LinearLayout parent, int titleRes, int subtitleRes,
                                       boolean checked, OnSwitch callback) {
        SwitchRowView row = new SwitchRowView(this, getString(titleRes), getString(subtitleRes), checked);
        row.setOnRowCheckedChange((r, isChecked) -> callback.onChecked(isChecked));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Theme.dp(this, 8f);
        parent.addView(row, params);
        return row;
    }

    private SliderRowView addSliderRow(LinearLayout parent, String title, float min, float max,
                                       float step, boolean integer, String format, float value,
                                       OnSlider callback) {
        SliderRowView row = new SliderRowView(this, title, min, max, step, integer, format);
        row.setValue(value);
        row.setOnValueChange((r, newValue, fromUser) -> {
            if (fromUser) {
                callback.onValue(newValue);
            }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Theme.dp(this, 8f);
        parent.addView(row, params);
        return row;
    }

    // ---------------------------------------------------------------- actions

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        if (Settings.canDrawOverlays(this)) {
            refreshStatus();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        try {
            startActivityForResult(intent, REQ_OVERLAY);
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_no_overlay_support, Toast.LENGTH_LONG).show();
        }
    }

    private void onToggleServiceClicked() {
        if (OverlayService.isRunning) {
            OverlayService.stop(this);
            repository.prefs().setMenuEnabled(false);
            Toast.makeText(this, R.string.toast_service_stopped, Toast.LENGTH_SHORT).show();
            handler.postDelayed(this::refreshStatus, 250L);
            return;
        }
        if (!OverlayService.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.toast_permission_needed, Toast.LENGTH_LONG).show();
            requestOverlayPermission();
            return;
        }
        OverlayService.start(this);
        Toast.makeText(this, R.string.toast_service_started, Toast.LENGTH_SHORT).show();
        handler.postDelayed(this::refreshStatus, 400L);
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_title)
                .setMessage(R.string.reset_confirm)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
                    repository.resetEverything();
                    applyValuesToRows();
                    Toast.makeText(this, R.string.reset_done, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // -------------------------------------------------------------- lifecycle

    @Override
    protected void onResume() {
        super.onResume();
        repository.addListener(this);
        refreshStatus();
        applyValuesToRows();
        handler.removeCallbacks(statusPoll);
        handler.post(statusPoll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        repository.removeListener(this);
        handler.removeCallbacks(statusPoll);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OVERLAY) {
            refreshStatus();
            if (OverlayService.canDrawOverlays(this) && repository.prefs().isMenuEnabled()
                    && !OverlayService.isRunning) {
                OverlayService.start(this);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Nothing to do: the service keeps running even without notification permission.
    }

    private void refreshStatus() {
        boolean overlayGranted = OverlayService.canDrawOverlays(this);
        boolean running = OverlayService.isRunning;
        boolean connected = repository.prefs().isIntegrationConnected();

        setDot(permissionDot, overlayGranted ? Theme.GREEN : Theme.ORANGE);
        permissionValue.setText(overlayGranted ? R.string.permission_granted : R.string.permission_missing);
        permissionValue.setTextColor(overlayGranted ? Theme.GREEN_LIGHT : Theme.ORANGE);

        setDot(serviceDot, running ? Theme.GREEN : Theme.TEXT_MUTED);
        serviceValue.setText(running ? R.string.service_running : R.string.service_stopped);
        serviceValue.setTextColor(running ? Theme.GREEN_LIGHT : Theme.TEXT_DIM);

        setDot(bridgeDot, connected ? Theme.BLUE : Theme.TEXT_MUTED);
        if (connected) {
            String label = repository.prefs().getClientLabel();
            if (label == null || label.isEmpty()) {
                label = repository.prefs().getClientPackage();
            }
            if (label == null || label.isEmpty()) {
                label = "—";
            }
            bridgeValue.setText(getString(R.string.bridge_connected, label));
            bridgeValue.setTextColor(Theme.BLUE);
        } else {
            bridgeValue.setText(R.string.bridge_disconnected);
            bridgeValue.setTextColor(Theme.TEXT_DIM);
        }

        toggleButton.setText(running ? R.string.btn_stop : R.string.btn_start);
        toggleButton.setBackgroundResource(running
                ? R.drawable.bg_button_ghost : R.drawable.bg_button_gradient);
        toggleButton.setTextColor(running ? Theme.TEXT : Theme.BG_DEEP);
    }

    private void setDot(View dot, int color) {
        if (dot == null) {
            return;
        }
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bg.setColor(color);
        bg.setStroke(Theme.dp(this, 1f), Theme.withAlpha(color, 0.35f));
        dot.setBackground(bg);
    }

    private void applyValuesToRows() {
        if (speedSwitch != null) speedSwitch.setChecked(repository.isSpeedEnabled());
        if (speedSlider != null) speedSlider.setValue(repository.getSpeed());
        if (jumpSwitch != null) jumpSwitch.setChecked(repository.isHighJumpEnabled());
        if (jumpSlider != null) jumpSlider.setValue(repository.getHighJump());
        if (invincibilitySwitch != null) invincibilitySwitch.setChecked(repository.isInvincibilityEnabled());
        if (coinSwitch != null) coinSwitch.setChecked(repository.isCoinMultiplierEnabled());
        if (coinSlider != null) coinSlider.setValue(repository.getCoinMultiplier());
        if (keysSwitch != null) keysSwitch.setChecked(repository.isKeysCounterEnabled());
        if (hapticsSwitch != null) hapticsSwitch.setChecked(repository.prefs().isHapticsEnabled());
        if (animationsSwitch != null) animationsSwitch.setChecked(repository.prefs().isAnimationsEnabled());
        if (snapSwitch != null) snapSwitch.setChecked(repository.prefs().isSnapToEdgeEnabled());
        if (opacitySlider != null) opacitySlider.setValue(Math.round(repository.prefs().getOpacity() * 100f));
        if (sizeSlider != null) sizeSlider.setValue(repository.prefs().getButtonSizeDp());
    }

    @Override
    public void onSnapshotChanged(MenuSnapshot snapshot) {
        refreshStatus();
        applyValuesToRows();
    }
}
