package com.spoiledmilk.spritebaker;

import java.util.ArrayList;
import java.util.List;

/** Shared fractional positions used both to fill suggestions and mark the browser. */
public final class AutomaticPoseSuggestions {
    private AutomaticPoseSuggestions(){ }

    public static PoseSelection standing(Sequence530 sequence){return pose(sequence,0,"suggestion");}
    public static PoseSelection leftStep(Sequence530 sequence){return pose(sequence,sequence.totalMillis()/3,"suggestion");}
    public static PoseSelection rightStep(Sequence530 sequence){return pose(sequence,sequence.totalMillis()*2/3,"suggestion");}
    /** Bounded manual fallback: three distinct encoded frames, never three times inside one long frame. */
    public static PoseSelection[] combat(Sequence530 sequence){
        if(sequence.frameIds.length<3)throw new IllegalArgumentException("combat sequence needs at least three encoded frames");
        int middle=(sequence.frameIds.length-1)/2;
        return new PoseSelection[]{atFrame(sequence,0,"suggestion"),atFrame(sequence,middle,"suggestion"),atFrame(sequence,sequence.frameIds.length-1,"suggestion")};
    }

    public static List<Marker> markers(SpriteProject project,Sequence530 sequence){
        List<Marker> markers=new ArrayList<>();
        if(project.standingSequenceId==sequence.id)markers.add(new Marker("AUTO Standing",standing(sequence)));
        if(project.walkingSequenceId==sequence.id){markers.add(new Marker("AUTO Left step",leftStep(sequence)));markers.add(new Marker("AUTO Right step",rightStep(sequence)));}
        if(project.combatSequenceId==sequence.id){PoseSelection[] combat=combat(sequence);for(int i=0;i<combat.length;i++)markers.add(new Marker("AUTO Combat "+(i+1),combat[i]));}
        return List.copyOf(markers);
    }

    public static List<String> labelsAt(List<Marker> markers,FrameSample sample){
        List<String> labels=new ArrayList<>();for(Marker marker:markers)if(marker.pose.frameIndex==sample.frameIndex&&marker.pose.cycleOffset==sample.cycleOffset)labels.add(marker.label);return List.copyOf(labels);
    }

    private static PoseSelection pose(Sequence530 sequence,long millis,String source){return new PoseSelection(AnimationTimeline.sample(sequence,millis),source);}
    private static PoseSelection atFrame(Sequence530 sequence,int frame,String source){return pose(sequence,AnimationTimeline.frameStartMillis(sequence,frame),source);}

    public static final class Marker {
        public final String label;
        public final PoseSelection pose;
        Marker(String label,PoseSelection pose){this.label=label;this.pose=pose;}
    }
}
