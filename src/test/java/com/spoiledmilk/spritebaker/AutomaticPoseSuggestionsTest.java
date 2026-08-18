package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AutomaticPoseSuggestionsTest {
    @Test void fractionalSuggestionsMapToExactSelectableClientCycles(){
        Sequence530 sequence=sequence(44);SpriteProject project=new SpriteProject();project.walkingSequenceId=44;project.combatSequenceId=44;
        java.util.List<AutomaticPoseSuggestions.Marker> markers=AutomaticPoseSuggestions.markers(project,sequence);
        PoseSelection left=AutomaticPoseSuggestions.leftStep(sequence),right=AutomaticPoseSuggestions.rightStep(sequence);
        assertAll(()->assertEquals(1,left.frameIndex),()->assertEquals(1,left.cycleOffset),()->assertEquals(2,right.frameIndex),()->assertEquals(1,right.cycleOffset));
        java.util.List<FrameSample> selectable=AnimationTimeline.selectableSamples(sequence,false);
        assertTrue(selectable.stream().anyMatch(s->AutomaticPoseSuggestions.labelsAt(markers,s).contains("AUTO Left step")));
        assertTrue(selectable.stream().anyMatch(s->AutomaticPoseSuggestions.labelsAt(markers,s).contains("AUTO Right step")));
        assertEquals(5,markers.size());
    }

    @Test void standingAndCombatMarkersUseTheSameSamplesAsAssignments(){
        Sequence530 sequence=sequence(9);SpriteProject project=new SpriteProject();project.standingSequenceId=9;project.combatSequenceId=9;
        java.util.List<AutomaticPoseSuggestions.Marker> markers=AutomaticPoseSuggestions.markers(project,sequence);
        assertEquals(java.util.List.of("AUTO Standing","AUTO Combat 1"),AutomaticPoseSuggestions.labelsAt(markers,AnimationTimeline.sample(sequence,0)));
    }

    private static Sequence530 sequence(int id){Sequence530 sequence=new Sequence530(id);sequence.durations=new int[]{2,3,4};sequence.frameIds=new int[]{100,101,102};sequence.loopOffset=3;return sequence;}
}
