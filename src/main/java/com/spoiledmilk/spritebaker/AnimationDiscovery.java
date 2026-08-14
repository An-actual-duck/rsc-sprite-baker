package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Metadata defaults plus explicitly non-authoritative nearby combat candidates. */
public final class AnimationDiscovery {
    private AnimationDiscovery(){}
    public static void populateKnown(SpriteProject project,AnimationWorkspace workspace){int[] known=knownSequences(workspace.npc,workspace.bas);if(project.standingSequenceId<0)project.standingSequenceId=known[0];if(project.walkingSequenceId<0)project.walkingSequenceId=known[1];}
    static int[] knownSequences(NpcDefinition530 npc,RenderAnimation530 bas){return bas==null?new int[]{npc.standingAnimation,npc.walkingAnimation}:new int[]{bas.standingAnimation,bas.walkingAnimation};}
    public static List<CombatCandidate> combatCandidates(AnimationWorkspace workspace)throws IOException{
        int standing=workspace.bas==null?workspace.npc.standingAnimation:workspace.bas.standingAnimation,walking=workspace.bas==null?workspace.npc.walkingAnimation:workspace.bas.walkingAnimation,referenceFramemap=-1;for(int id:new int[]{standing,walking})if(id>=0)try{Sequence530 reference=workspace.cache.loadSequence(id);if(reference.frameIds.length>0){referenceFramemap=workspace.cache.loadFrame(reference.frameIds[0]).framemap.id;break;}}catch(Exception ignored){}List<CombatCandidate> out=new ArrayList<>();Set<Integer> tried=new LinkedHashSet<>();for(int anchor:new int[]{standing,walking})if(anchor>=0)for(int delta=-8;delta<=8;delta++){int id=anchor+delta;if(id<0||id==standing||id==walking||!tried.add(id))continue;try{Sequence530 sequence=workspace.cache.loadSequence(id);if(sequence.frameIds.length<2||sequence.frameIds.length>20)continue;int framemap=workspace.cache.loadFrame(sequence.frameIds[0]).framemap.id;if(referenceFramemap>=0&&framemap!=referenceFramemap)continue;int score=100-Math.abs(delta)*5-Math.abs(sequence.frameIds.length-6)*2;out.add(new CombatCandidate(id,sequence.frameIds.length,sequence.totalCycles(),score,"same framemap and near known locomotion ID; review before use"));}catch(Exception ignored){}}
        out.sort(Comparator.comparingInt((CombatCandidate c)->c.score).reversed().thenComparingInt(c->c.sequenceId));return out.size()>12?List.copyOf(out.subList(0,12)):List.copyOf(out);}
}
