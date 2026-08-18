package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class CombatPoseDetectorTest {
    @Test void choosesDistinctDepartureStrikeAndRecoveryFrames(){
        Sequence530 sequence=sequence(70,7);
        PoseSelection[] poses=CombatPoseDetector.selectFromNovelty(sequence,new double[]{.01,.30,.80,.70,.75,.20,.01});
        assertArrayEquals(new int[]{1,2,4},new int[]{poses[0].frameIndex,poses[1].frameIndex,poses[2].frameIndex});
        assertEquals(0,poses[0].cycleOffset);assertEquals(0,poses[1].cycleOffset);assertEquals(0,poses[2].cycleOffset);
    }

    @Test void refusesExcursionsWithoutBothDepartureAndRecovery(){
        Sequence530 sequence=sequence(71,4);
        assertThrows(IllegalArgumentException.class,()->CombatPoseDetector.selectFromNovelty(sequence,new double[]{.1,.2,.3,.9}));
        assertThrows(IllegalArgumentException.class,()->CombatPoseDetector.selectFromNovelty(sequence,new double[]{.9,.3,.2,.1}));
    }

    @Test void motionScoreRewardsVisibleNoveltyAndReturn(){
        assertTrue(CombatPoseDetector.motionScore(80,.01,1)>CombatPoseDetector.motionScore(80,.0001,1));
        assertTrue(CombatPoseDetector.motionScore(80,.01,1)>CombatPoseDetector.motionScore(80,.01,0));
    }

    @Test void requiresVisibleDepartureAndRecoveryInsteadOfFrozenOrOneWayMotion(){
        assertTrue(CombatPoseDetector.isExcursion(new double[]{.00001,.003,.004,.001,.00001}));
        assertFalse(CombatPoseDetector.isExcursion(new double[]{.003,.003,.003,.003}));
        assertFalse(CombatPoseDetector.isExcursion(new double[]{0,.001,.002,.004}));
        assertFalse(CombatPoseDetector.isExcursion(new double[]{0,.00001,0}));
    }

    private static Sequence530 sequence(int id,int frames){Sequence530 sequence=new Sequence530(id);sequence.frameIds=new int[frames];sequence.durations=new int[frames];for(int i=0;i<frames;i++){sequence.frameIds[i]=100+i;sequence.durations[i]=i==0?20000:2;}return sequence;}
}
