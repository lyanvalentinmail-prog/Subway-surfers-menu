package com.submenus.overlay;

import android.app.Application;

import com.submenus.overlay.data.MenuRepository;

/** Application entry point: makes sure the bridge always serves a valid snapshot. */
public class MenuApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MenuRepository repository = MenuRepository.get(this);
        if (repository.prefs().getSnapshotJson() == null) {
            repository.publish();
        }
    }
}
