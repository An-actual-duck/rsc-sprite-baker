package com.spoiledmilk.spritebaker;

public final class TargetSheet {
    public static final int ROWS=3, COLUMNS=6;
    public static final String[] ROW_LABELS={"Standing","Left step","Right step"};
    public static final String[] COLUMN_LABELS={"Front","Diagonal","Side","Diagonal away","Away","Combat side"};
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
