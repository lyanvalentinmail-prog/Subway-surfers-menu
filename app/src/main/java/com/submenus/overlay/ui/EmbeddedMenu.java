package com.submenus.overlay.ui;

import android.app.Activity;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.submenus.overlay.data.MenuRepository;
import com.submenus.overlay.data.Prefs;

/**
 * The same floating menu, but <b>embedded inside an activity</b> instead of drawn over
 * other apps. No overlay permission, no service, no notification:
 *
 * <pre>
 * // inside onCreate(), after setContentView(...)
 * EmbeddedMenu.attach(this);
 * </pre>
 *
 * A circular button floats in the corner of your own screen; tapping it opens the full
 * Player / Collectibles / Settings panel. Everything it changes is published through
 * {@link MenuRepository}, so the hosting app can read the values in-process.
 */
public final class EmbeddedMenu implements MenuRepository.Listener {

    private final Activity activity;
    private final FrameLayout host;
    private final MenuRepository repository;
    private final Prefs prefs;
    private final BubbleView bubble;

    private FrameLayout scrim;
    private MenuPanelView panel;
    private boolean panelVisible;
    private boolean detached;

    private float downRawX;
    private float downRawY;
    private int startLeft;
    private int startTop;
    private boolean moved;
    private final int touchSlop;

    private EmbeddedMenu(Activity activity) {
        this.activity = activity;
        this.repository = MenuRepository.get(activity);
        this.prefs = repository.prefs();
        this.touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();

        ViewGroup content = activity.findViewById(android.R.id.content);
        host = new FrameLayout(activity);
        host.setClipChildren(false);
        host.setClipToPadding(false);
        FrameLayout.LayoutParams hostParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        content.addView(host, hostParams);

        bubble = new BubbleView(activity);
        bubble.setAnimationsEnabled(prefs.isAnimationsEnabled());
        bubble.setGlowPadding(Theme.dp(activity, 12f));
        bubble.setContentDescription(activity.getString(
                com.submenus.overlay.R.string.overlay_open));
        bubble.setIntegrationConnected(prefs.isIntegrationConnected());

        int sizePx = windowSizeFor(prefs.getButtonSizeDp());
        FrameLayout.LayoutParams bubbleParams = new FrameLayout.LayoutParams(sizePx, sizePx);
        bubbleParams.leftMargin = prefs.getEmbeddedButtonX() >= 0
                ? prefs.getEmbeddedButtonX() : 0;
        bubbleParams.topMargin = prefs.getEmbeddedButtonY() >= 0
                ? prefs.getEmbeddedButtonY() : 0;
        bubbleParams.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        host.addView(bubble, bubbleParams);

        bubble.setOnTouchListener(this::onBubbleTouch);
        repository.addListener(this);

        // Position the button after the first layout pass, when the host has a size.
        host.post(() -> {
            if (prefs.getEmbeddedButtonX() < 0 || prefs.getEmbeddedButtonY() < 0) {
                moveBubble(host.getWidth() - sizePx - Theme.dp(activity, 12f),
                        Math.round(host.getHeight() * 0.62f));
            }
        });
    }

    /** Adds the embedded menu to the given activity and returns its controller. */
    public static EmbeddedMenu attach(Activity activity) {
        return new EmbeddedMenu(activity);
    }

    private int windowSizeFor(int sizeDp) {
        return Theme.dp(activity, sizeDp) + Theme.dp(activity, 12f) * 2;
    }

    private boolean onBubbleTouch(View view, MotionEvent event) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bubble.getLayoutParams();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                startLeft = params.leftMargin;
                startTop = params.topMargin;
                moved = false;
                bubble.animatePress(0.9f, false);
                return true;

            case MotionEvent.ACTION_MOVE: {
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!moved && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                    moved = true;
                }
                if (moved) {
                    moveBubble(startLeft + Math.round(dx), startTop + Math.round(dy));
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
                bubble.animatePress(1f, true);
                if (!moved) {
                    view.performClick();
                    togglePanel();
                } else {
                    prefs.setEmbeddedButtonPosition(params.leftMargin, params.topMargin);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                bubble.animatePress(1f, true);
                return true;

            default:
                return false;
        }
    }

