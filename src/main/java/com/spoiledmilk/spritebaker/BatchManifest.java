package com.spoiledmilk.spritebaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Versioned, asset-free input contract for reviewed project batches. */
public final class BatchManifest {
    public static final int SCHEMA_VERSION=1;
    public int schemaVersion=SCHEMA_VERSION;
    public CacheExpectation cache=new CacheExpectation();
    public List<Entry> entries=new ArrayList<>();

    public static BatchManifest load(Path path)throws IOException{
        try(Reader reader=Files.newBufferedReader(path)){
            JsonElement parsed=JsonParser.parseReader(reader);
            if(!parsed.isJsonObject())throw new IOException("batch manifest root must be an object");
            JsonObject root=parsed.getAsJsonObject();
            if(!root.has("schemaVersion")||!root.get("schemaVersion").isJsonPrimitive())throw new IOException("batch manifest schemaVersion is required");
            int version=root.get("schemaVersion").getAsInt();
            if(version!=SCHEMA_VERSION)throw new IOException("unsupported batch manifest schemaVersion "+version+" (expected "+SCHEMA_VERSION+")");
            BatchManifest manifest=gson().fromJson(root,BatchManifest.class);
            if(manifest.entries==null||manifest.entries.isEmpty())throw new IOException("batch manifest entries must not be empty");
            if(manifest.cache==null)manifest.cache=new CacheExpectation();
            return manifest;
        }catch(com.google.gson.JsonParseException|IllegalStateException e){throw new IOException("invalid batch manifest JSON: "+e.getMessage(),e);}
    }

    static Gson gson(){return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();}

    public static final class CacheExpectation {
        public String dataFileSha256;
        public String referenceIndexSha256;
    }
    public static final class Entry {
        public String id;
        public String project;
        public String outputName;
        public Mapping mapping=new Mapping();
        public Expected expected=new Expected();
    }
    public static final class Mapping {
        public String assetKind="npc";
        public int gameId=-1;
        public String variant="default";
    }
    public static final class Expected {
        public String projectSha256;
        public String pngSha256;
        public String provenanceSha256;
        public Integer sheetWidth;
        public Integer sheetHeight;
    }
}
