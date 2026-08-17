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

## 2026-08-15 operation-15 audit

The pre-change census was byte-identical to the operation-27 result at SHA-256
`1c9d88a59b7ccfba41f5dbeac9fd62f579cb83c23a629e2e23764216efd14cdd`.
Two independent post-change packaged-JAR runs were byte-identical at SHA-256
`0724d289d6ecc841f932679cb236c02572b0084bbbc70fead2f259d8f855dcf6`.
The cache identity remained unchanged.

| Category | Before | After | Change |
| --- | ---: | ---: | ---: |
| Ready | 545 | 561 | +16 |
| Missing automatic animations | 178 | 178 | 0 |
| Unsupported material | 3,304 | 3,288 | -16 |
| Unsupported model | 3,946 | 3,946 | 0 |
| Morph/internal definition | 612 | 612 | 0 |
| Other failure | 5 | 5 | 0 |

Operation 15 appeared in 1,143 NPC reasons and accounted for 1,366
NPC/material diagnostic occurrences. Both values are now zero. The 16 newly
ready definitions are NPCs 126, 188, 190, 566, 891, 959, 1334, 3131, 3244,
4520, 4601, 6009, 6013, 6221, 6339, and 6345. The other 1,127 affected
definitions retain another precise material blocker; no material, texture,
color, or average-material fallback is introduced.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 6 | 1,165 | 1,188 |
| 9 | 520 | 520 |
| 12 | 30 | 30 |
| 15 | 1,366 | 0 |
| 17 | 4 | 4 |
| 19 | 468 | 525 |
| 20 | 199 | 199 |
| 21 | 0 | 79 |
| 22 | 41 | 41 |
| 39 | 111 | 111 |

Newly reachable graphs expose operation 21 in textures 168/199/134/310 (79
occurrences), operation 19 in textures 61/60/88/220 (57 additional
occurrences), operation 6 in texture 172 (23 additional occurrences), and
combine function 6 in texture 74 (three additional occurrences). All other
operation and combine-function frequencies are unchanged. Operations 4, 5,
13, 15, 27, 32, 34, 36, and 38 are now at zero. The 3,946 unsupported-model
results remain split between `BufferUnderflowException` (1,954 definitions)
and invalid decoded offsets (`newPosition > limit`, 1,992 definitions).

NPC 126 (Otherworldly being) is the packaged-render representative. Models
202/292/170/260, materials 268/252/256, 550 textured faces, standing sequence
808, walking sequence 819, all 18 cells, visible pixels, and transparency
validated with the documented default RSC-restrained renderer settings. Two
terminal-only packaged-JAR exports were byte-identical. The PNG SHA-256 is
`615fd903ef06e1fcd044695af0db234100940d5b519f1e78b2898ada02b73522`
and provenance SHA-256 is
`2bbc9fd0c5476fdf3602f4f20f9c7bd970137beb4f502b1a33aba66ea0b25496`.
NPC 72 remains ready; NPC 40 remains model/material render-compatible and
lacks only automatic standing metadata.

## Recommended next batch

Operation 6 is now the largest remaining opcode blocker at 1,188 diagnostic
occurrences, followed by operation 19 at 525, operation 9 at 520, operation 20
at 199, and operation 39 at 111. Operation 21 is newly visible at 79.
Combine functions 6 and 1 remain larger non-operation blockers and merit
separate pinned-semantics work. Preserve the deterministic census and exact
fail-closed policy; keep the unchanged model-decoder clusters separate.

## 2026-08-16 combine-function-6 audit

The pre-change census was byte-identical to the operation-15 result at SHA-256
`0724d289d6ecc841f932679cb236c02572b0084bbbc70fead2f259d8f855dcf6`.
Two independent post-change packaged-JAR runs were byte-identical at SHA-256
`5dadc9321431bad2a648880c09d6c8e1339a644119441115829ff945dc1591ff`.
The cache identity remained unchanged.

| Category | Before | After | Change |
| --- | ---: | ---: | ---: |
| Ready | 561 | 1,132 | +571 |
| Missing automatic animations | 178 | 218 | +40 |
| Unsupported material | 3,288 | 2,675 | -613 |
| Unsupported model | 3,946 | 3,946 | 0 |
| Morph/internal definition | 612 | 612 | 0 |
| Other failure | 5 | 7 | +2 |

