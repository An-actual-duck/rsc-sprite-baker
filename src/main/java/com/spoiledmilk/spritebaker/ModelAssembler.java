package com.spoiledmilk.spritebaker;

import java.util.List;
import net.runelite.cache.definitions.ModelDefinition;

/** Combines NPC components before animation so transform pivots span every component. */
public final class ModelAssembler {
    private ModelAssembler() { }

    public static ModelDefinition combine(List<ModelDefinition> parts) {
        if (parts.isEmpty()) throw new IllegalArgumentException("at least one model is required");
        ModelDefinition out = new ModelDefinition();
        out.id = -1;
        out.vertexCount = parts.stream().mapToInt(p -> p.vertexCount).sum();
        out.faceCount = parts.stream().mapToInt(p -> p.faceCount).sum();
        out.vertexX=new int[out.vertexCount]; out.vertexY=new int[out.vertexCount]; out.vertexZ=new int[out.vertexCount];
        out.faceIndices1=new int[out.faceCount]; out.faceIndices2=new int[out.faceCount]; out.faceIndices3=new int[out.faceCount];
        out.faceColors=new short[out.faceCount]; out.faceTransparencies=new byte[out.faceCount];
        out.faceTextures=new short[out.faceCount]; out.packedVertexGroups=new int[out.vertexCount];
        out.packedTransparencyVertexGroups=new int[out.faceCount];
        out.numTextureFaces=parts.stream().mapToInt(p -> p.numTextureFaces).sum();
        if(out.numTextureFaces>0){out.texIndices1=new short[out.numTextureFaces];out.texIndices2=new short[out.numTextureFaces];out.texIndices3=new short[out.numTextureFaces];out.textureRenderTypes=new byte[out.numTextureFaces];out.textureCoords=new byte[out.faceCount];java.util.Arrays.fill(out.textureCoords,(byte)-1);}
        java.util.Arrays.fill(out.faceTextures, (short)-1);
        java.util.Arrays.fill(out.packedVertexGroups, -1);
        java.util.Arrays.fill(out.packedTransparencyVertexGroups, -1);
        int vo=0, fo=0, to=0;
        for (ModelDefinition part : parts) {
            System.arraycopy(part.vertexX,0,out.vertexX,vo,part.vertexCount);
            System.arraycopy(part.vertexY,0,out.vertexY,vo,part.vertexCount);
            System.arraycopy(part.vertexZ,0,out.vertexZ,vo,part.vertexCount);
            if (part.packedVertexGroups != null) {
                System.arraycopy(part.packedVertexGroups,0,out.packedVertexGroups,vo,part.vertexCount);
            } else if (part.getVertexGroups() != null) {
                int[][] groups=part.getVertexGroups();
                for(int group=0;group<groups.length;group++)for(int vertex:groups[group])out.packedVertexGroups[vo+vertex]=group;
            }
            for (int i=0;i<part.faceCount;i++) {
                out.faceIndices1[fo+i]=part.faceIndices1[i]+vo;
                out.faceIndices2[fo+i]=part.faceIndices2[i]+vo;
                out.faceIndices3[fo+i]=part.faceIndices3[i]+vo;
            }
            System.arraycopy(part.faceColors,0,out.faceColors,fo,part.faceCount);
            if (part.faceTransparencies != null) System.arraycopy(part.faceTransparencies,0,out.faceTransparencies,fo,part.faceCount);
            if (part.faceTextures != null) System.arraycopy(part.faceTextures,0,out.faceTextures,fo,part.faceCount);
            if(part.numTextureFaces>0){
                for(int i=0;i<part.numTextureFaces;i++){
                    out.texIndices1[to+i]=(short)(Short.toUnsignedInt(part.texIndices1[i])+vo);
                    out.texIndices2[to+i]=(short)(Short.toUnsignedInt(part.texIndices2[i])+vo);
                    out.texIndices3[to+i]=(short)(Short.toUnsignedInt(part.texIndices3[i])+vo);
                }
                if(part.textureRenderTypes!=null)System.arraycopy(part.textureRenderTypes,0,out.textureRenderTypes,to,part.numTextureFaces);
                if(part.textureCoords!=null)for(int i=0;i<part.faceCount;i++)if(part.textureCoords[i]!=-1){int mapped=Byte.toUnsignedInt(part.textureCoords[i])+to;out.textureCoords[fo+i]=mapped<255?(byte)mapped:(byte)-1;}
            }
            if (part.packedTransparencyVertexGroups != null) System.arraycopy(part.packedTransparencyVertexGroups,0,out.packedTransparencyVertexGroups,fo,part.faceCount);
            vo+=part.vertexCount; fo+=part.faceCount;to+=part.numTextureFaces;
        }
        return out;
    }

    static ModelDefinition copy(ModelDefinition source) {
        ModelDefinition out=new ModelDefinition(); out.id=source.id; out.vertexCount=source.vertexCount; out.faceCount=source.faceCount;
        out.vertexX=source.vertexX.clone(); out.vertexY=source.vertexY.clone(); out.vertexZ=source.vertexZ.clone();
        out.faceIndices1=source.faceIndices1.clone(); out.faceIndices2=source.faceIndices2.clone(); out.faceIndices3=source.faceIndices3.clone();
        out.faceColors=source.faceColors.clone();
        out.faceTransparencies=source.faceTransparencies==null?null:source.faceTransparencies.clone();
        out.faceTextures=source.faceTextures==null?null:source.faceTextures.clone();
        out.numTextureFaces=source.numTextureFaces;
        out.texIndices1=source.texIndices1==null?null:source.texIndices1.clone();out.texIndices2=source.texIndices2==null?null:source.texIndices2.clone();out.texIndices3=source.texIndices3==null?null:source.texIndices3.clone();
        out.textureCoords=source.textureCoords==null?null:source.textureCoords.clone();out.textureRenderTypes=source.textureRenderTypes==null?null:source.textureRenderTypes.clone();
        out.packedVertexGroups=source.packedVertexGroups==null?null:source.packedVertexGroups.clone();
        out.packedTransparencyVertexGroups=source.packedTransparencyVertexGroups==null?null:source.packedTransparencyVertexGroups.clone();
        return out;
    }
}
