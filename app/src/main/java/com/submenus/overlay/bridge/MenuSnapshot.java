package com.submenus.overlay.bridge;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Immutable snapshot of everything the floating menu publishes.
 *
 * <p>This class (together with {@link BridgeContract}, {@link MenuLimits},
 * {@link ModMenuBridge} and {@link ModMenuListener}) is the whole integration
 * surface: it has zero dependencies on the rest of the app, so the same files can
 * be dropped into a game-side integration module and compiled there.</p>
 */
public final class MenuSnapshot {

    public final int schema;
    public final long updatedAt;

    // Player
    public final boolean speedEnabled;
    public final float speedMultiplier;
    public final boolean highJumpEnabled;
    public final float highJumpMultiplier;
    public final boolean invincibilityEnabled;

    // Collectibles
    public final boolean coinMultiplierEnabled;
    public final int coinMultiplier;
    public final boolean keysCounterEnabled;

    // Menu UI (published so an integration can log/verify what the user sees)
    public final boolean hapticsEnabled;
    public final boolean animationsEnabled;
    public final boolean snapToEdge;
    public final float overlayOpacity;
    public final int buttonSizeDp;
    public final String accent;

    private MenuSnapshot(Builder b) {
        this.schema = BridgeContract.SCHEMA_VERSION;
        this.updatedAt = b.updatedAt;
        this.speedEnabled = b.speedEnabled;
        this.speedMultiplier = MenuLimits.clamp(b.speedMultiplier, MenuLimits.SPEED_MIN, MenuLimits.SPEED_MAX);
        this.highJumpEnabled = b.highJumpEnabled;
        this.highJumpMultiplier = MenuLimits.clamp(b.highJumpMultiplier, MenuLimits.HIGH_JUMP_MIN, MenuLimits.HIGH_JUMP_MAX);
        this.invincibilityEnabled = b.invincibilityEnabled;
        this.coinMultiplierEnabled = b.coinMultiplierEnabled;
        this.coinMultiplier = MenuLimits.clamp(b.coinMultiplier, MenuLimits.COIN_MIN, MenuLimits.COIN_MAX);
        this.keysCounterEnabled = b.keysCounterEnabled;
        this.hapticsEnabled = b.hapticsEnabled;
        this.animationsEnabled = b.animationsEnabled;
        this.snapToEdge = b.snapToEdge;
        this.overlayOpacity = MenuLimits.clamp(b.overlayOpacity, MenuLimits.OPACITY_MIN, MenuLimits.OPACITY_MAX);
        this.buttonSizeDp = MenuLimits.clamp(b.buttonSizeDp, MenuLimits.BUTTON_SIZE_MIN_DP, MenuLimits.BUTTON_SIZE_MAX_DP);
        this.accent = b.accent == null ? "green" : b.accent;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Snapshot with every gameplay option disabled — the safe, neutral state. */
    public static MenuSnapshot defaults() {
        return builder().build();
    }

    /** True when at least one gameplay option is switched on. */
    public boolean hasActiveMods() {
        return speedEnabled || highJumpEnabled || invincibilityEnabled
                || coinMultiplierEnabled || keysCounterEnabled;
    }

    public JSONObject toJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("schema", schema);
            root.put("updatedAt", updatedAt);

            JSONObject player = new JSONObject();
            player.put("speedEnabled", speedEnabled);
            player.put("speedMultiplier", round(speedMultiplier));
            player.put("highJumpEnabled", highJumpEnabled);
            player.put("highJumpMultiplier", round(highJumpMultiplier));
            player.put("invincibilityEnabled", invincibilityEnabled);
            root.put("player", player);

            JSONObject collectibles = new JSONObject();
            collectibles.put("coinMultiplierEnabled", coinMultiplierEnabled);
            collectibles.put("coinMultiplier", coinMultiplier);
            collectibles.put("keysCounterEnabled", keysCounterEnabled);
            root.put("collectibles", collectibles);

            JSONObject ui = new JSONObject();
            ui.put("hapticsEnabled", hapticsEnabled);
            ui.put("animationsEnabled", animationsEnabled);
            ui.put("snapToEdge", snapToEdge);
            ui.put("overlayOpacity", round(overlayOpacity));
            ui.put("buttonSizeDp", buttonSizeDp);
            ui.put("accent", accent);
            root.put("ui", ui);
        } catch (JSONException e) {
            throw new IllegalStateException("Unable to serialize snapshot", e);
        }
        return root;
    }

    public String toJsonString() {
        return toJson().toString();
    }

    public String toPrettyJson() {
        try {
            return toJson().toString(2);
        } catch (JSONException e) {
            return toJsonString();
        }
    }

    public static MenuSnapshot fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return defaults();
        }
        try {
            return fromJson(new JSONObject(json));
        } catch (JSONException e) {
            return defaults();
        }
    }

    public static MenuSnapshot fromJson(JSONObject root) {
        Builder b = builder();
        if (root == null) {
            return b.build();
        }
        b.updatedAt(root.optLong("updatedAt", 0L));

        JSONObject player = root.optJSONObject("player");
        if (player != null) {
            b.speedEnabled(player.optBoolean("speedEnabled", false));
            b.speedMultiplier((float) player.optDouble("speedMultiplier", MenuLimits.SPEED_MIN));
            b.highJumpEnabled(player.optBoolean("highJumpEnabled", false));
            b.highJumpMultiplier((float) player.optDouble("highJumpMultiplier", MenuLimits.HIGH_JUMP_MIN));
            b.invincibilityEnabled(player.optBoolean("invincibilityEnabled", false));
        }

        JSONObject collectibles = root.optJSONObject("collectibles");
        if (collectibles != null) {
            b.coinMultiplierEnabled(collectibles.optBoolean("coinMultiplierEnabled", false));
            b.coinMultiplier(collectibles.optInt("coinMultiplier", MenuLimits.COIN_MIN));
            b.keysCounterEnabled(collectibles.optBoolean("keysCounterEnabled", false));
        }

        JSONObject ui = root.optJSONObject("ui");
        if (ui != null) {
            b.hapticsEnabled(ui.optBoolean("hapticsEnabled", true));
            b.animationsEnabled(ui.optBoolean("animationsEnabled", true));
            b.snapToEdge(ui.optBoolean("snapToEdge", true));
            b.overlayOpacity((float) ui.optDouble("overlayOpacity", MenuLimits.OPACITY_DEFAULT));
            b.buttonSizeDp(ui.optInt("buttonSizeDp", MenuLimits.BUTTON_SIZE_DEFAULT_DP));
            b.accent(ui.optString("accent", "green"));
        }
        return b.build();
    }

    private static double round(float value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    @Override
    public String toString() {
        return toJsonString();
    }

    /** Fluent builder used by the app and by tests. */
    public static final class Builder {
        private long updatedAt = System.currentTimeMillis();
        private boolean speedEnabled = false;
        private float speedMultiplier = MenuLimits.SPEED_MIN;
        private boolean highJumpEnabled = false;
        private float highJumpMultiplier = MenuLimits.HIGH_JUMP_MIN;
        private boolean invincibilityEnabled = false;
        private boolean coinMultiplierEnabled = false;
        private int coinMultiplier = MenuLimits.COIN_MIN;
        private boolean keysCounterEnabled = false;
        private boolean hapticsEnabled = true;
        private boolean animationsEnabled = true;
        private boolean snapToEdge = true;
        private float overlayOpacity = MenuLimits.OPACITY_DEFAULT;
        private int buttonSizeDp = MenuLimits.BUTTON_SIZE_DEFAULT_DP;
        private String accent = "green";

        public Builder updatedAt(long value) { this.updatedAt = value; return this; }
        public Builder speedEnabled(boolean value) { this.speedEnabled = value; return this; }
        public Builder speedMultiplier(float value) { this.speedMultiplier = value; return this; }
        public Builder highJumpEnabled(boolean value) { this.highJumpEnabled = value; return this; }
        public Builder highJumpMultiplier(float value) { this.highJumpMultiplier = value; return this; }
        public Builder invincibilityEnabled(boolean value) { this.invincibilityEnabled = value; return this; }
        public Builder coinMultiplierEnabled(boolean value) { this.coinMultiplierEnabled = value; return this; }
        public Builder coinMultiplier(int value) { this.coinMultiplier = value; return this; }
        public Builder keysCounterEnabled(boolean value) { this.keysCounterEnabled = value; return this; }
        public Builder hapticsEnabled(boolean value) { this.hapticsEnabled = value; return this; }
        public Builder animationsEnabled(boolean value) { this.animationsEnabled = value; return this; }
        public Builder snapToEdge(boolean value) { this.snapToEdge = value; return this; }
        public Builder overlayOpacity(float value) { this.overlayOpacity = value; return this; }
        public Builder buttonSizeDp(int value) { this.buttonSizeDp = value; return this; }
        public Builder accent(String value) { this.accent = value; return this; }

        public MenuSnapshot build() {
            return new MenuSnapshot(this);
        }
    }
}
