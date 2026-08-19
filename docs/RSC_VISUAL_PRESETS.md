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
   without a global RGB cube. The renderer carries each triangle's actual 3D
   signed face light through the depth buffer. One material-bounded 5×5 pass
   joins tiny coplanar facets without washing limb and body lighting together.
   A robust per-material base tone and its brightest visible orientation select
   shadow bands; texture noise cannot select its own shade and lighting never
   invents a highlight above the base tone.
5. Isolated dark cleanup runs before ramp selection. There is no global palette
   dither. A fixed 4×4 pattern appears only in the narrow transition between
   solid base/mid/shadow bands and in the falloff behind a genuine depth
   overlap. Flat materials remain solid, transparent silhouettes are never
   outlined, and stable output coordinates prevent random frame noise.

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

### Second checkpoint: geometric ramp shading

Hands-on review approved the smoothed colors and speckle removal but found the
first checkpoint too flat. The second checkpoint restores volume from model
geometry rather than treating ordered dithering as a substitute for shading.
Rasterization records the exact two-sided face-normal light value already used
by Original colors. Two bounded 7×7 alpha-aware averaging passes consolidate
small facets into readable shadow regions. Each decoded material derives one
robust unlit base tone, then the geometric field selects an adjacent shadow,
mid, or highlight entry from the fixed ramp. This avoids both extremes tested
during development: flattening all material lighting and outlining every tiny
triangle as an independent shade patch.

The nine-NPC audit still reports zero alpha mismatch, exact-black pixels, or
isolated dark speckles in all 162 frames. It now measures 8–27 distinct
luminance values per complete sheet and 7–32 RGB colors in the busiest
individual frame. Strong-transition counts are intentionally higher than the
flat checkpoint because actual shadow boundaries have returned, while remaining
below Original colors for Dark beast, Kree'arra, Big Snake, and Penance Queen;
King Black Dragon is effectively unchanged. The external report SHA-256 is
`e5cad7972e518de19cce30e3318d37bc6286a0d6a75a2b7145d62aacc9e40361`.
Visual approval remains the deciding gate for ramp spacing and shading strength.

### Third checkpoint: depth-contact separation

The RSC bear reference exposed a different missing cue from broad illumination:
foreground legs and other overlapping parts need a narrow dark region on the
surface behind them. The renderer now carries the final visible depth and stable
face identity beside ARGB, material, and geometric light. At an internal
face-to-face boundary, a five-percent model-depth threshold distinguishes a
real overlap from an ordinary same-depth triangle seam. Only the farther
surface receives a one-ramp shadow band, expanded inward by one pixel within
that same face. Transparent neighbors never qualify, so this cannot create a
silhouette outline. The foreground surface also remains unchanged.

Neutral regressions cover foreground/body overlap, reject transparent-edge
outlining, and retain the earlier geometric-light, stable supersample,
material-boundary, palette, and alpha tests. The nine-NPC audit again reports
zero alpha mismatch, exact black, or isolated dark speckles across all 162
frames. Strong-transition counts are below Original colors for Dark beast,
King Black Dragon, Kree'arra, Big Snake, Jelly, and Penance Queen. The external
report SHA-256 is
`eef03fcd88d2eaf10b6f3adadd9b6c120d70471554efd7931e71aa356219a750`.
Java 21 clean verification passes all 276 tests. The unchanged Original-colors
29-NPC matrix passes all 522 cells, including the established Abyssal demon and
Dark beast PNG hashes.

### Fourth checkpoint: shadow-only signed lighting

Visual review of the depth checkpoint found misplaced highlights without
useful shadow definition. The cause was the reference renderer's deliberately
two-sided absolute Lambert term: a polygon pointing away from the light can
receive the same magnitude as one pointing toward it. That remains unchanged
for Original colors, but it is unsuitable for choosing RSC shade ramps.

Rasterization now carries two separate light channels. Original RGB continues
to use the established absolute reference value, preserving every reference
pixel. RSC material shading uses `max(0, signedNormalDotLight)` plus the same
ambient, directional strength, and NPC ambient/contrast adjustments. It can
move a material down by one or two ramp entries for midtone and shadow, but can
never promote a light-facing surface above the material's robust base. The
depth-contact band remains an additional shadow on only the farther surface.