Combine function 6 appeared in 2,253 NPC reasons and accounted for 3,556
NPC/material diagnostic occurrences. Both values are now zero. Of those
definitions, 571 advance to ready, 40 advance to the precise missing-automatic-
animations category, and Tegid (1213) plus Khazard launderer (8428) advance to
precise sequence-decoder failures. The other 1,640 affected definitions retain
another material blocker. No material, texture, color, or average-material
fallback is introduced.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 6 | 1,188 | 1,216 |
| 9 | 520 | 520 |
| 12 | 30 | 30 |
| 17 | 4 | 4 |
| 19 | 525 | 527 |
| 20 | 199 | 199 |
| 21 | 79 | 145 |
| 22 | 41 | 41 |
| 39 | 111 | 111 |

Newly reachable graphs expose operation 21 in textures 63/206/162 (66
additional occurrences), operation 6 in textures 361/281 (28 additional
occurrences), and operation 19 in texture 335 (two additional occurrences).
All other operation frequencies are unchanged. Remaining unsupported combine
functions are function 1 at 1,926 occurrences, function 5 at 179, function 7
at 37, and function 2 at one. Functions 3 and 6 are supported; every other
function continues to fail closed. The 3,946 unsupported-model results remain
split between `BufferUnderflowException` (1,954 definitions) and invalid
decoded offsets (`newPosition > limit`, 1,992 definitions).

NPC 11 (Tramp) is the complete color-output packaged-render representative.
Its eight component models, materials 314/228/313/258/277/254, 907 textured
faces, standing sequence 808, walking sequence 819, all 18 cells, visible
pixels, and transparency validated with the default RSC-restrained settings.
Two terminal-only packaged-JAR exports were byte-identical. The PNG SHA-256 is
`75b22466987ea90dba6049a8e7cbc68356c6ad2ba3d219632e6c5f400ba63408`
and provenance SHA-256 is
`64871ea04125788d41866353dfc094e5314c0a5de063bb36657c3bc59127aba0`.

NPC 3124 (Pyramid block) provides real-cache monochrome-output coverage for
material 133. Its model 10817 and materials 133/270 rendered twice through the
packaged JAR with the same default renderer settings. Both 128x128 PNGs had
4,239 visible and 12,145 transparent pixels and SHA-256
`8b0ea18f7a73abbf2cc271dcf58f8e190e514897ffb0ed7c482683edda604b2c`.
It correctly remains missing automatic animations, so this is a static render
rather than an invented animation assignment. NPC 72 remains ready; NPC 40
remains model/material render-compatible and lacks only automatic standing
metadata.

## Recommended next batch

Combine function 1 is now the largest remaining material blocker at 1,926
diagnostic occurrences. Operation 6 follows at 1,216, then operation 19 at
527, operation 9 at 520, and operation 20 at 199. Keep each combine function
as a separate pinned-semantics batch, preserve explicit rejection for all
others, and retain the deterministic census and fail-closed policy.

## 2026-08-16 combine-function-1 audit

The fresh pre-change census reproduced the combine-function-6 result at
SHA-256
`5dadc9321431bad2a648880c09d6c8e1339a644119441115829ff945dc1591ff`.
Two independent post-change packaged-JAR runs were byte-identical at SHA-256
`d4a24f58cb3279bc893f1ea52247eb8eed3d44986870c9cb6dd954f621de4854`.
The cache identity remained unchanged.

| Category | Before | After | Change |
| --- | ---: | ---: | ---: |
| Ready | 1,132 | 1,786 | +654 |
| Missing automatic animations | 218 | 246 | +28 |
| Unsupported material | 2,675 | 1,993 | -682 |
| Unsupported model | 3,946 | 3,946 | 0 |
| Morph/internal definition | 612 | 612 | 0 |
| Other failure | 7 | 7 | 0 |

Combine function 1 appeared in 1,369 NPC reasons and accounted for 1,926
NPC/material diagnostic occurrences, concentrated in textures 203 (1,517),
132 (385), 233 (21), and 363 (3). Both totals are now zero. Of those
definitions, 654 advance to ready and 28 advance to the precise missing-
automatic-animations category. The other 687 definitions retain another
precise material blocker. No material, texture, color, average-material, or
other rendering fallback is introduced.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 6 | 1,216 | 1,604 |
| 9 | 520 | 520 |
| 12 | 30 | 30 |
| 17 | 4 | 4 |
| 19 | 527 | 527 |
| 20 | 199 | 220 |
| 21 | 145 | 145 |
| 22 | 41 | 41 |
| 39 | 111 | 111 |

Newly reachable graphs expose operation 6 in textures 132 (385 occurrences)
and 363 (3), plus operation 20 in texture 233 (21). All other operation
frequencies are unchanged. Remaining unsupported combine functions are
function 5 at 179 occurrences, function 7 at 37, and function 2 at one.
Functions 1, 3, and 6 are supported; every other function continues to fail
closed. The 3,946 unsupported-model results remain split between
`BufferUnderflowException` (1,954 definitions) and invalid decoded offsets
(`newPosition > limit`, 1,992 definitions).

