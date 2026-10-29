package com.submenus.overlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.submenus.overlay.data.Prefs;

/**
 * Restores the floating menu after a reboot or an app update, but only when the
 * user had it enabled and the overlay permission is still granted.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        Prefs prefs = Prefs.get(context);
        if (prefs.isMenuEnabled() && OverlayService.canDrawOverlays(context)) {
            try {
                OverlayService.start(context);
            } catch (Exception ignored) {
                // Some OEMs block background foreground-service starts; the user can
                // always open the app and press "Start menu".
            }
        }
    }
}
