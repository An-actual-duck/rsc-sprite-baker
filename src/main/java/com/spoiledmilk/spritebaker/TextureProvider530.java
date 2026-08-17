package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import net.runelite.cache.definitions.SpriteDefinition;
import net.runelite.cache.definitions.loaders.SpriteLoader;

/** Read-only cache-backed material provider with deterministic decoded-image caching. */
public final class TextureProvider530 implements MaterialProvider530 {
    private final MaterialDefinition530[] definitions;private final GraphSource graphs;private final SpriteSource sprites;private final ProceduralTexture530Decoder decoder=new ProceduralTexture530Decoder();private final Map<Integer,TextureMaterial530> loaded=new LinkedHashMap<>();private final ArrayDeque<Integer> loading=new ArrayDeque<>();
    public TextureProvider530(CacheReader cache)throws IOException{this(new MaterialTable530Decoder().decode(cache.loadFile(26,0,0)),id->cache.loadFile(9,id,0),id->loadSprite(cache,id));}
    TextureProvider530(MaterialDefinition530[] definitions,GraphSource graphs){this(definitions,graphs,id->{throw new UnsupportedTextureFormatException(id,"sprite dependency "+id+" requires a provider");});}
    TextureProvider530(MaterialDefinition530[] definitions,GraphSource graphs,SpriteSource sprites){this.definitions=definitions.clone();this.graphs=graphs;this.sprites=sprites;}
    @Override public synchronized TextureMaterial530 material(int id)throws IOException{
        if(id<0||id>=definitions.length||!definitions[id].present)throw new UnsupportedTextureFormatException(id,"missing material metadata");
        TextureMaterial530 hit=loaded.get(id);if(hit!=null)return hit;
        if(loading.contains(id))throw new UnsupportedTextureFormatException(id,"recursive texture dependency "+dependencyPath(id));
        if(loading.size()>=64)throw new UnsupportedTextureFormatException(id,"texture dependency depth exceeds 64: "+dependencyPath(id));
        loading.addLast(id);
        try{
            MaterialDefinition530 def=definitions[id];int size=def.lowDetail?64:128;
            ProceduralTexture530Decoder.Decoded decoded=decoder.decode(id,graphs.load(id),size,dependencyId->{TextureMaterial530 dependency=material(dependencyId);return new ProceduralTexture530Decoder.Dependency(dependency.size,dependency.pixels);},sprites::load);
            TextureMaterial530 material=new TextureMaterial530(def,size,decoded.pixels,decoded.operationTypes);loaded.put(id,material);return material;
        }finally{loading.removeLast();}
    }
    public Map<Integer,TextureMaterial530> loaded(){return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(loaded));}
    private String dependencyPath(int repeated){StringJoiner path=new StringJoiner(" -> ");for(int id:loading)path.add(Integer.toString(id));path.add(Integer.toString(repeated));return path.toString();}
    private static ProceduralTexture530Decoder.SpriteDependency loadSprite(CacheReader cache,int id)throws IOException{
        SpriteDefinition[] decoded=new SpriteLoader().load(id,cache.loadFile(8,id,0));
        if(decoded.length==0)throw new UnsupportedTextureFormatException(id,"empty sprite dependency");
        SpriteDefinition frame=decoded[0];int width=frame.getMaxWidth(),height=frame.getMaxHeight();
        if(width<=0||height<=0||frame.getWidth()<0||frame.getHeight()<0||frame.getOffsetX()<0||frame.getOffsetY()<0||frame.getOffsetX()+frame.getWidth()>width||frame.getOffsetY()+frame.getHeight()>height)throw new UnsupportedTextureFormatException(id,"invalid sprite dependency bounds");
        int[] source=frame.getPixels();if(source==null||source.length!=frame.getWidth()*frame.getHeight())throw new UnsupportedTextureFormatException(id,"invalid sprite dependency pixels");
        int[] pixels=new int[width*height];for(int y=0;y<frame.getHeight();y++)System.arraycopy(source,y*frame.getWidth(),pixels,(y+frame.getOffsetY())*width+frame.getOffsetX(),frame.getWidth());
        return new ProceduralTexture530Decoder.SpriteDependency(width,height,pixels);
    }
    @FunctionalInterface interface GraphSource{byte[] load(int id)throws IOException;}
    @FunctionalInterface interface SpriteSource{ProceduralTexture530Decoder.SpriteDependency load(int id)throws IOException;}
}
