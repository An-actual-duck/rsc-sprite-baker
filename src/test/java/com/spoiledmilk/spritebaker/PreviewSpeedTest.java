package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PreviewSpeedTest {
    @Test void defaultsToComfortableHalfSpeedAndOffersOnlyClearRates(){
        assertEquals(PreviewSpeed.HALF,PreviewSpeed.DEFAULT);
        assertArrayEquals(new String[]{"0.5×","0.75×","1×"},java.util.Arrays.stream(PreviewSpeed.values()).map(Object::toString).toArray(String[]::new));
    }

    @Test void scalesOnlyTheClockPositionNotEncodedPlanDurations(){
        TargetSheet sheet=sheet();AssembledPlayback.Plan plan=AssembledPlayback.plan(sheet,2,pose->20);
        assertEquals(80,plan.totalMillis);assertEquals(0,plan.stepIndexAt(PreviewSpeed.HALF.animationMillis(39)));
        assertEquals(1,plan.stepIndexAt(PreviewSpeed.HALF.animationMillis(40)));
        assertEquals(1,plan.stepIndexAt(PreviewSpeed.THREE_QUARTER.animationMillis(27)));
        assertEquals(1,plan.stepIndexAt(PreviewSpeed.NORMAL.animationMillis(20)));
        assertTrue(plan.steps.stream().allMatch(step->step.durationMillis==20));
    }

    @Test void switchingRatesCanPreserveCurrentAnimationPosition(){
        long animation=PreviewSpeed.HALF.animationMillis(120);
        long newClock=PreviewSpeed.THREE_QUARTER.realMillis(animation);
        assertEquals(60,animation);assertEquals(80,newClock);assertEquals(animation,PreviewSpeed.THREE_QUARTER.animationMillis(newClock));
    }

    private static TargetSheet sheet(){TargetSheet sheet=new TargetSheet();for(int r=0;r<3;r++)for(int c=0;c<6;c++){PoseSelection pose=new PoseSelection();pose.sequenceId=r+1;pose.frameIndex=r;sheet.cells[r][c].pose=pose;}return sheet;}
}
