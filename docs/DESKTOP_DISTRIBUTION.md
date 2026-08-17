# Zero-configuration desktop distribution

## End-user contract

The Linux and Windows archives each expand to one `RSC Sprite Baker` folder.
The user keeps that folder together and launches `Start RSC Sprite Baker.sh`
or `Start RSC Sprite Baker.cmd`; Java 11 or newer is the only prerequisite.
The launcher anchors all ordinary paths to its own folder, regardless of the
current working directory.

The default desktop does not ask for cache, project, or export locations. It
opens directly to the NPC browser and reads the bundled `cache` directory
through a strictly read-only storage implementation. A selected NPC opens as a
temporary editing session with discovered movement poses and conservative
combat suggestions. Export writes a human-readable PNG and provenance JSON to
the adjacent `exports` directory. Existing output is never replaced without a
confirmation prompt. Closing a temporary NPC editor returns directly to the
NPC browser; closing the browser exits the application. There is no
intermediate landing screen in the ordinary workflow. A startup failure is
reported directly and then exits.

Temporary adjustments are considered safe to discard after a successful
export. Closing or switching away before export prompts the user. Ordinary
desktop sessions do not create project JSON files or machine-specific
preferences.

## Advanced interfaces retained

Zero-configuration is the default, not a removal of the technical workflows.
The shaded JAR still contains `SelectorMain`, `HeadlessMain`, `Main`, and the
terminal-only `CompatibilityCensusMain` broad-cache scanner.
Developers can use the documented argument-driven portable-project selector,
single-project headless export, deterministic batch export, validation, and
dry-run interfaces. Those interfaces continue to accept explicit cache,
project, and output paths and are intentionally outside the ordinary desktop
UI. See [DESKTOP_APPLICATION.md](DESKTOP_APPLICATION.md) and
[BATCH_HANDOFF.md](BATCH_HANDOFF.md).

## Licensed-cache build

Build both archives from the Sprite Baker checkout with:

```bash
./scripts/build-distributions.sh 0.1.0
```

The build reads only `Server/data/cache` in the sibling 2009Scape source
checkout. It refuses to package an incomplete cache, tracked or staged cache
changes, files whose hydrated bytes do not match the exact revision's Git LFS
object ID and size, differing repository/server license copies, or an
unrecognized source layout. It never copies a cache or generated asset into
this Git repository; archives are build output under `target/distributions`.

Each archive contains:

- the dependency-bundled Sprite Baker JAR and one platform launcher;
- all 31 read-only JS5 files (`dat2`, `idx0` through `idx28`, and `idx255`);
- an initially empty writable `exports` directory;
- the complete 2009Scape AGPL-3.0 license text;
- the cache asset notice, exact source revision/remotes, and cache SHA-256
  inventory; and
- Sprite Baker's Java dependency notices.

The cache notice records the distribution's reviewed AGPL-3.0 conveyance
basis. Licensing and provenance remain explicit release gates: a publisher
must review that notice and the recorded source revision before publishing an
archive. An emulator source license must not be assumed to cover a different
cache or a cache obtained from another source.

## Terminal-only inspection

`build-distributions.sh` runs the full Maven suite headlessly, assembles fresh
archives in a private temporary directory, and calls:

```bash
./scripts/inspect-distributions.sh \
  target/distributions/rsc-sprite-baker-0.1.0-linux.tar.gz \
  target/distributions/rsc-sprite-baker-0.1.0-windows.zip
```

Inspection rejects unsafe, duplicate, case-colliding, or symbolic-link archive
paths; checks the exact platform launchers, empty exports folder, cache count,
license/provenance files, cache checksums, and required desktop/advanced JAR
classes; and verifies that the Linux cache is not writable. The build prints
final SHA-256 hashes and sizes only after inspection passes.

The archives intentionally do not bundle a Java runtime. No cache, archive,
or exported derivative is committed to this repository.

## 2026-08-14 terminal evidence

Two consecutive `0.1.0` builds from Sprite Baker checkpoint `608352c`, using
`SOURCE_DATE_EPOCH=1786680000`, and
2009Scape cache source revision
`b39b75e959bc68d54bf99392c22e85ef71273b84` produced identical inspected
archives:

