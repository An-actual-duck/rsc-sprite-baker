package com.spoiledmilk.spritebaker;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Non-GUI selected-cell preview state and pixel-exact display scaling. */
public final class FinalPreviewModel {
    public static final int DISPLAY_SCALE=2;
    public static final boolean DEFAULT_DOUBLED=true;
    private FinalPreviewModel(){ }

    public static PoseSelection assignedPose(TargetSheet sheet,DirectionAssignmentSelection selection){
        PoseSelection pose=sheet.cells[selection.destinationRow()][selection.destinationColumn()].pose;
        return pose==null?null:pose.copy();
    }
    public static int assignedDirection(TargetSheet sheet,DirectionAssignmentSelection selection){
        return sheet.effectiveSourceDirection(selection.destinationRow(),selection.destinationColumn());
    }
    public static BufferedImage displayImage(BufferedImage source,boolean mirror){return scaleNearest(source,DISPLAY_SCALE,mirror);}
    public static BufferedImage displayImage(BufferedImage source,boolean mirror,boolean doubled){return scaleNearest(source,DISPLAY_SCALE*(doubled?2:1),mirror);}
    static BufferedImage scaleNearest(BufferedImage source,int scale,boolean mirror){
        if(source==null)throw new NullPointerException("source");if(scale<1)throw new IllegalArgumentException("scale must be positive");
        BufferedImage out=new BufferedImage(source.getWidth()*scale,source.getHeight()*scale,BufferedImage.TYPE_INT_ARGB);Graphics2D graphics=out.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if(mirror)graphics.drawImage(source,out.getWidth(),0,-out.getWidth(),out.getHeight(),null);else graphics.drawImage(source,0,0,out.getWidth(),out.getHeight(),null);
        graphics.dispose();return out;
    }
}
