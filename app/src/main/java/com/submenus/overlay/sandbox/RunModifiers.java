package com.submenus.overlay.sandbox;

import com.submenus.overlay.bridge.MenuSnapshot;

/**
 * Turns a {@link MenuSnapshot} into the numbers the runner actually uses.
 *
 * <p>Pure Java on purpose: it has no Android dependency, so the mapping between the menu
 * and the gameplay is covered by JVM unit tests instead of trusting the UI.</p>
 */
public final class RunModifiers {

    public final float speedMultiplier;
    public final float jumpMultiplier;
    public final boolean invincible;
    public final boolean coinMultiplierEnabled;
    public final int coinMultiplier;
    public final boolean keysCounterEnabled;

    private RunModifiers(MenuSnapshot snapshot) {
        this.speedMultiplier = snapshot.speedEnabled ? snapshot.speedMultiplier : 1f;
        this.jumpMultiplier = snapshot.highJumpEnabled ? snapshot.highJumpMultiplier : 1f;
        this.invincible = snapshot.invincibilityEnabled;
        this.coinMultiplierEnabled = snapshot.coinMultiplierEnabled;
        this.coinMultiplier = snapshot.coinMultiplier;
        this.keysCounterEnabled = snapshot.keysCounterEnabled;
    }

    public static RunModifiers from(MenuSnapshot snapshot) {
        return new RunModifiers(snapshot == null ? MenuSnapshot.defaults() : snapshot);
    }

    /** Neutral values: the game behaves exactly like the original. */
    public static RunModifiers none() {
        return from(MenuSnapshot.defaults());
    }

    /** Coins granted by a single pickup. */
    public int coinsPerPickup() {
        return coinMultiplierEnabled ? coinMultiplier : 1;
    }

    /** Horizontal speed for the given base speed. */
    public float horizontalSpeed(float baseSpeed) {
        return baseSpeed * speedMultiplier;
    }

    /** Initial jump impulse for the given base impulse. */
    public float jumpImpulse(float baseImpulse) {
        return baseImpulse * jumpMultiplier;
    }

    /** True when a collision should end the run. */
    public boolean obstaclesDamage() {
        return !invincible;
    }

    public boolean isVanilla() {
        return speedMultiplier == 1f && jumpMultiplier == 1f && !invincible
                && !coinMultiplierEnabled && !keysCounterEnabled;
    }

    @Override
    public String toString() {
        return "RunModifiers{speed=" + speedMultiplier + ", jump=" + jumpMultiplier
                + ", invincible=" + invincible + ", coins=" + coinsPerPickup() + "}";
    }
}
