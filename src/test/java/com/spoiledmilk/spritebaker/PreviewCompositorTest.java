package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class PreviewCompositorTest {
    @Test void compositesTransparencyWithoutChangingSpriteOrExportAlpha(){
        BufferedImage sprite=new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB);sprite.setRGB(0,0,0x00010203);sprite.setRGB(1,0,0x80ff0000);int[] original=sprite.getRGB(0,0,2,1,null,0,2);
        BufferedImage preview=PreviewCompositor.over(sprite,Color.BLACK,false);
        assertArrayEquals(original,sprite.getRGB(0,0,2,1,null,0,2));assertEquals(0,sprite.getRGB(0,0)>>>24);assertEquals(128,sprite.getRGB(1,0)>>>24);
        assertEquals(0xff000000,preview.getRGB(0,0));assertEquals(0xff800000,preview.getRGB(1,0));assertEquals(2,preview.getWidth());assertEquals(1,preview.getHeight());
    }

    @Test void mirrorAndBackgroundAffectOnlyThePreviewCopy(){
        BufferedImage sprite=new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB);sprite.setRGB(0,0,0xffff0000);sprite.setRGB(1,0,0x00000000);
        BufferedImage preview=PreviewCompositor.over(sprite,new Color(0,128,0),true);
        assertEquals(0xff008000,preview.getRGB(0,0));assertEquals(0xffff0000,preview.getRGB(1,0));assertEquals(0xffff0000,sprite.getRGB(0,0));assertEquals(0,sprite.getRGB(1,0));
    }
}
