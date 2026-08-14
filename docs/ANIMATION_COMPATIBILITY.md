# Phase 2 animation compatibility

## Revision-530 client behavior

The reference behavior was traced from OpenRS2's archived build 530 client
(dated 2009-01-28). The analyzed jar was kept in `/tmp`, was not executed, and
is not present in this repository. Its SHA-256 was
`3d0ea4ddb99319482356ce2b0f38ba6dc82cd351a8ba193d2c306a4c1e0fe4bd`.

The client resolves a sequence from config index 20 using archive
`sequenceId >>> 7` and file `sequenceId & 127`. Sequence opcode 1 stores frame
durations followed by low and high halves of packed frame IDs. A packed ID's
high half selects a frame-set archive in index 0 and its low half selects the
file. The frame's first unsigned short identifies a framemap at index 1,
archive `framemapId`, file 0.

The frame uses two concurrent byte streams: transform flags begin after its
framemap ID and slot count, while signed-short-smart transform values begin
after every flag byte. Missing scale values default to 128; other transform
values default to zero. A non-pivot transform inserts the closest omitted
preceding type-0 pivot. Revision 530 reshuffles type-2 encoded values into
0..2047 client angle units. The framemap contains transform types, a boolean
per slot, a 16-bit transform mask per slot, and lists of model skin groups.

The actor update loop treats durations as client cycles. It starts a frame at
cycle 1 and advances only when `cycle > duration`, resetting the new frame to
cycle 1. One client cycle is 20 ms. Rendering passes `cycle - 1` as the tween
numerator and the current frame duration as denominator. Tweening occurs when
the sequence's tween flag or the client's global tween option is enabled and
the next frame has the identical framemap instance. Rotation takes the shortest
path modulo 2048; HSL hue takes the shortest path modulo 64; translation,
scale, alpha, saturation, and lightness are linear integer interpolation.
Per-transform hold flags can suppress interpolation.

Transforms are applied after component models are combined. Type 0 calculates
a pivot across all selected vertex groups, type 1 translates, type 2 rotates,
type 3 scales relative to the pivot, type 5 changes face alpha in steps of 8,
and type 7 changes packed HSL. This MVP implements those operations. The
revision-530 transform-mask overload is recorded but masks other than 65535
are not yet applied.

## Read-only cache proof

The same Phase 1 cache identity was inspected in place at
`/home/justin/2009scape/Server/data/cache`; no file under 2009scape was
modified. NPC 72 (`Troll`) still resolves to untextured model 3752 and BAS 42.
The live compatibility probe decoded and posed:

| Purpose | Sequence | Frames | Cycles | Framemap |
| --- | ---: | ---: | ---: | ---: |
| standing | 286 | 2 | 42 | 461 |
| walking | 283 | 8 | 54 | 461 |
| attack probe | 284 | 7 | 45 | 461 |
| defence probe | 285 | 4 | 33 | 461 |

Sequence 284 is a verified compatible attack-shaped source pose for the
bounded NPC-72 probe; the selector does not assert that an arbitrary NPC's
combat sequence can be inferred from BAS metadata. The user explicitly loads
and chooses combat sources.

An external 18-cell proof export at 1536×768 was generated twice from standing
286, walking 283, and combat 284 with a shared orthographic viewport. The PNG
SHA-256 was
`05c953297327a91803a0bc574e196f8139a517a7c016a36749070af322902fed`.
The PNG, source project, cache frames, and decoded geometry remain outside Git.

## Boundaries and limitations

- RuneLite 1.12.35 remains the BSD-2-Clause JS5 store and model decoder. The
  local strict adapters cover the legacy revision-530 metadata and animation
  formats that current RuneLite loaders do not model exactly.
- Textured models remain rejected. Animation blending, sequence interleave
  masks, equipment overrides, NPC morph resolution, client-exact rasterization,
  and non-65535 transform masks are out of scope.
- Timeline thumbnails show every encoded keyframe. The 20 ms scrubber previews
  deterministic in-between positions when tweening is enabled.
- Export uses a shared orthographic scale, origin-centered horizontal anchor,
  and ground anchor for all 18 cells. Phase 3 adds configurable target canvases,
  supersampled nearest-neighbor reduction, and visual treatment without changing
  this animation compatibility layer.
- Suggestions are deliberately non-authoritative: they only populate empty,
  unlocked cells. User cell overrides and locks are persisted.

Tests create neutral sequence, framemap, frame, skinned-model, timeline, sheet,
and project fixtures. They do not contain cache or derived game data.
