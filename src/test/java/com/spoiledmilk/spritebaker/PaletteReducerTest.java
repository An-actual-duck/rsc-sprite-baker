package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class PaletteReducerTest {
    @Test void unmodifiedModePreservesEveryArgbBit(){BufferedImage image=new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB);image.setRGB(0,0,0x7f123456);image.setRGB(1,0,0xffabcdef);VisualSettings settings=new VisualSettings();settings.palette=PaletteReducer.UNMODIFIED;BufferedImage output=PaletteReducer.apply(image,settings);assertArrayEquals(new int[]{0x7f123456,0xffabcdef},output.getRGB(0,0,2,1,null,0,2));}
    @Test void orderedPaletteIsDeterministicAndKeepsTransparency(){BufferedImage image=new BufferedImage(4,4,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<4;y++)for(int x=0;x<4;x++)image.setRGB(x,y,0xff6f8295);image.setRGB(0,0,0);VisualSettings settings=new VisualSettings();settings.palette=PaletteReducer.RSC_27;settings.dithering=PaletteReducer.ORDERED_4X4;BufferedImage a=PaletteReducer.apply(image,settings),b=PaletteReducer.apply(image,settings);assertArrayEquals(a.getRGB(0,0,4,4,null,0,4),b.getRGB(0,0,4,4,null,0,4));assertEquals(0,a.getRGB(0,0));for(int pixel:a.getRGB(0,0,4,4,null,0,4)){if((pixel>>>24)==0)continue;assertTrue(channel(pixel>>>16));assertTrue(channel(pixel>>>8));assertTrue(channel(pixel));}}
    @Test void nearestNeighborUsesDocumentedCenterSample(){BufferedImage source=new BufferedImage(4,4,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<4;y++)for(int x=0;x<4;x++)source.setRGB(x,y,0xff000000|(y*4+x));BufferedImage output=StaticRenderer.nearestNeighbor(source,2,2,2);assertEquals(0xff000005,output.getRGB(0,0));assertEquals(0xff000007,output.getRGB(1,0));assertEquals(0xff00000d,output.getRGB(0,1));assertEquals(0xff00000f,output.getRGB(1,1));}
    private static boolean channel(int shifted){int value=shifted&255;return value==0||value==128||value==255;}
}
