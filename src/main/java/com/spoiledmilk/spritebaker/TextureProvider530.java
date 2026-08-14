package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only cache-backed material provider with deterministic decoded-image caching. */
public final class TextureProvider530 implements MaterialProvider530 {
    private final CacheReader cache;private final MaterialDefinition530[] definitions;private final ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();private final Map<Integer,TextureMaterial530> loaded=new LinkedHashMap<>();
    public TextureProvider530(CacheReader cache)throws IOException{this.cache=cache;definitions=new MaterialTable530Decoder().decode(cache.loadFile(26,0,0));}
    @Override public TextureMaterial530 material(int id)throws IOException{
        if(id<0||id>=definitions.length||!definitions[id].present)throw new UnsupportedTextureFormatException(id,"missing material metadata");
        TextureMaterial530 hit=loaded.get(id);if(hit!=null)return hit;MaterialDefinition530 def=definitions[id];int size=def.lowDetail?64:128;
        ProceduralTexture530Decoder.Decoded decoded=decoder.decode(id,cache.loadFile(9,id,0),size);TextureMaterial530 material=new TextureMaterial530(def,size,decoded.pixels,decoded.operationTypes);loaded.put(id,material);return material;
    }
    public Map<Integer,TextureMaterial530> loaded(){return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(loaded));}
}
