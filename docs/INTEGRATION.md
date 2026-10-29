# Guía de integración del puente

Este documento explica cómo un cliente (una integración del lado del juego) lee el estado
del menú flotante y lo aplica **donde el juego realmente lo lee**. Es código completo, no
pseudocódigo: podés copiar los archivos y compilar.

El menú **no** modifica procesos de terceros. Publica su estado y espera que el juego lo
consuma; si nadie lo consume, el panel muestra “Sin integración” y no inventa resultados.

---

## 1. Archivos del puente

Copiá estos cinco archivos del proyecto del menú a tu módulo de integración:

```
app/src/main/java/com/submenus/overlay/bridge/
├── BridgeContract.java     # constantes (authority, URIs, extras)
├── MenuLimits.java         # rangos y pasos válidos
├── MenuSnapshot.java       # modelo + JSON (usa org.json, incluido en Android)
├── ModMenuListener.java    # callback
└── ModMenuBridge.java      # cliente: consulta, heartbeat, listener
```

No dependen de nada más del proyecto (ni AndroidX ni recursos), así que compilan solos en
cualquier módulo Android.

## 2. Manifiesto del cliente

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permiso normal: se concede al instalar, sin diálogo. -->
    <uses-permission android:name="com.submenus.overlay.permission.READ_MOD_SETTINGS" />

    <application ... />
</manifest>
```

## 3. Cliente completo

```java
package com.example.gameintegration;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.submenus.overlay.bridge.MenuSnapshot;
import com.submenus.overlay.bridge.ModMenuBridge;

/**
 * Puente entre el menú flotante y el juego.
 * Llamá a {@link #attach(Context, Applier)} una vez, cuando el juego arranca.
 */
public final class ModMenuClient {

    private static final String TAG = "ModMenuClient";
    private static final long HEARTBEAT_INTERVAL_MS = 5_000L;

    /** Punto único donde el juego aplica los valores. */
    public interface Applier {
        void apply(MenuSnapshot snapshot);
    }

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static Applier applier;
    private static String clientLabel = "Subway Surfers";
    private static int keys = -1;
    private static int coins = -1;

    private static final ModMenuBridgeListener LISTENER = new ModMenuBridgeListener();

    private ModMenuClient() {
    }

    public static void attach(Context context, Applier gameApplier) {
        if (gameApplier == null) {
            return;
        }
        applier = gameApplier;
        if (!ModMenuBridge.isAvailable(context)) {
            Log.i(TAG, "Menú no instalado: el juego corre sin modificaciones.");
            return;
        }
        // Estado actual, por si el menú ya estaba abierto.
        gameApplier.apply(ModMenuBridge.read(context));

        ModMenuBridge.registerListener(context, LISTENER);
        scheduleHeartbeat(context.getApplicationContext());
    }

    public static void detach(Context context) {
        ModMenuBridge.unregisterListener(context, LISTENER);
        handler.removeCallbacksAndMessages(null);
        applier = null;
    }

    /** El juego informa sus valores reales; el menú solo los muestra. */
    public static void reportCounters(int keysCount, int coinsCount) {
        keys = keysCount;
        coins = coinsCount;
    }

    private static void scheduleHeartbeat(final Context context) {
        handler.removeCallbacksAndMessages(null);
        handler.post(new Runnable() {
            @Override
            public void run() {
                ModMenuBridge.sendHeartbeat(context, clientLabel, keys, coins);
                handler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
            }
        });
    }

    private static final class ModMenuBridgeListener
            implements com.submenus.overlay.bridge.ModMenuListener {
        @Override
        public void onSnapshot(MenuSnapshot snapshot) {
            if (applier != null) {
                applier.apply(snapshot);
            }
        }
    }
}
```

Uso desde el juego:

```java
ModMenuClient.attach(context, snapshot -> {
    speedScale = snapshot.speedEnabled ? snapshot.speedMultiplier : 1f;
    jumpScale = snapshot.highJumpEnabled ? snapshot.highJumpMultiplier : 1f;
    invincible = snapshot.invincibilityEnabled;
    coinMultiplier = snapshot.coinMultiplierEnabled ? snapshot.coinMultiplier : 1;
    showKeyHud = snapshot.keysCounterEnabled;
});

// cada vez que cambian los contadores reales:
ModMenuClient.reportCounters(game.keys(), game.coins());
```

## 4. Contrato

| Elemento | Valor |
| --- | --- |
| Authority | `com.submenus.overlay.settings` |
| Snapshot | `content://com.submenus.overlay.settings/settings` |
| Salud | `content://com.submenus.overlay.settings/health` |
| Permiso | `com.submenus.overlay.permission.READ_MOD_SETTINGS` |
| Broadcast de cambios | `com.submenus.overlay.action.SETTINGS_CHANGED` (requiere el permiso) |
| `call("heartbeat", …)` | Marca la integración como conectada |
| `call("snapshot", …)` | Devuelve el último JSON |

### Columnas del cursor

| URI | Columnas |
| --- | --- |
| `/settings` | `snapshot_json`, `updated_at`, `schema` |
| `/health` | `last_heartbeat`, `client_label`, `schema` |

### Extras del heartbeat

| Extra | Tipo | Efecto |
| --- | --- | --- |
| `client_label` | String | Nombre que muestra el panel |
| `client_package` | String | Paquete del cliente |
| `keys_count` | int | Valor real de llaves reportado |
| `coins_count` | int | Valor real de monedas reportado |

## 5. Reglas de oro

1. **Nunca apliques valores sin leer el snapshot.** Si `isAvailable()` es `false`, el juego
   debe comportarse exactamente igual que la versión original.
2. **Respetá los límites** de `MenuLimits` (×1–×3 velocidad y salto, ×1–×50 monedas).
3. **No inventes telemetría.** Si no tenés el dato de llaves, no lo reportes: el menú
   mostrará “Sin datos todavía”.
4. **Los valores son intenciones del usuario**, no efectos mágicos: aplicá cada uno donde el
   juego lea la variable correspondiente (velocidad del corredor, impulso de salto, flag de
   invencibilidad, multiplicador de monedas, contador de llaves).
5. Si el menú se desinstala, `isAvailable()` pasa a `false`: desregistrá el listener y
   volvé a los valores originales.

## 6. Probar sin tocar un juego

1. Instalá la APK del menú, iniciá el menú flotante y abrí **Ver datos del puente** en la app.
2. Ese inspector es un cliente real del puente: muestra el JSON exacto y si un heartbeat
   está activo.
3. Con `adb logcat -s ModMenuBridge ModMenuClient` podés seguir la conversación entre
   ambos lados.
