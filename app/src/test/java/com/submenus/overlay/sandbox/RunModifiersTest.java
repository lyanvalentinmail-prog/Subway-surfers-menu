package com.submenus.overlay.sandbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.submenus.overlay.bridge.MenuSnapshot;

import org.junit.Test;

/** Verifies that the menu state really maps onto gameplay numbers. */
public class RunModifiersTest {

    @Test
    public void defaultsAreVanilla() {
        RunModifiers mods = RunModifiers.none();
        assertTrue(mods.isVanilla());
        // Vanilla means the base values pass through untouched.
        assertEquals(100f, mods.horizontalSpeed(100f), 0.0001f);
        assertEquals(100f, mods.jumpImpulse(100f), 0.0001f);
        assertEquals(1, mods.coinsPerPickup());
        assertTrue(mods.obstaclesDamage());
    }

    @Test
    public void speedAndJumpOnlyApplyWhenEnabled() {
        MenuSnapshot off = MenuSnapshot.builder()
                .speedMultiplier(2.5f)
                .highJumpMultiplier(2.5f)
                .build();
        assertEquals(100f, RunModifiers.from(off).horizontalSpeed(100f), 0.0001f);
        assertEquals(100f, RunModifiers.from(off).jumpImpulse(100f), 0.0001f);

        MenuSnapshot on = MenuSnapshot.builder()
                .speedEnabled(true).speedMultiplier(2.5f)
                .highJumpEnabled(true).highJumpMultiplier(1.8f)
                .build();
        RunModifiers mods = RunModifiers.from(on);
        assertEquals(250f, mods.horizontalSpeed(100f), 0.0001f);
        assertEquals(180f, mods.jumpImpulse(100f), 0.0001f);
        assertFalse(mods.isVanilla());
    }

    @Test
    public void invincibilityStopsDamage() {
        RunModifiers mods = RunModifiers.from(
                MenuSnapshot.builder().invincibilityEnabled(true).build());
        assertTrue(mods.invincible);
        assertFalse(mods.obstaclesDamage());
    }

    @Test
    public void coinMultiplierAppliesOnlyWhenEnabledAndStaysInRange() {
        assertEquals(1, RunModifiers.from(
                MenuSnapshot.builder().coinMultiplier(12).build()).coinsPerPickup());

        assertEquals(12, RunModifiers.from(
                MenuSnapshot.builder().coinMultiplierEnabled(true).coinMultiplier(12).build())
                .coinsPerPickup());

        // Values coming from a malformed source are clamped by the snapshot itself.
        assertEquals(50, RunModifiers.from(
                MenuSnapshot.builder().coinMultiplierEnabled(true).coinMultiplier(9999).build())
                .coinsPerPickup());
        assertEquals(1, RunModifiers.from(
                MenuSnapshot.builder().coinMultiplierEnabled(true).coinMultiplier(-4).build())
                .coinsPerPickup());
    }

    @Test
    public void nullSnapshotFallsBackToVanilla() {
        RunModifiers mods = RunModifiers.from(null);
        assertTrue(mods.isVanilla());
        assertEquals(1, mods.coinsPerPickup());
    }
}
