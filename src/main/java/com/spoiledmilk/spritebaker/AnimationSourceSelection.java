package com.spoiledmilk.spritebaker;

/** Role-first editor source state; the selected label and sequence cannot diverge. */
public final class AnimationSourceSelection {
    public enum Role {
        STANDING("Standing animation"), WALKING("Walking animation"), COMBAT("Combat animation");
        public final String label;
        Role(String label){this.label=label;}
        @Override public String toString(){return label;}
    }

    private final SpriteProject project;
    private Role role=Role.STANDING;
    private int directionColumn;

    public AnimationSourceSelection(SpriteProject project){this.project=project;if(project.standingSequenceId<0){if(project.walkingSequenceId>=0)role=Role.WALKING;else if(project.combatSequenceId>=0)role=Role.COMBAT;}}

    public Role role(){return role;}
    public void role(Role role){if(role==null)throw new NullPointerException("role");this.role=role;}
    public int sequenceId(){return sequenceId(role);}
    public int sequenceId(Role sourceRole){
        switch(sourceRole){
            case STANDING:return project.standingSequenceId;
            case WALKING:return project.walkingSequenceId;
            case COMBAT:return project.combatSequenceId;
            default:throw new AssertionError(sourceRole);
        }
    }
    public boolean assignSelectedRole(int sequenceId){
        if(sequenceId<0)throw new IllegalArgumentException("negative sequence ID");
        int previous=sequenceId();
        switch(role){
            case STANDING:project.standingSequenceId=sequenceId;break;
            case WALKING:project.walkingSequenceId=sequenceId;break;
            case COMBAT:project.combatSequenceId=sequenceId;break;
            default:throw new AssertionError(role);
        }
        return previous!=sequenceId;
    }
    public String displayLabel(){int id=sequenceId();return role.label+(id<0?" — not discovered":" — sequence "+id);}
    public int directionColumn(){return directionColumn;}
    public void directionColumn(int column){directionColumn=SheetDirection.checked(column);}
}
