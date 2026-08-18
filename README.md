# RSC Sprite Baker

RSC Sprite Baker is a standalone tool for rendering cache-backed 3D NPC
models and their animations into consistently aligned RuneScape
Classic-style sprite sheets.

The intended first input is the local 2009scape cache. The intended first
output is a transparent six-column by three-row PNG sheet containing front,
diagonal, side, diagonal-away, away, and combat views.

## Project boundaries

- This repository contains the baker, its tests, documentation, and neutral
  test fixtures only.
- RuneScape caches, extracted models, textures, rendered derivative assets,
  credentials, and absolute machine configuration do not belong in Git.
- Spoiled Milk may consume reviewed exported sprites, but its source and this
  repository have separate Git histories.
- The Core manager coordinates this project, while implementation happens in
  the dedicated `rsc-sprite-baker-ai-1` worker.

## Current status

The current desktop distribution is a zero-configuration workflow: users
launch the platform start file directly into the NPC browser, select an NPC from the bundled
read-only licensed cache, customize the sheet, and export PNG + provenance to
the adjacent `exports` folder. No cache, project, or export path setup is part
of the ordinary workflow. See
[`docs/DESKTOP_DISTRIBUTION.md`](docs/DESKTOP_DISTRIBUTION.md) for the build,
license/provenance, and terminal inspection contract.

Phase 6 also retains deterministic single-project and manifest-driven
headless export, validation-only/dry-run modes, atomic package publication,
and a versioned asset-free handoff contract. See
[`docs/BATCH_HANDOFF.md`](docs/BATCH_HANDOFF.md) for commands, schemas, and the
remaining Core integration decisions. The advanced portable-project desktop
entry point remains available through `SelectorMain`; the packaged JAR starts
the simplified desktop by default. See
[`docs/DESKTOP_APPLICATION.md`](docs/DESKTOP_APPLICATION.md) for packaging and
end-user workflow evidence. See
[`docs/TEXTURE_COMPATIBILITY.md`](docs/TEXTURE_COMPATIBILITY.md) for Phase 4's
fail-closed textured compatibility matrix and provenance,
[`docs/RSC_VISUAL_PRESETS.md`](docs/RSC_VISUAL_PRESETS.md) for the visual
pipeline, and [`docs/ANIMATION_COMPATIBILITY.md`](docs/ANIMATION_COMPATIBILITY.md)
for the client animation trace. The deterministic all-NPC terminal scanner,
lazy browser compatibility status, and current 8,590-definition results are in
[`docs/COMPATIBILITY_CENSUS.md`](docs/COMPATIBILITY_CENSUS.md), including the
terminal-only opcode-255 forensic audit. The resolved model-underflow audit
and bounded revision-530 decoder evidence are in
[`docs/MODEL_BUFFER_UNDERFLOW_AUDIT.md`](docs/MODEL_BUFFER_UNDERFLOW_AUDIT.md).
The Phase 1 CLI remains unchanged.

## Compatibility-spike CLI

JDK 11 or newer and Maven are required. The command refuses to write into the
cache and, when run from this checkout, into the repository:

```bash
mvn test
mvn exec:java \
  -Dexec.args="--cache /path/to/user-supplied/cache \
  --output-dir /tmp/rsc-sprite-baker-output --npc 72"
```

The output is one transparent diagnostic PNG and one JSON manifest.

## Animation selector MVP

The selector project records identifiers and timing only; it contains no cache
payload. The project and output paths may be placed outside the checkout:

```bash
mvn exec:java -Dexec.mainClass=com.spoiledmilk.spritebaker.SelectorMain \
  -Dexec.args="--cache /path/to/user-supplied/cache \
  --project /tmp/troll-project.json \
  --output-dir /tmp/troll-sheet --npc 72"
```

The final RSC sheet occupies most of the editor. Select a target cell, then
choose **Standing animation**, **Walking animation**, or **Combat animation**
and a Direction in the narrower Source poses panel. The role name is primary;
its technical sequence ID is shown secondarily. Changing roles clears and
reloads the timeline immediately, so its label and cards cannot describe a
different animation.

The timeline defaults to every selectable 20 ms client cycle, including
tweened poses, rather than only encoded keyframe starts. Larger cards show
frame, cycle offset, time, and secondary sequence ID; purple `AUTO` markers
identify automatic Standing, Left step, Right step, and Combat samples. An
optional **Keyframes only** view remains available. Select a card or scrub the
timeline, then replace the outlined cell or set its shared row.

Standing and Walking come from discovered NPC/BAS metadata. Combat offers
clearly labeled candidate animations. Raw sequence IDs are hidden under
**Advanced: manual sequence override** for NPCs whose source cannot be
discovered. The five movement directions share row poses until individually
overridden. Combat-side cells remain independent, and locks and suggestions
retain their existing behavior.

The prominent **Play final RSC loop** control lives beside the final-sheet
preview. It shows the actual assigned, palette-reduced export-size poses in the
selected direction: movement directions loop Standing → Left step → Standing
→ Right step, while Combat side loops its three combat cells. Its framing is
calculated from the complete 18-cell sheet exactly like export. It is the
editor's only playback control.

Black, white, neutral gray, grass green, and custom background controls make
transparent edges easy to review. This background is composited into a
temporary 1:1 preview only; it is not stored and cannot affect sprite alpha,
PNG output, hashes, batch output, or provenance. Final playback renders away
from Swing's event thread and stops when the NPC changes or editor closes.

Double-click a timeline pose or target cell to assign it. `Enter` replaces the
selected cell, `Ctrl+1`/`Ctrl+2`/`Ctrl+3` set shared rows, `L` toggles its lock,
and `Backspace` restores the shared row pose. Green, blue, purple, and red cell
frames identify shared, override, suggested, and locked states.

The default export uses 128×128 cells and creates a transparent 768×384 PNG;
cell width and height are configurable. Every cell shares scale, ground anchor,
framing policy, camera settings, and target canvas. The mirrored-direction
control is preview-only. The diagnostic JSON records the complete visual
configuration and one source trace per cell.

## Local collaboration

The manager checkout is `/home/justin/rsc-sprite-baker`. The exclusive worker
checkout is `/home/justin/rsc-sprite-baker-ai-1`.

```bash
./scripts/ai-manager.sh status
./scripts/ai-workspace.sh start ai-1 feat/cache-compatibility-spike
```

The small wrappers reuse the hardened collaboration implementation owned by
the sibling Core repository. Product source remains independent; only local
workspace orchestration is shared.
