package com.spoiledmilk.spritebaker;

import java.util.List;

/** Stable broad-cache review set. Entries are validation coverage, never render overrides. */
public final class NonHumanoidVisualMatrix {
    private NonHumanoidVisualMatrix(){}

    public static final List<Entry> ENTRIES = List.of(
        entry(50, "King Black Dragon", "dragon", "large-boss", "multipart", "textured"),
        entry(53, "Red dragon", "dragon"),
        entry(3068, "Skeletal Wyvern", "dragon", "slayer", "unusual"),
        entry(3808, "Tortoise", "quadruped", "multipart", "textured"),
        entry(3340, "Giant Mole", "quadruped", "large-boss"),
        entry(8133, "Corporeal Beast", "quadruped", "large-boss"),
        entry(61, "Spider", "arachnid"),
        entry(107, "Scorpion", "arachnid"),
        entry(1158, "Kalphite Queen", "insect", "flying", "large-boss", "multipart"),
        entry(4347, "Giant mosquito", "insect", "flying"),
        entry(3484, "Big Snake", "serpentine", "unusual"),
        entry(3943, "Giant Sea Snake", "serpentine", "aquatic", "unusual"),
        entry(3612, "Giant snail", "amorphous", "unusual"),
        entry(3200, "Chaos Elemental", "amorphous", "large-boss", "unusual"),
        entry(78, "Giant bat", "flying"),
        entry(3675, "Vulture", "flying", "textured"),
        entry(6222, "Kree'arra", "flying", "large-boss", "multipart", "textured"),
        entry(40, "Shark", "aquatic"),
        entry(1637, "Jelly", "aquatic", "amorphous", "slayer"),
        entry(1693, "Giant lobster", "aquatic"),
        entry(2745, "TzTok-Jad", "large-boss", "unusual"),
        entry(6260, "General Graardor", "large-boss", "multipart", "textured"),
        entry(5247, "Penance Queen", "large-boss", "multipart", "textured", "unusual"),
        entry(1608, "Kurask", "slayer"),
        entry(1610, "Gargoyle", "slayer", "flying", "textured"),
        entry(1615, "Abyssal demon", "slayer"),
        entry(2783, "Dark beast", "slayer", "quadruped"),
        entry(4353, "Cave horror", "slayer", "multipart"),
        entry(8349, "Tormented demon", "slayer", "textured")
    );

    private static Entry entry(int id, String name, String... families) {
        return new Entry(id, name, List.of(families));
    }

    public static final class Entry {
        public final int npcId;
        public final String expectedName;
        public final List<String> families;

        Entry(int npcId, String expectedName, List<String> families) {
            this.npcId = npcId;
            this.expectedName = expectedName;
            this.families = families;
        }
    }
}
