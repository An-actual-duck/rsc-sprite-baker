# Revision-530 combat-animation discovery audit

This audit used `/home/justin/2009scape/Server/data/cache` strictly as a
read-only compatibility input. No cache bytes, extracted frames, models,
textures, rendered images, or derivative assets are stored in this repository.

Cache identity:

- `main_file_cache.dat2`: 91,702,293 bytes; SHA-256
  `b5431211b019b9403b4cfca933f4c9635c1d5278d3730995dced0d8672b1cc91`
- `main_file_cache.idx255`: 174 bytes; SHA-256
  `83a2292c515596af0423764c48e41dfe1aac482920dca0b89ecb343db6dd4c30`

The deterministic motion analysis used untweened encoded keyframes, normalized
NPC width/height scale, a 90-degree side yaw, and a 15-degree projection pitch.
It records identifiers and numeric diagnostics only; renderer lighting,
materials, camera framing, and raster output do not participate.

## Filters that hid attacks

The prior path searched only `standing/walking ±16`, required 3–24 encoded
frames, required every frame to use one of the exact locomotion framemap IDs,
required a strict departure and return, silently discarded every load or
classification failure, retained only the top 12, and passed only one selected
sequence to the Combat browser. Thus authoritative attacks outside the narrow
ID window, attacks longer than 24 frames, and credible role-linked attacks that
were unsuitable for automatic three-pose selection were invisible. Candidate
deduplication and rejection provenance were absent, and browsing another
sequence replaced rather than supplemented the Combat timeline.

## Identifier-only compatibility evidence

| NPC | BAS | Standing / walking | Browseable combat sequences | Diagnostic evidence |
|---|---:|---:|---|---|
| 8349 Tormented demon | 910 | 10921 / 10920 | 10922 melee; 10918 magic; 10919 ranged | 10919 has 28 frames and 95% post-peak recovery, so the old 24-frame ceiling hid it; it remains browseable but is excluded from automatic ranking. |
| 6605 Revenant goblin | 567 | 7451 / 7452 | 7449 melee; 7499 magic; 7513 ranged | All three pass strict motion analysis; 7499 and 7513 lie outside the old ±16 proximity window. |
| 3068 Skeletal Wyvern | 645 | 2984 / 2982 | 2985 melee/magic; 2989 ranged | Both role relationships are skeleton-compatible and pass strict motion analysis. |
| 4397 Catablepon | 461 | 4269 / 4268 | 4271 melee; 4272 magic | Both distinct attacks pass strict motion analysis and remain independently browseable. |
| 4972 Giant Roc | 924 | 5021 / 5022 | 5024 melee; 5025 ranged | Both distinct attacks pass; ranged sequence 5025 records 79% recovery. |
| 1183 Elf warrior | 119 | 813 / 819 | 428 melee; 426 ranged | Both authoritative relationships are hundreds of IDs from locomotion and were unreachable by proximity discovery. |
| 13 Wizard | 4 | 808 / 819 | 711 range/magic | Sequence 711 passes strict analysis but is 97 IDs from standing; the incompatible melee relationship 2791 is rejected with a framemap reason. |

These relationships come from the adjacent 2009scape NPC combat configuration
and are validated against the selected revision-530 cache. They are optional:
a standalone cache without adjacent metadata uses the bounded related-group
compatibility fallback and does not depend on the 2009scape source tree.
