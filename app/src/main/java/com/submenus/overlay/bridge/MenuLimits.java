package com.submenus.overlay.bridge;

/** Hard limits of every published value. Both the UI and the integration use them. */
public final class MenuLimits {
    public static final float SPEED_MIN = 1.0f;
    public static final float SPEED_MAX = 3.0f;
    public static final float SPEED_STEP = 0.05f;
    public static final float SPEED_DEFAULT = 1.5f;

    public static final float HIGH_JUMP_MIN = 1.0f;
    public static final float HIGH_JUMP_MAX = 3.0f;
    public static final float HIGH_JUMP_STEP = 0.05f;
    public static final float HIGH_JUMP_DEFAULT = 1.5f;

    public static final int COIN_MIN = 1;
    public static final int COIN_MAX = 50;
    public static final int COIN_DEFAULT = 5;

    public static final float OPACITY_MIN = 0.4f;
    public static final float OPACITY_MAX = 1.0f;
    public static final float OPACITY_DEFAULT = 1.0f;

    public static final int BUTTON_SIZE_MIN_DP = 48;
    public static final int BUTTON_SIZE_MAX_DP = 88;
    public static final int BUTTON_SIZE_DEFAULT_DP = 64;

    private MenuLimits() {
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    /** Rounds a float to the given step and clamps it inside [min, max]. */
    public static float snapToStep(float value, float min, float max, float step) {
        float steps = Math.round((value - min) / step);
        return clamp(min + steps * step, min, max);
    }
}
