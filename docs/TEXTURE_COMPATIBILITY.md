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
gradient (2/3), multiply combine (7 function 3), linear curve parsing (8),
custom sampled color gradient (10 preset 0), hash noise (13), and range (30). Texture generation
uses the software client's 64/128 material-size flag and horizontal order. It
uses a fixed, manifest-recorded gamma of 1.0 instead of the source client's
preference-dependent and randomly perturbed brightness value.
Operation 13 is the client's zero-child monochrome hash-noise node and has no
serialized parameters. For every texel, it hashes the 12-bit X/Y fractions
with Java `int` overflow, masks the polynomial result to a non-negative integer,
scales it, and applies Java signed remainder `% 4096`. Unexpected parameters
fail closed. The primary trace is
[`TextureOpNoise.java`](https://github.com/conan513/2009scape-client/blob/a569f0af7754ada96ed7ac76d7582b2c7511b7a0/client/src/main/java/rt4/TextureOpNoise.java).
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
  emboss, line-noise, and other unverified operations remain
  unsupported. See `COMPATIBILITY_CENSUS.md` for exact current frequencies.
- Advanced type 1/2/3 mapping parameters are not decoded by the RuneLite model
  dependency; only the traced revision-software face-local behavior is used.
- The current rasterizer uses affine interpolation under the orthographic
  camera. It does not emulate perspective-correct texture sampling.
- Material scroll/effect bytes are decoded and diagnosed but animation of
  scrolling/effect materials is not implemented for static sprite export.
- Model 23905 and 23889 remain outside the licensed decoder's understood
  formats. There is no alternate-model fallback.
