package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class HorizontalOrientationTest {
    @Test void inversionIsPixelExactAndNeverMutatesRenderedOrExportDimensions(){
        BufferedImage source=new BufferedImage(3,2,BufferedImage.TYPE_INT_ARGB);int[] pixels={0x00112233,0x80445566,0xff778899,0xffaabbcc,0xffddeeff,0xff010203};source.setRGB(0,0,3,2,pixels,0,3);
        BufferedImage rightFacing=HorizontalOrientation.apply(source,true),nativeFacing=HorizontalOrientation.apply(source,false);
        assertEquals(3,rightFacing.getWidth());assertEquals(2,rightFacing.getHeight());assertEquals(3,nativeFacing.getWidth());assertEquals(2,nativeFacing.getHeight());
        for(int y=0;y<2;y++)for(int x=0;x<3;x++){assertEquals(pixels[y*3+x],source.getRGB(x,y));assertEquals(pixels[y*3+x],nativeFacing.getRGB(x,y));assertEquals(pixels[y*3+(2-x)],rightFacing.getRGB(x,y));}
    }

    @Test void exporterUsesDefaultAndExplicitProjectOrientation(){
        BufferedImage rendered=new BufferedImage(2,1,BufferedImage.TYPE_INT_ARGB);rendered.setRGB(0,0,0xffff0000);rendered.setRGB(1,0,0xff0000ff);SpriteProject project=new SpriteProject();
        BufferedImage defaultExport=SheetExporter.orientForExport(rendered,project);assertEquals(0xff0000ff,defaultExport.getRGB(0,0));assertEquals(0xffff0000,defaultExport.getRGB(1,0));
        project.mirroredPreview=false;BufferedImage nativeExport=SheetExporter.orientForExport(rendered,project);assertEquals(0xffff0000,nativeExport.getRGB(0,0));assertEquals(0xff0000ff,nativeExport.getRGB(1,0));
        assertEquals(0xffff0000,rendered.getRGB(0,0));assertEquals(0xff0000ff,rendered.getRGB(1,0));
    }
}
