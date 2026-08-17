package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.runelite.cache.definitions.ModelDefinition;
import org.junit.jupiter.api.Test;

class Revision530Type1ModelDecoderTest {
    private final Revision530Type1ModelDecoder decoder=new Revision530Type1ModelDecoder();

    @Test void decodesEveryOptionalFlagCombinationAndPinnedScalarNarrowing(){
        for(int options=0;options<64;options++){
            ModelDefinition model=decoder.decode(9000+options,Revision530Type1ModelFixture.oneFace(0,options));
            assertArrayEquals(new int[]{0,40,0},model.vertexX);assertArrayEquals(new int[]{0,0,40},model.vertexY);assertArrayEquals(new int[]{0,0,0},model.vertexZ);
            assertArrayEquals(new int[]{0},model.faceIndices1);assertArrayEquals(new int[]{1},model.faceIndices2);assertArrayEquals(new int[]{2},model.faceIndices3);
            assertArrayEquals(new short[]{0x1234},model.faceColors);
            assertEquals((options&1)!=0,model.faceRenderTypes!=null,"triangle-info option "+options);
            assertEquals((options&2)!=0,model.faceRenderPriorities!=null,"priority option "+options);
            if(model.faceRenderPriorities!=null)assertArrayEquals(new byte[]{-2},model.faceRenderPriorities);else assertEquals(7,model.priority);
            assertEquals((options&4)!=0,model.faceTransparencies!=null,"alpha option "+options);
            if(model.faceTransparencies!=null)assertArrayEquals(new byte[]{-128},model.faceTransparencies);
            assertEquals((options&8)!=0,model.packedTransparencyVertexGroups!=null,"face-bone option "+options);
            assertEquals((options&32)!=0,model.getVertexGroups()!=null,"vertex-bone option "+options);
            assertEquals((options&16)!=0,model.faceTextures!=null,"texture option "+options);
            if(model.faceTextures!=null){assertArrayEquals(new short[]{0},model.faceTextures);assertArrayEquals(new byte[]{0},model.textureCoords);}
        }
    }

    @Test void decodesTextureRenderTypesZeroThroughThreeAndEveryComplexField(){
        ModelDefinition530 simple=(ModelDefinition530)decoder.decode(1,Revision530Type1ModelFixture.oneFace(0,63));
        assertArrayEquals(new byte[]{0},simple.textureRenderTypes);assertArrayEquals(new short[]{0},simple.texIndices1);assertArrayEquals(new short[]{1},simple.texIndices2);assertArrayEquals(new short[]{2},simple.texIndices3);
        assertNotNull(simple.faceTextureUCoordinates[0]);assertNull(simple.textureScaleX);
        for(int type=1;type<=3;type++){
            ModelDefinition530 complex=(ModelDefinition530)decoder.decode(type+1,Revision530Type1ModelFixture.oneFace(type,63));
            assertArrayEquals(new byte[]{(byte)type},complex.textureRenderTypes);
            assertArrayEquals(new short[]{(short)0x8001},complex.texIndices1);assertArrayEquals(new short[]{0x7fff},complex.texIndices2);assertArrayEquals(new short[]{-1},complex.texIndices3);
            assertArrayEquals(new short[]{0x1234},complex.textureScaleX);assertArrayEquals(new short[]{(short)0x8000},complex.textureScaleY);assertArrayEquals(new short[]{-1},complex.textureScaleZ);
            assertArrayEquals(new byte[]{-2},complex.textureRotation);assertArrayEquals(new byte[]{-127},complex.textureDirection);assertArrayEquals(new byte[]{127},complex.textureTranslation);
            if(type==2){assertArrayEquals(new byte[]{-128},complex.textureCubeU);assertArrayEquals(new byte[]{126},complex.textureCubeV);}else{assertNull(complex.textureCubeU);assertNull(complex.textureCubeV);}
        }
    }

    @Test void decodesRepresentativesOfAllFiveAuditedUnderflowSignatures(){
        for(int complex=7;complex<=11;complex++){
            byte[] bytes=Revision530Type1ModelFixture.complexSignature(complex);
            assertTrue(decoder.matches(bytes));
            ModelDefinition530 model=(ModelDefinition530)decoder.decode(10000+complex,bytes);
            assertEquals(complex,model.numTextureFaces);assertEquals(complex-1,Byte.toUnsignedInt(model.textureRotation[complex-1]));
            @SuppressWarnings("unchecked") java.util.Map<String,Object> dependency=(java.util.Map<String,Object>)ModelFormatDiagnostic.analyze(1,bytes).get("dependencyType1");
            assertEquals(complex*2L,((Number)dependency.get("dataEndMinusFooter")).longValue());
        }
    }

