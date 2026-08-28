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
  "duration": 60,
  "index": 5,
  "data": {
    "floor": "§bFrozen Vault",
    "wave": 3,
    "party": ["§aAtenls", "§7PlayerTwo"]
  }
}
```

GuoScript sends this shape from `sendClientData(player, topic, data, duration, index)` and `broadcastClientData(topic, data, duration, index)`. All seven outer keys are required. Legacy three-argument calls still send the same shape with `duration` and `index` set to `null`.

- `version` must currently be `1`.
- `topic` identifies an independently displayed data group.
- `sequence` must increase within a topic. Duplicate or older snapshots are ignored.
- `full` must currently be `true`.
- `duration` and `index` are preserved as immutable JSON values. A numeric `duration` is the HUD lifetime in client ticks. `null` falls back to the topic's client-side `displaySeconds`; `-1` keeps the topic visible until it is replaced, removed, or the connection is cleared. Other non-numeric values use the same fallback behavior as `null`.
- A numeric `index` orders HUD panels from lowest to highest. `null` and other non-numeric values fall back to the topic's client-side `index`; custom topics default to `10`. Equal indexes retain the topics' stable first-seen order.
- `data` may be any JSON object, array, string, number, boolean, or null. `null` and an empty object `{}` are removal messages for that topic. Their sequence is retained, so an older packet cannot restore removed data.

Malformed messages, unsupported versions, partial updates, and stale sequences are ignored. Topic snapshots and sequence baselines are cleared when the client world changes or the connection closes. Clearing on a world change allows a Velocity/Bungee-style backend switch to accept the new server's sequence range even though the client remains connected to the proxy. Legacy Minecraft colors (`§0`-`§f`), `§r`, and `§x§R§R§G§G§B§B` colors in string values are displayed directly. Templates also support RGB colors in the form `&#rrggbb`; legacy ampersand colors such as `&a` and gradient syntax are not parsed.

## HUD and configuration

The HUD starts at a configurable scaled-screen margin of 5 pixels by default, follows the normal F1 HUD visibility condition, and computes each topic panel independently from its visible content. Press `H` in game to toggle the HUD; the key can be rebound in Minecraft's Controls screen and the new state is saved immediately. When the server sends `duration: null`, a topic is shown for its configured `displaySeconds` (three seconds by default). A numeric server duration overrides that fallback and is measured in client ticks. Topics without a configured template use the generic recursive JSON display.

Rounded backgrounds are drawn as non-overlapping horizontal spans, so translucent pixels are blended only once. Nested data, collection length, string length, wrapping, and screen height are bounded to keep the display readable.

On first launch the mod creates `config/rapidsutils.json`:

```json
{
  "enabled": true,
  "margin": 5,
  "backgroundOpacity": 0.6,
  "maxWidth": 300,
  "topics": {
    "dungeon": {
      "displaySeconds": 3.0,
      "index": 8,
      "template": "{rhombus} {dungeonDisplay}\n   &#999999特殊掉落 &#80b0d0{itemgot}/{itemgotmax}\n   &#999999材料掉落 &#80b0d0{dropsgot}/{dropsgotmax}\n   &#999999药剂掉落 &#80b0d0{healingPotionGot}/{healingPotionGotMax}{essenceDisplay}{finalDisplay}{extraData}"
    },
    "mastery": {
      "displaySeconds": 3.0,
      "index": 5,
      "template": "{rhombus} &#8098b8天赋状态{lootinstinctDisplay}{chestmagnetDisplay}{rarelootDisplay}{extraData}"
    },
    "update": {
      "displaySeconds": 60.0,
      "index": 20,
      "template": "{rhombus} &#8098b8Mod 已有可用更新! {version} \n 前往 wiki.dp4.us/#/rapids/updatelogs 查看更新日志并获取新 Mod !{extraData}"
    },
    "reload": {
      "displaySeconds": 60.0,
      "index": 1,
      "template": "{reload}{extraData}"
    }
  }
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

The remapped client mod is written to `build/libs/rapidsutils-1.0.0.jar`.
