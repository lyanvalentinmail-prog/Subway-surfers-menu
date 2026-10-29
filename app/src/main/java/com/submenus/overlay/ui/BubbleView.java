package com.submenus.overlay.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** The circular, draggable floating button. */
public class BubbleView extends View {

    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path boltPath = new Path();

    private boolean open;
    private boolean animationsEnabled = true;
    private boolean integrationConnected;
    private float pulse;
    private float pressScale = 1f;
    private ValueAnimator pulseAnimator;
    private ValueAnimator pressAnimator;

    public BubbleView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        circlePaint.setStyle(Paint.Style.FILL);
        glowPaint.setStyle(Paint.Style.FILL);
        ringPaint.setStyle(Paint.Style.STROKE);
        iconPaint.setStyle(Paint.Style.FILL);
        iconPaint.setColor(Theme.BG_DEEP);
    }

    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
        if (!enabled) {
            stopPulse();
            pulse = 0f;
            pressScale = 1f;
            invalidate();
        }
    }

    public void setOpen(boolean isOpen) {
        if (open == isOpen) {
            return;
        }
        open = isOpen;
        if (animationsEnabled) {
            animatePress(isOpen ? 0.9f : 1f, true);
        }
        invalidate();
    }

    public boolean isOpen() {
        return open;
    }

    public void setIntegrationConnected(boolean connected) {
        if (integrationConnected != connected) {
            integrationConnected = connected;
            invalidate();
        }
    }

    public void startPulse() {
        if (!animationsEnabled) {
            return;
        }
        stopPulse();
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1800L);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(a -> {
            pulse = (float) a.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    public void stopPulse() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        pulse = 0f;
        invalidate();
    }

    public void animatePress(float target, boolean release) {
        if (!animationsEnabled) {
            pressScale = release ? 1f : target;
            invalidate();
            return;
        }
        if (pressAnimator != null) {
            pressAnimator.cancel();
        }
        pressAnimator = ValueAnimator.ofFloat(pressScale, target);
        pressAnimator.setDuration(release ? 220L : 90L);
        pressAnimator.addUpdateListener(a -> {
            pressScale = (float) a.getAnimatedValue();
            invalidate();
        });
        pressAnimator.start();
        if (release) {
            pressAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    pressScale = 1f;
                    invalidate();
                }
            });
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float baseRadius = Math.min(w, h) / 2f;
        float radius = baseRadius * pressScale;

        // Pulsing halo (two expanding rings).
        if (animationsEnabled && pulse > 0f) {
            for (int i = 0; i < 2; i++) {
                float phase = (pulse + i * 0.5f) % 1f;
                float ringRadius = radius * (0.92f + phase * 0.32f);
                int alpha = (int) (110 * (1f - phase));
                ringPaint.setStrokeWidth(Theme.dp(getContext(), 2f));
                ringPaint.setColor(Theme.withAlpha(open ? Theme.ORANGE : Theme.GREEN, alpha / 255f));
                canvas.drawCircle(cx, cy, ringRadius, ringPaint);
            }
        }

        // Ambient glow.
        glowPaint.setShader(new RadialGradient(cx, cy, radius * 1.25f,
                new int[]{
                        Theme.withAlpha(open ? Theme.ORANGE : Theme.GREEN, 0.42f),
                        Theme.withAlpha(Theme.BLUE, 0.12f),
                        Color.TRANSPARENT},
                new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 1.25f, glowPaint);
        glowPaint.setShader(null);

        // Main disc with a diagonal green → blue gradient.
        circlePaint.setShader(new LinearGradient(cx - radius, cy - radius, cx + radius, cy + radius,
                new int[]{Theme.GREEN, Theme.BLUE, Theme.BLUE_DEEP},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, radius * 0.84f, circlePaint);
        circlePaint.setShader(null);

        // Inner dark rim to give depth.
        circlePaint.setColor(Theme.withAlpha(Theme.BG_DEEP, 0.22f));
        canvas.drawCircle(cx, cy, radius * 0.62f, circlePaint);

        // Status dot: green when an integration is connected, orange otherwise.
        ringPaint.setStyle(Paint.Style.FILL);
        ringPaint.setColor(integrationConnected ? Theme.GREEN_LIGHT : Theme.ORANGE);
        canvas.drawCircle(cx + radius * 0.46f, cy - radius * 0.46f, radius * 0.12f, ringPaint);
        ringPaint.setStyle(Paint.Style.STROKE);

        // Bolt icon (or a close cross when the panel is open).
        iconPaint.setColor(Theme.TEXT);
        if (open) {
            iconPaint.setStrokeWidth(Theme.dp(getContext(), 2.6f));
            iconPaint.setStyle(Paint.Style.STROKE);
            float arm = radius * 0.24f;
            canvas.drawLine(cx - arm, cy - arm, cx + arm, cy + arm, iconPaint);
            canvas.drawLine(cx - arm, cy + arm, cx + arm, cy - arm, iconPaint);
            iconPaint.setStyle(Paint.Style.FILL);
        } else {
            boltPath.reset();
            boltPath.moveTo(cx + radius * 0.14f, cy - radius * 0.46f);
            boltPath.lineTo(cx - radius * 0.26f, cy + radius * 0.06f);
            boltPath.lineTo(cx - radius * 0.02f, cy + radius * 0.06f);
            boltPath.lineTo(cx - radius * 0.14f, cy + radius * 0.48f);
            boltPath.lineTo(cx + radius * 0.26f, cy - radius * 0.05f);
            boltPath.lineTo(cx + radius * 0.02f, cy - radius * 0.05f);
            boltPath.close();
            canvas.drawPath(boltPath, iconPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopPulse();
        if (pressAnimator != null) {
            pressAnimator.cancel();
        }
        super.onDetachedFromWindow();
    }
}