NPC 0 (Hans) is the packaged-render representative for the high-volume
texture-203 path. Its six component models, materials
228/292/258/257/262/527/272/254, 676 textured faces, standing sequence 9870,
walking sequence 9869, all 18 cells, visible pixels, and transparency validated
with the default RSC-restrained settings. Two terminal-only exports from the
shaded packaged JAR were byte-identical. The PNG SHA-256 is
`261ccf8a8a762adf5ba6b64dd6f2b3eee3cf6d3f82b137645f5604c4778d06c6`
and provenance SHA-256 is
`5c0b71d5a0131362edb5a5c3f0ca18d45413d64fcf7ed4da77d4b3d2da6dd589`.
NPC 72 remains ready; NPC 40 remains model/material render-compatible and
lacks only automatic standing metadata.

## Recommended next batch

Operation 6 is now the largest remaining material blocker at 1,604 diagnostic
occurrences, followed by operation 19 at 527, operation 9 at 520, operation 20
at 220, and operation 21 at 145. Among combine functions, function 5 is the
largest remaining blocker at 179 occurrences. Preserve the deterministic
census, explicit rejection, and exact fail-closed rendering policy; keep the
unchanged model-decoder clusters in their own focused batch.

## 2026-08-16 operation-6 audit

The fresh pre-change census reproduced the combine-function-1 result at
SHA-256
`d4a24f58cb3279bc893f1ea52247eb8eed3d44986870c9cb6dd954f621de4854`.
Two independent post-change packaged-JAR runs were byte-identical at SHA-256
`9bc28d667792f32e78d613fe938268a8bff5050a63b5daae42ea14ef93af9aef`.
The cache identity remained unchanged.

| Category | Before | After | Change |
| --- | ---: | ---: | ---: |
| Ready | 1,786 | 1,922 | +136 |
| Missing automatic animations | 246 | 252 | +6 |
| Unsupported material | 1,993 | 1,849 | -144 |
| Unsupported model | 3,946 | 3,946 | 0 |
| Morph/internal definition | 612 | 612 | 0 |
| Other failure | 7 | 9 | +2 |

Operation 6 appeared in 1,352 NPC reasons and accounted for 1,604
NPC/material diagnostic occurrences. Both totals are now zero. Of those
definitions, 136 advance to ready and six advance to the precise missing-
automatic-animations category. NPCs 4765 and 5562 now expose an
`ArrayIndexOutOfBoundsException` in automatic sequence 4753. The other 1,208
affected definitions retain another precise material blocker. No material,
texture, color, average-material, or other rendering fallback is introduced.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 6 | 1,604 | 0 |
| 9 | 520 | 520 |
| 12 | 30 | 30 |
| 17 | 4 | 4 |
| 19 | 527 | 1,421 |
| 20 | 220 | 230 |
| 21 | 145 | 259 |
| 22 | 41 | 56 |
| 39 | 111 | 126 |

Newly reachable graphs expose operation 19 in textures 132 (+680), 335
(+197), 196 (+15), and 102 (+2); operation 21 in textures 238 (+108), 181
(+3), and 246 (+3); operation 39 in texture 352 (+15); operation 22 in texture
183 (+15); and operation 20 in texture 675 (+10). They also expose combine
function 5 in texture 308 (+41) and function 8 in texture 361 (+16).
Remaining unsupported combine totals are therefore function 5 at 220,
function 7 at 37, function 8 at 16, and function 2 at one. All other blocker
frequencies are unchanged. The 3,946 unsupported-model results remain split
between `BufferUnderflowException` (1,954 definitions) and invalid decoded
offsets (`newPosition > limit`, 1,992 definitions).

NPC 79 (Death wing) is the complete packaged-render representative. Model
18897, materials 182/281, the directly serialized color-output clamp in
material 281, 645 textured faces, standing sequence 4914, walking sequence
4913, all 18 cells, visible pixels, and transparency validated with the
default RSC-restrained settings. Two terminal-only exports from the shaded
packaged JAR were byte-identical. The PNG SHA-256 is
`77b981f62a7694755150cced94833cc320505f13492c8e2059ff525d2239ebfd`
and provenance SHA-256 is
`6d016eabf5bf947e3e820c6d9bcb73706f5ac18e2d77b60259e04e326d85aca8`.
NPC 72 remains ready; NPC 40 remains model/material render-compatible and
lacks only automatic standing metadata.

