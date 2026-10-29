package com.submenus.overlay;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

import com.submenus.overlay.bridge.MenuSnapshot;
import com.submenus.overlay.data.MenuRepository;
import com.submenus.overlay.data.Prefs;
import com.submenus.overlay.ui.BubbleView;
import com.submenus.overlay.ui.DragController;
import com.submenus.overlay.ui.MenuPanelView;
import com.submenus.overlay.ui.Theme;

/**
 * Owns the two overlay windows (draggable bubble + menu panel), keeps them in sync
 * with the stored preferences and polls the integration heartbeat.
 */
public class OverlayService extends Service implements MenuRepository.Listener, MenuPanelView.Callbacks {

    public static final String ACTION_START = "com.submenus.overlay.action.START";
    public static final String ACTION_STOP = "com.submenus.overlay.action.STOP";

    private static final String CHANNEL_ID = "subway_menu_overlay";
    private static final int NOTIFICATION_ID = 4242;
    private static final long HEARTBEAT_POLL_MS = 2_000L;
    private static final int GLOW_PADDING_DP = 12;

    /** True while the service is alive (used by the activity to refresh its UI). */
    public static volatile boolean isRunning = false;

    private WindowManager windowManager;
    private MenuRepository repository;
    private Prefs prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private BubbleView bubbleView;
    private WindowManager.LayoutParams bubbleParams;
    private DragController bubbleDrag;

    private MenuPanelView panelView;
    private WindowManager.LayoutParams panelParams;
    private boolean panelVisible;
    private int lastBubbleSizeDp = -1;
    private int screenWidth;
    private int screenHeight;
    private int edgeMarginPx;

    private ValueAnimator snapAnimator;

    private final Runnable heartbeatPoll = new Runnable() {
        @Override
        public void run() {
            updateBridgeState();
            handler.postDelayed(this, HEARTBEAT_POLL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        repository = MenuRepository.get(this);
        prefs = repository.prefs();
        readScreenMetrics();
        edgeMarginPx = Theme.dp(this, 8f);
        repository.addListener(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startAsForeground();
        prefs.setMenuEnabled(true);

        if (bubbleView == null) {
            showBubble();
            handler.post(heartbeatPoll);
        }
        return START_STICKY;
    }

    private void startAsForeground() {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) {
                return;
            }
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.notification_channel_desc));
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        int icon = R.drawable.ic_notification;

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openIntent, flags);

