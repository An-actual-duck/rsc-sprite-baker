package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.runelite.cache.definitions.ModelDefinition;

/** Detects combat as a distinct side-view departure from known locomotion. */
public final class CombatPoseDetector {
    private static final double SIDE_YAW=Math.toRadians(90), PITCH=Math.toRadians(15);
    private CombatPoseDetector(){ }

    static Baseline baseline(AnimationWorkspace workspace,int standingId,int walkingId)throws IOException{
        List<Signature> signatures=new ArrayList<>();
        java.util.Set<Integer> tried=new java.util.LinkedHashSet<>();
        for(int sequenceId:new int[]{standingId,walkingId})if(sequenceId>=0&&tried.add(sequenceId))try{
            Sequence530 sequence=workspace.cache.loadSequence(sequenceId);List<Signature> sequenceSignatures=new ArrayList<>();
            for(FrameSample sample:AnimationTimeline.selectableSamples(sequence,true))sequenceSignatures.add(signature(workspace.pose(new PoseSelection(sample,"combat-baseline"),false),workspace.npc));
            signatures.addAll(sequenceSignatures);
        }catch(IllegalArgumentException|IOException ignored){}
        if(signatures.isEmpty())throw new IllegalArgumentException("combat detection needs a standing or walking baseline");
        return new Baseline(List.copyOf(signatures),scale(signature(workspace.baseModel,workspace.npc)));
    }

    static Detection detect(AnimationWorkspace workspace,Baseline baseline,Sequence530 sequence,int metadataScore)throws IOException{
        if(sequence.frameIds.length<3)throw new IllegalArgumentException("combat sequence needs at least three encoded frames");
        if(sequence.frameIds.length>24)throw new IllegalArgumentException("combat sequence exceeds bounded frame count");
        double[] novelty=new double[sequence.frameIds.length];List<Signature> signatures=new ArrayList<>();
        for(int frame=0;frame<sequence.frameIds.length;frame++){
            FrameSample sample=AnimationTimeline.sample(sequence,AnimationTimeline.frameStartMillis(sequence,frame));
            Signature value=signature(workspace.pose(new PoseSelection(sample,"combat-analysis"),false),workspace.npc);signatures.add(value);
            novelty[frame]=distance(value,baseline.signatures)/baseline.scale;
        }
        int distinct=distinct(signatures,baseline.scale);int peak=maximum(novelty);
        double endpoint=Math.max(novelty[0],novelty[novelty.length-1]),excursion=novelty[peak]-endpoint;
        if(distinct<3||!isExcursion(novelty))throw new IllegalArgumentException("sequence does not contain three distinct side-view poses that depart from and return to locomotion");
        PoseSelection[] poses=selectDistinct(sequence,novelty,signatures,baseline.scale);int score=motionScore(metadataScore,novelty[peak],excursion/novelty[peak]);
        return new Detection(poses,score,novelty[peak],distinct);
    }

    public static PoseSelection[] suggest(AnimationWorkspace workspace,int standingId,int walkingId,int combatId)throws IOException{
        Baseline baseline=baseline(workspace,standingId,walkingId);Sequence530 sequence=workspace.cache.loadSequence(combatId);
        return detect(workspace,baseline,sequence,0).poses();
    }

    static PoseSelection[] selectFromNovelty(Sequence530 sequence,double[] novelty){
        if(novelty.length!=sequence.frameIds.length||novelty.length<3)throw new IllegalArgumentException("three combat keyframes required");
        int peak=maximum(novelty),before=maximum(novelty,0,peak),after=maximum(novelty,peak+1,novelty.length);
        if(peak==0||peak==novelty.length-1)throw new IllegalArgumentException("combat excursion must have departure and recovery frames");
        return new PoseSelection[]{atFrame(sequence,before),atFrame(sequence,peak),atFrame(sequence,after)};
    }

    private static PoseSelection[] selectDistinct(Sequence530 sequence,double[] novelty,List<Signature> signatures,double scale){
        int peak=maximum(novelty);if(peak==0||peak==novelty.length-1)throw new IllegalArgumentException("combat excursion must have departure and recovery frames");
        int before=maximumDistinct(novelty,0,peak,signatures,List.of(signatures.get(peak)),scale);
        int after=maximumDistinct(novelty,peak+1,novelty.length,signatures,List.of(signatures.get(peak),signatures.get(before)),scale);
        return new PoseSelection[]{atFrame(sequence,before),atFrame(sequence,peak),atFrame(sequence,after)};
    }

    static int motionScore(int metadataScore,double peakNovelty,double returnRatio){
        int noveltyBonus=(int)Math.min(60,Math.round(Math.sqrt(Math.max(0,peakNovelty))*1800));
        int returnBonus=(int)Math.round(Math.max(0,Math.min(1,returnRatio))*20);
        return metadataScore+noveltyBonus+returnBonus;
    }

