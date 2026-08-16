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

The operation-36 audit originally recommended operation 34. Its completed
result is recorded below.

## 2026-08-15 operation-34 audit

The pre-change census was byte-identical to the operation-36 result at
SHA-256 `2997f13601519f0cc8522b15a7ff288b2ac71c5eaccbabdd6d737a9a0fbaac29`.
Two independent post-change runs were byte-identical at SHA-256
`6508c7ade64b9835e6809d3101c8006612c217f4cf3feff36d74fe999672909c`.
The cache identity remained unchanged.

| Category | Before | After | Change |
| --- | ---: | ---: | ---: |
| Ready | 256 | 426 | +170 |
| Missing automatic animations | 139 | 153 | +14 |
| Unsupported material | 3,636 | 3,449 | -187 |
| Unsupported model | 3,946 | 3,946 | 0 |
| Morph/internal definition | 612 | 612 | 0 |
| Other failure | 1 | 4 | +3 |

Operation 34 appeared in 2,992 NPC reasons and accounted for 9,810
NPC/material diagnostic occurrences. Both values are now zero. Of those 2,992
NPCs, 187 clear material validation entirely: 170 become ready, 14 need only
automatic animation metadata, and three portal definitions (6147, 6155, 7556)
reach a precise sequence-6880 pose failure. The other 2,805 remain unsupported
materials because decoding operation 34 exposes a later unsupported graph
node. No color, average material, or texture fallback is introduced.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 4 | 284 | 284 |
| 5 | 109 | 135 |
| 6 | 1,142 | 1,147 |
| 9 | 520 | 520 |
| 12 | 30 | 30 |
| 13 | 3,716 | 7,688 |
| 15 | 232 | 1,139 |
| 19 | 169 | 228 |
| 20 | 116 | 116 |
| 22 | 41 | 41 |
| 27 | 748 | 748 |
| 32 | 561 | 720 |
| 34 | 9,810 | 0 |
| 38 | 2,558 | 2,749 |
| 39 | 111 | 111 |

The increases are newly reachable diagnostics within nested graphs. Operation
36 remains at zero. The two model-decoder clusters are unchanged.

NPC 61 (Spider, model 24613, material 111) is a representative transition to
`ready`: standing sequence 6247 and walking sequence 6248 both validate. A
packaged-JAR, validation-only 18-cell export passed with visible and transparent
pixels; its PNG SHA-256 is
`e18c39eaf24c19ba27fc8cd8f9d730bca74d01e1f4527edebc42c32127f0d26e`
and provenance SHA-256 is
`84ad898887dbbf55a01a43839fb47425c6040104016623d833ba21c8c351ce7d`.
NPC 72 remains fully automatic and ready. NPC 40 remains model/material
render-compatible and classified only by its absent standing metadata.

## Recommended next batch

The operation-34 audit originally recommended operation 13. Its completed
result is recorded below.

## 2026-08-15 operation-13 audit

The pre-change census was byte-identical to the operation-34 result at
SHA-256 `6508c7ade64b9835e6809d3101c8006612c217f4cf3feff36d74fe999672909c`.
Two independent post-change runs and the packaged-JAR census were byte-identical
at SHA-256
`aee3fd87547103c91294f16d3654fa6fea9549d0d095166c61ede737bc969502`.
The cache identity remained unchanged.

| Category | Before | After | Change |
| --- | ---: | ---: | ---: |
| Ready | 426 | 437 | +11 |
| Missing automatic animations | 153 | 153 | 0 |
| Unsupported material | 3,449 | 3,438 | -11 |
| Unsupported model | 3,946 | 3,946 | 0 |
| Morph/internal definition | 612 | 612 | 0 |
| Other failure | 4 | 4 | 0 |

