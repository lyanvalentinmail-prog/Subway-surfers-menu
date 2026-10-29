package com.submenus.overlay.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

/** Label + live value + slider, used for speed, jump, coin multiplier, opacity and size. */
public class SliderRowView extends LinearLayout {

    public interface OnValueChange {
        /** @param value the value already snapped to the configured step. */
        void onValueChange(SliderRowView row, float value, boolean fromUser);
    }

    private final TextView titleView;
    private final TextView valueView;
    private final SeekBar seekBar;

    private final float min;
    private final float max;
    private final float step;
    private final boolean integerValues;
    private final String format;

    private float currentValue;
    private OnValueChange listener;
    private boolean syncing;

    public SliderRowView(Context context, String title, float min, float max, float step,
                         boolean integerValues, String valueFormat) {
        super(context);
        this.min = min;
        this.max = max;
        this.step = step;
        this.integerValues = integerValues;
        this.format = valueFormat;

        setOrientation(VERTICAL);
        int padH = Theme.dp(context, 14f);
        int padV = Theme.dp(context, 12f);
        setPadding(padH, padV, padH, padV);
        setBackground(Theme.rounded(Theme.CARD_ALT, Theme.withAlpha(Theme.STROKE, 0.55f), 16f, context));

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(14f);
        titleView.setTextColor(Theme.TEXT);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        header.addView(titleView, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        valueView = new TextView(context);
        valueView.setTextSize(13f);
        valueView.setTextColor(Theme.ORANGE);
        valueView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        valueView.setPadding(Theme.dp(context, 10f), Theme.dp(context, 2f),
                Theme.dp(context, 10f), Theme.dp(context, 2f));
        valueView.setBackground(Theme.pill(Theme.withAlpha(Theme.ORANGE, 0.12f),
                Theme.withAlpha(Theme.ORANGE, 0.35f), context));
        header.addView(valueView);

        addView(header);

        seekBar = new SeekBar(context);
        seekBar.setMax(steps());
        LayoutParams seekParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(context, 34f));
        seekParams.topMargin = Theme.dp(context, 8f);
        addView(seekBar, seekParams);
        styleSeekBar(context);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                currentValue = valueForProgress(progress);
                updateValueLabel();
                if (fromUser && !syncing && listener != null) {
                    listener.onValueChange(SliderRowView.this, currentValue, true);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                // Nothing to do.
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                // The value stream is continuous; nothing to commit here.
            }
        });
    }

    private void styleSeekBar(Context context) {
        int height = Theme.dp(context, 8f);
        int radius = height / 2;

        GradientDrawable track = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Theme.GREEN, Theme.BLUE});
        track.setCornerRadius(radius);
        track.setSize(0, height);
        seekBar.setProgressDrawable(track);

        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.OVAL);
        thumb.setColor(Theme.TEXT);
        int thumbSize = Theme.dp(context, 18f);
        thumb.setSize(thumbSize, thumbSize);
        thumb.setStroke(Theme.dp(context, 3f), Theme.GREEN);
        seekBar.setThumb(thumb);
        seekBar.setThumbOffset(Theme.dp(context, 1f));
        seekBar.setSplitTrack(false);
        seekBar.setPadding(Theme.dp(context, 2f), 0, Theme.dp(context, 2f), 0);
        seekBar.getProgressDrawable().setAlpha(255);
        seekBar.setBackground(null);

        if (seekBar.getThumb() != null) {
            seekBar.getThumb().setColorFilter(Theme.withAlpha(Theme.GREEN, 1f), PorterDuff.Mode.SRC_ATOP);
        }
    }

    private int steps() {
        if (step <= 0f) {
            return 100;
        }
        return Math.max(1, Math.round((max - min) / step));
    }

    private float valueForProgress(int progress) {
        float value = min + progress * step;
        if (integerValues) {
            return Math.round(value);
        }
        return Math.round(value * 100f) / 100f;
    }

    private int progressForValue(float value) {
        if (step <= 0f) {
            return 0;
        }
        return Math.round((value - min) / step);
    }

    private void updateValueLabel() {
        String text;
        if (integerValues) {
            text = String.format(Locale.US, format, Math.round(currentValue));
        } else {
            text = String.format(Locale.US, format, currentValue);
        }
        valueView.setText(text);
    }

    public void setAnimationsEnabled(boolean enabled) {
        // Sliders are inherently continuous; nothing to toggle.
    }

    public void setHapticsEnabled(boolean enabled) {
        seekBar.setHapticFeedbackEnabled(enabled);
    }

    public float getValue() {
        return currentValue;
    }

    public void setValue(float value) {
        syncing = true;
        currentValue = Math.max(min, Math.min(max, value));
        seekBar.setProgress(progressForValue(currentValue));
        updateValueLabel();
        syncing = false;
    }

    public void setOnValueChange(OnValueChange listener) {
        this.listener = listener;
    }

    public SeekBar getSeekBar() {
        return seekBar;
    }

    public void setTitleTextSize(float sp) {
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
    }
}
