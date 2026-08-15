package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/** Read-only cache-backed material provider with deterministic decoded-image caching. */
public final class TextureProvider530 implements MaterialProvider530 {
    private final MaterialDefinition530[] definitions;private final GraphSource graphs;private final ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();private final Map<Integer,TextureMaterial530> loaded=new LinkedHashMap<>();private final ArrayDeque<Integer> loading=new ArrayDeque<>();
    public TextureProvider530(CacheReader cache)throws IOException{this(new MaterialTable530Decoder().decode(cache.loadFile(26,0,0)),id->cache.loadFile(9,id,0));}
    TextureProvider530(MaterialDefinition530[] definitions,GraphSource graphs){this.definitions=definitions.clone();this.graphs=graphs;}
    @Override public synchronized TextureMaterial530 material(int id)throws IOException{
        if(id<0||id>=definitions.length||!definitions[id].present)throw new UnsupportedTextureFormatException(id,"missing material metadata");
        TextureMaterial530 hit=loaded.get(id);if(hit!=null)return hit;
        if(loading.contains(id))throw new UnsupportedTextureFormatException(id,"recursive texture dependency "+dependencyPath(id));
        if(loading.size()>=64)throw new UnsupportedTextureFormatException(id,"texture dependency depth exceeds 64: "+dependencyPath(id));
        loading.addLast(id);
        try{
            MaterialDefinition530 def=definitions[id];int size=def.lowDetail?64:128;
            ProceduralTexture530Decoder.Decoded decoded=decoder.decode(id,graphs.load(id),size,dependencyId->{TextureMaterial530 dependency=material(dependencyId);return new ProceduralTexture530Decoder.Dependency(dependency.size,dependency.pixels);});
            TextureMaterial530 material=new TextureMaterial530(def,size,decoded.pixels,decoded.operationTypes);loaded.put(id,material);return material;
        }finally{loading.removeLast();}
    }
    public Map<Integer,TextureMaterial530> loaded(){return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(loaded));}
    private String dependencyPath(int repeated){StringJoiner path=new StringJoiner(" -> ");for(int id:loading)path.add(Integer.toString(id));path.add(Integer.toString(repeated));return path.toString();}
    @FunctionalInterface interface GraphSource{byte[] load(int id)throws IOException;}
}
