# AdvancedShadows

**A clientside Fabric mod that overlays sky light levels as colored carpets — built for finding skybases.**

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-green?style=flat-square)
![Fabric](https://img.shields.io/badge/Fabric-Loader-orange?style=flat-square)
![Environment](https://img.shields.io/badge/Environment-Client--only-blue?style=flat-square)

---

## What it does

AdvancedShadows renders colored overlay quads on top of walkable surfaces, showing you exactly where the sky can and cannot "see" — similar to a light level overlay, but focused on **skylight** instead of block light.

This is especially useful for **finding skybases**: any area that appears red or orange from above is fully or partially hidden from sky view, meaning a skybase built there would be harder to spot.

| Color | Skylight Level | Meaning |
|-------|---------------|---------|
| 🔴 Red | 0 | Completely shadowed — no sky visibility |
| 🟠 Orange | 1–7 | Heavily shadowed |
| 🟡 Yellow | 8–14 | Partially shadowed |
| *(none)* | 15 | Full sky exposure — no overlay |

---

## Features

- **F7** to toggle the overlay on/off (shown in action bar)
- Works on **solid surfaces** and **leaves** (walk on treetops and see shadows below skybase candidates)
- **Overworld only** — automatically disabled in Nether and End where skylight doesn't apply
- 16 block render radius, 4 blocks vertical range
- Pure clientside — no server-side component, works on any server

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.11
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Drop `AdvancedShadows.jar` into your `.minecraft/mods` folder
4. Launch and press **F7** in-game

---

## Keybinds

| Key | Action |
|-----|--------|
| `F7` | Toggle shadow overlay |

The keybind can be changed in **Options → Controls → Miscellaneous**.

---

## Compatibility

- ✅ Multiplayer (clientside only, no ban risk from the mod itself — check server rules)
- ✅ Singleplayer
- ✅ Works alongside other rendering mods (Sodium, Iris, etc.)
- ❌ Nether / End (intentionally disabled)

---

## License

MIT — see [LICENSE](LICENSE)