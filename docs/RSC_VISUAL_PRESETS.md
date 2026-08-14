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
6. Reduce to target size by taking the center source sample of each integer
   pixel block. No smoothing or platform graphics scaling participates.
7. Optionally reduce RGB colors to a fixed cube with a deterministic 4×4 Bayer
   threshold. Alpha values are retained exactly.

The default is a 128×128 target cell rendered at 384×384 (3×) before reduction.
Widths and heights from 16 through 512, supersampling from 1× through 8×,
padding, model scale, pitch, yaw offset, and vertical position are persisted in
the project. All 18 output cells always have the same dimensions and viewport.

## Presets

Presets are starting points. Editing an individual control marks the project
as `Custom`; the resulting exact values remain persisted and appear in the
manifest.

| Preset | Camera/scale | Light | Color |
| --- | --- | --- | --- |
| Unmodified studio | 15° pitch, 0.92 scale | ambient 0.45, directional 0.55 | original rendered RGB, no dither |
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
- Textured faces are still rejected and remain explicitly Phase 4.
- Large cells and high supersampling increase CPU and memory cost; selector
  previews currently render synchronously.