Operation 13 appeared in 2,519 NPC reasons and accounted for 7,688
NPC/material diagnostic occurrences. Both values are now zero. Eleven NPCs
clear material validation and become ready: Ice warrior definitions 125, 145,
and 3073; Swarming turoth definitions 1611 and 1628; Turoth definitions 1622
and 1627; and Charmed Warrior definitions 3104 through 3107. The other 2,508
affected NPCs remain unsupported materials because a later graph node is now
reachable. No texture, color, or average-material fallback is introduced.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 4 | 284 | 284 |
| 5 | 135 | 135 |
| 6 | 1,147 | 1,150 |
| 9 | 520 | 520 |
| 12 | 30 | 30 |
| 13 | 7,688 | 0 |
| 15 | 1,139 | 1,139 |
| 19 | 228 | 228 |
| 20 | 116 | 137 |
| 22 | 41 | 41 |
| 27 | 748 | 1,753 |
| 32 | 720 | 2,242 |
| 38 | 2,749 | 2,749 |
| 39 | 111 | 111 |

The increases in operations 6, 20, 27, and 32 are newly reachable diagnostics
within nested graphs. Operations 34 and 36 remain at zero, and model/animation
failure counts do not change.

NPC 125 (Ice warrior) is the packaged-render representative. Its seven
component models, materials 249/291/303/302, 1,076 textured faces, standing
sequence 842, walking sequence 841, all 18 cells, visible pixels, and
transparency validated. The PNG SHA-256 is
`340fe9d03a9816c1b47a22b9c44de8fa1a89795f88f46face4c1ce1e6e641ba7`
and provenance SHA-256 is
`84402a7ca4699ff12d12b9bc8f79388aab66e9233c2561cf31dcc68172a169b7`.
NPC 61 and NPC 72 remain ready; NPC 40 remains model/material
render-compatible and lacks only automatic standing metadata.

## Recommended next batch

The operation-13 audit originally recommended operation 38. Its completed
result is recorded below.

## 2026-08-15 operation-38 audit

The pre-change census was byte-identical to the operation-13 result at
SHA-256 `aee3fd87547103c91294f16d3654fa6fea9549d0d095166c61ede737bc969502`.
Two independent post-change runs and the packaged-JAR census were byte-identical
at SHA-256
`a4c14dafc32ee1f98432f144affb967f811e811b8725076a2eedf90bd25df167`.
The cache identity remained unchanged.

| Category | Before | After | Change |
| --- | ---: | ---: | ---: |
| Ready | 437 | 463 | +26 |
| Missing automatic animations | 153 | 159 | +6 |
| Unsupported material | 3,438 | 3,405 | -33 |
| Unsupported model | 3,946 | 3,946 | 0 |
| Morph/internal definition | 612 | 612 | 0 |
| Other failure | 4 | 5 | +1 |

Operation 38 accounted for 2,749 NPC/material diagnostic occurrences. It now
accounts for zero. In total, 2,257 NPC reasons changed: 33 clear material
validation entirely while 2,224 remain unsupported materials because a later
graph node is now reachable. Of the 33 category changes, 26 become ready, six
need only automatic animation metadata, and NPC 8108 (Crate) reaches a precise
automatic-sequence failure: sequence 10362 contains unsupported opcode 36.
No texture, color, or average-material fallback is introduced.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 4 | 284 | 284 |
| 5 | 135 | 2,178 |
| 6 | 1,150 | 1,150 |
| 9 | 520 | 520 |
| 12 | 30 | 30 |
| 15 | 1,139 | 1,329 |
| 19 | 228 | 228 |
| 20 | 137 | 137 |
| 22 | 41 | 41 |
| 27 | 1,753 | 1,756 |
| 32 | 2,242 | 2,242 |
| 38 | 2,749 | 0 |
| 39 | 111 | 111 |

The increases in operations 5, 15, and 27 are newly reachable diagnostics
within nested graphs. Operations 13, 34, and 36 remain at zero. The 3,946
unsupported-model results remain split between `BufferUnderflowException`
(1,954 definitions) and invalid decoded offsets (`newPosition > limit`, 1,992
definitions).

NPC 131 (Penguin) is the packaged-render representative. Model 21547,
materials 182/347/171, 391 textured faces, standing sequence 5668, walking
sequence 5666, all 18 cells, visible pixels, and transparency validated. The
PNG SHA-256 is
`60e9aeac8cee9c931e7d556aec83e8e9bffca11872a03525cd0a2a1f9bac6f55`
and provenance SHA-256 is
`f4317ff64c54458196e69c59ce67b0d6f718676105310422488d8cb90cbe3c14`.
NPC 125, NPC 61, and NPC 72 remain ready; NPC 40 remains model/material
render-compatible and lacks only automatic standing metadata.

