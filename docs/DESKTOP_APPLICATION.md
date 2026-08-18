# Desktop Application

## Build and launch

For the normal end-user archive and its no-setup workflow, start with
[`DESKTOP_DISTRIBUTION.md`](DESKTOP_DISTRIBUTION.md). The material below also
documents the retained advanced project-oriented interface.

RSC Sprite Baker requires Java 11 or newer. Build the tested, dependency-bundled
application JAR with:

```bash
mvn clean verify
```

The runnable artifact is `target/rsc-sprite-baker.jar`. It contains the
application, Gson, RuneLite cache dependencies, its main-class manifest, and
`META-INF/THIRD_PARTY_NOTICES.md`. Start it in any of these ways:

```bash
java -jar target/rsc-sprite-baker.jar
./scripts/rsc-sprite-baker
```

On Windows, run `scripts\rsc-sprite-baker.bat`. The scripts report a missing
JAR or Java installation without invoking Maven. `Main` remains the Phase-1
CLI and `SelectorMain` retains the argument-driven technical selector entry
point; packaging changes neither interface.

Phase 6 headless export uses `scripts/rsc-sprite-baker-headless` or
`scripts\rsc-sprite-baker-headless.bat`. Its single-project, batch,
validation-only, dry-run, and handoff package contracts are documented in
[`BATCH_HANDOFF.md`](BATCH_HANDOFF.md).

## Advanced project-oriented launch

Launching `SelectorMain` with explicit arguments opens the retained portable
project editor. The former guided project forms remain in the codebase for
this advanced workflow; they are not exposed by the packaged JAR's ordinary
`DesktopMain` entry point. The ordinary desktop instead opens the NPC browser
directly and discovers the adjacent cache and exports folders automatically.

Create New Project is one owned form. It displays independently browsable and
clearly labeled fields for:

1. a JS5 cache directory containing `main_file_cache.dat2` and
   `main_file_cache.idx255` directly;
2. a project location and separate project filename;
3. an export directory; and
4. an initial NPC ID, after which the full NPC browser is available.

The `.json` suffix is added automatically and the resulting path is shown in
the form. Validation errors remain inline without closing the form. A replace
warning appears only when that exact resulting JSON file already exists.
Open Existing Project uses the same consolidated approach but accepts an
existing JSON file and obtains its NPC ID and visual/pose behavior from it.

Cache and export paths are deliberately not written into the portable project.
Their latest safe locations and per-project associations are stored in
`~/.rsc-sprite-baker/preferences.json` for reuse and for the ten-entry recent
project menu. A missing cache, stale recent path, malformed project, or
incompatible NPC produces a user-visible error instead of silently changing
inputs.

The editor File menu provides New Project, Open Project, Open Recent, Save,
Save As, export-folder selection, and Exit through owned platform controls.
Closing an advanced editor terminates its standalone process when no other
window remains; File > Exit terminates the desktop process explicitly. The NPC menu
opens lazy name/ID search and detailed compatibility diagnostics. Selecting a
different NPC creates a new project so an existing project's persisted
selections are never silently repurposed. Edited projects carry a title-bar
marker and prompt to Save, Discard, or Cancel before closing or switching.

## NPC and animation discovery

The browser indexes NPC IDs from index 18 metadata without initially
decompressing every definition. Its initial result list is deliberately empty
and explains that the user can type a name or exact ID or select metadata
filters. Typing starts a live search after a 300 ms debounce; a newer text or
filter change cancels and supersedes the older request, whose progress and
results cannot update the new query. An entire numeric query uses direct exact-
ID lookup. General searches remain capped at 250 results.

Whitespace-separated text terms are case-insensitive and all must occur in the
combined NPC ID/name text. The text condition is always ANDed with the metadata
tag group. **All** requires every selected tag; **Any** requires at least one.
A blank text query with tags searches by metadata alone. Blank text with no
tags restores the empty instruction rather than listing arbitrary definitions.

The tags are derived from decoded NPC/BAS metadata, not names or manually
maintained NPC lists:

- **Automatic animations available** means resolved standing and walking IDs
  are both present; **Needs manual animation selection** is its complement.
- **Multipart model** means the definition references more than one component
  model.
- **Uses recolors** and **Uses retextures** come from their respective mapping
  arrays.
- **Altered model scale** means width or height scale differs from 128.
- **Morph/internal definition** means morph metadata is present or the
  definition has no component model.

Selecting a result checks component models, textures, mapping modes, and
supported material operations in the background. Search/filter work and full
compatibility diagnosis never run on Swing's event thread. The cache session
remains read-only and closes with the browser.

