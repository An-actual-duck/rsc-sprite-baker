package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnimationSourceSelectionTest {
    @Test void roleSelectionAlwaysReportsItsAssignedAnimation(){
        SpriteProject project=new SpriteProject();project.standingSequenceId=286;project.walkingSequenceId=283;project.combatSequenceId=290;
        AnimationSourceSelection source=new AnimationSourceSelection(project);
        assertEquals("Standing animation — sequence 286",source.displayLabel());source.role(AnimationSourceSelection.Role.WALKING);assertEquals(283,source.sequenceId());assertEquals("Walking animation — sequence 283",source.displayLabel());source.role(AnimationSourceSelection.Role.COMBAT);assertEquals(290,source.sequenceId());assertEquals("Combat animation — sequence 290",source.displayLabel());
    }

    @Test void manualOverrideUpdatesTheSelectedRoleAndNoOtherRole(){
        SpriteProject project=new SpriteProject();project.standingSequenceId=1;project.walkingSequenceId=2;project.combatSequenceId=-1;AnimationSourceSelection source=new AnimationSourceSelection(project);source.role(AnimationSourceSelection.Role.COMBAT);
        assertEquals("Combat animation — not discovered",source.displayLabel());assertTrue(source.assignSelectedRole(55));assertEquals(55,source.sequenceId());assertEquals(1,project.standingSequenceId);assertEquals(2,project.walkingSequenceId);assertFalse(source.assignSelectedRole(55));
    }

    @Test void labelsAreMeaningfulAndDirectionIsValidated(){
        assertEquals("Standing animation",AnimationSourceSelection.Role.STANDING.toString());assertEquals("Walking animation",AnimationSourceSelection.Role.WALKING.toString());assertEquals("Combat animation",AnimationSourceSelection.Role.COMBAT.toString());AnimationSourceSelection source=new AnimationSourceSelection(new SpriteProject());source.directionColumn(4);assertEquals(4,source.directionColumn());assertThrows(IllegalArgumentException.class,()->source.directionColumn(6));
    }
    @Test void startsOnTheFirstAutomaticallyAvailableRole(){SpriteProject project=new SpriteProject();project.walkingSequenceId=88;AnimationSourceSelection source=new AnimationSourceSelection(project);assertEquals(AnimationSourceSelection.Role.WALKING,source.role());assertEquals(88,source.sequenceId());}
}
