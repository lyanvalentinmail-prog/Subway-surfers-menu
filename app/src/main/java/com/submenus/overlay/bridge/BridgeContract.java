package com.submenus.overlay.bridge;

/**
 * Shared constants of the mod-menu integration bridge.
 *
 * <p>The overlay application only <b>stores and publishes</b> the user's intents
 * (speed multiplier, high jump, invincibility, coin multiplier, keys counter).
 * A game integration — normally compiled into the game process, or a separate
 * app holding {@link #PERMISSION_READ} — reads that state through the
 * {@code ContentProvider} and decides what to do with it. Nothing is faked:
 * if no integration is connected the menu says so.</p>
 */
public final class BridgeContract {

    /** Authority of the settings provider exposed by this app. */
    public static final String AUTHORITY = "com.submenus.overlay.settings";

    /** URI that returns the current snapshot: content://AUTHORITY/settings */
    public static final String URI_SETTINGS = "content://" + AUTHORITY + "/settings";

    /** URI that returns the integration heartbeat: content://AUTHORITY/health */
    public static final String URI_HEALTH = "content://" + AUTHORITY + "/health";

    /** Normal permission required to read the bridge. */
    public static final String PERMISSION_READ = "com.submenus.overlay.permission.READ_MOD_SETTINGS";

    /** Broadcast sent by the menu every time the user changes something. */
    public static final String ACTION_SETTINGS_CHANGED = "com.submenus.overlay.action.SETTINGS_CHANGED";

    /** Broadcast an integration sends so the menu can show a real "connected" state. */
    public static final String ACTION_INTEGRATION_HEARTBEAT = "com.submenus.overlay.action.INTEGRATION_HEARTBEAT";

    public static final String EXTRA_SNAPSHOT_JSON = "snapshot_json";
    public static final String EXTRA_UPDATED_AT = "updated_at";
    public static final String EXTRA_CLIENT_LABEL = "client_label";
    public static final String EXTRA_CLIENT_PACKAGE = "client_package";

    /**
     * Optional, <b>real</b> telemetry an integration may attach to a heartbeat.
     * The menu only displays what was actually reported; it never invents a value.
     */
    public static final String EXTRA_KEYS_COUNT = "keys_count";
    public static final String EXTRA_COINS_COUNT = "coins_count";

    /** ContentProvider.call() method names. */
    public static final String METHOD_HEARTBEAT = "heartbeat";
    public static final String METHOD_SNAPSHOT = "snapshot";

    /** Cursor columns returned by {@link #URI_SETTINGS} and {@link #URI_HEALTH}. */
    public static final String COLUMN_JSON = "snapshot_json";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_SCHEMA = "schema";
    public static final String COLUMN_LAST_HEARTBEAT = "last_heartbeat";
    public static final String COLUMN_CLIENT_LABEL = "client_label";

    /** Version of the JSON schema published by this app. */
    public static final int SCHEMA_VERSION = 1;

    /** A heartbeat older than this (ms) means "not connected". */
    public static final long HEARTBEAT_TIMEOUT_MS = 15_000L;

    private BridgeContract() {
    }
}
