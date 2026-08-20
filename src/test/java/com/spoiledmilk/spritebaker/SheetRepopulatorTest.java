package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SheetRepopulatorTest {
    @Test void cyclesUnlockedCellsBySemanticRoleWhilePreservingLocksAndSourceViews(){
        Sequence530 standing=sequence(10,2,2,2),walking=sequence(20,2,2,2),combat=sequence(30,2,2,2);
        PoseSelection[] movement={AutomaticPoseSuggestions.standing(standing),AutomaticPoseSuggestions.leftStep(walking),AutomaticPoseSuggestions.rightStep(walking)};
        PoseSelection[] combatPoses=AutomaticPoseSuggestions.combat(combat);TargetSheet sheet=new TargetSheet();sheet.autoPopulate(movement,combatPoses);
        sheet.override(0,1,pose(99,1,20),4);sheet.cells[0][1].locked=true;
        sheet.override(1,2,movement[1],4);long firstLeft=sheet.cells[1][2].pose.timeMillis;

        SheetRepopulator.Result first=SheetRepopulator.repopulate(sheet,standing,walking,combat,combatPoses);
        assertEquals(17,first.replacedCells);assertEquals(1,first.lockedCells);assertTrue(first.noDifferentAlternative.isEmpty());
        assertPose(sheet.cells[0][1].pose,99,1,20);assertTrue(sheet.cells[0][1].locked);assertEquals(4,sheet.cells[0][1].sourceDirection);
        assertEquals(4,sheet.cells[1][2].sourceDirection);assertNotEquals(firstLeft,sheet.cells[1][2].pose.timeMillis);assertFalse(sheet.cells[1][2].override);
        for(int column=0;column<5;column++){if(column!=1)assertEquals(10,sheet.cells[0][column].pose.sequenceId);assertEquals(20,sheet.cells[1][column].pose.sequenceId);assertTrue(sheet.cells[1][column].pose.timeMillis<walking.totalMillis()/2);assertEquals(20,sheet.cells[2][column].pose.sequenceId);assertTrue(sheet.cells[2][column].pose.timeMillis>=walking.totalMillis()/2);}
        assertEquals(30,sheet.cells[0][5].pose.sequenceId);assertEquals(30,sheet.cells[1][5].pose.sequenceId);assertEquals(30,sheet.cells[2][5].pose.sequenceId);
        long afterFirst=sheet.cells[0][0].pose.timeMillis;SheetRepopulator.Result second=SheetRepopulator.repopulate(sheet,standing,walking,combat,combatPoses);assertNotEquals(afterFirst,sheet.cells[0][0].pose.timeMillis);assertEquals(17,second.replacedCells);
    }

    @Test void reportsEveryUnlockedCellThatHasNoDifferentViableAlternative(){
        Sequence530 single=sequence(7,1);PoseSelection only=new PoseSelection(AnimationTimeline.sample(single,0),"test");TargetSheet sheet=new TargetSheet();
        for(int row=0;row<3;row++)for(int column=0;column<6;column++)sheet.cells[row][column].pose=only.copy();
        SheetRepopulator.Result result=SheetRepopulator.repopulate(sheet,single,single,single,new PoseSelection[]{only,only,only});
        assertEquals(0,result.replacedCells);assertEquals(18,result.noDifferentAlternative.size());assertTrue(result.compactSummary().contains("18 cells"));assertTrue(result.summary().contains("Standing / Facing camera"));assertTrue(result.summary().contains("Right step / Combat side"));
    }

    private static Sequence530 sequence(int id,int... durations){Sequence530 sequence=new Sequence530(id);sequence.durations=durations;sequence.frameIds=new int[durations.length];for(int i=0;i<durations.length;i++)sequence.frameIds[i]=100+i;sequence.loopOffset=durations.length;return sequence;}
    private static PoseSelection pose(int sequence,int frame,long millis){PoseSelection pose=new PoseSelection();pose.sequenceId=sequence;pose.frameIndex=frame;pose.timeMillis=millis;return pose;}
    private static void assertPose(PoseSelection pose,int sequence,int frame,long millis){assertEquals(sequence,pose.sequenceId);assertEquals(frame,pose.frameIndex);assertEquals(millis,pose.timeMillis);}
}