## Recommended next batch

Operation 19 is now the largest remaining material blocker at 1,421
diagnostic occurrences, followed by operation 9 at 520, operation 21 at 259,
operation 20 at 230, and combine function 5 at 220. Preserve the deterministic
census, explicit rejection, and exact fail-closed rendering policy; keep the
unchanged model-decoder clusters in their own focused batch.

## 2026-08-16 operations-19/9/21/20 audit

The fresh pre-change census reproduced the operation-6 result at SHA-256
`9bc28d667792f32e78d613fe938268a8bff5050a63b5daae42ea14ef93af9aef`.
Each operation was committed and audited sequentially. The intermediate report
hashes are operation 19
`aa8a947bccb7307c40b0ff318f727a834c58f8b0f21fe17b0fff12b12395f0fc`,
operation 9
`fafb7c69f6a83db5b2e164e9c4e9c92a6fe11302760021b5aa899a37143b6ad0`,
and operation 21
`944a44d4c33ff5ebcf5c4ef9dc2d59e0e1b48073b26e7f13ca11956bddcee33e`.
Two independent complete-batch packaged-JAR censuses were byte-identical at
SHA-256
`45119bbbc8911d4fa07c28ee9e608d3f656f1bbf829618c3095162fa2f629e0a`.
The cache identity remained unchanged.

| Stage | Ready | Missing automatic animations | Unsupported material | Other failure |
| --- | ---: | ---: | ---: | ---: |
| Before batch | 1,922 | 252 | 1,849 | 9 |
| After operation 19 | 2,030 | 471 | 1,522 | 9 |
| After operation 9 | 2,249 | 479 | 1,295 | 9 |
| After operation 21 | 2,284 | 566 | 1,173 | 9 |
| After operation 20 | 2,371 | 575 | 1,075 | 11 |
| Combined change | +449 | +323 | -774 | +2 |

The per-operation attribution is:

- Operation 19 appeared in 1,052 NPC reasons and 1,421 diagnostic
  occurrences. Both reach zero; 108 definitions become ready, 219 become
  missing-automatic-animations, and 725 retain another material blocker.
- Operation 9 appeared in 452 NPC reasons and 520 diagnostic occurrences at
  its checkpoint. Both reach zero; 219 definitions become ready, eight become
  missing-automatic-animations, and 225 retain another material blocker.
- Operation 21 appeared in 226 NPC reasons and 259 diagnostic occurrences at
  its checkpoint. Both reach zero; 35 definitions become ready, 87 become
  missing-automatic-animations, and 104 retain another material blocker.
- Operation 20 had grown from 230 baseline occurrences to 282 at its
  checkpoint as earlier operations exposed more graphs. It appeared in 243
  NPC reasons there. Both reach zero; 87 definitions become ready, nine become
  missing-automatic-animations, two expose precise sequence failures, and 145
  retain another material blocker.

NPCs 8176 and 8177 (Fish) are the two newly visible non-material failures:
automatic sequences 10426 and 10427 explicitly reject unsupported sequence
opcodes 47 and 49. No material, texture, color, average-material, or other
rendering fallback is introduced.

Remaining unsupported-operation diagnostic occurrences are:

| Operation | Before | After |
| ---: | ---: | ---: |
| 9 | 520 | 0 |
| 12 | 30 | 30 |
| 17 | 4 | 25 |
| 19 | 1,421 | 0 |
| 20 | 230 | 0 |
| 21 | 259 | 0 |
| 22 | 56 | 153 |
| 39 | 126 | 126 |
| 255 | 0 | 68 |

Operation 19 exposes combine function 2 in texture 132 (+680), combine
function 10 in texture 196 (+15), operation 20 in texture 80 (+4), and curve
interpolation 2 in texture 186 (+2). Operation 9 exposes additional operation
20 diagnostics in textures 221 (+38) and 267 (+10). Operation 21 exposes
operation 255 in texture 168 (+68), operation 22 in textures 162 (+4) and 246
(+3), and a fail-closed unexpected Fill parameter in texture 134 (+3).
Operation 20 exposes operation 22 in textures 219 (+70) and 502 (+20),
operation 17 in texture 233 (+21), combine function 10 in texture 675 (+10),
and combine function 5 in texture 96 (+1).

Final unsupported combine totals are function 2 at 681 occurrences, function
5 at 221, function 7 at 37, function 10 at 25, and function 8 at 16. Curve
interpolation remains at 18 occurrences for mode 1 and seven for mode 2. The
3,946 unsupported-model results remain split between
`BufferUnderflowException` (1,954 definitions) and invalid decoded offsets
(`newPosition > limit`, 1,992 definitions). Morph/internal definitions remain
612 and are unchanged.

