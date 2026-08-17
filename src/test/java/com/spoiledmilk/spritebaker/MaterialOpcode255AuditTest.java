package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class MaterialOpcode255AuditTest {
    @Test void oneBytePinnedFillKeepsCurveCache255OutOfTheOperationTypeField(){
        byte[] graph=neutralGraph();
        MaterialOpcode255Audit.Trace legacy=MaterialOpcode255Audit.trace(graph,false),pinned=MaterialOpcode255Audit.trace(graph,true);
        assertEquals("apparent operation 255",legacy.stop);assertEquals(255,legacy.nodes.get(1).type);assertEquals(9,legacy.nodes.get(1).typeOffset);
        assertNull(pinned.stop);assertTrue(pinned.consumed==pinned.total);assertEquals(8,pinned.nodes.get(1).type);assertEquals(255,pinned.nodes.get(1).cache);assertEquals(8,pinned.nodes.get(1).typeOffset);assertEquals(9,pinned.nodes.get(1).cacheOffset);
        ProceduralTexture530Decoder.Decoded decoded=new ProceduralTexture530Decoder().decode(168,graph,4);
        assertEquals(java.util.List.of(0,8),decoded.operationTypes);assertTrue(java.util.Arrays.stream(decoded.pixels).allMatch(pixel->pixel==0xcbcbcb));
    }
    @Test void pinnedTraceRejectsTruncationRatherThanInventingAnOperation(){
        byte[] graph=neutralGraph(),truncated=java.util.Arrays.copyOf(graph,graph.length-4);MaterialOpcode255Audit.Trace trace=MaterialOpcode255Audit.trace(truncated,true);
        assertNotNull(trace.stop);assertTrue(trace.stop.contains("trailing material-metadata bytes")||trace.stop.contains("truncated"));assertFalse((Boolean)trace.report().get("complete"));
    }
    private static byte[] neutralGraph(){ByteArrayOutputStream out=new ByteArrayOutputStream();bytes(out,2,0,0,1,1,0,203);bytes(out,1,8,255,1,0,0,2,0,0,0,0,16,0,16,0,0);bytes(out,1,0);bytes(out,1,1,1,1,0,0,0,0,0);return out.toByteArray();}
    private static void bytes(ByteArrayOutputStream out,int... values){for(int value:values)out.write(value);}
}
