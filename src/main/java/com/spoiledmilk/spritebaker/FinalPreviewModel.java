package com.spoiledmilk.spritebaker;

import java.awt.Graphics2D;
import java.awt.Rectangle;
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
    public static int renderedDirection(TargetSheet sheet,DirectionAssignmentSelection selection,boolean swapFacingAway){return SheetDirection.rendered(assignedDirection(sheet,selection),swapFacingAway);}
    public static BufferedImage displayImage(BufferedImage source,boolean mirror){return scaleNearest(source,DISPLAY_SCALE,mirror);}
    public static BufferedImage displayImage(BufferedImage source,boolean mirror,boolean doubled){return scaleNearest(source,DISPLAY_SCALE*(doubled?2:1),mirror);}
    public static BufferedImage displaySprite(BufferedImage source,boolean mirror,boolean doubled){
        Rectangle bounds=visibleBounds(source);BufferedImage content=source.getSubimage(bounds.x,bounds.y,bounds.width,bounds.height);
        return scaleNearest(content,DISPLAY_SCALE*(doubled?2:1),mirror);
    }
    static Rectangle visibleBounds(BufferedImage source){
        if(source==null)throw new NullPointerException("source");int left=source.getWidth(),top=source.getHeight(),right=-1,bottom=-1;
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)if((source.getRGB(x,y)>>>24)!=0){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);}
        return right<left?new Rectangle(0,0,source.getWidth(),source.getHeight()):new Rectangle(left,top,right-left+1,bottom-top+1);
    }
    static BufferedImage scaleNearest(BufferedImage source,int scale,boolean mirror){
        if(source==null)throw new NullPointerException("source");if(scale<1)throw new IllegalArgumentException("scale must be positive");
        BufferedImage out=new BufferedImage(source.getWidth()*scale,source.getHeight()*scale,BufferedImage.TYPE_INT_ARGB);Graphics2D graphics=out.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if(mirror)graphics.drawImage(source,out.getWidth(),0,-out.getWidth(),out.getHeight(),null);else graphics.drawImage(source,0,0,out.getWidth(),out.getHeight(),null);
        graphics.dispose();return out;
    }
}
