# RapidsUtils

RapidsUtils is a client-only Fabric mod for Minecraft 1.21.11. It receives UTF-8 JSON sent by a Bukkit/Paper server on the `rapidsclientdata:data` and `rapidsclientdata:player` plugin messaging channels and presents server-controlled data in the HUD.

The receiver is registered before the client connects. Fabric advertises the registered play channel to the server through Minecraft's channel registration payload after join, so Bukkit can detect the listener without Fabric or RapidsUtils on the server.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.3 or newer
- Fabric API 0.141.4+1.21.11 or newer
- Java 21

The server does not need Fabric or this mod. It only needs to send the documented Bukkit plugin message payload.

When the server advertises the incoming Plugin Messaging channel `rapidsclientdata:version`, the client sends its mod version once per backend-server join. A Velocity-style switch produces another Game Join without disconnecting the client, so the version is reported again to the new backend. Registration may arrive before or after the world change and Game Join; either order results in one report, while ordinary dimension changes do not trigger another report. The message body is the raw UTF-8 version string, currently `20260901-2100`.

## Wire protocol

The complete plugin message body is one UTF-8 JSON document:

```json
{
  "version": 1,
  "topic": "dungeon",
  "sequence": 42,
  "full": true,
  "duration": 60,
  "index": 5,
  "x": 160,
  "y": 90,
  "opacity": 0.75,
  "fadeIn": 5,
  "fadeOut": 15,
  "data": {
    "floor": "§bFrozen Vault",
    "wave": 3,
    "party": ["§aAtenls", "§7PlayerTwo"]
  }
}
```

GuoScript sends the base shape from `sendClientData(player, topic, data, duration, index)` and `broadcastClientData(topic, data, duration, index)`. The original seven outer keys remain required. The optional `x`, `y`, `opacity`, `fadeIn`, and `fadeOut` keys may be omitted or set to `null`.

- `version` must currently be `1`.
- `topic` identifies an independently displayed data group.
- `sequence` must increase within a topic. Duplicate or older snapshots are ignored.
- `full` must currently be `true`.
- `duration` and `index` are preserved as immutable JSON values. A numeric `duration` is the HUD lifetime in client ticks. `null` falls back to the topic's client-side `displaySeconds`; `-1` keeps the topic visible until it is replaced, removed, or the connection is cleared. Other non-numeric values use the same fallback behavior as `null`.
- A numeric `index` orders HUD panels from lowest to highest. `null` and other non-numeric values fall back to the topic's client-side `index`; custom topics default to `10`. Equal indexes retain the topics' stable first-seen order.
- Numeric `x` and `y` values specify the center of that topic's panel, not its top-left corner. A floating-point value from `0` inclusive to `1` exclusive is a fraction of the scaled screen size, so `x: 0.5, y: 0.5` places the panel at screen center. Other numbers are scaled HUD pixel coordinates. Either axis may be supplied independently. An omitted, `null`, or non-numeric axis keeps the normal client-side position for that axis. A topic with a server-provided `y` is positioned independently and does not consume space in the normal vertical stack.
- Numeric `opacity` overrides the topic panel background opacity. Values are clamped to `0.0` through `1.0`; omitted, `null`, and non-numeric values use the client's configured background opacity.
- Numeric non-negative `fadeIn` and `fadeOut` values control the panel animation in client ticks. They default to `5` and `15`; `0` disables that phase. Fade-in starts only when a topic first becomes active, while later snapshots for the same active topic update its content and expiration without restarting the animation. Fade-out starts after the latest snapshot's `duration` expires. Once the fade-out completes, a later snapshot for that topic starts a new fade-in.
- `data` may be any JSON object, array, string, number, boolean, or null. `null` and an empty object `{}` are removal messages for that topic. Their sequence is retained, so an older packet cannot restore removed data.

Malformed messages, unsupported versions, partial updates, and stale sequences are ignored. Topic snapshots and sequence baselines are cleared when the client world changes or the connection closes. Clearing on a world change allows a Velocity/Bungee-style backend switch to accept the new server's sequence range even though the client remains connected to the proxy. Legacy Minecraft colors (`§0`-`§f`), `§r`, and `§x§R§R§G§G§B§B` colors in string values are displayed directly. Templates also support RGB colors in the form `&#rrggbb`.

## Player vitals override

The `rapidsclientdata:player` message body is a separate UTF-8 JSON document containing all six numeric fields:

```json
{
  "health": 72.5,
  "health_max": 100,
  "health_regen": 2.25,
  "mana": 48,
  "mana_max": 80,
  "mana_regen": 3
}
```

The first valid message activates the player-vitals override. While active, the vanilla health row is suppressed and two 182-pixel bars are drawn above the normal status-bar baseline: health above mana. The health fill is green above 65%, blue above 35%, and red at 35% or below; mana is blue. Each bar clamps only its visual fill between zero and its maximum while preserving the server value in the label, shows the corresponding regeneration value at the right edge, uses a 20%-opacity tinted empty region, and has a muted same-hue gray border.

All six keys are required and must be JSON numbers. Invalid messages are ignored without replacing the last valid snapshot. The override is cleared on a client-world change or disconnect, so the vanilla health row returns until another valid `rapidsclientdata:player` message arrives.

## HUD and configuration

The HUD starts at a configurable scaled-screen margin of 5 pixels by default, follows the normal F1 HUD visibility condition, and computes each topic panel independently from its visible content. The hidden topic `hidden_notification` is anchored to the bottom-right corner using that same screen margin while keeping its text left-aligned. Press `H` in game to toggle the HUD; the key can be rebound in Minecraft's Controls screen and the new state is saved immediately. When the server sends `duration: null`, a topic is shown for its configured `displaySeconds` (three seconds by default). A numeric server duration overrides that fallback and is measured in client ticks. Topics without a configured template use the generic recursive JSON display.

The whole panel, including its background and text, fades as one unit. Rounded backgrounds are drawn as non-overlapping horizontal spans, so translucent pixels are blended only once. Nested data, collection length, string length, wrapping, and screen height are bounded to keep the display readable.

On first launch the mod creates `config/rapidsutils.json`. The built-in `dungeon`, `mastery`, `update`, and `reload` topics keep their existing fallback durations and indexes, but are hidden from configuration and always render with the hard-coded `{display}{extraData}` template.

Server topics whose IDs start with `hidden_`, such as `hidden_test`, are also hidden from configuration and always render with the hard-coded `{display}` template. Their client fallback duration and index remain the generic 3 seconds and 10 when the server omits those values.

```json
{
  "enabled": true,
  "margin": 5,
  "backgroundOpacity": 0.6,
  "maxWidth": 300,
  "topics": {}
}
```

Template variables use `{variable}` syntax. `{rhombus}` is built in and renders `◆`. Every top-level `data` key is available directly, such as `{dungeonDisplay}`; nested object values can also be addressed as `{parent.child}`. Variables that are not present in the received `data` are replaced with an empty string.

No configuration library is required. If optional Mod Menu 17.0.0 is installed, open RapidsUtils from the Mods screen to edit global HUD options, open a topic to edit its fallback display duration, fallback sort index, and multiline template together, or use **Add topic** to create another client-side topic definition. The JSON file remains directly editable without Mod Menu.

## Build

On Windows:

```powershell
$env:JAVA_HOME = 'D:\MC\jdk-21.0.10'
.\gradlew.bat build
```

The remapped client mod is written to `build/libs/rapidsutils-20260901-2100.jar`.