NPC 74 (Zombie) is the packaged-render representative for operations 19, 9,
and 21. Its seven component models, materials
393/314/84/59/392/118/288/238, 954 textured faces, standing sequence 5576,
walking sequence 5577, all 18 cells, visible pixels, and transparency
validated. Materials 393/84 directly contain operation 19, material 118
contains operation 9, and material 238 contains operation 21. Two terminal-
only exports from the shaded JAR were byte-identical: PNG SHA-256
`22b554fd1f66ffd0c711b8105027b6b59e367de27f732664910f80362575530a`
and provenance SHA-256
`3d90b6644faeb436a2a41038211d42d27e0ecf0df82c21cc14215c75564dd9b1`.

NPC 165 (Gnome shop keeper) represents operation 20 through material 221. Its
three component models, materials 57/404/125/221/121, 213 textured faces,
standing sequence 195, walking sequence 189, all 18 cells, visible pixels, and
transparency validated in two byte-identical packaged-JAR exports. The PNG
SHA-256 is
`8948b63a22bdd364a73d5f5b85731c0fcf496728e9bb2611f8cdb99fe53416f1`
and provenance SHA-256 is
`acf5058594c6df238d1a927a43bc8819e71568e964cb51f6b57b7b4e4662496d`.
NPC 72 remains ready; NPC 40 remains model/material render-compatible and
lacks only automatic standing metadata.

## Recommended next batch

Combine function 2 is now the largest remaining material blocker at 681
diagnostic occurrences, followed by combine function 5 at 221, operation 22
at 153, operation 39 at 126, and operation 255 at 68. Preserve the sequential
commit structure, deterministic census, explicit rejection, and exact fail-
closed rendering policy; keep the unchanged model-decoder clusters separate.

## 2026-08-16 combine-2/5 and operations-22/39 audit

The fresh pre-change census reproduced the operations-19/9/21/20 result at
SHA-256
`45119bbbc8911d4fa07c28ee9e608d3f656f1bbf829618c3095162fa2f629e0a`.
Each item was committed and audited sequentially. Intermediate report hashes
are combine function 2
`15d5daaf6c69a6623ec60009dacb8129a68896b297f9b141687cb769f274978d`,
combine function 5
`fb2c3395888130b7ac4f4369498d78fd731d970bec0235257f718073af951cc8`,
and operation 22
`66e43434479ca038c8b0095e0753e5b4f35700faa81de93e9cdbcc6d45081dab`.
Two independent complete-batch packaged-JAR censuses were byte-identical at
SHA-256
`f3fb5c74d0142ba7a0af455f457ce7b55445985ba689bd16eb4a44ef8c01eacb`.
The cache identity remained unchanged.

| Stage | Ready | Missing automatic animations | Unsupported material | Other failure |
| --- | ---: | ---: | ---: | ---: |
| Before batch | 2,371 | 575 | 1,075 | 11 |
| After combine function 2 | 2,844 | 616 | 544 | 28 |
| After combine function 5 | 3,005 | 638 | 356 | 33 |
| After operation 22 | 3,091 | 645 | 263 | 33 |
| After operation 39 | 3,136 | 646 | 216 | 34 |
| Combined change | +765 | +71 | -859 | +23 |

Unsupported models remain 3,946 and morph/internal definitions remain 612 at
every stage. Per-item attribution is:

- Combine function 2 accounted for 681 diagnostic occurrences in 546 NPC
  reasons. Both reach zero; 473 definitions become ready, 41 become missing-
  automatic-animations, 17 expose precise automatic-sequence failures, and
  15 retain another material blocker. The pinned node computes raw signed
  `first - second` independently for monochrome and each color channel.
- Combine function 5 accounted for 221 diagnostic occurrences in 215 NPC
  reasons at its checkpoint. Both reach zero; 161 definitions become ready,
  22 become missing-automatic-animations, five expose precise sequence
  failures, and 27 retain another material blocker. The pinned screen formula
  is `4096 - ((4096 - first) * (4096 - second) >> 12)` with Java integer
  overflow and no node-local clamp.
- Operation 22 accounted for 153 diagnostic occurrences in 150 NPC reasons at
  its checkpoint. Both reach zero; 86 definitions become ready, seven become
  missing-automatic-animations, and 57 retain another material blocker. It
  preserves coordinates and computes `4096 - child` using either the child's
  monochrome output or each color channel according to serialized code 0.