## Recommended next batch

The operation-38 audit originally recommended operation 32. Its completed
result is recorded below.

## 2026-08-15 operation-32 audit

The pre-change census was byte-identical to the operation-38 result at
SHA-256 `a4c14dafc32ee1f98432f144affb967f811e811b8725076a2eedf90bd25df167`.
Two independent post-change packaged-JAR runs were byte-identical at SHA-256
`255433bfb6b6a701b0a5a6920f674f6a0cb66059abf55ee4971c51bf6d859ea5`.
The cache identity remained unchanged.

| Category | Before | After | Change |
| --- | ---: | ---: | ---: |
| Ready | 463 | 500 | +37 |
| Missing automatic animations | 159 | 169 | +10 |
| Unsupported material | 3,405 | 3,358 | -47 |
| Unsupported model | 3,946 | 3,946 | 0 |
| Morph/internal definition | 612 | 612 | 0 |
| Other failure | 5 | 5 | 0 |

Operation 32 appeared in 1,700 NPC reasons and accounted for 2,242
NPC/material diagnostic occurrences. Both values are now zero, and all 1,700
reasons advance. Forty-seven NPCs clear material validation: 37 become ready
and ten need only automatic animation metadata. The other 1,653 remain
unsupported materials because a later graph node is now reachable. No
material, texture, color, or average-material fallback is introduced.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 4 | 284 | 1,822 |
| 5 | 2,178 | 2,178 |
| 6 | 1,150 | 1,165 |
| 9 | 520 | 520 |
| 12 | 30 | 30 |
| 15 | 1,329 | 1,343 |
| 17 | 0 | 4 |
| 19 | 228 | 442 |
| 20 | 137 | 150 |
| 22 | 41 | 41 |
| 27 | 1,756 | 1,756 |
| 32 | 2,242 | 0 |
| 39 | 111 | 111 |

The increases in operations 4, 6, 15, 17, 19, and 20 are newly reachable
diagnostics within nested graphs. Operations 13, 34, 36, and 38 remain at
zero. The 3,946 unsupported-model results remain split between
`BufferUnderflowException` (1,954 definitions) and invalid decoded offsets
(`newPosition > limit`, 1,992 definitions).

NPC 1013 (Swamp toad) is the packaged-render representative. Model 3447,
material 318 with graph operations 34/10/32/7/10/8, 155 textured faces,
standing sequence 1018, walking sequence 1021, all 18 cells, visible pixels,
and transparency validated. The PNG SHA-256 is
`26fba51cb48c4bce3dbbd86962f8e96ca5b920cbf33454ae4204c434d4ff509d`
and provenance SHA-256 is
`faec6dd97dadec7325c6cc7afff36c700217c64c531a82838c6fa10e4685ec47`.
NPC 131, NPC 125, NPC 61, and NPC 72 remain ready; NPC 40 remains
model/material render-compatible and lacks only automatic standing metadata.

## Recommended next batch

The operation-32 audit originally recommended operation 5. Its completed
result is recorded below.

## 2026-08-15 operation-5 audit

The pre-change census was byte-identical to the operation-32 result at
SHA-256 `255433bfb6b6a701b0a5a6920f674f6a0cb66059abf55ee4971c51bf6d859ea5`.
Two independent post-change packaged-JAR runs were byte-identical at SHA-256
`fea51fedcd7945928da61d5cac4819d6d9ba3f0e4a29305063f047ea5d80831a`.
The cache identity remained unchanged.

| Category | Before | After | Change |
| --- | ---: | ---: | ---: |
| Ready | 500 | 539 | +39 |
| Missing automatic animations | 169 | 178 | +9 |
| Unsupported material | 3,358 | 3,310 | -48 |
| Unsupported model | 3,946 | 3,946 | 0 |
| Morph/internal definition | 612 | 612 | 0 |
| Other failure | 5 | 5 | 0 |