Neutral tests reverse one triangle's winding and prove that its underside is
ambient-only while its upward face is brighter; a separate ramp test proves
that signed light adds shadows but never highlights. The nine-NPC audit retains
zero alpha mismatches, exact-black pixels, and isolated dark speckles. Its
external report SHA-256 is
`90fd47b5f63fd352939153803ac7557029cbf3c3b3f2e082bb4af5a79786e318`.
Java 21 clean verification passes all 278 tests. This checkpoint remains
subject to hands-on approval of shadow strength.

### Fifth checkpoint: localized dithered shadow bands

Hands-on review confirmed that removing false highlights was correct but that
solid ramp thresholds still did not resemble RSC's pixel-drawn shading. This
checkpoint uses dithering as the boundary of a detected shadow, never as a
whole-image effect. Signed illumination is measured relative to the brightest
visible orientation of the same decoded material. Well-lit, clearly midtone,
and clearly shadowed regions use solid adjacent ramp colors; only two narrow
illumination intervals alternate those adjacent colors with a fixed 4×4 mask.
A uniformly lit surface therefore has no dither at all.

The earlier two broad 7×7 illumination passes are replaced by one
material-bounded 5×5 pass so a small limb does not inherit the body's average
light. Real depth overlaps now create a solid dark core on the farther face and
a three-pixel diminishing, dithered falloff within that same face. Neither the
foreground part nor a transparent silhouette is modified.

Java 21 `mvn clean verify` passes all 281 tests. Focused regressions cover flat
surface exclusion, deterministic adjacent-ramp coverage, solid shade bands,
contact-shadow falloff, foreground preservation, and transparent-edge
exclusion. The nine-NPC terminal audit reports zero alpha mismatches, exact
black pixels, or isolated dark speckles in all 162 styled frames. Styled sheets
contain 8–35 RGB colors and 4–26 luminance levels; transition counts now include
the intentional shadow-boundary pattern and range from 17 for Big Snake to
17,757 for the large Troll control. The external report is
`/tmp/rsc-material-banded-dither.hYPzkH/report.json`, SHA-256
`b2af61f0f6ee0850a361affddc7b1c3e34769e35a3aedd3364026d3ce2938b2a`.
Visual review remains the approval gate for shadow placement and pattern
strength.

### Sixth checkpoint: smooth-normal shadow masses

Visual review found that face-normal shading still exposed disconnected pale
facets instead of readable dithered shadow masses. The alternate approach in
this checkpoint computes normals after each animation pose, uses the cache
model's averaged vertex normals for ordinary smooth faces, and interpolates the
signed shadow value across the triangle. Faces explicitly marked flat by the
model remain flat. Original-colors RGB still uses its unchanged pinned
face-light path; the interpolated signal is exclusive to the RSC material
preset.

The pattern also cannot leave a sparse lighter color inside a mostly dark
region. Each transition uses no more than 50 percent of the next darker ramp;
greater coverage becomes the next solid shadow tone. Contact shadows use a
solid two-step core followed by 50-percent, solid one-step, and 50-percent
falloff bands on the farther geometry. This produces dark marks over a lighter
material region, rather than pale marks inside a shadow.

Java 21 `mvn clean verify` passes all 282 tests. New neutral coverage proves
smooth vertex interpolation, explicit flat-face preservation, signed
back-face behavior, and solid promotion instead of pale minority pixels. The
nine-NPC audit again records zero alpha mismatches, exact-black pixels, and
isolated dark speckles. Compared with the prior localized-dither checkpoint,
strong transitions fall from 168 to 107 for Abyssal demon, 7,647 to 6,024 for
Tortoise, 9,390 to 6,817 for Kree'arra, and 4,892 to 4,808 for Penance Queen.
The external report is
`/tmp/rsc-material-smooth-shadow-solid.gMX4dt/report.json`, SHA-256
`33c216dd0441ebda8501dee0475eca93e2e6ca153e8cff9d47a32b0e791f1249`.
Hands-on review remains required before this appearance is approved.

