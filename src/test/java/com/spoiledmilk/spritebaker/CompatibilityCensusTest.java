package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.GsonBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompatibilityCensusTest {
    @Test void aggregatesStableCategoriesBlockersAndModelClusters() {
        List<NpcCompatibility> definitions = List.of(
            result(2, NpcCompatibility.Category.UNSUPPORTED_MODEL, "model 99: BufferUnderflowException"),
            result(0, NpcCompatibility.Category.READY, "validated"),
            result(1, NpcCompatibility.Category.UNSUPPORTED_MATERIAL, "texture 7 unsupported: procedural operation 36"),
            result(3, NpcCompatibility.Category.UNSUPPORTED_MODEL, "model 100: BufferUnderflowException"));
        LinkedHashMap<String,Object> identity = new LinkedHashMap<>();
        identity.put("dataSha256", "abc");
        CompatibilityCensus census = new CompatibilityCensus(identity, definitions);
        assertEquals(4, census.definitionCount);
        assertEquals(1, census.categories.get("ready"));
        assertEquals(1, census.unsupportedOperationBlockers.get("36"));
        assertEquals(2, census.modelFailureClusters.get("BufferUnderflowException"));
        String first = new GsonBuilder().setPrettyPrinting().create().toJson(census);
        String second = new GsonBuilder().setPrettyPrinting().create().toJson(new CompatibilityCensus(identity, definitions));
        assertEquals(first, second);
    }

    private static NpcCompatibility result(int id, NpcCompatibility.Category category, String reason) {
        return new NpcCompatibility(id, "npc-" + id, category, reason, List.of(id), List.of(), -1, -1);
    }
}