- Operation 39 grew from 126 baseline occurrences to 165 in 76 NPC reasons as
  operation 22 exposed more graphs. Both reach zero; 45 definitions become
  ready, one becomes missing-automatic-animations, NPC 1553 exposes a precise
  sequence-1474 trailing-byte failure, and 29 retain another material blocker.
  Its serialized unsigned sprite ID resolves index 8/archive ID/file 0, selects
  frame 0, expands the trimmed frame into its full canvas, and nearest-scales
  RGB coordinates. The pinned software path ignores alpha and monochrome
  consumers read its red channel.

All fixed-point operations preserve Java overflow until the final texture-root
clamp. Serialized output-mode values other than exactly `1` remain color, as in
the pinned client. Functions 1, 3, and 6 and every previously supported
operation remain covered. No material, texture, color, average-material,
missing-sprite, or alpha fallback is introduced.

Remaining diagnostic occurrences are:

| Blocker | Before | After |
| --- | ---: | ---: |
| Combine function 2 | 681 | 0 |
| Combine function 5 | 221 | 0 |
| Combine function 7 | 37 | 53 |
| Combine function 8 | 16 | 16 |
| Combine function 10 | 25 | 25 |
| Operation 12 | 30 | 30 |
| Operation 17 | 25 | 25 |
| Operation 22 | 153 | 0 |
| Operation 39 | 126 | 0 |
| Operation 255 | 68 | 70 |

The increases in functions 7 and operation 255 are newly reachable blockers,
not substitutions. Curve interpolation remains 18 occurrences for mode 1 and
seven for mode 2; unexpected Fill parameter 18 remains three occurrences. The
3,946 unsupported-model results remain split between
`BufferUnderflowException` (1,954 definitions) and invalid decoded offsets
(`newPosition > limit`, 1,992 definitions).

NPC 284 (Doric) represents both combine functions through graph paths in
textures 132 and 229, with seven component models, 15 model-referenced materials,
458 textured faces, standing sequence 101, and walking sequence 98. NPC 146
(Gull) represents operations 22 and 39 through the path that includes textures
471 and 366, with model 26841, four materials, 344 textured faces, standing
sequence 6771, and walking sequence 6773. For each NPC, two terminal-only
validation renders from the shaded packaged JAR exercised all 18 cells,
visible pixels, and transparency and were byte-identical. Doric's PNG SHA-256
is `9042a427ddaa86ba8049fdb7cf7bcf4e0106d8684f2e280f5d59318d2dc962ad`
and provenance SHA-256 is
`07c7b4b195310ea6038c8db7571362f88cc8621832ce6ae28d9c80af9e209f7d`.
Gull's PNG SHA-256 is
`788ad30863f2b7d64217cfd9de81dff3734ea19300ac653a6bc80d84dedd7bd1`
and provenance SHA-256 is
`f30075c63a3d02b0dff151be681461df19719ec57b78fc7a1e7abdcb39b996af`.
NPC 72 remains fully automatic and ready. NPC 40 remains model/material
render-compatible and lacks only automatic standing metadata.

## Recommended next batch

Operation 255 is now the largest remaining opcode blocker at 70 diagnostic
occurrences, followed by combine function 7 at 53, operation 12 at 30,
operation 17 and combine function 10 at 25 each, and combine function 8 at 16.
Operation 255 is outside the pinned 0-through-39 factory and should first be
traced as a serialization or cache-revision issue before adding a node. Keep
the unchanged model-decoder clusters in their own focused batch.

## 2026-08-16 apparent operation-255 investigation

`MaterialOpcode255AuditMain` is a terminal-only forensic companion to the
census. It records every affected definition and model/material route, raw
graph hashes and byte windows, and parallel framing traces without changing
the production decoder:

```bash
mvn clean package
java -cp target/rsc-sprite-baker.jar \
  com.spoiledmilk.spritebaker.MaterialOpcode255AuditMain \
  --cache /path/to/user-supplied/cache \
  --output /tmp/rsc-sprite-baker-op255-audit.json
```

The output path must remain outside the cache and repository. Two independent
packaged-JAR runs on the pinned cache were byte-identical at SHA-256
`816db2d8b273e70122169c00b5a36fd4db59010a16834e03ddcaff6d524b763f`.
The dat2 and reference-index hashes remain the values recorded above.

### Determination

There is no operation 255 in this graph. Material/texture graph 168 is 186
bytes at index 9/archive 168/file 0, with SHA-256
`5bd02af05b2407b362e3ae40eec7218e0ef25b45f7d08b3b51e48db8f0a7e187`.
Its 16-node graph is structurally complete and is followed by the exact nine
bytes read by the pinned `GlTexture` material wrapper. This rules out
truncation and corruption. All affected definitions share this single graph,
which rules against a mixed-revision cluster.

