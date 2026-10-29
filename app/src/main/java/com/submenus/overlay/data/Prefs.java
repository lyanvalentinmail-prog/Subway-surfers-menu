package com.submenus.overlay.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.submenus.overlay.bridge.BridgeContract;
import com.submenus.overlay.bridge.MenuLimits;

/**
 * Single local storage for the whole app (SharedPreferences).
 * Everything the user toggles survives reboots and app restarts.
 */
public final class Prefs {

    private static final String FILE_NAME = "subway_menu_prefs_v1";

    // Gameplay
    public static final String KEY_SPEED_ENABLED = "speed_enabled";
    public static final String KEY_SPEED_MULT = "speed_multiplier";
    public static final String KEY_HIGH_JUMP_ENABLED = "high_jump_enabled";
    public static final String KEY_HIGH_JUMP_MULT = "high_jump_multiplier";
    public static final String KEY_INVINCIBILITY = "invincibility_enabled";
    public static final String KEY_COIN_ENABLED = "coin_multiplier_enabled";
    public static final String KEY_COIN_MULT = "coin_multiplier";
    public static final String KEY_KEYS_COUNTER = "keys_counter_enabled";

    // Menu look & feel
    public static final String KEY_HAPTICS = "ui_haptics";
    public static final String KEY_ANIMATIONS = "ui_animations";
    public static final String KEY_SNAP_TO_EDGE = "ui_snap_to_edge";
    public static final String KEY_OPACITY = "ui_opacity";
    public static final String KEY_BUTTON_SIZE = "ui_button_size_dp";

    // Runtime state
    public static final String KEY_BUTTON_X = "btn_x";
    public static final String KEY_BUTTON_Y = "btn_y";
    public static final String KEY_EMBED_X = "embed_btn_x";
    public static final String KEY_EMBED_Y = "embed_btn_y";
    public static final String KEY_MENU_ENABLED = "menu_enabled";
    public static final String KEY_SNAPSHOT_JSON = "published_snapshot_json";
    public static final String KEY_LAST_UPDATE = "published_updated_at";
    public static final String KEY_LAST_HEARTBEAT = "integration_last_heartbeat";
    public static final String KEY_CLIENT_LABEL = "integration_client_label";
    public static final String KEY_CLIENT_PACKAGE = "integration_client_package";
    public static final String KEY_REPORTED_KEYS = "integration_reported_keys";
    public static final String KEY_REPORTED_COINS = "integration_reported_coins";

    private static volatile Prefs instance;

    private final SharedPreferences sp;

    private Prefs(SharedPreferences sp) {
        this.sp = sp;
    }

