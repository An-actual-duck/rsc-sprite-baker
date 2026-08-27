package com.spoiledmilk.spritebaker;

import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Optional read-only combat-role relationships adjacent to a compatible cache. */
final class CombatMetadata530 {
    private CombatMetadata530(){ }

    static List<Entry> load(Path cacheDirectory,int npcId)throws IOException{
        Path file=adjacentFile(cacheDirectory);
        return file==null?List.of():loadFile(file,npcId);
    }

    static Path adjacentFile(Path cacheDirectory){
        Path parent=cacheDirectory.toAbsolutePath().normalize().getParent();
        if(parent==null)return null;
        Path candidate=parent.resolve("configs").resolve("npc_configs.json");
        return Files.isRegularFile(candidate)?candidate:null;
    }

    static List<Entry> loadFile(Path file,int npcId)throws IOException{
        Map<Integer,List<String>> roles=new LinkedHashMap<>();
        try(JsonReader in=new JsonReader(Files.newBufferedReader(file))){
            in.beginArray();
            while(in.hasNext()){
                Integer id=null;Map<String,Integer> values=new LinkedHashMap<>();in.beginObject();
                while(in.hasNext()){
                    String name=in.nextName();
                    if("id".equals(name))id=readInteger(in);
                    else if("melee_animation".equals(name)||"magic_animation".equals(name)||"range_animation".equals(name))values.put(name,readInteger(in));
                    else in.skipValue();
                }
                in.endObject();
                if(id!=null&&id==npcId)for(Map.Entry<String,Integer> value:values.entrySet())if(value.getValue()!=null&&value.getValue()>0)
                    roles.computeIfAbsent(value.getValue(),ignored->new ArrayList<>()).add(roleLabel(value.getKey()));
            }
            in.endArray();
        }
        List<Entry> out=new ArrayList<>();for(Map.Entry<Integer,List<String>> value:roles.entrySet())out.add(new Entry(value.getKey(),value.getValue(),file));return List.copyOf(out);
    }

    private static Integer readInteger(JsonReader in)throws IOException{
        switch(in.peek()){
            case NULL:in.nextNull();return null;
            case NUMBER:return in.nextInt();
            case STRING:String value=in.nextString();if(value.isBlank())return null;try{return Integer.valueOf(value);}catch(NumberFormatException ignored){return null;}
            default:in.skipValue();return null;
        }
    }
    private static String roleLabel(String key){return key.substring(0,key.indexOf('_'));}

    static final class Entry{
        final int sequenceId;final List<String> roles;final Path source;
        Entry(int sequenceId,List<String> roles,Path source){this.sequenceId=sequenceId;this.roles=List.copyOf(roles);this.source=source;}
        String provenance(){return"NPC combat metadata ("+String.join("/",roles)+")";}
    }
}
