package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Broad, provenance-bearing combat browsing plus conservative automatic ranking. */
public final class AnimationDiscovery {
    private AnimationDiscovery(){}
    public static void populateKnown(SpriteProject project,AnimationWorkspace workspace){int[] known=knownSequences(workspace.npc,workspace.bas);if(project.standingSequenceId<0)project.standingSequenceId=known[0];if(project.walkingSequenceId<0)project.walkingSequenceId=known[1];}
    static int[] knownSequences(NpcDefinition530 npc,RenderAnimation530 bas){return bas==null?new int[]{npc.standingAnimation,npc.walkingAnimation}:new int[]{bas.standingAnimation,bas.walkingAnimation};}

    public static List<CombatCandidate> combatCandidates(AnimationWorkspace workspace)throws IOException{return discoverCombat(workspace).candidates;}

    public static CombatDiscovery discoverCombat(AnimationWorkspace workspace)throws IOException{
        int standing=workspace.bas==null?workspace.npc.standingAnimation:workspace.bas.standingAnimation;
        int walking=workspace.bas==null?workspace.npc.walkingAnimation:workspace.bas.walkingAnimation;
        List<String> rejected=new ArrayList<>();Map<Integer,Sequence530> locomotion=new LinkedHashMap<>();Set<Integer> framemaps=new LinkedHashSet<>();
        for(int id:new int[]{standing,walking})if(id>=0&&!locomotion.containsKey(id))try{Sequence530 sequence=workspace.cache.loadSequence(id);locomotion.put(id,sequence);for(int frameId:sequence.frameIds)framemaps.add(workspace.cache.loadFrame(frameId).framemap.id);}catch(Exception e){rejected.add("locomotion sequence "+id+": "+failure(e));}
        if(framemaps.isEmpty())return new CombatDiscovery(List.of(),List.copyOf(rejected),0,"no usable standing/walking framemap baseline");
        CombatPoseDetector.Baseline baseline=CombatPoseDetector.baseline(workspace,standing,walking);

        List<CombatMetadata530.Entry> metadata;
        try{metadata=CombatMetadata530.load(workspace.cachePath,workspace.npc.id);}catch(Exception e){metadata=List.of();rejected.add("adjacent NPC combat metadata: "+failure(e));}
        Map<Integer,CombatMetadata530.Entry> explicit=new LinkedHashMap<>();for(CombatMetadata530.Entry entry:metadata)explicit.put(entry.sequenceId,entry);
        LinkedHashSet<Integer> ids=new LinkedHashSet<>(explicit.keySet());boolean compatibilityFallback=metadata.isEmpty();if(compatibilityFallback)ids.addAll(workspace.cache.relatedSequenceIds(standing,walking));
        Map<String,Integer> fingerprints=new LinkedHashMap<>();List<CombatCandidate> out=new ArrayList<>();int scanned=0;
        for(int id:ids){if(id<0||id==standing||id==walking)continue;CombatMetadata530.Entry relationship=explicit.get(id);if(relationship==null&&nearestDistance(id,standing,walking)>32)continue;scanned++;String source=relationship==null?"bounded ±32 cache compatibility":"adjacent "+relationship.provenance();
            try{
                Sequence530 sequence=workspace.cache.loadSequence(id);
                if(sequence.frameIds.length<2){rejected.add("sequence "+id+" ["+source+"]: fewer than two encoded frames");continue;}
                if(sequence.frameIds.length>64){rejected.add("sequence "+id+" ["+source+"]: exceeds bounded 64-frame analysis");continue;}
                if(!compatible(sequence,framemaps,workspace)){rejected.add("sequence "+id+" ["+source+"]: skeleton/framemap differs from locomotion");continue;}
                if(isLocomotionDuplicate(sequence,locomotion.values())){rejected.add("sequence "+id+" ["+source+"]: duplicates standing/walking frames");continue;}
                String fingerprint=fingerprint(sequence);Integer duplicate=fingerprints.putIfAbsent(fingerprint,id);
                if(duplicate!=null){rejected.add("sequence "+id+" ["+source+"]: duplicates candidate sequence "+duplicate);continue;}

                CombatPoseDetector.Analysis analysis=CombatPoseDetector.analyze(workspace,baseline,sequence);CombatPoseDetector.Detection strict=null;String strictFailure=null;
                if(sequence.frameIds.length<=24)try{strict=CombatPoseDetector.detect(workspace,baseline,sequence,proximityScore(id,standing,walking,sequence.frameIds.length));}catch(IllegalArgumentException e){strictFailure=e.getMessage();}
                if(relationship==null&&!analysis.credibleDeparture()){
                    rejected.add("sequence "+id+" ["+source+"]: "+browseRejection(analysis,strictFailure));continue;
                }
                int score=strict==null?proximityScore(id,standing,walking,sequence.frameIds.length):strict.score;
                String evidence=analysis.distinctPoses+" distinct side-view poses; peak frame "+analysis.peakFrame+"; "+Math.round(Math.max(0,analysis.recoveryRatio)*100)+"% recovery";
                if(strict==null)evidence+="; automatic recommendation rejected"+(strictFailure==null?"":" ("+strictFailure+")");else evidence+="; strict departure/strike/recovery recommendation";
                String provenance=relationship==null?"related cache sequence group":relationship.provenance();String confidence=relationship!=null?"authoritative role":strict==null?"review":"high";
                out.add(new CombatCandidate(id,sequence.frameIds.length,sequence.totalCycles(),score,provenance,confidence,evidence,strict==null?null:strict.poses()));
            }catch(Exception e){rejected.add("sequence "+id+" ["+source+"]: "+failure(e));}
        }
        out.sort(Comparator.comparingInt((CombatCandidate c)->explicit.containsKey(c.sequenceId)?0:1).thenComparing((CombatCandidate c)->c.automaticRecommendation()?0:1).thenComparing(Comparator.comparingInt((CombatCandidate c)->c.score).reversed()).thenComparingInt(c->c.sequenceId));
        return new CombatDiscovery(List.copyOf(out),List.copyOf(rejected),scanned,compatibilityFallback?"bounded ±32 cache compatibility fallback":"adjacent NPC combat metadata");
    }

