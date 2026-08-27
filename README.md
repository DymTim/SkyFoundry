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
> schemas, commands, and features may change before the first stable release.

## 📖 About

Most traditional Skyblock plugins are designed primarily around vanilla Bukkit or Paper servers. SkyFoundry is being built specifically for an environment where mods are a core part of the experience.

Rather than adapting an existing Skyblock plugin, SkyFoundry provides a purpose-built foundation for:

- Island creation and management
- Team and role management
- Island protection
- Persistent island data
- Individual member homes
- Island deletion and resetting
- Island progression
- Daily missions
- Island value tracking
- Modded block and block entity support
- Server progression systems
- Integration with mods such as Create
- Integration with other SkyFoundry plugins

The goal is a modular Skyblock core that remains maintainable as the server and its modpack evolve.

---

## ✨ Current Features

### 🏝️ Island System

- Automatic void island world creation
- SQLite-backed island persistence
- Automatic island location allocation
- Configurable island spacing
- `50×50` starting island boundaries
- Expansion up to `300×300`
- Full world-height island ownership
- Persistent island ownership
- Individual island homes for each member
- Island ownership transfers
- Island deletion
- Island resetting
- Configurable lifetime reset limits
- Temporary island boundary visualization

### 👥 Members & Roles

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
- Ownership transfers with confirmation
- Persistent membership and roles
- Individual member homes

### 🛡️ Island Protection

SkyFoundry includes island-aware protection designed to work in both vanilla and modded environments.

Currently implemented protection includes:

- Block placement protection
- Block breaking protection
- Container protection
- Interaction protection
- Entity protection
- Explosion protection
- Fluid boundary protection
- Vanilla piston boundary protection
- Modded block protection
- Administrative protection bypass

Protection is based on the island's current boundary and automatically accounts for island size upgrades.

> [!NOTE]
> Full Create automation boundary protection is not yet implemented. Certain Create contraptions, such as Mechanical Pistons, operate outside Bukkit's normal block event system and require additional compatibility work.

### 📈 Island Progression

SkyFoundry uses a shared island progression system based on **Island XP**.

Island XP is currently earned through daily missions and is shared by the entire island team.

Progression currently includes:

- Custom Island Level system
- Mission-based Island XP
- Five randomized daily missions
- Shared mission progress between island members
- Level-gated mission eligibility
- Persistent daily missions
- Island-wide mission completion notifications
- Island-wide level-up notifications
- Progressive island size upgrades
- Separate block-based Island Value

#### Island Level

Island Level is based entirely on **Island XP earned through progression activities such as daily missions**.

Island Value does **not** contribute toward Island Level.

```text
Daily Missions
      ↓
Island XP
      ↓
Island Level
```

The leveling curve is configurable and becomes progressively more demanding as the island reaches higher levels.

#### Daily Missions

Each island receives **5 randomized daily missions**.

Mission objectives are selected from activities available to the island at its current progression level, preventing low-level islands from receiving objectives involving resources they have not yet unlocked.

Mission categories include activities such as:

- Mining
- Farming
- Forestry
- Combat
- Fishing

Mission progress is shared across the entire island.

```text
Owner contributes
        +
Co-Owner contributes
        +
Member contributes
        ↓
Shared Mission Progress
```

Daily mission selections and progress are persisted in SQLite, preventing missions from rerolling when the server restarts.

#### Island Value

Island Value is a separate measurement of the physical development of an island.

Configured blocks contribute value while they exist on the island.

```text
Place valuable block
        ↓
Island Value increases

Break valuable block
        ↓
Island Value decreases
```

Island Value does not grant Island XP and cannot increase Island Level.

### 📐 Island Expansion

Islands begin at `50×50` and can expand in `50` block increments.

| Stage           | Island Size |
| --------------- | ----------: |
| Starting Island |     `50×50` |
| Upgrade 1       |   `100×100` |
| Upgrade 2       |   `150×150` |
| Upgrade 3       |   `200×200` |
| Upgrade 4       |   `250×250` |
| Maximum         |   `300×300` |

Island boundaries update immediately when an upgrade is applied.

Players can temporarily visualize their current island boundary with:

```text
/island border
```

---

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

Island centers are separated by `500` blocks, allowing islands to expand to their maximum size while maintaining space between neighboring islands.

At the maximum `300×300` size, neighboring island regions still retain a `200` block separation between their boundaries.

---

## 💻 Commands

### Player Commands