The pinned revision-530 `Texture` factory creates only operations 0 through
39. Its `TextureOpMonochromeFill` reads parameter 0 with one unsigned-byte
read and scales it as `(value << 12) / 255`. The baker currently reads that
parameter as an unsigned 16-bit value. At graph offsets 72 through 80 this has
a decisive framing consequence:

| Offset | Pinned meaning | Current baker framing |
| ---: | --- | --- |
| 73 | operation 0 | operation 0 |
| 76 | parameter code 0 | parameter code 0 |
| 77 | one-byte fill value `cb` | first fill-value byte |
| 78 | next node descriptor 6 | second fill-value byte |
| 79 | next node operation 8 (curve) | next node descriptor 8 |
| 80 | curve cache byte `ff` | apparent operation 255 |

The exact 25-byte evidence window, offsets 68 through 92 inclusive, is
`010003050500010100cb0608ff0100000300000b3b03120fff`. The complete raw graph
also contains `ff` parameter/cache values at offsets 29, 30, 60, 92, 143, and
155; none occupy a type field under pinned framing. With the one-byte fill
width, all 16 descriptors and nodes align, roots 8 and 14 are read correctly,
all 186 bytes are consumed, and the next genuine unsupported node is operation
17 at type offset 111. Thus the classification is:

- genuine material operation: no;
- parser desynchronization: yes;
- incorrect parameter length/signedness: length is wrong (two bytes instead
  of one unsigned byte); signedness is not the cause;
- mixed cache revisions: no evidence;
- truncated or corrupt data: no;
- sentinel or non-operation value: no—the byte is the curve node's ordinary
  cache setting.

The primary comparison is the pinned commit
`a569f0af7754ada96ed7ac76d7582b2c7511b7a0`: `Texture.java` supplies graph
framing and the 0-through-39 factory, `TextureOpMonochromeFill.java` supplies
the one-byte fixed-point decode, `TextureOpCurve.java` establishes the valid
255 cache byte, `TextureOp17.java` establishes the newly exposed real node,
and `GlTexture.java` accounts for every trailing byte.

### Exact affected scope

The production census reports 70 repeated diagnostics across 62 definitions;
the forensic inventory deduplicates them to one causal material/texture graph,
168. Every affected NPC is listed here by display name and ID:

- Sheep: 42, 1762, 1764, 3311, 5148–5155, 5165; Golden sheep: 5172.
- Crawling Hand: 1648, 1649, 1654, 1655; Bloodworm: 2031; Large mosquito:
  2493; Bullrush: 3336.
- Big Snake: 3484; Swamp snake: 3599–3602; Dead swamp snake: 3603–3605;
  Sea Snake Young: 3939; Sea Snake Hatchling: 3940; Giant Sea Snake: 3943.
- Splatter: 3727–3731, 7595–7597; Spinner: 3747–3751, 7598.
- Autumn Elemental: 5533–5538; Summer Elemental: 5547–5552.
- War tortoise: 6815, 6816; Mutated bloodveld: 7642, 7643.

Their complete component-model set is
`1674, 6632, 8904, 13910, 13911, 14171, 14549, 14550, 15096, 20283,
20284, 20285, 20288, 20289, 22264, 22265, 22269, 22270, 30460, 33713,
33715, 33716, 33718, 33723, 33728`. The ten models with faces directly bound
to 168 are `1674, 6632, 8904, 14549, 14550, 20288, 20289, 22265, 33713,
33715`; the JSON report preserves all 39 NPC/model/source/resolved-material
routes and face counts. The full model-referenced material context for these
definitions is `59, 91, 111, 134, 157, 168, 179, 182, 183, 192, 283, 297,
317, 318, 338, 345, 347, 379, 386, 394, 415, 432, 444`. Only graph 168 owns
the framing defect; the other IDs explain indirect dependency and multipart
scope and must not be described as opcode-255 graphs.

At this investigation checkpoint production remained deliberately unchanged
and fail-closed at the apparent 255. No operation, material, color, texture, or
average-material substitute was added. The separately reviewed operation-0
correction described below subsequently applied the identified one-byte rule
and continues to reject the then-exposed operation 17 explicitly.

Two independent packaged-JAR full censuses remain byte-identical at SHA-256
`f3fb5c74d0142ba7a0af455f457ce7b55445985ba689bd16eb4a44ef8c01eacb`:
3,136 ready, 646 missing automatic animations, 216 unsupported material,
3,946 unsupported model, 612 morph/internal definitions, and 34 other
failures. NPC 72 remains fully automatic and ready; NPC 40 remains
model/material render-compatible and lacks only automatic standing metadata.

