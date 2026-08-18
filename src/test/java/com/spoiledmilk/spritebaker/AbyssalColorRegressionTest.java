package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import net.runelite.cache.definitions.ModelDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AbyssalColorRegressionTest {
    @Test void abyssalRecolorRetainsPinnedDarkRedThroughPreviewAndPng(@TempDir Path directory)throws Exception{
        ModelDefinition model=triangle(4015);
        NpcDefinition530 npc=new NpcDefinition530(1615);
        npc.name="Abyssal demon";npc.recolorFrom=new short[]{4015};npc.recolorTo=new short[]{528};
        VisualSettings settings=new VisualSettings();settings.cellWidth=48;settings.cellHeight=48;settings.supersample=1;settings.padding=4;settings.ambient=1;settings.diffuse=0;
        BufferedImage sprite=new StaticRenderer().renderStyled(List.of(model),npc,0,null,settings);
        int[] visible=Arrays.stream(sprite.getRGB(0,0,48,48,null,0,48)).filter(pixel->(pixel>>>24)!=0).toArray();
        assertTrue(visible.length>100);
        assertTrue(Arrays.stream(visible).allMatch(pixel->(pixel&0xffffff)!=0));
        assertTrue(Arrays.stream(visible).anyMatch(pixel->((pixel>>>16)&255)>((pixel>>>8)&255)*3/2));

        int[] transparentExport=sprite.getRGB(0,0,48,48,null,0,48);
        BufferedImage preview=PreviewCompositor.over(sprite,new Color(96,96,96),false);
        assertArrayEquals(transparentExport,sprite.getRGB(0,0,48,48,null,0,48));
        assertTrue(Arrays.stream(preview.getRGB(0,0,48,48,null,0,48)).anyMatch(pixel->((pixel>>>16)&255)>((pixel>>>8)&255)*3/2));

        Path png=directory.resolve("npc-1615.png");assertTrue(ImageIO.write(sprite,"png",png.toFile()));
        BufferedImage decoded=ImageIO.read(png.toFile());
        assertArrayEquals(transparentExport,decoded.getRGB(0,0,48,48,null,0,48));
    }

    @Test void revision530DefaultBrightnessConversionIsExactAndDeterministic(){
        assertEquals(0x512421,StaticRenderer.packedHslToRgb(528));
        assertEquals(StaticRenderer.packedHslToRgb(528),StaticRenderer.packedHslToRgb(528));
    }

    private static ModelDefinition triangle(int packedHsl){
        ModelDefinition model=new ModelDefinition();model.id=5062;model.vertexCount=3;
        model.vertexX=new int[]{-20,20,0};model.vertexY=new int[]{0,0,-50};model.vertexZ=new int[]{0,0,10};
        model.faceCount=1;model.faceIndices1=new int[]{0};model.faceIndices2=new int[]{1};model.faceIndices3=new int[]{2};model.faceColors=new short[]{(short)packedHsl};return model;
    }
}
