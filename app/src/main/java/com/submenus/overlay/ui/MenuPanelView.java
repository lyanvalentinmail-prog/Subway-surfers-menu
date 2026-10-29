package com.submenus.overlay.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.submenus.overlay.R;
import com.submenus.overlay.bridge.MenuLimits;
import com.submenus.overlay.bridge.MenuSnapshot;
import com.submenus.overlay.data.MenuRepository;

import java.util.Locale;

/**
 * The floating panel: header with connection state, three category tabs and the
 * matching controls. Everything is built in code so the overlay needs no inflater.
 */
public class MenuPanelView extends LinearLayout implements MenuRepository.Listener {

    public interface Callbacks {
        void onCloseRequested();
    }

    private static final int TAB_PLAYER = 0;
    private static final int TAB_COLLECTIBLES = 1;
    private static final int TAB_SETTINGS = 2;

    private final MenuRepository repository;
    private final Callbacks callbacks;

    private final TextView statusPill;
    private final TextView subtitleView;
    private final TextView[] tabs = new TextView[3];
    private final LinearLayout content;
    private final ScrollView scrollView;
    private final TextView footer;

    private int selectedTab = TAB_PLAYER;
    private boolean animationsEnabled = true;

    // Player rows
    private SwitchRowView speedSwitch;
    private SliderRowView speedSlider;
    private SwitchRowView jumpSwitch;
    private SliderRowView jumpSlider;
    private SwitchRowView invincibilitySwitch;

    // Collectibles rows
    private SwitchRowView coinSwitch;
    private SliderRowView coinSlider;
    private SwitchRowView keysSwitch;
    private TextView telemetryView;

    // Settings rows
    private SwitchRowView hapticsSwitch;
    private SwitchRowView animationsSwitch;
    private SwitchRowView snapSwitch;
    private SliderRowView opacitySlider;
    private SliderRowView sizeSlider;

