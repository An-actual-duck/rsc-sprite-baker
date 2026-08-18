package com.spoiledmilk.spritebaker;

import java.awt.Color;
import java.awt.image.BufferedImage;

/** Preview-only alpha compositing; the transparent source image is never modified. */
public final class PreviewCompositor {
    private PreviewCompositor(){ }

    public static BufferedImage over(BufferedImage sprite,Color background,boolean mirror){
        return over(sprite,background,mirror,Color.WHITE);
    }

    /** Multiplies sprite RGB by a preview-only tint before alpha compositing. */
    public static BufferedImage over(BufferedImage sprite,Color background,boolean mirror,Color spriteTint){
        if(sprite==null||background==null||spriteTint==null)throw new NullPointerException();
        int width=sprite.getWidth(),height=sprite.getHeight(),backgroundRgb=background.getRGB();
        BufferedImage preview=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        int br=(backgroundRgb>>>16)&255,bg=(backgroundRgb>>>8)&255,bb=backgroundRgb&255;
        int tr=spriteTint.getRed(),tg=spriteTint.getGreen(),tb=spriteTint.getBlue();
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            int source=sprite.getRGB(mirror?width-1-x:x,y),alpha=source>>>24,inverse=255-alpha;
            int sr=((source>>>16)&255)*tr/255,sg=((source>>>8)&255)*tg/255,sb=(source&255)*tb/255;
            int red=(sr*alpha+br*inverse+127)/255;
            int green=(sg*alpha+bg*inverse+127)/255;
            int blue=(sb*alpha+bb*inverse+127)/255;
            preview.setRGB(x,y,0xff000000|(red<<16)|(green<<8)|blue);
        }
        return preview;
    }
}
