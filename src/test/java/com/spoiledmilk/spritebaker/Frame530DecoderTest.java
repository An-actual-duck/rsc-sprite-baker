package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class Frame530DecoderTest {
    @Test void decodesSplitStreamsImplicitPivotAnd530Rotations(){
        Framemap530 map=new Framemap530(12,new int[]{0,1,2},new boolean[3],new int[]{65535,65535,65535},new int[][]{{0},{0},{0}});
        byte[] bytes={0,12,3,0,1,7,74,65,66,67};
        Frame530 frame=new Frame530Decoder().decode(99,map,bytes);
        assertArrayEquals(new int[]{0,1,2},frame.slots);assertArrayEquals(new int[]{0,10,8},frame.x);
        assertEquals(16,frame.y[2]);assertEquals(24,frame.z[2]);
    }
    @Test void framemapIncludes530BooleanAndMasks(){
        byte[] bytes={2,0,3,1,0,(byte)0xff,(byte)0xff,0,7,1,2,4,5,6};
        Framemap530 map=new Framemap530Decoder().decode(4,bytes);
        assertArrayEquals(new int[]{0,3},map.types);assertTrue(map.interpolated[0]);assertEquals(65535,map.masks[0]);assertArrayEquals(new int[]{5,6},map.groups[1]);
    }
}
