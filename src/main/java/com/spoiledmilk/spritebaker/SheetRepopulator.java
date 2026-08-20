package com.spoiledmilk.spritebaker;

import java.util.ArrayList;
import java.util.List;

/** Cycles unlocked cells through role-appropriate animation alternatives. */
public final class SheetRepopulator {
    private SheetRepopulator(){ }

    public static Result repopulate(TargetSheet sheet,Sequence530 standing,Sequence530 walking,
                                    Sequence530 combat,PoseSelection[] combatAnchors){
        if(sheet==null)throw new NullPointerException("sheet");
        @SuppressWarnings("unchecked") List<PoseSelection>[] movement=new List[TargetSheet.ROWS],combatByRole=new List[TargetSheet.ROWS];
        for(int row=0;row<TargetSheet.ROWS;row++){movement[row]=movementCandidates(standing,walking,row);combatByRole[row]=combat==null?List.of():combatCandidates(combat,row,combatAnchors);}
        int replaced=0,locked=0;List<String> unavailable=new ArrayList<>();
        for(int row=0;row<TargetSheet.ROWS;row++)for(int column=0;column<TargetSheet.COLUMNS;column++){
            TargetSheet.Cell cell=sheet.cells[row][column];
            if(cell.locked){locked++;continue;}
            List<PoseSelection> candidates=column==TargetSheet.COLUMNS-1&&combat!=null?combatByRole[row]:movement[row];
            PoseSelection next=nextDifferent(cell.pose,candidates);
            if(next==null){unavailable.add(TargetSheet.ROW_LABELS[row]+" / "+TargetSheet.COLUMN_LABELS[column]);continue;}
            cell.pose=next.copy();cell.pose.source="repopulate";cell.override=false;
            // The source view is independent of the pose and intentionally survives repopulation.
            replaced++;
        }
        for(int row=0;row<TargetSheet.ROWS;row++)for(int column=0;column<TargetSheet.COLUMNS-1;column++)if(!sheet.cells[row][column].locked&&sheet.cells[row][column].pose!=null){sheet.sharedRows[row]=sheet.cells[row][column].pose.copy();sheet.sharedRows[row].source="repopulate";break;}
        return new Result(replaced,locked,List.copyOf(unavailable));
    }

    static List<PoseSelection> movementCandidates(Sequence530 standing,Sequence530 walking,int row){
        if(row<0||row>=TargetSheet.ROWS)throw new IllegalArgumentException("row outside target sheet: "+row);
        Sequence530 sequence=row==0?(standing==null?walking:standing):(walking==null?standing:walking);
        if(sequence==null)return List.of();
        List<FrameSample> samples=AnimationTimeline.selectableSamples(sequence,false);
        if(row==0||walking==null)return poses(samples);
        long midpoint=Math.max(1,sequence.totalMillis()/2);List<FrameSample> phase=new ArrayList<>();
        for(FrameSample sample:samples)if(row==1?sample.timeMillis<midpoint:sample.timeMillis>=midpoint)phase.add(sample);
        return poses(phase.isEmpty()?samples:phase);
    }

    static List<PoseSelection> combatCandidates(Sequence530 sequence,int row,PoseSelection[] anchors){
        if(row<0||row>=TargetSheet.ROWS)throw new IllegalArgumentException("row outside target sheet: "+row);
        List<FrameSample> samples=AnimationTimeline.selectableSamples(sequence,false);long total=Math.max(1,sequence.totalMillis());
        long lower=row*total/3,upper=(row+1)*total/3;
        if(anchors!=null&&anchors.length==TargetSheet.ROWS&&anchors[0]!=null&&anchors[1]!=null&&anchors[2]!=null){
            long first=(anchors[0].timeMillis+anchors[1].timeMillis)/2;
            long second=(anchors[1].timeMillis+anchors[2].timeMillis)/2;
            lower=row==0?0:row==1?first:second;upper=row==0?first:row==1?second:total;
        }
        List<FrameSample> phase=new ArrayList<>();for(FrameSample sample:samples){boolean inPhase=row==0?sample.timeMillis<=upper:row==1?sample.timeMillis>lower&&sample.timeMillis<=upper:sample.timeMillis>lower&&sample.timeMillis<total;if(inPhase)phase.add(sample);}
        return poses(phase.isEmpty()?samples:phase);
    }

    private static List<PoseSelection> poses(List<FrameSample> samples){List<PoseSelection> out=new ArrayList<>();for(FrameSample sample:samples)out.add(new PoseSelection(sample,"repopulate"));return List.copyOf(out);}
    private static PoseSelection nextDifferent(PoseSelection current,List<PoseSelection> candidates){
        if(candidates.isEmpty())return null;int currentIndex=-1;
        for(int i=0;i<candidates.size();i++)if(samePose(current,candidates.get(i))){currentIndex=i;break;}
        for(int offset=1;offset<=candidates.size();offset++){PoseSelection candidate=candidates.get(Math.floorMod(currentIndex+offset,candidates.size()));if(!samePose(current,candidate))return candidate;}
        return null;
    }
    private static boolean samePose(PoseSelection left,PoseSelection right){return left!=null&&right!=null&&left.sequenceId==right.sequenceId&&left.frameIndex==right.frameIndex&&left.cycleOffset==right.cycleOffset&&left.timeMillis==right.timeMillis;}

    public static final class Result {
        public final int replacedCells,lockedCells;
        public final List<String> noDifferentAlternative;
        Result(int replaced,int locked,List<String> unavailable){replacedCells=replaced;lockedCells=locked;noDifferentAlternative=unavailable;}
        public String compactSummary(){String base="Repopulated "+replacedCells+" cells; preserved "+lockedCells+" locked cells";return noDifferentAlternative.isEmpty()?base:base+"; "+noDifferentAlternative.size()+" cells have no different viable alternative (hover for details)";}
        public String summary(){String base="Repopulated "+replacedCells+" cells; preserved "+lockedCells+" locked cells";return noDifferentAlternative.isEmpty()?base:base+". No different viable alternative: "+String.join(", ",noDifferentAlternative);}
    }
}
