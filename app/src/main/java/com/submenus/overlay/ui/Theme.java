package com.submenus.overlay.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

/** Colors, dimensions and rounded-background factories shared by the whole UI. */
public final class Theme {

    public static final int BG_DEEP = Color.parseColor("#060910");
    public static final int PANEL = Color.parseColor("#0B1220");
    public static final int CARD = Color.parseColor("#111A29");
    public static final int CARD_ALT = Color.parseColor("#0E1725");
    public static final int STROKE = Color.parseColor("#22314A");
    public static final int TEXT = Color.parseColor("#EAF2FF");
    public static final int TEXT_DIM = Color.parseColor("#9DB0CC");
    public static final int TEXT_MUTED = Color.parseColor("#6C8099");

    public static final int GREEN = Color.parseColor("#22E07A");
    public static final int GREEN_LIGHT = Color.parseColor("#5CF2A8");
    public static final int BLUE = Color.parseColor("#2CB6FF");
    public static final int BLUE_DEEP = Color.parseColor("#1E6BFF");
    public static final int ORANGE = Color.parseColor("#FF9B3D");
    public static final int ORANGE_DEEP = Color.parseColor("#FF6B2C");
    public static final int DANGER = Color.parseColor("#FF5C6C");

    private Theme() {
    }

    public static int dp(Context context, float value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                context.getResources().getDisplayMetrics()));
    }

    public static float sp(Context context, float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                context.getResources().getDisplayMetrics());
    }

    /** Rounded rectangle with a subtle 1dp border. */
    public static GradientDrawable rounded(int fill, int stroke, float radiusDp, Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, radiusDp));
        if (stroke != Color.TRANSPARENT) {
            drawable.setStroke(Math.max(1, dp(context, 1f)), stroke);
        }
        return drawable;
    }

    /** Green → blue horizontal gradient used by primary buttons and headers. */
    public static GradientDrawable gradient(float radiusDp, Context context, int start, int end) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{start, end});
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    public static GradientDrawable pill(int fill, int stroke, Context context) {
        return rounded(fill, stroke, 999f, context);
    }

    public static int withAlpha(int color, float alpha) {
        int a = Math.round(Color.alpha(color) * Math.max(0f, Math.min(1f, alpha)));
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }
}
