package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class AnimationDiscoveryTest {
    @Test void basMetadataOverridesNpcFallbackForKnownLocomotion(){NpcDefinition530 npc=new NpcDefinition530(1);npc.standingAnimation=10;npc.walkingAnimation=11;RenderAnimation530 bas=new RenderAnimation530(2);bas.standingAnimation=20;bas.walkingAnimation=21;assertArrayEquals(new int[]{20,21},AnimationDiscovery.knownSequences(npc,bas));assertArrayEquals(new int[]{10,11},AnimationDiscovery.knownSequences(npc,null));}
    @Test void combatCandidateLabelMakesRoleAndHeuristicStatusExplicit(){CombatCandidate candidate=new CombatCandidate(30,6,40,90,"same framemap and near known locomotion ID; review before use");assertTrue(candidate.toString().startsWith("Possible combat animation"));assertTrue(candidate.toString().contains("review"));assertTrue(candidate.toString().endsWith("(sequence 30)"));}
    @Test void npcCatalogMappingMatchesRevision530Grouping(){assertEquals(72,NpcCatalog.npcId(0,72));assertEquals(129,NpcCatalog.npcId(1,1));}
    @Test void locomotionDuplicatesAreRejectedByExactCacheFrameIdentity(){Sequence530 standing=sequence(1,100,101),walking=sequence(2,200,201),duplicate=sequence(3,100,101),distinct=sequence(4,100,102);assertTrue(AnimationDiscovery.isLocomotionDuplicate(duplicate,java.util.List.of(standing,walking)));assertFalse(AnimationDiscovery.isLocomotionDuplicate(distinct,java.util.List.of(standing,walking)));}
    @Test void automaticChoiceSkipsBroadReviewCandidatesAndPreservesStrictRanking(){CombatCandidate broad=new CombatCandidate(9,4,12,500,"metadata only"),strict=new CombatCandidate(10,5,14,80,"strict",poses(10)),later=new CombatCandidate(11,5,14,200,"strict",poses(11));assertSame(strict,AnimationDiscovery.chooseAutomatic(java.util.List.of(broad,strict,later)));}
    private static Sequence530 sequence(int id,int... frames){Sequence530 sequence=new Sequence530(id);sequence.frameIds=frames;sequence.durations=new int[frames.length];java.util.Arrays.fill(sequence.durations,1);return sequence;}
    private static PoseSelection[] poses(int sequence){PoseSelection[] out={new PoseSelection(),new PoseSelection(),new PoseSelection()};for(PoseSelection pose:out)pose.sequenceId=sequence;return out;}
}
