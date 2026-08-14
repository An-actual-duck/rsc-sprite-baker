package com.spoiledmilk.spritebaker;

import java.util.Arrays;

public final class Frame530Decoder {
    public Frame530 decode(int id, Framemap530 map, byte[] data) {
        BinaryInput flagsIn = new BinaryInput(data);
        int mapId = flagsIn.u16();
        if (mapId != map.id) throw new IllegalArgumentException("frame framemap id mismatch");
        int slotCount = flagsIn.u8();
        if (slotCount > map.types.length) throw new IllegalArgumentException("frame uses more slots than its framemap");
        BinaryInput values = new BinaryInput(data, 3 + slotCount);
        int[] slots=new int[slotCount*2], xs=new int[slotCount*2], ys=new int[slotCount*2];
        int[] zs=new int[slotCount*2], high=new int[slotCount*2], pivots=new int[slotCount*2];
        int size=0, last=-1;
        Arrays.fill(pivots, -1);
        for (int slot=0; slot<slotCount; slot++) {
            int bits=flagsIn.u8();
            if (bits == 0) continue;
            int pivot=-1;
            if (map.types[slot] != 0) {
                for (int prior=slot-1; prior>last; prior--) if (map.types[prior] == 0) {
                    slots[size]=prior; xs[size]=ys[size]=zs[size]=0; high[size]=0; pivots[size]=-1;
                    pivot=size++; break;
                }
            }
            slots[size]=slot;
            int defaultValue=map.types[slot] == 3 ? 128 : 0;
            xs[size]=(bits&1)!=0 ? values.signedShortSmart() : defaultValue;
            ys[size]=(bits&2)!=0 ? values.signedShortSmart() : defaultValue;
            zs[size]=(bits&4)!=0 ? values.signedShortSmart() : defaultValue;
            if (map.types[slot] == 2) {
                xs[size]=rotation(xs[size]); ys[size]=rotation(ys[size]); zs[size]=rotation(zs[size]);
            }
            high[size]=(bits >>> 3) & 3; pivots[size]=pivot; last=slot; size++;
        }
        if (values.remaining()!=0) throw new IllegalArgumentException("frame " + id + " streams do not consume input");
        return new Frame530(id,map,Arrays.copyOf(slots,size),Arrays.copyOf(xs,size),Arrays.copyOf(ys,size),
            Arrays.copyOf(zs,size),Arrays.copyOf(high,size),Arrays.copyOf(pivots,size));
    }

    private static int rotation(int value) { return ((value & 255) << 3) + ((value >>> 8) & 7); }
}
