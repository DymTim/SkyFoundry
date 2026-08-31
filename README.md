# ⚙️ SkyFoundry

> A custom, modded-first Skyblock core built for Minecraft 1.21.1.

**SkyFoundry** is a from-scratch Skyblock plugin built for the SkyFoundry Minecraft server, with a focus on clean architecture, modded gameplay, and future extensibility.

Currently developed and tested on **Youer 1.21.1** using **Java 21**.

![Version](https://img.shields.io/badge/version-0.1.0--SNAPSHOT-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)
![Java](https://img.shields.io/badge/Java-21-blue)

## 🏝️ Core Features

`0.1.0-SNAPSHOT` is focused on building the basic Skyblock foundation.

- Island creation and deletion
- Island homes
- Island members and invitations
- Owner, Co-Owner, and Member roles
- Ownership transfers
- Trusted players
- Island protection
- SQLite persistence
- Admin and debugging tools

## ⌨️ Commands

### Island

```text
/island
/island create
/island delete
/island home
/island sethome
/island info

/island invite <player>
/island accept
/island deny
/island leave
/island kick <player>
/island promote <player>
/island demote <player>
/island transfer <player>
/island trust <player>
/island untrust <player>
```

### Administration

```text
/skyfoundry
/sf
```

## 🧩 Future Plans

Once the core island system is stable, SkyFoundry will move toward a modular add-on architecture.

Planned systems include:

- SkyFoundry Progression
- Daily and weekly missions
- Forever milestones
- Foundry Stockpile
- Island XP and levels
- SkyFoundry Generator
- SkyFoundry Professions
- Create machine limits

## 🛠️ Development

|               |              |
| ------------- | ------------ |
| **Minecraft** | 1.21.1       |
| **Java**      | 21           |
| **Server**    | Youer 1.21.1 |
| **API**       | Paper API    |
| **Build**     | Gradle       |
| **Database**  | SQLite       |

> [!WARNING]
> SkyFoundry is currently in early development. Commands, APIs, database structures, and features may change throughout the `0.x` development cycle.

## 📜 License

Copyright © 2026 SkyFoundry. All rights reserved.
