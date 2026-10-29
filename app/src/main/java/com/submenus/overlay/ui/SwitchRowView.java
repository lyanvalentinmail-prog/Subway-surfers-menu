package com.submenus.overlay.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A title + optional subtitle on the left, an animated {@link ToggleSwitch} on the right. */
public class SwitchRowView extends LinearLayout {

    public interface OnRowCheckedChange {
        void onRowCheckedChange(SwitchRowView row, boolean isChecked);
    }

    private final TextView titleView;
    private final TextView subtitleView;
    private final ToggleSwitch toggle;
    private OnRowCheckedChange rowListener;

    public SwitchRowView(Context context, String title, String subtitle, boolean checked) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int padH = Theme.dp(context, 14f);
        int padV = Theme.dp(context, 12f);
        setPadding(padH, padV, padH, padV);
        setBackground(Theme.rounded(Theme.CARD_ALT, Theme.withAlpha(Theme.STROKE, 0.55f), 16f, context));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(VERTICAL);
        LayoutParams textParams = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMarginEnd(Theme.dp(context, 12f));
        texts.setLayoutParams(textParams);

        titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(14f);
        titleView.setTextColor(Theme.TEXT);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        texts.addView(titleView);

        subtitleView = new TextView(context);
        subtitleView.setTextSize(11f);
        subtitleView.setTextColor(Theme.TEXT_MUTED);
        if (subtitle == null || subtitle.isEmpty()) {
            subtitleView.setVisibility(GONE);
        } else {
            subtitleView.setText(subtitle);
        }
        LayoutParams subParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = Theme.dp(context, 3f);
        texts.addView(subtitleView, subParams);

        addView(texts);

        toggle = new ToggleSwitch(context);
        toggle.setCheckedSilently(checked);
        toggle.setOnCheckedChangeListener((view, isChecked) -> {
            if (rowListener != null) {
                rowListener.onRowCheckedChange(SwitchRowView.this, isChecked);
            }
        });
        LayoutParams toggleParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(toggle, toggleParams);

        setOnClickListener(v -> toggle.setChecked(!toggle.isChecked(), true));
    }

    public void setAnimationsEnabled(boolean enabled) {
        toggle.setAnimationsEnabled(enabled);
    }

    public void setHapticsEnabled(boolean enabled) {
        toggle.setHapticFeedbackEnabled(enabled);
    }

    public boolean isChecked() {
        return toggle.isChecked();
    }

    public void setChecked(boolean checked) {
        toggle.setCheckedSilently(checked);
    }

    public ToggleSwitch getToggle() {
        return toggle;
    }

    public void setSubtitle(CharSequence text) {
        subtitleView.setText(text);
        subtitleView.setVisibility(text == null || text.length() == 0 ? GONE : VISIBLE);
    }

    public void setOnRowCheckedChange(OnRowCheckedChange listener) {
        this.rowListener = listener;
    }

    public void setTitleColor(int color) {
        titleView.setTextColor(color);
    }
}