    static CombatCandidate chooseAutomatic(List<CombatCandidate> candidates){for(CombatCandidate candidate:candidates)if(candidate.automaticRecommendation())return candidate;return null;}
    static boolean isLocomotionDuplicate(Sequence530 candidate,Iterable<Sequence530> locomotion){for(Sequence530 reference:locomotion)if(Arrays.equals(candidate.frameIds,reference.frameIds))return true;return false;}
    private static boolean compatible(Sequence530 sequence,Set<Integer> framemaps,AnimationWorkspace workspace)throws IOException{for(int frameId:sequence.frameIds)if(!framemaps.contains(workspace.cache.loadFrame(frameId).framemap.id))return false;return true;}
    private static String fingerprint(Sequence530 sequence){return Arrays.toString(sequence.frameIds)+"/"+Arrays.toString(sequence.durations);}
    private static int proximityScore(int id,int standing,int walking,int frames){int distance=Integer.MAX_VALUE;for(int anchor:new int[]{standing,walking})if(anchor>=0)distance=Math.min(distance,Math.abs(id-anchor));return 100-Math.min(100,distance)-Math.abs(frames-6)*2;}
    private static int nearestDistance(int id,int standing,int walking){int distance=Integer.MAX_VALUE;for(int anchor:new int[]{standing,walking})if(anchor>=0)distance=Math.min(distance,Math.abs(id-anchor));return distance;}
    private static String browseRejection(CombatPoseDetector.Analysis a,String strictFailure){if(a.distinctPoses<3)return"fewer than three distinct side-view poses";if(a.peakFrame==0)return"starts at maximum departure (no wind-up)";if(a.peakFrame==a.novelty.length-1)return"ends at maximum departure (one-way/death-like motion)";if(a.novelty[a.peakFrame]<5.0e-5)return"duplicates locomotion geometry within the visible threshold";if(a.recoveryRatio<.05)return"less than 5% recovery after peak departure";return strictFailure==null?"not a credible bounded attack excursion":strictFailure;}
    private static String failure(Throwable e){while(e.getCause()!=null)e=e.getCause();String message=e.getMessage();return e.getClass().getSimpleName()+(message==null?"":": "+message);}

    public static final class CombatDiscovery{
        public final List<CombatCandidate> candidates;public final List<String> rejections;public final int scannedSequenceCount;public final String provenance;
        CombatDiscovery(List<CombatCandidate> candidates,List<String> rejections,int scanned,String provenance){this.candidates=candidates;this.rejections=rejections;this.scannedSequenceCount=scanned;this.provenance=provenance;}
        public String summary(){long automatic=candidates.stream().filter(CombatCandidate::automaticRecommendation).count();return candidates.size()+" browsable combat sequences ("+automatic+" automatic), "+rejections.size()+" rejected after "+scannedSequenceCount+" bounded checks; "+provenance;}
    }
}
