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

    private static TextureProvider530 provider(Map<Integer,byte[]> graphs) {
        MaterialDefinition530[] definitions = new MaterialDefinition530[graphs.size()];
        for (int id = 0; id < definitions.length; id++) definitions[id] = new MaterialDefinition530(id,true,true,true,true,false,0,0,0,0,0);
        return new TextureProvider530(definitions, graphs::get);
    }

    private static byte[] colorFill(int rgb) {
        return new byte[]{1,0,1,1,1,0,(byte)(rgb>>>16),(byte)(rgb>>>8),(byte)rgb,0,0,0};
    }
}
