package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class SpriteInspectionModelTest {
    @Test void integerZoomReplicatesExportPixelsWithoutInterpolation(){
        BufferedImage source=new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB);source.setRGB(0,0,0xffff0000);source.setRGB(1,0,0xff00ff00);
        BufferedImage zoomed=SpriteInspectionModel.integerScale(source,3);
        assertEquals(6,zoomed.getWidth());assertEquals(3,zoomed.getHeight());
        for(int y=0;y<3;y++)for(int x=0;x<3;x++)assertEquals(0xffff0000,zoomed.getRGB(x,y));
        for(int y=0;y<3;y++)for(int x=3;x<6;x++)assertEquals(0xff00ff00,zoomed.getRGB(x,y));
        assertEquals(0xffff0000,source.getRGB(0,0));assertEquals(0xff00ff00,source.getRGB(1,0));
    }

    @Test void updatesFramesDirectionsAndBackgroundWithoutChangingSprite(){
        BufferedImage first=new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB);first.setRGB(0,0,0);first.setRGB(1,0,0xffff0000);
        int[] original=first.getRGB(0,0,2,1,null,0,2);SpriteInspectionModel model=new SpriteInspectionModel();model.zoom(2);
        model.update(first,Color.BLACK,false,"Standing — Side");BufferedImage shown=model.presented();assertEquals("Standing — Side",model.label());assertEquals(0xff000000,shown.getRGB(0,0));assertEquals(0xffff0000,shown.getRGB(2,0));
        model.update(first,Color.WHITE,true,"Left step — Away");shown=model.presented();assertEquals("Left step — Away",model.label());assertEquals(0xffff0000,shown.getRGB(0,0));assertEquals(0xffffffff,shown.getRGB(2,0));
        assertArrayEquals(original,first.getRGB(0,0,2,1,null,0,2));
    }

    @Test void zoomAndAvailabilityAreEphemeralAndBounded(){
        SpriteInspectionModel model=new SpriteInspectionModel();assertEquals(3,model.zoom());assertFalse(model.available());model.clear("Rendering…");assertEquals("Rendering…",model.label());assertNull(model.presented());assertThrows(IllegalArgumentException.class,()->model.zoom(0));assertThrows(IllegalArgumentException.class,()->model.zoom(5));
    }
}
