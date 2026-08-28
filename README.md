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

Malformed messages, unsupported versions, partial updates, and stale sequences are ignored. Topic state is cleared when the client disconnects. Legacy Minecraft colors (`§0`-`§f`), `§r`, and `§x§R§R§G§G§B§B` colors in string values are displayed directly. Templates also support RGB colors in the form `&#rrggbb`; legacy ampersand colors such as `&a` and gradient syntax are not parsed.

## HUD and configuration

The HUD starts at scaled screen coordinates `8, 8` by default, follows the normal F1 HUD visibility condition, and computes each topic panel independently from its visible content. A topic is shown for three seconds after its latest accepted snapshot by default. Its duration and template can be configured together. Topics without a configured template use the generic recursive JSON display.

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
      "template": "{rhombus} {dungeonDisplay}\n   &#999999特殊掉落 &#80b0d0{itemgot}/{itemgotmax}\n   &#999999材料掉落 &#80b0d0{dropsgot}/{dropsgotmax}\n   &#999999药剂掉落 &#80b0d0{healingPotionGot}/{healingPotionGotMax}"
    },
    "mastery": {
      "displaySeconds": 3.0,
      "template": "{rhombus} &#8098b8天赋状态{lootinstinctDisplay}{chestmagnetDisplay}{rarelootDisplay}"
    },
    "update": {
      "displaySeconds": 60.0,
      "template": "{rhombus} &#8098b8Mod 已有可用更新! {version} \n 前往 wiki.dp4.us/#/rapids/updatelogs 查看更新日志并获取新 Mod !"
    }
  }
}
```

Template variables use `{variable}` syntax. `{rhombus}` is built in and renders `◆`. Every top-level `data` key is available directly, such as `{dungeonDisplay}`; nested object values can also be addressed as `{parent.child}`. Unknown variables remain visible in the HUD so configuration mistakes can be found easily.

No configuration library is required. If optional Mod Menu 17.0.0 is installed, open RapidsUtils from the Mods screen to edit global HUD options, open a topic to edit its display duration and multiline template together, or use **Add topic** to create another client-side topic definition. The JSON file remains directly editable without Mod Menu.

## Build

On Windows:

```powershell
$env:JAVA_HOME = 'D:\MC\jdk-21.0.10'
.\gradlew.bat build
```

The remapped client mod is written to `build/libs/rapidsutils-1.0.0.jar`.