    static boolean isExcursion(double[] novelty){if(novelty.length<3)return false;int peak=maximum(novelty);double endpoint=Math.max(novelty[0],novelty[novelty.length-1]);return peak>0&&peak<novelty.length-1&&novelty[peak]>=5.0e-5&&novelty[peak]-endpoint>novelty[peak]*.15;}

    private static PoseSelection atFrame(Sequence530 sequence,int frame){return new PoseSelection(AnimationTimeline.sample(sequence,AnimationTimeline.frameStartMillis(sequence,frame)),"automatic-combat-deviation");}
    private static int maximum(double[] values){return maximum(values,0,values.length);}
    private static int maximum(double[] values,int start,int end){if(start>=end)throw new IllegalArgumentException("empty combat phase");int best=start;for(int i=start+1;i<end;i++)if(values[i]>values[best])best=i;return best;}
    private static int maximumDistinct(double[] novelty,int start,int end,List<Signature> signatures,List<Signature> excluded,double scale){int best=-1;for(int i=start;i<end;i++){boolean duplicate=false;for(Signature value:excluded)if(distance(signatures.get(i),List.of(value))/scale<=1.0e-10){duplicate=true;break;}if(!duplicate&&(best<0||novelty[i]>novelty[best]))best=i;}if(best<0)throw new IllegalArgumentException("combat departure, strike, and recovery are not visually distinct");return best;}
    private static int distinct(List<Signature> values,double scale){List<Signature> unique=new ArrayList<>();outer:for(Signature value:values){for(Signature prior:unique)if(distance(value,List.of(prior))/scale<=1.0e-10)continue outer;unique.add(value);}return unique.size();}
    private static double distance(Signature value,List<Signature> references){double best=Double.POSITIVE_INFINITY;for(Signature reference:references){if(value.xy.length!=reference.xy.length)continue;double sum=0;for(int i=0;i<value.xy.length;i++){double delta=value.xy[i]-reference.xy[i];sum+=delta*delta;}best=Math.min(best,sum/value.xy.length);}if(!Double.isFinite(best))throw new IllegalArgumentException("combat poses do not share model topology");return best;}
    private static double scale(Signature value){double minX=Double.POSITIVE_INFINITY,maxX=Double.NEGATIVE_INFINITY,minY=Double.POSITIVE_INFINITY,maxY=Double.NEGATIVE_INFINITY;for(int i=0;i<value.xy.length;i+=2){minX=Math.min(minX,value.xy[i]);maxX=Math.max(maxX,value.xy[i]);minY=Math.min(minY,value.xy[i+1]);maxY=Math.max(maxY,value.xy[i+1]);}double width=maxX-minX,height=maxY-minY;return Math.max(1,width*width+height*height);}
    private static Signature signature(ModelDefinition model,NpcDefinition530 npc){
        double[] out=new double[model.vertexCount*2];double centerX=0,ground=Double.POSITIVE_INFINITY;
        for(int vertex=0;vertex<model.vertexCount;vertex++){
            double x=model.vertexX[vertex]*npc.widthScale/128.0,up=-model.vertexY[vertex]*npc.heightScale/128.0,z=model.vertexZ[vertex]*npc.widthScale/128.0;
            double sideX=Math.cos(SIDE_YAW)*x+Math.sin(SIDE_YAW)*z,depth=-Math.sin(SIDE_YAW)*x+Math.cos(SIDE_YAW)*z;
            double sideY=up*Math.cos(PITCH)-depth*Math.sin(PITCH);out[vertex*2]=sideX;out[vertex*2+1]=sideY;centerX+=sideX;ground=Math.min(ground,sideY);
        }
        centerX/=model.vertexCount;for(int vertex=0;vertex<model.vertexCount;vertex++){out[vertex*2]-=centerX;out[vertex*2+1]-=ground;}
        return new Signature(out);
    }

    static final class Baseline {final List<Signature> signatures;final double scale;Baseline(List<Signature> signatures,double scale){this.signatures=signatures;this.scale=scale;}}
    private static final class Signature {final double[] xy;Signature(double[] xy){this.xy=xy;}}
    static final class Detection {
        final PoseSelection[] poses;final int score;final double peakNovelty;final int distinctPoses;
        Detection(PoseSelection[] poses,int score,double peakNovelty,int distinctPoses){this.poses=poses;this.score=score;this.peakNovelty=peakNovelty;this.distinctPoses=distinctPoses;}
        PoseSelection[] poses(){return new PoseSelection[]{poses[0].copy(),poses[1].copy(),poses[2].copy()};}
    }
}