Known standing and walking sequences come from BAS metadata when present, with
NPC-definition animation IDs as the fallback. Combat animations are not
authoritatively identified by this metadata. The detector builds a normalized
side-view geometry baseline from every encoded standing and walking keyframe,
then examines nearby sequences whose complete frame set uses the locomotion
framemap set. A candidate must contain at least three visibly distinct poses,
depart from that locomotion baseline, and recover toward it; frozen sequences,
locomotion duplicates, one-way death poses, and mismatched framemaps are
rejected. The bounded sequence-ID neighborhood and frame count remain
secondary ranking signals. Animation roles, candidate selection, and raw
numeric sequence IDs are absent from the normal workflow. They remain
available under **Advanced > Manual animation sources** for definitions
without usable discovery. Existing overrides and locks remain authoritative
when detection refreshes recommendations.

## Source selection and playback

The editing path is deliberately linear:

1. Choose a direction or click a cell in that direction's sheet column.
2. Select the Standing, Left step, or Right step cell to replace.
3. Choose one of that direction's complete 20 ms alternative poses or scrub
   within its source animation.
4. Replace the selected cell.

The six primary choices are **Facing camera**, **Facing diagonal**, **Side**,
**Diagonal away**, **Away**, and **Combat side**. Direction choice and sheet
column selection stay synchronized in both directions. The ordinary editor
does not ask the user to select or assign Standing, Walking, or Combat
animation sources.

The final sheet and its animation preview receive roughly three quarters of
the normal 1700×980 editor. The narrower source panel uses larger pose cards,
two per row at its default width, with comfortable spacing and vertical
scrolling. The 3×6 placement grid is centered at a bounded compact size with
smaller thumbnails. Its compact direction header is laid out separately from
the three equal-height pose rows, so Standing, Left step, and Right step remain
visible instead of losing a full sprite row to header spacing. The former
row-setting button stack is replaced by one
prominent **Auto populate** action. It recomputes all 18 recommended cells,
replaces unlocked cells, preserves locks, and uses the movement recommendation
as the existing bounded fallback when no combat animation can be discovered.
When cache metadata supplies walking but no separate standing animation, cycle
zero of that walking animation supplies the visible Standing row without
inventing or persisting a standing sequence ID. Definitions missing both
automatic sources remain fail-closed.

For a movement direction, the browser combines every viable pose from the
discovered standing and walking animations; for Combat side it contains only
the current best combat poses. The complete timeline is the default. It
includes one entry per client cycle, so tweened positions between encoded
keyframe starts are selectable. Every entry leads with Standing, Walking, or
Combat as its understandable source, then displays encoded frame index, cycle
offset, and milliseconds; sequence ID remains secondary diagnostic detail.
Entries matching the automatic suggestions are highlighted and
labeled `AUTO Standing`, `AUTO Left step`, `AUTO Right step`, or `AUTO Combat
1–3`. Movement suggestions retain their timeline formulas. Combat suggestions
are distinct encoded wind-up, maximum-deviation, and recovery frames selected
from the side-view analysis; elapsed-time thirds can no longer land all three
slots inside one long frame. **Keyframes only** is an optional compact view.
All poses use one stable direction-specific viewport, and every sample passes
its own frame/cycle identity to the poser; multi-frame animations do not reuse
frame zero.

The source status identifies the selected direction and destination sheet row
first, followed by the alternative's source, frame, cycle, time, and secondary
sequence ID. Changing direction, manual source, or keyframe mode invalidates
the old result before loading its replacement; stale asynchronous results
cannot repopulate another direction. Selecting an alternative also updates the
final 1:1 preview before assignment without mutating the sheet.

**Play final RSC loop** is the primary playback action and sits inside the
final-sheet preview area rather than the bottom action bar. It uses the normal
poser, full 18-cell export viewport, palette reducer, selected direction, and
actual configured cell dimensions. Swing presents those pixels 1:1 with no
smooth scaling. Movement columns loop Standing, Left step, Standing, Right
step; Combat side loops its three independently assigned combat poses. Each
pose remains visible for its selected source frame's encoded duration.

There is no source-only playback control. The final assembled loop supports
pause/resume and stops on NPC changes, editor closure, or invalidated project
state.

The final playback/preview area is wider and taller. It always uses original
model colors and offers black, white, neutral-gray, grass-green, and custom
background choices plus an obvious current-color swatch. `PreviewCompositor`
preserves sprite RGB and alpha, then creates a separate opaque
background-composited image. Background choice is ephemeral UI state: it is
absent from the project schema, exporter, batch processor, manifest, and
hashing paths, and the palette-reduced transparent render is never mutated.

## Responsiveness and feedback

One serial background lane owns cache reads for the active project. Model and
material diagnosis, combat discovery, timeline thumbnail generation, pose
suggestions, cell/actual-size previews, project export, and save operations do
not execute on the Swing event thread. Final playback pre-renders its bounded
three/four-pose loop in the same background lane. Rapid preview changes
supersede queued preview work.
The status bar and indeterminate progress indicator identify the active
operation; failures are shown both in context and in an error dialog.