| Command                     | Description                               |
| --------------------------- | ----------------------------------------- |
| `/island`                   | Display island commands                   |
| `/island create`            | Create an island                          |
| `/island home`              | Teleport to your personal island home     |
| `/island sethome`           | Set your personal island home             |
| `/island info`              | View island information                   |
| `/island level`             | View Island Level and XP                  |
| `/island value`             | View the island's block value             |
| `/island missions`          | View today's shared daily missions        |
| `/island members`           | View island members                       |
| `/island invite <player>`   | Invite a player                           |
| `/island accept`            | Accept an island invitation               |
| `/island decline`           | Decline an island invitation              |
| `/island leave`             | Leave your island                         |
| `/island kick <player>`     | Remove an island member                   |
| `/island promote <player>`  | Promote a member to Co-Owner              |
| `/island demote <player>`   | Demote a Co-Owner                         |
| `/island transfer <player>` | Transfer island ownership                 |
| `/island upgrade`           | Expand the island boundary                |
| `/island border`            | Temporarily visualize the island boundary |
| `/island resets`            | View lifetime island reset usage          |
| `/island reset`             | Request an island reset                   |
| `/island delete`            | Request permanent island deletion         |
| `/island confirm`           | Confirm a pending destructive action      |
| `/island cancel`            | Cancel a pending confirmation             |

The `/is` alias can be used in place of `/island`.

### Administration

| Command              | Description                      |
| -------------------- | -------------------------------- |
| `/skyfoundry`        | Display SkyFoundry information   |
| `/skyfoundry status` | Display plugin and island status |
| `/skyfoundry reload` | Reload the configuration         |

The `/sf` alias can be used in place of `/skyfoundry`.

---

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
- [x] Custom island level system
- [x] Block value system
- [x] Mission-based progression

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

---

## 🧩 Project Structure

SkyFoundry is designed around separate services and systems rather than placing all island functionality into a single manager.

```text
net.skyfoundry.core
├── command
├── config
├── confirmation
├── database
├── home
├── invite
├── island
├── progression
│   ├── boundary
│   ├── listener
│   └── mission
├── protection
│   └── listener
├── reset
├── service
└── world
```

Major systems remain separated so they can evolve independently as the plugin grows.

For example:

- `island` handles island data and membership
- `service` handles island lifecycle operations
- `protection` handles island access and boundary enforcement
- `progression` handles Island Level, Island Value, upgrades, and missions
- `world` handles the SkyFoundry void world

Future schematic, integration, and API systems will follow the same modular approach.

---

## 💾 Persistence

SkyFoundry uses **SQLite** for persistent server data.

Persistent systems currently include:

- Islands
- Island ownership
- Island members
- Member roles
- Individual island homes
- Reset usage
- Island size
- Island XP
- Island Value
- Daily mission selections
- Daily mission progress

The database is stored locally within the plugin's data directory.

---

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

---

## 🧪 Versioning

SkyFoundry follows semantic versioning.

Development builds currently use the `-SNAPSHOT` suffix:

```text
0.1.0-SNAPSHOT
```

Version numbers follow:

```text
MAJOR.MINOR.PATCH
```

For example:

```text
0.1.0-SNAPSHOT
0.1.0
0.2.0
1.0.0
1.1.0
1.1.1
2.0.0
```

During early development, `0.x.x` versions represent development milestones where APIs, configuration formats, commands, and database structures may still change.

The first stable production release will be **SkyFoundry 1.0.0**.

---

## 🎯 Project Goals

SkyFoundry is both a production server project and an exploration of building a Skyblock platform specifically around the challenges introduced by modern modded Minecraft.

The primary engineering goals are:

1. **Modded Compatibility**  
   Reliably handle modded blocks, block entities, entities, and automation.

2. **Safe Island Management**  
   Ensure island creation, deletion, resetting, and ownership changes behave correctly even on heavily modded islands.

3. **Reliable Protection**  
   Protect island boundaries from players, vanilla mechanics, automation, and modded systems.

4. **Persistent Data**  
   Maintain reliable island, membership, progression, mission, and configuration data using SQLite.

5. **Modular Architecture**  
   Keep the Skyblock core separated into maintainable systems rather than a monolithic island manager.

6. **Extensibility**  
   Provide a clean foundation for SkyFoundryGenerator, SkyFoundryProfessions, SkyFoundryLimits, and future server systems.

7. **Long-Term Progression**  
   Support a progression-focused Skyblock experience where islands grow through gameplay rather than simply accumulating valuable blocks.

---

## 🔗 Planned SkyFoundry Ecosystem

SkyFoundry is intended to act as the core of a larger collection of server systems.

Planned integrations include:

| Project                   | Purpose                                       |
| ------------------------- | --------------------------------------------- |
| **SkyFoundryGenerator**   | Custom island resource generation             |
| **SkyFoundryProfessions** | Player professions, perks, and specialization |
| **SkyFoundryLimits**      | Create machinery and island resource limits   |
| **SkyFoundryEvents**      | Server-wide events and seasonal systems       |

These systems are intended to integrate with the core without requiring their functionality to be built directly into SkyFoundry itself.

---

## 📜 License

SkyFoundry is currently under active development and no open-source license has been granted.

Unless a license is added in the future, the source code remains **all rights reserved**.

Viewing the public source repository does not grant permission to copy, redistribute, modify, or publish the project.

---

<p align="center">
  <strong>⚙️ Create. Expand. Ascend.</strong>
</p>

<p align="center">
  Built for the SkyFoundry Minecraft Server
</p>
