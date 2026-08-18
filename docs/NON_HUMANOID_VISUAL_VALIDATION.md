# Non-humanoid visual validation

`NonHumanoidVisualAuditMain` is a deterministic, terminal-only quality gate for
creature body types that are poorly represented by humanoid-only tests. It uses
the same zero-configuration animation discovery, automatic pose selection,
shared framing, textured renderer, palette reduction, exporter, and provenance
writer as the desktop. It never opens a window and contains no NPC-specific
render settings.

The stable 29-definition matrix covers dragons, quadrupeds, arachnids,
insects, serpentine and unusual proportions, flying and aquatic creatures,
amorphous bodies, large bosses, multipart/textured monsters, and Slayer-style
monsters. For every NPC the audit records the cache identity, component model
IDs, automatic sequence choices, all 18 pose selections, material diagnostics,
direction yaw, per-cell ARGB hash, visible/translucent pixel counts, alpha
bounds, exact-black count, distinct RGB count, average RGB, edge contact,
occupancy, and exported PNG/provenance hashes.

Run it from a built checkout with output outside both Git and the cache:

```bash
java -cp target/rsc-sprite-baker.jar \
  com.spoiledmilk.spritebaker.NonHumanoidVisualAuditMain \
  --cache /path/to/user-supplied/cache \
  --output /tmp/non-humanoid-visual-audit.json \
  --exports /tmp/non-humanoid-visual-exports
```

The entry point fails individual definitions closed in the report. A clean
entry requires all 18 cells, a decodable 768×384 transparent export, non-empty
cells, no cell with more than 75% exact-black visible pixels, no cell-edge
contact, at least two rendered movement poses in every
direction, at least three distinct standing directions, distinct detected
combat poses, and no material diagnostic error. Hashes and occupancy remain
available for review even when a threshold is flagged. Cache data and generated
sprites must stay outside this repository.

## 2026-08-18 baseline and correction

The pinned cache identity was:

- `main_file_cache.dat2` SHA-256
  `b5431211b019b9403b4cfca933f4c9635c1d5278d3730995dced0d8672b1cc91`
- `main_file_cache.idx255` SHA-256
  `83a2292c515596af0423764c48e41dfe1aac482920dca0b89ecb343db6dd4c30`

The first run passed 28 of 29 entries. NPC 40, Shark, accurately decodes a
walking sequence (10) but has no separate standing sequence. The desktop's
automatic setup consequently filled only 12 of 18 cells. The systemic
correction uses cycle zero of a discovered walking animation as the visible
rest pose only when standing metadata is absent. It does not assign or invent a
standing sequence ID. Standing-only creatures retain their standing pose for
the movement rows, while definitions missing both sources still fail closed.

After the correction all 29 entries and all 522 cells pass. Every movement
direction has three different rendered pose hashes, every entry has five
different standing-direction hashes, detected combat columns have three
different hashes, no cell is empty, no rendered pixel touches a cell edge, and
all material diagnostics are clean. Nine entries exercise translucent output.
Measured visible occupancy spans 10.9–78.1% of cell width and 3.9–78.9% of cell
height. The low end is expected for thin, long silhouettes under the existing
shared origin/scale policy—notably Spider and Big Snake—and is retained for the
later aesthetic decision rather than changing the RSC style in this pass.

Shark has no compatible combat candidate. Its combat-side column deliberately
uses the three movement recommendations and is reported as
`movementCombatFallback`; it is not presented as discovered combat metadata.
This is the only matrix fallback. The terminal review found no justification
for an NPC-specific renderer, camera, scale, animation, or material override.

Two complete runs from the shaded JAR were byte-identical at report SHA-256
`4d123ac92d60fe630160c269195f38057c6a490a4c0bf7402a28ffb19664dec7`.
The exhaustive 8,590-definition census remained unchanged at SHA-256
`360ab988150e65c42cadc1dc46f7fbd480e0b7b8413d8595a0c16f4fe0d04e10`:
6,926 ready, 1,051 missing automatic animations, 612 morph/internal, one
unsupported material, and zero model or other failures. Java 21 clean verify
passes all 253 tests.

