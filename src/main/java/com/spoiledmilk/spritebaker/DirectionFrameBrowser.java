package com.spoiledmilk.spritebaker;

import java.util.ArrayList;
import java.util.List;

/** Direction-first source state shared by the editor and its non-GUI tests. */
public final class DirectionFrameBrowser {
    public enum Source {
        STANDING("Standing animation"), WALKING("Walking animation"), COMBAT("Combat animation");

        public final String label;
        Source(String label){this.label=label;}
        @Override public String toString(){return label;}
    }

    private final SpriteProject project;
    private int direction;
    private List<Integer> combatSequenceIds=List.of();
    private int browsedCombatSequence=-1;

    public DirectionFrameBrowser(SpriteProject project){
        if(project==null)throw new NullPointerException("project");
        this.project=project;
    }

    public int direction(){return direction;}
    public void direction(int column){direction=SheetDirection.checked(column);}
    public boolean combatDirection(){return direction==TargetSheet.COLUMNS-1;}
    public String directionLabel(){return SheetDirection.label(direction);}
    public List<Source> visibleSources(){return sourcesFor(direction);}

    public static List<Source> sourcesFor(int column){
        SheetDirection.checked(column);
        return column==TargetSheet.COLUMNS-1?List.of(Source.COMBAT):List.of(Source.STANDING,Source.WALKING);
    }

    public static Source sourceForCell(int row,int column){
        if(row<0||row>=TargetSheet.ROWS)throw new IllegalArgumentException("row outside target sheet: "+row);
        SheetDirection.checked(column);
        return column==TargetSheet.COLUMNS-1?Source.COMBAT:row==0?Source.STANDING:Source.WALKING;
    }

    public int sequenceId(Source source){
        switch(source){
            case STANDING:return project.standingSequenceId;
            case WALKING:return project.walkingSequenceId;
            case COMBAT:return project.combatSequenceId;
            default:throw new AssertionError(source);
        }
    }

    public List<Integer> visibleSequenceIds(){
        if(combatDirection()){
            int id=browsedCombatSequence>=0?browsedCombatSequence:project.combatSequenceId;
            return id<0?List.of():List.of(id);
        }
        List<Integer> ids=new ArrayList<>();
        for(Source source:visibleSources()){int id=sequenceId(source);if(id>=0)ids.add(id);}
        return List.copyOf(ids);
    }

    public void combatSequenceIds(List<Integer> ids){
        java.util.LinkedHashSet<Integer> unique=new java.util.LinkedHashSet<>();for(Integer id:ids)if(id!=null&&id>=0)unique.add(id);
        if(project.combatSequenceId>=0)unique.add(project.combatSequenceId);
        combatSequenceIds=List.copyOf(unique);
        if(!combatSequenceIds.contains(browsedCombatSequence))browsedCombatSequence=combatSequenceIds.contains(project.combatSequenceId)?project.combatSequenceId:combatSequenceIds.isEmpty()?-1:combatSequenceIds.get(0);
    }
    public List<Integer> combatSequenceIds(){return combatSequenceIds;}
    public int browsedCombatSequence(){return browsedCombatSequence;}
    public boolean browseCombatSequence(int sequenceId){if(!combatSequenceIds.contains(sequenceId))throw new IllegalArgumentException("undiscovered combat sequence "+sequenceId);int previous=browsedCombatSequence;browsedCombatSequence=sequenceId;return previous!=sequenceId;}

    public boolean assign(Source source,int sequenceId){
        if(source==null)throw new NullPointerException("source");
        if(sequenceId<0)throw new IllegalArgumentException("negative sequence ID");
        int previous=sequenceId(source);
        switch(source){
            case STANDING:project.standingSequenceId=sequenceId;break;
            case WALKING:project.walkingSequenceId=sequenceId;break;
            case COMBAT:project.combatSequenceId=sequenceId;break;
            default:throw new AssertionError(source);
        }
        if(source==Source.COMBAT){java.util.LinkedHashSet<Integer> ids=new java.util.LinkedHashSet<>(combatSequenceIds);ids.add(sequenceId);combatSequenceIds=List.copyOf(ids);browsedCombatSequence=sequenceId;}
        return previous!=sequenceId;
    }

    public static List<Alternative> alternatives(int column,Sequence530 standing,Sequence530 walking,
                                                  Sequence530 combat,boolean keyframesOnly){
        return alternatives(column,standing,walking,combat,keyframesOnly,null);
    }

    public static List<Alternative> alternatives(int column,Sequence530 standing,Sequence530 walking,
                                                  Sequence530 combat,boolean keyframesOnly,PoseSelection[] combatSuggestions){
        List<Alternative> out=new ArrayList<>();
        for(Source source:sourcesFor(column)){
            Sequence530 sequence=source==Source.STANDING?standing:source==Source.WALKING?walking:combat;
            if(sequence==null)continue;
            List<AutomaticPoseSuggestions.Marker> markers=markers(source,sequence,combatSuggestions);
            for(FrameSample sample:AnimationTimeline.selectableSamples(sequence,keyframesOnly))
                out.add(new Alternative(source,sample,AutomaticPoseSuggestions.labelsAt(markers,sample)));
        }
        return List.copyOf(out);
    }

    private static List<AutomaticPoseSuggestions.Marker> markers(Source source,Sequence530 sequence,PoseSelection[] combatSuggestions){
        List<AutomaticPoseSuggestions.Marker> markers=new ArrayList<>();
        if(source==Source.STANDING)markers.add(new AutomaticPoseSuggestions.Marker("AUTO Standing",AutomaticPoseSuggestions.standing(sequence)));
        else if(source==Source.WALKING){
            markers.add(new AutomaticPoseSuggestions.Marker("AUTO Left step",AutomaticPoseSuggestions.leftStep(sequence)));
            markers.add(new AutomaticPoseSuggestions.Marker("AUTO Right step",AutomaticPoseSuggestions.rightStep(sequence)));
        }else{
            PoseSelection[] poses=combatSuggestions==null?AutomaticPoseSuggestions.combat(sequence):combatSuggestions;
            for(int i=0;i<poses.length;i++)markers.add(new AutomaticPoseSuggestions.Marker("AUTO Combat "+(i+1),poses[i]));
        }
        return List.copyOf(markers);
    }

    public static final class Alternative {
        public final Source source;
        public final FrameSample sample;
        public final List<String> suggestionLabels;

        Alternative(Source source,FrameSample sample,List<String> suggestionLabels){
            this.source=source;this.sample=sample;this.suggestionLabels=List.copyOf(suggestionLabels);
        }
        public boolean suggested(){return !suggestionLabels.isEmpty();}
        public PoseSelection pose(){return new PoseSelection(sample,"direction-browser");}
    }
}
