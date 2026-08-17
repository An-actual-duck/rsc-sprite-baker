package com.spoiledmilk.spritebaker;

import java.util.List;
import net.runelite.cache.definitions.ModelDefinition;

/** Combines NPC components before animation so transform pivots span every component. */
public final class ModelAssembler {
    private ModelAssembler() { }

    public static ModelDefinition combine(List<ModelDefinition> parts) {
        if (parts.isEmpty()) throw new IllegalArgumentException("at least one model is required");
        boolean extended=parts.stream().anyMatch(ModelDefinition530.class::isInstance);
        ModelDefinition out = extended ? new ModelDefinition530() : new ModelDefinition();
        out.id = -1;
        out.vertexCount = parts.stream().mapToInt(p -> p.vertexCount).sum();
        out.faceCount = parts.stream().mapToInt(p -> p.faceCount).sum();
        out.vertexX=new int[out.vertexCount]; out.vertexY=new int[out.vertexCount]; out.vertexZ=new int[out.vertexCount];
        out.faceIndices1=new int[out.faceCount]; out.faceIndices2=new int[out.faceCount]; out.faceIndices3=new int[out.faceCount];
        out.faceColors=new short[out.faceCount]; out.faceTransparencies=new byte[out.faceCount];
        if(parts.stream().anyMatch(p->p.faceRenderTypes!=null))out.faceRenderTypes=new byte[out.faceCount];
        if(parts.stream().anyMatch(p->p.faceRenderPriorities!=null))out.faceRenderPriorities=new byte[out.faceCount];
        out.faceTextures=new short[out.faceCount]; out.packedVertexGroups=new int[out.vertexCount];
        out.packedTransparencyVertexGroups=new int[out.faceCount];
        out.numTextureFaces=parts.stream().mapToInt(p -> p.numTextureFaces).sum();
        if(out.numTextureFaces>0){out.texIndices1=new short[out.numTextureFaces];out.texIndices2=new short[out.numTextureFaces];out.texIndices3=new short[out.numTextureFaces];out.textureRenderTypes=new byte[out.numTextureFaces];out.textureCoords=new byte[out.faceCount];java.util.Arrays.fill(out.textureCoords,(byte)-1);}
        if(extended&&out.numTextureFaces>0){ModelDefinition530 mapped=(ModelDefinition530)out;mapped.textureScaleX=new short[out.numTextureFaces];mapped.textureScaleY=new short[out.numTextureFaces];mapped.textureScaleZ=new short[out.numTextureFaces];mapped.textureRotation=new byte[out.numTextureFaces];mapped.textureDirection=new byte[out.numTextureFaces];mapped.textureTranslation=new byte[out.numTextureFaces];mapped.textureCubeU=new byte[out.numTextureFaces];mapped.textureCubeV=new byte[out.numTextureFaces];}
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
            if(out.faceRenderTypes!=null&&part.faceRenderTypes!=null)System.arraycopy(part.faceRenderTypes,0,out.faceRenderTypes,fo,part.faceCount);
            if(out.faceRenderPriorities!=null){if(part.faceRenderPriorities!=null)System.arraycopy(part.faceRenderPriorities,0,out.faceRenderPriorities,fo,part.faceCount);else java.util.Arrays.fill(out.faceRenderPriorities,fo,fo+part.faceCount,part.priority);}
            if (part.faceTransparencies != null) System.arraycopy(part.faceTransparencies,0,out.faceTransparencies,fo,part.faceCount);
            if (part.faceTextures != null) System.arraycopy(part.faceTextures,0,out.faceTextures,fo,part.faceCount);
            if(part.numTextureFaces>0){
                for(int i=0;i<part.numTextureFaces;i++){
                    boolean vertices=part.textureRenderTypes==null||part.textureRenderTypes[i]==0;
                    out.texIndices1[to+i]=vertices?(short)(Short.toUnsignedInt(part.texIndices1[i])+vo):part.texIndices1[i];
                    out.texIndices2[to+i]=vertices?(short)(Short.toUnsignedInt(part.texIndices2[i])+vo):part.texIndices2[i];
                    out.texIndices3[to+i]=vertices?(short)(Short.toUnsignedInt(part.texIndices3[i])+vo):part.texIndices3[i];
                }
                if(part.textureRenderTypes!=null)System.arraycopy(part.textureRenderTypes,0,out.textureRenderTypes,to,part.numTextureFaces);
                if(part.textureCoords!=null)for(int i=0;i<part.faceCount;i++)if(part.textureCoords[i]!=-1){int mapped=Byte.toUnsignedInt(part.textureCoords[i])+to;if(mapped>=255)throw new IllegalArgumentException("combined texture-coordinate index "+mapped+" is not representable");out.textureCoords[fo+i]=(byte)mapped;}
                if(extended&&part instanceof ModelDefinition530){copyMappings((ModelDefinition530)part,(ModelDefinition530)out,to,part.numTextureFaces);}
            }
            if (part.packedTransparencyVertexGroups != null) System.arraycopy(part.packedTransparencyVertexGroups,0,out.packedTransparencyVertexGroups,fo,part.faceCount);
            vo+=part.vertexCount; fo+=part.faceCount;to+=part.numTextureFaces;
        }
        return out;
    }

    static ModelDefinition copy(ModelDefinition source) {
        ModelDefinition out=source instanceof ModelDefinition530?new ModelDefinition530():new ModelDefinition(); out.id=source.id; out.vertexCount=source.vertexCount; out.faceCount=source.faceCount;
        out.vertexX=source.vertexX.clone(); out.vertexY=source.vertexY.clone(); out.vertexZ=source.vertexZ.clone();
        out.faceIndices1=source.faceIndices1.clone(); out.faceIndices2=source.faceIndices2.clone(); out.faceIndices3=source.faceIndices3.clone();
        out.faceColors=source.faceColors.clone();
        out.faceRenderTypes=source.faceRenderTypes==null?null:source.faceRenderTypes.clone();
        out.faceRenderPriorities=source.faceRenderPriorities==null?null:source.faceRenderPriorities.clone();out.priority=source.priority;
        out.faceTransparencies=source.faceTransparencies==null?null:source.faceTransparencies.clone();
        out.faceTextures=source.faceTextures==null?null:source.faceTextures.clone();
        out.numTextureFaces=source.numTextureFaces;
        out.texIndices1=source.texIndices1==null?null:source.texIndices1.clone();out.texIndices2=source.texIndices2==null?null:source.texIndices2.clone();out.texIndices3=source.texIndices3==null?null:source.texIndices3.clone();
        out.textureCoords=source.textureCoords==null?null:source.textureCoords.clone();out.textureRenderTypes=source.textureRenderTypes==null?null:source.textureRenderTypes.clone();
        out.packedVertexGroups=source.packedVertexGroups==null?null:source.packedVertexGroups.clone();
        out.packedTransparencyVertexGroups=source.packedTransparencyVertexGroups==null?null:source.packedTransparencyVertexGroups.clone();
        if(source instanceof ModelDefinition530)copyMappings((ModelDefinition530)source,(ModelDefinition530)out,0,source.numTextureFaces);
        return out;
    }

    private static void copyMappings(ModelDefinition530 source,ModelDefinition530 target,int offset,int length){
        target.textureScaleX=copy(source.textureScaleX,target.textureScaleX,offset,length);
        target.textureScaleY=copy(source.textureScaleY,target.textureScaleY,offset,length);
        target.textureScaleZ=copy(source.textureScaleZ,target.textureScaleZ,offset,length);
        target.textureRotation=copy(source.textureRotation,target.textureRotation,offset,length);
        target.textureDirection=copy(source.textureDirection,target.textureDirection,offset,length);
        target.textureTranslation=copy(source.textureTranslation,target.textureTranslation,offset,length);
        target.textureCubeU=copy(source.textureCubeU,target.textureCubeU,offset,length);
        target.textureCubeV=copy(source.textureCubeV,target.textureCubeV,offset,length);
    }
    private static short[] copy(short[] source,short[] target,int offset,int length){if(source==null)return target;if(target==null)target=new short[offset+length];System.arraycopy(source,0,target,offset,length);return target;}
    private static byte[] copy(byte[] source,byte[] target,int offset,int length){if(source==null)return target;if(target==null)target=new byte[offset+length];System.arraycopy(source,0,target,offset,length);return target;}
}