Operation 5 appeared in 2,046 NPC reasons and accounted for 2,178 NPC/material
diagnostic occurrences. Both values are now zero, and all 2,046 reasons
advance. Forty-eight NPCs clear material validation: 39 become ready and nine
need only automatic animation metadata. The other 1,998 remain unsupported
materials because a later graph node is now reachable. No material, texture,
color, or average-material fallback is introduced.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 4 | 1,822 | 1,822 |
| 5 | 2,178 | 0 |
| 6 | 1,165 | 1,165 |
| 9 | 520 | 520 |
| 12 | 30 | 30 |
| 15 | 1,343 | 1,366 |
| 17 | 4 | 4 |
| 19 | 442 | 468 |
| 20 | 150 | 150 |
| 22 | 41 | 41 |
| 27 | 1,756 | 1,756 |
| 39 | 111 | 111 |

The increases in operations 15 and 19 are newly reachable diagnostics within
nested graphs. Operations 5, 13, 32, 34, 36, and 38 are now at zero. The 3,946
unsupported-model results remain split between `BufferUnderflowException`
(1,954 definitions) and invalid decoded offsets (`newPosition > limit`, 1,992
definitions).

NPC 78 (Giant bat) is the packaged-render representative. Model 18898,
materials 185/59, graph operations 0/38/8/5 and 0/3/8, 524 textured faces,
standing sequence 4914, walking sequence 4913, all 18 cells, visible pixels,
and transparency validated. The PNG SHA-256 is
`7a25cad7486731b4996d1eb82bf787b7c4b0a96bc510eb39fd0cdbfe8743b485`
and provenance SHA-256 is
`5e3e80d32d1f64f7a49c3e35115962d2061ef78d033461fbc8e419c99947c96e`.
NPC 1013, NPC 131, NPC 125, NPC 61, and NPC 72 remain ready; NPC 40 remains
model/material render-compatible and lacks only automatic standing metadata.

## Recommended next batch

The operation-5 audit originally recommended operation 4. Its completed result
is recorded below.

## 2026-08-15 operation-4 audit

The pre-change census was byte-identical to the operation-5 result at SHA-256
`fea51fedcd7945928da61d5cac4819d6d9ba3f0e4a29305063f047ea5d80831a`.
Two independent post-change packaged-JAR runs were byte-identical at SHA-256
`07cd3ac4661fe5520f6d40297655c541279f213caa8cf3e0867275e313285c9b`.
The cache identity remained unchanged.

| Category | Before | After | Change |
| --- | ---: | ---: | ---: |
| Ready | 539 | 539 | 0 |
| Missing automatic animations | 178 | 178 | 0 |
| Unsupported material | 3,310 | 3,310 | 0 |
| Unsupported model | 3,946 | 3,946 | 0 |
| Morph/internal definition | 612 | 612 | 0 |
| Other failure | 5 | 5 | 0 |

Operation 4 appeared in 1,284 NPC reasons and accounted for 1,822 NPC/material
diagnostic occurrences. Both values are now zero, and all 1,284 reasons
advance, but every affected NPC retains another unsupported material. No
top-level category changes. Texture 261 now resolves in 280 NPC reasons;
texture 203 instead reaches its precise combine-function-1 blocker in 1,124
NPC reasons. No material, texture, color, or average-material fallback is
introduced.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 4 | 1,822 | 0 |
| 6 | 1,165 | 1,165 |
| 9 | 520 | 520 |
| 12 | 30 | 30 |
| 15 | 1,366 | 1,366 |
| 17 | 4 | 4 |
| 19 | 468 | 468 |
| 20 | 150 | 150 |
| 22 | 41 | 41 |
| 27 | 1,756 | 1,756 |
| 39 | 111 | 111 |

No unsupported operation frequency increases; the newly exposed blocker is a
previously diagnosed but unsupported combine mode rather than another
operation ID. Operations 4, 5, 13, 32, 34, 36, and 38 are now at zero. The
3,946 unsupported-model results remain split between `BufferUnderflowException`
(1,954 definitions) and invalid decoded offsets (`newPosition > limit`, 1,992
definitions).

