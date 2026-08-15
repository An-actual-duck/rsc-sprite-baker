package com.spoiledmilk.spritebaker;

import java.util.List;

/** Deterministic, user-facing compatibility result for one cache NPC definition. */
public final class NpcCompatibility {
    public enum Category {
        READY("ready", "Ready"),
        MISSING_AUTOMATIC_ANIMATIONS("missing-automatic-animations", "Missing automatic animations"),
        UNSUPPORTED_MATERIAL("unsupported-material", "Unsupported material"),
        UNSUPPORTED_MODEL("unsupported-model", "Unsupported model"),
        MORPH_INTERNAL_DEFINITION("morph-internal-definition", "Morph/internal definition"),
        OTHER_FAILURE("other-failure", "Other failure");

        public final String id;
        public final String display;

        Category(String id, String display) {
            this.id = id;
            this.display = display;
        }
    }

    public final int npcId;
    public final String name;
    public final Category category;
    public final String categoryId;
    public final String reason;
    public final List<Integer> modelIds;
    public final List<Integer> materialIds;
    public final int standingSequenceId;
    public final int walkingSequenceId;

    NpcCompatibility(int npcId, String name, Category category, String reason,
                     List<Integer> modelIds, List<Integer> materialIds,
                     int standingSequenceId, int walkingSequenceId) {
        this.npcId = npcId;
        this.name = name;
        this.category = category;
        this.categoryId = category.id;
        this.reason = reason;
        this.modelIds = List.copyOf(modelIds);
        this.materialIds = List.copyOf(materialIds);
        this.standingSequenceId = standingSequenceId;
        this.walkingSequenceId = walkingSequenceId;
    }

    public String summary() {
        return category.display + ": " + reason;
    }
}
