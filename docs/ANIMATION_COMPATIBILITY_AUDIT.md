# Revision-530 animation compatibility audit

## Scope and baseline

The deterministic census at SHA-256
`97e06eae6129d5a91bc5292a9aa5ce8ecbb324edcadfef40da0fed3e00072990`
contained 228 `other-failure` definitions. Exactly 227 failed while decoding or
validating an automatically selected sequence; NPC 1688 failed earlier on
unrelated NPC-definition opcode 138 and was excluded from production changes.
Missing automatic animation metadata remained a separate census category and
was not treated as a decoder error.

The 227 animation failures grouped by observed symptom as follows:

| Symptom before correction | Definitions | Root cause |
| --- | ---: | --- |
| Unsupported sequence opcode | 123 | Parser desynchronization after incorrectly framed opcode 13 or incorrectly consumed opcode 16 |
| Frame lookup/decode `ArrayIndexOutOfBoundsException` | 61 | Misframed sequence data produced incorrect packed frame IDs; the frame decoder was not the cause |
| Sequence trailing bytes | 43 | Opcode 13 variable records or opcode 16's following opcode were left/consumed at the wrong boundary |
| Unrelated NPC definition decode | 1 | NPC 1688 opcode 138; outside this animation batch and still fail-closed |

Priority clusters were sequence 4689 trailing bytes (29 definitions), sequence
112 downstream frame decoding (21), sequence 10920 apparent opcode 24 (18),
and sequence 10288 downstream frame decoding (16). All four, and every related
shared animation cluster, resolve through the same bounded sequence correction.

## Pinned evidence and correction

The pinned client is revision
`a569f0af7754ada96ed7ac76d7582b2c7511b7a0`, primarily `SeqType.java` and
`AnimFrame.java`.

Sequence opcode 13 starts with an unsigned 16-bit entry count. Each entry then
starts with an unsigned byte variant count. Zero has no payload. A positive
count consumes one unsigned 24-bit sound value followed by `count - 1`
unsigned 16-bit alternate values. Production previously read a one-byte entry
count followed by a flat list of 24-bit values.

Sequence opcode 16 has no serialized payload and sets a boolean. Production
previously consumed one signed byte. The corrected decoder records the flag
without advancing beyond the opcode. Unknown opcodes and trailing bytes remain
explicit errors; no bytes, frames, or sequences are ignored or invented.

Representative raw sequence identities after exact decoding:

| Sequence | Bytes | Frames | SHA-256 |
| ---: | ---: | ---: | --- |
| 4689 | 107 | 15 | `a9baeb07ae7f15e4a7f7a2313e81927ceba1409342e079a3d7a167ad644b9a3b` |
| 112 | 23 | 2 | `77480ce3ba507ad0b66fef8544e8c4d6381c74bc4ac567875960d93a49629183` |
| 10920 | 229 | 32 | `743b8ceec4d1ff9c9d9931945571902ca75f8d7d2c36865876d01885dcfb209d` |
| 10288 | 119 | 16 | `80b0be10a2a5fa78df2bacb382e06991bda3a4f121ee3a17fbeff786da8739e8` |

Focused neutral fixtures cover zero-, one-, and multi-variant opcode-13
records, opcode 16 followed immediately by another opcode, exact flag
retention, and truncated variable records. Existing unknown-opcode and final
stream-consumption checks remain unchanged.

## Result

Two post-change censuses were byte-identical at SHA-256
`ce9ea749886e502b72f5f57788f0346ee8970057a96d2fc59b7b66baac748d78`.
All 227 animation failures become ready. Ready rises from 6,698 to 6,925 and
`other-failure` falls from 228 to one. Missing automatic animations remains
1,051 exactly, proving missing metadata was neither fabricated nor
reclassified.

NPC 4813 (Vyrewatch) is the terminal packaged-render representative. Its
corrected standing sequence 4689, walking sequence 4683, models 17952/17959,
six materials, 428 textured faces, and all 18 cells validate with visible and
transparent pixels. The external project SHA-256 is
`cb7d4e6e2dc5faf89eb015e367008c285bf77e2a2adf2e417314df5af751958e`;
the PNG SHA-256 is
`757389e63ff8e90fb538dcb9d58d4c6241f2057fda43f2e1570b6df2f8006a94`;
and the provenance SHA-256 is
`bbba735a194aea63b9e88a8ed91d129fc6225bbf0956955682f57885de34d55a`.
No cache input or generated derivative is tracked.

The only remaining other failure is NPC 1688's unsupported revision-530 NPC
definition opcode 138 at byte 115. It is not an animation failure, and this
batch deliberately leaves it fail-closed.
