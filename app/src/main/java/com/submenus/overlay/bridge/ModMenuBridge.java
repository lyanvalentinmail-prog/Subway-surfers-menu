package com.submenus.overlay.bridge;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client side of the bridge. Drop this file (plus {@link BridgeContract},
 * {@link MenuLimits}, {@link MenuSnapshot} and {@link ModMenuListener}) into an
 * integration module and call:
 *
 * <pre>
 * // once, e.g. in Application.onCreate()
 * if (ModMenuBridge.isAvailable(context)) {
 *     ModMenuBridge.sendHeartbeat(context, "Subway Surfers");
 *     ModMenuBridge.registerListener(context, snapshot -&gt; applyToGame(snapshot));
 * }
 * </pre>
 *
 * The bridge never guesses: if {@link #isAvailable(Context)} is false there is no
 * menu installed and the integration must simply run the vanilla game.
 */
public final class ModMenuBridge {

    private static final String TAG = "ModMenuBridge";

    private static final Map<ModMenuListener, Registration> REGISTRATIONS = new ConcurrentHashMap<>();

    private ModMenuBridge() {
    }

    /** True when the menu app is installed and its provider answers. */
    public static boolean isAvailable(Context context) {
        if (context == null) {
            return false;
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    Uri.parse(BridgeContract.URI_SETTINGS),
                    new String[]{BridgeContract.COLUMN_JSON},
                    null, null, null);
            return cursor != null && cursor.moveToFirst();
        } catch (Exception e) {
            Log.d(TAG, "Menu provider not available: " + e.getClass().getSimpleName());
            return false;
        } finally {
            closeQuietly(cursor);
        }
    }

    /** Reads the current snapshot. Returns {@link MenuSnapshot#defaults()} if unreachable. */
    public static MenuSnapshot read(Context context) {
        if (context == null) {
            return MenuSnapshot.defaults();
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    Uri.parse(BridgeContract.URI_SETTINGS),
                    new String[]{BridgeContract.COLUMN_JSON},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String json = cursor.getString(0);
                return MenuSnapshot.fromJson(json);
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to read snapshot: " + e.getClass().getSimpleName());
        } finally {
            closeQuietly(cursor);
        }
        return MenuSnapshot.defaults();
    }

    /** Registers a listener for {@link BridgeContract#ACTION_SETTINGS_CHANGED}. */
    public static void registerListener(Context context, ModMenuListener listener) {
        if (context == null || listener == null || REGISTRATIONS.containsKey(listener)) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                String json = intent.getStringExtra(BridgeContract.EXTRA_SNAPSHOT_JSON);
                listener.onSnapshot(MenuSnapshot.fromJson(json));
            }
        };
        IntentFilter filter = new IntentFilter(BridgeContract.ACTION_SETTINGS_CHANGED);
        boolean registered = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, BridgeContract.PERMISSION_READ, null,
                        Context.RECEIVER_EXPORTED);
            } else {
                appContext.registerReceiver(receiver, filter, BridgeContract.PERMISSION_READ, null);
            }
            registered = true;
        } catch (Exception e) {
            Log.w(TAG, "Unable to register bridge listener", e);
        }
        if (registered) {
            REGISTRATIONS.put(listener, new Registration(appContext, receiver));
        }
    }

    /** Removes a listener previously added with {@link #registerListener}. */
    public static void unregisterListener(Context context, ModMenuListener listener) {
        Registration registration = REGISTRATIONS.remove(listener);
        if (registration != null) {
            try {
                registration.context.unregisterReceiver(registration.receiver);
            } catch (IllegalArgumentException ignored) {
                // Already unregistered.
            }
        }
    }

    /** Tells the menu that a real integration is alive, so it can show "connected". */
    public static void sendHeartbeat(Context context, String clientLabel) {
        sendHeartbeat(context, clientLabel, -1, -1);
    }

    /**
     * Heartbeat that also reports <b>real</b> values read from the running game, so the
     * menu can display them (used by the Keys Counter). Pass a negative number to omit
     * a value — the menu then shows "no data yet" instead of an invented zero.
     */
    public static void sendHeartbeat(Context context, String clientLabel, int keysCount, int coinsCount) {
        if (context == null) {
            return;
        }
        ContentResolver resolver = context.getContentResolver();
        Bundle extras = new Bundle();
        extras.putString(BridgeContract.EXTRA_CLIENT_LABEL, clientLabel);
        extras.putString(BridgeContract.EXTRA_CLIENT_PACKAGE, context.getPackageName());
        if (keysCount >= 0) {
            extras.putInt(BridgeContract.EXTRA_KEYS_COUNT, keysCount);
        }
        if (coinsCount >= 0) {
            extras.putInt(BridgeContract.EXTRA_COINS_COUNT, coinsCount);
        }
        try {
            resolver.call(Uri.parse(BridgeContract.URI_HEALTH), BridgeContract.METHOD_HEARTBEAT,
                    clientLabel, extras);
        } catch (Exception e) {
            Log.d(TAG, "Heartbeat failed: " + e.getClass().getSimpleName());
        }
    }

    /** Milliseconds since the last heartbeat sent by any integration, or -1. */
    public static long lastHeartbeatAge(Context context) {
        if (context == null) {
            return -1L;
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    Uri.parse(BridgeContract.URI_HEALTH),
                    new String[]{BridgeContract.COLUMN_LAST_HEARTBEAT},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                long last = cursor.getLong(0);
                return last <= 0L ? -1L : System.currentTimeMillis() - last;
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to read health: " + e.getClass().getSimpleName());
        } finally {
            closeQuietly(cursor);
        }
        return -1L;
    }

    private static void closeQuietly(Cursor cursor) {
        if (cursor != null) {
            try {
                cursor.close();
            } catch (Exception ignored) {
                // Nothing to do.
            }
        }
    }

    private static final class Registration {
        final Context context;
        final BroadcastReceiver receiver;

        Registration(Context context, BroadcastReceiver receiver) {
            this.context = context;
            this.receiver = receiver;
        }
    }
}