The licensed-cache distribution builder and terminal inspector accepted both
platform archives, including exact cache identities, read-only cache contents,
empty adjacent exports directories, launcher separation, license/source
records, and application-JAR contents. The shaded JAR SHA-256 is
`618d165c02561094dfbe6ca9d8a743620f98e03e5b17f07331b172e1455aa08c`.
The Linux archive is 77,108,164 bytes at SHA-256
`b50bd1e55d43c3734996bee891950f9123d354feee82571071afecf4bdddbd41`;
the Windows archive is 77,108,958 bytes at SHA-256
`9ac3e69d8ea95a7dbde5dbb400f3d95c1a0ad269f727961a1f670749b8fed302`.
All reports, archives, and rendered derivatives remain outside Git.

## 2026-08-18 original-color correction

NPC 1615's single model 5062 has 488 faces, of which 480 are textured through
materials 59, 283, 310, and 318. Its definition recolors packed HSL 4015 to
528; 254 faces use the recolored value and 198 use packed HSL 16. There is no
NPC retexture. The pre-change unmodified raster retained 171 colors and an RGB
average of 24/17/17, but the zero-configuration 5-level cube plus dithering
collapsed 1,796 of 2,008 visible pixels to exact black. This established that
the failure was after model decode, recolor, material resolution, lighting,
and rasterization rather than missing cache color.

The systemic correction uses the pinned revision-530 packed-HSL palette
conversion at deterministic default brightness 0.7 and makes unmodified RGB
the zero-configuration default. Optional RSC cube presets remain available for
deliberate advanced styling. A representative side render of NPC 1615 now has
2,008 visible pixels, zero exact black, 370 distinct colors, and average RGB
44/32/31. Equivalent cache-backed samples of King Black Dragon, Red dragon,
Kurask, Gargoyle, Nechryael, Bloodveld, Dark beast, and Tormented demon also
retain non-black color.

The strengthened 29-NPC matrix passed all 522 cells under `Original colors`.
NPC 1615's complete sheet contains 36,907 visible pixels, zero exact black,
and at least 319 distinct RGB values in every cell. The external matrix report
SHA-256 is
`026e75033612102ced926f6540e5dbe3205f22ad6b2c398c3b9b2a3e7f44ba8f`;
the NPC-1615 PNG is 76,545 bytes at SHA-256
`f3dd808338ad14be265ae86fb60d0c29e699177c2d98c7c7761cb6490da786a0`.
The focused neutral regression also proves the 4015-to-528 recolor remains
chromatic through preview-only compositing and a transparent PNG round trip.
No NPC-specific recolor, material, camera, or renderer override was added.

## Suggested visual browse list

The complete matrix is source-controlled in `NonHumanoidVisualMatrix`. A
compact high-value manual browse list is:

- 50 King Black Dragon, 53 Red dragon, 3068 Skeletal Wyvern
- 3808 Tortoise, 3340 Giant Mole, 8133 Corporeal Beast
- 61 Spider, 107 Scorpion, 1158 Kalphite Queen, 4347 Giant mosquito
- 3484 Big Snake, 3943 Giant Sea Snake, 3612 Giant snail, 3200 Chaos Elemental
- 78 Giant bat, 3675 Vulture, 6222 Kree'arra
- 40 Shark, 1637 Jelly, 1693 Giant lobster
- 2745 TzTok-Jad, 6260 General Graardor, 5247 Penance Queen
- 1608 Kurask, 1610 Gargoyle, 1615 Abyssal demon, 2783 Dark beast,
  4353 Cave horror, 8349 Tormented demon

Especially unusual first checks are Tortoise (six multipart models and 21
materials), Kree'arra (winged multipart boss), Big Snake (very long/thin
silhouette), Jelly (amorphous textured body), Penance Queen (eight component
models), and Shark (walking-only automatic metadata). Manual inspection should
use a neutral-gray preview background for intentionally dark creatures such as
King Black Dragon; the exported PNG remains transparent.

## Limits

- Numeric direction hashes prove that the prescribed yaws render different
  results; final semantic/artistic judgment of “facing” remains a manual visual
  review.
- Combat discovery is a bounded motion heuristic. Movement fallback remains
  explicit when no compatible candidate exists.
- Shared origin-centered framing intentionally prevents per-frame jitter. Very
  thin or strongly elongated bodies can therefore occupy less cell area than a
  humanoid. This pass records that behavior and does not redesign it.
- The matrix is a regression cross-section, not a claim that all 6,926 ready
  definitions received human visual review.
