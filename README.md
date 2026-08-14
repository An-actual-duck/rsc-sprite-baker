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

Phase 2 adds revision-530 sequence/frame/framemap decoding, deterministic
time-based posing, a source timeline beside a labeled 6×3 selection sheet,
portable selection projects, and normalized sheet export. See
[`docs/ANIMATION_COMPATIBILITY.md`](docs/ANIMATION_COMPATIBILITY.md) for the
client trace and live-cache evidence. The Phase 1 CLI remains unchanged.

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

Load a source sequence, scrub or select any rendered keyframe, then set a
shared standing/left-step/right-step row or replace the currently outlined
cell. The five movement directions share row poses until individually
overridden. Combat-side cells are assigned independently. Locks prevent row,
suggestion, and replacement actions; suggestions fill empty cells only.

Export creates a transparent 1536×768 PNG and diagnostic JSON with one trace
entry per cell. All cells share scale, ground anchor, crop policy, camera, and
canvas. The mirrored-direction control is preview-only.

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
