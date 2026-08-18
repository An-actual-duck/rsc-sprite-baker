package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NonHumanoidVisualMatrixTest {
    @Test void matrixIsBroadStableAndFreeOfNpcSpecificRenderConfiguration(){
        Set<Integer> ids=new HashSet<>();
        Set<String> families=new HashSet<>();
        for(NonHumanoidVisualMatrix.Entry entry:NonHumanoidVisualMatrix.ENTRIES){
            assertTrue(ids.add(entry.npcId),"duplicate NPC "+entry.npcId);
            assertFalse(entry.expectedName.isBlank());
            families.addAll(entry.families);
        }
        assertTrue(NonHumanoidVisualMatrix.ENTRIES.size()>=24);
        assertTrue(families.containsAll(Set.of("dragon","quadruped","arachnid","insect","serpentine",
            "unusual","flying","aquatic","amorphous","large-boss","multipart","textured","slayer")));
    }
}
