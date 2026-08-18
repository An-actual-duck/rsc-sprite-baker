package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class MaterialTable530DecoderTest {
    @Test void decodesNeutralParallelArrayFixture(){
        ByteArrayOutputStream b=new ByteArrayOutputStream();bytes(b,0,2,1,0); // count, presence
        bytes(b,1,0,1,1); // four flags for the one present entry
        bytes(b,0xfe,3,4,5,0x12,0x34); // four signed bytes, average color
        MaterialDefinition530[] out=new MaterialTable530Decoder().decode(b.toByteArray());
        assertEquals(2,out.length);assertTrue(out[0].present);assertTrue(out[0].modelTextureFlag);assertFalse(out[0].opaque);assertTrue(out[0].lowDetail);assertTrue(out[0].materialFlag3);
        assertEquals(-2,out[0].scrollU);assertEquals(3,out[0].scrollV);assertEquals(254,out[0].colorBoost());assertEquals(3,out[0].grayscaleBlend());assertEquals(4,out[0].materialType());assertEquals(5,out[0].materialArgument());assertEquals(0x1234,out[0].averageColor);assertFalse(out[1].present);
    }
    private static void bytes(ByteArrayOutputStream out,int... values){for(int value:values)out.write(value);}
}
