package com.spoiledmilk.spritebaker;

/** Canonical target-sheet directions shared by previews, playback, and export. */
public final class SheetDirection {
    public static final double[] YAW_DEGREES={0,45,90,135,180,90};

    private SheetDirection(){ }

    public static int checked(int column){
        if(column<0||column>=TargetSheet.COLUMNS)throw new IllegalArgumentException("direction column outside target sheet: "+column);
        return column;
    }

    public static double yawDegrees(int column){return YAW_DEGREES[checked(column)];}
    public static String label(int column){return TargetSheet.COLUMN_LABELS[checked(column)];}
    public static int rendered(int sourceDirection,boolean swapFacingAway){
        int direction=checked(sourceDirection);if(!swapFacingAway)return direction;
        switch(direction){case 0:return 4;case 1:return 3;case 3:return 1;case 4:return 0;default:return direction;}
    }
}
