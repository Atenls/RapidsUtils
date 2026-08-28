# RapidsUtils

RapidsUtils is a client-only Fabric mod for Minecraft 1.21.11. It receives UTF-8 JSON sent by a Bukkit/Paper server on the `rapidsclientdata:data` plugin messaging channel and presents the latest topic snapshots in a compact HUD.

The receiver is registered before the client connects. Fabric advertises the registered play channel to the server through Minecraft's channel registration payload after join, so Bukkit can detect the listener without Fabric or RapidsUtils on the server.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.3 or newer
- Fabric API 0.141.4+1.21.11 or newer
- Java 21

The server does not need Fabric or this mod. It only needs to send the documented Bukkit plugin message payload.

## Wire protocol

The complete plugin message body is one UTF-8 JSON document:

```json
{
  "version": 1,
  "topic": "dungeon",
  "sequence": 42,
  "full": true,
  "data": {
    "floor": "§bFrozen Vault",
    "wave": 3,
    "party": ["§aAtenls", "§7PlayerTwo"]
  }
}
```

- `version` must currently be `1`.
- `topic` identifies an independently displayed data group.
- `sequence` must increase within a topic. Duplicate or older snapshots are ignored.
- `full` must currently be `true`.
- `data` may be any JSON object, array, string, number, boolean, or null.

Malformed messages, unsupported versions, partial updates, and stale sequences are ignored. Topic state is cleared when the client disconnects. Legacy Minecraft colors (`§0`-`§f`), `§r`, and `§x§R§R§G§G§B§B` colors in string values are displayed directly; ampersand colors and custom gradient syntax are intentionally not parsed.

## HUD and configuration

The HUD starts at scaled screen coordinates `8, 8` by default, follows the normal F1 HUD visibility condition, and computes its panel size from the visible content. Nested data, collection length, string length, wrapping, and screen height are bounded to keep the display readable.

On first launch the mod creates `config/rapidsutils.json`:

```json
{
  "enabled": true,
  "margin": 8,
  "backgroundOpacity": 0.75,
  "maxWidth": 300
}
```

No configuration library or Mod Menu is required.

## Build

On Windows:

```powershell
$env:JAVA_HOME = 'D:\MC\jdk-21.0.10'
.\gradlew.bat build
```

The remapped client mod is written to `build/libs/rapidsutils-1.0.0.jar`.
