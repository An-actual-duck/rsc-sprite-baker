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
confirmation prompt. Closing the initial browser reveals a small fallback
shell so the process never becomes an ownerless background application.
Closing a temporary NPC editor returns directly to the NPC browser.

Temporary adjustments are considered safe to discard after a successful
export. Closing or switching away before export prompts the user. Ordinary
desktop sessions do not create project JSON files or machine-specific
preferences.

## Advanced interfaces retained

Zero-configuration is the default, not a removal of the technical workflows.
The shaded JAR still contains `SelectorMain`, `HeadlessMain`, and `Main`.
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

Two consecutive `0.1.0` builds from Sprite Baker checkpoint `0b79722` and
2009Scape cache source revision
`b39b75e959bc68d54bf99392c22e85ef71273b84` produced identical inspected
archives:

- Linux tar.gz: `a72de47ecdbf65fbfe4ee4511b3160c8b5d0f962a10000f27f8fc41f14a794c2`
  (74 MiB displayed size);
- Windows zip: `45ee1b22e2d70f098c7ad471ab22da5736bba5ecb41d131b31a614f55c5d857a`
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
