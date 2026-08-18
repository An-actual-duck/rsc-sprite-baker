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
    @Test void consumesPinnedVariableSoundListsAndZeroPayloadOpcode16(){
        byte[] fixture={13,0,3,0,2,1,2,3,4,5,1,6,7,8,16,15,0};
        Sequence530 sequence=new Sequence530Decoder().decode(78,fixture);
        assertTrue(sequence.tween);assertTrue(sequence.opcode16);assertEquals(0,sequence.frameIds.length);
    }
    @Test void soundListTruncationFailsClosed(){
        assertThrows(IndexOutOfBoundsException.class,()->new Sequence530Decoder().decode(79,new byte[]{13,0,1,2,1,2,3,0}));
    }
}
