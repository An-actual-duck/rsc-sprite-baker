package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnimationDiscoveryTest {
    @Test void basMetadataOverridesNpcFallbackForKnownLocomotion(){NpcDefinition530 npc=new NpcDefinition530(1);npc.standingAnimation=10;npc.walkingAnimation=11;RenderAnimation530 bas=new RenderAnimation530(2);bas.standingAnimation=20;bas.walkingAnimation=21;assertArrayEquals(new int[]{20,21},AnimationDiscovery.knownSequences(npc,bas));assertArrayEquals(new int[]{10,11},AnimationDiscovery.knownSequences(npc,null));}
    @Test void combatCandidateLabelMakesHeuristicStatusExplicit(){CombatCandidate candidate=new CombatCandidate(30,6,40,90,"near known locomotion ID; review before use");assertTrue(candidate.toString().contains("review"));}
    @Test void npcCatalogMappingMatchesRevision530Grouping(){assertEquals(72,NpcCatalog.npcId(0,72));assertEquals(129,NpcCatalog.npcId(1,1));}
}
