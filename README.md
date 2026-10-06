# Legacy Mod Menu (Minecraft 1.12.2 Forge)

1.12.2 Forge port of Mod Menu (originally by Prospector & TerraformersMC).
Enhances and modernizes Minecraft's mod list screen with search, sorting, tag/badge filters, parent-child mod hierarchies, and in-game configuration access.

## Features
- **Modern Mod List Screen**: Replaces the default mod list with a responsive split-pane interface.
- **Search & Filtering**: Search mods by name, mod ID, author, summary, or badge keywords (`forge`, `liteloader`, `cleanroom`, `client`, `library`, `deprecated`, `modpack`).
- **1.12.2 Loader Badges**:
  - `Forge`: Regular Forge mods.
  - `LiteLoader`: Detects and labels LiteLoader mods when running in a Forge + LiteLoader environment.
  - `Cleanroom`: Detects and labels Cleanroom mods / CleanroomMC environment.
  - `Client`, `Library`, `Deprecated`, `Modpack`, `Minecraft`.
- **In-Game Configuration**: Directly opens mod config GUIs (`IModGuiFactory`) from the mod menu.
- **Menu Customization**: Customize title screen and pause menu buttons (Classic, Shrink, Shrink Left, Icon, Replace).
- **Shortcut Key**: Default keybinding to open the mod list at any time.

## Building
Requires JDK 8+ and uses RetroFuturaGradle:
```bash
./gradlew jar reobfJar
```
The compiled jar will be located in `build/libs/`.
