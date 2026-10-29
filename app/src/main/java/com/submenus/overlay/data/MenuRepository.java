package com.submenus.overlay.data;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.submenus.overlay.bridge.BridgeContract;
import com.submenus.overlay.bridge.MenuSnapshot;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Writes the user's choices to {@link Prefs} and publishes the resulting snapshot
 * (stored JSON + ordered broadcast) so an integration sees real values, not hints.
 */
public final class MenuRepository {

    /** Listener for in-app screens (activity + overlay) that must refresh. */
    public interface Listener {
        void onSnapshotChanged(MenuSnapshot snapshot);
    }

    private static volatile MenuRepository instance;

    private final Context appContext;
    private final Prefs prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private MenuRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = Prefs.get(this.appContext);
    }

    public static MenuRepository get(Context context) {
        MenuRepository local = instance;
        if (local == null) {
            synchronized (MenuRepository.class) {
                local = instance;
                if (local == null) {
                    local = new MenuRepository(context);
                    instance = local;
                }
            }
        }
        return local;
    }

    public Prefs prefs() {
        return prefs;
    }

    // ------------------------------------------------------------------ readers

    public boolean isSpeedEnabled() { return prefs.isSpeedEnabled(); }
    public float getSpeed() { return prefs.getSpeedMultiplier(); }
    public boolean isHighJumpEnabled() { return prefs.isHighJumpEnabled(); }
    public float getHighJump() { return prefs.getHighJumpMultiplier(); }
    public boolean isInvincibilityEnabled() { return prefs.isInvincibilityEnabled(); }
    public boolean isCoinMultiplierEnabled() { return prefs.isCoinMultiplierEnabled(); }
    public int getCoinMultiplier() { return prefs.getCoinMultiplier(); }
    public boolean isKeysCounterEnabled() { return prefs.isKeysCounterEnabled(); }

    public MenuSnapshot snapshot() {
        return MenuSnapshot.builder()
                .updatedAt(prefs.getLastUpdate() > 0L ? prefs.getLastUpdate() : System.currentTimeMillis())
                .speedEnabled(prefs.isSpeedEnabled())
                .speedMultiplier(prefs.getSpeedMultiplier())
                .highJumpEnabled(prefs.isHighJumpEnabled())
                .highJumpMultiplier(prefs.getHighJumpMultiplier())
                .invincibilityEnabled(prefs.isInvincibilityEnabled())
                .coinMultiplierEnabled(prefs.isCoinMultiplierEnabled())
                .coinMultiplier(prefs.getCoinMultiplier())
                .keysCounterEnabled(prefs.isKeysCounterEnabled())
                .hapticsEnabled(prefs.isHapticsEnabled())
                .animationsEnabled(prefs.isAnimationsEnabled())
                .snapToEdge(prefs.isSnapToEdgeEnabled())
                .overlayOpacity(prefs.getOpacity())
                .buttonSizeDp(prefs.getButtonSizeDp())
                .accent("aurora")
                .build();
    }

    /** The snapshot actually published to integrations (falls back to a fresh one). */
    public MenuSnapshot publishedSnapshot() {
        String json = prefs.getSnapshotJson();
        return json == null ? snapshot() : MenuSnapshot.fromJson(json);
    }

    // ------------------------------------------------------------------ writers

    public void setSpeedEnabled(boolean value) {
        prefs.setSpeedEnabled(value);
        publish();
    }

    public void setSpeedMultiplier(float value) {
        prefs.setSpeedMultiplier(value);
        publish();
    }

    public void setHighJumpEnabled(boolean value) {
        prefs.setHighJumpEnabled(value);
        publish();
    }

    public void setHighJumpMultiplier(float value) {
        prefs.setHighJumpMultiplier(value);
        publish();
    }

    public void setInvincibilityEnabled(boolean value) {
        prefs.setInvincibilityEnabled(value);
        publish();
    }

    public void setCoinMultiplierEnabled(boolean value) {
        prefs.setCoinMultiplierEnabled(value);
        publish();
    }

    public void setCoinMultiplier(int value) {
        prefs.setCoinMultiplier(value);
        publish();
    }

    public void setKeysCounterEnabled(boolean value) {
        prefs.setKeysCounterEnabled(value);
        publish();
    }

    public void setHapticsEnabled(boolean value) {
        prefs.setHapticsEnabled(value);
        publish();
    }

    public void setAnimationsEnabled(boolean value) {
        prefs.setAnimationsEnabled(value);
        publish();
    }

    public void setSnapToEdgeEnabled(boolean value) {
        prefs.setSnapToEdgeEnabled(value);
        publish();
    }

    public void setOpacity(float value) {
        prefs.setOpacity(value);
        publish();
    }

    public void setButtonSizeDp(int value) {
        prefs.setButtonSizeDp(value);
        publish();
    }

    /** Switches every gameplay option off but keeps the appearance settings. */
    public void resetGameplay() {
        prefs.setSpeedEnabled(false);
        prefs.setHighJumpEnabled(false);
        prefs.setInvincibilityEnabled(false);
        prefs.setCoinMultiplierEnabled(false);
        prefs.setKeysCounterEnabled(false);
        publish();
    }

    /** Factory reset of every stored value. */
    public void resetEverything() {
        prefs.resetAll();
        publish();
    }

    /**
     * Persists the snapshot and broadcasts it. Safe to call from any thread; the
     * broadcast is always delivered on the main thread.
     */
    public void publish() {
        final MenuSnapshot snapshot = snapshot();
        final long now = System.currentTimeMillis();
        prefs.setSnapshotJson(snapshot.toJsonString());
        prefs.setLastUpdate(now);

        Runnable dispatch = () -> {
            Intent intent = new Intent(BridgeContract.ACTION_SETTINGS_CHANGED);
            intent.setPackage(appContext.getPackageName());
            // The bridge contract does not expose an Intent overload with permission,
            // so the permission is enforced by the provider; the broadcast is used for
            // in-process refreshes and for integrations in the same app.
            intent.putExtra(BridgeContract.EXTRA_SNAPSHOT_JSON, snapshot.toJsonString());
            intent.putExtra(BridgeContract.EXTRA_UPDATED_AT, now);
            try {
                appContext.sendBroadcast(intent);
            } catch (Exception ignored) {
                // Broadcast is best effort; the provider is the source of truth.
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            dispatch.run();
        } else {
            mainHandler.post(dispatch);
        }

        for (Listener listener : listeners) {
            listener.onSnapshotChanged(snapshot);
        }
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