        Intent stopIntent = new Intent(this, OverlayService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent, flags);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(icon)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(icon,
                        getString(R.string.notification_stop), stopPending).build());
        return builder.build();
    }

    // ------------------------------------------------------------- overlays

    private void showBubble() {
        int sizeDp = prefs.getButtonSizeDp();
        int sizePx = windowSizeFor(sizeDp);
        lastBubbleSizeDp = sizeDp;

        bubbleView = new BubbleView(this);
        bubbleView.setAnimationsEnabled(prefs.isAnimationsEnabled());
        bubbleView.setGlowPadding(Theme.dp(this, GLOW_PADDING_DP));
        bubbleView.setOpen(false);

        bubbleParams = new WindowManager.LayoutParams(
                sizePx, sizePx,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        bubbleParams.gravity = Gravity.TOP | Gravity.START;

        // Restore the last position, otherwise park the bubble on the right edge.
        int savedX = prefs.getButtonX();
        int savedY = prefs.getButtonY();
        bubbleParams.x = savedX >= 0 ? clampX(savedX, sizePx) : screenWidth - sizePx - edgeMarginPx;
        bubbleParams.y = savedY >= 0 ? clampY(savedY, sizePx) : Math.round(screenHeight * 0.34f);

        bubbleDrag = new DragController(windowManager, bubbleParams, bubbleView, new DragController.Listener() {
            @Override
            public void onTap(DragController controller) {
                togglePanel();
            }

            @Override
            public void onDragStart(DragController controller) {
                cancelSnapAnimation();
                bubbleView.animatePress(0.88f, false);
            }

            @Override
            public void onDrag(DragController controller, int windowX, int windowY) {
                if (panelVisible) {
                    positionPanel();
                }
            }

            @Override
            public void onDragEnd(DragController controller, int windowX, int windowY) {
                bubbleView.animatePress(1f, true);
                snapToEdge();
            }
        });

        try {
            windowManager.addView(bubbleView, bubbleParams);
        } catch (Exception e) {
            stopSelf();
        }
        updateBridgeState();
    }

    private void togglePanel() {
        if (panelVisible) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    private void showPanel() {
        if (bubbleView == null) {
            return;
        }
        if (panelView == null) {
            panelView = new MenuPanelView(this, repository, this);
            panelView.setAnimationsEnabled(prefs.isAnimationsEnabled());
            panelView.setHapticsEnabled(prefs.isHapticsEnabled());
            panelView.setAlpha(prefs.getOpacity());
            panelView.setBridgeState(prefs.isIntegrationConnected(), prefs.getClientLabel());
        }

        int panelWidth = Math.min(Theme.dp(this, 312f), screenWidth - Theme.dp(this, 24f));
        panelView.setPanelWidth(panelWidth);
        int maxContentHeight = Math.round(screenHeight * 0.46f);
        panelView.setContentMaxHeight(maxContentHeight);

        panelParams = new WindowManager.LayoutParams(
                panelWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START;

        int[] position = computePanelPosition(panelWidth, screenHeight);
        panelParams.x = position[0];
        panelParams.y = position[1];

        if (panelView.getParent() != null) {
            windowManager.removeView(panelView);
        }
        windowManager.addView(panelView, panelParams);
        panelVisible = true;

        bubbleView.setOpen(true);
        bubbleView.startPulse();

        // The real height is only known after layout; reposition then.
        panelView.post(() -> {
            if (panelVisible) {
                positionPanel();
            }
        });

        if (prefs.isAnimationsEnabled()) {
            panelView.setAlpha(0f);
            panelView.setScaleX(0.94f);
            panelView.setScaleY(0.94f);
            panelView.animate()
                    .alpha(prefs.getOpacity())
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(180L)
                    .start();
        }
    }

    private void hidePanel() {
        if (panelView != null && panelVisible) {
            final MenuPanelView view = panelView;
            panelVisible = false;
            if (prefs.isAnimationsEnabled()) {
                view.animate().alpha(0f).scaleX(0.95f).scaleY(0.95f).setDuration(120L)
                        .withEndAction(() -> detachPanel(view)).start();
            } else {
                detachPanel(view);
            }
        }
        if (bubbleView != null) {
            bubbleView.setOpen(false);
            bubbleView.stopPulse();
        }
    }

    private void detachPanel(MenuPanelView view) {
        try {
            if (view.getParent() != null) {
                windowManager.removeView(view);
            }
        } catch (Exception ignored) {
            // Already detached.
        }
        if (view == panelView) {
            panelView = null;
        }
    }

    private int[] computePanelPosition(int panelWidth, int panelHeightEstimate) {
        int bubbleSize = bubbleParams.width;
        int gap = Theme.dp(this, 10f);

        int spaceRight = screenWidth - (bubbleParams.x + bubbleSize);
        int x;
        if (spaceRight >= panelWidth + gap) {
            x = bubbleParams.x + bubbleSize + gap;
        } else {
            x = bubbleParams.x - panelWidth - gap;
        }
        x = Math.max(edgeMarginPx, Math.min(x, screenWidth - panelWidth - edgeMarginPx));

        int y = bubbleParams.y + bubbleSize / 2 - panelHeightEstimate / 2;
        y = Math.max(Theme.dp(this, 16f),
                Math.min(y, screenHeight - panelHeightEstimate - Theme.dp(this, 16f)));
        return new int[]{x, y};
    }

    private void positionPanel() {
        if (!panelVisible || panelView == null || panelParams == null) {
            return;
        }
        int height = panelView.getHeight() > 0 ? panelView.getHeight() : Math.round(screenHeight * 0.6f);
        int[] position = computePanelPosition(panelParams.width, height);
        panelParams.x = position[0];
        panelParams.y = position[1];
        try {
            windowManager.updateViewLayout(panelView, panelParams);
        } catch (Exception ignored) {
            // View already detached.
        }
    }

    // ------------------------------------------------------------- dragging

    private void snapToEdge() {
        if (bubbleParams == null || bubbleView == null) {
            return;
        }
        int size = bubbleParams.width;
        int targetX;
        if (prefs.isSnapToEdgeEnabled()) {
            int leftDistance = bubbleParams.x;
            int rightDistance = screenWidth - (bubbleParams.x + size);
            targetX = leftDistance <= rightDistance ? edgeMarginPx : screenWidth - size - edgeMarginPx;
        } else {
            targetX = clampX(bubbleParams.x, size);
        }
        int targetY = clampY(bubbleParams.y, size);

        if (!prefs.isAnimationsEnabled() || targetX == bubbleParams.x) {
            bubbleParams.x = targetX;
            bubbleParams.y = targetY;
            updateBubbleLayout();
            persistBubblePosition();
            return;
        }

        cancelSnapAnimation();
        int startX = bubbleParams.x;
        snapAnimator = ValueAnimator.ofFloat(0f, 1f);
        snapAnimator.setDuration(220L);
        snapAnimator.addUpdateListener(a -> {
            float fraction = (float) a.getAnimatedValue();
            bubbleParams.x = Math.round(startX + (targetX - startX) * fraction);
            bubbleParams.y = targetY;
            updateBubbleLayout();
            if (panelVisible) {
                positionPanel();
            }
        });
        snapAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                persistBubblePosition();
            }
        });
        snapAnimator.start();
    }

    private void cancelSnapAnimation() {
        if (snapAnimator != null) {
            snapAnimator.cancel();
            snapAnimator = null;
        }
    }

    private void persistBubblePosition() {
        if (bubbleParams != null) {
            prefs.setButtonPosition(bubbleParams.x, bubbleParams.y);
        }
    }

    private void updateBubbleLayout() {
        try {
            if (bubbleView != null && bubbleView.getParent() != null) {
                windowManager.updateViewLayout(bubbleView, bubbleParams);
            }
        } catch (Exception ignored) {
            // The view is gone.
        }
    }

    private int clampX(int x, int size) {
        return Math.max(0, Math.min(x, screenWidth - size));
    }

    private int clampY(int y, int size) {
        int top = Theme.dp(this, 8f);
        int bottom = Math.max(top, screenHeight - size - Theme.dp(this, 8f));
        return Math.max(top, Math.min(y, bottom));
    }

    private void readScreenMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            windowManager.getDefaultDisplay().getRealMetrics(metrics);
        } else {
            windowManager.getDefaultDisplay().getMetrics(metrics);
        }
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
    }

    // ------------------------------------------------------------- callbacks

    private void updateBridgeState() {
        boolean connected = prefs.isIntegrationConnected();
        if (bubbleView != null) {
            bubbleView.setIntegrationConnected(connected);
        }
        if (panelView != null) {
            panelView.setBridgeState(connected, prefs.getClientLabel());
        }
    }

    @Override
    public void onSnapshotChanged(MenuSnapshot snapshot) {
        if (bubbleView != null) {
            bubbleView.setAnimationsEnabled(snapshot.animationsEnabled);
            if (snapshot.buttonSizeDp != lastBubbleSizeDp) {
                resizeBubble(snapshot.buttonSizeDp);
            }
        }
        if (panelView != null) {
            panelView.setAnimationsEnabled(snapshot.animationsEnabled);
            panelView.setHapticsEnabled(snapshot.hapticsEnabled);
        }
        updateBridgeState();
    }

    private int windowSizeFor(int sizeDp) {
        return Theme.dp(this, sizeDp) + Theme.dp(this, GLOW_PADDING_DP) * 2;
    }

    private void resizeBubble(int sizeDp) {
        lastBubbleSizeDp = sizeDp;
        int sizePx = windowSizeFor(sizeDp);
        bubbleParams.width = sizePx;
        bubbleParams.height = sizePx;
        bubbleParams.x = clampX(bubbleParams.x, sizePx);
        bubbleParams.y = clampY(bubbleParams.y, sizePx);
        updateBubbleLayout();
        if (panelVisible) {
            positionPanel();
        }
    }

    @Override
    public void onCloseRequested() {
        hidePanel();
    }

    // --------------------------------------------------------------- destroy

    @Override
    public void onDestroy() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        cancelSnapAnimation();
        repository.removeListener(this);
        if (panelView != null) {
            panelView.release();
            detachPanel(panelView);
        }
        if (bubbleView != null) {
            try {
                windowManager.removeView(bubbleView);
            } catch (Exception ignored) {
                // Already removed.
            }
            bubbleView = null;
        }
        prefs.setMenuEnabled(false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ------------------------------------------------------------ entrypoints

    public static void start(Context context) {
        Intent intent = new Intent(context, OverlayService.class);
        intent.setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, OverlayService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static boolean canDrawOverlays(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || android.provider.Settings.canDrawOverlays(context);
    }
}
