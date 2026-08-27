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
        return loadWithDiagnostics(cacheDirectory,npcId).entries;
    }

    static LoadResult loadWithDiagnostics(Path cacheDirectory,int npcId)throws IOException{
        List<String> diagnostics=new ArrayList<>();Path packaged=packagedFile(cacheDirectory);
        if(Files.isRegularFile(packaged))try{return new LoadResult(loadManifest(packaged,npcId),List.of(),"packaged combat-role manifest");}catch(IOException e){diagnostics.add("packaged combat-role manifest "+packaged+": "+e.getMessage());}
        Path adjacent=adjacentFile(cacheDirectory);if(adjacent!=null)return new LoadResult(loadFile(adjacent,npcId),List.copyOf(diagnostics),"adjacent NPC combat metadata");
        return new LoadResult(List.of(),List.copyOf(diagnostics),packaged==null?"no combat-role metadata":"packaged combat-role manifest unavailable");
    }

    static Path packagedFile(Path cacheDirectory){Path parent=cacheDirectory.toAbsolutePath().normalize().getParent();return parent==null?cacheDirectory.resolve(CombatRoleManifest.RELATIVE_PATH):parent.resolve(CombatRoleManifest.RELATIVE_PATH);}

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
        List<Entry> out=new ArrayList<>();for(Map.Entry<Integer,List<String>> value:roles.entrySet())out.add(new Entry(value.getKey(),value.getValue(),file,"adjacent NPC combat metadata"));return List.copyOf(out);
    }

    private static List<Entry> loadManifest(Path file,int npcId)throws IOException{
        List<Entry> out=new ArrayList<>();for(CombatRoleManifest.Entry record:CombatRoleManifest.load(file).forNpc(npcId)){Map<Integer,List<String>> roles=new LinkedHashMap<>();add(roles,record.meleeSequenceId,"melee");add(roles,record.magicSequenceId,"magic");add(roles,record.rangeSequenceId,"range");for(Map.Entry<Integer,List<String>> value:roles.entrySet())out.add(new Entry(value.getKey(),value.getValue(),file,"packaged combat-role manifest"));}return List.copyOf(out);
    }
    private static void add(Map<Integer,List<String>> roles,Integer sequence,String role){if(sequence!=null)roles.computeIfAbsent(sequence,ignored->new ArrayList<>()).add(role);}

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
        final int sequenceId;final List<String> roles;final Path source;final String sourceLabel;
        Entry(int sequenceId,List<String> roles,Path source,String sourceLabel){this.sequenceId=sequenceId;this.roles=List.copyOf(roles);this.source=source;this.sourceLabel=sourceLabel;}
        String provenance(){return sourceLabel+" ("+String.join("/",roles)+")";}
    }
    static final class LoadResult{final List<Entry> entries;final List<String> diagnostics;final String provenance;LoadResult(List<Entry> entries,List<String> diagnostics,String provenance){this.entries=entries;this.diagnostics=diagnostics;this.provenance=provenance;}}
}
