package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import net.runelite.cache.definitions.ModelDefinition;
import org.junit.jupiter.api.Test;

class ModelAssemblerTextureTest {
    @Test void offsetsNeutralComponentVerticesAndTextureMappings(){
        ModelDefinition a=part(1,(byte)0),b=part(2,(byte)2);ModelDefinition combined=ModelAssembler.combine(List.of(a,b));
        assertEquals(6,combined.vertexCount);assertEquals(2,combined.numTextureFaces);
        assertArrayEquals(new short[]{0,0},combined.texIndices1);assertArrayEquals(new short[]{1,1},combined.texIndices2);assertArrayEquals(new short[]{2,2},combined.texIndices3);
        assertArrayEquals(new byte[]{0,1},combined.textureCoords);assertArrayEquals(new byte[]{0,2},combined.textureRenderTypes);
        ModelDefinition copy=ModelAssembler.copy(combined);assertArrayEquals(combined.textureCoords,copy.textureCoords);assertNotSame(combined.textureCoords,copy.textureCoords);
    }
    @Test void preservesRevision530ComplexMappingsWithoutTreatingPmnAsVertexIndices(){
        Revision530Type1ModelDecoder decoder=new Revision530Type1ModelDecoder();
        ModelDefinition530 first=(ModelDefinition530)decoder.decode(3,Revision530Type1ModelFixture.oneFace(2,63));
        ModelDefinition530 second=(ModelDefinition530)decoder.decode(4,Revision530Type1ModelFixture.oneFace(2,63));
        ModelDefinition530 combined=(ModelDefinition530)ModelAssembler.combine(List.of(first,second));
        assertArrayEquals(new short[]{(short)0x8001,(short)0x8001},combined.texIndices1);
        assertArrayEquals(new short[]{0x1234,0x1234},combined.textureScaleX);
        assertArrayEquals(new byte[]{-2,-2},combined.textureRotation);assertArrayEquals(new byte[]{-128,-128},combined.textureCubeU);
        assertArrayEquals(new byte[]{0,1},combined.textureCoords);
        ModelDefinition530 copy=(ModelDefinition530)ModelAssembler.copy(combined);assertArrayEquals(combined.textureScaleZ,copy.textureScaleZ);assertNotSame(combined.textureScaleZ,copy.textureScaleZ);
    }
    private static ModelDefinition part(int id,byte type){ModelDefinition m=new ModelDefinition();m.id=id;m.vertexCount=3;m.vertexX=new int[]{0,1,0};m.vertexY=new int[]{0,0,1};m.vertexZ=new int[]{0,0,0};m.faceCount=1;m.faceIndices1=new int[]{0};m.faceIndices2=new int[]{1};m.faceIndices3=new int[]{2};m.faceColors=new short[]{1};m.faceTextures=new short[]{5};m.numTextureFaces=1;m.texIndices1=new short[]{0};m.texIndices2=new short[]{1};m.texIndices3=new short[]{2};m.textureCoords=new byte[]{0};m.textureRenderTypes=new byte[]{type};return m;}
}
