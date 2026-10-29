# Subway Surfers Mod Menu

![Build APK](https://github.com/lyanvalentinmail-prog/Subway-surfers-menu/actions/workflows/build-apk.yml/badge.svg)

Menú flotante **premium en tema oscuro** para Android, escrito en **Java puro + Gradle**,
pensado para acompañar una partida de Subway Surfers: un botón circular arrastrable abre un
panel con interruptores animados (verdes, azules y naranjas), tres categorías y guardado
local automático.

La APK se compila **desde GitHub, sin computadora**, con el workflow incluido: entras a la
pestaña *Actions*, ejecutas el build y descargas el `.apk` desde *Artifacts* (o desde
*Releases*).

---

## ⚠️ Qué hace y qué no hace (leé esto primero)

Este proyecto es **un menú flotante real con un puente de integración real**. No es una APK
pirata de Subway Surfers ni un inyector mágico:

| Sí hace | No hace |
| --- | --- |
| Dibuja un overlay real (`TYPE_APPLICATION_OVERLAY`) sobre cualquier app | No modifica ni incluye el APK del juego |
| Guarda tus preferencias y las publica en un `ContentProvider` consultable | No simula monedas, llaves ni puntajes |
| Muestra un estado de conexión honesto: si nadie se conecta al puente, dice “sin integración” | No muestra números inventados de llaves/monedas |
| Expone `Speed`, `High Jump`, `Invincibility`, `Coin Multiplier` y `Keys Counter` como valores verificables por una integración | No aplica por sí solo esos valores dentro del juego: eso lo hace la integración (ver abajo) |

> **Por qué:** los efectos sobre el juego solo existen si el proceso del juego lee esos
> valores. Un menú que “aplica” cambios sin que el juego los lea estaría simulando resultados
> inexistentes. Por eso el menú publica el estado en un contrato estable y **muestra si hay
> un cliente conectado**. Cuando no lo hay, lo dice sin adornos.

La integración es el archivo [`ModMenuBridge.java`](app/src/main/java/com/submenus/overlay/bridge/ModMenuBridge.java)
más el contrato del puente; se pueden copiar tal cual en el módulo que ejecute del lado del
juego, porque no dependen de nada más del proyecto. La guía completa, con un cliente
funcional, está en [`docs/INTEGRATION.md`](docs/INTEGRATION.md).

---

## ✨ Características

### Menú flotante
- **Botón circular arrastrable** con halo pulsante, icono de rayo, sombra y feedback de presión.
- **Se pega a los bordes** de forma animada y recuerda su posición entre sesiones.
- **Punto de estado** en el botón: verde = integración conectada, naranja = sin conexión.
- Panel que se abre al lado del botón, **reposicionado automáticamente** para no salirse de la pantalla.
- Se cierra con la ✕ o tocando el botón otra vez.

### Diseño premium oscuro
- Fondo `#060910`, tarjetas `#111A29`, bordes suaves `#22314A`.
- Acentos **verde `#22E07A`**, **azul `#2CB6FF`**, **naranja `#FF9B3D`**.
- Encabezado con degradado verde→azul, píldoras de estado, tipografía sans con tracking.
- Interruptores **dibujados a mano** (`ToggleSwitch`), con animación `PathInterpolator` y brillo.
- Sliders con pista degradada y pulgar verde; valores en vivo en píldoras naranjas.

### Interruptores, animaciones y persistencia
- Interruptores ON/OFF animados con vibración opcional.
- Opción para **desactivar animaciones** (accesibilidad y ahorro de batería).
- Todo se guarda en `SharedPreferences` al instante, incluido el tamaño del botón, la posición
  y la opacidad del panel.
- Permite **apagar todas las modificaciones** de una vez.

### Categorías y opciones

| Categoría | Opciones |
| --- | --- |
| **Player** | `Speed` (×1.00 – ×3.00), `High Jump` (×1.00 – ×3.00), `Invincibility` |
| **Collectibles** | `Coin Multiplier` (×1 – ×50), `Keys Counter` (con lectura real reportada por la integración) |
| **Settings** | Vibración, animaciones, pegado a bordes, opacidad del panel, tamaño del botón, apagar todo |

### Compatibilidad
- `minSdk 24` (Android 7.0) · `targetSdk 34` (Android 14).
- Servicio en primer plano con tipo `specialUse` para Android 14+.
- Sin dependencias externas (ni AndroidX): el APK compila rápido y no depende de descargas raras.
- Textos en **español** e **inglés** (`values/` y `values-en/`).

---

## 🚀 Compilar la APK desde GitHub (sin computadora)

1. **Haz fork** de este repositorio (o usa el tuyo directamente).
2. Abre la pestaña **Actions** → workflow **Build APK**.
3. Pulsa **Run workflow** → rama `main` → **Run workflow**.
4. Espera unos 2–4 minutos (la primera vez tarda un poco más por el SDK).
5. Abre la ejecución terminada y descarga el artefacto
   **`SubwaySurfersModMenu-APK-1.0.0`**: dentro están los dos `.apk`.
   - `…-release.apk` → instala esta.
   - `…-debug.apk` → variante de depuración.
6. (Opcional) Para publicar una **Release** con el APK adjunto: ejecuta el workflow con
   `make_release` activado, o crea un tag (`git tag v1.0.0 && git push origin v1.0.0`).

Cada `push` también dispara el build, así que cualquier cambio genera APK nueva.

### Instalar en el teléfono
1. Descarga el `.apk` desde GitHub (el archivo queda en *Descargas*).
2. Ábrelo y permite **“Instalar apps de esta fuente”** si Android lo pide.
3. Abre **Subway Surfers Mod Menu** y pulsa **Conceder permiso** (permiso de superposición).
4. Pulsa **Iniciar menú**: aparece el botón flotante. Arrástralo donde quieras y tócalo para
   abrir el panel.

> El APK de *release* está firmado con el **keystore de depuración** para que puedas
> instalarlo sin configurar secretos. Para publicar en Play Store necesitarías tu propia
> firma y eso está fuera del alcance de este proyecto.

---

## 🧩 Cómo se conecta con el juego (puente real)

El menú expone su estado como **datos**, no como promesas:

| Recurso | Valor |
| --- | --- |
| Autoridad | `com.submenus.overlay.settings` |
| Snapshot | `content://com.submenus.overlay.settings/settings` |
| Salud / heartbeat | `content://com.submenus.overlay.settings/health` |
| Permiso (normal) | `com.submenus.overlay.permission.READ_MOD_SETTINGS` |
| Broadcast de cambios | `com.submenus.overlay.action.SETTINGS_CHANGED` |
| Método `call()` | `heartbeat`, `snapshot` |

### JSON publicado (ejemplo real)

```json
{
  "schema": 1,
  "updatedAt": 1730000000000,
  "player": {
    "speedEnabled": true,
    "speedMultiplier": 1.75,
    "highJumpEnabled": false,
    "highJumpMultiplier": 1.5,
    "invincibilityEnabled": true
  },
  "collectibles": {
    "coinMultiplierEnabled": true,
    "coinMultiplier": 5,
    "keysCounterEnabled": true
  },
  "ui": {
    "hapticsEnabled": true,
    "animationsEnabled": true,
    "snapToEdge": true,
    "overlayOpacity": 1.0,
    "buttonSizeDp": 64,
    "accent": "aurora"
  }
}
```

### Lado de la integración

En el manifiesto del cliente:

```xml
<uses-permission android:name="com.submenus.overlay.permission.READ_MOD_SETTINGS" />
```

Y en el código (las clases `bridge/` del proyecto se copian tal cual):

```java
if (ModMenuBridge.isAvailable(context)) {
    ModMenuBridge.sendHeartbeat(context, "Subway Surfers");       // el menú muestra "Conectado"
    ModMenuBridge.registerListener(context, snapshot -> {
        // Aplicá los valores donde el juego realmente los lea:
        player.setSpeedScale(snapshot.speedEnabled ? snapshot.speedMultiplier : 1f);
        player.setJumpScale(snapshot.highJumpEnabled ? snapshot.highJumpMultiplier : 1f);
        player.setInvincible(snapshot.invincibilityEnabled);
        coins.setPickupMultiplier(snapshot.coinMultiplierEnabled ? snapshot.coinMultiplier : 1);
        if (snapshot.keysCounterEnabled) hud.showKeys(game.keys());
    });
} else {
    // Sin menú instalado: el juego corre normal, sin inventar nada.
}
```

Para que el contador de llaves muestre **el valor real del juego**, la integración lo reporta
en el heartbeat:

```java
ModMenuBridge.sendHeartbeat(context, "Subway Surfers", game.keys(), game.coins());
```

Si nunca se reporta nada, el panel muestra **“Sin datos todavía”** en lugar de un cero falso.

---

## 🗂️ Estructura del proyecto

```
Subway-surfers-menu/
├── .github/workflows/build-apk.yml      # CI: compila y publica la APK
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/submenus/overlay/
│       │   ├── MenuApp.java             # Application
│       │   ├── MainActivity.java        # Panel de control (permisos, opciones)
│       │   ├── InspectorActivity.java   # Visor del JSON del puente
│       │   ├── OverlayService.java      # Ventanas flotantes + notificación
│       │   ├── BootReceiver.java        # Restaura el menú tras reiniciar
│       │   ├── bridge/                  # Contrato público (copiable al juego)
│       │   │   ├── BridgeContract.java
│       │   │   ├── MenuLimits.java
│       │   │   ├── MenuSnapshot.java
│       │   │   ├── ModMenuBridge.java
│       │   │   ├── ModMenuListener.java
│       │   │   └── SettingsProvider.java
│       │   ├── data/
│       │   │   ├── Prefs.java           # Persistencia local
│       │   │   └── MenuRepository.java  # Estado + publicación
│       │   └── ui/
│       │       ├── Theme.java
│       │       ├── BubbleView.java      # Botón circular dibujado a mano
│       │       ├── DragController.java  # Arrastre + tap
│       │       ├── ToggleSwitch.java    # Interruptor animado
│       │       ├── SwitchRowView.java
│       │       ├── SliderRowView.java
│       │       └── MenuPanelView.java   # Panel con categorías
│       └── res/                         # Colores, estilos, textos es/en, layouts
├── docs/INTEGRATION.md                  # Guía del puente con cliente completo
├── app/src/test/java/…/MenuLimitsTest.java  # Tests JVM del contrato
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

---

## 💻 Compilar en local (opcional)

Requisitos: JDK 17 y Android SDK 34.

```bash
# compilar depuración y release
gradle assembleDebug assembleRelease

# el APK queda en:
# app/build/outputs/apk/release/app-release.apk
```

Si usás Android Studio, abrí la carpeta del proyecto y aceptá cuando ofrezca generar el
*Gradle wrapper*; también podés crearlo con `gradle wrapper --gradle-version 8.7`.

---

## 🛠️ Solución de problemas

| Síntoma | Causa y solución |
| --- | --- |
| No aparece el botón flotante | Falta el permiso de superposición. Abrí la app → **Conceder permiso** → activá *Permitir apps superpuestas*. |
| El botón desaparece al abrir un juego | Algunos fabricantes (Xiaomi, Huawei, Samsung) matan servicios en segundo plano: activá *Autostart* / *Sin restricciones de batería* para la app. |
| Dice “Sin integración” en el panel | Es correcto: ningún cliente envió heartbeat. Es el comportamiento honesto, no un error. |
| Android 14 no muestra la notificación | Concedé el permiso de notificaciones cuando la app lo pida; el menú funciona igual. |
| El build falla en Actions por SDK | Ejecutá de nuevo el workflow: la primera ejecución a veces espera la caché del SDK. Revisá *Logs* del paso que falló. |

---

## 📄 Aviso legal

Proyecto **educativo** sobre superposición de ventanas en Android, persistencia de
preferencias y comunicación entre procesos mediante `ContentProvider`. No incluye ni
distribuye contenido de Subway Surfers; las marcas mencionadas pertenecen a sus dueños. Usalo
solo en partidas propias, sin servicios en línea ni detrás de cuentas ajenas, y respetá los
términos de servicio de los juegos y de las tiendas que uses.
