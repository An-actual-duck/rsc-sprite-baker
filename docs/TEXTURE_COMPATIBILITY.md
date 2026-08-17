# Revision-530 Texture Compatibility

## Bounded implementation

Phase 4 keeps RuneLite `cache` 1.12.35 as the BSD-2-Clause JS5 and model
decoder. Revision 530 does not use RuneLite's current texture-archive layout:
index 9 contains one procedural graph per archive, while index 26 archive 0
file 0 is a parallel-array material table. The local adapters decode that
layout independently and fail closed when a graph needs an unsupported
operation. No decompiled client or external viewer implementation is copied or
linked.

The supported procedural operations are deliberately limited to the first
verified textured NPC: monochrome/color fill (0/1), horizontal/vertical
gradient (2/3), randomized tiles (4), box blur (5), clamp (6),
addition/multiply/overlay combine (7 functions 1/3/6), linear curve parsing
(8), coordinate flip (9), custom sampled color gradient (10 preset
0), hash noise (13), cellular distance noise (15), coordinate displacement
(19), tiling (20), interpolation (21), stripes (27), range (30),
bump lighting (32), multi-octave gradient
noise (34), nested texture dependencies (36), and line noise (38). Texture
generation uses the software client's 64/128 material-size flag and horizontal
order. It uses a fixed, manifest-recorded gamma of 1.0 instead of the source
client's preference-dependent and randomly perturbed brightness value.
Operation 4 is the client's zero-child monochrome randomized tile generator.
Its serialized parameters are unsigned 8-bit column count (code 0) and row
count/RNG seed (code 1), followed by unsigned 16-bit horizontal jitter (code
2), vertical jitter (code 3), alternating row offset (code 4), vertical phase
(code 5), mortar width (code 6), and brightness variation (code 7). Defaults
are 4, 8, 409, 204, 1024, 0, 81, and 1024. The implementation preserves the
client's bounded Java RNG sequence, 12-bit boundary calculations, alternating
offset sign, 4096-period wrapping, strict mortar comparisons, and monochrome
brightness output. Empty grids and unexpected parameters fail closed. The
primary trace is
[`TextureOp4.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOp4.java).
Operation 5 is the client's one-child separable box blur. Its serialized
parameters are unsigned 8-bit horizontal radius (code 0), vertical radius
(code 1), and monochrome-output flag (code 2), with defaults 1, 1, and color
output. Both passes wrap at material boundaries and use the client's truncated
16-bit reciprocal `65536 / (2r + 1)`, including truncation between horizontal
and vertical passes. Monochrome mode reads a color child's first channel and
replicates the blurred result; color mode blurs all three channels
independently. Unexpected parameters fail closed. The primary trace is
[`TextureOp5.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOp5.java).
Operation 6 is the client's one-child clamp node. Parameter code 0 is an
unsigned 16-bit lower bound (default 0), code 1 is an unsigned 16-bit upper
bound (default 4096), and code 2 selects monochrome output only when its byte
equals 1. Color output clamps each child RGB channel independently;
monochrome output clamps the child first/monochrome channel and repeats it
across RGB. Bounds and channel values stay in their serialized fixed-point
domain with no scaling, interpolation, or arithmetic overflow. The child is
sampled at the identical X/Y coordinate, so the clamp adds no transform or
wrapping beyond the child's own behavior. Inclusive comparisons and even a
lower bound greater than the upper bound retain the client's exact branch
ordering. Unexpected parameters fail closed. The primary trace is
[`TextureOpClamp.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOpClamp.java).
Combine operation 7 has two child inputs and defaults to overlay function 6.
Parameter code 0 is the unsigned function ID; code 1 selects monochrome output
only when its unsigned byte equals 1. Function 1 adds child 1 to child 0 with
plain Java signed `int` arithmetic and no fixed-point rescaling. Color mode
adds the corresponding RGB channels; monochrome mode adds the first channel
from each child and repeats the result across RGB. Function 2 subtracts child
1 from child 0, function 3 multiplies the operands and shifts right 12, and
function 5 computes screen as
`4096 - ((4096 - child0) * (4096 - child1) >> 12)`. Function 6 treats child 1 as the overlay
control: values below 2048 produce `child1 * child0 >> 11`; values at or above
2048 produce `4096 - ((4096 - child0) * (4096 - child1) >> 11)`. Color mode
applies that branch independently to RGB. Monochrome mode reads the first
channel of each child and repeats the result across RGB. Function 7 is color
dodge with child 0 as the denominator control:
`(child1 << 12) / (4096 - child0)`. Exactly `child0 == 4096` returns 4096;
other zero, negative, or out-of-range operands follow Java signed `int` shift
overflow and division toward zero without another guard. Calculations use
Java signed `int` overflow and have no node-level clamp; only the existing final
texture conversion clamps channels to 0..255. Color mode applies each function
independently to RGB, while monochrome mode reads the first channel and repeats
it. Function 10 compares the two raw signed fixed-point operands and returns
their maximum independently per color channel or for the monochrome first
channel. It performs no arithmetic, division, zero handling, fixed-point
rescaling, wrapping, or node-local clamp; upstream overflowed integers retain
Java signed comparison. Every other function ID remains
an explicit unsupported-material error. The primary trace is
[`TextureOpCombine.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOpCombine.java).
Operation 9 is the client's one-child coordinate flip. Unsigned byte codes 0
and 1 enable horizontal and vertical reversal only when equal to 1; both
default to enabled. Code 2 similarly selects monochrome output, which defaults
to color. Enabled axes sample `mask - coordinate`, disabled axes preserve the
coordinate, and no fixed-point arithmetic or additional wrapping occurs.
Color mode preserves all child channels; monochrome mode samples the child
first/monochrome channel and repeats it across RGB. Unexpected parameters fail
closed. The primary trace is
[`TextureOpFlip.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOpFlip.java).
Operation 12 is the client's zero-child monochrome waveform generator. Code 0
selects coordinates (default 0): zero uses the difference between the 12-bit X
and Y fractions, while every nonzero value uses the centered half-scale radial
distance and multiplies it by pi. Code 1 selects the waveform (default 0): zero
indexes the client's 256-entry sine table, two produces a triangle, and every
other value emits the phase ramp. Code 3 is an unsigned frequency byte
(default 1). Codes 2, 4, 5, and 6 are serialized zero-byte fields in material
275 and are ignored by the pinned decoder without consuming payload. Other
unobserved codes remain fail-closed.

Linear or radial phase is multiplied by frequency and wrapped with the pinned
`phase -= phase & 0xFFFFF000`, producing 0..4095. Radial distance preserves the
client's float division, `Math.sqrt`, double multiplication, and narrowing
casts. Sine-table construction and all coordinate and waveform expressions
retain Java signed `int` evaluation. Legal unsigned parameters and 64/128
texture coordinates keep operation-local products within `int`; there is no
implicit widening, child input, color mode, node-local clamp, or texture
substitution. The primary traces are
[`TextureOp12.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOp12.java),
[`TextureOp.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOp.java),
and
[`Texture.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/Texture.java).
Operation 13 is the client's zero-child monochrome hash-noise node and has no
serialized parameters. For every texel, it hashes the 12-bit X/Y fractions
with Java `int` overflow, masks the polynomial result to a non-negative integer,
scales it, and applies Java signed remainder `% 4096`. Unexpected parameters
fail closed. The primary trace is
[`TextureOpNoise.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOpNoise.java).
Operation 15 is the client's zero-child monochrome cellular-distance node. Its
serialized parameters are shared X/Y scale (code 0), unsigned seed (code 1),
unsigned 16-bit feature-point jitter (code 2), nearest-distance selector (code
3), distance metric (code 4), and independent X and Y scales (codes 5 and 6).
Defaults are 5, 5, 0, 2048, selector 2, and squared-Euclidean metric 1. The
implementation preserves the client's independently seeded permutation and
512-entry signed-short offset tables, half-cell coordinate bias, periodic cell
index wrapping, 3-by-3 feature search, Java integer overflow, and ordered four
nearest distances. Metrics 0 through 5 are Euclidean, squared Euclidean,
Manhattan, Chebyshev, squared sum-of-square-roots, and fourth-root distance;
selectors 0 through 4 return the first, second, second-minus-first, third, and
fourth distances. Other metric bytes use the client's Euclidean branch and
other selector bytes produce zero. The node has no child, texture, sprite, or
color input and always emits monochrome, repeated across RGB channels.
Unexpected parameter codes fail closed. The primary trace is
[`TextureOp15.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOp15.java).
Operation 17 is the client's one-child color HSL-adjustment node. It preserves
the child's coordinates and promotes monochrome children to three identical
channels. Code 0 is a signed 16-bit hue offset; codes 1 and 2 are signed-byte
saturation and lightness percentages converted with `(value << 12) / 100`.
All three default to zero and there is no output-mode flag. The implementation
preserves the client's integer RGB/HSL conversions, division truncation,
operand ordering, signed Java `int` overflow, 0..4096 saturation/lightness
clamps, and repeated hue wrapping. The pinned strict `hue > 4096` comparison is
also retained: hue exactly 4096 selects no sector beyond 0..5 and therefore
leaves the node's prior RGB fields unchanged. Rows are generated and cached as
whole scanlines so that this state follows the pinned request order. Gray input retains hue zero, so
positive saturation follows the pinned red-sector behavior. Unexpected codes
and truncated parameters fail closed. The primary traces are
[`TextureOp17.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOp17.java),
[`TextureOp.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOp.java),
and the signed `g1b`/`g2b` reads in
[`Buffer.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/Buffer.java).
Operation 19 is the client's three-child coordinate-displacement node. Child
0 is the sampled image, child 1 supplies angle, and child 2 supplies magnitude.
Code 0 stores an unsigned 16-bit displacement scale shifted left four bits
(default 32768); code 1 selects monochrome output only when equal to 1. Color
angle quantization is `angle * 255 >> 12 & 255`, while monochrome uses
`angle >> 4 & 255`. The exact 256-entry client sine/cosine tables, signed Java
`int` overflow, two sequential 12-bit shifts, and mask-based X/Y wrapping are
preserved. Color mode samples all source channels; monochrome samples the
source first/monochrome channel. Unexpected parameters fail closed. The
primary trace is
[`TextureOp19.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOp19.java).
Operation 20 is the client's one-child, color-only tiling node. Unsigned byte
codes 0 and 1 select horizontal and vertical tile counts, both defaulting to
4. Integer division determines tile dimensions; remainder followed by integer
rescaling maps each texel back into the child image. When a positive tile count
exceeds the texture dimension, that axis deliberately samples child coordinate
zero. Zero counts fail closed before the client's otherwise unavoidable divide
by zero. The node has no serialized output-mode flag; monochrome children are
promoted to RGB by the normal child contract. The primary trace is
[`TextureOpTile.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOpTile.java).
Operation 21 is the client's three-child interpolator. Child 0 is selected at
control 4096, child 1 at control zero, and child 2 supplies the first/
monochrome control channel. Code 0 selects monochrome output only when equal to
1. Other control values use
`((4096 - control) * child1 + control * child0) >> 12` independently per
channel, preserving operand ordering, signed Java `int` overflow,
extrapolation outside 0..4096, and the exact zero/4096 fast paths. All children
are sampled at unchanged coordinates, and no node-level clamp is added.
Unexpected parameters fail closed. The primary trace is
[`TextureOpInterpolate.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOpInterpolate.java).
Operation 27 is the client's zero-child monochrome stripe node. Its serialized
parameters are unsigned 8-bit band count (code 0), unsigned 16-bit duty width
(code 1), and unsigned 8-bit coordinate mode (code 2), with defaults 10, 2048,
and 0. Post-decode setup divides the 4096 fixed-point domain into equal integer
intervals, scales the duty width within each interval, and installs the client's
4096 sentinel boundary. Modes 0 and 1 select Y and X; modes 2 and 3 apply the
client's signed-shift diagonal transforms. Other mode bytes retain the client's
constant coordinate-zero behavior. The transformed 12-bit texture fractions
remain in the wrapped material domain, strict start/end comparisons select
either 4096 or zero, and the monochrome result is repeated across RGB channels.
There are no child, texture, sprite, or random dependencies. A zero band count
and unexpected parameter codes fail closed. The primary trace is
[`TextureOp27.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOp27.java).
Operation 32 is the client's one-child monochrome bump-lighting node. Its
serialized parameters are unsigned 16-bit gradient scale (code 0), horizontal
light angle (code 1), and vertical light angle (code 2), with defaults 4096,
3216, and 3216. The implementation preserves the 64-pixel half-scale rule,
wrapped central differences, first-channel input rule for color children,
client-generated 32,896-byte normal lookup table, fixed-point normal and light
vectors, float angle conversion, and monochrome dot-product output. Unexpected
parameters fail closed. The primary trace is
[`TextureOp32.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOp32.java).
Operation 34 is the client's zero-child monochrome multi-octave gradient-noise
node. The decoder preserves its 12-bit fixed-point interpolation, Java-seeded
permutation table, normalization flag, octave trimming, persistence or explicit
signed amplitudes, independent X/Y scales, and unsigned seed. Its output enters
the same material-provider and textured-rasterizer path as every other decoded
graph. The primary trace is
[`TextureOp34.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOp34.java).
Operation 36 resolves another procedural texture by its unsigned 16-bit ID,
preserving the dependency's 64/128 size and using deterministic nearest-neighbor
sampling when sizes differ. The provider caches successful dependencies,
rejects dependency cycles with their exact path, and rejects chains deeper than
64 graphs. This follows the revision-client `TextureOpTexture` contract while
retaining the baker's fixed gamma policy; the primary trace is
[`TextureOpTexture.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOpTexture.java).
Operation 38 is the client's zero-child monochrome line-noise node and has no
external texture or sprite dependencies. Its serialized parameters are an
unsigned 8-bit seed (code 0), unsigned 16-bit line count (code 1), unsigned
8-bit line length (code 2), unsigned 16-bit base angle (code 3), and unsigned
16-bit angle range (code 4), with defaults 0, 2000, 16, 0, and 4096. The
implementation preserves the client's Java-seeded bounded RNG, 256-entry
12-bit sine/cosine tables, angle indexing, integer endpoint calculations,
wrapped coordinates, fixed-point intensity ramp, and Bresenham-style line
drawing. Unexpected parameters fail closed. The primary trace is
[`TextureOp38.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOp38.java).
Other operation IDs, combine modes,
curve interpolation modes, presets, and sprite dependencies produce an exact
material error in the selector and exporter. Average material colors are
recorded but never used as a hidden substitute.

The apparent operation 255 formerly reported for material 168 was a parser
desynchronization, not an operation. Operation-0 parameter 0 now consumes the
pinned one unsigned byte and scales it as `(value << 12) / 255`, so the curve
cache byte of 255 remains correctly framed. Material 168 now decodes operation
17 through the exact pinned HSL path. See the forensic and follow-up audits in
`COMPATIBILITY_CENSUS.md`; no operation-255 decoder or visual substitution
exists.

Type-0 mapping triangles use the model's decoded texture-coordinate triangle.
For revision-530 type 1/2/3 mapping records, RuneLite preserves the render type
but does not expose all transform parameters. The revision-530 software path
discards those mapping triangles and shades the face using its own vertices;
the baker mirrors that narrow behavior with face-local `(0,0), (1,0), (0,1)`
coordinates and records every fallback. It does not claim full advanced UV
support.

Index-26's opaque flag selects between ordinary texels and zero-RGB alpha-test
discard. Face transparency is applied deterministically with source-over
blending. These rules are covered by generated neutral fixtures.

## Live compatibility matrix

All paths below were inspected read-only from
`/home/justin/2009scape/Server/data/cache` on 2026-08-14. Cache identity in the
external diagnostic manifest is dat2 SHA-256
`b5431211b019b9403b4cfca933f4c9635c1d5278d3730995dced0d8672b1cc91` and
reference-index SHA-256
`83a2292c515596af0423764c48e41dfe1aac482920dca0b89ecb343db6dd4c30`.

| Case | Identifiers | Result | Evidence / limitation |
| --- | --- | --- | --- |
| Untextured | NPC 72 Troll; model 3752 | Supported, unchanged | 390 vertices, 739 faces; Phase-1 PNG SHA-256 remains `b6d9ebd11c681dc61e40b5a5e4e063326a2e0071a0f7f2e57a178bf5c181e758`. |
| Textured animated | NPC 40 Shark; model 2848; sequence 10; materials 157/171 | Supported | 70 textured faces, 31 type-0 mappings, 39 documented face-local fallbacks. Two complete 18-cell exports were byte-identical: PNG SHA-256 `4568d2194f59c6d0d3118dd594531a517c83052c40fcec28896d5b348182ab44`; manifest SHA-256 `c49d42c26770f3524cfce9f9c6b572567ab3db71252aede3e92bf7e442f36a5d`. |
| Multipart | NPC 42 Sheep; models 20283/20289/20285 | Model assembly supported; materials unsupported | Three components combine with 430 textured faces. Operation 36 is resolved; remaining unsupported operations are reported and export stops. |
| Additive-combined recolored/retextured multipart | NPC 0 Hans; six component models; five recolors; materials 228/292/258/257/262/527/272/254 | Supported | Combine function 1 now decodes, including the high-volume texture 203 graph. Standing sequence 9870, walking sequence 9869, all 676 textured faces, and all 18 cells validate in two byte-identical packaged-JAR renders (PNG SHA-256 `261ccf8a8a762adf5ba6b64dd6f2b3eee3cf6d3f82b137645f5604c4778d06c6`). |
| Subtractive/screen-combined multipart | NPC 284 Doric; seven component models; graph paths include textures 132/229 | Supported | Combine functions 2 and 5 preserve pinned operand, fixed-point, overflow, and output-mode behavior. Standing sequence 101, walking sequence 98, all 458 textured faces, and all 18 cells validate in two byte-identical packaged-JAR renders (PNG SHA-256 `9042a427ddaa86ba8049fdb7cf7bcf4e0106d8684f2e280f5d59318d2dc962ad`). |
| Color-dodge animated | NPC 3747 Spinner; model 14549; materials 168/183 | Supported | Material 183 exercises combine function 7 with pinned operand, shift-overflow, division, exact-denominator guard, and output-mode behavior. Standing sequence 3906, walking sequence 3907, all 388 textured faces, and all 18 cells validate in two byte-identical packaged-JAR renders (PNG SHA-256 `4798059951d426fa3f13882fdad74de5ecf895c21c0eeab3dbfba609a598c621`). |
| Waveform animated | NPC 4521 Enchanted Broom; model 16738; materials 185/275/206 | Supported | Material 275 exercises operation 12's linear triangle path, frequency 4, and serialized zero-byte fields. Standing/walking sequence 4372, all 216 textured faces, and all 18 cells validate in two byte-identical packaged-JAR renders (PNG SHA-256 `a9e7a37220fc40e0fe2552fc16bc1ae78a5285b31a521a9df5bb0493cf359325`). |
| Maximum-combined animated | NPC 1734 Magic tree; model 21838; materials 196/110/34/8 | Supported | Material 196 exercises combine function 10's signed per-channel maximum. Shared standing/walking sequence 5750, all 805 textured faces, and all 18 cells validate in two byte-identical packaged-JAR renders (PNG SHA-256 `e631467a1125c36c2b02407b7ac7b6cc220de29740b46ef6393ff081dc7da566`). |
| Cosine-curved animated | NPC 2535 Teak; model 21849; materials 134/196/110 | Supported | Material 134 exercises curve interpolation mode 1's pinned marker selection, cosine weighting, 257-entry signed-short table, and lookup clamping. Shared standing/walking sequence 5750, all 657 textured faces, and all 18 cells validate in a packaged-JAR render (PNG SHA-256 `e21f85b33922236e6e1f0cd3b4f69cb544e17793df1534a207a9772eac17ef2a`). |
| Inverted sprite-backed animated | NPC 146 Gull; model 26841; materials 364/471/57/439 | Supported | Operations 22 and 39 resolve the pinned invert and sprite-canvas path, including texture 366's external sprite dependency. Standing sequence 6771, walking sequence 6773, all 344 textured faces, and all 18 cells validate in two byte-identical packaged-JAR renders (PNG SHA-256 `788ad30863f2b7d64217cfd9de81dff3734ea19300ac653a6bc80d84dedd7bd1`). |
| Alpha/mapping stress | NPC 61 Spider; model 24613; material 111 | Supported | Operation 34 now decodes; models, material, standing sequence 6247, and walking sequence 6248 validate. Its 298 textured faces continue to use the documented advanced-mapping fallback. |
| Hash-noise multipart | NPC 125 Ice warrior; seven component models; materials 249/291/303/302 | Supported | Operation 13 now decodes; standing sequence 842, walking sequence 841, and all 1,076 textured faces validate. |
| Line-noise animated | NPC 131 Penguin; model 21547; materials 182/347/171 | Supported | Operation 38 now decodes; standing sequence 5668, walking sequence 5666, and all 391 textured faces validate in a packaged 18-cell render. |
| Bump-lit animated | NPC 1013 Swamp toad; model 3447; material 318 | Supported | Operation 32 now decodes; standing sequence 1018, walking sequence 1021, and all 155 textured faces validate in a packaged 18-cell render. |
| Box-blurred animated | NPC 78 Giant bat; model 18898; materials 185/59 | Supported | Operation 5 now decodes; standing sequence 4914, walking sequence 4913, and all 524 textured faces validate in a packaged 18-cell render. |
| Clamped animated | NPC 79 Death wing; model 18897; materials 182/281 | Supported | Material 281 directly exercises color-output operation 6. Standing sequence 4914, walking sequence 4913, all 645 textured faces, and all 18 cells validate in two byte-identical packaged-JAR renders (PNG SHA-256 `77b981f62a7694755150cced94833cc320505f13492c8e2059ff525d2239ebfd`). |
| Displaced/flipped/interpolated multipart | NPC 74 Zombie; seven component models; materials 393/314/84/59/392/118/288/238 | Supported | Materials 393/84 directly exercise operation 19, material 118 operation 9, and material 238 operation 21. Standing sequence 5576, walking sequence 5577, all 954 textured faces, and all 18 cells validate in two byte-identical packaged-JAR renders (PNG SHA-256 `22b554fd1f66ffd0c711b8105027b6b59e367de27f732664910f80362575530a`). |
| Tiled multipart | NPC 165 Gnome shop keeper; models 2909/2901/2917; materials 57/404/125/221/121 | Supported | Material 221 directly exercises operation 20. Standing sequence 195, walking sequence 189, all 213 textured faces, and all 18 cells validate in two byte-identical packaged-JAR renders (PNG SHA-256 `8948b63a22bdd364a73d5f5b85731c0fcf496728e9bb2611f8cdb99fe53416f1`). |
| Randomized-tile material | Material 261; operations 0/4/30 | Supported | Operation 4 now decodes. A packaged-JAR render on a neutral in-memory textured triangle was deterministic with 434 visible pixels and ARGB SHA-256 `81706338fa2297a54f347e7a18fd34216b6d9f95065785d42adedbd07d0b8da0`. No affected NPC clears its other material blockers yet. |
| Striped multipart | NPC 560 Jiminua; seven component models; materials 228/249/59/268/291/251/252 | Supported | Operation 27 now decodes; standing sequence 808, walking sequence 819, all 467 textured faces, and all 18 cells validate in two byte-identical packaged-JAR renders. |
| Cellular-noise multipart | NPC 126 Otherworldly being; models 202/292/170/260; materials 268/252/256 | Supported | Operation 15 now decodes; standing sequence 808, walking sequence 819, all 550 textured faces, and all 18 cells validate in two byte-identical packaged-JAR renders. |
| Overlay-combined multipart | NPC 11 Tramp; eight component models; materials 314/228/313/258/277/254 | Supported | Color-output combine function 6 now decodes; standing sequence 808, walking sequence 819, all 907 textured faces, and all 18 cells validate in two byte-identical packaged-JAR renders. |
| Monochrome overlay | NPC 3124 Pyramid block; model 10817; materials 133/270 | Static render supported; automatic animations absent | Material 133 exercises monochrome combine function 6. Two packaged-JAR static renders were byte-identical with 4,239 visible pixels; the definition correctly remains in missing automatic animations. |
| Known difficult model | model 23905 | Unsupported model | RuneLite model decoder throws `BufferUnderflowException`. |
| Known difficult model | model 23889 | Unsupported model | RuneLite model decoder reports an invalid offset (`newPosition > limit`). |

The real Shark project, PNG, manifest, and comparison output were generated in
`/tmp/rsc-phase4-shark-a` and `/tmp/rsc-phase4-shark-b`; none is tracked by Git.
The compatibility probe reads only identifiers and decoded metadata. It does
not extract or redistribute cache payloads.

## End-to-end selector evidence

The external Shark project assigns sequence 10 at 0, 160, and 320 ms to the
shared standing/left-step/right-step rows and independently assigns the same
three source positions to the combat-side column. The saved project is loaded
by the same animation workspace, timeline renderer, shared viewport, textured
renderer, and `SheetExporter` used by the Swing selector. Its manifest records
all 18 sequence/frame/time traces, model and material IDs, mapping counts,
camera, lighting, palette, cache identity, and output hash.

NPC 40 has no decoded canonical standing or attack sequence in its BAS subset;
sequence 10 is therefore a compatibility animation chosen explicitly for all
three roles, not an automatic semantic claim. The selector leaves every pose
replaceable and persists the choices normally.

## Remaining limitations

- The procedural graph language is intentionally incomplete. Remaining
  combine function 8, curve interpolation modes 1 and 2, and one
  color-gradient sample-count variant fail
  closed. See `COMPATIBILITY_CENSUS.md` for exact current frequencies.
- Advanced type 1/2/3 mapping parameters are not decoded by the RuneLite model
  dependency; only the traced revision-software face-local behavior is used.
- The current rasterizer uses affine interpolation under the orthographic
  camera. It does not emulate perspective-correct texture sampling.
- Material scroll/effect bytes are decoded and diagnosed but animation of
  scrolling/effect materials is not implemented for static sprite export.
- Model 23905 and 23889 remain outside the licensed decoder's understood
  formats. There is no alternate-model fallback.
