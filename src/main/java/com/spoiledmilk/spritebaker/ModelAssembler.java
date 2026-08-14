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
        java.util.Arrays.fill(out.faceTextures, (short)-1);
        java.util.Arrays.fill(out.packedVertexGroups, -1);
        java.util.Arrays.fill(out.packedTransparencyVertexGroups, -1);
        int vo=0, fo=0;
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
            if (part.packedTransparencyVertexGroups != null) System.arraycopy(part.packedTransparencyVertexGroups,0,out.packedTransparencyVertexGroups,fo,part.faceCount);
            vo+=part.vertexCount; fo+=part.faceCount;
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
        out.packedVertexGroups=source.packedVertexGroups==null?null:source.packedVertexGroups.clone();
        out.packedTransparencyVertexGroups=source.packedTransparencyVertexGroups==null?null:source.packedTransparencyVertexGroups.clone();
        return out;
    }
}
