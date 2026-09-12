# YUBYEOL's Canvas Grid Mod

A small client-side Fabric mod for painting on [ArtMap](https://github.com/Fupery/ArtMap) easels.
It draws a 32x32 grid over the canvas, labels every fourth line, and highlights the pixel that
your current view will paint.

## What it does

- Draws a 32x32 grid on the map that sits on the easel, with bold lines and tick labels
  (4, 8, 12, ... 28) along the top and left edges.
- While you are seated on the easel, outlines the pixel that ArtMap will paint for your current
  yaw and pitch. ArtMap chooses the pixel from the view angles, not from where the crosshair
  appears to hit, so the outline can differ from the crosshair; the outline is the one that
  matches the server.
- Shows the pixel coordinates in a corner of the screen.
- Repaints the canvas as sharp one-pixel squares from the client-side map data, so each painted
  pixel sits exactly inside its grid cell instead of bleeding under the lines.
- Has an in-game settings screen (Mod Menu, or the settings key) in English and Korean.

## What it does not do

- No automatic clicking, aiming, or colour selection. It only draws on your screen.
- No packets are sent to the server and no network connections are made.
- No automatic updates. New versions are published as new releases only.

## Keys

| Key | Default | Action |
| --- | --- | --- |
| Toggle canvas grid | `G` | Turn the overlay on or off |
| Open canvas grid settings | unbound | Open the settings screen (also available from Mod Menu) |
| Save alignment debug file | unbound | Writes frame and view numbers to `config/yubyeol_canvas_grid/` for bug reports |

All keys can be changed in Options > Controls.

## Settings

Everything below can be changed in the settings screen; hover a control for a short explanation.
A Korean user guide with the reasoning behind each setting is in `docs/사용설명서.md`.
The values are stored in `config/yubyeol_canvas_grid.properties`.

| Setting | Default | Meaning |
| --- | --- | --- |
| Overlay | on | Master switch, same as the toggle key |
| Sharp pixels | on | Repaint the canvas pixels as sharp squares under the grid |
| Line width | 2 | Thickness of the grid lines, 1 to 5 |
| Line opacity | 45 % | Opacity of the thin lines; bold lines are drawn stronger |
| Grid colour | black | One of ten preset colours |
| Pointer colour | red | Colour of the pixel outline |
| Pointer width | 3 | Thickness of the pixel outline, 1 to 5 |
| Bold lines | on | Draw every n-th line bold |
| Bold every | 4 | 2, 4, 8 or 16 pixels; also sets where tick labels go |
| Tick labels | on | Numbers along the top and left edges |
| Pointer outline | on | Outline of the pixel the view will paint, while seated |
| Pixel coordinates | on | Coordinates text on the HUD |
| Coordinates position | bottom left | Any of the four screen corners |
| Easel search range | 6 blocks | How far to look for an easel |

`surfaceOffset` (blocks, -0.1 to 0.1) exists only in the file, for servers that draw frames at an
unusual depth.

## Requirements

- Minecraft 1.21.4
- Fabric Loader 0.16 or newer
- Fabric API
- Mod Menu (optional, for the settings button in the mod list)

## Building

```
./gradlew build
```

The jar is written to `build/libs/`. The build is the standard Fabric Loom setup, so anyone can
rebuild it and compare the result with a published release.

## Releases

Each release lists the SHA-256 of the jar and a VirusTotal scan link.

## License

MIT. The cursor angle table in `CursorTable.java` reproduces the lookup table that the ArtMap
plugin uses, so that the highlighted pixel is the one the server paints.
