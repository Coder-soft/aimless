# Aimeless

A **client-side** Minecraft Fabric mod that provides aim assistance — press **V** to toggle auto-aim at the nearest player within range.

## Features

- **Toggle on/off** — Press V to enable or disable aim assistance (shows status message)
- **Closest target prioritization** — Automatically locks onto the nearest player within range
- **Smart filtering** — Skips yourself, dead players, and invisible players
- **Reaction delay** — Configurable tick delay (`REACTION_TICKS`) to feel more natural
- **Instant snap** — Immediate camera alignment to target (configurable range up to 5 blocks)

## How It Works

The mod listens on every client tick. When aim is toggled on, it waits `REACTION_TICKS` (default: 3), finds the closest valid player within `MAX_RANGE` (default: 5 blocks), and instantly snaps your camera to their eye position using trigonometric calculations (`atan2`).

## Requirements

- Minecraft **1.21.11**
- Fabric Loader **>=0.19.3**
- Fabric API

## Installation

1. Install **Fabric Loader** for Minecraft 1.21.11
2. Download the **Fabric API** jar and place it in your `mods/` folder
3. Download the `aimeless` jar and place it in your `mods/` folder
4. Launch the game

## Building from Source

```bash
./gradlew build
```

The compiled jar will be in `build/libs/`.

## Usage

1. Launch Minecraft with the mod installed
2. Join a world or server
3. Press **V** to toggle aim assistance on (you'll see "Aimless Enabled" in chat)
4. Look generally toward other players — your view will snap to the nearest valid target
5. Press **V** again to toggle off

## Configuration

Edit `AimlessClient.java` before building:

| Variable | Default | Description |
|---|---|---|
| `MAX_RANGE` | `5.0` | Maximum distance in blocks to detect targets |
| `REACTION_TICKS` | `3` | Tick delay between checks (3 ticks = ~150ms) |
| Key binding | `V` | Change by modifying `GLFW.GLFW_KEY_V` |

## Technical Details

- **Mappings:** Yarn
- **Entrypoint:** `client` (`AimlessClient`)
- **Trigonometry:** Uses `atan2` to compute yaw/pitch from delta vectors, with Minecraft's Z+ reference frame offset (-90° yaw correction)
- **License:** MIT

## License

MIT — see [LICENSE.txt](LICENSE.txt).
