# CuffsX — 1.20.1 Fabric

CuffsX is a Fabric mod for Minecraft 1.20.1 that adds a full handcuff system for player control, administration, and roleplay scenarios.

## ✨ Features

- Two types of handcuffs: **hands** and **legs**
- Timed application and removal process (progress bar)
- Durability system — players can break free by pressing RMB (hands) or Space (legs)
- Lead system — lead a cuffed player using a vanilla lead item
- Automatic teleport if leashed player moves more than 3 blocks away
- Leg cuffs lock the player in place (zero movement speed)
- Action bar status — cuffed players always see their cuff type on screen
- Disconnect handling — leash detaches gracefully on disconnect with notifications to both parties
- Offline cuff removal via command
- Bypass permission — players with `cuffsx.bypass` cannot be cuffed
- Full command system with logs, tracking, and applies archive
- Persistent logs survive server restarts (stored in world data)
- Log entries expire individually after 24 hours; expired APPLY entries are archived
- Archived applies expire after 6 hours from archival
- LuckPerms permissions support
- Optional FTB Chunks claim integration

## 📦 Requirements

- Fabric API
- LuckPerms (required for permissions)

## ➕ Optional

- FTB Chunks 2001.3.6+ (prevents cuffing players inside claimed land)

## 🚀 Supported Versions

- Minecraft 1.20.1
- Fabric Loader 0.14+

## 📚 Wiki

See the `/wiki` folder for full documentation:
- [Commands](wiki/Commands.md)
- [Permissions](wiki/Permissions.md)
- [Mechanics](wiki/Mechanics.md)
- [FAQ](wiki/FAQ.md)

## 🔨 Build

Compiled jar is located in `build/libs/` after running:

```
./gradlew build
```
