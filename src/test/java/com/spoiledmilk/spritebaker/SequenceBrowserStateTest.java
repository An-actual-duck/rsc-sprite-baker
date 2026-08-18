package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SequenceBrowserStateTest {
    @Test void browsingAndDirectionChangesNeverMutateAssignments(){
        SpriteProject project=new SpriteProject();project.standingSequenceId=10;project.walkingSequenceId=11;project.combatSequenceId=12;
        SequenceBrowserState browser=new SequenceBrowserState(project);browser.browse(99);browser.directionColumn(4);
        assertAll(()->assertEquals(10,project.standingSequenceId),()->assertEquals(11,project.walkingSequenceId),()->assertEquals(12,project.combatSequenceId));
        assertEquals(99,browser.browsedSequenceId());assertEquals(4,browser.directionColumn());
    }

    @Test void onlyExplicitRoleActionAssignsBrowsedSequence(){
        SpriteProject project=new SpriteProject();SequenceBrowserState browser=new SequenceBrowserState(project);browser.browse(42);
        assertTrue(browser.assign(SequenceBrowserState.Role.WALKING));assertEquals(42,project.walkingSequenceId);assertEquals(-1,project.standingSequenceId);assertFalse(browser.assign(SequenceBrowserState.Role.WALKING));
    }
}
