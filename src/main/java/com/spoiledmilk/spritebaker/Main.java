package com.spoiledmilk.spritebaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.fs.Index;

public final class Main {
    private static final String RUNELITE_VERSION = "1.12.35";

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        Path cacheDirectory = arguments.cacheDirectory.toRealPath();
        Path outputDirectory = arguments.outputDirectory.toAbsolutePath().normalize();
        enforceOutputBoundary(outputDirectory, cacheDirectory, Paths.get("").toRealPath());
        Files.createDirectories(outputDirectory);

        Path png = outputDirectory.resolve("npc-" + arguments.npcId + "-static.png");
        Path manifest = outputDirectory.resolve("npc-" + arguments.npcId + "-diagnostic.json");
        try (CacheReader cache = new CacheReader(cacheDirectory)) {
            NpcDefinition530 npc = cache.loadNpc(arguments.npcId);
            RenderAnimation530 renderAnimation = npc.renderAnimation == -1
                ? null : cache.loadRenderAnimation(npc.renderAnimation);
            List<ModelDefinition> models = new ArrayList<>();
            List<Map<String, Object>> modelDiagnostics = new ArrayList<>();
            for (int modelId : npc.modelIds) {
                byte[] rawModel = cache.loadFile(CacheReader.MODEL_INDEX, modelId, 0);
                ModelDefinition model = cache.loadModel(modelId);
                models.add(model);
                modelDiagnostics.add(modelDiagnostic(model, rawModel));
            }

            BufferedImage image = new StaticRenderer().render(models, npc);
            if (!ImageIO.write(image, "PNG", png.toFile())) {
                throw new IOException("no PNG writer is installed");
            }
            Map<String, Object> diagnostic = manifest(
                cache, cacheDirectory, npc, renderAnimation, modelDiagnostics, png);
            Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            try (Writer writer = Files.newBufferedWriter(manifest)) {
                gson.toJson(diagnostic, writer);
                writer.write(System.lineSeparator());
            }
        }
        System.out.println("Rendered " + png);
        System.out.println("Wrote " + manifest);
    }

    static void enforceOutputBoundary(Path output, Path cache, Path workingDirectory) {
        if (output.startsWith(cache)) {
            throw new IllegalArgumentException("output directory must not be inside the read-only cache");
        }
        if (Files.exists(workingDirectory.resolve("pom.xml"))
            && Files.isDirectory(workingDirectory.resolve("src"))
            && output.startsWith(workingDirectory)) {
            throw new IllegalArgumentException("output directory must be outside the repository checkout");
        }
    }

    private static Map<String, Object> modelDiagnostic(ModelDefinition model, byte[] raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", model.id);
        result.put("location", orderedMap("index", CacheReader.MODEL_INDEX, "archive", model.id, "file", 0));
        result.put("format", detectModelFormat(raw));
        result.put("encodedBytes", raw.length);
        result.put("vertices", model.vertexCount);
        result.put("faces", model.faceCount);
        result.put("textureFaces", countTexturedFaces(model));
        result.put("modelTextureTriangles", model.numTextureFaces);
        return result;
    }

    private static String detectModelFormat(byte[] data) {
        int last = data[data.length - 1];
        int beforeLast = data[data.length - 2];
        if (beforeLast == -1 && last == -3) return "runelite-type-3";
        if (beforeLast == -1 && last == -2) return "runelite-type-2";
        if (beforeLast == -1 && last == -1) return "revision-530-type-1";
        return "runelite-old-format";
    }

    private static long countTexturedFaces(ModelDefinition model) {
        if (model.faceTextures == null) return 0;
        return java.util.stream.IntStream.range(0, model.faceCount)
            .filter(index -> model.faceTextures[index] != -1)
            .count();
    }

    private static Map<String, Object> manifest(CacheReader cache, Path cacheDirectory,
                                                 NpcDefinition530 npc,
                                                 RenderAnimation530 renderAnimation,
                                                 List<Map<String, Object>> models,
                                                 Path png) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", 1);
        root.put("tool", orderedMap(
            "name", "RSC Sprite Baker compatibility spike",
            "version", "0.1.0-SNAPSHOT",
            "javaRuntime", System.getProperty("java.version")));
        root.put("decoder", orderedMap(
            "archiveAndModel", "net.runelite:cache:" + RUNELITE_VERSION,
            "license", "BSD-2-Clause",
            "revisionAdapter", "strict local NPC/BAS metadata adapter for revision 530"));

        List<Map<String, Object>> indexes = new ArrayList<>();
        for (Index index : cache.store().getIndexes()) {
            indexes.add(orderedMap(
                "id", index.getId(),
                "protocol", index.getProtocol(),
                "revision", index.getRevision(),
                "archives", index.getArchives().size()));
        }
        Path dataFile = cacheDirectory.resolve("main_file_cache.dat2");
        Path referenceIndex = cacheDirectory.resolve("main_file_cache.idx255");
        Map<String, Object> cacheIdentity = new LinkedHashMap<>();
        cacheIdentity.put("directory", cacheDirectory.toString());
        cacheIdentity.put("layout", "JS5 dat2 with idx0..idx28 and idx255");
        cacheIdentity.put("indexCount", cache.store().getIndexes().size());
        cacheIdentity.put("dataFile", fileIdentity(dataFile));
        cacheIdentity.put("referenceIndex", fileIdentity(referenceIndex));
        cacheIdentity.put("indexes", indexes);
        root.put("cache", cacheIdentity);

        Map<String, Object> npcSource = new LinkedHashMap<>();
        npcSource.put("id", npc.id);
        npcSource.put("name", npc.name);
        npcSource.put("location", orderedMap(
            "index", CacheReader.NPC_INDEX, "archive", npc.id >>> 7, "file", npc.id & 0x7f));
        npcSource.put("componentModelIds", npc.modelIds);
        npcSource.put("models", models);
        root.put("npc", npcSource);
        root.put("appearance", orderedMap(
            "recolors", pairs(npc.recolorFrom, npc.recolorTo),
            "retextures", pairs(npc.retextureFrom, npc.retextureTo),
            "recolorPaletteIndices", npc.recolorPaletteIndices,
            "widthScale", npc.widthScale,
            "heightScale", npc.heightScale));

        Map<String, Object> animations = new LinkedHashMap<>();
        animations.put("renderAnimationId", npc.renderAnimation);
        animations.put("standingAnimationId", renderAnimation == null ? npc.standingAnimation : renderAnimation.standingAnimation);
        animations.put("walkingAnimationId", renderAnimation == null ? npc.walkingAnimation : renderAnimation.walkingAnimation);
        animations.put("runningAnimationId", renderAnimation == null ? -1 : renderAnimation.runningAnimation);
        animations.put("appliedPose", "none; decoded base/static model pose (animation decode is Phase 2)");
        root.put("animations", animations);

        root.put("camera", orderedMap(
            "projection", "orthographic-auto-fit",
            "yawDegrees", StaticRenderer.YAW_DEGREES,
            "pitchDegrees", StaticRenderer.PITCH_DEGREES,
            "paddingPixels", StaticRenderer.PADDING,
            "groundAnchor", "bottom-center"));
        root.put("lighting", orderedMap(
            "model", "two-sided Lambertian",
            "direction", StaticRenderer.LIGHT_DIRECTION,
            "ambient", StaticRenderer.AMBIENT_LIGHT,
            "diffuse", StaticRenderer.DIFFUSE_LIGHT,
            "npcAmbientAdjustment", npc.ambient,
            "npcContrastAdjustment", npc.contrast));
        root.put("render", orderedMap(
            "renderer", "deterministic Java software triangle rasterizer",
            "width", StaticRenderer.WIDTH,
            "height", StaticRenderer.HEIGHT,
            "background", "transparent",
            "texturing", "unsupported/rejected",
            "png", png.getFileName().toString(),
            "pngSha256", Hashing.sha256(png)));
        root.put("compatibility", orderedMap(
            "status", "compatible for selected untextured old-format model",
            "runeliteStore", "success",
            "runeliteModelLoader", "success for all selected component models",
            "runeliteNpcLoader", "not revision-530-safe; legacy opcodes 42 and 127 require the strict adapter",
            "modelDecoder", "exactly framed revision-530 type-1 models use the bounded pinned decoder; other formats retain the RuneLite dependency path",
            "unsupported", List.of("textures", "animation transforms", "NPC morph resolution", "recolor palette opcode 42 application")));
        return root;
    }

    private static Map<String, Object> fileIdentity(Path path) throws IOException {
        return orderedMap(
            "name", path.getFileName().toString(),
            "bytes", Files.size(path),
            "sha256", Hashing.sha256(path));
    }

    private static List<Map<String, Integer>> pairs(short[] from, short[] to) {
        List<Map<String, Integer>> pairs = new ArrayList<>();
        for (int i = 0; i < from.length; i++) {
            Map<String, Integer> pair = new LinkedHashMap<>();
            pair.put("from", Short.toUnsignedInt(from[i]));
            pair.put("to", Short.toUnsignedInt(to[i]));
            pairs.add(pair);
        }
        return pairs;
    }

    private static Map<String, Object> orderedMap(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("orderedMap requires key/value pairs");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            result.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return result;
    }

    static final class Arguments {
        final Path cacheDirectory;
        final Path outputDirectory;
        final int npcId;

        private Arguments(Path cacheDirectory, Path outputDirectory, int npcId) {
            this.cacheDirectory = cacheDirectory;
            this.outputDirectory = outputDirectory;
            this.npcId = npcId;
        }

        static Arguments parse(String[] args) {
            Path cache = null;
            Path output = null;
            int npc = 72;
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 >= args.length) usage();
                switch (args[i]) {
                    case "--cache": cache = Paths.get(args[i + 1]); break;
                    case "--output-dir": output = Paths.get(args[i + 1]); break;
                    case "--npc": npc = Integer.parseInt(args[i + 1]); break;
                    default: usage();
                }
            }
            if (cache == null || output == null || npc < 0) usage();
            return new Arguments(cache, output, npc);
        }

        private static void usage() {
            throw new IllegalArgumentException(
                "usage: --cache PATH --output-dir PATH [--npc ID] (default NPC: 72)");
        }
    }
}
