package com.spoiledmilk.spritebaker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

/** The selected-direction playback order for the assembled target sheet. */
public final class AssembledPlayback {
    private AssembledPlayback(){ }

    public static Plan plan(TargetSheet sheet,int directionColumn,ToLongFunction<PoseSelection> durationMillis){
        int column=SheetDirection.checked(directionColumn);
        int[] rows=column==5?new int[]{0,1,2}:new int[]{0,1,0,2};
        List<Step> steps=new ArrayList<>();long total=0;
        for(int row:rows){
            PoseSelection pose=sheet.cells[row][column].pose;
            if(pose==null)throw new IllegalStateException("unassigned "+TargetSheet.ROW_LABELS[row]+" / "+SheetDirection.label(column)+" cell");
            long duration=durationMillis.applyAsLong(pose);
            if(duration<=0)throw new IllegalArgumentException("non-positive playback duration");
            steps.add(new Step(row,pose.copy(),sheet.effectiveSourceDirection(row,column),duration));total+=duration;
        }
        return new Plan(List.copyOf(steps),total);
    }

    public static final class Step {
        public final int row;
        public final PoseSelection pose;
        public final int sourceDirection;
        public final long durationMillis;
        Step(int row,PoseSelection pose,int sourceDirection,long durationMillis){this.row=row;this.pose=pose;this.sourceDirection=sourceDirection;this.durationMillis=durationMillis;}
    }

    public static final class Plan {
        public final List<Step> steps;
        public final long totalMillis;
        Plan(List<Step> steps,long totalMillis){this.steps=steps;this.totalMillis=totalMillis;}
        public int stepIndexAt(long elapsedMillis){
            long position=Math.floorMod(elapsedMillis,totalMillis);
            for(int i=0;i<steps.size();i++){long duration=steps.get(i).durationMillis;if(position<duration)return i;position-=duration;}
            throw new AssertionError("unreachable playback position");
        }
    }
}
