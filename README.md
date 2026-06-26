# Aimless

A lightweight client-side Fabric mod that adds smooth aim assistance to Minecraft. Press **V** to toggle — the mod will automatically track and snap your view to the nearest player within range.

---

## Why This Exists

Built for testing, movement practice, or just messing around on servers. It's intentionally simple: no complex smoothing curves, no prediction, no fancy GUI. Just a toggle, a configurable delay, and instant snapping to the closest valid target.

---

## Features

- **Toggle with V** — Press once to enable, press again to disable. Status shows in chat.
- **Closest-target priority** — Automatically picks the nearest player in range.
- **Smart filtering** — Ignores yourself, dead players, spectators, and invisible players.
- **Reaction delay** — Configurable tick delay (`REACTION_TICKS`, default 3) so it doesn't feel instant.
- **Range limit** — Only targets within `MAX_RANGE` blocks (default 5).
- **Zero config files** — Tweak values in the source and rebuild. No JSON/TOML to edit at runtime.

---

## Requirements

| Component | Version |
|-----------|---------|
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.3+ |
| Fabric API | Latest for 1.21.11 |
| Java | 21 |

---

## Installation

### For Players
1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.11.
2. Drop [Fabric API](https://modrinth.com/mod/fabric-api) into your `mods/` folder.
3. Download the latest `aimless-<version>.jar` from [releases](https://github.com/Coder-soft/aimless/releases) and place it in `mods/`.
4. Launch the game.

### For Developers
```bash
# Clone and build
git clone https://github.com/Coder-soft/aimless.git
cd aimless
./gradlew build
```
The output JAR lands in `build/libs/`.

---

## Usage

1. Join a world or server.
2. Press **V** — you'll see "Aimless Enabled" in chat.
3. Look in the general direction of other players.
4. After the reaction delay, your view snaps to the nearest valid target's eyes.
5. Press **V** again to disable ("Aimless Disabled").

---

## Configuration

All settings are constants in `src/main/java/org/codersoft/mohenjo/aimless/client/AimlessClient.java`. Edit before building:

| Constant | Default | Description |
|----------|---------|-------------|
| `MAX_RANGE` | `5.0` | Max distance (blocks) to acquire a target |
| `REACTION_TICKS` | `3` | Delay between scans (~150 ms at 20 TPS) |
| Key binding | `V` (`GLFW_KEY_V`) | Toggle key — change to any GLFW key code |

No runtime config screen. Recompile to change values.

---

## How It Works (Brief)

On every client tick while enabled:
1. Increment an internal tick counter.
2. Once it hits `REACTION_TICKS`, scan all players in the world.
3. Filter out invalid targets (self, dead, invisible, > `MAX_RANGE`).
4. Pick the closest by Euclidean distance.
5. Calculate yaw/pitch via `atan2(deltaZ, deltaX)` and `atan2(deltaY, horizontalDist)`, applying Minecraft's −90° yaw offset.
6. Snap the player's camera instantly to those angles.
7. Reset the counter.

No interpolation, no smoothing — it's a hard snap. That's by design.

---

## Technical Notes

- **Mappings:** Yarn
- **Entrypoint:** `client` (`AimlessClient`)
- **Mixins:** One client-only mixin for key handling
- **License:** MIT

---

## Reporting Issues

Found a bug? Have a suggestion? [Open an issue](https://github.com/Coder-soft/aimless/issues) — include your Minecraft version, Fabric Loader version, and a description of what happened.

---

## License

MIT — see [LICENSE.txt](LICENSE.txt).