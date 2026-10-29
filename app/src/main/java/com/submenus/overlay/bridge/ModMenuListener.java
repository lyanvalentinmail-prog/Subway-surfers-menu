package com.submenus.overlay.bridge;

/** Callback used by an integration to be notified of menu changes. */
public interface ModMenuListener {

    /**
     * Called on the main thread every time the user changes something in the menu.
     *
     * @param snapshot complete, already validated state of the menu.
     */
    void onSnapshot(MenuSnapshot snapshot);
}
