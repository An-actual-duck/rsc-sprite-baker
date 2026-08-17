# Model BufferUnderflow diagnostic audit

`ModelBufferUnderflowAuditMain` is a deterministic, terminal-only diagnostic
for the NPC definitions whose current production model load stops with
`BufferUnderflowException`. It does not decode replacement geometry and is not
used by `CacheReader`, rendering, compatibility classification, or the desktop
workflow.

```bash
mvn clean package
java -cp target/rsc-sprite-baker.jar \
  com.spoiledmilk.spritebaker.ModelBufferUnderflowAuditMain \
  --cache /path/to/user-supplied/cache \
  --output /tmp/rsc-sprite-baker-model-underflow.json
```

The output must be outside the cache and repository. It contains cache
identity, every currently affected NPC and all of its component model IDs,
deduplicated failing model records, raw byte lengths and SHA-256 hashes, exact
23-byte footers and trailing windows, format markers, parsed unsigned counts
and flags, calculated stream layouts, precise dependency failure reads,
structural clusters, and successful neighboring-model comparisons. Source
model bytes are never written to the report or repository.

## 2026-08-17 result

Two complete runs over all 8,590 definitions were byte-identical at SHA-256
`af3d2487d360a3f10eb77dec5753634030acd09e1cc740b991f43e09dbf633b1`.
The cache identity was unchanged:

- dat2 SHA-256: `b5431211b019b9403b4cfca933f4c9635c1d5278d3730995dced0d8672b1cc91`
- reference-index SHA-256: `83a2292c515596af0423764c48e41dfe1aac482920dca0b89ecb343db6dd4c30`

The 1,954 affected NPC definitions deduplicate to 669 failing model IDs. Every
one is a structurally complete type-1 file with the `ff ff` trailer. Every
pinned revision-530 layout ends exactly at the start of its 23-byte footer.
None has an unknown texture-render type, particle/revision extension, pinned
layout gap, or evidence of truncation.

| Complex texture faces | Unique models | Affected NPCs | Representative | Dependency failure |
| ---: | ---: | ---: | ---: | --- |
| 7 | 205 | 768 | 496 | extension i32 at offset 2691; requested 4, remaining 2 |
| 8 | 157 | 445 | 277 | extension i32 at EOF; requested 4, remaining 0 |
| 9 | 126 | 450 | 560 | third extension u16 at EOF; requested 2, remaining 0 |
| 10 | 111 | 391 | 2970 | second extension u16 at EOF; requested 2, remaining 0 |
| 11 | 70 | 204 | 541 | first extension u16 at EOF; requested 2, remaining 0 |

NPC counts overlap when a definition references failing models from more than
one signature. The seven-face signature is the highest-yield cluster. Model
496 affects NPC 36 (Wyson the gardener), NPC 811 (Stanford), and 766 other
definitions. Other representative routes include model 277 from NPC 690
(Tower Archer), model 560 from NPC 1006 (Sea slug), model 2970 from NPC 118 (Dwarf), and
model 541 from NPC 9 (Guard). The external JSON records every route and full
component-model list.

Representative raw evidence:

| Model | Bytes | SHA-256 | Exact 23-byte footer |
| ---: | ---: | --- | --- |
| 496 | 2,693 | `b26004e3767d02445f048d81e7fa80aca3e127cbf187e5aa3a787fa6dad14fce` | `007500e207010a0001010100560052004a010400e2ffff` |
| 277 | 6,043 | `1ee9be55b09600620d2ce40e20ee7050759b56228a0ef7eb1d51bed1309e0eda` | `011d01ef0800ff0001010100ee00f200f2027301ecffff` |
| 560 | 2,730 | `23bff588202db8e2824afb41982eb51e6adae9a08b34480405c1f8e123ce7b29` | `008500fa0900ff0000010100520052004c012900faffff` |
| 2970 | 1,838 | `ea1ca727b28985769cc7265cfb88c6585bb3f6c6a54b045c44d66506d746678e` | `005900a50b00ff0000010100460051004900c30029ffff` |
| 541 | 2,265 | `82bb105fcfb932539ebcbd92743b5c2e7eae4a2346878fbabb2d4669482cf278` | `005300a60b01ff00010101003d0047004e00ae00a6ffff` |

## Pinned and dependency comparison

The evidence source is revision
`a569f0af7754ada96ed7ac76d7582b2c7511b7a0` of
`client/src/main/java/rt4/RawModel.java`. Its constructor selects the new
revision-530 format only for `ff ff`, uses a 23-byte footer, reads vertex and
face counts as unsigned shorts and texture counts/flags as unsigned bytes, and
lays out each complex texture face as:

```text
P/M/N 6 bytes + scale X/Y/Z 6 bytes + rotation 1 byte
             + auxiliary 1 byte + auxiliary 1 byte
```

Type 2 adds two final cube bytes. The dependency is
`net.runelite:cache:1.12.35` `ModelLoader.decodeType1`. Its earlier sections,
footer size, unsigned counts, optional-stream flags, and texture-coordinate
length agree with the pinned client. Its final offset calculation instead
allocates two bytes for rotation and three auxiliary bytes per complex face.
It therefore advances exactly two excess bytes per complex face. It also only
populates type-0 texture triples in this decoder path, so correcting the final
offset alone would not constitute complete complex-texture support.

For 7–11 complex faces the excess is 14–22 bytes. The dependency lands inside
or beyond the footer, treats a footer byte as an extension flag, and fails at
the exact read shown in the table. Production stack frames independently
match `decodeType1` lines 1106–1109 for all 669 models.

Successful neighboring comparisons reinforce the result. Models 276/278,
495/497, 540/542, 559/561, and 2968/2971 were checked around the five
representatives. Type-1 neighbors with one to six complex faces also have a
pinned data end exactly at the footer, while the dependency overruns by two
bytes per face. They happen to return only because the probed footer byte is
zero or enough footer bytes remain to satisfy the spurious extension reads.
Those successes are accidental and are not evidence that the dependency has
decoded their complex texture streams correctly. Model 497 is an old-format
control and does not have the type-1 discrepancy.

The audit therefore rejects these alternative explanations:

- wrong format selection or footer-size assumption;
- signed/unsigned count errors;
- missing optional streams or a texture-coordinate length error;
- genuine revision extensions;
- truncated/corrupt data or mixed cache revisions.

The root cause is a dependency-library limitation: its type-1 complex-texture
layout is not the pinned revision-530 layout.

## Narrowest safe follow-up

Implementation should proceed in this order:

1. Add a revision-530 type-1 decoder selected only for `ff ff` models whose
   complete pinned stream calculation ends exactly at the 23-byte footer.
2. Port every pinned stream, including complex texture types 1–3 and their
   one-byte rotation/auxiliary fields; validate each boundary before reading.
3. Compare decoded vertices, faces, indices, bones, colors, texture IDs,
   texture coordinates, and complex mappings against pinned representatives
   from all five signatures and against successful one-to-six-face controls.
4. Rerun the full census and then separately assess accidentally successful
   complex-texture models; do not assume this batch's 1,954-definition gain is
   the complete visual impact.

Old, type-2, and type-3 models should remain on the dependency decoder. No
geometry may be skipped, synthesized, or substituted. The principal known
risk is that a footer-only patch could clear the exceptions while leaving
complex mappings silently wrong; the follow-up must implement and test the
entire pinned type-1 path.
