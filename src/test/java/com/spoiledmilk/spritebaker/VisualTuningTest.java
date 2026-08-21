package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class VisualTuningTest {
    @Test void defaultVariationIsPixelExactAndLowerValuesRemoveOnlySameFacetSpeckles(){
        BufferedImage source=new BufferedImage(3,3,BufferedImage.TYPE_INT_ARGB);int[] surfaces=new int[9],facets=new int[9];Arrays.fill(surfaces,7);Arrays.fill(facets,4);for(int y=0;y<3;y++)for(int x=0;x<3;x++)source.setRGB(x,y,0xff646464);source.setRGB(1,1,0xffc8c8c8);
        assertSame(source,VisualTuning.colorVariation(source,surfaces,facets,1));BufferedImage flat=VisualTuning.colorVariation(source,surfaces,facets,0);assertEquals(0xff646464,flat.getRGB(1,1));assertEquals(0xffc8c8c8,source.getRGB(1,1));
        facets[4]=9;BufferedImage protectedDetail=VisualTuning.colorVariation(source,surfaces,facets,0);assertEquals(0xffc8c8c8,protectedDetail.getRGB(1,1));
    }

    @Test void variationCanBeEmphasizedAndNeverChangesDimensionsOrAlpha(){
        BufferedImage source=new BufferedImage(3,3,BufferedImage.TYPE_INT_ARGB);int[] surfaces=new int[9],facets=new int[9];Arrays.fill(surfaces,2);Arrays.fill(facets,3);for(int y=0;y<3;y++)for(int x=0;x<3;x++)source.setRGB(x,y,0x80646464);source.setRGB(1,1,0x80828282);BufferedImage output=VisualTuning.colorVariation(source,surfaces,facets,1.5);assertEquals(145,(output.getRGB(1,1)>>>16)&255);assertEquals(0x80,output.getRGB(1,1)>>>24);assertEquals(3,output.getWidth());assertEquals(3,output.getHeight());
    }

    @Test void colorIntensityDefaultsExactAndZeroProducesDeterministicGray(){
        BufferedImage source=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);source.setRGB(0,0,0x7fc04020);assertSame(source,VisualTuning.colorIntensity(source,1));BufferedImage gray=VisualTuning.colorIntensity(source,0);int pixel=gray.getRGB(0,0),red=(pixel>>>16)&255;assertEquals(red,(pixel>>>8)&255);assertEquals(red,pixel&255);assertEquals(0x7f,pixel>>>24);
    }
}