    private void moveBubble(int left, int top) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bubble.getLayoutParams();
        int maxLeft = Math.max(0, host.getWidth() - params.width);
        int maxTop = Math.max(0, host.getHeight() - params.height);
        params.leftMargin = Math.max(0, Math.min(left, maxLeft));
        params.topMargin = Math.max(0, Math.min(top, maxTop));
        bubble.setLayoutParams(params);
        if (panelVisible) {
            positionPanel();
        }
    }

    private void togglePanel() {
        if (panelVisible) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    /** Opens the menu programmatically (useful from a title screen button). */
    public void showPanel() {
        if (detached || panelVisible) {
            return;
        }
        if (panel == null) {
            panel = new MenuPanelView(activity, repository, this::hidePanel);
            panel.setAnimationsEnabled(prefs.isAnimationsEnabled());
            panel.setHapticsEnabled(prefs.isHapticsEnabled());
            panel.setBridgeState(prefs.isIntegrationConnected(), prefs.getClientLabel());
        }

        int width = Math.min(Theme.dp(activity, 312f), host.getWidth() - Theme.dp(activity, 24f));
        panel.setPanelWidth(width);
        panel.setContentMaxHeight(Math.round(host.getHeight() * 0.46f));

        scrim = new FrameLayout(activity);
        scrim.setBackgroundColor(Color.parseColor("#8C050A12"));
        scrim.setOnClickListener(v -> hidePanel());
        host.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                width, ViewGroup.LayoutParams.WRAP_CONTENT);
        host.addView(panel, panelParams);
        panelVisible = true;
        positionPanel();
        host.post(this::positionPanel);

        bubble.setOpen(true);
        bubble.startPulse();

        if (prefs.isAnimationsEnabled()) {
            panel.setAlpha(0f);
            panel.setScaleX(0.94f);
            panel.setScaleY(0.94f);
            panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(170L).start();
            scrim.setAlpha(0f);
            scrim.animate().alpha(1f).setDuration(150L).start();
        }
        panel.bringToFront();
    }

    /** Closes the menu panel if it is open. */
    public void hidePanel() {
        if (!panelVisible) {
            return;
        }
        panelVisible = false;
        final MenuPanelView closing = panel;
        final FrameLayout closingScrim = scrim;
        scrim = null;

        Runnable removal = () -> {
            if (closing != null && closing.getParent() != null) {
                host.removeView(closing);
            }
            if (closingScrim != null && closingScrim.getParent() != null) {
                host.removeView(closingScrim);
            }
        };

        if (prefs.isAnimationsEnabled() && closing != null) {
            closing.animate().alpha(0f).scaleX(0.95f).scaleY(0.95f).setDuration(120L)
                    .withEndAction(removal).start();
        } else {
            removal.run();
        }

        bubble.setOpen(false);
        bubble.stopPulse();
    }

    public boolean isPanelVisible() {
        return panelVisible;
    }

    private void positionPanel() {
        if (panel == null || !panelVisible) {
            return;
        }
        panel.setX(Math.max(0f, (host.getWidth() - panel.getWidth()) / 2f));
        float preferredY = bubble.getTop() + bubble.getHeight() / 2f - panel.getHeight() / 2f;
        float minY = Theme.dp(activity, 12f);
        float maxY = Math.max(minY, host.getHeight() - panel.getHeight() - Theme.dp(activity, 12f));
        panel.setY(Math.max(minY, Math.min(preferredY, maxY)));
    }

    /** Refreshes the button look (used when the integration heartbeat changes). */
    public void refreshBridgeState() {
        if (bubble != null) {
            bubble.setIntegrationConnected(prefs.isIntegrationConnected());
        }
    }

    @Override
    public void onSnapshotChanged(com.submenus.overlay.bridge.MenuSnapshot snapshot) {
        if (detached || bubble == null) {
            return;
        }
        bubble.setAnimationsEnabled(snapshot.animationsEnabled);
        bubble.setIntegrationConnected(prefs.isIntegrationConnected());

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bubble.getLayoutParams();
        int target = windowSizeFor(snapshot.buttonSizeDp);
        if (params.width != target) {
            params.width = target;
            params.height = target;
            int maxLeft = Math.max(0, host.getWidth() - target);
            int maxTop = Math.max(0, host.getHeight() - target);
            params.leftMargin = Math.min(params.leftMargin, maxLeft);
            params.topMargin = Math.min(params.topMargin, maxTop);
            bubble.setLayoutParams(params);
            if (panelVisible) {
                positionPanel();
            }
        }
    }

    /** Removes every view and stops listening. Call it from {@code onDestroy()}. */
    public void detach() {
        detached = true;
        repository.removeListener(this);
        if (panel != null) {
            panel.release();
            panel = null;
        }
        ViewGroup parent = (ViewGroup) host.getParent();
        if (parent != null) {
            parent.removeView(host);
        }
    }
}
