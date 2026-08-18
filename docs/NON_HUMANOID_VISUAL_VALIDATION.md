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
bounds, edge contact, occupancy, and exported PNG/provenance hashes.

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
cells, no cell-edge contact, at least two rendered movement poses in every
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
use a neutral-gray preview background for predominantly black creatures such
as King Black Dragon; the exported PNG remains transparent.

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
