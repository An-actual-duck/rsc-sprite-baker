package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.cache.definitions.ModelDefinition;

/** Shared-cache, deterministic compatibility probe used by census and lazy desktop diagnosis. */
public final class NpcCompatibilityScanner {
    private final CacheReader cache;
    private final TextureProvider530 textures;
    private final Map<Integer,ModelDefinition> models = new LinkedHashMap<>();
    private final Map<Integer,Sequence530> sequences = new LinkedHashMap<>();
    private final Map<Integer,Frame530> frames = new LinkedHashMap<>();
    private final ModelAnimator animator = new ModelAnimator();

    public NpcCompatibilityScanner(CacheReader cache) throws IOException {
        this.cache = cache;
        textures = new TextureProvider530(cache);
    }

    public NpcCompatibility assess(int npcId) {
        NpcDefinition530 npc;
        try {
            npc = cache.loadNpc(npcId);
        } catch (Exception e) {
            return result(npcId, "<definition " + npcId + ">", NpcCompatibility.Category.OTHER_FAILURE,
                "definition decode: " + failure(e), List.of(), List.of(), -1, -1);
        }
        List<Integer> modelIds = Arrays.stream(npc.modelIds).boxed().collect(java.util.stream.Collectors.toList());
        if (npc.morphDefinition) {
            return result(npc, NpcCompatibility.Category.MORPH_INTERNAL_DEFINITION,
                "definition selects another NPC through morph metadata", modelIds, List.of(), -1, -1);
        }
        if (npc.modelIds.length == 0) {
            return result(npc, NpcCompatibility.Category.MORPH_INTERNAL_DEFINITION,
                "definition has no component models", modelIds, List.of(), -1, -1);
        }

        List<ModelDefinition> parts = new ArrayList<>();
        for (int modelId : npc.modelIds) {
            try {
                parts.add(model(modelId));
            } catch (Exception e) {
                return result(npc, NpcCompatibility.Category.UNSUPPORTED_MODEL,
                    "model " + modelId + ": " + failure(e), modelIds, List.of(), -1, -1);
            }
        }

        ModelDefinition combined;
        try {
            combined = ModelAssembler.combine(parts);
        } catch (Exception e) {
            return result(npc, NpcCompatibility.Category.UNSUPPORTED_MODEL,
                "model assembly: " + failure(e), modelIds, List.of(), -1, -1);
        }
        TextureDiagnostics530.Report materials = TextureDiagnostics530.analyze(combined, npc, textures);
        if (!materials.supported()) {
            return result(npc, NpcCompatibility.Category.UNSUPPORTED_MATERIAL,
                String.join("; ", materials.errors), modelIds, materials.materialIds, -1, -1);
        }

        RenderAnimation530 bas = null;
        if (npc.renderAnimation >= 0) {
            try {
                bas = cache.loadRenderAnimation(npc.renderAnimation);
            } catch (Exception e) {
                return result(npc, NpcCompatibility.Category.OTHER_FAILURE,
                    "render-animation " + npc.renderAnimation + ": " + failure(e), modelIds, materials.materialIds, -1, -1);
            }
        }
        int[] automatic = AnimationDiscovery.knownSequences(npc, bas);
        if (automatic[0] < 0 || automatic[1] < 0) {
            String missing = automatic[0] < 0 && automatic[1] < 0 ? "standing and walking"
                : automatic[0] < 0 ? "standing" : "walking";
            return result(npc, NpcCompatibility.Category.MISSING_AUTOMATIC_ANIMATIONS,
                missing + " sequence metadata is absent", modelIds, materials.materialIds, automatic[0], automatic[1]);
        }
        for (int sequenceId : automatic) {
            try {
                Sequence530 sequence = sequence(sequenceId);
                if (sequence.frameIds.length == 0) throw new IllegalArgumentException("sequence has no frames");
                Frame530 frame = frame(sequence.frameIds[0]);
                animator.pose(combined, frame, null, 0, Math.max(1, sequence.durations[0]));
            } catch (Exception e) {
                return result(npc, NpcCompatibility.Category.OTHER_FAILURE,
                    "automatic sequence " + sequenceId + ": " + failure(e), modelIds, materials.materialIds, automatic[0], automatic[1]);
            }
        }
        return result(npc, NpcCompatibility.Category.READY,
            "models, materials, standing " + automatic[0] + ", and walking " + automatic[1] + " validated",
            modelIds, materials.materialIds, automatic[0], automatic[1]);
    }

    private ModelDefinition model(int id) throws IOException {
        ModelDefinition hit = models.get(id);
        if (hit == null) { hit = cache.loadModel(id); models.put(id, hit); }
        return hit;
    }

    private Sequence530 sequence(int id) throws IOException {
        Sequence530 hit = sequences.get(id);
        if (hit == null) { hit = cache.loadSequence(id); sequences.put(id, hit); }
        return hit;
    }

    private Frame530 frame(int id) throws IOException {
        Frame530 hit = frames.get(id);
        if (hit == null) { hit = cache.loadFrame(id); frames.put(id, hit); }
        return hit;
    }

    private static NpcCompatibility result(NpcDefinition530 npc, NpcCompatibility.Category category,
                                            String reason, List<Integer> models, List<Integer> materials,
                                            int standing, int walking) {
        return result(npc.id, npc.name, category, reason, models, materials, standing, walking);
    }

    private static NpcCompatibility result(int id, String name, NpcCompatibility.Category category,
                                            String reason, List<Integer> models, List<Integer> materials,
                                            int standing, int walking) {
        return new NpcCompatibility(id, name, category, reason, models, materials, standing, walking);
    }

    static String failure(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null) root = root.getCause();
        String message = root.getMessage();
        return root.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }
}
