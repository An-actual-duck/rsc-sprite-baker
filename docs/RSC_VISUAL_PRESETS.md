# Phase 3 RSC visual treatment

## Rendering pipeline

Phase 3 is a separate configurable path around the deterministic Phase 1/2
software rasterizer. The original 256×256 Phase 1 entry point retains its
fixed camera, lighting, and pixels.

Each sheet export performs the following operations in order:

1. Pose and assemble all 18 models using the persisted Phase 2 selections.
2. Project every posed model at its column yaw plus the configured yaw offset.
3. Calculate one origin-centered horizontal extent and one ground anchor across
   the complete sheet at the configured orthographic pitch.
4. Apply the user scale to that shared fit. Vertical position is an explicit
   output-pixel offset and never changes one cell independently.
5. Rasterize each cell at `target size × integer supersample` with the shared
   viewport and the configured ambient/directional light.
6. Reduce to target size. Original styles take the center source sample of
   each integer pixel block. The explicit RSC material style instead tracks
   the decoded material or untextured packed-color surface through the depth
   buffer and chooses a robust median only from that same surface.
7. Optionally reduce RGB colors to a fixed cube with a deterministic 4×4 Bayer
   threshold. Alpha values are retained exactly.

The default is a 128×128 target cell rendered at 384×384 (3×) before reduction.
The zero-configuration default preserves original rendered colors and applies
no palette reduction or dithering. Reduced palettes remain explicit advanced
style choices.
Widths and heights from 16 through 512, supersampling from 1× through 8×,
padding, model scale, pitch, yaw offset, and vertical position are persisted in
the project. All 18 output cells always have the same dimensions and viewport.

## Presets

Presets are starting points. Editing an individual control marks the project
as `Custom`; the resulting exact values remain persisted and appear in the
manifest.

| Preset | Camera/scale | Light | Color |
| --- | --- | --- | --- |
| Original colors (default) | 12° pitch, 0.90 scale | ambient 0.52, directional 0.40 | original rendered RGB, no dither |
| Unmodified studio | 15° pitch, 0.92 scale | ambient 0.45, directional 0.55 | original rendered RGB, no dither |
| RSC material | 12° pitch, 0.90 scale | ambient 0.54, directional 0.36 | material-aware reduction, restrained fixed chroma/light ramps, no blanket dither |
| RSC crisp | 12° pitch, 0.90 scale | ambient 0.52, directional 0.40 | fixed 5×5×5 RGB cube, no dither |
| RSC restrained | same as crisp | same as crisp | 5×5×5 cube, 4×4 ordered dither at 0.30 strength |
| RSC coarse | 10° pitch, 0.88 scale | ambient 0.56, directional 0.34 | 4×4×4 cube, 4×4 ordered dither at 0.40 strength |

The light direction is controlled as azimuth and elevation and converted to a
normalized vector. Lighting remains deliberately simple and restrained:
two-sided Lambertian face shading plus the decoded NPC ambient/contrast
adjustments. `Unmodified color` means no post-render palette conversion; it
does not bypass source HSL conversion or lighting.

The palette menu also exposes the coarser 3×3×3 cube for deliberate stylized
experiments. Ordered dithering is spatially fixed to output coordinates, so it
is reproducible and does not shimmer between frames. Dithering never changes
transparent pixels.

## RSC material first visual checkpoint

The new **RSC material** preset is separate from **Original colors**, whose
rendering path and pixels remain unchanged. It addresses the diagnosed noise
before ordinary palette reduction:

1. Rasterization writes a stable surface identity beside depth and ARGB. A
   textured surface is keyed by resolved material ID; an untextured surface is
   keyed by its post-NPC-recolor packed HSL value.
2. The output pixel's center sample still owns alpha and surface selection, so
   silhouettes exactly match Original colors. Within that selected surface,
   the supersample block uses a channel median instead of a single potentially
   dark procedural texel.
3. Two bounded 3×3 median passes operate only among pixels with the same
   decoded surface identity. Strong material boundaries and alpha edges cannot
   bleed into one another.
4. Fixed chroma families and six light/mid/shadow levels produce bold regions
   without a global RGB cube. A restrained one-step silhouette shade supplies
   edge definition, while a minimum luminance prevents isolated near-black
   texture values from becoming black outlines.
5. Isolated dark cleanup runs before and after ramp selection. Blanket ordered
   dithering is intentionally disabled in this checkpoint: the diagnosis found
   that it added single-pixel transitions to already textured materials. The
   treatment will add dithering only if visual review identifies a specific
   smooth transition where it improves the result without temporal noise.

`MaterialStylizationAuditMain` is a terminal-only, cache-backed comparison for
Abyssal demon 1615, Dark beast 2783, King Black Dragon 50, Tortoise 3808,
Kree'arra 6222, Big Snake 3484, Jelly 1637, Penance Queen 5247, and the
untextured Troll 72 control. It exports Original and RSC-material sheets
outside Git and records alpha-mask differences, exact black pixels, isolated
dark speckles, strong interior transitions, palette size, per-frame speckle
range, and per-frame palette size. Neutral tests additionally move one dark
texel within a supersample block and require identical output, guarding the
most direct source of animation shimmer.

Run it after building the shaded JAR:

```bash
java -cp target/rsc-sprite-baker.jar \
  com.spoiledmilk.spritebaker.MaterialStylizationAuditMain \
  --cache /path/to/user-supplied/cache \
  --output /tmp/rsc-material-audit.json \
  --exports /tmp/rsc-material-audit-exports
```

The first checkpoint's pinned-cache run produced zero alpha-mask mismatches,
zero exact-black pixels, and zero isolated dark speckles in every one of the
162 styled frames. Strong interior RGB transitions and complete-sheet palette
sizes changed as follows:

| NPC | Transitions original → styled | RGB colors original → styled |
| --- | ---: | ---: |
| 1615 Abyssal demon | 1,220 → 5 | 756 → 11 |
| 2783 Dark beast | 1,660 → 886 | 873 → 16 |
| 50 King Black Dragon | 3,340 → 2,663 | 1,201 → 15 |
| 3808 Tortoise | 11,573 → 6,377 | 5,548 → 27 |
| 6222 Kree'arra | 10,576 → 5,750 | 1,400 → 13 |
| 3484 Big Snake | 450 → 1 | 516 → 9 |
| 1637 Jelly | 346 → 114 | 342 → 20 |
| 5247 Penance Queen | 18,347 → 3,372 | 8,990 → 24 |
| 72 Troll, untextured control | 4,077 → 4,021 | 314 → 5 |

The maximum palette used by any individual styled frame is 24 colors. The
external comparison report SHA-256 is
`df697dd44938c880346f46b771260e3560927779fe560db5db650837a19e0e74`.
The complete 29-NPC Original-colors matrix still passes all 522 cells; Abyssal
demon and Dark beast retain their established Original-colors PNG hashes
`3bcfcdaea1411a8e1a51bb1fb2af8ad13d69fff669c92a6c7fd95e1bab7e02c0`
and
`e54ee8218425c92518f4c46579174991c4b70ecec80d24c18aed309de7d6e9d1`.
These measurements establish a safe technical checkpoint, not final aesthetic
approval. The branch remains active for hands-on review and tuning.

## Revision-530 packed-HSL color

Model face colors and NPC recolor targets are packed HSL values, not direct
RGB. The pinned client constructs its 65,536-entry raster palette with the
half-bin hue and saturation offsets, 128 lightness steps, per-channel power,
integer `* 256` narrowing, and nonzero-black rule in
[`Rasteriser.calculateBrightness`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/Rasteriser.java#L3151-L3228).
The default client preference is brightness 3, selecting exponent 0.7 in
[`DisplayMode`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/DisplayMode.java#L245-L260).

Sprite Baker now uses that exact 0.7 conversion deterministically for model
and textured-face modulation. It intentionally omits the client's random
plus-or-minus 0.015 display perturbation because identical exports must hash
identically. Post-render palette cubes are optional style transforms and are
not pinned-client color behavior.

## Selector behavior

The target grid communicates assignment origin directly:

- green: shared row assignment;
- blue: individual override;
- purple: automatic suggestion;
- red: locked; and
- orange outer frame: currently selected cell.

Double-clicking the timeline or a target cell assigns the current source pose.
Keyboard shortcuts are `Enter` for replace, `Ctrl+1` through `Ctrl+3` for
shared rows, `L` for lock, and `Backspace` to restore a shared pose. Existing
buttons remain available. Suggestions only fill empty unlocked cells and never
replace a shared pose, override, or prior suggestion.

The actual-size preview uses the same shared viewport and visual pipeline as
export. The larger grid thumbnails are presentation-scaled for convenient
selection; they are not export pixels. Mirroring remains a preview behavior and
is recorded separately from the actual camera directions.

## NPC-72 comparison evidence

Comparison renders were generated outside Git from the same NPC-72 standing
286, walking 283, and attack-probe 284 selections, using 128×128 cells and 3×
supersampling:

| Preset | External PNG SHA-256 |
| --- | --- |
| Unmodified studio | `f27d53f069358c7a5a8cfa217bb77e5d727341e0907cde04d86a387089ca69ac` |
| RSC crisp | `4dddd30eee232c5782ec605d1710fbd8fb552ecb0f9ff493858a79b97b2f8b3c` |
| RSC restrained | `c1d71e5f553a918d8374d3a5db467bf07e6f6ecf71fbf0845ff5001afa80b1f5` |
| RSC coarse | `da2418b39570a64e30d1c5eee785035c2a6c290caa16ce5fa67c1652c358d015` |

Two independent restrained exports produced the same PNG hash above and the
same manifest SHA-256
`c843c3c8d4b5ab59a875787599758b9a7b56f42ffd7768ef936eba1d60636038`.
The legacy Phase 1 probe still produced
`b6d9ebd11c681dc61e40b5a5e4e063326a2e0071a0f7f2e57a178bf5c181e758`.
No comparison PNG, project, cache data, or decoded asset is committed.

## Known limitations

- The renderer remains orthographic. A low-FOV perspective alternative is not
  necessary for the first RSC preset and is not implemented.
- Palette cubes and ordered dithering are controlled stylistic tools, not a
  claim to reproduce the original RSC palette or rasterizer exactly.
- Face priorities, clipping, animation blending, equipment overrides, and
  non-65535 framemap masks remain outside the current renderer.
- Textured faces use the fail-closed material pipeline documented in
  `TEXTURE_COMPATIBILITY.md`; absent material metadata remains unsupported.
- Large cells and high supersampling increase CPU, memory use, and background
  preview latency.
