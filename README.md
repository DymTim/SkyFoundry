# ⚙️ SkyFoundry

> A custom, modded-first Skyblock core built for Minecraft 1.21.1.

**SkyFoundry** is a from-scratch Skyblock plugin designed for the SkyFoundry Minecraft server, with a focus on hybrid Bukkit/NeoForge environments and heavily modded gameplay.

The project is currently being developed and tested on **Youer 1.21.1** using **Java 21**.

![Version](https://img.shields.io/badge/version-0.1.0--SNAPSHOT-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)
![Java](https://img.shields.io/badge/Java-21-blue)
![Status](https://img.shields.io/badge/status-in%20development-yellow)

> [!IMPORTANT]
> SkyFoundry is currently in early development and is being built specifically
> for the SkyFoundry Minecraft server. APIs, configuration formats, database
> schemas, and features may change before the first stable release.

## 📖 About

Most traditional Skyblock plugins are designed primarily around vanilla Bukkit or Paper servers. SkyFoundry is being built specifically for an environment where mods are a core part of the experience.

Rather than adapting an existing Skyblock plugin, SkyFoundry provides a purpose-built foundation for:

- Island management
- Team and role management
- Island protection
- Persistent island data
- Modded block and block entity support
- Server progression systems
- Integration with mods such as Create
- Integration with other SkyFoundry plugins

The goal is a modular Skyblock core that remains maintainable as the server and its modpack evolve.

## ✨ Current Features

### Island System

- Automatic void island world creation
- SQLite-backed island persistence
- Automatic island location allocation
- Configurable island spacing
- 50×50 starting island boundaries
- Full world-height island ownership
- Persistent island ownership
- Island homes

### Members & Roles

Three island roles are currently supported:

| Role         | Description                                       |
| ------------ | ------------------------------------------------- |
| **Owner**    | Full control over the island                      |
| **Co-Owner** | Management permissions without owner-only actions |
| **Member**   | Standard island member                            |

Current team functionality includes:

- Player invitations
- Invite expiration
- Accepting and declining invitations
- Configurable member limits
- Leaving an island
- Kicking members
- Promoting members
- Demoting Co-Owners
- Persistent membership and roles

## 🏝️ Island Architecture

SkyFoundry islands use the following default configuration:

| Property       |   Default    |
| :------------- | :----------: |
| Starting Size  |   `50×50`    |
| Maximum Size   |  `300×300`   |
| Size Upgrade   | `50 blocks`  |
| Island Spacing | `500 blocks` |
| Member Limit   |     `5`      |
| World Type     |    `Void`    |
| Persistence    |   `SQLite`   |

Island ownership applies across the **entire vertical height of the world** within the island's X/Z boundary.

Island centers are separated by 500 blocks, allowing islands to expand to their maximum size while maintaining space between neighboring islands.

## 💻 Commands

### Player Commands

| Command                    | Description                  |
| -------------------------- | ---------------------------- |
| `/island`                  | Display island commands      |
| `/island create`           | Create an island             |
| `/island home`             | Teleport to your island      |
| `/island info`             | View island information      |
| `/island members`          | View island members          |
| `/island invite <player>`  | Invite a player              |
| `/island accept`           | Accept an island invitation  |
| `/island decline`          | Decline an island invitation |
| `/island leave`            | Leave your island            |
| `/island kick <player>`    | Remove an island member      |
| `/island promote <player>` | Promote a member to Co-Owner |
| `/island demote <player>`  | Demote a Co-Owner            |

The `/is` alias can be used in place of `/island`.

### Administration

| Command              | Description                      |
| -------------------- | -------------------------------- |
| `/skyfoundry`        | Display SkyFoundry information   |
| `/skyfoundry status` | Display plugin and island status |
| `/skyfoundry reload` | Reload the configuration         |

The `/sf` alias can be used in place of `/skyfoundry`.

## 🗺️ Roadmap

### Island Management

- [x] Void island world
- [x] Island allocation
- [x] SQLite persistence
- [x] Island creation
- [x] Island homes
- [x] Island members
- [x] Island roles
- [x] Island invitations
- [x] Individual member homes
- [x] Ownership transfer
- [x] Island deletion
- [x] Island reset system
- [x] Lifetime reset limits

### Protection

- [x] Block placement and breaking protection
- [x] Container and interaction protection
- [x] Entity protection
- [x] Explosion protection
- [x] Fluid boundary protection
- [x] Piston boundary protection
- [x] Modded block protection
- [ ] Create automation protection

### Island Progression

- [x] Island size upgrades
- [x] 300×300 maximum island size
- [x] Island boundary visualization
- [ ] Custom island level system
- [ ] Block value system
- [ ] Mission-based progression

### Island Generation

- [ ] WorldEdit schematic support
- [ ] `starter.schem`
- [ ] Multiple starter templates
- [ ] Starter template selection GUI

### Integrations

- [ ] SkyFoundryGenerator
- [ ] SkyFoundryProfessions
- [ ] SkyFoundryLimits
- [ ] Create machine limits
- [ ] Public SkyFoundry API

## 🧩 Project Structure

SkyFoundry is designed around separate services rather than placing all island functionality into a single manager.

```text
net.skyfoundry.core
├── command
├── config
├── database
├── invite
├── island
├── service
└── world
```

As development continues, additional systems such as protection, schematics, homes, upgrades, and APIs will remain separated into their own modules.

## 🛠️ Development

### Requirements

To build SkyFoundry, you will need:

- **Java 21**
- **Gradle**
- **Minecraft 1.21.1 API dependencies**

The primary runtime and testing environment is:

- **Youer 1.21.1**
- Bukkit/Paper-compatible plugin environment
- NeoForge mod environment

### Building

Clone the repository and build with the included Gradle wrapper.

#### Windows

```powershell
.\gradlew clean build
```

#### Linux / macOS

```bash
./gradlew clean build
```

The compiled plugin will be generated in:

```text
build/libs/
```

## 🧪 Versioning

SkyFoundry follows semantic versioning.

Development builds use the `-SNAPSHOT` suffix:

```text
0.1.0-SNAPSHOT
```

Stable releases will use standard semantic versions:

```text
MAJOR.MINOR.PATCH
```

For example:

```text
1.0.0
1.1.0
1.1.1
2.0.0
```

The first stable production release will be **SkyFoundry 1.0.0**.

## 🎯 Project Goals

SkyFoundry is both a production server project and an exploration of building a Skyblock platform specifically around the challenges introduced by modern modded Minecraft.

The primary engineering goals are:

1. **Modded Compatibility**
   Reliably handle modded blocks, block entities, entities, and automation.

2. **Safe Island Management**
   Ensure island creation, deletion, and resetting behave correctly even on heavily modded islands.

3. **Reliable Protection**
   Protect island boundaries from players, vanilla mechanics, automation, and modded systems.

4. **Persistent Data**
   Maintain reliable island, membership, progression, and configuration data using SQLite.

5. **Modular Architecture**
   Keep the Skyblock core separate from progression, professions, generators, machine limits, and other server systems.

6. **Extensibility**
   Provide a clean foundation for future SkyFoundry plugins and integrations.

## 📜 License

SkyFoundry is currently under active development and no open-source license has been granted.

Unless a license is added in the future, the source code remains **all rights reserved**.

---

<p align="center">
  <strong>⚙️ Create. Expand. Ascend.</strong>
</p>

<p align="center">
  Built for the SkyFoundry Minecraft Server
</p>
