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
        byte[] fixture={1,0,34,1,0};
        UnsupportedTextureFormatException e=assertThrows(UnsupportedTextureFormatException.class,()->new ProceduralTexture530Decoder().decode(901,fixture,4));
        assertTrue(e.getMessage().contains("operation 34"));
    }
    @Test void operation36SamplesNestedTextureAtDependencyResolution()throws Exception{
        byte[] fixture=textureDependency(7);
        int[] dependency={0x0000ff,0xff0000,0xffffff,0x000000}; // stored horizontally reversed
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(902,fixture,4,
            id->{assertEquals(7,id);return new ProceduralTexture530Decoder.Dependency(2,dependency);});
        assertArrayEquals(new int[]{0x0000ff,0x0000ff,0xff0000,0xff0000},java.util.Arrays.copyOfRange(decoded.pixels,0,4));
        assertEquals(java.util.List.of(36),decoded.operationTypes);
    }
    static byte[] textureDependency(int id){return new byte[]{1,0,36,1,1,0,(byte)(id>>>8),(byte)id,0,0,0};}
    private static void bytes(ByteArrayOutputStream out,int... values){for(int value:values)out.write(value);}
}
