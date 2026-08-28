# RapidsUtils

RapidsUtils is a client-only Fabric mod for Minecraft 1.21.11. It receives UTF-8 JSON sent by a Bukkit/Paper server on the `rapidsclientdata:data` plugin messaging channel and presents the latest topic snapshots in a compact HUD.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.4 or newer
- Fabric API 0.141.6+1.21.11 or newer
- Java 21

The server does not need Fabric or this mod. It only needs to send the documented Bukkit plugin message payload.

## Build

On Windows:

```powershell
$env:JAVA_HOME = 'D:\MC\jdk-21.0.10'
.\gradlew.bat build
```

The remapped client mod is written to `build/libs/rapidsutils-1.0.0.jar`.
