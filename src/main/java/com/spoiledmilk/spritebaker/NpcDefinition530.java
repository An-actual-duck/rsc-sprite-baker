package com.spoiledmilk.spritebaker;

import java.util.Arrays;

/** The Phase 1 subset of a revision-530 NPC definition. */
public final class NpcDefinition530 {
    public final int id;
    public String name = "null";
    public int[] modelIds = new int[0];
    public int standingAnimation = -1;
    public int walkingAnimation = -1;
    public int renderAnimation = -1;
    public short[] recolorFrom = new short[0];
    public short[] recolorTo = new short[0];
    public short[] retextureFrom = new short[0];
    public short[] retextureTo = new short[0];
    public byte[] recolorPaletteIndices = new byte[0];
    public int widthScale = 128;
    public int heightScale = 128;
    public int ambient;
    public int contrast;
    public boolean morphDefinition;

    public NpcDefinition530(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "NpcDefinition530{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", modelIds=" + Arrays.toString(modelIds) +
            ", standingAnimation=" + standingAnimation +
            ", walkingAnimation=" + walkingAnimation +
            ", renderAnimation=" + renderAnimation +
            ", widthScale=" + widthScale +
            ", heightScale=" + heightScale +
            '}';
    }
}
