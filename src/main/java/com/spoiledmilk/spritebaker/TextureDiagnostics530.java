package com.spoiledmilk.spritebaker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.runelite.cache.definitions.ModelDefinition;

/** User-facing compatibility diagnosis for a model's materials and mapping modes. */
public final class TextureDiagnostics530 {
    private TextureDiagnostics530(){}
    public static Report analyze(ModelDefinition model,NpcDefinition530 npc,TextureProvider530 provider){
        Set<Integer> ids=new LinkedHashSet<>();int textured=0,type0=0,advanced=0,local=0;
        if(model.faceTextures!=null)for(int f=0;f<model.faceCount;f++){int id=texture(model.faceTextures[f],npc);if(id<0)continue;textured++;ids.add(id);if(model.textureCoords==null||model.textureCoords[f]==-1)local++;else{int t=Byte.toUnsignedInt(model.textureCoords[f]);if(t<model.numTextureFaces&&model.textureRenderTypes!=null&&model.textureRenderTypes[t]==0)type0++;else advanced++;}}
        List<Integer> supported=new ArrayList<>();List<String> errors=new ArrayList<>();for(int id:ids)try{provider.material(id);supported.add(id);}catch(Exception e){errors.add(e.getMessage());}
        return new Report(new ArrayList<>(ids),supported,errors,textured,type0,advanced,local);
    }
    private static int texture(short source,NpcDefinition530 npc){if(source==-1)return-1;for(int i=0;i<npc.retextureFrom.length;i++)if(source==npc.retextureFrom[i])return Short.toUnsignedInt(npc.retextureTo[i]);return Short.toUnsignedInt(source);}
    public static final class Report{
        public final List<Integer> materialIds,supportedMaterialIds;public final List<String> errors;public final int texturedFaces,type0Mappings,advancedMappingFallbacks,faceLocalMappings;
        Report(List<Integer> ids,List<Integer> supported,List<String> errors,int faces,int type0,int advanced,int local){materialIds=List.copyOf(ids);supportedMaterialIds=List.copyOf(supported);this.errors=List.copyOf(errors);texturedFaces=faces;type0Mappings=type0;advancedMappingFallbacks=advanced;faceLocalMappings=local;}
        public boolean supported(){return errors.isEmpty();}
        public String summary(){if(texturedFaces==0)return"Untextured model";return supported()?"Textured: "+texturedFaces+" faces, materials "+materialIds+", advanced mappings "+advancedMappingFallbacks:"Unsupported material: "+String.join("; ",errors);}
    }
}
