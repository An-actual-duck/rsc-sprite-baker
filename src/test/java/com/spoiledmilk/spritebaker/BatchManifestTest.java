package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BatchManifestTest {
    @Test void loadsVersionedNeutralManifestAndRejectsUnknownVersion(@TempDir Path directory)throws Exception{
        Path valid=directory.resolve("batch.json");Files.writeString(valid,"{\"schemaVersion\":1,\"entries\":[{\"id\":\"neutral\",\"project\":\"project.json\",\"outputName\":\"neutral\",\"mapping\":{\"assetKind\":\"npc\",\"gameId\":7,\"variant\":\"default\"}}]}");
        BatchManifest manifest=BatchManifest.load(valid);assertEquals(1,manifest.entries.size());assertEquals(7,manifest.entries.get(0).mapping.gameId);
        Path future=directory.resolve("future.json");Files.writeString(future,"{\"schemaVersion\":99,\"entries\":[{}]}");assertThrows(java.io.IOException.class,()->BatchManifest.load(future));
    }
    @Test void collisionsAreCaseInsensitive(){BatchManifest.Entry first=entry("One","Sprite"),second=entry("Two","sprite"),third=entry("Three","other");assertEquals(java.util.Set.of("sprite"),BatchProcessor.collisionNames(List.of(first,second,third)));}
    private static BatchManifest.Entry entry(String id,String output){BatchManifest.Entry value=new BatchManifest.Entry();value.id=id;value.outputName=output;return value;}
}