    @Test void rejectsTruncationUnknownTypesExtensionsAndStructuralMismatches(){
        assertFalse(decoder.matches(new byte[22]));
        byte[] unknown=Revision530Type1ModelFixture.oneFace(0,0);unknown[0]=4;assertFalse(decoder.matches(unknown));
        byte[] extension=Revision530Type1ModelFixture.oneFace(0,0);extension[Revision530Type1ModelFixture.footer(extension)+5]=2;assertFalse(decoder.matches(extension));
        byte[] mismatch=Revision530Type1ModelFixture.oneFace(0,0);int footer=Revision530Type1ModelFixture.footer(mismatch);Revision530Type1ModelFixture.putU16(mismatch,footer+11,3);assertFalse(decoder.matches(mismatch));
        byte[] overflow=Revision530Type1ModelFixture.oneFace(0,0);footer=Revision530Type1ModelFixture.footer(overflow);Revision530Type1ModelFixture.putU16(overflow,footer+11,65535);
        IllegalArgumentException failure=assertThrows(IllegalArgumentException.class,()->decoder.decode(5,overflow));assertTrue(failure.getMessage().contains("vertex-x boundary"));assertTrue(failure.getMessage().contains("requested 65535 bytes"));
    }

    @Test void validatesSmartAndCoordinateReadsAgainstTheirDeclaredStreams(){
        byte[] vertex=Revision530Type1ModelFixture.oneFace(0,0);int footer=Revision530Type1ModelFixture.footer(vertex);
        Revision530Type1ModelFixture.putU16(vertex,footer+11,1);Revision530Type1ModelFixture.putU16(vertex,footer+13,2);
        IllegalArgumentException vertexFailure=assertThrows(IllegalArgumentException.class,()->decoder.decode(6,vertex));assertTrue(vertexFailure.getMessage().contains("vertex-x"));assertTrue(vertexFailure.getMessage().contains("requested 1 bytes"));

        byte[] indices=Revision530Type1ModelFixture.oneFace(0,0);indices[5]=(byte)128;
        IllegalArgumentException indexFailure=assertThrows(IllegalArgumentException.class,()->decoder.decode(7,indices));assertTrue(indexFailure.getMessage().contains("face-indices"));

        byte[] coordinates=Revision530Type1ModelFixture.oneFace(0,16);coordinates[8]=0;coordinates[9]=0;
        IllegalArgumentException coordinateFailure=assertThrows(IllegalArgumentException.class,()->decoder.decode(8,coordinates));assertTrue(coordinateFailure.getMessage().contains("texture-coordinates"));
    }

    @Test void selectorLeavesOldTypeTwoAndTypeThreeMarkersOutsideCorrectedPath(){
        byte[] old=new byte[23],type2=new byte[23],type3=new byte[26];type2[21]=(byte)255;type2[22]=(byte)254;type3[24]=(byte)255;type3[25]=(byte)253;
        assertFalse(decoder.matches(old));assertFalse(decoder.matches(type2));assertFalse(decoder.matches(type3));
    }

    @Test void decodedTypeZeroModelRendersThroughMaterialProviderDeterministically(){
        ModelDefinition model=decoder.decode(9,Revision530Type1ModelFixture.oneFace(0,16));
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,false,false,0,0,0,0,0);
        TextureMaterial530 material=new TextureMaterial530(definition,2,new int[]{0xff0000,0x00ff00,0x0000ff,0xffffff},List.of(0));
        NpcDefinition530 npc=new NpcDefinition530(1);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        StaticRenderer renderer=new StaticRenderer();int[] first=renderer.renderStyled(List.of(model),npc,0,null,settings,id->material).getRGB(0,0,40,40,null,0,40),second=renderer.renderStyled(List.of(model),npc,0,null,settings,id->material).getRGB(0,0,40,40,null,0,40);
        assertArrayEquals(first,second);assertTrue(java.util.Arrays.stream(first).anyMatch(pixel->(pixel>>>24)==255));
    }

    @Test void pinnedAdvancedMappingsProduceDeterministicCoordinatesAndRenders(){
        double[][] expected={
            {13.75941276550293,0.95703125,45.7589225769043,0.95703125,13.75941276550293,1.03515625},
            {1.2704410552978516,0.0390625,0.7217465043067932,0.0390625,1.2704410552978516,-0.0390625},
            {0.2500016689300537,1.451181173324585,0.7500016689300537,1.451181173324585,0.2500016689300537,1.541006326675415}};
        MaterialDefinition530 definition=new MaterialDefinition530(0,true,true,true,false,false,0,0,0,0,0);
        TextureMaterial530 material=new TextureMaterial530(definition,2,new int[]{0xff0000,0x00ff00,0x0000ff,0xffffff},List.of(0));
        NpcDefinition530 npc=new NpcDefinition530(2);VisualSettings settings=new VisualSettings();settings.cellWidth=40;settings.cellHeight=40;settings.supersample=1;settings.padding=4;settings.palette=PaletteReducer.UNMODIFIED;
        for(int type=1;type<=3;type++){
            ModelDefinition model=decoder.decode(20+type,Revision530Type1ModelFixture.oneFace(type,63));
            assertArrayEquals(expected[type-1],StaticRenderer.textureCoordinates(model,0),0.0);
            int[] first=new StaticRenderer().renderStyled(List.of(model),npc,0,null,settings,id->material).getRGB(0,0,40,40,null,0,40);
            int[] second=new StaticRenderer().renderStyled(List.of(model),npc,0,null,settings,id->material).getRGB(0,0,40,40,null,0,40);
            assertArrayEquals(first,second);assertTrue(java.util.Arrays.stream(first).anyMatch(pixel->(pixel>>>24)!=0));
        }
    }
}
