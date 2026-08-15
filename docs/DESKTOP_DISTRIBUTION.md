# Zero-configuration desktop distribution

## End-user contract

The Linux and Windows archives each expand to one `RSC Sprite Baker` folder.
The user keeps that folder together and launches `Start RSC Sprite Baker.sh`
or `Start RSC Sprite Baker.cmd`; Java 11 or newer is the only prerequisite.
The launcher anchors all ordinary paths to its own folder, regardless of the
current working directory.

The default desktop does not ask for cache, project, or export locations. It
opens a persistent shell, reads the bundled `cache` directory through a
strictly read-only storage implementation, and presents the NPC browser. A
selected NPC opens as a temporary editing session with discovered movement
poses and conservative combat suggestions. Export writes a human-readable PNG
and provenance JSON to the adjacent `exports` directory. Existing output is
never replaced without a confirmation prompt.

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
