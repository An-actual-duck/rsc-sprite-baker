package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CombatRoleManifestTest {
    @TempDir Path temporary;
    private static final String REVISION="0123456789abcdef0123456789abcdef01234567";

    @Test void derivationIsMinimalSortedAndByteDeterministic()throws Exception{
        Path source=temporary.resolve("npc_configs.json");Files.writeString(source,"[{\"id\":\"9\",\"name\":\"Ignored\",\"melee_animation\":\"0\",\"magic_animation\":\"41\"},{\"id\":\"7\",\"range_animation\":\"52\",\"melee_animation\":\"40\"},{\"id\":8,\"death_animation\":99}]");
        Path first=temporary.resolve("first.json"),second=temporary.resolve("second.json");CombatRoleManifest.derive(source,REVISION).write(first);CombatRoleManifest.derive(source,REVISION).write(second);
        assertArrayEquals(Files.readAllBytes(first),Files.readAllBytes(second));CombatRoleManifest loaded=CombatRoleManifest.load(first);assertEquals(2,loaded.entries.size());assertEquals(7,loaded.entries.get(0).npcId);assertEquals(40,loaded.entries.get(0).meleeSequenceId);assertEquals(52,loaded.entries.get(0).rangeSequenceId);assertNull(loaded.entries.get(0).magicSequenceId);assertEquals(Hashing.sha256(source),loaded.provenance.sourceSha256);assertFalse(Files.readString(first).contains("Ignored"));assertFalse(Files.readString(first).contains("death_animation"));
    }

    @Test void malformedSchemaOrderingAndPayloadHashFailClosed()throws Exception{
        Path source=temporary.resolve("npc_configs.json");Files.writeString(source,"[{\"id\":7,\"melee_animation\":40},{\"id\":9,\"magic_animation\":41}]");Path manifest=temporary.resolve("manifest.json");CombatRoleManifest.derive(source,REVISION).write(manifest);
        String valid=Files.readString(manifest);Files.writeString(manifest,valid.replace("\"schemaVersion\": 1","\"schemaVersion\": 2"));assertThrows(java.io.IOException.class,()->CombatRoleManifest.load(manifest));
        Files.writeString(manifest,valid.replace("\"entriesSha256\": \"","\"entriesSha256\": \"0"));assertThrows(java.io.IOException.class,()->CombatRoleManifest.load(manifest));
        Files.writeString(manifest,valid.replace("\"npcId\": 7","\"npcId\": 10"));assertThrows(java.io.IOException.class,()->CombatRoleManifest.load(manifest));
    }
}
