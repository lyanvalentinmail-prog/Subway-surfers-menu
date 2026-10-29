package com.submenus.overlay.sandbox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import com.submenus.overlay.bridge.MenuSnapshot;
import com.submenus.overlay.bridge.ModMenuBridge;
import com.submenus.overlay.data.MenuRepository;
import com.submenus.overlay.ui.Theme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * A small, self-contained endless runner used as a <b>real</b> host for the mod menu.
 *
 * <p>This is the part that proves integration: the same menu writes into
 * {@link MenuRepository} and this view reads those values in-process and applies them to
 * actual gameplay (speed, jump impulse, invincibility, coin multiplier, key counter).
 * At the same time it reports the counters back through
 * {@link ModMenuBridge#sendHeartbeat}, so the floating overlay can show how many keys the
 * run really collected.</p>
 */
public class RunGameView extends View implements MenuRepository.Listener {

    public interface HudListener {
        void onHud(int coins, int keys, int score, boolean gameOver);
    }

    private static final int TYPE_OBSTACLE_LOW = 0;
    private static final int TYPE_OBSTACLE_TALL = 1;
    private static final int TYPE_COIN = 2;
    private static final int TYPE_KEY = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MenuRepository repository;

    private final List<Entity> entities = new ArrayList<>();

    // Live gameplay modifiers read from the menu (pure mapping, unit tested).
    private RunModifiers mods = RunModifiers.none();

    // Game state.
    private boolean running;
    private long lastFrameNanos;
    private float groundY;
    private float playerX;
    private float playerW;
    private float playerH;
    private float jumpHeight;
    private float verticalSpeed;
    private boolean onGround = true;
    private float invincibleFlash;
    private int score;
    private int coins;
    private int keys;
    private boolean gameOver;
    private float spawnTimer;
    private float coinTimer;
    private float keyTimer;
    private float reportTimer;
    private float timeAlive;

    private HudListener hudListener;

    private final Runnable frame = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            long now = System.nanoTime();
            float dt = lastFrameNanos == 0L ? 0.016f : (now - lastFrameNanos) / 1_000_000_000f;
            lastFrameNanos = now;
            dt = Math.min(dt, 0.05f);
            update(dt);
            invalidate();
            if (running) {
                handler.postDelayed(this, 16L);
            }
        }
    };

    public RunGameView(Context context) {
        super(context);
        this.repository = MenuRepository.get(context);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        applySnapshot(repository.snapshot());
        repository.addListener(this);
        resetRun();
    }

    public void setHudListener(HudListener listener) {
        this.hudListener = listener;
    }

    // ------------------------------------------------------------------ engine

    public void resume() {
        if (running) {
            return;
        }
        running = true;
        lastFrameNanos = 0L;
        handler.post(frame);
    }

    public void pause() {
        running = false;
        handler.removeCallbacks(frame);
    }

    public void resetRun() {
        entities.clear();
        score = 0;
        coins = 0;
        keys = 0;
        gameOver = false;
        jumpHeight = 0f;
        verticalSpeed = 0f;
        onGround = true;
        spawnTimer = 0.6f;
        coinTimer = 1.2f;
        keyTimer = 6f;
        timeAlive = 0f;
        notifyHud();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    private void update(float dt) {
        if (gameOver) {
            return;
        }
        timeAlive += dt;
        invincibleFlash = Math.max(0f, invincibleFlash - dt * 3f);

        // Base speed grows slowly with distance, then the menu multiplier on top.
        float baseSpeed = Theme.dp(getContext(), 170f) * (1f + Math.min(1.2f, timeAlive * 0.012f));
        float speed = mods.horizontalSpeed(baseSpeed);

        score += Math.round(dt * 12f * mods.speedMultiplier);

        // Jump physics: the menu multiplier scales the impulse (High Jump).
        if (!onGround) {
            jumpHeight += verticalSpeed * dt;
            verticalSpeed -= Theme.dp(getContext(), 2400f) * dt;
            if (jumpHeight <= 0f) {
                jumpHeight = 0f;
                verticalSpeed = 0f;
                onGround = true;
            }
        }

        spawnTimer -= dt;
        if (spawnTimer <= 0f) {
            spawnObstacle();
            float gap = Math.max(0.55f, 1.35f - mods.speedMultiplier * 0.22f);
            spawnTimer = gap + random.nextFloat() * 0.45f;
        }

        coinTimer -= dt;
        if (coinTimer <= 0f) {
            spawnPickup(TYPE_COIN);
            coinTimer = 0.6f + random.nextFloat() * 0.8f;
        }

        keyTimer -= dt;
        if (keyTimer <= 0f) {
            spawnPickup(TYPE_KEY);
            keyTimer = 5f + random.nextFloat() * 4f;
        }

        Iterator<Entity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            entity.x -= speed * dt;
            if (entity.x + entity.w < -Theme.dp(getContext(), 40f)) {
                iterator.remove();
                continue;
            }
            if (intersectsPlayer(entity)) {
                iterator.remove();
                handlePickup(entity);
            }
        }

        reportTimer -= dt;
        if (reportTimer <= 0f) {
            reportTimer = 1f;
            // Report the REAL counters of this run to the menu bridge.
            ModMenuBridge.sendHeartbeat(getContext(), "Sandbox Run", keys, coins);
        }
        notifyHud();
    }

    private void handlePickup(Entity entity) {
        switch (entity.type) {
            case TYPE_COIN:
                coins += mods.coinsPerPickup();
                break;
            case TYPE_KEY:
                keys += 1;
                break;
            default:
                if (mods.invincible) {
                    invincibleFlash = 1f;
                    score += 25;
                } else {
                    gameOver = true;
                    notifyHud();
                }
                break;
        }
    }

    private boolean intersectsPlayer(Entity entity) {
        float playerTop = groundY - playerH - jumpHeight;
        float playerBottom = playerTop + playerH;
        float playerLeft = playerX;
        float playerRight = playerX + playerW;
        return entity.x < playerRight && entity.x + entity.w > playerLeft
                && entity.y < playerBottom && entity.y + entity.h > playerTop;
    }

    private void spawnObstacle() {
        float tall = random.nextFloat();
        float h = tall > 0.66f
                ? Theme.dp(getContext(), 74f)
                : Theme.dp(getContext(), 42f);
        float w = Theme.dp(getContext(), tall > 0.66f ? 34f : 46f);
        Entity entity = new Entity();
        entity.type = tall > 0.66f ? TYPE_OBSTACLE_TALL : TYPE_OBSTACLE_LOW;
        entity.w = w;
        entity.h = h;
        entity.x = getWidth() + Theme.dp(getContext(), 20f);
        entity.y = groundY - h;
        entities.add(entity);
    }

    private void spawnPickup(int type) {
        Entity entity = new Entity();
        entity.type = type;
        float size = Theme.dp(getContext(), type == TYPE_KEY ? 22f : 18f);
        entity.w = size;
        entity.h = size;
        entity.x = getWidth() + Theme.dp(getContext(), 20f);
        // Some pickups float high enough that you must jump for them.
        boolean high = random.nextFloat() > 0.55f;
        entity.y = groundY - Theme.dp(getContext(), high ? 96f : 34f) - size;
        entities.add(entity);
    }

    // ------------------------------------------------------------------- input

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (gameOver) {
                resetRun();
                return true;
            }
            if (onGround) {
                onGround = false;
                // High Jump scales the initial impulse.
                verticalSpeed = mods.jumpImpulse(Theme.dp(getContext(), 1150f));
                performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    // -------------------------------------------------------------------- draw

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        groundY = h - Theme.dp(getContext(), 96f);
        playerX = Math.round(w * 0.18f);
        playerW = Theme.dp(getContext(), 40f);
        playerH = Theme.dp(getContext(), 52f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        if (w == 0 || h == 0) {
            return;
        }
        if (groundY <= 0f) {
            groundY = h - Theme.dp(getContext(), 96f);
            playerX = Math.round(w * 0.18f);
            playerW = Theme.dp(getContext(), 40f);
            playerH = Theme.dp(getContext(), 52f);
        }

        drawSky(canvas, w, h);
        drawGround(canvas, w);
        drawTrackLines(canvas, w);

        for (Entity entity : entities) {
            switch (entity.type) {
                case TYPE_COIN:
                    drawCoin(canvas, entity);
                    break;
                case TYPE_KEY:
                    drawKey(canvas, entity);
                    break;
                default:
                    drawObstacle(canvas, entity);
                    break;
            }
        }

        drawPlayer(canvas);
        drawHud(canvas, w);

        if (gameOver) {
            drawGameOver(canvas, w, h);
        }
    }

    private void drawSky(Canvas canvas, int w, int h) {
        paint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{Color.parseColor("#0E2038"), Color.parseColor("#0A1524"), Theme.BG_DEEP},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // City blocks on the horizon, scrolling with the run.
        float offset = (timeAlive * 18f) % Theme.dp(getContext(), 70f);
        paint.setColor(Color.parseColor("#122238"));
        for (float x = -offset; x < w; x += Theme.dp(getContext(), 70f)) {
            float building = Theme.dp(getContext(), 60f + ((int) (x / 70) % 3) * 26f);
            rect.set(x, groundY - building - Theme.dp(getContext(), 60f),
                    x + Theme.dp(getContext(), 52f), groundY);
            canvas.drawRoundRect(rect, Theme.dp(getContext(), 6f), Theme.dp(getContext(), 6f), paint);
        }

        // Faint gradient glow behind the player lane.
        paint.setShader(new LinearGradient(0, groundY - Theme.dp(getContext(), 200f), 0, groundY,
                Theme.withAlpha(Theme.BLUE, 0.10f), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawRect(0, groundY - Theme.dp(getContext(), 200f), w, groundY, paint);
        paint.setShader(null);
    }

    private void drawGround(Canvas canvas, int w) {
        paint.setColor(Color.parseColor("#0F1B2B"));
        canvas.drawRect(0, groundY, w, getHeight(), paint);

        paint.setShader(new LinearGradient(0, groundY, w, groundY,
                new int[]{Theme.GREEN, Theme.BLUE, Theme.ORANGE}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, groundY, w, groundY + Theme.dp(getContext(), 3f), paint);
        paint.setShader(null);
    }

    private void drawTrackLines(Canvas canvas, int w) {
        paint.setColor(Theme.withAlpha(Theme.TEXT_DIM, 0.18f));
        float offset = (timeAlive * Theme.dp(getContext(), 170f) * mods.speedMultiplier)
                % Theme.dp(getContext(), 64f);
        for (float x = -offset; x < w; x += Theme.dp(getContext(), 64f)) {
            canvas.drawRect(x, groundY + Theme.dp(getContext(), 26f),
                    x + Theme.dp(getContext(), 26f),
                    groundY + Theme.dp(getContext(), 28f), paint);
        }
    }

    private void drawObstacle(Canvas canvas, Entity entity) {
        paint.setColor(Color.parseColor("#1D3B57"));
        rect.set(entity.x, entity.y, entity.x + entity.w, entity.y + entity.h);
        canvas.drawRoundRect(rect, Theme.dp(getContext(), 8f), Theme.dp(getContext(), 8f), paint);

        paint.setColor(Theme.withAlpha(Theme.ORANGE, 0.85f));
        canvas.drawRect(entity.x, entity.y, entity.x + entity.w,
                entity.y + Theme.dp(getContext(), 5f), paint);
        paint.setColor(Theme.withAlpha(Theme.TEXT_MUTED, 0.35f));
        canvas.drawRect(entity.x + Theme.dp(getContext(), 6f),
                entity.y + Theme.dp(getContext(), 14f),
                entity.x + entity.w - Theme.dp(getContext(), 6f),
                entity.y + Theme.dp(getContext(), 16f), paint);
    }

    private void drawCoin(Canvas canvas, Entity entity) {
        float cx = entity.x + entity.w / 2f;
        float cy = entity.y + entity.h / 2f;
        paint.setColor(Theme.withAlpha(Theme.ORANGE, 0.25f));
        canvas.drawCircle(cx, cy, entity.w * 0.9f, paint);
        paint.setColor(Theme.ORANGE);
        canvas.drawCircle(cx, cy, entity.w / 2f, paint);
        paint.setColor(Theme.withAlpha(Theme.BG_DEEP, 0.55f));
        canvas.drawCircle(cx, cy, entity.w / 5f, paint);
    }

    private void drawKey(Canvas canvas, Entity entity) {
        float cx = entity.x + entity.w / 2f;
        float cy = entity.y + entity.h / 2f;
        paint.setColor(Theme.withAlpha(Theme.GREEN, 0.25f));
        canvas.drawCircle(cx, cy, entity.w * 0.9f, paint);
        paint.setColor(Theme.GREEN_LIGHT);
        canvas.drawCircle(cx, cy - entity.h * 0.18f, entity.w * 0.30f, paint);
        rect.set(cx - entity.w * 0.07f, cy, cx + entity.w * 0.07f, cy + entity.h * 0.42f);
        canvas.drawRect(rect, paint);
        rect.set(cx + entity.w * 0.05f, cy + entity.h * 0.20f,
                cx + entity.w * 0.26f, cy + entity.h * 0.30f);
        canvas.drawRect(rect, paint);
    }

    private void drawPlayer(Canvas canvas) {
        float top = groundY - playerH - jumpHeight;

        // Shadow shrinks while jumping.
        float shadowScale = 1f - Math.min(0.6f, jumpHeight / Theme.dp(getContext(), 400f));
        paint.setColor(Theme.withAlpha(Color.BLACK, 0.35f * shadowScale));
        rect.set(playerX, groundY - Theme.dp(getContext(), 5f),
                playerX + playerW, groundY + Theme.dp(getContext(), 3f));
        canvas.drawOval(rect, paint);

        paint.setShader(new LinearGradient(playerX, top, playerX + playerW, top + playerH,
                Theme.GREEN, Theme.BLUE_DEEP, Shader.TileMode.CLAMP));
        rect.set(playerX, top, playerX + playerW, top + playerH);
        canvas.drawRoundRect(rect, Theme.dp(getContext(), 12f), Theme.dp(getContext(), 12f), paint);
        paint.setShader(null);

        // Visor.
        paint.setColor(Theme.withAlpha(Theme.BG_DEEP, 0.75f));
        rect.set(playerX + Theme.dp(getContext(), 8f), top + Theme.dp(getContext(), 12f),
                playerX + playerW - Theme.dp(getContext(), 8f), top + Theme.dp(getContext(), 26f));
        canvas.drawRoundRect(rect, Theme.dp(getContext(), 6f), Theme.dp(getContext(), 6f), paint);

        // Invincibility aura (real: while the menu flag is on, obstacles do nothing).
        if (mods.invincible) {
            float pulse = 0.55f + 0.35f * (float) Math.sin(timeAlive * 6f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Theme.dp(getContext(), 3f));
            paint.setColor(Theme.withAlpha(Theme.GREEN_LIGHT, pulse));
            canvas.drawRoundRect(rect, Theme.dp(getContext(), 12f), Theme.dp(getContext(), 12f), paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if (invincibleFlash > 0f) {
            paint.setColor(Theme.withAlpha(Theme.ORANGE, invincibleFlash * 0.5f));
            canvas.drawCircle(playerX + playerW / 2f, top + playerH / 2f,
                    playerW * (1.4f - invincibleFlash * 0.4f), paint);
        }
    }

    private void drawHud(Canvas canvas, int w) {
        textPaint.setTextSize(Theme.sp(getContext(), 13f));
        textPaint.setColor(Theme.TEXT);
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(String.format(Locale.US, "SCORE %d", score),
                Theme.dp(getContext(), 16f), Theme.dp(getContext(), 34f), textPaint);

        drawPill(canvas, w - Theme.dp(getContext(), 122f), Theme.dp(getContext(), 14f),
                Theme.dp(getContext(), 106f), Theme.dp(getContext(), 30f),
                Theme.withAlpha(Theme.ORANGE, 0.16f), Theme.ORANGE,
                String.format(Locale.US, "\u25CF %d", coins));

        drawPill(canvas, w - Theme.dp(getContext(), 122f), Theme.dp(getContext(), 50f),
                Theme.dp(getContext(), 106f), Theme.dp(getContext(), 30f),
                Theme.withAlpha(Theme.GREEN, 0.16f), Theme.GREEN_LIGHT,
                String.format(Locale.US, "\u2691 %d", keys));

        if (mods.keysCounterEnabled) {
            textPaint.setTextSize(Theme.sp(getContext(), 10f));
            textPaint.setColor(Theme.TEXT_MUTED);
            canvas.drawText("KEYS COUNTER ON",
                    Theme.dp(getContext(), 16f), Theme.dp(getContext(), 52f), textPaint);
        }
    }

    private void drawPill(Canvas canvas, float x, float y, float w, float h,
                          int fill, int strokeColor, String text) {
        rect.set(x, y, x + w, y + h);
        paint.setColor(fill);
        canvas.drawRoundRect(rect, h / 2f, h / 2f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Theme.dp(getContext(), 1f));
        paint.setColor(Theme.withAlpha(strokeColor, 0.5f));
        canvas.drawRoundRect(rect, h / 2f, h / 2f, paint);
        paint.setStyle(Paint.Style.FILL);

        textPaint.setTextSize(Theme.sp(getContext(), 12f));
        textPaint.setColor(strokeColor);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, x + w / 2f, y + h / 2f + Theme.dp(getContext(), 4f), textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawGameOver(Canvas canvas, int w, int h) {
        paint.setColor(Theme.withAlpha(Theme.BG_DEEP, 0.78f));
        canvas.drawRect(0, 0, w, h, paint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Theme.ORANGE);
        textPaint.setTextSize(Theme.sp(getContext(), 24f));
        textPaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        canvas.drawText("RUN OVER", w / 2f, h / 2f - Theme.dp(getContext(), 20f), textPaint);

        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textPaint.setTextSize(Theme.sp(getContext(), 14f));
        textPaint.setColor(Theme.TEXT);
        canvas.drawText(String.format(Locale.US, "Score %d · Coins %d · Keys %d",
                score, coins, keys), w / 2f, h / 2f + Theme.dp(getContext(), 12f), textPaint);

        textPaint.setTextSize(Theme.sp(getContext(), 12f));
        textPaint.setColor(Theme.TEXT_MUTED);
        canvas.drawText("Toca para volver a correr", w / 2f,
                h / 2f + Theme.dp(getContext(), 40f), textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    // ------------------------------------------------------------- menu bridge

    @Override
    public void onSnapshotChanged(MenuSnapshot snapshot) {
        applySnapshot(snapshot);
        invalidate();
    }

    private void applySnapshot(MenuSnapshot snapshot) {
        mods = RunModifiers.from(snapshot);
    }

    private void notifyHud() {
        if (hudListener != null) {
            hudListener.onHud(coins, keys, score, gameOver);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        pause();
        repository.removeListener(this);
        super.onDetachedFromWindow();
    }

    private static final class Entity {
        int type;
        float x;
        float y;
        float w;
        float h;
    }
}
