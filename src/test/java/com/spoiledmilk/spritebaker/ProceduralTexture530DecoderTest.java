package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class ProceduralTexture530DecoderTest {
    @Test void rendersNeutralGradientRangeFixtureDeterministically(){
        ByteArrayOutputStream b=new ByteArrayOutputStream();bytes(b,2); // nodes
        bytes(b,0,2,1,0); // horizontal gradient
        bytes(b,0,30,1,2,0,4,0,1,12,0,0); // range params, child 0
        bytes(b,1,0,0); // color, alpha and brightness roots
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(900,b.toByteArray(),4);
        assertArrayEquals(new int[]{0xa0a0a0,0x808080,0x606060,0x404040},java.util.Arrays.copyOfRange(decoded.pixels,0,4));
        assertEquals(java.util.List.of(2,30),decoded.operationTypes);
    }
    @Test void rejectsUnknownOperationWithoutSubstitution(){
        byte[] fixture={1,0,35,1,0};
        UnsupportedTextureFormatException e=assertThrows(UnsupportedTextureFormatException.class,()->new ProceduralTexture530Decoder().decode(901,fixture,4));
        assertTrue(e.getMessage().contains("operation 35"));
    }
    @Test void operation13ReproducesFixedPointHashNoiseAndRejectsParameters(){
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(905,hashNoise(),8);
        assertArrayEquals(new int[]{16053492,16053492,7500402,0,16185078,7697781,7763574,0,
            0,14803425,0,6250335,2105376,14540253,0,6184542},java.util.Arrays.copyOfRange(decoded.pixels,0,16));
        assertEquals(java.util.List.of(13),decoded.operationTypes);
        UnsupportedTextureFormatException error=assertThrows(UnsupportedTextureFormatException.class,
            ()->new ProceduralTexture530Decoder().decode(906,new byte[]{1,0,13,1,1,0,0,0,0},8));
        assertTrue(error.getMessage().contains("operation parameter 0 for HashNoise"));
    }
    @Test void operation34RendersDefaultSeededNoiseDeterministically(){
        byte[] fixture=defaultNoise();
        ProceduralTexture530Decoder.Decoded first=new ProceduralTexture530Decoder().decode(903,fixture,16);
        ProceduralTexture530Decoder.Decoded second=new ProceduralTexture530Decoder().decode(903,fixture,16);
        assertArrayEquals(first.pixels,second.pixels);
        assertArrayEquals(new int[]{11316396,8421504,5460819,8421504,10921638,12632256,12632256,8421504,
            5460819,8421504,11316396,8421504,5855577,4210752,4144959,8421504},java.util.Arrays.copyOfRange(first.pixels,0,16));
        assertEquals(java.util.List.of(34),first.operationTypes);
    }
    @Test void operation34HonorsExplicitAmplitudesScalesSeedAndNormalization(){
        byte[] fixture={1,0,34,1,7,0,0,1,3,2,(byte)255,(byte)255,16,0,8,0,4,0,3,3,4,7,5,5,6,2,0,0,0};
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(904,fixture,8);
        assertArrayEquals(new int[]{0,855309,4802889,0,2236962,0,4079166,0},java.util.Arrays.copyOfRange(decoded.pixels,0,8));
    }
    @Test void operation36SamplesNestedTextureAtDependencyResolution()throws Exception{
        byte[] fixture=textureDependency(7);
        int[] dependency={0x0000ff,0xff0000,0xffffff,0x000000}; // stored horizontally reversed
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(902,fixture,4,
            id->{assertEquals(7,id);return new ProceduralTexture530Decoder.Dependency(2,dependency);});
        assertArrayEquals(new int[]{0x0000ff,0x0000ff,0xff0000,0xff0000},java.util.Arrays.copyOfRange(decoded.pixels,0,4));
        assertEquals(java.util.List.of(36),decoded.operationTypes);
    }
    @Test void operation38DrawsParameterizedWrappedLinesWithoutDependencies()throws Exception{
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(907,lineNoise(),8,
            id->{throw new AssertionError("operation 38 must not resolve texture "+id);});
        assertArrayEquals(new int[]{0,5263440,0,0,0,0,0,0,0,0,4408131,0,0,0,0,0},
            java.util.Arrays.copyOfRange(decoded.pixels,0,16));
        assertEquals(java.util.List.of(38),decoded.operationTypes);
        UnsupportedTextureFormatException error=assertThrows(UnsupportedTextureFormatException.class,
            ()->new ProceduralTexture530Decoder().decode(908,new byte[]{1,0,38,1,1,5,0,0,0},8));
        assertTrue(error.getMessage().contains("operation parameter 5 for LineNoise"));
    }
    @Test void operation32LightsItsMonochromeInputWithExactFixedPointParameters(){
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(909,bumpLighting(),8);
        assertArrayEquals(new int[]{5526612,7895160,0,7500402,11184810,7829367,0,7105644,
            2500134,3421236,3815994,10921638,2236962,0,3815994,7039851},java.util.Arrays.copyOfRange(decoded.pixels,0,16));
        assertTrue(java.util.Arrays.stream(decoded.pixels).allMatch(pixel->(pixel>>16&255)==(pixel>>8&255)&&(pixel>>8&255)==(pixel&255)));
        assertEquals(java.util.List.of(13,32),decoded.operationTypes);
        UnsupportedTextureFormatException error=assertThrows(UnsupportedTextureFormatException.class,
            ()->new ProceduralTexture530Decoder().decode(910,new byte[]{1,0,32,1,1,3,0,0,0,0},8));
        assertTrue(error.getMessage().contains("operation parameter 3 for BumpLighting"));
    }
    @Test void operation5WrapsAndTruncatesEachMonochromeBlurPass(){
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(911,boxBlurMonochrome(),8);
        assertArrayEquals(new int[]{3618615,2500134,3026478,3487029,2631720,2171169,3092271,3947580,
            5723991,5395026,6513507,6118749,4144959,3289650,3684408,4868682},java.util.Arrays.copyOfRange(decoded.pixels,0,16));
        assertTrue(java.util.Arrays.stream(decoded.pixels).allMatch(pixel->(pixel>>16&255)==(pixel>>8&255)&&(pixel>>8&255)==(pixel&255)));
        assertEquals(java.util.List.of(13,5),decoded.operationTypes);
    }
    @Test void operation5PreservesColorChannelsAndRejectsUnknownParameters(){
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(912,boxBlurColor(),8);
        assertArrayEquals(new int[]{8019538,9134923,10184515,8347471,6510427,4673383,5788768,6904153},
            java.util.Arrays.copyOfRange(decoded.pixels,0,8));
        assertTrue(java.util.Arrays.stream(decoded.pixels).anyMatch(pixel->(pixel>>16&255)!=(pixel>>8&255)));
        assertEquals(java.util.List.of(2,10,5),decoded.operationTypes);
        UnsupportedTextureFormatException error=assertThrows(UnsupportedTextureFormatException.class,
            ()->new ProceduralTexture530Decoder().decode(913,new byte[]{1,0,5,1,1,3,0,0,0,0},8));
        assertTrue(error.getMessage().contains("operation parameter 3 for BoxBlur"));
    }
    static byte[] hashNoise(){return new byte[]{1,0,13,1,0,0,0,0};}
    static byte[] defaultNoise(){return new byte[]{1,0,34,1,0,0,0,0};}
    static byte[] textureDependency(int id){return new byte[]{1,0,36,1,1,0,(byte)(id>>>8),(byte)id,0,0,0};}
    static byte[] lineNoise(){return new byte[]{1,0,38,1,5,0,7,1,0,6,2,4,3,4,0,4,8,0,0,0,0};}
    static byte[] bumpLighting(){return new byte[]{2,0,13,1,0,0,32,1,3,0,6,0,1,10,0,2,4,0,0,1,0,0};}
    static byte[] boxBlurMonochrome(){return new byte[]{2,0,13,1,0,0,5,1,3,0,2,1,1,2,1,0,1,0,0};}
    static byte[] boxBlurColor(){return new byte[]{3,0,2,1,0,0,10,1,1,0,0,2,0,0,16,64,(byte)128,16,0,(byte)240,(byte)128,32,0,0,5,1,3,0,2,1,1,2,0,1,2,0,0};}
    private static void bytes(ByteArrayOutputStream out,int... values){for(int value:values)out.write(value);}
}
