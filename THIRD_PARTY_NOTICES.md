# Third-party notices

## RuneLite cache module

This project depends on `net.runelite:cache:1.12.35` for JS5 store/archive and
model decoding. RuneLite is distributed under the BSD 2-Clause License.

- Source: <https://github.com/runelite/runelite/tree/master/cache>
- License: <https://github.com/runelite/runelite/blob/master/LICENSE>

The dependency is resolved from RuneLite's Maven repository and is not vendored
into this repository.

Phase 4 continues to use RuneLite's decoded model texture-coordinate fields and
BSD-licensed type-0 UV calculation semantics. The revision-530 index-26 table
and bounded index-9 procedural graph adapters are original code in this
repository. No decompiled-client or third-party procedural renderer is copied,
vendored, or linked.