### Seventh checkpoint: screen-space RSC form shadows

The uncolored RSC demon reference showed that its volume comes from broad
darker masses on the light-opposed silhouette, beneath overhangs, at joints,
and behind crossing limbs—not from isolated light-facing polygons. This
checkpoint therefore treats smooth vertex illumination as a secondary solid
shadow classifier rather than a dither source. It selects zero, one, or two
whole darker ramp steps and cannot create pale holes inside those regions.

A new screen-space form pass projects the configured studio light consistently
across all six directional columns. It preserves the light-facing silhouette
and creates a one-ramp dark core plus two-pixel dithered inward falloff only on
the opposed edge. Existing depth-overlap shading supplies the stronger shadow
behind foreground limbs and other crossing geometry. The two form signals use
their maximum rather than accumulating, preventing accidental over-darkening.
Transparent pixels remain untouched and the edge treatment never becomes an
external outline.

Java 21 `mvn clean verify` passes all 283 tests. Neutral coverage verifies the
light-facing corner, opposed edge, broad inward falloff, transparent exterior,
solid illumination bands, depth overlap, and deterministic dither. The
nine-NPC cache audit records zero alpha mismatches, exact-black pixels, or
isolated dark speckles. Strong transitions fall further to 91 for Abyssal
demon, 485 for Dark beast, 2,619 for King Black Dragon, 13 for Big Snake, 4,557
for Penance Queen, and 13,793 for the large Troll control. Original-colors
hashes remain exactly
`3bcfcdaea1411a8e1a51bb1fb2af8ad13d69fff669c92a6c7fd95e1bab7e02c0`
for Abyssal demon and
`e54ee8218425c92518f4c46579174991c4b70ecec80d24c18aed309de7d6e9d1`
for Dark beast. The external report is
`/tmp/rsc-material-form-shadow.pS2sdd/report.json`, SHA-256
`4732bb3725e6132547556233f73683d18db9b3d9f4cc741c4a4aa449602dc48d`.
Hands-on review remains the approval gate.

### Eighth checkpoint: detail-preserving reduction and real dark ramps

Further hands-on review identified that the flattening stage itself was
discarding facial features, narrow edges, and small part boundaries before the
new form shadows could use them. Two mechanisms were responsible. First, the
center supersample alone selected the output material, so a narrow eye or dark
edge covering two high-resolution samples could disappear. Second, two
final-resolution median passes removed supported detail along with procedural
noise. Separately, dark material bases already at the first fixed ramp entry
could not move any lower, making a requested shadow numerically identical to
the base.

Reduction now keeps center ownership for alpha and silhouettes but may select a
different decoded surface inside the same supersample block only when at least
two samples support it and its median is at least 28 luminance points darker.
There is deliberately no corresponding bright-minority rule, so small pale
facets cannot be promoted. One material-bounded median pass replaces two. Dark
features within a shared texture/material survive only when a neighboring
pixel supports the same local luminance departure; single dark texels still
fail the support test.

Shade steps are now multiplicative at 72 percent of the preceding material
tone rather than lower indexes in a globally fixed base table. Every base can
therefore produce a darker mid and shadow. The final luminance floor is 34:
the first audit at a lower floor demonstrated that intentional dither could
re-enter the near-black speckle metric, so that version was rejected rather
than checkpointed.

Java 21 `mvn clean verify` passes all 285 tests. New regressions prove that a
two-subpixel dark feature survives while a lone speck does not, and that every
base has distinct mathematical shadow steps. The final nine-NPC audit reports
zero alpha mismatches, exact-black pixels, isolated dark speckles, or temporal
speckle variation. Complete sheets retain 17–74 RGB colors across the varied
textured set, while individual frames peak at 10–47 colors. Original-colors
hashes remain unchanged. The external report is
`/tmp/rsc-material-detail-shadow-floor.ohvsHA/report.json`, SHA-256
`b8d3b3c60e024b33b0ed8e5eb0c04d200a2eeca7c82a744ded9d8d0537c9e976`.
Visual inspection must determine whether recovered eyes, part edges, and
shadows now balance correctly against the flattened material regions.

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
