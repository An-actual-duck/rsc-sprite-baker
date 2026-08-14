# Headless Batch Export and Handoff Contract

## Commands

Build the dependency-bundled application JAR with `mvn clean verify`. Java 11
or newer is required. The desktop launcher and the Phase 1/2 technical entry
points remain unchanged. Phase 6 adds these non-Maven commands:

```bash
./scripts/rsc-sprite-baker-headless single \
  --cache /path/to/cache \
  --project /path/to/reviewed-project.json \
  --output-dir /path/to/new-or-owned-handoff \
  --name troll-default

./scripts/rsc-sprite-baker-headless batch \
  --cache /path/to/cache \
  --batch-manifest /path/to/reviewed-batch.json \
  --output-dir /path/to/new-or-owned-handoff
```

Windows uses `scripts\rsc-sprite-baker-headless.bat` with the same arguments.
The shaded JAR can also be invoked directly:

```bash
java -cp target/rsc-sprite-baker.jar \
  com.spoiledmilk.spritebaker.HeadlessMain batch ...
```

Add `--dry-run` for schema, identity, assignment, model/material, animation,
dimension, mapping, and collision checks without rasterization. Add
`--validate-only` to render and verify output hashes, dimensions, visible
pixels, and transparency in private staging without publishing sprites. A
validation report is written beside the requested output path. `--report
/outside/path.json` selects a different report location; reports may not be
placed inside the publishable package, cache, or tool checkout.

Exit status is 0 for an accepted/validated run, 1 for entry validation
failures, and 2 for command or manifest errors. The deterministic JSON report
is also printed to standard output.

## Batch manifest schema 1

The batch manifest is versioned, contains no assets, and resolves project
paths relative to its own directory. Absolute paths and paths escaping that
directory are rejected. A reviewed batch must pin both cache identity hashes:

```json
{
  "schemaVersion": 1,
  "cache": {
    "dataFileSha256": "<64 lowercase hex characters>",
    "referenceIndexSha256": "<64 lowercase hex characters>"
  },
  "entries": [
    {
      "id": "npc-72-default",
      "project": "projects/npc-72.json",
      "outputName": "npc-72-default",
      "mapping": {
        "assetKind": "npc",
        "gameId": 72,
        "variant": "default"
      },
      "expected": {
        "projectSha256": "<optional SHA-256>",
        "pngSha256": "<optional SHA-256>",
        "provenanceSha256": "<optional SHA-256>",
        "sheetWidth": 768,
        "sheetHeight": 384
      }
    }
  ]
}
```

`id`, `outputName`, and `variant` use portable filename characters and are at
most 80 characters. Entry IDs, output names, and `(assetKind, gameId,
variant)` mapping targets must be unique case-insensitively. The mapping NPC
ID must match the project NPC. Expected values are assertions, never values
used to alter an export.

Project schemas 1 and 2 are accepted because schema 1 is an existing supported
Sprite Baker migration. Migration occurs in memory and is reported as a
warning; the reviewed source file and its hash are not changed.

## Validation and transaction boundary

Every run computes the dat2 and idx255 identities. Each entry validates:

- supported project schema and visual settings;
- all 18 assigned cells, sequence/frame bounds, frame duration positions, and
  pose decoding;
- NPC/model decoding and fail-closed texture/material diagnostics;
- theoretical six-by-three output dimensions;
- portable and collision-free names and mapping targets; and
- every supplied project and dimension expectation.

Normal and validation-only modes additionally verify the rendered dimensions,
an alpha-capable PNG containing both transparent and visible pixels, and every
supplied PNG/provenance hash. Dry-run reports supplied output hashes as
unchecked rather than pretending raster output was validated.

Normal export renders every entry beneath a sibling temporary directory. The
mapping and report are created there only after all entries pass. Publication
uses filesystem atomic renames. A failed run deletes staging and writes a
failure report beside the requested destination; it does not change an
existing accepted package.

For safety, an existing output directory is replaceable only when it contains
the `.sprite-baker-handoff-v1` ownership marker. A foreign directory or
symbolic link is rejected. The filesystem must support atomic moves; the tool
does not silently fall back to partial copying.

## Handoff package schema 1

An accepted output is the complete, reviewable unit handed to Spoiled Milk:

```text
<handoff>/
  .sprite-baker-handoff-v1
  batch-report.json
  sprite-mapping.json
  entries/
    <outputName>/
      npc-<id>-rsc-sheet.png
      npc-<id>-sheet-diagnostic.json
```

`batch-report.json` records tool version, mode, pinned cache identity, batch
and project hashes, NPC/model/material identifiers, all 18 source animation
selections, complete visual settings, dimensions, transparency checks, output
hashes, warnings, and per-entry failures. It contains no timestamps, staging
paths, or random identifiers.

`sprite-mapping.json` is the explicit, path-independent import boundary. It
contains schema/contract versions and, for each accepted entry, the generic
NPC game ID, variant, PNG/provenance paths, and both hashes. Each per-entry
diagnostic is the existing Sprite Baker provenance manifest with cache,
appearance, animation, camera, lighting, palette, material, and renderer
details.

Reviewed project files are inputs and are not copied into the package. Cache
content, extracted models/textures/animations, and third-party assets are
never included. Generated packages remain outside this repository and still
require an explicit provenance/licensing review before distribution.

## Decisions deliberately left to Core integration

Sprite Baker does not assume or create Core-Framework paths. A separate Core
task must decide:

- where reviewed packages live and who approves/imports them;
- how generic `(assetKind, gameId, variant)` mappings become runtime override
  keys;
- whether the runtime consumes sheets directly or splits their labeled cells;
- precedence, missing-override fallback, versioning, rollback, and release
  packaging behavior; and
- the licensing/provenance gate for each generated derivative asset.
