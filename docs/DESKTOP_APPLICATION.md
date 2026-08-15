# Desktop Application

## Build and launch

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

## First launch

The application opens immediately into a persistent desktop shell with a
normal taskbar entry. Its File menu and central actions provide Create New
Project and Open Existing Project; Open Recent appears once a project has been
opened successfully. Cancelling a form, correcting invalid input, or failing
to load a project always leaves a visible shell or editor rather than an
ownerless background JVM.

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
Closing an editor returns to the application shell; File > Exit terminates the
desktop process. The NPC menu
opens lazy name/ID search and detailed compatibility diagnostics. Selecting a
different NPC creates a new project so an existing project's persisted
selections are never silently repurposed. Edited projects carry a title-bar
marker and prompt to Save, Discard, or Cancel before closing or switching.

## NPC and animation discovery

The browser indexes NPC IDs from index 18 metadata without initially
decompressing every definition. It loads the first page or matching names on
demand. Selecting a result checks component models, textures, mapping modes,
and supported material operations in the background.

Known standing and walking sequences come from BAS metadata when present, with
NPC-definition animation IDs as the fallback. Combat animations are not
authoritatively identified by this metadata. The application therefore lists
nearby, decodable sequences that share the locomotion framemap, with
frame/cycle counts under the label “Likely combat (review).” Previewing a
candidate sets the source role but never assigns
or replaces a target-sheet pose. Existing suggestion, override, and lock rules
remain authoritative.

## Responsiveness and feedback

One serial background lane owns cache reads for the active project. Model and
material diagnosis, combat discovery, timeline thumbnail generation, pose
suggestions, cell/actual-size previews, project export, and save operations do
not execute on the Swing event thread. Rapid preview changes supersede queued
preview work. The status bar and indeterminate progress indicator identify the
active operation; failures are shown both in context and in an error dialog.

The NPC browser uses its own read-only cache session and reports search
progress. It closes that session with the browser. Closing the editor stops
queued work before closing the active cache store.

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

## Boundaries and limitations

- The application bundles its Java dependencies but not a Java runtime. Java
  11 or newer is required.
- Name search decodes definitions lazily and may take time on a cold cache; it
  reports progress and caps one result set at 250 entries.
- Combat ranking is intentionally conservative metadata proximity, not combat
  semantics. Users must preview and choose.
- The Phase-4 difficult-model and procedural-material limitations remain as
  documented in `TEXTURE_COMPATIBILITY.md`.
- Cache payloads, extracted assets, previews, exports, and local preferences
  are never packaged into the application JAR or committed to this repository.
