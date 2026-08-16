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
gradient (2/3), randomized tiles (4), box blur (5), multiply/overlay combine
(7 functions 3/6), linear curve parsing (8), custom sampled color gradient (10 preset
0), hash noise (13), cellular distance noise (15), stripes (27), range (30),
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
Combine operation 7 has two child inputs and defaults to overlay function 6.
Parameter code 0 is the unsigned function ID; code 1 selects monochrome output
only when its unsigned byte equals 1. Function 6 treats child 1 as the overlay
control: values below 2048 produce `child1 * child0 >> 11`; values at or above
2048 produce `4096 - ((4096 - child0) * (4096 - child1) >> 11)`. Color mode
applies that branch independently to RGB. Monochrome mode reads the first
channel of each child and repeats the result across RGB. Function 3 retains its
`child1 * child0 >> 12` multiply behavior in both output modes. Calculations use
Java signed `int` overflow and have no node-level clamp; only the existing final
texture conversion clamps channels to 0..255. Every other function ID remains
an explicit unsupported-material error. The primary trace is
[`TextureOpCombine.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOpCombine.java).
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
| Recolored/retextured multipart | NPC 0 Hans; six component models; five recolors | Model assembly/recolor metadata supported; materials unsupported | Retextured material IDs 228/292/258/257/262/527/272/254 resolve nested textures and report any later unsupported graph operation. No substitution occurs. |
| Alpha/mapping stress | NPC 61 Spider; model 24613; material 111 | Supported | Operation 34 now decodes; models, material, standing sequence 6247, and walking sequence 6248 validate. Its 298 textured faces continue to use the documented advanced-mapping fallback. |
| Hash-noise multipart | NPC 125 Ice warrior; seven component models; materials 249/291/303/302 | Supported | Operation 13 now decodes; standing sequence 842, walking sequence 841, and all 1,076 textured faces validate. |
| Line-noise animated | NPC 131 Penguin; model 21547; materials 182/347/171 | Supported | Operation 38 now decodes; standing sequence 5668, walking sequence 5666, and all 391 textured faces validate in a packaged 18-cell render. |
| Bump-lit animated | NPC 1013 Swamp toad; model 3447; material 318 | Supported | Operation 32 now decodes; standing sequence 1018, walking sequence 1021, and all 155 textured faces validate in a packaged 18-cell render. |
| Box-blurred animated | NPC 78 Giant bat; model 18898; materials 185/59 | Supported | Operation 5 now decodes; standing sequence 4914, walking sequence 4913, and all 524 textured faces validate in a packaged 18-cell render. |
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

- The procedural graph language is intentionally incomplete. Sprite-backed,
  emboss, and other unverified operations remain
  unsupported. See `COMPATIBILITY_CENSUS.md` for exact current frequencies.
- Advanced type 1/2/3 mapping parameters are not decoded by the RuneLite model
  dependency; only the traced revision-software face-local behavior is used.
- The current rasterizer uses affine interpolation under the orthographic
  camera. It does not emulate perspective-correct texture sampling.
- Material scroll/effect bytes are decoded and diagnosed but animation of
  scrolling/effect materials is not implemented for static sprite export.
- Model 23905 and 23889 remain outside the licensed decoder's understood
  formats. There is no alternate-model fallback.
