# ⚙️ SkyFoundry

A lightweight custom SkyBlock core built for the **SkyFoundry** Minecraft server.

SkyFoundry is being built from scratch for a modded multiplayer SkyBlock experience, with a focus on simplicity, performance, and compatibility with hybrid Bukkit/NeoForge servers.

## Development

- **Minecraft:** 1.21.1
- **Server:** Youer 1.21.1
- **Java:** 21
- **Build:** Gradle
- **Version:** 0.1.0-SNAPSHOT

## Current Goals

The initial version focuses on the basic SkyBlock foundation:

- Island creation
- Island deletion
- Island homes
- Void world generation
- Starter island schematics
- Island protection
- SQLite persistence

Additional systems will be added after the core island functionality is stable.

## Building

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The compiled plugin will be located in:

```text
build/libs/
```

## License

This project is currently developed specifically for the SkyFoundry server.

## Addons

SkyFoundry `0.2.0-SNAPSHOT` includes an initial addon API and loader. Addon JARs are loaded from `plugins/SkyFoundry/addons/` and must contain an `addon.yml` file.

```yaml
name: ExampleAddon
version: 0.1.0
main: com.example.ExampleAddon
api-version: 1
depend: []
softdepend: []
```

Addon main classes extend `net.skyfoundry.api.addon.SkyFoundryAddon`. The included `example-addon/` project is a minimal test addon.
