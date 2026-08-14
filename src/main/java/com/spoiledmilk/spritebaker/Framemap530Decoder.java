package com.spoiledmilk.spritebaker;

public final class Framemap530Decoder {
    public Framemap530 decode(int id, byte[] data) {
        BinaryInput in = new BinaryInput(data);
        int count = in.u8();
        int[] types = new int[count];
        boolean[] interpolated = new boolean[count];
        int[] masks = new int[count];
        int[][] groups = new int[count][];
        for (int i = 0; i < count; i++) types[i] = in.u8();
        for (int i = 0; i < count; i++) interpolated[i] = in.u8() == 1;
        for (int i = 0; i < count; i++) masks[i] = in.u16();
        for (int i = 0; i < count; i++) groups[i] = new int[in.u8()];
        for (int i = 0; i < count; i++) for (int j = 0; j < groups[i].length; j++) groups[i][j] = in.u8();
        if (in.remaining() != 0) throw new IllegalArgumentException("framemap " + id + " has trailing bytes");
        return new Framemap530(id, types, interpolated, masks, groups);
    }
}
