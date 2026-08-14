# RSC Sprite Baker Roadmap

## Objective

Build a deterministic cache-to-sprite tool that can pose a cache-backed 3D NPC
model, render selected animation frames from fixed RSC-style views, and export
an aligned transparent sprite sheet without requiring Blender or manual screen
capture.

## Output contract

The first production preset exports:

- a transparent PNG with six columns and three animation rows;
- front, diagonal, side, diagonal-away, away, and combat columns;
- one shared crop and foot/ground anchor across every frame;
- nearest-neighbor reduction from a larger internal render target;
- a JSON manifest containing source identifiers, frame selections, model
  assembly, recolors/retextures, camera, lighting, scale, and tool version; and
- no embedded source cache payload.

## Phase 1: compatibility spike

1. Identify the cache revision and store/index layout in a user-selected input
   directory.
2. Evaluate a licensed cache decoder, beginning with RuneLite's BSD-licensed
   cache module, against the local 2009scape cache.
3. Resolve one simple untextured NPC definition into its component model IDs,
   recolors, scaling, and animation identifiers.
4. Decode and render one static pose offscreen.
5. Save a transparent PNG and a diagnostic manifest outside the repository.
6. Document format incompatibilities rather than silently substituting or
   vendoring unlicensed code.

Exit criterion: one reproducible NPC still is decoded from the configured
cache and rendered without committing any source or output asset.

## Phase 2: animation and sheet MVP

1. Decode sequence, frame, framemap/skeleton, and vertex-skin data.
2. Assemble multipart NPC models without losing animation groups.
3. Apply one idle/standing sequence, one walking sequence, and one combat
   sequence while respecting their frame durations.
4. Expose the complete animation timeline as rendered thumbnails and a
   scrubber. Where the source client interpolates between discrete keyframes,
   permit deterministic time-based sampling of those in-between poses rather
   than limiting selection to encoded keyframes.
5. Provide a labeled six-column by three-row target sheet beside the source
   timeline. Users can drag or assign source poses into target slots and lock
   their choices before export.
6. Treat the first five direction columns as three shared movement poses:
   standing, left step, and right step. Selecting a pose at the row level
   renders it from front, diagonal, side, diagonal-away, and away cameras;
   individual cells may override the shared selection when a particular angle
   needs correction.
7. Treat the sixth column as side-view combat: standing/ready followed by two
   deliberately selected moments moving into the attack. Reusing the side
   standing pose is the default, not a requirement.
8. Offer automatic suggestions for the standing pose, opposing gait extremes,
   and useful attack moments, but never make automatic selection authoritative.
   The user can preview, replace, and lock every suggested choice.
9. Normalize crop, ground anchor, scale, and frame canvas across the complete
   sheet, with a live mirrored-direction preview.
10. Save selections by sequence ID plus frame/time position in the manifest and
    export the initial 18-frame sheet.

Exit criterion: an animated NPC produces a stable RSC-layout sheet twice with
identical inputs and equivalent output, and every exported cell can be traced
back to a user-visible, persisted source pose selection.

## Phase 3: RSC visual treatment

1. Add orthographic or very-low-FOV camera presets.
2. Add restrained directional and ambient lighting controls.
3. Render above target resolution and downscale with nearest-neighbor sampling.
4. Add optional controlled palette reduction and dithering.
5. Provide onion-skin, anchor, and silhouette previews for manual adjustment.

## Phase 4: textures and difficult models

1. Decode texture/material information and UV mappings.
2. Handle transparency and alpha-tested materials.
3. Add per-NPC overrides for camera, offsets, frame choices, and scale.
4. Produce clear diagnostics for unsupported opcodes, formats, or materials.

## Phase 5: usable desktop tool

Provide a small interface containing cache path, NPC search/ID, animation
selection, source timeline/frame browser, labeled target sheet, drag-and-drop
or explicit assignment, selection locks, view preview, camera/light controls,
scale, palette preset, output directory, and export controls. Settings are
stored as portable project files without source assets.

## Phase 6: batch and Spoiled Milk integration

1. Add headless command-line export from a reviewed project file.
2. Add repeatable batch processing and output validation.
3. Establish a deliberate import step for Spoiled Milk's remastered sprite
   override system.
4. Keep generated sprites and their provenance review separate from tool
   releases.

## Deferred decisions

- Public source license and dependency policy.
- Redistribution policy for output derived from third-party game assets.
- Texture support breadth beyond the first verified cache.
- Whether additional RSC layouts or player/item/object presets belong in v1.