- Linux tar.gz: `bc41011f934663311fba5022ab3f77b1ffc490e63ae7c70f59c59ecf77daeaeb`
  (74 MiB displayed size);
- Windows zip: `d90b8330a914a436879c46108b7192dc39e8beb2d59a5cedd7bf1da4fd96a9d6`
  (74 MiB displayed size).

The full Java 21 suite passed headlessly with 58 tests. The Linux launcher was
extracted, invoked from `/tmp` with AWT forced headless, reached the packaged
desktop entry point, reported the expected graphical-environment diagnostic,
and exited with status 2 without creating an export. This checks executable
packaging without opening a GUI.

A terminal-only NPC-72 static render then exercised the packaged read-only
cache adapter against the real cache. Before and after hashes were identical:

- `main_file_cache.dat2`:
  `b5431211b019b9403b4cfca933f4c9635c1d5278d3730995dced0d8672b1cc91`;
- `main_file_cache.idx255`:
  `83a2292c515596af0423764c48e41dfe1aac482920dca0b89ecb343db6dd4c30`.

The diagnostic PNG and JSON stayed in a private `/tmp` directory. No GUI was
opened, no desktop input was synthesized, and no cache or derived asset was
added to Git.

The final packaged Linux workflow was then exercised interactively by the
user. Launch opened directly to the NPC browser with the intended streamlined
load flow and no intermediate landing screen. NPC 72 loaded successfully;
visual/pose edits and export behaved as intended; and closing the browser
exited the application. This interactive validation was performed by the user,
not by an automated desktop session.

Compatibility breadth remains a known risk: during the same review the user
reported that other attempted NPCs did not load. NPC 72 demonstrates the basic
zero-configuration workflow, but this handoff does not claim that every cache
NPC is supported. Existing fail-closed model, texture, and animation limits
still apply and should be reviewed per NPC before publishing derived output.

## 2026-08-15 compatibility build inspection

The broad-cache compatibility build passed Java 21 `mvn clean verify` with 62
tests. With `SOURCE_DATE_EPOCH=1786752589`, the provenance-checked distribution
builder copied the 31 licensed-cache files from the recorded sibling checkout
into private staging, and the terminal inspector accepted launcher separation,
empty exports directories, cache checksums and permissions, licenses/notices,
safe archive paths, and all desktop/advanced entry points including
`CompatibilityCensusMain`.

- shaded JAR SHA-256: `8222938a4c4fbb28273be930d29e31dbc911ca4cfb7a540e781939b6dfb912fa`
- Linux archive SHA-256: `18feece17a1e602890e344e41ebd37d38a717d57851f946580866935793ad371`
  (76,980,696 bytes)
- Windows archive SHA-256: `204bb8b638a5430e530c74da89af4c793fb4b5e6c33d860448e4f914e1f3fe4e`
  (76,981,553 bytes)

The packaged census entry point then reproduced the byte-identical report hash
recorded in `COMPATIBILITY_CENSUS.md`. Cache dat2 and idx255 hashes were
unchanged after the audit.

## 2026-08-16 combine-2/5 and operations-22/39 inspection

The complete Java 21 build passed 136 tests. With
`SOURCE_DATE_EPOCH=1786752589`, the licensed-cache distribution builder and
terminal inspector again accepted both archives: launcher separation, empty
exports directories, all 31 cache files and checksums, read-only Linux cache,
licenses/provenance, safe paths, and desktop plus advanced entry points. No GUI
was launched or automated.

- shaded JAR SHA-256: `b1413400b8e8edcc1f27af573e88b21607f693bd55712c029302a96a263ac8b3`
- Linux archive SHA-256: `d521cda916b553cbf8fcece8c0b7528ad3e87867d7932b17a5a3b444dd5c09c3`
  (77,006,194 bytes)
- Windows archive SHA-256: `d160cc093deea285798243ed8d7bc3cc137d18f61a31e77a54a63e6398bbde78`
  (77,006,858 bytes)

Two packaged-JAR full-cache censuses were byte-identical at the hash recorded
in `COMPATIBILITY_CENSUS.md`. Two validation-only packaged renders each for
NPC 284 and NPC 146 were also byte-identical. The cache dat2 and idx255 hashes
remained unchanged, and no cache or rendered derivative entered the repository.