The NPC browser uses its own read-only cache session and reports search
progress. It closes that session with the browser. Closing the editor stops
queued work before closing the active cache store. When an NPC choice replaces
an editor, the old editor is explicitly marked as a programmatic replacement;
its close callback therefore cannot reopen another NPC browser beside the new
editor. An intentional close of a transient editor still returns to browsing.

Save and export operate on an immutable project snapshot. If editing continues
while background I/O runs, the completed operation does not incorrectly mark
newer edits as saved.

## 2026-08-14 end-user evidence

A second isolated first-run exercise covers the persistent shell and
consolidated forms added after the original wizard test. With Java 21 and an
empty `user.home`, the shaded JAR:

- opened a durable `RSC Sprite Baker` application window with its own WM class
  and taskbar entry;
- kept the Create form open with inline errors when its required paths were
  empty, then returned to the visible shell when that form was cancelled;
- accepted the cache, project location, `exact-isolated-project` filename,
  export location, and NPC 72 in one form and created exactly
  `exact-isolated-project.json`;
- reopened that project through Open Existing and again through Open Recent;
- restored the shell when the editor closed; and
- terminated the Java process cleanly when the shell closed.

The isolated project, preferences, and all runtime output remained under
`/tmp` and outside Git. No file in the read-only cache was modified.

A clean workflow was exercised against the read-only cache at
`/home/justin/2009scape/Server/data/cache`, with all output under
`/tmp/rsc-phase5-first-run-20260814`:

- the shaded JAR displayed its first-run welcome flow with an isolated empty
  home directory;
- the cache index exposed 8,590 NPC definition IDs;
- name search found Troll definitions and exact-ID search resolved NPC 40 as
  Shark/model 2848;
- a new portable NPC-72 project was created, saved, added to isolated recents,
  and reopened;
- BAS 42 populated standing sequence 286 and walking sequence 283;
- candidate discovery presented 285, 287, and 284 as review-required
  same-framemap possibilities without setting `combatSequenceId`; and
- the desktop editor opened at 1700×980, completed its timeline and actual-size
  previews, displayed “Untextured model,” and returned to Ready status.

Neutral screenshots and all cache-derived previews remained outside Git. No
file under 2009scape was changed.

## 2026-08-18 live browser-filter evidence

A terminal-only metadata audit exercised the production `NpcCatalog` against
all 8,590 definitions in the pinned read-only cache. Every definition received
exactly one of the complementary animation tags. Counts were:

| Metadata tag | Definitions |
| --- | ---: |
| Automatic animations available | 7,466 |
| Needs manual animation selection | 1,124 |
| Multipart model | 5,261 |
| Uses recolors | 4,940 |
| Uses retextures | 1,380 |
| Altered model scale | 1,423 |
| Morph/internal definition | 612 |

Blank criteria returned zero results. Exact ID 50 returned King Black Dragon
directly; text terms `king dragon` plus the All/Multipart filter returned IDs
50 and 2642; a blank-text All query for Uses retextures plus Altered model
scale returned 219 definitions. A deliberately cancelled broad query stopped
after 129 cancellation checks rather than scanning the remaining definitions.

Java 21 `mvn clean verify` passes all 262 tests. The exhaustive compatibility
census remains byte-identical at SHA-256
`360ab988150e65c42cadc1dc46f7fbd480e0b7b8413d8595a0c16f4fe0d04e10`:
6,926 ready, 1,051 missing automatic animations, 612 morph/internal, and one
unsupported material, with no model or other failures. The shaded JAR is
6,408,437 bytes at SHA-256
`2eb020a68f7ed61ac97de3f3319a930efe4633b75176bd867eb0c50a65ec4c5a`.

The licensed-cache distribution builder and terminal inspector accepted the
77,117,605-byte Linux archive at SHA-256
`b06ab324440b264acc777baeb4507b46fbbe1d68ae5284f286ca5da7edd21718`
and the 77,118,461-byte Windows archive at SHA-256
`f655b0bfdc976e9ab525c38aab5931a1546bc3c750b3448c0c211394fd92b6ce`.
No GUI was launched or automated, the source cache remained unchanged, and no
cache payload, report, or generated derivative was added to Git.

## Boundaries and limitations

- The application bundles its Java dependencies but not a Java runtime. Java
  11 or newer is required.
- Name and metadata search decode definitions lazily and may take time on a
  cold cache; they report progress, supersede stale requests, and cap one
  result set at 250 entries. Exact numeric IDs use direct lookup.
- Combat discovery is a bounded side-view motion heuristic, not authoritative
  cache metadata. It requires distinct departure and recovery poses and leaves
  manual sequence/frame selection available for exceptional NPCs.
- Original-color preview and export retain the pinned textured-face packed-HSL
  modulation, including intentional NPC recolors and model-authored accents.
- The Phase-4 difficult-model and procedural-material limitations remain as
  documented in `TEXTURE_COMPATIBILITY.md`.
- Cache payloads, extracted assets, previews, exports, and local preferences
  are never packaged into the application JAR or committed to this repository.
