RSC Sprite Baker — Quick Start
==============================

Linux
-----
Run `Start RSC Sprite Baker.sh` from this folder.

Windows
-------
Run `Start RSC Sprite Baker.cmd` from this folder.

Java 11 or newer must be installed. Leave this folder together and launch it
with the platform file above; do not run the JAR directly. The application
automatically uses the bundled read-only `cache` directory. Browse for an NPC,
customize its poses and visual settings, then choose Export. PNG sheets and
provenance manifests are written to this folder's `exports` directory
automatically. A normal launch opens directly to the NPC browser.

Ordinary desktop work is temporary: changing NPCs or closing the editor may
discard adjustments that have not been exported. The application warns before
discarding changed work.

Advanced automation and portable-project workflows remain available through
the documented command-line entry points in the source repository. They are
not required for ordinary desktop use:
https://github.com/An-actual-duck/rsc-sprite-baker

Licensing and source information for the bundled cache and minimal derived
combat-role metadata is in `licenses`.
Third-party Java dependency notices are in `THIRD_PARTY_NOTICES.md`.
