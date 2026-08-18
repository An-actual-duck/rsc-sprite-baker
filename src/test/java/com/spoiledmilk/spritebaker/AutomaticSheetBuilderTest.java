package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AutomaticSheetBuilderTest {
    @Test void walkingOnlyCreaturesUseWalkingStartAsRestWithoutInventingMetadata(){
        Sequence530 walking=sequence(10);
        PoseSelection[] poses=AutomaticSheetBuilder.movementSuggestions(null,walking);
        assertAll(
            ()->assertEquals(10,poses[0].sequenceId),
            ()->assertEquals(0,poses[0].frameIndex),
            ()->assertEquals(0,poses[0].cycleOffset),
            ()->assertEquals("automatic-walking-rest-fallback",poses[0].source),
            ()->assertEquals(10,poses[1].sequenceId),
            ()->assertEquals(10,poses[2].sequenceId));
    }

    @Test void standingOnlyCreaturesRetainOneBoundedPoseForAllMovementRows(){
        Sequence530 standing=sequence(20);
        PoseSelection[] poses=AutomaticSheetBuilder.movementSuggestions(standing,null);
        assertAll(
            ()->assertEquals(20,poses[0].sequenceId),
            ()->assertEquals(0,poses[0].frameIndex),
            ()->assertEquals(20,poses[1].sequenceId),
            ()->assertEquals(20,poses[2].sequenceId));
    }

    @Test void separateStandingAndWalkingSequencesPreserveFractionalSuggestions(){
        PoseSelection[] poses=AutomaticSheetBuilder.movementSuggestions(sequence(30),sequence(31));
        assertAll(
            ()->assertEquals(30,poses[0].sequenceId),
            ()->assertEquals(31,poses[1].sequenceId),
            ()->assertEquals(31,poses[2].sequenceId),
            ()->assertNotEquals(poses[1].timeMillis,poses[2].timeMillis));
    }

    @Test void absentAutomaticMetadataRemainsFailClosed(){
        assertArrayEquals(new PoseSelection[]{null,null,null},AutomaticSheetBuilder.movementSuggestions(null,null));
    }

    private static Sequence530 sequence(int id){
        Sequence530 sequence=new Sequence530(id);
        sequence.durations=new int[]{2,3,4};
        sequence.frameIds=new int[]{100,101,102};
        sequence.loopOffset=3;
        return sequence;
    }
}
