package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class PlaybackWorkflowTest {
    @Test void clockPlaysPausesResumesLoopsAndClosesCleanly(){
        AtomicLong now=new AtomicLong(1000);PlaybackClock clock=new PlaybackClock(now::get);clock.play();now.set(1075);assertEquals(75,clock.elapsedMillis());assertEquals(15,clock.loopPosition(60));clock.pause();now.set(2000);assertEquals(75,clock.elapsedMillis());clock.seek(40);clock.play();now.set(2025);assertEquals(65,clock.elapsedMillis());clock.close();assertFalse(clock.isPlaying());assertTrue(clock.isClosed());assertEquals(0,clock.elapsedMillis());assertThrows(IllegalStateException.class,clock::play);
    }

    @Test void movementUsesStandingLeftStandingRightWithEncodedDurations(){
        TargetSheet sheet=sheet();AssembledPlayback.Plan plan=AssembledPlayback.plan(sheet,2,p->p.sequenceId*10L);
        assertArrayEquals(new int[]{0,1,0,2},plan.steps.stream().mapToInt(s->s.row).toArray());
        assertArrayEquals(new int[]{10,20,10,30},plan.steps.stream().mapToInt(s->(int)s.durationMillis).toArray());
        assertEquals(0,plan.stepIndexAt(0));assertEquals(1,plan.stepIndexAt(10));assertEquals(2,plan.stepIndexAt(30));assertEquals(3,plan.stepIndexAt(40));assertEquals(0,plan.stepIndexAt(70));
    }

    @Test void combatSideUsesItsThreeAssignedPoses(){
        TargetSheet sheet=sheet();AssembledPlayback.Plan plan=AssembledPlayback.plan(sheet,5,p->20);
        assertArrayEquals(new int[]{0,1,2},plan.steps.stream().mapToInt(s->s.row).toArray());assertEquals(60,plan.totalMillis);
    }

    @Test void finalPlaybackCarriesEachCellsEffectiveSourceDirection(){
        TargetSheet sheet=sheet();sheet.override(1,2,sheet.cells[1][2].pose,4);
        AssembledPlayback.Plan plan=AssembledPlayback.plan(sheet,2,p->20);
        assertArrayEquals(new int[]{2,4,2,2},plan.steps.stream().mapToInt(s->s.sourceDirection).toArray());
    }

    @Test void directionsAreCanonicalAndMissingCellsFailClosed(){
        assertEquals("Diagonal away",SheetDirection.label(3));assertEquals(135,SheetDirection.yawDegrees(3));
        TargetSheet sheet=sheet();sheet.cells[1][4].pose=null;assertThrows(IllegalStateException.class,()->AssembledPlayback.plan(sheet,4,p->20));assertThrows(IllegalArgumentException.class,()->AssembledPlayback.plan(sheet,0,p->0));assertThrows(IllegalArgumentException.class,()->SheetDirection.yawDegrees(6));
    }

    private static TargetSheet sheet(){TargetSheet sheet=new TargetSheet();for(int r=0;r<3;r++)for(int c=0;c<6;c++){PoseSelection pose=new PoseSelection();pose.sequenceId=r+1;pose.frameIndex=r;pose.source="test";sheet.cells[r][c].pose=pose;}return sheet;}
}