    public MenuPanelView(Context context, MenuRepository repository, Callbacks callbacks) {
        super(context);
        this.repository = repository;
        this.callbacks = callbacks;

        setOrientation(VERTICAL);
        setBackground(Theme.rounded(Theme.withAlpha(Theme.PANEL, 0.97f), Theme.withAlpha(Theme.STROKE, 0.9f),
                24f, context));
        setElevation(Theme.dp(context, 12f));
        int pad = Theme.dp(context, 14f);
        setPadding(pad, pad, pad, Theme.dp(context, 12f));

        // ------------------------------------------------------------ header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(context);
        titles.setOrientation(VERTICAL);
        titles.setLayoutParams(new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(context);
        title.setText(R.string.overlay_title);
        title.setTextSize(17f);
        title.setLetterSpacing(0.16f);
        title.setTextColor(Theme.TEXT);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        titles.addView(title);

        subtitleView = new TextView(context);
        subtitleView.setText(R.string.overlay_subtitle);
        subtitleView.setTextSize(11f);
        subtitleView.setTextColor(Theme.TEXT_MUTED);
        subtitleView.setLetterSpacing(0.06f);
        LayoutParams subParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = Theme.dp(context, 2f);
        titles.addView(subtitleView, subParams);

        header.addView(titles);

        statusPill = new TextView(context);
        statusPill.setTextSize(9.5f);
        statusPill.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        statusPill.setPadding(Theme.dp(context, 8f), Theme.dp(context, 4f),
                Theme.dp(context, 8f), Theme.dp(context, 4f));
        statusPill.setBackground(Theme.pill(Theme.withAlpha(Theme.ORANGE, 0.14f),
                Theme.withAlpha(Theme.ORANGE, 0.4f), context));
        statusPill.setTextColor(Theme.ORANGE);
        header.addView(statusPill);

        TextView close = new TextView(context);
        close.setText("\u2715");
        close.setTextSize(15f);
        close.setGravity(Gravity.CENTER);
        close.setTextColor(Theme.TEXT_DIM);
        int closeSize = Theme.dp(context, 30f);
        close.setLayoutParams(new LayoutParams(closeSize, closeSize));
        LayoutParams closeParams = new LayoutParams(closeSize, closeSize);
        closeParams.setMarginStart(Theme.dp(context, 8f));
        close.setLayoutParams(closeParams);
        close.setBackground(Theme.rounded(Theme.withAlpha(Color.WHITE, 0.06f),
                Theme.withAlpha(Theme.STROKE, 0.8f), 999f, context));
        close.setContentDescription(context.getString(R.string.overlay_close));
        close.setOnClickListener(v -> {
            if (callbacks != null) {
                callbacks.onCloseRequested();
            }
        });
        header.addView(close);

        addView(header);

        // Top gradient hairline.
        View hairline = new View(context);
        hairline.setBackground(Theme.gradient(999f, context, Theme.GREEN, Theme.BLUE_DEEP));
        LayoutParams hairlineParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(context, 2f));
        hairlineParams.topMargin = Theme.dp(context, 12f);
        addView(hairline, hairlineParams);

        // -------------------------------------------------------------- tabs
        LinearLayout tabBar = new LinearLayout(context);
        tabBar.setOrientation(HORIZONTAL);
        tabBar.setBackground(Theme.rounded(Theme.withAlpha(Color.WHITE, 0.045f),
                Theme.withAlpha(Theme.STROKE, 0.6f), 14f, context));
        int tabPadding = Theme.dp(context, 3f);
        tabBar.setPadding(tabPadding, tabPadding, tabPadding, tabPadding);

        LayoutParams tabBarParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tabBarParams.topMargin = Theme.dp(context, 12f);
        addView(tabBar, tabBarParams);

        String[] labels = {
                context.getString(R.string.tab_player),
                context.getString(R.string.tab_collectibles),
                context.getString(R.string.tab_settings)};
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            TextView tab = new TextView(context);
            tab.setText(labels[i]);
            tab.setTextSize(11.5f);
            tab.setGravity(Gravity.CENTER);
            tab.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tab.setPadding(0, Theme.dp(context, 8f), 0, Theme.dp(context, 8f));
            tab.setLayoutParams(new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            tab.setOnClickListener(v -> selectTab(index));
            tabs[i] = tab;
            tabBar.addView(tab);
        }

        // ----------------------------------------------------------- content
        scrollView = new ScrollView(context);
        scrollView.setFillViewport(false);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(OVER_SCROLL_NEVER);

        content = new LinearLayout(context);
        content.setOrientation(VERTICAL);
        content.setPadding(0, Theme.dp(context, 4f), 0, Theme.dp(context, 4f));
        scrollView.addView(content, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LayoutParams scrollParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = Theme.dp(context, 10f);
        addView(scrollView, scrollParams);

        // ------------------------------------------------------------ footer
        footer = new TextView(context);
        footer.setText(R.string.overlay_footer_hint);
        footer.setTextSize(9.5f);
        footer.setTextColor(Theme.TEXT_MUTED);
        footer.setGravity(Gravity.CENTER);
        LayoutParams footerParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerParams.topMargin = Theme.dp(context, 8f);
        addView(footer, footerParams);

        selectTab(TAB_PLAYER);
        setBridgeState(repository.prefs().isIntegrationConnected(),
                repository.prefs().getClientLabel());
        repository.addListener(this);
    }

    /** Maximum height of the scrolling area so the panel never covers the whole screen. */
    public void setContentMaxHeight(int px) {
        scrollView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px, 0f));
    }

    public void setPanelWidth(int px) {
        setLayoutParams(new ViewGroup.LayoutParams(px, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void setAnimationsEnabled(boolean enabled) {
        this.animationsEnabled = enabled;
        applyToRows();
    }

    public void applyToRows() {
        if (speedSwitch != null) speedSwitch.setAnimationsEnabled(animationsEnabled);
        if (jumpSwitch != null) jumpSwitch.setAnimationsEnabled(animationsEnabled);
        if (invincibilitySwitch != null) invincibilitySwitch.setAnimationsEnabled(animationsEnabled);
        if (coinSwitch != null) coinSwitch.setAnimationsEnabled(animationsEnabled);
        if (keysSwitch != null) keysSwitch.setAnimationsEnabled(animationsEnabled);
        if (hapticsSwitch != null) hapticsSwitch.setAnimationsEnabled(animationsEnabled);
        if (animationsSwitch != null) animationsSwitch.setAnimationsEnabled(animationsEnabled);
        if (snapSwitch != null) snapSwitch.setAnimationsEnabled(animationsEnabled);
    }

    public void setHapticsEnabled(boolean enabled) {
        if (speedSwitch != null) speedSwitch.setHapticsEnabled(enabled);
        if (jumpSwitch != null) jumpSwitch.setHapticsEnabled(enabled);
        if (invincibilitySwitch != null) invincibilitySwitch.setHapticsEnabled(enabled);
        if (coinSwitch != null) coinSwitch.setHapticsEnabled(enabled);
        if (keysSwitch != null) keysSwitch.setHapticsEnabled(enabled);
        if (hapticsSwitch != null) hapticsSwitch.setHapticsEnabled(enabled);
        if (animationsSwitch != null) animationsSwitch.setHapticsEnabled(enabled);
        if (snapSwitch != null) snapSwitch.setHapticsEnabled(enabled);
    }

    public void setBridgeState(boolean connected, String clientLabel) {
        if (connected) {
            statusPill.setText(R.string.overlay_bridge_connected);
            statusPill.setTextColor(Theme.GREEN_LIGHT);
            statusPill.setBackground(Theme.pill(Theme.withAlpha(Theme.GREEN, 0.14f),
                    Theme.withAlpha(Theme.GREEN, 0.45f), getContext()));
            subtitleView.setText(clientLabel == null || clientLabel.isEmpty()
                    ? getContext().getString(R.string.overlay_subtitle)
                    : clientLabel);
        } else {
            statusPill.setText(R.string.overlay_bridge_offline);
            statusPill.setTextColor(Theme.ORANGE);
            statusPill.setBackground(Theme.pill(Theme.withAlpha(Theme.ORANGE, 0.14f),
                    Theme.withAlpha(Theme.ORANGE, 0.4f), getContext()));
            subtitleView.setText(R.string.overlay_subtitle);
        }
        updateTelemetry();
    }

    // ------------------------------------------------------------------- tabs

    private void selectTab(int index) {
        selectedTab = index;
        for (int i = 0; i < tabs.length; i++) {
            boolean selected = i == index;
            tabs[i].setTextColor(selected ? Theme.BG_DEEP : Theme.TEXT_DIM);
            tabs[i].setBackground(selected
                    ? Theme.gradient(12f, getContext(), Theme.GREEN, Theme.BLUE)
                    : null);
            tabs[i].setAlpha(selected ? 1f : 0.85f);
        }
        rebuild();
    }

    private void rebuild() {
        content.removeAllViews();
        speedSwitch = null;
        speedSlider = null;
        jumpSwitch = null;
        jumpSlider = null;
        invincibilitySwitch = null;
        coinSwitch = null;
        coinSlider = null;
        keysSwitch = null;
        telemetryView = null;
        hapticsSwitch = null;
        animationsSwitch = null;
        snapSwitch = null;
        opacitySlider = null;
        sizeSlider = null;

        switch (selectedTab) {
            case TAB_PLAYER:
                buildPlayerTab();
                break;
            case TAB_COLLECTIBLES:
                buildCollectiblesTab();
                break;
            default:
                buildSettingsTab();
                break;
        }
        applyToRows();
        setHapticsEnabled(repository.prefs().isHapticsEnabled());
    }

    private void buildPlayerTab() {
        addSectionLabel(getContext().getString(R.string.tab_player));

        speedSwitch = addSwitchRow(R.string.opt_speed, R.string.opt_speed_sub,
                repository.isSpeedEnabled());
        speedSwitch.setOnRowCheckedChange((row, checked) -> repository.setSpeedEnabled(checked));

        speedSlider = addSliderRow(getContext().getString(R.string.opt_speed),
                MenuLimits.SPEED_MIN, MenuLimits.SPEED_MAX, MenuLimits.SPEED_STEP,
                false, "%.2f\u00d7", repository.getSpeed());
        speedSlider.setOnValueChange((row, value, fromUser) -> repository.setSpeedMultiplier(value));

        jumpSwitch = addSwitchRow(R.string.opt_high_jump, R.string.opt_high_jump_sub,
                repository.isHighJumpEnabled());
        jumpSwitch.setOnRowCheckedChange((row, checked) -> repository.setHighJumpEnabled(checked));

        jumpSlider = addSliderRow(getContext().getString(R.string.opt_high_jump),
                MenuLimits.HIGH_JUMP_MIN, MenuLimits.HIGH_JUMP_MAX, MenuLimits.HIGH_JUMP_STEP,
                false, "%.2f\u00d7", repository.getHighJump());
        jumpSlider.setOnValueChange((row, value, fromUser) -> repository.setHighJumpMultiplier(value));

        invincibilitySwitch = addSwitchRow(R.string.opt_invincibility, R.string.opt_invincibility_sub,
                repository.isInvincibilityEnabled());
        invincibilitySwitch.setOnRowCheckedChange(
                (row, checked) -> repository.setInvincibilityEnabled(checked));
    }

    private void buildCollectiblesTab() {
        addSectionLabel(getContext().getString(R.string.tab_collectibles));

        coinSwitch = addSwitchRow(R.string.opt_coin, R.string.opt_coin_sub,
                repository.isCoinMultiplierEnabled());
        coinSwitch.setOnRowCheckedChange((row, checked) -> repository.setCoinMultiplierEnabled(checked));

        coinSlider = addSliderRow(getContext().getString(R.string.opt_coin),
                MenuLimits.COIN_MIN, MenuLimits.COIN_MAX, 1f,
                true, "%d\u00d7", repository.getCoinMultiplier());
        coinSlider.setOnValueChange((row, value, fromUser) ->
                repository.setCoinMultiplier(Math.round(value)));

        keysSwitch = addSwitchRow(R.string.opt_keys, R.string.opt_keys_sub,
                repository.isKeysCounterEnabled());
        keysSwitch.setOnRowCheckedChange((row, checked) -> repository.setKeysCounterEnabled(checked));

        telemetryView = new TextView(getContext());
        telemetryView.setTextSize(10.5f);
        telemetryView.setTextColor(Theme.TEXT_MUTED);
        telemetryView.setPadding(Theme.dp(getContext(), 4f), Theme.dp(getContext(), 8f),
                Theme.dp(getContext(), 4f), 0);
        content.addView(telemetryView);
        updateTelemetry();
    }

    private void buildSettingsTab() {
        addSectionLabel(getContext().getString(R.string.prefs_title));

        hapticsSwitch = addSwitchRow(R.string.pref_haptics, R.string.pref_haptics_sub,
                repository.prefs().isHapticsEnabled());
        hapticsSwitch.setOnRowCheckedChange((row, checked) -> {
            repository.setHapticsEnabled(checked);
            setHapticsEnabled(checked);
        });

        animationsSwitch = addSwitchRow(R.string.pref_animations, R.string.pref_animations_sub,
                repository.prefs().isAnimationsEnabled());
        animationsSwitch.setOnRowCheckedChange((row, checked) -> {
            animationsEnabled = checked;
            repository.setAnimationsEnabled(checked);
            applyToRows();
        });

        snapSwitch = addSwitchRow(R.string.pref_snap, R.string.pref_snap_sub,
                repository.prefs().isSnapToEdgeEnabled());
        snapSwitch.setOnRowCheckedChange(
                (row, checked) -> repository.setSnapToEdgeEnabled(checked));

        opacitySlider = addSliderRow(getContext().getString(R.string.pref_opacity),
                40f, 100f, 5f, true, "%d%%",
                Math.round(repository.prefs().getOpacity() * 100f));
        opacitySlider.setOnValueChange((row, value, fromUser) -> {
            repository.setOpacity(value / 100f);
            setAlpha(value / 100f);
        });
        setAlpha(repository.prefs().getOpacity());

        sizeSlider = addSliderRow(getContext().getString(R.string.pref_size),
                MenuLimits.BUTTON_SIZE_MIN_DP, MenuLimits.BUTTON_SIZE_MAX_DP, 4f, true, "%d dp",
                repository.prefs().getButtonSizeDp());
        sizeSlider.setOnValueChange((row, value, fromUser) ->
                repository.setButtonSizeDp(Math.round(value)));

        addSectionLabel(getContext().getString(R.string.reset_title));

        TextView reset = new TextView(getContext());
        reset.setText(R.string.action_reset_mods);
        reset.setTextSize(13f);
        reset.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        reset.setTextColor(Theme.DANGER);
        reset.setGravity(Gravity.CENTER);
        reset.setPadding(0, Theme.dp(getContext(), 12f), 0, Theme.dp(getContext(), 12f));
        reset.setBackground(Theme.rounded(Theme.withAlpha(Theme.DANGER, 0.1f),
                Theme.withAlpha(Theme.DANGER, 0.35f), 14f, getContext()));
        reset.setOnClickListener(v -> {
            repository.resetGameplay();
            rebuild();
        });
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Theme.dp(getContext(), 6f);
        content.addView(reset, params);
    }

    private void addSectionLabel(String text) {
        TextView label = new TextView(getContext());
        label.setText(text.toUpperCase(Locale.getDefault()));
        label.setTextSize(9.5f);
        label.setLetterSpacing(0.14f);
        label.setTextColor(Theme.TEXT_MUTED);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Theme.dp(getContext(), 6f);
        params.bottomMargin = Theme.dp(getContext(), 6f);
        params.setMarginStart(Theme.dp(getContext(), 2f));
        content.addView(label, params);
    }

    private SwitchRowView addSwitchRow(int titleRes, int subtitleRes, boolean checked) {
        SwitchRowView row = new SwitchRowView(getContext(),
                getContext().getString(titleRes), getContext().getString(subtitleRes), checked);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Theme.dp(getContext(), 6f);
        content.addView(row, params);
        return row;
    }

    private SliderRowView addSliderRow(String title, float min, float max, float step,
                                       boolean integerValues, String format, float value) {
        SliderRowView row = new SliderRowView(getContext(), title, min, max, step,
                integerValues, format);
        row.setValue(value);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = Theme.dp(getContext(), 6f);
        content.addView(row, params);
        return row;
    }

    private void updateTelemetry() {
        if (telemetryView == null || !repository.isKeysCounterEnabled()) {
            return;
        }
        int keys = repository.prefs().getReportedKeys();
        int coins = repository.prefs().getReportedCoins();
        StringBuilder text = new StringBuilder();
        if (keys >= 0) {
            text.append(getContext().getString(R.string.keys_reported, keys));
        } else {
            text.append(getContext().getString(R.string.keys_no_data));
        }
        if (coins >= 0) {
            text.append(" · ").append(getContext().getString(R.string.coins_reported, coins));
        }
        telemetryView.setText(text.toString());
    }

    @Override
    public void onSnapshotChanged(MenuSnapshot snapshot) {
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
        updateTelemetry();
    }

    /** Stops listening to the repository; call before dropping the view. */
    public void release() {
        repository.removeListener(this);
    }
}
