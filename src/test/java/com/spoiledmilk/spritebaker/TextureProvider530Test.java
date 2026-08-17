package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TextureProvider530Test {
    @Test void resolvesAndCachesNestedTextureDependency() throws Exception {
        TextureProvider530 provider = provider(Map.of(
            0, ProceduralTexture530DecoderTest.textureDependency(1),
            1, colorFill(0x336699)));
        TextureMaterial530 material = provider.material(0);
        assertEquals(0x336699, material.pixels[0]);
        assertEquals(java.util.List.of(36), material.operationTypes);
        assertEquals(2, provider.loaded().size());
        assertTrue(provider.material(0) == material);
    }

    @Test void rejectsRecursiveTextureDependencyWithDeterministicPath() {
        TextureProvider530 provider = provider(Map.of(
            0, ProceduralTexture530DecoderTest.textureDependency(1),
            1, ProceduralTexture530DecoderTest.textureDependency(0)));
        UnsupportedTextureFormatException error = assertThrows(UnsupportedTextureFormatException.class, () -> provider.material(0));
        assertTrue(error.getMessage().contains("recursive texture dependency 0 -> 1 -> 0"));
        assertTrue(provider.loaded().isEmpty());
    }

    @Test void decodesAndCachesOperation38WithoutLoadingAnotherMaterial() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.lineNoise()));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(38),material.operationTypes);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesOperation32WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.bumpLighting()));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(13,32),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(8947848,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorOperation5WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.boxBlurColor()));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(2,10,5),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(9594183,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesOperation4WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.brickTiles()));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(4),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(15921906,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorOperation6WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.clampColor(0)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(1,6),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x204060,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorOperation19WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.coordinateDisplacement(0,2048,4096,32767)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(3,10,30,30,19),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x13417e,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorOperation9WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.colorFlip(1,0,0)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(2,10,9),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x104080,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorOperation21WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.interpolateColor(1024,0)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(1,1,30,21),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x2898d8,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorOperation20WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.colorTile(64,1)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(2,10,20),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x104080,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesOperation27WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.stripes(2)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(27),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesOperation15WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.cellular(2,1,0x1234)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(15),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(9013641,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesOperation12WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.waveform(1,2,255)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(12),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x979797,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorCombineFunction6WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.colorCombine(6,0)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(1,1,7),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x10a0f0,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorCombineFunction1WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.colorCombine(1,0)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(1,1,7),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x60ffff,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorCombineFunction2WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.colorCombine(2,0)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(1,1,7),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x200000,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorCombineFunction5WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.colorCombine(5,0)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(1,1,7),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x58d0f8,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorCombineFunction7WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.colorCombine(7,0)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(1,1,7),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x2affff,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorCombineFunction10WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.colorCombine(10,0)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(1,1,7),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x40a0e0,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesColorCombineFunction8WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.colorCombine(8,0)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(1,1,7),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x0040d5,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesCurveMode1WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.curve(1,new int[][]{{0,0},{4096,4096}})));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(2,8),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0xffffff,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesOperation22WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.invertColor(0)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(1,22),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0xc08040,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void decodesAndCachesOperation17WithoutExternalDependencies() throws Exception {
        TextureProvider530 provider=provider(Map.of(0,ProceduralTexture530DecoderTest.hslAdjust(0x4080c0,3000,25,-25)));
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(1,17),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x1a7010,material.pixels[0]);
        assertEquals(1,provider.loaded().size());
        assertTrue(provider.material(0)==material);
    }

    @Test void resolvesAndCachesOperation39SpriteDependencies() throws Exception {
        MaterialDefinition530[] definitions={new MaterialDefinition530(0,true,true,true,true,false,0,0,0,0,0)};
        int[] calls={0};TextureProvider530 provider=new TextureProvider530(definitions,
            id->ProceduralTexture530DecoderTest.spriteDependency(321),
            id->{calls[0]++;assertEquals(321,id);return new ProceduralTexture530Decoder.SpriteDependency(2,2,new int[]{0xff0000,0x00ff00,0x0000ff,0xffffff});});
        TextureMaterial530 material=provider.material(0);
        assertEquals(java.util.List.of(39),material.operationTypes);
        assertEquals(64,material.size);
        assertEquals(0x00ff00,material.pixels[0]);
        assertEquals(1,calls[0]);
        assertTrue(provider.material(0)==material);
    }

    private static TextureProvider530 provider(Map<Integer,byte[]> graphs) {
        MaterialDefinition530[] definitions = new MaterialDefinition530[graphs.size()];
        for (int id = 0; id < definitions.length; id++) definitions[id] = new MaterialDefinition530(id,true,true,true,true,false,0,0,0,0,0);
        return new TextureProvider530(definitions, graphs::get);
    }

    private static byte[] colorFill(int rgb) {
        return new byte[]{1,0,1,1,1,0,(byte)(rgb>>>16),(byte)(rgb>>>8),(byte)rgb,0,0,0};
    }
}
