package com.submenus.overlay.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.PathInterpolator;

/**
 * Custom animated switch: a rounded track that fills with the green→blue gradient
 * while the knob slides. Drawn by hand so it looks identical on every OEM skin.
 */
public class ToggleSwitch extends View {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(ToggleSwitch view, boolean isChecked);
    }

    private static final float TRACK_HEIGHT_DP = 28f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private final RectF knobRect = new RectF();

    private boolean checked;
    private float progress;
    private boolean animationsEnabled = true;
    private OnCheckedChangeListener listener;
    private ValueAnimator animator;

    public ToggleSwitch(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        knobPaint.setColor(0xFFFFFFFF);
        trackPaint.setStyle(Paint.Style.FILL);
        glowPaint.setStyle(Paint.Style.FILL);
        setContentDescription("switch");
    }

    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    public boolean isChecked() {
        return checked;
    }

    /** Sets the value without notifying the listener (used when syncing the UI). */
    public void setCheckedSilently(boolean value) {
        checked = value;
        progress = value ? 1f : 0f;
        invalidate();
    }

    public void setChecked(boolean value, boolean notify) {
        if (checked == value) {
            if (!notify) {
                setCheckedSilently(value);
            }
            return;
        }
        checked = value;
        animateTo(value ? 1f : 0f);
        if (notify && listener != null) {
            listener.onCheckedChanged(this, value);
        }
    }

    private void animateTo(float target) {
        if (animator != null) {
            animator.cancel();
        }
        if (!animationsEnabled) {
            progress = target;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(progress, target);
        animator.setDuration(Math.round(220 * Math.abs(target - progress) + 60));
        animator.setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f));
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = Theme.dp(getContext(), TRACK_HEIGHT_DP);
        int width = Theme.dp(getContext(), 52f);
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float h = getHeight();
        float w = getWidth();
        float radius = h / 2f;
        float padding = Theme.dp(getContext(), 3f);

        trackRect.set(0f, 0f, w, h);

        // Off track + on gradient blended by progress.
        int offColor = Theme.withAlpha(Theme.STROKE, 0.9f);
        trackPaint.setColor(blend(offColor, Theme.withAlpha(Theme.BLUE_DEEP, 0.9f), progress));
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

        if (progress > 0.01f) {
            int left = blend(offColor, Theme.GREEN, progress);
            int right = blend(offColor, Theme.BLUE, progress);
            trackPaint.setColor(Theme.withAlpha(left, 0.95f));
            canvas.drawRoundRect(trackRect, radius, radius, trackPaint);
            trackPaint.setColor(Theme.withAlpha(right, 0.45f));
            canvas.drawRect(radius, 0f, w - radius, h, trackPaint);
        }

        // Subtle outer glow when on.
        if (progress > 0.02f) {
            glowPaint.setColor(Theme.withAlpha(Theme.GREEN, 0.35f * progress));
            glowPaint.setShadowLayer(Theme.dp(getContext(), 6f) * progress, 0f, 0f,
                    Theme.withAlpha(Theme.GREEN, 0.8f * progress));
            canvas.drawRoundRect(trackRect, radius, radius, glowPaint);
            glowPaint.clearShadowLayer();
        }

        // Knob.
        float knobDiameter = h - padding * 2f;
        float travel = w - knobDiameter - padding * 2f;
        float left = padding + travel * progress;
        knobRect.set(left, padding, left + knobDiameter, padding + knobDiameter);
        knobPaint.setColor(0xFFFFFFFF);
        knobPaint.setShadowLayer(Theme.dp(getContext(), 3f), 0f, Theme.dp(getContext(), 1f),
                0x55000000);
        canvas.drawOval(knobRect, knobPaint);
        knobPaint.clearShadowLayer();

        // Inner dot colored by state.
        float inner = knobDiameter * 0.34f;
        float cx = knobRect.centerX();
        float cy = knobRect.centerY();
        knobPaint.setColor(blend(Theme.TEXT_MUTED, Theme.GREEN, progress));
        canvas.drawCircle(cx, cy, inner, knobPaint);
    }

    private static int blend(int from, int to, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        int a = Math.round(android.graphics.Color.alpha(from)
                + (android.graphics.Color.alpha(to) - android.graphics.Color.alpha(from)) * clamped);
        int r = Math.round(android.graphics.Color.red(from)
                + (android.graphics.Color.red(to) - android.graphics.Color.red(from)) * clamped);
        int g = Math.round(android.graphics.Color.green(from)
                + (android.graphics.Color.green(to) - android.graphics.Color.green(from)) * clamped);
        int b = Math.round(android.graphics.Color.blue(from)
                + (android.graphics.Color.blue(to) - android.graphics.Color.blue(from)) * clamped);
        return android.graphics.Color.argb(a, r, g, b);
    }

    @Override
    public boolean performClick() {
        toggle();
        return true;
    }

    private void toggle() {
        setChecked(!checked, true);
        if (isHapticFeedbackEnabled()) {
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }
}
