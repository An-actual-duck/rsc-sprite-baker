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
    @Test void combineFunction6PreservesOperandOrderingColorAndMonochromeModes(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded color=decoder.decode(928,colorCombine(6,0),4);
        ProceduralTexture530Decoder.Decoded monochrome=decoder.decode(929,colorCombine(6,1),4);
        assertTrue(java.util.Arrays.stream(color.pixels).allMatch(pixel->pixel==0x10a0f0));
        assertTrue(java.util.Arrays.stream(monochrome.pixels).allMatch(pixel->pixel==0x101010));
        assertArrayEquals(color.pixels,decoder.decode(930,colorCombine(6,2),4).pixels);
        assertEquals(java.util.List.of(1,1,7),color.operationTypes);
    }
    @Test void combineFunction6UsesJavaOverflowThenFinalTextureClamp(){
        ProceduralTexture530Decoder.Decoded high=new ProceduralTexture530Decoder().decode(931,monochromeCombine(6,1,65535,65535),4);
        assertTrue(java.util.Arrays.stream(high.pixels).allMatch(pixel->pixel==0xffffff));
        ProceduralTexture530Decoder.Decoded ordered=new ProceduralTexture530Decoder().decode(932,monochromeCombine(6,1,1024,3072),4);
        ProceduralTexture530Decoder.Decoded reversed=new ProceduralTexture530Decoder().decode(933,monochromeCombine(6,1,3072,1024),4);
        assertTrue(java.util.Arrays.stream(ordered.pixels).allMatch(pixel->pixel==0xa0a0a0));
        assertTrue(java.util.Arrays.stream(reversed.pixels).allMatch(pixel->pixel==0x606060));
    }
    @Test void combineFunction3RemainsExactAndOtherFunctionsFailClosed(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertTrue(java.util.Arrays.stream(decoder.decode(934,colorCombine(3,0),4).pixels).allMatch(pixel->pixel==0x0850a8));
        assertTrue(java.util.Arrays.stream(decoder.decode(935,colorCombine(3,1),4).pixels).allMatch(pixel->pixel==0x080808));
        for(int function=0;function<=12;function++)if(function!=3&&function!=6){
            int unsupported=function;
            UnsupportedTextureFormatException error=assertThrows(UnsupportedTextureFormatException.class,
                ()->decoder.decode(936,colorCombine(unsupported,0),4));
            assertTrue(error.getMessage().contains("combine function "+function));
        }
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,
            ()->decoder.decode(937,new byte[]{1,0,7,1,1,2,0,0,0,0,0},4));
        assertTrue(parameter.getMessage().contains("operation parameter 2 for Combine"));
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
    @Test void operation5ColorOutputFeedsMonochromeConsumersFromItsFirstChannel(){
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(914,boxBlurColorThenCurve(),8);
        assertArrayEquals(new int[]{8026746,9145227,10197915,8355711,6513507,4671303,5789784,6908265},
            java.util.Arrays.copyOfRange(decoded.pixels,0,8));
        assertEquals(java.util.List.of(2,10,5,8),decoded.operationTypes);
    }
    @Test void operation4GeneratesParameterizedRandomizedWrappedTilesWithoutDependencies()throws Exception{
        ProceduralTexture530Decoder.Decoded first=new ProceduralTexture530Decoder().decode(915,brickTiles(),8,
            id->{throw new AssertionError("operation 4 must not resolve texture "+id);});
        ProceduralTexture530Decoder.Decoded second=new ProceduralTexture530Decoder().decode(915,brickTiles(),8);
        assertArrayEquals(first.pixels,second.pixels);
        assertArrayEquals(new int[]{15921906,0,14540253,14540253,14869218,14869218,14869218,15921906,
            0,0,0,0,0,0,0,0},java.util.Arrays.copyOfRange(first.pixels,0,16));
        assertTrue(java.util.Arrays.stream(first.pixels).allMatch(pixel->(pixel>>16&255)==(pixel>>8&255)&&(pixel>>8&255)==(pixel&255)));
        assertEquals(java.util.List.of(4),first.operationTypes);
    }
    @Test void operation4RejectsUnknownParametersAndEmptyGrids(){
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,
            ()->new ProceduralTexture530Decoder().decode(916,new byte[]{1,0,4,1,1,8,0,0,0},8));
        assertTrue(parameter.getMessage().contains("operation parameter 8 for BrickTiles"));
        UnsupportedTextureFormatException grid=assertThrows(UnsupportedTextureFormatException.class,
            ()->new ProceduralTexture530Decoder().decode(917,new byte[]{1,0,4,1,1,0,0,0,0,0},8));
        assertTrue(grid.getMessage().contains("brick grid 0x8"));
    }
    @Test void operation27TransformsParameterizedStripesForEveryPinnedMode()throws Exception{
        int[][] expected={
            {16777215,16777215,16777215,16777215,16777215,16777215,16777215,16777215,0,0,0,0,0,0,0,0},
            {0,16777215,0,0,16777215,0,0,16777215,0,16777215,0,0,16777215,0,0,16777215},
            {16777215,16777215,0,0,0,0,16777215,16777215,0,16777215,16777215,0,0,0,0,16777215},
            {0,0,0,16777215,16777215,0,0,0,0,0,16777215,16777215,0,0,0,16777215}};
        for(int mode=0;mode<expected.length;mode++){
            ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(918,stripes(mode),8,
                id->{throw new AssertionError("operation 27 must not resolve texture "+id);});
            assertArrayEquals(expected[mode],java.util.Arrays.copyOfRange(decoded.pixels,0,16),"mode "+mode);
            assertTrue(java.util.Arrays.stream(decoded.pixels).allMatch(pixel->pixel==0||pixel==0xffffff));
            assertEquals(java.util.List.of(27),decoded.operationTypes);
        }
    }
    @Test void operation27PreservesPinnedUnknownModeAndRejectsInvalidSerialization(){
        ProceduralTexture530Decoder.Decoded unknownMode=new ProceduralTexture530Decoder().decode(919,stripes(4),8);
        assertTrue(java.util.Arrays.stream(unknownMode.pixels).allMatch(pixel->pixel==0xffffff));
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,
            ()->new ProceduralTexture530Decoder().decode(920,new byte[]{1,0,27,1,1,3,0,0,0},8));
        assertTrue(parameter.getMessage().contains("operation parameter 3 for Stripes"));
        UnsupportedTextureFormatException bands=assertThrows(UnsupportedTextureFormatException.class,
            ()->new ProceduralTexture530Decoder().decode(921,new byte[]{1,0,27,1,1,0,0,0,0,0},8));
        assertTrue(bands.getMessage().contains("stripe band count 0"));
    }
    @Test void operation15ReproducesEveryPinnedDistanceMetricWithoutDependencies()throws Exception{
        int[][] expected={
            {5723991,5197647,7829367,1052688,8421504,16250871,5460819,6776679},
            {3487029,2763306,8092539,1447446,10395294,16777215,6250335,7829367},
            {6250335,6316128,9605778,1973790,9605778,16777215,5987163,6579300},
            {5723991,5131854,7237230,921102,5921370,13027014,5526612,7105644},
            {9868950,11513775,16053492,4605510,10132122,16777215,9605778,8816262},
            {5395026,5263440,5592405,592137,5460819,11974326,3618615,4539717}};
        for(int metric=0;metric<expected.length;metric++){
            ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(922,cellular(2,metric,0x1234),8,
                id->{throw new AssertionError("operation 15 must not resolve texture "+id);});
            assertArrayEquals(expected[metric],java.util.Arrays.copyOfRange(decoded.pixels,0,8),"metric "+metric);
            assertTrue(java.util.Arrays.stream(decoded.pixels).allMatch(pixel->(pixel>>16&255)==(pixel>>8&255)&&(pixel>>8&255)==(pixel&255)));
            assertEquals(java.util.List.of(15),decoded.operationTypes);
        }
    }
    @Test void operation15ReproducesEveryPinnedNearestDistanceSelector(){
        int[][] expected={
            {263172,197379,1315860,7237230,2236962,131586,2763306,2302755},
            {3750201,2960685,9474192,8684676,12698049,16777215,9079434,10132122},
            {3487029,2763306,8092539,1447446,10395294,16777215,6250335,7829367},
            {14145495,10395294,10395294,9079434,16316664,16777215,16777215,16777215},
            {16777215,15000804,11382189,16777215,16777215,16777215,16777215,16777215}};
        for(int selector=0;selector<expected.length;selector++){
            ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(923,cellular(selector,1,0x1234),8);
            assertArrayEquals(expected[selector],java.util.Arrays.copyOfRange(decoded.pixels,0,8),"selector "+selector);
        }
    }
    @Test void operation15PreservesPinnedFallbackModesSignedOffsetsAndFailClosedParameters(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertArrayEquals(decoder.decode(924,cellular(2,0,0x1234),8).pixels,decoder.decode(924,cellular(2,6,0x1234),8).pixels);
        assertTrue(java.util.Arrays.stream(decoder.decode(925,cellular(5,1,0x1234),8).pixels).allMatch(pixel->pixel==0));
        int[] signedOffsets=decoder.decode(926,cellular(2,1,0xffff),8).pixels;
        assertArrayEquals(new int[]{16777215,16777215,16777215,16777215,16777215,16777215,16777215,16777215},java.util.Arrays.copyOfRange(signedOffsets,0,8));
        assertEquals(10921638,signedOffsets[25]);
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,
            ()->decoder.decode(927,new byte[]{1,0,15,1,1,7,0,0,0},8));
        assertTrue(parameter.getMessage().contains("operation parameter 7 for CellularNoise"));
    }
    static byte[] hashNoise(){return new byte[]{1,0,13,1,0,0,0,0};}
    static byte[] defaultNoise(){return new byte[]{1,0,34,1,0,0,0,0};}
    static byte[] textureDependency(int id){return new byte[]{1,0,36,1,1,0,(byte)(id>>>8),(byte)id,0,0,0};}
    static byte[] lineNoise(){return new byte[]{1,0,38,1,5,0,7,1,0,6,2,4,3,4,0,4,8,0,0,0,0};}
    static byte[] bumpLighting(){return new byte[]{2,0,13,1,0,0,32,1,3,0,6,0,1,10,0,2,4,0,0,1,0,0};}
    static byte[] boxBlurMonochrome(){return new byte[]{2,0,13,1,0,0,5,1,3,0,2,1,1,2,1,0,1,0,0};}
    static byte[] boxBlurColor(){return new byte[]{3,0,2,1,0,0,10,1,1,0,0,2,0,0,16,64,(byte)128,16,0,(byte)240,(byte)128,32,0,0,5,1,3,0,2,1,1,2,0,1,2,0,0};}
    static byte[] boxBlurColorThenCurve(){return new byte[]{4,0,2,1,0,0,10,1,1,0,0,2,0,0,16,64,(byte)128,16,0,(byte)240,(byte)128,32,0,0,5,1,3,0,2,1,1,2,0,1,0,8,1,1,0,0,2,0,0,0,0,16,0,16,0,2,3,0,0};}
    static byte[] brickTiles(){return new byte[]{1,0,4,1,8,0,3,1,5,2,2,0,3,1,0,4,4,0,5,1,44,6,0,(byte)128,7,3,32,0,0,0};}
    static byte[] stripes(int mode){return new byte[]{1,0,27,1,3,0,3,1,6,0,2,(byte)mode,0,0,0};}
    static byte[] cellular(int selector,int metric,int jitter){return new byte[]{1,0,15,1,7,0,4,1,7,2,(byte)(jitter>>>8),(byte)jitter,3,(byte)selector,4,(byte)metric,5,3,6,6,0,0,0};}
    static byte[] colorCombine(int function,int outputMode){return new byte[]{3,0,1,1,1,0,64,(byte)128,(byte)192,0,1,1,1,0,32,(byte)160,(byte)224,0,7,1,2,0,(byte)function,1,(byte)outputMode,0,1,2,0,0};}
    static byte[] monochromeCombine(int function,int outputMode,int first,int second){return new byte[]{3,0,0,1,1,0,(byte)(first>>>8),(byte)first,0,0,1,1,0,(byte)(second>>>8),(byte)second,0,7,1,2,0,(byte)function,1,(byte)outputMode,0,1,2,0,0};}
    private static void bytes(ByteArrayOutputStream out,int... values){for(int value:values)out.write(value);}
}
