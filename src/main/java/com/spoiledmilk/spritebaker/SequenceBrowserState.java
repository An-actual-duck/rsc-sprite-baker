package com.spoiledmilk.spritebaker;

/** Ephemeral source-browser state. Only an explicit assignment mutates the project. */
public final class SequenceBrowserState {
    public enum Role { STANDING, WALKING, COMBAT }

    private final SpriteProject project;
    private int browsedSequenceId=-1;
    private int directionColumn;

    public SequenceBrowserState(SpriteProject project){this.project=project;}

    public void browse(int sequenceId){
        if(sequenceId<0)throw new IllegalArgumentException("negative sequence ID");
        browsedSequenceId=sequenceId;
    }

    public int browsedSequenceId(){return browsedSequenceId;}
    public int directionColumn(){return directionColumn;}
    public void directionColumn(int column){directionColumn=SheetDirection.checked(column);}

    public boolean assign(Role role){
        if(browsedSequenceId<0)throw new IllegalStateException("no browsed sequence");
        int previous;
        switch(role){
            case STANDING: previous=project.standingSequenceId;project.standingSequenceId=browsedSequenceId;break;
            case WALKING: previous=project.walkingSequenceId;project.walkingSequenceId=browsedSequenceId;break;
            case COMBAT: previous=project.combatSequenceId;project.combatSequenceId=browsedSequenceId;break;
            default: throw new AssertionError(role);
        }
        return previous!=browsedSequenceId;
    }
}
