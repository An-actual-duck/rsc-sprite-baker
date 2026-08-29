# RSC Sprite Baker

RSC Sprite Baker turns 3D NPC models and animations into aligned, transparent
RuneScape Classic-style sprite sheets. It automatically suggests the 18 poses
needed by an RSC NPC—five movement directions plus a side-view combat
direction, with standing, left-step, and right-step frames—and lets you replace
any suggestion before exporting.

The normal desktop version is self-contained and project-free. Pick an NPC,
adjust its appearance and poses, preview the finished animation, and export.

## Download and launch

Download the archive for your operating system from the
[latest GitHub release](https://github.com/An-actual-duck/rsc-sprite-baker/releases/latest),
then extract the entire archive. Keep everything inside the extracted
`RSC Sprite Baker` folder together.

Java 11 or newer is required. The application does not bundle Java.

### Windows

Open the extracted folder and double-click:

```text
Start RSC Sprite Baker.cmd
```

Use the `.cmd` launcher, not `rsc-sprite-baker.jar`.

### Linux

Open the extracted folder and run:

```text
Start RSC Sprite Baker.sh
```

Most desktop file managers can launch it by double-clicking and choosing
**Run**. From a terminal, use:

```bash
./Start\ RSC\ Sprite\ Baker.sh
```

If the launcher lost its executable permission while being copied, restore it
once with:

```bash
chmod +x "Start RSC Sprite Baker.sh"
```

## Quick start

1. Launch the application. It opens directly to **Browse NPCs**.
2. Enter part of an NPC name, or one exact numeric NPC ID, and press **Search**
   or Enter. The browser deliberately starts empty rather than loading an
   arbitrary first page.
3. Select a result and click **Load Selected NPC**. The baker discovers the
   model's movement and combat animations and fills the sprite sheet with its
   best suggestions.
4. Choose **Original** or **Material** from **Look**, then adjust the visual
   controls if needed.
5. Inspect each direction with **Play final RSC loop**. Replace questionable
   frames from the source-pose browser and lock frames you want to keep.
6. Click **Export PNG + provenance**.

Exports are written automatically to the `exports` folder beside the launcher:

- `npc-<id>-<name>-rsc-sheet.png` is the finished transparent sprite sheet.
- `npc-<id>-<name>-sheet-provenance.json` records the source poses, directions,
  and visual settings used to make it.

Changing NPCs or closing before exporting can discard the current edits. The
application warns before doing so. Existing exports are never overwritten
without confirmation.

## Original and Material looks

The **Look** menu applies a complete group of sensible starting settings.

- **Original** preserves the model's rendered colors and texture detail. Use it
  when you want the closest representation of the source model or a reference
  for comparison.
- **Material** simplifies fine texture variation into cleaner color and shadow
  ramps. It removes much of the noisy, speckled detail found in 3D textures and
  is usually the better starting point for an RSC-style sprite.
- **Custom** appears after you alter an individual setting.
- Profiles created with **Save settings** also appear here. A saved profile
  restores all visual settings, including the selected surface-color method.

The separate **Surface colors** control lets you switch between original
material detail and simplified RSC-style material ramps without resetting all
the other controls.

Material is intended as a starting point, not a mandatory final appearance.
NPCs with important texture detail may look better in Original or with a higher
**Texture detail** value.

## Visual controls

Every numeric control has both a slider and an editable value. Changing one
updates the previews and changes **Look** to Custom.

| Control | What it changes |
| --- | --- |
| **Frame width** | Width of each exported cell. Increasing it adds horizontal canvas space; it does not enlarge the model by itself. |
| **Frame height** | Height of each exported cell. Increasing it adds vertical canvas space. |
| **Render quality** | Internal render resolution used before reducing to the export size. Higher values produce cleaner edges and material sampling, but render more slowly. It does not change final dimensions. |
| **Edge margin** | Minimum transparent space between the sprite and the edge of a frame. Increase it if limbs, wings, or weapons are being clipped. |
| **Sprite size** | Scales the model within every frame. Increase it for a larger character; decrease it when the model does not fit cleanly. |
| **Camera tilt** | Tilts the camera up or down. Use it to show more of the top or front of the model. |
| **Turn offset** | Rotates all views slightly left or right while keeping them assigned to the same direction. Useful when the source model's natural forward angle is a little off. |
| **Vertical position** | Moves the sprite up or down inside every frame without changing its size. |
| **Surface colors** | Chooses detailed original materials or simplified RSC-style material ramps. |
| **Base brightness** | Light applied to all visible surfaces. Raise it to brighten dark areas without moving the light. |
| **Directional light** | Strength of the directed light and therefore the contrast between lit and shaded sides. |
| **Light direction** | Moves the main light around the character horizontally. |
| **Light height** | Moves the main light higher or lower above the character. |
| **Color variation %** | Controls small local color differences on the same surface. `100%` preserves the normal result; lower values smooth speckling, while higher values emphasize variation. |
| **Texture detail %** | Controls how strongly decoded texture detail remains. `100%` preserves it; lower values blend it toward the underlying lit material color. |
| **Color intensity %** | Controls color saturation. Lower values mute colors; higher values make them stronger. It does not change transparency or geometry. |
| **Shadow depth %** | Strength of inner and contact shadows in the Material surface style. It does not affect Original surface colors. |

**Save settings** asks for a profile name and stores the current visual setup as
a reusable entry in **Look**. Saved profiles are local to your computer.

## Choosing and replacing frames

The final sheet has six columns:

1. Facing camera
2. Facing diagonal
3. Side
4. Diagonal away
5. Away
6. Combat side

Each column contains **Standing**, **Left step**, and **Right step** cells.
For Combat side, those three cells form the attack animation.

To replace a frame:

1. Click the destination cell in the **Final RSC sprite sheet**. Its orange
   outline identifies the cell you are editing.
2. Choose the desired **Browse source view**. This can differ from the
   destination direction when automatic direction detection is wrong.
3. Select an alternative source pose on the left. The scrubber can select a
   nearby point within its animation.
4. Click **Replace selected cell**, press Enter, or double-click the source
   pose.

Useful frame controls:

- **Keyframes only** hides in-between 20 ms animation samples for a shorter
  source list. Leave it off when you want the widest frame selection.
- **Combat sequence** switches among credible attack animations discovered for
  the NPC. It is active while browsing Combat side.
- **Lock** protects an individual cell from replacement and from Repopulate.
  Pressing `L` also toggles the selected cell's lock.
- **Repopulate** advances each unlocked cell to another suitable suggestion.
  It preserves locked cells and direction overrides. If no other valid option
  exists for a cell, that cell remains unchanged.
- **Restore canonical direction** keeps the selected pose but renders it from
  its normal destination direction. Use this to remove a per-cell direction
  override.

Cell borders show how a frame was assigned: purple is automatic, blue is a
manual pose override, gold is a direction override, green is assigned, red is
locked, and the orange outer border is the currently selected destination.

## Preview, orientation, and export controls

- **Play final RSC loop** previews the selected output direction using the
  actual assigned frames. Movement plays standing, left step, standing, right
  step; Combat side plays its three attack frames.
- **Preview speed** changes playback speed only. It does not alter the exported
  poses or source timing.
- **2× larger preview** enlarges the inspection display with nearest-neighbor
  scaling. It does not change exported pixels.
- **Horizontal inversion (face right)** mirrors the source cards, sheet,
  preview, and export. It is enabled by default to match the expected RSC
  orientation.
- **Swap facing and away** exchanges the front and rear directions, including
  their diagonals. Use it when the baker interpreted the model's front as its
  back. Side and Combat side are unchanged.
- **Export PNG + provenance** writes the 18-frame PNG and its matching JSON
  record to `exports`.

## Menus and advanced tools

- **NPC > Browse NPCs** returns to NPC selection.
- **NPC > Compatibility Details** reports whether the current model and its
  materials were decoded successfully.
- **Advanced > Combat discovery details** lists considered attack animations,
  their evidence, and rejection reasons.
- **Advanced > Manual animation sources** accepts a sequence ID when automatic
  discovery cannot find a usable movement or combat animation.
- **Advanced > Legacy palette reduction** retains fixed color-cube and ordered
  dithering controls for compatibility with older saved projects. Most users
  should start with Material, Color variation, Texture detail, Color intensity,
  and Shadow depth instead.
- **Help > About** shows a short description of the current workflow.

## Troubleshooting

- **Nothing happens when launching:** make sure the archive was extracted and
  Java 11 or newer is installed. Keep the launcher, JAR, cache, metadata, and
  license folders together.
- **Application JAR not found:** launch from inside the extracted folder and do
  not move the `.cmd` or `.sh` file away from `rsc-sprite-baker.jar`.
- **An NPC search shows nothing:** enter part of its name or an exact numeric
  ID, then explicitly press Search or Enter.
- **A model is clipped:** reduce Sprite size or increase Frame width, Frame
  height, or Edge margin.
- **The sprite is noisy or speckled:** select Material, then reduce Color
  variation and Texture detail.
- **Front and back are reversed:** enable Swap facing and away. For one unusual
  cell, browse a different source view and replace only that destination.
- **A replacement does nothing:** check whether the destination cell is locked.

## Developers and project boundaries

This repository contains the baker, tests, documentation, and neutral test
fixtures. RuneScape caches, extracted models, textures, and rendered derivative
assets are not committed to Git. Released desktop archives include the licensed,
read-only cache and provenance needed by the zero-configuration workflow.

The advanced portable-project selector, headless single-project export,
deterministic batch export, validation, and dry-run interfaces remain available
for development and automation:

- [Desktop distribution and packaging](docs/DESKTOP_DISTRIBUTION.md)
- [Advanced desktop application](docs/DESKTOP_APPLICATION.md)
- [Visual rendering pipeline](docs/RSC_VISUAL_PRESETS.md)
- [Animation compatibility](docs/ANIMATION_COMPATIBILITY.md)
- [Batch and integration handoff](docs/BATCH_HANDOFF.md)
- [Compatibility census](docs/COMPATIBILITY_CENSUS.md)

Building from source requires JDK 11 or newer and Maven. The basic test command
is:

```bash
mvn test
```
