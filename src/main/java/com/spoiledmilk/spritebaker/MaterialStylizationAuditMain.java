package com.spoiledmilk.spritebaker;

import com.google.gson.GsonBuilder;
import java.awt.image.BufferedImage;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/** Terminal-only original-versus-RSC-material audit over the visual review set. */
public final class MaterialStylizationAuditMain {
    private static final int[] NPCS = {1615, 2783, 50, 3808, 6222, 3484, 1637, 5247, 72};
    private MaterialStylizationAuditMain() { }

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        Path repository = Path.of("").toRealPath(), cache = arguments.cache.toRealPath();
        Path output = arguments.output.toAbsolutePath().normalize();
        Path exports = arguments.exports.toAbsolutePath().normalize();
        Main.enforceOutputBoundary(output.getParent(), cache, repository);
        Main.enforceOutputBoundary(exports, cache, repository);
        Files.createDirectories(output.getParent());
        Files.createDirectories(exports);
        List<Map<String,Object>> results = new ArrayList<>();
        for (int npcId : NPCS) {
            System.err.println("Auditing material style for NPC " + npcId);
            results.add(audit(cache, exports, npcId));
        }
        Map<String,Object> report = ordered(
            "schemaVersion", 1,
            "purpose", "original-versus-RSC-material terminal visual metrics",
            "cache", ordered("directory", cache.toString(),
                "dataSha256", Hashing.sha256(cache.resolve("main_file_cache.dat2")),
                "referenceIndexSha256", Hashing.sha256(cache.resolve("main_file_cache.idx255"))),
            "npcIds", NPCS,
            "results", results);
        try (Writer writer = Files.newBufferedWriter(output)) {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(report, writer);
            writer.write(System.lineSeparator());
        }
        System.out.println("Audited " + results.size() + " NPCs");
        System.out.println("Wrote " + output);
        System.out.println("Export evidence: " + exports);
    }

    private static Map<String,Object> audit(Path cache, Path exports, int npcId) throws Exception {
        try (AnimationWorkspace workspace = new AnimationWorkspace(cache, npcId)) {
            SpriteProject original = new SpriteProject();
            original.npcId = npcId;
            AutomaticSheetBuilder.populate(original, workspace);
            SpriteProject styled = original.copy();
            styled.visual.applyPreset("RSC material");
            Path originalDirectory = exports.resolve(String.format("npc-%05d/original", npcId));
            Path styledDirectory = exports.resolve(String.format("npc-%05d/rsc-material", npcId));
            new SheetExporter().export(workspace, original, originalDirectory);
            new SheetExporter().export(workspace, styled, styledDirectory);
            BufferedImage originalImage = ImageIO.read(originalDirectory.resolve("npc-" + npcId + "-rsc-sheet.png").toFile());
            BufferedImage styledImage = ImageIO.read(styledDirectory.resolve("npc-" + npcId + "-rsc-sheet.png").toFile());
            SpriteQualityMetrics.Metrics before = SpriteQualityMetrics.measure(originalImage);
            SpriteQualityMetrics.Metrics after = SpriteQualityMetrics.measure(styledImage);
            int alphaMismatches = SpriteQualityMetrics.alphaMismatches(originalImage, styledImage);
            return ordered("npcId", npcId, "name", workspace.npc.name,
                "componentModelIds", workspace.npc.modelIds,
                "alphaMismatchPixels", alphaMismatches,
                "original", metrics(before, Hashing.sha256(originalDirectory.resolve("npc-" + npcId + "-rsc-sheet.png")), originalImage),
                "rscMaterial", metrics(after, Hashing.sha256(styledDirectory.resolve("npc-" + npcId + "-rsc-sheet.png")), styledImage),
                "isolatedDarkReduction", before.isolatedDarkPixels - after.isolatedDarkPixels,
                "transitionReduction", before.interiorTransitions - after.interiorTransitions);
        }
    }

    private static Map<String,Object> metrics(SpriteQualityMetrics.Metrics metrics, String sha256,
                                              BufferedImage sheet) {
        return ordered("pngSha256", sha256, "visiblePixels", metrics.visiblePixels,
            "blackPixels", metrics.blackPixels, "isolatedDarkPixels", metrics.isolatedDarkPixels,
            "interiorTransitions", metrics.interiorTransitions, "distinctRgb", metrics.distinctRgb,
            "distinctLuminanceLevels", metrics.distinctLuminanceLevels,
            "temporalFrameMetrics", temporalFrameMetrics(sheet));
    }

    private static Map<String,Object> temporalFrameMetrics(BufferedImage sheet) {
        int minSpeckles = Integer.MAX_VALUE, maxSpeckles = 0, maxColors = 0;
        for (int row = 0; row < 3; row++) for (int column = 0; column < 6; column++) {
            BufferedImage cell = sheet.getSubimage(column * 128, row * 128, 128, 128);
            SpriteQualityMetrics.Metrics metrics = SpriteQualityMetrics.measure(cell);
            minSpeckles = Math.min(minSpeckles, metrics.isolatedDarkPixels);
            maxSpeckles = Math.max(maxSpeckles, metrics.isolatedDarkPixels);
            maxColors = Math.max(maxColors, metrics.distinctRgb);
        }
        return ordered("sampledFrames", 18, "minimumIsolatedDarkPixels", minSpeckles,
            "maximumIsolatedDarkPixels", maxSpeckles,
            "isolatedDarkRangeAcrossFrames", maxSpeckles - minSpeckles,
            "maximumDistinctRgbPerFrame", maxColors);
    }

    private static Map<String,Object> ordered(Object... values) {
        Map<String,Object> out = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) out.put((String) values[i], values[i + 1]);
        return out;
    }

    private static final class Arguments {
        Path cache, output, exports;
        static Arguments parse(String[] args) {
            Arguments parsed = new Arguments();
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 >= args.length) usage();
                if ("--cache".equals(args[i])) parsed.cache = Path.of(args[i + 1]);
                else if ("--output".equals(args[i])) parsed.output = Path.of(args[i + 1]);
                else if ("--exports".equals(args[i])) parsed.exports = Path.of(args[i + 1]);
                else usage();
            }
            if (parsed.cache == null || parsed.output == null || parsed.exports == null
                || parsed.output.getParent() == null) usage();
            return parsed;
        }
        static void usage() {
            throw new IllegalArgumentException("usage: --cache PATH --output REPORT.json --exports DIRECTORY");
        }
    }
}
