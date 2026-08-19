package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.List;
import net.runelite.cache.definitions.ModelDefinition;
import org.junit.jupiter.api.Test;

class StaticRendererTest {
    @Test void rscStyleUsesSignedNormalsSoUndersidesCannotBecomeHighlights(){
        ModelDefinition fixture=neutralModel();NpcDefinition530 npc=new NpcDefinition530(1);
        double underside=StaticRenderer.styleFaceBrightness(fixture,0,npc,new double[]{0,1,0},.54,.36);
        fixture.faceIndices2[0]=2;fixture.faceIndices3[0]=1;
        double upward=StaticRenderer.styleFaceBrightness(fixture,0,npc,new double[]{0,1,0},.54,.36);
        assertEquals(.54,underside,1e-12);assertTrue(upward>underside);
    }

    @Test void revision530RenderType2MarkerFaceIsNotRasterized(){
        ModelDefinition fixture=neutralModel();fixture.faceColors=new short[]{(short)65535};fixture.faceRenderTypes=new byte[]{2};
        NpcDefinition530 npc=new NpcDefinition530(1615);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;
        BufferedImage image=new StaticRenderer().renderStyled(List.of(fixture),npc,0,null,settings);
        assertTrue(java.util.Arrays.stream(image.getRGB(0,0,40,40,null,0,40)).allMatch(pixel->pixel==0));
        assertTrue(StaticRenderer.hiddenFace(fixture,0));
    }

    @Test void packedColor65535IsStillRenderedWhenFaceTypeIsDrawable(){
        ModelDefinition fixture=neutralModel();fixture.faceColors=new short[]{(short)65535};fixture.faceRenderTypes=new byte[]{0};
        NpcDefinition530 npc=new NpcDefinition530(2783);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;
        BufferedImage image=new StaticRenderer().renderStyled(List.of(fixture),npc,0,null,settings);
        assertTrue(java.util.Arrays.stream(image.getRGB(0,0,40,40,null,0,40)).anyMatch(pixel->(pixel>>>24)!=0));
        assertFalse(StaticRenderer.hiddenFace(fixture,0));
    }

    @Test void hiddenTexturedFaceDoesNotResolveOrSubstituteItsMaterial(){
        ModelDefinition fixture=neutralModel();fixture.faceColors=new short[]{123};fixture.faceRenderTypes=new byte[]{2};fixture.faceTextures=new short[]{12};
        NpcDefinition530 npc=new NpcDefinition530(1);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;
        BufferedImage image=new StaticRenderer().renderStyled(List.of(fixture),npc,0,null,settings);
        assertTrue(java.util.Arrays.stream(image.getRGB(0,0,40,40,null,0,40)).allMatch(pixel->pixel==0));
    }

