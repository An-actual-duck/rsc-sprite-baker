package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class Sequence530DecoderTest {
    @Test void decodesRevision530TimelineAndTweenFlag(){
        byte[] fixture={1,0,2,0,3,0,5,0,1,0,2,0,10,0,10,2,0,2,15,0};
        Sequence530 sequence=new Sequence530Decoder().decode(77,fixture);
        assertArrayEquals(new int[]{3,5},sequence.durations);
        assertArrayEquals(new int[]{(10<<16)|1,(10<<16)|2},sequence.frameIds);
        assertEquals(2,sequence.loopOffset);assertTrue(sequence.tween);assertEquals(160,sequence.totalMillis());
    }
    @Test void rejectsUnknownOpcode(){assertThrows(IllegalArgumentException.class,()->new Sequence530Decoder().decode(1,new byte[]{99,0}));}
}
