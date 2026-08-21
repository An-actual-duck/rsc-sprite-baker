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
of the ordinary workflow. The browser begins with an empty instruction; a
name search or one exact numeric ID runs only after Search or Enter. See
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
The repeatable terminal-only creature-body review matrix, current results, and
suggested manual browse list are in
[`docs/NON_HUMANOID_VISUAL_VALIDATION.md`](docs/NON_HUMANOID_VISUAL_VALIDATION.md).
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

The final RSC sheet occupies most of the editor. Choose one of its six
directions—**Facing camera**, **Facing diagonal**, **Side**, **Diagonal away**,
**Away**, or **Combat side**—then select the Standing, Left step, or Right step
cell to complete. Clicking a sheet cell initially browses its matching source
view, but later source browsing never moves the orange destination selection.
There is no ordinary animation-role selector.

For each movement direction, the alternative-pose browser automatically
combines the discovered standing and walking animations. Combat side shows the
best current combat animation instead. The browser defaults to every
selectable 20 ms client cycle, including tweened poses, rather than only
encoded keyframe starts. Larger cards show their understandable animation
source, frame, cycle offset, and time; the sequence ID is secondary. Purple
`AUTO` markers identify the exact Standing, Left step, Right step, and Combat
samples used by automatic sheet completion. An optional **Keyframes only**
view remains available. Select a card or scrub it, then replace the
orange-outlined cell.

Standing and walking sources come from discovered NPC/BAS metadata. Combat
detection projects those locomotion keyframes from the side, rejects nearby
sequences that merely repeat locomotion or never recover from a departure, and
ranks sequences with a distinct wind-up, peak deviation, and recovery. Its
three suggestions are separate encoded frames even when one frame has an
unusually long duration. Raw source roles, candidate choices, and sequence IDs
are confined to **Advanced > Manual animation sources** for exceptional NPCs.
**Repopulate** advances every unlocked cell to its next viable alternative
within the Standing, Left-step, Right-step, or combat phase appropriate to
that cell. It preserves locks and persisted source-direction overrides and
reports cells for which no different alternative exists.
Individual cells can then be replaced from the direction browser.

The prominent **Play final RSC loop** control lives beside the final-sheet
preview. It shows the actual assigned export-size poses in the
selected direction: movement directions loop Standing → Left step → Standing
→ Right step, while Combat side loops its three combat cells. Its framing is
calculated from the complete 18-cell sheet exactly like export. It is the
editor's only playback control. When playback is paused, the enlarged final
preview returns to the exact pose assigned to the orange-selected cell rather
than the first source-animation frame. Preview speed defaults to 0.5×, with
0.75× and 1× choices; this UI clock does not change source timing or any
saved/exported data. A default-on **2× larger preview** toggle doubles the
normal preview display, producing a genuinely larger sprite (4× source pixels
instead of the normal preview's 2×). The scrollable preview keeps that scale
even when the image exceeds the available viewport, while the export retains
its configured native pixels. Transparent export-cell margins are trimmed only
from this inspection display so blank alignment space does not push the visible
character below the viewport.

The enlarged playback area always shows original model colors, including
intentional cache colors, NPC recolors, and accent regions. Textured faces
retain the pinned client face-color and material-metadata modulation instead
of displaying the procedural texture as uncolored gray. Display scaling remains
preview-only and cannot affect sprite pixels. New projects, and older projects
without an explicit orientation choice, enable **Horizontal inversion (face
right)** by default to match established RSC sprite orientation. The control
can disable inversion, and an explicitly saved choice is preserved. Sheet
thumbnails, final playback, exported PNG pixels, hashes, and provenance all use
the same orientation. Final playback renders away from Swing's event thread and
stops when the NPC changes or editor closes.

The adjacent, default-off **Swap facing and away** setting maps Facing camera
to Away, Facing diagonal to Diagonal away, and vice versa. Side and Combat
side remain unchanged. It preserves the fixed sheet positions while applying
the mapped view consistently to sheet cells, selected preview, final playback,
export, saved projects, and provenance. Older projects default to no swap, and
explicitly saved choices are retained.

Double-click a timeline pose to assign it. `Enter` replaces the selected cell
and `L` toggles its lock. Purple, blue, green, gold, and red cell frames
identify automatic, overridden, assigned, direction-overridden, and locked states.

The default export uses 128×128 cells and creates a transparent 768×384 PNG;
cell width and height are configurable. Every cell shares scale, ground anchor,
framing policy, camera settings, and target canvas. The diagnostic JSON records
the applied horizontal orientation and facing/away mapping, complete visual
configuration, and both source and rendered direction traces per cell.

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
