package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class ProceduralTexture530DecoderTest {
    @Test void operation0ReadsOneUnsignedByteAndUsesPinnedFixedPointScaling(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        for(int value:new int[]{0,1,127,128,203,254,255}){
            int channel=Math.min(255,((value<<12)/255)>>4),pixel=(channel<<16)|(channel<<8)|channel;
            assertTrue(java.util.Arrays.stream(decoder.decode(899,monochromeFill(value),4).pixels).allMatch(actual->actual==pixel),"value "+value);
        }
    }
    @Test void operation0TruncationAndUnknownParametersRemainFailClosed(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        UnsupportedTextureFormatException truncated=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(899,new byte[]{1,0,0,1,1,0},4));
        assertTrue(truncated.getMessage().contains("truncated operation 0 parameter 0"));
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(899,new byte[]{1,0,0,1,1,1,0,0,0},4));
        assertTrue(parameter.getMessage().contains("operation parameter 1 for Fill"));
    }
    @Test void curveMode1UsesPinnedCosineTableAndDeterministicFixedPointInterpolation(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();byte[] graph=curve(1,new int[][]{{0,0},{4096,4096}});
        ProceduralTexture530Decoder.Decoded first=decoder.decode(1025,graph,4),second=decoder.decode(1025,graph,4);
        assertArrayEquals(new int[]{0xdbdbdb,0x808080,0x252525,0},java.util.Arrays.copyOfRange(first.pixels,0,4));
        assertArrayEquals(first.pixels,second.pixels);assertEquals(java.util.List.of(2,8),first.operationTypes);
    }
    @Test void curveMode1SelectsRawMarkerSegmentsAtBoundariesAndClampsSignedShortTable(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertArrayEquals(new int[]{0x808080,0,0x7f7f7f,0xffffff},java.util.Arrays.copyOfRange(
            decoder.decode(1026,curve(1,new int[][]{{0,4096},{2048,0},{4096,4096}}),4).pixels,0,4));
        assertTrue(java.util.Arrays.stream(decoder.decode(1027,curveOverConstant(65535,new int[][]{{0,0},{4096,2048}}),4).pixels).allMatch(pixel->pixel==0x7f7f7f));
        assertTrue(java.util.Arrays.stream(decoder.decode(1027,curve(1,new int[][]{{0,65535},{4096,65535}}),4).pixels).allMatch(pixel->pixel==0xffffff));
    }
    @Test void curveMode1PreservesMinimalUnsortedAndDuplicateMarkerBehavior(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertArrayEquals(new int[]{0xa1a1a1,0x404040,0x9f9f9f,0xffffff},java.util.Arrays.copyOfRange(
            decoder.decode(1028,curve(1,new int[][]{{4096,0},{0,4096},{2048,1024}}),4).pixels,0,4));
        assertArrayEquals(decoder.decode(1029,curve(1,new int[][]{{0,0},{0,2048},{4096,4096}}),4).pixels,
            decoder.decode(1029,curve(1,new int[][]{{0,2048},{4096,4096}}),4).pixels);
        assertThrows(ArithmeticException.class,()->decoder.decode(1030,curve(1,new int[][]{{0,0},{0,4096}}),4));
    }
    @Test void curveMode1RejectsMalformedPayloadsAndLeavesOtherModesFailClosed(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        for(byte[] truncated:new byte[][]{{1,0,8,1,1,0},{1,0,8,1,1,0,1,2,0,0,0}}){
            UnsupportedTextureFormatException error=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(1031,truncated,4));
            assertTrue(error.getMessage().contains("truncated operation 8 parameter 0"));
        }
        UnsupportedTextureFormatException count=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(1032,curve(1,new int[][]{{0,0}}),4));
        assertTrue(count.getMessage().contains("curve marker count"));
        UnsupportedTextureFormatException mode=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(1033,curve(2,new int[][]{{0,0},{4096,4096}}),4));
        assertTrue(mode.getMessage().contains("curve interpolation 2"));
        assertArrayEquals(new int[]{0xc0c0c0,0x808080,0x404040,0},java.util.Arrays.copyOfRange(decoder.decode(1033,curve(0,new int[][]{{0,0},{4096,4096}}),4).pixels,0,4));
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(1034,new byte[]{1,0,8,1,1,1,0,0,0},4));
        assertTrue(parameter.getMessage().contains("operation parameter 1 for Curve"));
    }
    @Test void operation17DefaultsPreserveCoordinatesAndConvertColorAndMonochromeChildren(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded color=decoder.decode(979,hslAdjust(0x4080c0),4),second=decoder.decode(979,hslAdjust(0x4080c0),4);
        assertTrue(java.util.Arrays.stream(color.pixels).allMatch(pixel->pixel==0x4080c0));assertArrayEquals(color.pixels,second.pixels);assertEquals(java.util.List.of(1,17),color.operationTypes);
        ProceduralTexture530Decoder.Decoded monochrome=decoder.decode(980,hslAdjustMonochrome(203),4);
        assertTrue(java.util.Arrays.stream(monochrome.pixels).allMatch(pixel->pixel==0xcbcbcb));assertEquals(java.util.List.of(0,17),monochrome.operationTypes);
        assertArrayEquals(new int[]{0xc0c0c0,0x808080,0x404040,0},java.util.Arrays.copyOfRange(decoder.decode(981,hslAdjustGradient(),4).pixels,0,4));
    }
    @Test void operation17DecodesEverySignedParameterAndClampsOrWrapsBoundaries(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertEquals(0xbf40c0,decoder.decode(982,hslAdjust(0x4080c0,1024,0,0),4).pixels[0]);
        assertEquals(0x40c040,decoder.decode(983,hslAdjust(0x4080c0,-1024,0,0),4).pixels[0]);
        assertEquals(0x0080ff,decoder.decode(984,hslAdjust(0x4080c0,0,50,0),4).pixels[0]);
        assertEquals(0x808080,decoder.decode(985,hslAdjust(0x4080c0,0,-50,0),4).pixels[0]);
        assertEquals(0xffffff,decoder.decode(986,hslAdjust(0x4080c0,0,0,50),4).pixels[0]);
        assertEquals(0,decoder.decode(987,hslAdjust(0x4080c0,0,0,-50),4).pixels[0]);
        assertEquals(0x1a7010,decoder.decode(988,hslAdjust(0x4080c0,3000,25,-25),4).pixels[0]);
        assertEquals(0xffffff,decoder.decode(989,hslAdjust(0x4080c0,32767,127,127),4).pixels[0]);
        assertEquals(0,decoder.decode(990,hslAdjust(0x4080c0,-32768,-128,-128),4).pixels[0]);
        assertEquals(0,decoder.decode(990,hslAdjust(0x4080c0,1707,0,0),4).pixels[0]);
        assertEquals(0xc04040,decoder.decode(990,hslAdjust(0x4080c0,-2389,0,0),4).pixels[0]);
    }
    @Test void operation17PreservesPinnedGraySaturationAndJavaOverflowBehavior(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertEquals(0xff0000,decoder.decode(991,hslAdjust(0x808080,0,127,0),4).pixels[0]);
        ProceduralTexture530Decoder.Decoded overflow=decoder.decode(992,hslOverflow(),4),again=decoder.decode(992,hslOverflow(),4);
        assertEquals(0xffffff,overflow.pixels[0]);assertArrayEquals(overflow.pixels,again.pixels);assertEquals(17,overflow.operationTypes.get(overflow.operationTypes.size()-1));
    }
    @Test void operation17RejectsEveryMalformedParameterAndInvalidChildren(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        byte[][] truncated={{1,0,17,1,1,0,0},{1,0,17,1,1,1},{1,0,17,1,1,2}};
        for(int code=0;code<truncated.length;code++){int parameterCode=code;UnsupportedTextureFormatException error=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(993,truncated[parameterCode],4));assertTrue(error.getMessage().contains("truncated operation 17 parameter "+parameterCode));}
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(994,new byte[]{1,0,17,1,1,3,0,0,0},4));
        assertTrue(parameter.getMessage().contains("operation parameter 3 for HslAdjust"));
        UnsupportedTextureFormatException child=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(995,new byte[]{1,0,17,1,0,1,0,0,0},4));
        assertTrue(child.getMessage().contains("invalid child operation 1"));
    }
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
    @Test void operation12DefaultsToLinearSineMonochromeDeterministically(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded first=decoder.decode(1005,defaultWaveform(),4),second=decoder.decode(1005,defaultWaveform(),4);
        assertArrayEquals(new int[]{0,0x7e7e7e,0xffffff,0x808080},java.util.Arrays.copyOfRange(first.pixels,0,4));
        assertArrayEquals(first.pixels,second.pixels);assertEquals(java.util.List.of(12),first.operationTypes);
    }
    @Test void operation12ImplementsEveryWaveformSelectorBranch(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertArrayEquals(new int[]{0xc0c0c0,0x808080,0x404040,0},java.util.Arrays.copyOfRange(decoder.decode(1006,waveform(0,1,1),4).pixels,0,4));
        assertArrayEquals(new int[]{0x808080,0xffffff,0x808080,0},java.util.Arrays.copyOfRange(decoder.decode(1007,waveform(0,2,1),4).pixels,0,4));
        assertArrayEquals(decoder.decode(1006,waveform(0,1,1),4).pixels,decoder.decode(1008,waveform(0,255,1),4).pixels);
    }
    @Test void operation12UsesLinearZeroAndRadialNonzeroCoordinateModes(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        int[] radial=decoder.decode(1009,waveform(1,1,1),4).pixels;
        assertArrayEquals(new int[]{0xe0e0e0,0xc9c9c9,0xe0e0e0,0x1c1c1c},java.util.Arrays.copyOfRange(radial,0,4));
        assertArrayEquals(radial,decoder.decode(1010,waveform(255,1,1),4).pixels);
        assertFalse(java.util.Arrays.equals(radial,decoder.decode(1011,waveform(0,1,1),4).pixels));
    }
    @Test void operation12PreservesUnsignedFrequencyBoundariesAndFixedPointWrapping(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertTrue(java.util.Arrays.stream(decoder.decode(1012,waveform(1,2,0),4).pixels).allMatch(pixel->pixel==0));
        assertArrayEquals(new int[]{0x3e3e3e,0x6e6e6e,0x3e3e3e,0x383838},java.util.Arrays.copyOfRange(decoder.decode(1013,waveform(1,2,1),4).pixels,0,4));
        assertArrayEquals(new int[]{0x818181,0x8d8d8d,0x818181,0x686868},java.util.Arrays.copyOfRange(decoder.decode(1014,waveform(1,2,255),4).pixels,0,4));
    }
    @Test void operation12PreservesAllZeroByteParameterFramingAndRejectsMalformedParameters(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertArrayEquals(decoder.decode(1015,defaultWaveform(),4).pixels,decoder.decode(1015,waveformNoOpParameters(),4).pixels);
        for(int code:new int[]{0,1,3}){
            UnsupportedTextureFormatException error=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(1015,new byte[]{1,0,12,1,1,(byte)code},4));
            assertTrue(error.getMessage().contains("truncated operation 12 parameter "+code));
        }
        UnsupportedTextureFormatException unknown=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(1016,new byte[]{1,0,12,1,1,(byte)255,0,0,0},4));
        assertTrue(unknown.getMessage().contains("operation parameter 255 for Waveform"));
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
    @Test void operation6ClampsColorChannelsAtInclusiveUnsignedBounds(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded color=decoder.decode(944,clampColor(0),4);
        ProceduralTexture530Decoder.Decoded monochrome=decoder.decode(950,clampColor(1),4);
        assertTrue(java.util.Arrays.stream(color.pixels).allMatch(pixel->pixel==0x204060));
        assertTrue(java.util.Arrays.stream(monochrome.pixels).allMatch(pixel->pixel==0x202020));
        assertArrayEquals(color.pixels,decoder.decode(945,clampColor(2),4).pixels);
        assertEquals(java.util.List.of(1,6),color.operationTypes);
    }
    @Test void operation6PreservesCoordinatesAndClampsMonochromeFirstChannel(){
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(946,clampGradient(1024,3072,1),4);
        assertArrayEquals(new int[]{0xc0c0c0,0x808080,0x404040,0x404040},java.util.Arrays.copyOfRange(decoded.pixels,0,4));
        assertEquals(java.util.List.of(2,6),decoded.operationTypes);
    }
    @Test void operation6PreservesReversedBoundsUnsignedValuesAndFailClosedParameters(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded reversed=decoder.decode(947,clampGradient(3072,1024,1),4);
        assertArrayEquals(new int[]{0x404040,0xc0c0c0,0xc0c0c0,0xc0c0c0},java.util.Arrays.copyOfRange(reversed.pixels,0,4));
        assertTrue(java.util.Arrays.stream(decoder.decode(948,clampMonochromeFill(0,65535,65535),4).pixels).allMatch(pixel->pixel==0xffffff));
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,
            ()->decoder.decode(949,new byte[]{1,0,6,1,1,3,0,0,0,0},4));
        assertTrue(parameter.getMessage().contains("operation parameter 3 for Clamp"));
    }
    @Test void combineFunction6UsesJavaOverflowThenFinalTextureClamp(){
        ProceduralTexture530Decoder.Decoded high=new ProceduralTexture530Decoder().decode(931,monochromeCombine(6,1,65535,65535),4);
        assertTrue(java.util.Arrays.stream(high.pixels).allMatch(pixel->pixel==0xffffff));
        ProceduralTexture530Decoder.Decoded ordered=new ProceduralTexture530Decoder().decode(932,monochromeCombine(6,1,1024,3072),4);
        ProceduralTexture530Decoder.Decoded reversed=new ProceduralTexture530Decoder().decode(933,monochromeCombine(6,1,3072,1024),4);
        assertTrue(java.util.Arrays.stream(ordered.pixels).allMatch(pixel->pixel==0xa0a0a0));
        assertTrue(java.util.Arrays.stream(reversed.pixels).allMatch(pixel->pixel==0x606060));
    }
    @Test void combineFunction1AddsColorOrFirstChannelsBeforeFinalClamp(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded color=decoder.decode(938,colorCombine(1,0),4);
        ProceduralTexture530Decoder.Decoded monochrome=decoder.decode(939,colorCombine(1,1),4);
        assertTrue(java.util.Arrays.stream(color.pixels).allMatch(pixel->pixel==0x60ffff));
        assertTrue(java.util.Arrays.stream(monochrome.pixels).allMatch(pixel->pixel==0x606060));
        assertArrayEquals(color.pixels,decoder.decode(940,colorCombine(1,2),4).pixels);
    }
    @Test void combineFunction1PreservesJavaOverflowWithoutNodeClamping(){
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(941,overflowingAddition(),4);
        assertTrue(java.util.Arrays.stream(decoded.pixels).allMatch(pixel->pixel==0));
        assertEquals(18,decoded.operationTypes.size());
        assertEquals(16,decoded.operationTypes.stream().filter(type->type==7).count());
    }
    @Test void combineFunction2SubtractsSecondFromFirstForColorAndMonochromeOutputs(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded color=decoder.decode(961,colorCombine(2,0),4);
        ProceduralTexture530Decoder.Decoded monochrome=decoder.decode(962,colorCombine(2,1),4);
        assertTrue(java.util.Arrays.stream(color.pixels).allMatch(pixel->pixel==0x200000));
        assertTrue(java.util.Arrays.stream(monochrome.pixels).allMatch(pixel->pixel==0x202020));
        assertArrayEquals(color.pixels,decoder.decode(963,colorCombine(2,2),4).pixels);
        assertTrue(java.util.Arrays.stream(decoder.decode(964,monochromeCombine(2,1,0,65535),4).pixels).allMatch(pixel->pixel==0));
    }
    @Test void combineFunction5ScreensColorAndMonochromeWithFixedPointOverflow(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded color=decoder.decode(965,colorCombine(5,0),4);
        ProceduralTexture530Decoder.Decoded monochrome=decoder.decode(966,colorCombine(5,1),4);
        assertTrue(java.util.Arrays.stream(color.pixels).allMatch(pixel->pixel==0x58d0f8));
        assertTrue(java.util.Arrays.stream(monochrome.pixels).allMatch(pixel->pixel==0x585858));
        assertArrayEquals(color.pixels,decoder.decode(967,colorCombine(5,2),4).pixels);
        assertTrue(java.util.Arrays.stream(decoder.decode(968,monochromeCombine(5,1,65535,65535),4).pixels).allMatch(pixel->pixel==0xffffff));
    }
    @Test void combineFunction7UsesPinnedOperandOrderDivisionAndOutputModes(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded color=decoder.decode(996,colorCombine(7,0),4);
        ProceduralTexture530Decoder.Decoded monochrome=decoder.decode(997,colorCombine(7,1),4);
        assertTrue(java.util.Arrays.stream(color.pixels).allMatch(pixel->pixel==0x2affff));
        assertTrue(java.util.Arrays.stream(monochrome.pixels).allMatch(pixel->pixel==0x2a2a2a));
        assertArrayEquals(color.pixels,decoder.decode(998,colorCombine(7,2),4).pixels);
        assertEquals(java.util.List.of(1,1,7),color.operationTypes);
        assertTrue(java.util.Arrays.stream(decoder.decode(999,monochromeCombine(7,1,4096,0),4).pixels).allMatch(pixel->pixel==0xffffff));
        assertTrue(java.util.Arrays.stream(decoder.decode(1000,monochromeCombine(7,1,0,0),4).pixels).allMatch(pixel->pixel==0));
    }
    @Test void combineFunction7PreservesJavaOverflowNegativeResultsAndFinalClamp(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertTrue(java.util.Arrays.stream(decoder.decode(1001,monochromeCombine(7,1,65535,4096),4).pixels).allMatch(pixel->pixel==0));
        assertTrue(java.util.Arrays.stream(decoder.decode(1002,combine7Overflow(),4).pixels).allMatch(pixel->pixel==0));
        assertArrayEquals(decoder.decode(1002,combine7Overflow(),4).pixels,decoder.decode(1002,combine7Overflow(),4).pixels);
    }
    @Test void combineFunction7RejectsMalformedParametersAndPreservesUnsupportedFunctions(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        UnsupportedTextureFormatException function=assertThrows(UnsupportedTextureFormatException.class,
            ()->decoder.decode(1003,new byte[]{1,0,7,1,1,0},4));
        assertTrue(function.getMessage().contains("truncated operation 7 parameter 0"));
        UnsupportedTextureFormatException mode=assertThrows(UnsupportedTextureFormatException.class,
            ()->decoder.decode(1004,new byte[]{1,0,7,1,1,1},4));
        assertTrue(mode.getMessage().contains("truncated operation 7 parameter 1"));
    }
    @Test void combineFunction10TakesPerChannelSignedMaximumInBothOutputModes(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded color=decoder.decode(1017,colorCombine(10,0),4),monochrome=decoder.decode(1018,colorCombine(10,1),4);
        assertTrue(java.util.Arrays.stream(color.pixels).allMatch(pixel->pixel==0x40a0e0));
        assertTrue(java.util.Arrays.stream(monochrome.pixels).allMatch(pixel->pixel==0x404040));
        assertArrayEquals(color.pixels,decoder.decode(1019,colorCombine(10,2),4).pixels);
        assertArrayEquals(color.pixels,decoder.decode(1017,colorCombine(10,0),4).pixels);
        assertEquals(java.util.List.of(1,1,7),color.operationTypes);
    }
    @Test void combineFunction10PreservesZerosExtremesAndSignedOverflowIntermediates(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertTrue(java.util.Arrays.stream(decoder.decode(1020,monochromeCombine(10,1,0,0),4).pixels).allMatch(pixel->pixel==0));
        assertTrue(java.util.Arrays.stream(decoder.decode(1021,monochromeCombine(10,1,0,65535),4).pixels).allMatch(pixel->pixel==0xffffff));
        assertTrue(java.util.Arrays.stream(decoder.decode(1022,monochromeCombine(10,1,65535,0),4).pixels).allMatch(pixel->pixel==0xffffff));
        assertTrue(java.util.Arrays.stream(decoder.decode(1023,combine10Overflow(),4).pixels).allMatch(pixel->pixel==0x404040));
    }
    @Test void combineFunction10RejectsMalformedParameters(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        for(int code:new int[]{0,1}){
            UnsupportedTextureFormatException error=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(1024,new byte[]{1,0,7,1,1,(byte)code},4));
            assertTrue(error.getMessage().contains("truncated operation 7 parameter "+code));
        }
    }
    @Test void combineFunction8UsesPinnedOperandOrderArithmeticAndOutputModes(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded color=decoder.decode(1035,colorCombine(8,0),4),monochrome=decoder.decode(1036,colorCombine(8,1),4);
        assertTrue(java.util.Arrays.stream(color.pixels).allMatch(pixel->pixel==0x0040d5));
        assertTrue(java.util.Arrays.stream(monochrome.pixels).allMatch(pixel->pixel==0));
        assertArrayEquals(color.pixels,decoder.decode(1037,colorCombine(8,2),4).pixels);
        assertArrayEquals(color.pixels,decoder.decode(1035,colorCombine(8,0),4).pixels);
        assertEquals(java.util.List.of(1,1,7),color.operationTypes);
        assertTrue(java.util.Arrays.stream(decoder.decode(1038,monochromeCombine(8,1,2048,3072),4).pixels).allMatch(pixel->pixel==0x808080));
        assertTrue(java.util.Arrays.stream(decoder.decode(1039,monochromeCombine(8,1,3072,2048),4).pixels).allMatch(pixel->pixel==0x555555));
    }
    @Test void combineFunction8PreservesZeroGuardEqualityExtremesAndJavaOverflow(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertTrue(java.util.Arrays.stream(decoder.decode(1040,monochromeCombine(8,1,0,65535),4).pixels).allMatch(pixel->pixel==0));
        assertTrue(java.util.Arrays.stream(decoder.decode(1041,monochromeCombine(8,1,2048,2048),4).pixels).allMatch(pixel->pixel==0));
        assertTrue(java.util.Arrays.stream(decoder.decode(1042,monochromeCombine(8,1,4096,4096),4).pixels).allMatch(pixel->pixel==0xffffff));
        assertTrue(java.util.Arrays.stream(decoder.decode(1043,monochromeCombine(8,1,65535,65535),4).pixels).allMatch(pixel->pixel==0xffffff));
        assertTrue(java.util.Arrays.stream(decoder.decode(1044,combine8NegativeDivisor(),4).pixels).allMatch(pixel->pixel==0x101010));
        assertTrue(java.util.Arrays.stream(decoder.decode(1044,combine8ShiftOverflowProbe(),4).pixels).allMatch(pixel->pixel==0));
    }
    @Test void combineFunction8RejectsMalformedParametersAndLeavesOtherFunctionsFailClosed(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        for(int code:new int[]{0,1}){
            UnsupportedTextureFormatException error=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(1045,new byte[]{1,0,7,1,1,(byte)code},4));
            assertTrue(error.getMessage().contains("truncated operation 7 parameter "+code));
        }
        for(int function:new int[]{0,4,9,11,12}){
            UnsupportedTextureFormatException error=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(1046,colorCombine(function,0),4));
            assertTrue(error.getMessage().contains("combine function "+function));
        }
    }
    @Test void previouslySupportedCombineFunctionsRemainExactAndOthersFailClosed(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertTrue(java.util.Arrays.stream(decoder.decode(934,colorCombine(3,0),4).pixels).allMatch(pixel->pixel==0x0850a8));
        assertTrue(java.util.Arrays.stream(decoder.decode(935,colorCombine(3,1),4).pixels).allMatch(pixel->pixel==0x080808));
        assertTrue(java.util.Arrays.stream(decoder.decode(942,colorCombine(6,0),4).pixels).allMatch(pixel->pixel==0x10a0f0));
        assertTrue(java.util.Arrays.stream(decoder.decode(943,colorCombine(6,1),4).pixels).allMatch(pixel->pixel==0x101010));
        for(int function=0;function<=12;function++)if(function!=1&&function!=2&&function!=3&&function!=5&&function!=6&&function!=7&&function!=8&&function!=10){
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
    @Test void operation22InvertsColorAndMonochromeChannelsWithoutChangingCoordinates(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded color=decoder.decode(969,invertColor(0),4);
        ProceduralTexture530Decoder.Decoded monochrome=decoder.decode(970,invertColor(1),4);
        assertTrue(java.util.Arrays.stream(color.pixels).allMatch(pixel->pixel==0xc08040));
        assertTrue(java.util.Arrays.stream(monochrome.pixels).allMatch(pixel->pixel==0xc0c0c0));
        assertArrayEquals(color.pixels,decoder.decode(971,invertColor(2),4).pixels);
        assertEquals(java.util.List.of(1,22),color.operationTypes);
    }
    @Test void operation22PreservesSignedResultsAndRejectsUnknownParameters(){
        assertTrue(java.util.Arrays.stream(new ProceduralTexture530Decoder().decode(972,invertMonochrome(65535),4).pixels).allMatch(pixel->pixel==0));
        UnsupportedTextureFormatException error=assertThrows(UnsupportedTextureFormatException.class,
            ()->new ProceduralTexture530Decoder().decode(973,new byte[]{1,0,22,1,1,1,0,0,0,0},4));
        assertTrue(error.getMessage().contains("operation parameter 1 for Invert"));
    }
    @Test void operation39LoadsTrimsAndNearestScalesItsSerializedSpriteDependency()throws Exception{
        int[] pixels={0xff0000,0x00ff00,0x0000ff,0xffffff};
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(974,spriteDependency(321),4,
            id->{throw new AssertionError("operation 39 must not resolve texture "+id);},
            id->{assertEquals(321,id);return new ProceduralTexture530Decoder.SpriteDependency(2,2,pixels);});
        assertArrayEquals(new int[]{0x00ff00,0x00ff00,0xff0000,0xff0000,
            0x00ff00,0x00ff00,0xff0000,0xff0000,
            0xffffff,0xffffff,0x0000ff,0x0000ff},java.util.Arrays.copyOfRange(decoded.pixels,0,12));
        assertEquals(java.util.List.of(39),decoded.operationTypes);
    }
    @Test void operation39FeedsItsRedChannelToMonochromeConsumersAndFailsClosed(){
        int[] pixels={0xff0000,0x00ff00,0x0000ff,0xffffff};
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded mono=assertDoesNotThrow(()->decoder.decode(975,spriteThenMonochromeInvert(321),4,
            id->{throw new AssertionError("operation 39 must not resolve texture "+id);},
            id->new ProceduralTexture530Decoder.SpriteDependency(2,2,pixels)));
        assertArrayEquals(new int[]{0xffffff,0xffffff,0x010101,0x010101},java.util.Arrays.copyOfRange(mono.pixels,0,4));
        UnsupportedTextureFormatException missing=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(976,spriteDependency(321),4));
        assertTrue(missing.getMessage().contains("sprite dependency 321 requires a provider"));
        UnsupportedTextureFormatException dimensions=assertThrows(UnsupportedTextureFormatException.class,()->decoder.decode(977,spriteDependency(321),4,
            id->null,id->new ProceduralTexture530Decoder.SpriteDependency(2,2,new int[3])));
        assertTrue(dimensions.getMessage().contains("invalid sprite dependency dimensions"));
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,
            ()->decoder.decode(978,new byte[]{1,0,39,1,1,1,0,0,0,0},4));
        assertTrue(parameter.getMessage().contains("operation parameter 1 for SpriteDependencyNode"));
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
    @Test void operation19UsesDistinctPinnedColorAndMonochromeAngleQuantization(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded color=decoder.decode(951,coordinateDisplacement(0,2048,4096,32767),8);
        ProceduralTexture530Decoder.Decoded monochrome=decoder.decode(952,coordinateDisplacement(1,2048,4096,32767),8);
        assertArrayEquals(new int[]{0x2c4874,0x2c4874,0x2c4874,0x2c4874,0x2c4874,0x2c4874,0x2c4874,0x2c4874,
            0x485068,0x485068,0x485068,0x485068,0x485068,0x485068,0x485068,0x485068},java.util.Arrays.copyOfRange(color.pixels,0,16));
        assertArrayEquals(new int[]{0xb8b8b8,0xb8b8b8,0xb8b8b8,0xb8b8b8,0xb8b8b8,0xb8b8b8,0xb8b8b8,0xb8b8b8,
            0xd4d4d4,0xd4d4d4,0xd4d4d4,0xd4d4d4,0xd4d4d4,0xd4d4d4,0xd4d4d4,0xd4d4d4},java.util.Arrays.copyOfRange(monochrome.pixels,0,16));
        assertArrayEquals(color.pixels,decoder.decode(953,coordinateDisplacement(2,2048,4096,32767),8).pixels);
        assertEquals(java.util.List.of(3,10,30,30,19),color.operationTypes);
    }
    @Test void operation19PreservesDefaultScaleWrappedCoordinatesAndJavaOverflow(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        int[] defaults=decoder.decode(954,coordinateDisplacement(0,1024,4096,-1),8).pixels;
        assertArrayEquals(new int[]{0xd4782c,0xd4782c,0xd4782c,0xd4782c,0xd4782c,0xd4782c,0xd4782c,0xd4782c},java.util.Arrays.copyOfRange(defaults,0,8));
        int[] overflow=decoder.decode(955,coordinateDisplacement(0,2048,65535,65535),8).pixels;
        assertArrayEquals(new int[]{0xd4782c,0xd4782c,0xd4782c,0xd4782c,0xd4782c,0xd4782c,0xd4782c,0xd4782c},java.util.Arrays.copyOfRange(overflow,0,8));
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,
            ()->decoder.decode(956,new byte[]{1,0,19,1,1,2,0,0,0,0},8));
        assertTrue(parameter.getMessage().contains("operation parameter 2 for CoordinateDisplacement"));
    }
    @Test void operation9FlipsColorCoordinatesAndPreservesChannels(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded horizontal=decoder.decode(957,colorFlip(1,0,0),8);
        assertArrayEquals(new int[]{0x104080,0x2c4874,0x485068,0x64585c,0x806050,0x9c6844,0xb87038,0xd4782c},java.util.Arrays.copyOfRange(horizontal.pixels,0,8));
        ProceduralTexture530Decoder.Decoded neither=decoder.decode(958,colorFlip(2,2,2),8);
        assertArrayEquals(new int[]{0xd4782c,0xb87038,0x9c6844,0x806050,0x64585c,0x485068,0x2c4874,0x104080},java.util.Arrays.copyOfRange(neither.pixels,0,8));
        assertEquals(java.util.List.of(2,10,9),horizontal.operationTypes);
    }
    @Test void operation9FlipsMonochromeRowsAndPreservesPinnedDefaults(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        int[] vertical=decoder.decode(959,monochromeFlip(0,1,1),8).pixels;
        assertArrayEquals(new int[]{0xe0e0e0,0xe0e0e0,0xe0e0e0,0xe0e0e0,0xe0e0e0,0xe0e0e0,0xe0e0e0,0xe0e0e0,
            0xc0c0c0,0xc0c0c0,0xc0c0c0,0xc0c0c0,0xc0c0c0,0xc0c0c0,0xc0c0c0,0xc0c0c0},java.util.Arrays.copyOfRange(vertical,0,16));
        assertArrayEquals(new int[]{0,0x202020,0x404040,0x606060,0x808080,0xa0a0a0,0xc0c0c0,0xe0e0e0},
            java.util.Arrays.copyOfRange(decoder.decode(960,defaultFlip(),8).pixels,0,8));
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,
            ()->decoder.decode(961,new byte[]{1,0,9,1,1,3,0,0,0,0},8));
        assertTrue(parameter.getMessage().contains("operation parameter 3 for Flip"));
    }
    @Test void operation21InterpolatesOrderedColorAndMonochromeChildren(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        ProceduralTexture530Decoder.Decoded color=decoder.decode(962,interpolateColor(1024,0),4);
        ProceduralTexture530Decoder.Decoded monochrome=decoder.decode(963,interpolateColor(1024,1),4);
        assertTrue(java.util.Arrays.stream(color.pixels).allMatch(pixel->pixel==0x2898d8));
        assertTrue(java.util.Arrays.stream(monochrome.pixels).allMatch(pixel->pixel==0x282828));
        assertArrayEquals(color.pixels,decoder.decode(964,interpolateColor(1024,2),4).pixels);
        assertEquals(java.util.List.of(1,1,30,21),color.operationTypes);
    }
    @Test void operation21PreservesExactEndpointsExtrapolationOverflowAndParameters(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        assertTrue(java.util.Arrays.stream(decoder.decode(965,interpolateColor(0,0),4).pixels).allMatch(pixel->pixel==0x20a0e0));
        assertTrue(java.util.Arrays.stream(decoder.decode(966,interpolateColor(4096,0),4).pixels).allMatch(pixel->pixel==0x4080c0));
        assertTrue(java.util.Arrays.stream(decoder.decode(967,interpolateMonochrome(65535,0),4).pixels).allMatch(pixel->pixel==0));
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,
            ()->decoder.decode(968,new byte[]{1,0,21,1,1,1,0,0,0,0},4));
        assertTrue(parameter.getMessage().contains("operation parameter 1 for Interpolate"));
    }
    @Test void operation20TilesColorCoordinatesWithPinnedIntegerRescaling(){
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(969,colorTile(2,1),8);
        assertArrayEquals(new int[]{0xb87038,0x806050,0x485068,0x104080,0xb87038,0x806050,0x485068,0x104080},java.util.Arrays.copyOfRange(decoded.pixels,0,8));
        assertEquals(java.util.List.of(2,10,20),decoded.operationTypes);
    }
    @Test void operation20TilesMonochromeRowsAndPreservesCollapsedAxesAndDefaults(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        int[] vertical=decoder.decode(970,monochromeTile(1,2),8).pixels;
        assertArrayEquals(new int[]{0,0,0,0,0,0,0,0,0x404040,0x404040,0x404040,0x404040,0x404040,0x404040,0x404040,0x404040},java.util.Arrays.copyOfRange(vertical,0,16));
        assertTrue(java.util.Arrays.stream(decoder.decode(971,colorTile(255,255),8).pixels).allMatch(pixel->pixel==0x104080));
        assertArrayEquals(new int[]{0x808080,0,0x808080,0,0x808080,0,0x808080,0},
            java.util.Arrays.copyOfRange(decoder.decode(972,defaultTile(),8).pixels,0,8));
    }
    @Test void operation20RejectsUnknownParametersAndZeroTileCounts(){
        ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();
        UnsupportedTextureFormatException parameter=assertThrows(UnsupportedTextureFormatException.class,
            ()->decoder.decode(973,new byte[]{1,0,20,1,1,2,0,0,0,0},8));
        assertTrue(parameter.getMessage().contains("operation parameter 2 for Tile"));
        UnsupportedTextureFormatException grid=assertThrows(UnsupportedTextureFormatException.class,
            ()->decoder.decode(974,colorTile(0,4),8));
        assertTrue(grid.getMessage().contains("tile grid 0x4"));
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
    static byte[] clampColor(int outputMode){return new byte[]{2,0,1,1,1,0,16,64,(byte)128,0,6,1,3,0,2,0,1,6,0,2,(byte)outputMode,0,1,0,0};}
    static byte[] clampGradient(int lower,int upper,int outputMode){return new byte[]{2,0,2,1,0,0,6,1,3,0,(byte)(lower>>>8),(byte)lower,1,(byte)(upper>>>8),(byte)upper,2,(byte)outputMode,0,1,0,0};}
    static byte[] clampMonochromeFill(int value,int lower,int upper){return new byte[]{2,0,0,1,1,0,(byte)value,0,6,1,3,0,(byte)(lower>>>8),(byte)lower,1,(byte)(upper>>>8),(byte)upper,2,1,0,1,0,0};}
    static byte[] coordinateDisplacement(int outputMode,int angle,int magnitude,int serializedScale){
        ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,5,0,3,1,0);
        bytes(out,0,10,1,1,0,0,2,0,0,16,64,128,16,0,240,128,32,0);
        constantRange(out,angle,0);constantRange(out,magnitude,0);
        if(serializedScale<0)bytes(out,0,19,1,1,1,outputMode,1,2,3);
        else bytes(out,0,19,1,2,0,serializedScale>>>8,serializedScale,1,outputMode,1,2,3);
        bytes(out,4,0,0);return out.toByteArray();
    }
    static byte[] colorFlip(int horizontal,int vertical,int outputMode){
        ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,3,0,2,1,0);
        bytes(out,0,10,1,1,0,0,2,0,0,16,64,128,16,0,240,128,32,0);
        bytes(out,0,9,1,3,0,horizontal,1,vertical,2,outputMode,1,2,0,0);return out.toByteArray();
    }
    static byte[] monochromeFlip(int horizontal,int vertical,int outputMode){return new byte[]{2,0,3,1,0,0,9,1,3,0,(byte)horizontal,1,(byte)vertical,2,(byte)outputMode,0,1,0,0};}
    static byte[] defaultFlip(){return new byte[]{2,0,2,1,0,0,9,1,0,0,1,0,0};}
    static byte[] interpolateColor(int control,int outputMode){ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,4,0,1,1,1,0,64,128,192,0,1,1,1,0,32,160,224);constantRange(out,control,0);bytes(out,0,21,1,1,0,outputMode,0,1,2,3,0,0);return out.toByteArray();}
    static byte[] interpolateMonochrome(int control,int outputMode){
        ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,5,0,2,1,0);
        constantRange(out,65535,0);constantRange(out,0,0);constantRange(out,control,0);
        bytes(out,0,21,1,1,0,outputMode,1,2,3,4,0,0);return out.toByteArray();
    }
    static byte[] colorTile(int horizontalTiles,int verticalTiles){
        ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,3,0,2,1,0);
        bytes(out,0,10,1,1,0,0,2,0,0,16,64,128,16,0,240,128,32,0);
        bytes(out,0,20,1,2,0,horizontalTiles,1,verticalTiles,1,2,0,0);return out.toByteArray();
    }
    static byte[] monochromeTile(int horizontalTiles,int verticalTiles){return new byte[]{2,0,3,1,0,0,20,1,2,0,(byte)horizontalTiles,1,(byte)verticalTiles,0,1,0,0};}
    static byte[] defaultTile(){return new byte[]{2,0,2,1,0,0,20,1,0,0,1,0,0};}
    static byte[] colorCombine(int function,int outputMode){return new byte[]{3,0,1,1,1,0,64,(byte)128,(byte)192,0,1,1,1,0,32,(byte)160,(byte)224,0,7,1,2,0,(byte)function,1,(byte)outputMode,0,1,2,0,0};}
    static byte[] monochromeCombine(int function,int outputMode,int first,int second){ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,4,0,2,1,0);constantRange(out,first,0);constantRange(out,second,0);bytes(out,0,7,1,2,0,function,1,outputMode,1,2,3,0,0);return out.toByteArray();}
    static byte[] invertColor(int outputMode){return new byte[]{2,0,1,1,1,0,64,(byte)128,(byte)192,0,22,1,1,0,(byte)outputMode,0,1,0,0};}
    static byte[] invertMonochrome(int value){ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,3,0,2,1,0);constantRange(out,value,0);bytes(out,0,22,1,1,0,1,1,2,0,0);return out.toByteArray();}
    static byte[] spriteDependency(int id){return new byte[]{1,0,39,1,1,0,(byte)(id>>>8),(byte)id,0,0,0};}
    static byte[] spriteThenMonochromeInvert(int id){return new byte[]{2,0,39,1,1,0,(byte)(id>>>8),(byte)id,0,22,1,1,0,1,0,1,0,0};}
    static byte[] overflowingAddition(){ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,18,0,2,1,0);constantRange(out,65535,0);for(int node=2;node<18;node++)bytes(out,0,7,1,2,0,1,1,1,node-1,node-1);bytes(out,17,0,0);return out.toByteArray();}
    static byte[] combine7Overflow(){ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,8,0,2,1,0);constantRange(out,0,0);constantRange(out,65535,0);for(int node=3;node<=6;node++)bytes(out,0,7,1,2,0,1,1,1,node-1,node-1);bytes(out,0,7,1,2,0,7,1,1,1,6,7,0,0);return out.toByteArray();}
    static byte[] combine10Overflow(){ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,20,0,2,1,0);constantRange(out,65535,0);constantRange(out,1024,0);bytes(out,0,7,1,2,0,1,1,1,1,1);for(int node=4;node<=18;node++)bytes(out,0,7,1,2,0,1,1,1,node-1,node-1);bytes(out,0,7,1,2,0,10,1,1,18,2,19,0,0);return out.toByteArray();}
    static byte[] combine8NegativeDivisor(){
        ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,7,0,2,1,0);constantRange(out,65535,0);constantRange(out,0,0);
        bytes(out,0,7,1,2,0,2,1,1,2,1,0,7,1,2,0,8,1,1,3,2);constantRange(out,4096,0);
        bytes(out,0,7,1,2,0,2,1,1,4,5,6,0,0);return out.toByteArray();
    }
    static byte[] combine8ShiftOverflowProbe(){
        ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,9,0,2,1,0);constantRange(out,65535,0);
        bytes(out,0,7,1,2,0,8,1,1,1,1);constantRange(out,7935,0);bytes(out,0,7,1,2,0,2,1,1,2,3);
        for(int node=5;node<=8;node++)bytes(out,0,7,1,2,0,1,1,1,node-1,node-1);
        bytes(out,8,0,0);return out.toByteArray();
    }
    static byte[] defaultWaveform(){return new byte[]{1,0,12,1,0,0,0,0};}
    static byte[] waveformNoOpParameters(){return new byte[]{1,0,12,1,4,2,4,5,6,0,0,0};}
    static byte[] waveform(int coordinateMode,int selector,int frequency){return new byte[]{1,0,12,1,3,0,(byte)coordinateMode,1,(byte)selector,3,(byte)frequency,0,0,0};}
    static byte[] monochromeFill(int value){return new byte[]{1,0,0,1,1,0,(byte)value,0,0,0};}
    static byte[] curve(int mode,int[][] markers){
        ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,2,0,2,1,0,0,8,1,1,0,mode,markers.length);
        for(int[] marker:markers)bytes(out,marker[0]>>>8,marker[0],marker[1]>>>8,marker[1]);
        bytes(out,0,1,0,0);return out.toByteArray();
    }
    static byte[] curveOverConstant(int value,int[][] markers){
        ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,3,0,2,1,0);constantRange(out,value,0);bytes(out,0,8,1,1,0,1,markers.length);
        for(int[] marker:markers)bytes(out,marker[0]>>>8,marker[0],marker[1]>>>8,marker[1]);
        bytes(out,1,2,0,0);return out.toByteArray();
    }
    static byte[] hslAdjust(int rgb){ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,2);colorFillNode(out,rgb);bytes(out,0,17,1,0,0,1,0,0);return out.toByteArray();}
    static byte[] hslAdjust(int rgb,int hue,int saturation,int lightness){ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,2);colorFillNode(out,rgb);bytes(out,0,17,1,3,0,hue>>>8,hue,1,saturation,2,lightness,0,1,0,0);return out.toByteArray();}
    static byte[] hslAdjustMonochrome(int value){ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,2,0,0,1,1,0,value,0,17,1,0,0,1,0,0);return out.toByteArray();}
    static byte[] hslAdjustGradient(){return new byte[]{2,0,2,1,0,0,17,1,0,0,1,0,0};}
    static byte[] hslOverflow(){ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,22);colorFillNode(out,0xff0000);for(int node=1;node<=8;node++)bytes(out,0,7,1,2,0,1,1,0,node-1,node-1);colorFillNode(out,0x00ff00);for(int node=10;node<=17;node++)bytes(out,0,7,1,2,0,1,1,0,node-1,node-1);bytes(out,0,7,1,2,0,2,1,0,8,17);colorFillNode(out,0xffffff);bytes(out,0,7,1,2,0,1,1,0,18,19,0,17,1,0,20,21,0,0);return out.toByteArray();}
    private static void colorFillNode(ByteArrayOutputStream out,int rgb){bytes(out,0,1,1,1,0,rgb>>>16,rgb>>>8,rgb);}
    private static void constantRange(ByteArrayOutputStream out,int value,int child){bytes(out,0,30,1,3,0,value>>>8,value,1,value>>>8,value,2,1,child);}
    private static void bytes(ByteArrayOutputStream out,int... values){for(int value:values)out.write(value);}
}
