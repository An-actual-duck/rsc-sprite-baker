package com.spoiledmilk.spritebaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Deterministic, asset-free combat-role subset derived during licensed packaging. */
public final class CombatRoleManifest {
    public static final int SCHEMA_VERSION=1;
    public static final String RELATIVE_PATH="metadata/combat-roles-v1.json";
    public int schemaVersion=SCHEMA_VERSION;
    public Provenance provenance=new Provenance();
    public String entriesSha256;
    public List<Entry> entries=new ArrayList<>();

    public static CombatRoleManifest derive(Path source,String sourceRevision)throws IOException{
        CombatRoleManifest out=new CombatRoleManifest();out.provenance.sourceRevision=sourceRevision;out.provenance.sourceSha256=Hashing.sha256(source);
        Map<Integer,Entry> records=new TreeMap<>();
        try(JsonReader in=new JsonReader(Files.newBufferedReader(source))){in.beginArray();while(in.hasNext()){
            Integer npc=null,melee=null,magic=null,range=null;in.beginObject();while(in.hasNext()){String key=in.nextName();if("id".equals(key))npc=integer(in);else if("melee_animation".equals(key))melee=positive(integer(in));else if("magic_animation".equals(key))magic=positive(integer(in));else if("range_animation".equals(key))range=positive(integer(in));else in.skipValue();}in.endObject();
            if(npc!=null&&npc>=0&&(melee!=null||magic!=null||range!=null)){Entry entry=new Entry();entry.npcId=npc;entry.meleeSequenceId=melee;entry.magicSequenceId=magic;entry.rangeSequenceId=range;records.put(npc,entry);}
        }in.endArray();}
        out.entries.addAll(records.values());out.entriesSha256=Hashing.sha256(canonicalEntries(out.entries));return out;
    }

    public static CombatRoleManifest load(Path path)throws IOException{
        try(Reader reader=Files.newBufferedReader(path)){
            JsonElement parsed=JsonParser.parseReader(reader);if(!parsed.isJsonObject())throw new IOException("combat-role manifest root must be an object");JsonObject root=parsed.getAsJsonObject();
            if(!root.has("schemaVersion")||root.get("schemaVersion").getAsInt()!=SCHEMA_VERSION)throw new IOException("unsupported combat-role manifest schemaVersion");
            CombatRoleManifest manifest=gson().fromJson(root,CombatRoleManifest.class);validate(manifest);return manifest;
        }catch(com.google.gson.JsonParseException|IllegalStateException|NumberFormatException e){throw new IOException("invalid combat-role manifest JSON: "+e.getMessage(),e);}
    }

    public void write(Path path)throws IOException{validate(this);Path parent=path.toAbsolutePath().getParent();if(parent!=null)Files.createDirectories(parent);try(Writer writer=Files.newBufferedWriter(path)){gson().toJson(this,writer);writer.write(System.lineSeparator());}}
    public List<Entry> forNpc(int npcId){int low=0,high=entries.size()-1;while(low<=high){int middle=(low+high)>>>1;Entry entry=entries.get(middle);if(entry.npcId<npcId)low=middle+1;else if(entry.npcId>npcId)high=middle-1;else return List.of(entry);}return List.of();}

    static void validate(CombatRoleManifest manifest)throws IOException{
        if(manifest==null||manifest.schemaVersion!=SCHEMA_VERSION)throw new IOException("unsupported combat-role manifest schemaVersion");
        if(manifest.provenance==null||!"2009scape".equals(manifest.provenance.sourceProject)||!"Server/data/configs/npc_configs.json".equals(manifest.provenance.sourcePath)||!"AGPL-3.0".equals(manifest.provenance.license)||!sha(manifest.provenance.sourceSha256)||manifest.provenance.sourceRevision==null||!manifest.provenance.sourceRevision.matches("[0-9a-f]{40,64}"))throw new IOException("combat-role manifest provenance is incomplete");
        if(manifest.entries==null||manifest.entries.isEmpty())throw new IOException("combat-role manifest entries must not be empty");int previous=-1;
        for(Entry entry:manifest.entries){if(entry==null||entry.npcId<=previous||entry.npcId<0)throw new IOException("combat-role manifest NPC IDs must be strictly increasing");previous=entry.npcId;if(!positiveOrNull(entry.meleeSequenceId)||!positiveOrNull(entry.magicSequenceId)||!positiveOrNull(entry.rangeSequenceId)||(entry.meleeSequenceId==null&&entry.magicSequenceId==null&&entry.rangeSequenceId==null))throw new IOException("combat-role manifest sequence IDs must be positive");}
        String actual=Hashing.sha256(canonicalEntries(manifest.entries));if(!sha(manifest.entriesSha256)||!actual.equalsIgnoreCase(manifest.entriesSha256))throw new IOException("combat-role manifest entriesSha256 mismatch");
    }

    static String canonicalEntries(List<Entry> entries){StringBuilder out=new StringBuilder();for(Entry entry:entries)out.append(entry.npcId).append('\t').append(value(entry.meleeSequenceId)).append('\t').append(value(entry.magicSequenceId)).append('\t').append(value(entry.rangeSequenceId)).append('\n');return out.toString();}
    private static String value(Integer value){return value==null?"-":value.toString();}
    private static boolean positiveOrNull(Integer value){return value==null||value>0;}
    private static boolean sha(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    private static Integer positive(Integer value){return value!=null&&value>0?value:null;}
    private static Integer integer(JsonReader in)throws IOException{switch(in.peek()){case NULL:in.nextNull();return null;case NUMBER:return in.nextInt();case STRING:String value=in.nextString();if(value.isBlank())return null;try{return Integer.valueOf(value);}catch(NumberFormatException ignored){return null;}default:in.skipValue();return null;}}
    private static Gson gson(){return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();}

    public static final class Provenance{public String sourceProject="2009scape";public String sourcePath="Server/data/configs/npc_configs.json";public String sourceRevision;public String sourceSha256;public String license="AGPL-3.0";}
    public static final class Entry{public int npcId;public Integer meleeSequenceId,magicSequenceId,rangeSequenceId;}
}
