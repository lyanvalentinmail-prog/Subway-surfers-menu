package com.submenus.overlay.sandbox;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.submenus.overlay.R;
import com.submenus.overlay.data.MenuRepository;
import com.submenus.overlay.ui.EmbeddedMenu;

/**
 * The game screen with the mod menu <b>inside the app</b>: {@link RunGameView} runs the
 * gameplay and {@link EmbeddedMenu} floats the same Player / Collectibles / Settings panel
 * over it, with no overlay permission and no background service.
 */
public class SandboxActivity extends Activity {

    private RunGameView gameView;
    private EmbeddedMenu menu;
    private TextView subtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sandbox);

        gameView = findViewById(R.id.game_view);
        subtitle = findViewById(R.id.sandbox_subtitle);

        gameView.setHudListener((coins, keys, score, gameOver) -> subtitle.setText(
                getString(R.string.sandbox_hud, score, coins, keys)));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // The menu lives inside this activity.
        menu = EmbeddedMenu.attach(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
        if (menu != null) {
            menu.refreshBridgeState();
        }
    }

    @Override
    protected void onPause() {
        gameView.pause();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (menu != null && menu.isPanelVisible()) {
            menu.hidePanel();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (menu != null) {
            menu.detach();
            menu = null;
        }
        super.onDestroy();
    }
}