No affected NPC is eligible for a complete packaged export because each still
has another unsupported material. Packaged-JAR verification therefore used
newly resolved material 261 (operations 0/4/30) on a neutral in-memory textured
triangle. Two renders were identical, contained 434 visible pixels, and had
ARGB SHA-256
`81706338fa2297a54f347e7a18fd34216b6d9f95065785d42adedbd07d0b8da0`.
Material 203 continued to fail closed with `combine function 1`. NPC 78, NPC
1013, NPC 131, NPC 125, NPC 61, and NPC 72 remain ready; NPC 40 remains
model/material render-compatible and lacks only automatic standing metadata.

## Recommended next batch

Operation 27 is now the largest material blocker at 1,756 diagnostic
occurrences, followed by operation 15 at 1,366, operation 6 at 1,165,
operation 9 at 520, and operation 19 at 468. Combine function 1 is also a
high-yield non-operation blocker exposed by this batch. Keep each addition tied
to pinned client semantics and neutral fixtures, rerun this census after every
addition, and preserve exact fail-closed diagnostics. The unchanged
model-decoder clusters and known animation/definition failures remain separate
focused batches.

## 2026-08-15 operation-27 audit

The pre-change census was byte-identical to the operation-4 result at SHA-256
`07cd3ac4661fe5520f6d40297655c541279f213caa8cf3e0867275e313285c9b`.
Two independent post-change packaged-JAR runs were byte-identical at SHA-256
`1c9d88a59b7ccfba41f5dbeac9fd62f579cb83c23a629e2e23764216efd14cdd`.
The cache identity remained unchanged.

| Category | Before | After | Change |
| --- | ---: | ---: | ---: |
| Ready | 539 | 545 | +6 |
| Missing automatic animations | 178 | 178 | 0 |
| Unsupported material | 3,310 | 3,304 | -6 |
| Unsupported model | 3,946 | 3,946 | 0 |
| Morph/internal definition | 612 | 612 | 0 |
| Other failure | 5 | 5 | 0 |

Operation 27 appeared in 1,325 NPC reasons and accounted for 1,756
NPC/material diagnostic occurrences. Both values are now zero. Jiminua (560),
Irena (835 and 4988), Mouse (901), Crypt rat (2032), and A Meiyerditch child
(4751) advance from unsupported material to ready. The other 1,319 affected
definitions retain another precise material blocker; no material, texture,
color, or average-material fallback is introduced.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 6 | 1,165 | 1,165 |
| 9 | 520 | 520 |
| 12 | 30 | 30 |
| 15 | 1,366 | 1,366 |
| 17 | 4 | 4 |
| 19 | 468 | 468 |
| 20 | 150 | 199 |
| 22 | 41 | 41 |
| 27 | 1,756 | 0 |
| 39 | 111 | 111 |

The only newly exposed opcode blocker is operation 20 in texture 221, adding
49 occurrences. Existing combine-function blockers are unchanged: function 1
has 1,926 occurrences, function 6 has 3,553, function 5 has 179, function 7
has 37, and function 2 has one. Operations 4, 5, 13, 27, 32, 34, 36, and 38
are now at zero. The 3,946 unsupported-model results remain split between
`BufferUnderflowException` (1,954 definitions) and invalid decoded offsets
(`newPosition > limit`, 1,992 definitions).

NPC 560 (Jiminua) is the packaged-render representative. Its seven component
models, materials 228/249/59/268/291/251/252, 467 textured faces, standing
sequence 808, walking sequence 819, all 18 cells, visible pixels, and
transparency validated. Two terminal-only packaged-JAR exports were
byte-identical. The PNG SHA-256 is
`3f60507ff6e52631de4819e1fedf2ac9936081698212e2805938d007c0734d44`
and provenance SHA-256 is
`123054c082d2e1b4300e0c40c3039f4d1de3176d36128c90e8cc06591646ac4f`.
NPC 72 remains ready; NPC 40 remains model/material render-compatible and
lacks only automatic standing metadata.

## Recommended next batch

Operation 15 is now the largest remaining opcode blocker at 1,366 diagnostic
occurrences, followed by operation 6 at 1,165, operation 9 at 520, operation
19 at 468, and operation 20 at 199. Combine functions 6 and 1 are larger
non-operation blockers and merit separate pinned-semantics work. Preserve the
same deterministic census and fail-closed policy; keep the unchanged model
decoder clusters in their own focused batch.
