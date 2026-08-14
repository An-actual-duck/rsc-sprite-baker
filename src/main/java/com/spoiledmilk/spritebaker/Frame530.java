package com.spoiledmilk.spritebaker;

public final class Frame530 {
    public final int id;
    public final Framemap530 framemap;
    public final int[] slots, x, y, z, flags;
    public final int[] pivotSlots;

    Frame530(int id, Framemap530 framemap, int[] slots, int[] x, int[] y, int[] z,
             int[] flags, int[] pivotSlots) {
        this.id=id; this.framemap=framemap; this.slots=slots; this.x=x; this.y=y; this.z=z;
        this.flags=flags; this.pivotSlots=pivotSlots;
    }
}
