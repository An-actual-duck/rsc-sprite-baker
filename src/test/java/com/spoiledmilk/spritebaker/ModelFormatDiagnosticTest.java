package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.nio.BufferUnderflowException;
import java.util.Map;
import net.runelite.cache.definitions.loaders.ModelLoader;
import org.junit.jupiter.api.Test;

class ModelFormatDiagnosticTest {
    @Test void tracesPinnedType1ComplexTextureLayoutToExactDependencyUnderflow(){
        byte[] model=revision530ComplexTextureFixture();Map<String,Object> report=ModelFormatDiagnostic.analyze(9001,model);
        assertEquals("type-1",report.get("format"));assertEquals("ffff",report.get("revisionMarker"));assertEquals(114.0,number(map(report,"pinnedRevision530").get("calculatedDataEnd")));assertEquals(114.0,number(report.get("footerOffset")));
        assertEquals(128.0,number(map(report,"dependencyType1").get("calculatedDataEnd")));assertEquals(14.0,number(map(report,"dependencyType1").get("dataEndMinusFooter")));
        Map<String,Object> failure=map(report,"failure");assertEquals("extension-metadata-i32",failure.get("stage"));assertEquals("dependency-extension",failure.get("stream"));assertEquals(135.0,number(failure.get("offset")));assertEquals(4.0,number(failure.get("requestedBytes")));assertEquals(2.0,number(failure.get("remainingBytes")));
        assertEquals(ModelFormatDiagnostic.ROOT_CAUSE,report.get("likelyRootCause"));
        assertThrows(BufferUnderflowException.class,()->new ModelLoader().load(9001,model));
    }

    @Test void recordsUnsignedCountsFlagsFooterAndTextureTypes(){
        Map<String,Object> report=ModelFormatDiagnostic.analyze(9002,revision530ComplexTextureFixture());Map<String,Object> counts=map(report,"counts"),lengths=map(report,"serializedLengths");
        assertEquals(1.0,number(counts.get("vertices")));assertEquals(0.0,number(counts.get("faces")));assertEquals(7.0,number(counts.get("textureFaces")));assertEquals(7.0,number(counts.get("complexTextureFaces")));assertEquals(0.0,number(counts.get("unknownTextureTypes")));assertEquals(1.0,number(lengths.get("vertexY")));
        assertTrue((Boolean)map(report,"pinnedRevision530").get("layoutMatchesFooter"));assertFalse((Boolean)map(report,"flags").get("particleExtension"));
    }

    @Test void distinguishesEveryDependencyFormatMarkerWithoutSpeculativeParsing(){
        byte[] type3=new byte[26];type3[24]=(byte)0xff;type3[25]=(byte)0xfd;
        byte[] type2=new byte[23];type2[21]=(byte)0xff;type2[22]=(byte)0xfe;
        byte[] old=new byte[18];
        assertEquals("type-3",ModelFormatDiagnostic.analyze(1,type3).get("format"));assertEquals("type-2",ModelFormatDiagnostic.analyze(2,type2).get("format"));assertEquals("old",ModelFormatDiagnostic.analyze(3,old).get("format"));
    }

    @Test void reportsTruncatedFormatSelectionDeterministically(){
        Map<String,Object> first=ModelFormatDiagnostic.analyze(4,new byte[]{(byte)0xff}),second=ModelFormatDiagnostic.analyze(4,new byte[]{(byte)0xff});
        assertEquals("unselectable",first.get("format"));Map<String,Object> failure=map(first,"failure");assertEquals("format-selection",failure.get("stage"));assertEquals(2.0,number(failure.get("requestedBytes")));assertEquals(1.0,number(failure.get("remainingBytes")));assertEquals(new Gson().toJson(first),new Gson().toJson(second));
    }

    private static byte[] revision530ComplexTextureFixture(){
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(int i=0;i<7;i++)out.write(1);out.write(2);out.write(64);
        for(int i=0;i<42+42+7+7+7;i++)out.write(0);
        bytes(out,0,1,0,0); // one vertex, no faces
        bytes(out,7,0,0,0,0,0,0); // texture count and six flags
        bytes(out,0,0,0,1,0,0,0,0,0,0,0xff,0xff); // x/y/z/index/coord lengths and marker
        assertEquals(137,out.size());return out.toByteArray();
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Map<String,Object> source,String key){return(Map<String,Object>)source.get(key);}
    private static double number(Object value){return((Number)value).doubleValue();}
    private static void bytes(ByteArrayOutputStream out,int... values){for(int value:values)out.write(value);}
}
