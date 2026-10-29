package com.submenus.overlay.ui;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

/**
 * Turns any window view into a draggable element that still reports taps.
 * The window is moved by updating {@link WindowManager.LayoutParams#x}/{@code y}.
 */
public final class DragController implements View.OnTouchListener {

    public interface Listener {
        void onTap(DragController controller);

        void onDragStart(DragController controller);

        void onDrag(DragController controller, int windowX, int windowY);

        void onDragEnd(DragController controller, int windowX, int windowY);
    }

    private final WindowManager windowManager;
    private final WindowManager.LayoutParams params;
    private final Listener listener;
    private final int touchSlop;

    private float downRawX;
    private float downRawY;
    private int startX;
    private int startY;
    private boolean dragging;
    private boolean moved;

    public DragController(WindowManager windowManager, WindowManager.LayoutParams params,
                          View view, Listener listener) {
        this.windowManager = windowManager;
        this.params = params;
        this.listener = listener;
        this.touchSlop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        view.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                startX = params.x;
                startY = params.y;
                dragging = false;
                moved = false;
                view.animate().cancel();
                return true;

            case MotionEvent.ACTION_MOVE: {
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!moved && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                    moved = true;
                    dragging = true;
                    if (listener != null) {
                        listener.onDragStart(this);
                    }
                }
                if (dragging) {
                    params.x = startX + Math.round(dx);
                    params.y = startY + Math.round(dy);
                    safeUpdate(view);
                    if (listener != null) {
                        listener.onDrag(this, params.x, params.y);
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
                if (dragging || moved) {
                    if (listener != null) {
                        listener.onDragEnd(this, params.x, params.y);
                    }
                } else {
                    view.performClick();
                    if (listener != null) {
                        listener.onTap(this);
                    }
                }
                dragging = false;
                moved = false;
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (dragging && listener != null) {
                    listener.onDragEnd(this, params.x, params.y);
                }
                dragging = false;
                moved = false;
                return true;

            default:
                return false;
        }
    }

    private void safeUpdate(View view) {
        try {
            windowManager.updateViewLayout(view, params);
        } catch (IllegalArgumentException ignored) {
            // The view was already removed from the window manager.
        }
    }

    public int getX() {
        return params.x;
    }

    public int getY() {
        return params.y;
    }

    public void setPosition(int x, int y) {
        params.x = x;
        params.y = y;
    }

    public WindowManager.LayoutParams getParams() {
        return params;
    }
}
