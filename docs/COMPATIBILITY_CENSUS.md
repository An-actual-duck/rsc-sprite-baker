# Broad-cache Compatibility Census

`CompatibilityCensusMain` is a deterministic, terminal-only audit of every NPC
definition ID in a cache. It loads definitions lazily, checks component model
decoding and assembly, resolves every referenced material without substitution,
and validates the first frame of the automatically discovered standing and
walking sequences. Each definition is assigned exactly one category:

- `ready`
- `missing-automatic-animations`
- `unsupported-material`
- `unsupported-model`
- `morph-internal-definition`
- `other-failure`

The JSON contains the cache hashes, stable aggregate maps, and a record for all
definitions with IDs, model/material IDs, automatic sequences, and the precise
reason for the result. The output path must be outside both the cache and the
repository.

```bash
mvn clean package
java -cp target/rsc-sprite-baker.jar \
  com.spoiledmilk.spritebaker.CompatibilityCensusMain \
  --cache /path/to/user-supplied/cache \
  --output /tmp/rsc-sprite-baker-census.json
```

The desktop NPC browser uses the same scanner only after an NPC is selected.
Opening the browser and searching names continue to decode definitions only;
model, material, and animation compatibility work is lazy and its result is
then shown on the selected list entry and in the detail text.

## 2026-08-15 operation-36 audit

The read-only audit covered 8,590 definitions. Cache identity:

- dat2 SHA-256: `b5431211b019b9403b4cfca933f4c9635c1d5278d3730995dced0d8672b1cc91`
- reference-index SHA-256: `83a2292c515596af0423764c48e41dfe1aac482920dca0b89ecb343db6dd4c30`

The baseline report SHA-256 was
`d73a3435cc3fba2db29a417ec3ef9f38cc1afec52871370145a658ae8c47e337`.
After operation 36, two independent runs were byte-identical at SHA-256
`2997f13601519f0cc8522b15a7ff288b2ac71c5eaccbabdd6d737a9a0fbaac29`.
Reports were written under `/tmp` and are not repository artifacts.

| Category | Before | After |
| --- | ---: | ---: |
| Ready | 256 | 256 |
| Missing automatic animations | 139 | 139 |
| Unsupported material | 3,636 | 3,636 |
| Unsupported model | 3,946 | 3,946 |
| Morph/internal definition | 612 | 612 |
| Other failure | 1 | 1 |

Operation 36 was present in the baseline reasons for 3,115 NPCs and accounted
for 15,744 NPC/material diagnostic occurrences. After nested dependencies were
implemented, all 3,115 reasons advanced past operation 36 and no operation-36
blocker remains. No NPC changed top-level category because those nested graphs
then exposed additional unsupported operations. This is intentionally
fail-closed: resolving a dependency does not turn a later unsupported node into
a color or texture fallback.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 4 | 1 | 284 |
| 5 | 85 | 109 |
| 6 | 505 | 1,142 |
| 9 | 320 | 520 |
| 12 | 3 | 30 |
| 13 | 331 | 3,716 |
| 15 | 174 | 232 |
| 19 | 145 | 169 |
| 20 | 112 | 116 |
| 22 | 2 | 41 |
| 27 | 191 | 748 |
| 32 | 424 | 561 |
| 34 | 2,087 | 9,810 |
| 36 | 15,744 | 0 |
| 38 | 380 | 2,558 |
| 39 | 28 | 111 |

The increases are newly visible blockers reached through operation-36
dependencies, not regressions or substitutions. Counts are occurrences in NPC
material diagnostics rather than unique graphs, so a shared material is counted
for every affected NPC.

The 3,946 unsupported-model results form two stable decoder clusters:

- `BufferUnderflowException`: 1,954 definitions
- invalid decoded offset (`newPosition > limit`): 1,992 definitions

The single other-definition failure is NPC 1688, whose definition uses
unsupported revision-530 opcode 138 at byte 115.

## Guard cases

NPC 72 (Troll, model 3752) remains `ready`; standing sequence 286 and walking
sequence 283 are discovered and their first frames pose successfully. NPC 40
(Shark, model 2848, materials 157/171) remains model/material render-compatible
and is classified only as `missing-automatic-animations`: walking sequence 10
is present but standing metadata is absent. A packaged-JAR, validation-only
headless render of the preserved reviewed sequence-10 project passed all 18
cells and reproduced PNG SHA-256
`4568d2194f59c6d0d3118dd594531a517c83052c40fcec28896d5b348182ab44`
and provenance SHA-256
`c49d42c26770f3524cfce9f9c6b572567ab3db71252aede3e92bf7e442f36a5d`.

## Recommended next batch

Operation 34 is the next material target: it is the largest remaining blocker
at 9,810 diagnostic occurrences. Operation 13 follows at 3,716, then operation
38 at 2,558. Keep each operation bounded by primary-client semantics and neutral
fixtures, rerun this census after each addition, and retain exact failure for
every graph that still needs an unsupported operation. Model work should be a
separate batch, beginning by identifying the format boundary behind the two
decoder clusters rather than introducing alternate-model fallback.