    @Test
    void rendersGeneratedNeutralMeshDeterministicallyOnTransparency() {
        ModelDefinition fixture = new ModelDefinition();
        fixture.id = 9000;
        fixture.vertexCount = 4;
        fixture.vertexX = new int[] {-40, 40, 0, 0};
        fixture.vertexY = new int[] {0, 0, 0, -80};
        fixture.vertexZ = new int[] {-30, -30, 40, 0};
        fixture.faceCount = 4;
        fixture.faceIndices1 = new int[] {0, 0, 1, 2};
        fixture.faceIndices2 = new int[] {2, 1, 2, 0};
        fixture.faceIndices3 = new int[] {1, 3, 3, 3};
        fixture.faceColors = new short[] {5000, 5000, 5000, 5000};

        NpcDefinition530 npc = new NpcDefinition530(1);
        StaticRenderer renderer = new StaticRenderer();
        BufferedImage first = renderer.render(List.of(fixture), npc);
        BufferedImage second = renderer.render(List.of(fixture), npc);

        assertEquals(StaticRenderer.WIDTH, first.getWidth());
        assertEquals(StaticRenderer.HEIGHT, first.getHeight());
        assertEquals(0, first.getRGB(0, 0));
        int[] firstPixels = first.getRGB(0, 0, first.getWidth(), first.getHeight(), null, 0, first.getWidth());
        int[] secondPixels = second.getRGB(0, 0, second.getWidth(), second.getHeight(), null, 0, second.getWidth());
        assertArrayEquals(firstPixels, secondPixels);
        assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel -> (pixel >>> 24) == 255));
    }

    @Test
    void rendersStyledTargetSizeFromSharedSupersampledViewport() {
        ModelDefinition fixture = neutralModel();
        NpcDefinition530 npc = new NpcDefinition530(1);
        VisualSettings settings = new VisualSettings();
        settings.cellWidth = 48; settings.cellHeight = 56; settings.supersample = 3;
        settings.palette = PaletteReducer.UNMODIFIED; settings.dithering = PaletteReducer.NO_DITHER;
        StaticRenderer renderer = new StaticRenderer();
        StaticRenderer.Viewport viewport = renderer.fitStyled(
            List.of(new StaticRenderer.View(fixture, 0), new StaticRenderer.View(fixture, 90)), npc, settings);
        BufferedImage first = renderer.renderStyled(List.of(fixture), npc, 0, viewport, settings);
        BufferedImage second = renderer.renderStyled(List.of(fixture), npc, 0, viewport, settings);
        assertEquals(48, first.getWidth()); assertEquals(56, first.getHeight());
        assertArrayEquals(first.getRGB(0,0,48,56,null,0,48),second.getRGB(0,0,48,56,null,0,48));
    }

    @Test void rendersNeutralTextureAndHonorsAlphaTestDeterministically() {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{12};fixture.textureCoords=new byte[]{-1};
        NpcDefinition530 npc=new NpcDefinition530(2);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        MaterialDefinition530 opaque=new MaterialDefinition530(12,true,true,true,false,false,0,0,0,0,0);
        TextureMaterial530 stripes=new TextureMaterial530(opaque,2,new int[]{0xff0000,0x00ff00,0x0000ff,0xffffff},List.of(1));
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,id->stripes),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,id->stripes);
        assertArrayEquals(first.getRGB(0,0,40,40,null,0,40),second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(first.getRGB(0,0,40,40,null,0,40)).anyMatch(pixel->(pixel>>>24)==255));

        MaterialDefinition530 tested=new MaterialDefinition530(12,true,true,false,false,false,0,0,0,0,0);
        TextureMaterial530 transparentBlack=new TextureMaterial530(tested,1,new int[]{0},List.of(0));BufferedImage empty=renderer.renderStyled(List.of(fixture),npc,0,null,settings,id->transparentBlack);
        assertTrue(java.util.Arrays.stream(empty.getRGB(0,0,40,40,null,0,40)).allMatch(pixel->pixel==0));
    }

    @Test void texturedFacesRetainPinnedPackedHslColorModulation(){
        ModelDefinition fixture=neutralModel();fixture.faceColors=new short[]{(short)((7<<7)|64)};fixture.faceTextures=new short[]{12};fixture.textureCoords=new byte[]{-1};
        NpcDefinition530 npc=new NpcDefinition530(25);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;settings.ambient=1;settings.diffuse=0;
        MaterialDefinition530 definition=new MaterialDefinition530(12,true,true,true,false,false,0,0,0,0,0);TextureMaterial530 white=new TextureMaterial530(definition,1,new int[]{0xffffff},List.of(0));
        BufferedImage image=new StaticRenderer().renderStyled(List.of(fixture),npc,0,null,settings,id->white);
        assertTrue(java.util.Arrays.stream(image.getRGB(0,0,40,40,null,0,40)).anyMatch(pixel->(pixel>>>24)==255&&((pixel>>>16)&255)>180&&((pixel>>>16)&255)>((pixel>>>8)&255)*3));
    }

    @Test void pinnedMaterialColorBytesBlendTowardGrayAndBoostChannels(){
        int redHsl=(7<<7)|64;MaterialDefinition530 plain=new MaterialDefinition530(1,true,true,true,false,false,0,0,0,0,0),modified=new MaterialDefinition530(1,true,true,true,false,false,64,(byte)128,0,0,0);
        int base=StaticRenderer.texturedModulation(redHsl,plain,1.0),changed=StaticRenderer.texturedModulation(redHsl,modified,1.0);
        assertEquals(0xfa2916,base);assertEquals(0xffb9ac,changed);
    }

    @Test void rendersOperation34MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.defaultNoise());
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(34),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(3);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation13MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.hashNoise());
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(13),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(4);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation38MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.lineNoise());
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(38),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(5);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation32MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.bumpLighting());
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(13,32),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(6);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation5MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.boxBlurColor());
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(2,10,5),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(7);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation4MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.brickTiles());
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(4),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(8);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation27MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.stripes(2));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(27),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(9);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation6MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.clampColor(0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,6),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(13);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation19MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.coordinateDisplacement(0,2048,4096,32767));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(3,10,30,30,19),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(14);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation17MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.hslAdjust(0x4080c0,3000,25,-25));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,17),material.operationTypes);assertEquals(0x1a7010,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(17);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation9MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.colorFlip(1,0,0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(2,10,9),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(15);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation21MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.interpolateColor(1024,0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,1,30,21),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(16);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation20MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.colorTile(2,1));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(2,10,20),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(17);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation15MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.cellular(2,1,0x1234));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(15),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(10);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation12MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.waveform(1,2,255));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(12),material.operationTypes);assertEquals(0x979797,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(18);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersCombineFunction6MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.colorCombine(6,0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,1,7),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(11);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersCombineFunction1MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.colorCombine(1,0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,1,7),material.operationTypes);
        NpcDefinition530 npc=new NpcDefinition530(12);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersCombineFunction2MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.colorCombine(2,0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,1,7),material.operationTypes);assertEquals(0x200000,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(13);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersCombineFunction5MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.colorCombine(5,0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,1,7),material.operationTypes);assertEquals(0x58d0f8,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(14);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersCombineFunction7MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.colorCombine(7,0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,1,7),material.operationTypes);assertEquals(0x2affff,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(17);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersCombineFunction10MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.colorCombine(10,0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,1,7),material.operationTypes);assertEquals(0x40a0e0,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(19);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersCombineFunction9MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.colorCombine(9,0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,1,7),material.operationTypes);assertEquals(0x2080c0,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(24);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersCombineFunction11MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.colorCombine(11,0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,1,7),material.operationTypes);assertEquals(0x202020,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(23);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersCombineFunction8MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.colorCombine(8,0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,1,7),material.operationTypes);assertEquals(0x0040d5,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(21);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersCurveMode1MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.curve(1,new int[][]{{0,0},{4096,4096}}));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(2,8),material.operationTypes);assertEquals(0xffffff,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(20);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersCurveMode2MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.curve(2,new int[][]{{0,0},{4096,4096}}));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(2,8),material.operationTypes);assertEquals(0xffffff,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(22);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation22MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.invertColor(0));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(1,22),material.operationTypes);assertEquals(0xc08040,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(15);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersTexture334StyleOneSampleGradientThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.colorGradient(new int[][]{{0,32,160,224}}));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(2,10),material.operationTypes);assertEquals(0x20a0e0,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(23);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void rendersOperation39MaterialThroughCacheProviderDeterministically() throws Exception {
        ModelDefinition fixture=neutralModel();fixture.faceTextures=new short[]{0};fixture.textureCoords=new byte[]{-1};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0);
        TextureProvider530 provider=new TextureProvider530(new MaterialDefinition530[]{definition},id->ProceduralTexture530DecoderTest.spriteDependency(321),
            id->new ProceduralTexture530Decoder.SpriteDependency(2,2,new int[]{0xff0000,0x00ff00,0x0000ff,0xffffff}));
        TextureMaterial530 material=provider.material(0);assertEquals(List.of(39),material.operationTypes);assertEquals(0x00ff00,material.pixels[0]);
        NpcDefinition530 npc=new NpcDefinition530(16);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();BufferedImage first=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider),second=renderer.renderStyled(List.of(fixture),npc,0,null,settings,provider);
        int[] firstPixels=first.getRGB(0,0,40,40,null,0,40);assertArrayEquals(firstPixels,second.getRGB(0,0,40,40,null,0,40));assertTrue(java.util.Arrays.stream(firstPixels).anyMatch(pixel->(pixel>>>24)==255));
    }

    private static ModelDefinition neutralModel() {
        ModelDefinition fixture = new ModelDefinition(); fixture.id=9001; fixture.vertexCount=3;
        fixture.vertexX=new int[]{-20,20,0};fixture.vertexY=new int[]{0,0,-50};fixture.vertexZ=new int[]{0,0,10};
        fixture.faceCount=1;fixture.faceIndices1=new int[]{0};fixture.faceIndices2=new int[]{1};fixture.faceIndices3=new int[]{2};fixture.faceColors=new short[]{5000};return fixture;
    }
}