    public static Prefs get(Context context) {
        Prefs local = instance;
        if (local == null) {
            synchronized (Prefs.class) {
                local = instance;
                if (local == null) {
                    local = new Prefs(context.getApplicationContext()
                            .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE));
                    instance = local;
                }
            }
        }
        return local;
    }

    public SharedPreferences raw() {
        return sp;
    }

    // ---------------------------------------------------------------- gameplay

    public boolean isSpeedEnabled() {
        return sp.getBoolean(KEY_SPEED_ENABLED, false);
    }

    public void setSpeedEnabled(boolean value) {
        sp.edit().putBoolean(KEY_SPEED_ENABLED, value).apply();
    }

    public float getSpeedMultiplier() {
        return MenuLimits.clamp(sp.getFloat(KEY_SPEED_MULT, MenuLimits.SPEED_DEFAULT),
                MenuLimits.SPEED_MIN, MenuLimits.SPEED_MAX);
    }

    public void setSpeedMultiplier(float value) {
        sp.edit().putFloat(KEY_SPEED_MULT,
                MenuLimits.snapToStep(value, MenuLimits.SPEED_MIN, MenuLimits.SPEED_MAX, MenuLimits.SPEED_STEP)).apply();
    }

    public boolean isHighJumpEnabled() {
        return sp.getBoolean(KEY_HIGH_JUMP_ENABLED, false);
    }

    public void setHighJumpEnabled(boolean value) {
        sp.edit().putBoolean(KEY_HIGH_JUMP_ENABLED, value).apply();
    }

    public float getHighJumpMultiplier() {
        return MenuLimits.clamp(sp.getFloat(KEY_HIGH_JUMP_MULT, MenuLimits.HIGH_JUMP_DEFAULT),
                MenuLimits.HIGH_JUMP_MIN, MenuLimits.HIGH_JUMP_MAX);
    }

    public void setHighJumpMultiplier(float value) {
        sp.edit().putFloat(KEY_HIGH_JUMP_MULT,
                MenuLimits.snapToStep(value, MenuLimits.HIGH_JUMP_MIN, MenuLimits.HIGH_JUMP_MAX, MenuLimits.HIGH_JUMP_STEP)).apply();
    }

    public boolean isInvincibilityEnabled() {
        return sp.getBoolean(KEY_INVINCIBILITY, false);
    }

    public void setInvincibilityEnabled(boolean value) {
        sp.edit().putBoolean(KEY_INVINCIBILITY, value).apply();
    }

    public boolean isCoinMultiplierEnabled() {
        return sp.getBoolean(KEY_COIN_ENABLED, false);
    }

    public void setCoinMultiplierEnabled(boolean value) {
        sp.edit().putBoolean(KEY_COIN_ENABLED, value).apply();
    }

    public int getCoinMultiplier() {
        return MenuLimits.clamp(sp.getInt(KEY_COIN_MULT, MenuLimits.COIN_DEFAULT),
                MenuLimits.COIN_MIN, MenuLimits.COIN_MAX);
    }

    public void setCoinMultiplier(int value) {
        sp.edit().putInt(KEY_COIN_MULT,
                MenuLimits.clamp(value, MenuLimits.COIN_MIN, MenuLimits.COIN_MAX)).apply();
    }

    public boolean isKeysCounterEnabled() {
        return sp.getBoolean(KEY_KEYS_COUNTER, false);
    }

    public void setKeysCounterEnabled(boolean value) {
        sp.edit().putBoolean(KEY_KEYS_COUNTER, value).apply();
    }

    // --------------------------------------------------------------- appearance

    public boolean isHapticsEnabled() {
        return sp.getBoolean(KEY_HAPTICS, true);
    }

    public void setHapticsEnabled(boolean value) {
        sp.edit().putBoolean(KEY_HAPTICS, value).apply();
    }

    public boolean isAnimationsEnabled() {
        return sp.getBoolean(KEY_ANIMATIONS, true);
    }

    public void setAnimationsEnabled(boolean value) {
        sp.edit().putBoolean(KEY_ANIMATIONS, value).apply();
    }

    public boolean isSnapToEdgeEnabled() {
        return sp.getBoolean(KEY_SNAP_TO_EDGE, true);
    }

    public void setSnapToEdgeEnabled(boolean value) {
        sp.edit().putBoolean(KEY_SNAP_TO_EDGE, value).apply();
    }

    public float getOpacity() {
        return MenuLimits.clamp(sp.getFloat(KEY_OPACITY, MenuLimits.OPACITY_DEFAULT),
                MenuLimits.OPACITY_MIN, MenuLimits.OPACITY_MAX);
    }

    public void setOpacity(float value) {
        sp.edit().putFloat(KEY_OPACITY,
                MenuLimits.clamp(value, MenuLimits.OPACITY_MIN, MenuLimits.OPACITY_MAX)).apply();
    }

    public int getButtonSizeDp() {
        return MenuLimits.clamp(sp.getInt(KEY_BUTTON_SIZE, MenuLimits.BUTTON_SIZE_DEFAULT_DP),
                MenuLimits.BUTTON_SIZE_MIN_DP, MenuLimits.BUTTON_SIZE_MAX_DP);
    }

    public void setButtonSizeDp(int value) {
        sp.edit().putInt(KEY_BUTTON_SIZE,
                MenuLimits.clamp(value, MenuLimits.BUTTON_SIZE_MIN_DP, MenuLimits.BUTTON_SIZE_MAX_DP)).apply();
    }

    // ------------------------------------------------------------------- runtime

    public int getButtonX() {
        return sp.getInt(KEY_BUTTON_X, -1);
    }

    public int getButtonY() {
        return sp.getInt(KEY_BUTTON_Y, -1);
    }

    public void setButtonPosition(int x, int y) {
        sp.edit().putInt(KEY_BUTTON_X, x).putInt(KEY_BUTTON_Y, y).apply();
    }

    public int getEmbeddedButtonX() {
        return sp.getInt(KEY_EMBED_X, -1);
    }

    public int getEmbeddedButtonY() {
        return sp.getInt(KEY_EMBED_Y, -1);
    }

    public void setEmbeddedButtonPosition(int x, int y) {
        sp.edit().putInt(KEY_EMBED_X, x).putInt(KEY_EMBED_Y, y).apply();
    }

    public boolean isMenuEnabled() {
        return sp.getBoolean(KEY_MENU_ENABLED, false);
    }

    public void setMenuEnabled(boolean value) {
        sp.edit().putBoolean(KEY_MENU_ENABLED, value).apply();
    }

    public String getSnapshotJson() {
        return sp.getString(KEY_SNAPSHOT_JSON, null);
    }

    public void setSnapshotJson(String json) {
        sp.edit().putString(KEY_SNAPSHOT_JSON, json).apply();
    }

    public long getLastUpdate() {
        return sp.getLong(KEY_LAST_UPDATE, 0L);
    }

    public void setLastUpdate(long value) {
        sp.edit().putLong(KEY_LAST_UPDATE, value).apply();
    }

    public long getLastHeartbeat() {
        return sp.getLong(KEY_LAST_HEARTBEAT, 0L);
    }

    public void setLastHeartbeat(long value) {
        sp.edit().putLong(KEY_LAST_HEARTBEAT, value).apply();
    }

    public String getClientLabel() {
        return sp.getString(KEY_CLIENT_LABEL, "");
    }

    public String getClientPackage() {
        return sp.getString(KEY_CLIENT_PACKAGE, "");
    }

    public void setClient(String label, String packageName) {
        sp.edit()
                .putString(KEY_CLIENT_LABEL, label == null ? "" : label)
                .putString(KEY_CLIENT_PACKAGE, packageName == null ? "" : packageName)
                .apply();
    }

    /** Keys/coins count reported by the integration, or -1 when nothing was reported. */
    public int getReportedKeys() {
        return sp.getInt(KEY_REPORTED_KEYS, -1);
    }

    public int getReportedCoins() {
        return sp.getInt(KEY_REPORTED_COINS, -1);
    }

    /** True when an integration reported a heartbeat within the timeout window. */
    public boolean isIntegrationConnected() {
        long last = getLastHeartbeat();
        return last > 0L && (System.currentTimeMillis() - last) < BridgeContract.HEARTBEAT_TIMEOUT_MS;
    }

    /** Clears every value, including the stored snapshot. */
    public void resetAll() {
        sp.edit().clear().apply();
    }
}