The licensed-cache distribution build reran all 138 tests and its terminal
archive inspection confirmed the audit entry point in both application JARs,
the exact 31-file read-only cache payload, license/source records, empty
adjacent exports directory, and safe archive paths. The external inspection
artifacts were SHA-256
`ba86aa1cb75101e4290d9494507bd8fd0bb596582986e99b3da792f32c168279`
(Linux) and
`b325f0ed0294616f1f3a72cef826c5786d58a94a2d5fc90a52ec577586f49285`
(Windows); neither archive is committed.

## 2026-08-16 operation-0 framing correction

Operation 0 parameter 0 now consumes exactly one unsigned byte and stores the
pinned fixed-point value `(value << 12) / 255`. This is the narrow correction
established by the preceding forensic investigation; operation 17 remains
unimplemented and explicitly rejected, and no rendering fallback was added.

Legacy neutral fixtures were handled according to what they were testing.
Fixtures that genuinely represent operation-0 fill now serialize one byte.
Fixtures that had used the old two-byte behavior merely to inject arbitrary
16-bit constants into combine, displacement, interpolation, invert, or
overflow tests now use supported monochrome range nodes with equal minimum and
maximum. Their high-range fixed-point and Java-overflow coverage is therefore
preserved without encoding invalid operation-0 data.

Two independent full-cache censuses from the shaded JAR were byte-identical at
SHA-256
`5b48790613518af6b3bb45487518d5d502fa4ae88d27025033187b18731a6837`.
The cache identity and every top-level category remain unchanged:

| Category | Before | After |
| --- | ---: | ---: |
| Ready | 3,136 | 3,136 |
| Missing automatic animations | 646 | 646 |
| Unsupported material | 216 | 216 |
| Unsupported model | 3,946 | 3,946 |
| Morph/internal definition | 612 | 612 |
| Other failure | 34 | 34 |

All 62 definitions identified by the opcode-255 investigation advance beyond
the false type. Their 70 repeated material-168 diagnostics change exactly from
operation 255 to genuine unsupported operation 17; no affected definition
changes top-level category because graph 168 still fails closed. Global
operation-17 occurrences consequently rise from 25 to 95, while operation-255
occurrences fall from 70 to zero. The complete operation totals are now
operation 17 at 95 and operation 12 at 30.

The correction also removes all three `operation parameter 18 for Fill`
diagnostics in texture 134. Correct framing exposes combine function 10 there,
raising its occurrences from 25 to 28. Other remaining material blockers are
unchanged: combine function 7 at 53, combine function 8 at 16, curve
interpolation mode 1 at 18, and curve interpolation mode 2 at seven. The first
genuine blockers for the former opcode-255 definitions are therefore:

- operation 17 in material 168 for all 62 definitions (70 repeated
  diagnostics);
- combine function 7 in material 183 for the six Spinner definitions;
- curve interpolation mode 1 in material 415 for the 12 seasonal Elementals;
- combine function 10 in material 134 for Bullrush NPC 3336.

The operation-255 terminal audit remains usable after the correction. Schema
2 labels the old two-byte parse as historical, traces the production one-byte
framing, inventories the same 62 definitions and 25 component models through
their material-168 operation-17 result, and records that no false opcode 255
remains. Two independent reports were byte-identical at SHA-256
`f07351d012865192921d33585da5e6a18fecc8a35c1750c4b450832479980e7f`.
NPC 72 remains fully automatic and ready. NPC 40 remains
model/material render-compatible and lacks only automatic standing metadata.

The licensed-cache distribution build reran all 140 tests and passed terminal
inspection for both archives, including the exact read-only cache payload,
license/source records, empty adjacent exports folder, safe paths, and audit
entry point. The external artifacts were SHA-256
`8fc25bd7497e8eaae35bbaa7548cba8b519a12ba5b5f6907375de1080c5dd7fa`
(Linux) and
`a569149e39b38661eb5785513f31fe010641fb51d8f522c7bfd437cf292a6332`
(Windows); neither is committed.

## Recommended next batch

Operation 17 is now the largest remaining opcode blocker at 95 diagnostic
occurrences and is the required next material-semantics batch. It must be
traced independently from the pinned client; the operation-0 correction does
not establish its parameters or rendering behavior. Combine function 7 follows
at 53 occurrences, operation 12 at 30, combine function 10 at 28, and combine
function 8 at 16. Preserve explicit rejection and the no-substitution policy.
