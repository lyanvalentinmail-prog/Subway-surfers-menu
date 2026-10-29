package com.submenus.overlay.bridge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;

import com.submenus.overlay.data.Prefs;

/**
 * Read-only window into the menu state.
 *
 * <pre>
 * content://com.submenus.overlay.settings/settings  -&gt; snapshot JSON
 * content://com.submenus.overlay.settings/health    -&gt; last heartbeat
 * </pre>
 *
 * Requires {@link BridgeContract#PERMISSION_READ} (a normal permission, so a
 * re-signed game simply declares it in its manifest).
 */
public class SettingsProvider extends ContentProvider {

    public static final String PATH_SETTINGS = "settings";
    public static final String PATH_HEALTH = "health";

    @Override
    public boolean onCreate() {
        return true;
    }

    private Prefs prefs() {
        return Prefs.get(getContext());
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String path = uri.getPath() == null ? "" : uri.getPath().replace("/", "");
        Prefs prefs = prefs();

        if (PATH_HEALTH.equals(path)) {
            MatrixCursor cursor = new MatrixCursor(new String[]{
                    BridgeContract.COLUMN_LAST_HEARTBEAT,
                    BridgeContract.COLUMN_CLIENT_LABEL,
                    BridgeContract.COLUMN_SCHEMA});
            cursor.addRow(new Object[]{
                    prefs.getLastHeartbeat(),
                    prefs.getClientLabel(),
                    BridgeContract.SCHEMA_VERSION});
            return cursor;
        }

        if (PATH_SETTINGS.equals(path) || path.isEmpty()) {
            String json = prefs.getSnapshotJson();
            if (json == null) {
                json = MenuSnapshot.defaults().toJsonString();
            }
            MatrixCursor cursor = new MatrixCursor(new String[]{
                    BridgeContract.COLUMN_JSON,
                    BridgeContract.COLUMN_UPDATED_AT,
                    BridgeContract.COLUMN_SCHEMA});
            cursor.addRow(new Object[]{json, prefs.getLastUpdate(), BridgeContract.SCHEMA_VERSION});
            return cursor;
        }

        throw new IllegalArgumentException("Unknown bridge path: " + uri);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Prefs prefs = prefs();
        Bundle result = new Bundle();

        if (BridgeContract.METHOD_HEARTBEAT.equals(method)) {
            String label = extras == null ? arg : extras.getString(BridgeContract.EXTRA_CLIENT_LABEL, arg);
            String pkg = extras == null ? null : extras.getString(BridgeContract.EXTRA_CLIENT_PACKAGE);
            prefs.setLastHeartbeat(System.currentTimeMillis());
            prefs.setClient(label, pkg);

            // Optional real telemetry: an integration may report the values it read
            // from the running game so the menu can display them instead of guessing.
            if (extras != null && extras.containsKey(BridgeContract.EXTRA_KEYS_COUNT)) {
                prefs.raw().edit()
                        .putInt(Prefs.KEY_REPORTED_KEYS, extras.getInt(BridgeContract.EXTRA_KEYS_COUNT, 0))
                        .apply();
            }
            if (extras != null && extras.containsKey(BridgeContract.EXTRA_COINS_COUNT)) {
                prefs.raw().edit()
                        .putInt(Prefs.KEY_REPORTED_COINS, extras.getInt(BridgeContract.EXTRA_COINS_COUNT, 0))
                        .apply();
            }

            result.putInt(BridgeContract.COLUMN_SCHEMA, BridgeContract.SCHEMA_VERSION);
            result.putLong(BridgeContract.COLUMN_LAST_HEARTBEAT, prefs.getLastHeartbeat());
            result.putString(BridgeContract.COLUMN_JSON, prefs.getSnapshotJson());
            return result;
        }

        if (BridgeContract.METHOD_SNAPSHOT.equals(method)) {
            result.putInt(BridgeContract.COLUMN_SCHEMA, BridgeContract.SCHEMA_VERSION);
            result.putLong(BridgeContract.COLUMN_UPDATED_AT, prefs.getLastUpdate());
            result.putString(BridgeContract.COLUMN_JSON, prefs.getSnapshotJson());
            return result;
        }

        return super.call(method, arg, extras);
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.submenus.settings";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("The menu bridge is read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("The menu bridge is read-only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("The menu bridge is read-only");
    }
}
