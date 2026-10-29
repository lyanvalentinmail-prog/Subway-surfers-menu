package com.submenus.overlay.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** JVM tests for the value rules and contract shared by the menu and the integration. */
public class MenuLimitsTest {

    @Test
    public void clampKeepsValuesInsideRange() {
        assertEquals(MenuLimits.SPEED_MIN,
                MenuLimits.clamp(0.2f, MenuLimits.SPEED_MIN, MenuLimits.SPEED_MAX), 0.0001f);
        assertEquals(MenuLimits.SPEED_MAX,
                MenuLimits.clamp(9f, MenuLimits.SPEED_MIN, MenuLimits.SPEED_MAX), 0.0001f);
        assertEquals(1.5f,
                MenuLimits.clamp(1.5f, MenuLimits.SPEED_MIN, MenuLimits.SPEED_MAX), 0.0001f);
        assertEquals(MenuLimits.COIN_MIN, MenuLimits.clamp(0, MenuLimits.COIN_MIN, MenuLimits.COIN_MAX));
        assertEquals(MenuLimits.COIN_MAX, MenuLimits.clamp(999, MenuLimits.COIN_MIN, MenuLimits.COIN_MAX));
    }

    @Test
    public void snapToStepRoundsToTheNearestStep() {
        assertEquals(1.5f, MenuLimits.snapToStep(1.52f, MenuLimits.SPEED_MIN,
                MenuLimits.SPEED_MAX, MenuLimits.SPEED_STEP), 0.0001f);
        assertEquals(1.55f, MenuLimits.snapToStep(1.55f, MenuLimits.SPEED_MIN,
                MenuLimits.SPEED_MAX, MenuLimits.SPEED_STEP), 0.0001f);
        assertEquals(MenuLimits.SPEED_MAX, MenuLimits.snapToStep(4f, MenuLimits.SPEED_MIN,
                MenuLimits.SPEED_MAX, MenuLimits.SPEED_STEP), 0.0001f);
        assertEquals(MenuLimits.HIGH_JUMP_MIN, MenuLimits.snapToStep(0.1f,
                MenuLimits.HIGH_JUMP_MIN, MenuLimits.HIGH_JUMP_MAX, MenuLimits.HIGH_JUMP_STEP), 0.0001f);
    }

    @Test
    public void bridgeContractIsStable() {
        assertEquals("content://com.submenus.overlay.settings/settings", BridgeContract.URI_SETTINGS);
        assertEquals("content://com.submenus.overlay.settings/health", BridgeContract.URI_HEALTH);
        assertEquals("com.submenus.overlay.settings", BridgeContract.AUTHORITY);
        assertEquals("com.submenus.overlay.permission.READ_MOD_SETTINGS", BridgeContract.PERMISSION_READ);
        assertFalse(BridgeContract.ACTION_SETTINGS_CHANGED.isEmpty());
        assertEquals(1, BridgeContract.SCHEMA_VERSION);
        assertTrue(BridgeContract.HEARTBEAT_TIMEOUT_MS > 0L);
    }
}
