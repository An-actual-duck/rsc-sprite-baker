package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Metadata locomotion plus side-view locomotion-deviation combat detection. */
public final class AnimationDiscovery {
    private AnimationDiscovery(){}
    public static void populateKnown(SpriteProject project,AnimationWorkspace workspace){int[] known=knownSequences(workspace.npc,workspace.bas);if(project.standingSequenceId<0)project.standingSequenceId=known[0];if(project.walkingSequenceId<0)project.walkingSequenceId=known[1];}
    static int[] knownSequences(NpcDefinition530 npc,RenderAnimation530 bas){return bas==null?new int[]{npc.standingAnimation,npc.walkingAnimation}:new int[]{bas.standingAnimation,bas.walkingAnimation};}

    public static List<CombatCandidate> combatCandidates(AnimationWorkspace workspace)throws IOException{
        int standing=workspace.bas==null?workspace.npc.standingAnimation:workspace.bas.standingAnimation;
        int walking=workspace.bas==null?workspace.npc.walkingAnimation:workspace.bas.walkingAnimation;
        Set<Integer> referenceFramemaps=new LinkedHashSet<>();
        for(int id:new int[]{standing,walking})if(id>=0)try{
            Sequence530 reference=workspace.cache.loadSequence(id);
            for(int frameId:reference.frameIds)referenceFramemaps.add(workspace.cache.loadFrame(frameId).framemap.id);
        }catch(Exception ignored){}
        if(referenceFramemaps.isEmpty())return List.of();
        CombatPoseDetector.Baseline baseline=CombatPoseDetector.baseline(workspace,standing,walking);
        List<CombatCandidate> out=new ArrayList<>();Set<Integer> tried=new LinkedHashSet<>();
        for(int anchor:new int[]{standing,walking})if(anchor>=0)for(int delta=-16;delta<=16;delta++){
            int id=anchor+delta;if(id<0||id==standing||id==walking||!tried.add(id))continue;
            try{
                Sequence530 sequence=workspace.cache.loadSequence(id);if(sequence.frameIds.length<3||sequence.frameIds.length>24)continue;
                boolean compatible=true;for(int frameId:sequence.frameIds)if(!referenceFramemaps.contains(workspace.cache.loadFrame(frameId).framemap.id)){compatible=false;break;}if(!compatible)continue;
                int metadataScore=100-Math.abs(delta)*3-Math.abs(sequence.frameIds.length-6)*2;
                CombatPoseDetector.Detection detection=CombatPoseDetector.detect(workspace,baseline,sequence,metadataScore);
                String reason="side-view departure from standing/walking with "+detection.distinctPoses+" distinct poses and locomotion recovery";
                out.add(new CombatCandidate(id,sequence.frameIds.length,sequence.totalCycles(),detection.score,reason,detection.poses()));
            }catch(Exception ignored){}
        }
        out.sort(Comparator.comparingInt((CombatCandidate candidate)->candidate.score).reversed().thenComparingInt(candidate->candidate.sequenceId));
        return out.size()>12?List.copyOf(out.subList(0,12)):List.copyOf(out);
    }
}
