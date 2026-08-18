package com.spoiledmilk.spritebaker;

public final class TargetSheet {
    public static final int ROWS=3, COLUMNS=6;
    public static final String[] ROW_LABELS={"Standing","Left step","Right step"};
    public static final String[] COLUMN_LABELS={"Facing camera","Facing diagonal","Side","Diagonal away","Away","Combat side"};
    public Cell[][] cells=new Cell[ROWS][COLUMNS];
    public PoseSelection[] sharedRows=new PoseSelection[ROWS];

    public TargetSheet(){for(int r=0;r<ROWS;r++)for(int c=0;c<COLUMNS;c++)cells[r][c]=new Cell();}

    public void assignShared(int row, PoseSelection selection) {
        sharedRows[row]=selection.copy(); sharedRows[row].source="user-row";
        for(int col=0;col<5;col++) if(!cells[row][col].locked && !cells[row][col].override) {
            cells[row][col].pose=selection.copy(); cells[row][col].pose.source="row-shared";
        }
    }
    public boolean suggest(int row,int col,PoseSelection selection) {
        Cell cell=cells[row][col];
        if(cell.pose!=null || cell.locked) return false;
        cell.pose=selection.copy(); cell.pose.source="suggestion"; return true;
    }
    /** Replaces every unlocked cell with the current recommendations. */
    public int autoPopulate(PoseSelection[] movement,PoseSelection[] combat){
        if(movement==null||movement.length!=ROWS)throw new IllegalArgumentException("three movement poses required");
        if(combat!=null&&combat.length!=ROWS)throw new IllegalArgumentException("three combat poses required");
        int changed=0;
        for(int row=0;row<ROWS;row++){
            PoseSelection movementPose=movement[row];
            if(movementPose!=null){sharedRows[row]=movementPose.copy();sharedRows[row].source="auto-populate";}
            for(int column=0;column<COLUMNS;column++){
                Cell cell=cells[row][column];if(cell.locked)continue;
                PoseSelection recommended=column<5?movementPose:combat==null?movementPose:combat[row];
                if(recommended==null)continue;
                if(cell.override||!samePose(cell.pose,recommended)||!"auto-populate".equals(cell.pose.source))changed++;
                cell.pose=recommended.copy();cell.pose.source="auto-populate";cell.override=false;
            }
        }
        return changed;
    }
    public int autoPopulateCombat(PoseSelection[] combat){
        if(combat==null||combat.length!=ROWS)throw new IllegalArgumentException("three combat poses required");
        int changed=0;
        for(int row=0;row<ROWS;row++){
            Cell cell=cells[row][COLUMNS-1];if(cell.locked||combat[row]==null)continue;
            if(cell.override||!samePose(cell.pose,combat[row])||!"auto-populate".equals(cell.pose.source))changed++;
            cell.pose=combat[row].copy();cell.pose.source="auto-populate";cell.override=false;
        }
        return changed;
    }
    /** Refreshes detected combat recommendations without replacing user overrides or locks. */
    public int refreshDetectedCombat(PoseSelection[] combat){
        if(combat==null||combat.length!=ROWS)throw new IllegalArgumentException("three combat poses required");
        int changed=0;
        for(int row=0;row<ROWS;row++){
            Cell cell=cells[row][COLUMNS-1];if(cell.locked||cell.override||combat[row]==null)continue;
            if(!samePose(cell.pose,combat[row])||!"combat-detection".equals(cell.pose.source))changed++;
            cell.pose=combat[row].copy();cell.pose.source="combat-detection";
        }
        return changed;
    }
    private static boolean samePose(PoseSelection left,PoseSelection right){return left!=null&&left.sequenceId==right.sequenceId&&left.frameIndex==right.frameIndex&&left.cycleOffset==right.cycleOffset&&left.timeMillis==right.timeMillis;}
    public void override(int row,int col,PoseSelection selection) {
        Cell cell=cells[row][col]; if(cell.locked) return;
        cell.pose=selection.copy(); cell.pose.source="user-cell"; cell.override=true;
    }
    public void clearOverride(int row,int col) {
        Cell cell=cells[row][col]; if(cell.locked) return; cell.override=false;
        cell.pose=col<5 && sharedRows[row]!=null?sharedRows[row].copy():null;
        if(cell.pose!=null)cell.pose.source="row-shared";
    }
    public void suggestCombatStandingFromSide() {
        if(cells[0][2].pose!=null)suggest(0,5,cells[0][2].pose);
    }
    public static final class Cell { public PoseSelection pose; public boolean locked; public boolean override; }
}
