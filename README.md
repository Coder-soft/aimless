# Aimless

<p>
  <img alt="Requires Fabric API" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/requires/fabric-api_64h.png">
  <img alt="Fabric" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/fabric_vector.svg">
  <img alt="Forge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/unsupported/forge_64h.png">
</p>

Client-side Fabric mod — press **V** to toggle aim assist that snaps your view to the nearest player.

## How it works

Once toggled on (`/aimless` to check, `/aimless <ticks>` to set reaction delay), the mod scans nearby players (max 3 blocks) every *reaction ticks* (default 6, ~300ms at 20 TPS) and snaps your crosshair to the nearest valid player's eye position on each tick.

**What it checks:**
- Only targets human players you can see (name-tagged, valid UUID)
- Skips yourself, dead players, and players outside 3-block range
- Ignores NPCs, bots, and non-player entities

**Client-side only** — no server-side changes needed. Your rotation is updated locally via `setYaw`/`setPitch`. The `/aimless reaction` command is a Fabric client command; it resets to 6 when you rejoin.

## Install

1. [Fabric Loader](https://fabricmc.net/use/) for your MC version
2. [Fabric API](https://modrinth.com/mod/fabric-api) in `mods/`
3. Download from [releases](https://github.com/Coder-soft/aimless/releases)

## Build

```bash
./gradlew build
# JAR in build/libs/
```

MIT