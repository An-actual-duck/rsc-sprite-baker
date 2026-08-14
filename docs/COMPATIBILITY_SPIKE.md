# Phase 1 compatibility spike

## Result

The local 2009scape cache is compatible with a deliberately narrow hybrid
decoder. RuneLite 1.12.35 successfully reads the JS5 disk store and decodes one
selected old-format, untextured model. A strict local revision-530 adapter is
required for NPC and BAS metadata because RuneLite's current OSRS loaders do
not consume all legacy opcodes.

The positive target is NPC 72 (`Troll`):

- NPC location: index 18, archive 0, file 72
- component model: 3752 at index 7, archive 3752, file 0
- model format: RuneLite old format
- decoded geometry: 390 vertices, 739 faces, zero texture faces
- recolors/retextures: none
- width/height scale: 128/128
- render animation (BAS): 42
- resolved stand/walk animation IDs: 286/283
- applied pose: decoded base/static pose; animation transforms are Phase 2

The offscreen compatibility render uses a 256×256 orthographic auto-fit camera,
25° yaw, 15° pitch, 16-pixel padding, bottom-center grounding, two-sided
Lambertian lighting, and a transparent background. It is a diagnostic still,
not the eventual RSC visual preset.

## Cache identity

The inspected input is `/home/justin/2009scape/Server/data/cache`. It has the
JS5 `main_file_cache.dat2`, `main_file_cache.idx0` through `idx28`, and
`idx255` layout. RuneLite reports 29 indexes, predominantly index protocol 6.
The cache is associated with 2009scape's revision-530 data; JS5 stores per-index
revisions rather than one authoritative whole-cache revision number.

- `main_file_cache.dat2`: 91,702,293 bytes, SHA-256
  `b5431211b019b9403b4cfca933f4c9635c1d5278d3730995dced0d8672b1cc91`
- `main_file_cache.idx255`: 174 bytes, SHA-256
  `83a2292c515596af0423764c48e41dfe1aac482920dca0b89ecb343db6dd4c30`
- relevant index revisions: models/index 7 = 746; NPCs/index 18 = 198;
  configs/index 2 = 142

The generated diagnostic manifest records all 29 index protocols, revisions,
and archive counts for stronger identity.

## Reproduction

Use JDK 11 or newer. Output must be outside both the checkout and cache:

```bash
mvn test
mvn exec:java \
  -Dexec.args="--cache /home/justin/2009scape/Server/data/cache \
  --output-dir /tmp/rsc-sprite-baker-phase1 --npc 72"
```

Expected external files are `npc-72-static.png` and
`npc-72-diagnostic.json`. Two consecutive local runs produced PNG SHA-256
`b6d9ebd11c681dc61e40b5a5e4e063326a2e0071a0f7f2e57a178bf5c181e758`.
ImageIO/JVM changes are not yet claimed to preserve encoded PNG bytes; the
committed renderer test checks deterministic pixels.

## Incompatibilities and limits

- RuneLite's current `NpcLoader` does not safely consume revision-530 opcodes
  such as legacy recolor-palette opcode 42 and BAS opcode 127. The local adapter
  consumes known 530 metadata and fails with an exact offset on anything else.
- RuneLite's `ModelLoader` supports model 3752, but support across this cache is
  partial. Negative probes included chicken model 23905 (`BufferUnderflowException`)
  and cow model 23889 (invalid offset). No alternate or unlicensed decoder is
  silently substituted.
- Textured faces are rejected. Texture payloads are neither read nor rendered.
- The spike records animation IDs but does not decode or apply frames,
  framemaps/skeletons, or vertex groups.
- NPC morph resolution and opcode-42 palette substitution are not implemented.
- The renderer is a small diagnostic software rasterizer. It does not implement
  the full client rasterizer's priorities, clipping, texture mapping, or exact
  lighting semantics.

No cache bytes, extracted models, textures, animations, PNGs, or other derived
game assets are committed. Tests construct neutral metadata and geometry in
memory.
