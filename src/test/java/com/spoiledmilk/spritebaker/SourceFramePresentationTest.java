package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class SourceFramePresentationTest {
    @Test void selectionCardsUseEffectiveFacingAndAwayDirectionWithoutChangingSourceIdentity(){
        int[] swapped={4,3,2,1,0,5};
        for(int sourceDirection=0;sourceDirection<TargetSheet.COLUMNS;sourceDirection++){
            assertEquals(sourceDirection,SourceFramePresentation.renderDirection(sourceDirection,false));
            assertEquals(swapped[sourceDirection],SourceFramePresentation.renderDirection(sourceDirection,true));
        }
        assertEquals(180.0,SheetDirection.yawDegrees(SourceFramePresentation.renderDirection(0,true)));
        assertEquals(135.0,SheetDirection.yawDegrees(SourceFramePresentation.renderDirection(1,true)));
    }

    @Test void inversionAndDirectionSwapComposeWithoutMutatingCardPixels(){
        BufferedImage rendered=new BufferedImage(3,1,BufferedImage.TYPE_INT_ARGB);
        rendered.setRGB(0,0,0xffff0000);rendered.setRGB(1,0,0xff00ff00);rendered.setRGB(2,0,0xff0000ff);
        int renderDirection=SourceFramePresentation.renderDirection(0,true);BufferedImage oriented=SourceFramePresentation.orient(rendered,true);
        assertEquals(4,renderDirection);assertEquals(3,oriented.getWidth());assertEquals(1,oriented.getHeight());
        assertEquals(0xff0000ff,oriented.getRGB(0,0));assertEquals(0xff00ff00,oriented.getRGB(1,0));assertEquals(0xffff0000,oriented.getRGB(2,0));
        assertEquals(0xffff0000,rendered.getRGB(0,0));assertEquals(0xff0000ff,rendered.getRGB(2,0));
    }
}
