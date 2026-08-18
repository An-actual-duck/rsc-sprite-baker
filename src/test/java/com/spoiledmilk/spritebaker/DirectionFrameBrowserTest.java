package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class DirectionFrameBrowserTest {
    @Test void movementDirectionsCombineStandingAndWalkingWithoutRoleSelection(){
        SpriteProject project=new SpriteProject();project.standingSequenceId=10;project.walkingSequenceId=20;project.combatSequenceId=30;
        DirectionFrameBrowser browser=new DirectionFrameBrowser(project);browser.direction(3);
        assertEquals("Diagonal away",browser.directionLabel());
        assertEquals(List.of(DirectionFrameBrowser.Source.STANDING,DirectionFrameBrowser.Source.WALKING),browser.visibleSources());
        assertEquals(List.of(10,20),browser.visibleSequenceIds());
    }

    @Test void combatDirectionContainsOnlyCombatAlternatives(){
        SpriteProject project=new SpriteProject();project.standingSequenceId=10;project.walkingSequenceId=20;project.combatSequenceId=30;
        DirectionFrameBrowser browser=new DirectionFrameBrowser(project);browser.direction(5);
        assertTrue(browser.combatDirection());assertEquals(List.of(DirectionFrameBrowser.Source.COMBAT),browser.visibleSources());assertEquals(List.of(30),browser.visibleSequenceIds());
    }

    @Test void completeMovementAlternativesCarryExactSmartSelectionMarkers(){
        Sequence530 standing=sequence(10,new int[]{2,1});Sequence530 walking=sequence(20,new int[]{2,3,1});
        List<DirectionFrameBrowser.Alternative> alternatives=DirectionFrameBrowser.alternatives(0,standing,walking,null,false);
        assertEquals(9,alternatives.size());
        assertEquals(List.of("AUTO Standing"),alternatives.get(0).suggestionLabels);
        assertEquals(1,alternatives.stream().filter(a->a.suggestionLabels.contains("AUTO Left step")).count());
        assertEquals(1,alternatives.stream().filter(a->a.suggestionLabels.contains("AUTO Right step")).count());
        assertTrue(alternatives.subList(0,3).stream().allMatch(a->a.source==DirectionFrameBrowser.Source.STANDING));
        assertTrue(alternatives.subList(3,9).stream().allMatch(a->a.source==DirectionFrameBrowser.Source.WALKING));
    }

    @Test void combatAlternativesMarkThreeSuggestedPosesAndExcludeMovement(){
        Sequence530 combat=sequence(30,new int[]{2,3,1});
        List<DirectionFrameBrowser.Alternative> alternatives=DirectionFrameBrowser.alternatives(5,sequence(10,new int[]{1}),sequence(20,new int[]{1}),combat,false);
        assertEquals(6,alternatives.size());assertTrue(alternatives.stream().allMatch(a->a.source==DirectionFrameBrowser.Source.COMBAT));
        assertEquals(3,alternatives.stream().filter(DirectionFrameBrowser.Alternative::suggested).count());
    }

    @Test void combatMarkersFollowDetectedFramesInsteadOfElapsedTimeFractions(){
        Sequence530 combat=sequence(30,new int[]{20000,2,2,2});PoseSelection[] detected={at(combat,1),at(combat,2),at(combat,3)};
        List<DirectionFrameBrowser.Alternative> alternatives=DirectionFrameBrowser.alternatives(5,null,null,combat,false,detected);
        assertEquals(List.of(1,2,3),alternatives.stream().filter(DirectionFrameBrowser.Alternative::suggested).map(value->value.sample.frameIndex).collect(java.util.stream.Collectors.toList()));
    }

    @Test void sheetCellDeterminesTechnicalSourceAndManualAssignmentIsBounded(){
        SpriteProject project=new SpriteProject();DirectionFrameBrowser browser=new DirectionFrameBrowser(project);
        assertEquals(DirectionFrameBrowser.Source.STANDING,DirectionFrameBrowser.sourceForCell(0,2));
        assertEquals(DirectionFrameBrowser.Source.WALKING,DirectionFrameBrowser.sourceForCell(2,4));
        assertEquals(DirectionFrameBrowser.Source.COMBAT,DirectionFrameBrowser.sourceForCell(0,5));
        assertTrue(browser.assign(DirectionFrameBrowser.Source.WALKING,88));assertEquals(88,project.walkingSequenceId);assertFalse(browser.assign(DirectionFrameBrowser.Source.WALKING,88));
        assertThrows(IllegalArgumentException.class,()->browser.direction(6));assertThrows(IllegalArgumentException.class,()->browser.assign(DirectionFrameBrowser.Source.STANDING,-1));
    }

    private static Sequence530 sequence(int id,int[] durations){Sequence530 sequence=new Sequence530(id);sequence.durations=durations;sequence.frameIds=new int[durations.length];for(int i=0;i<durations.length;i++)sequence.frameIds[i]=100+i;sequence.loopOffset=durations.length;return sequence;}
    private static PoseSelection at(Sequence530 sequence,int frame){return new PoseSelection(AnimationTimeline.sample(sequence,AnimationTimeline.frameStartMillis(sequence,frame)),"detected");}
}
