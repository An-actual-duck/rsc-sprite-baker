package com.spoiledmilk.spritebaker;

import java.awt.image.BufferedImage;

/** Applies the persisted horizontal sprite orientation without mutating renderer output. */
public final class HorizontalOrientation {
    private HorizontalOrientation(){ }

    public static BufferedImage apply(BufferedImage source,boolean inverted){
        if(source==null)throw new NullPointerException("source");int width=source.getWidth(),height=source.getHeight();
        BufferedImage out=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)out.setRGB(x,y,source.getRGB(inverted?width-1-x:x,y));
        return out;
    }
}
