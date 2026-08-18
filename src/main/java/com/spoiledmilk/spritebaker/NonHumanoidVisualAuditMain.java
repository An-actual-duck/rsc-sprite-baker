package com.spoiledmilk.spritebaker;

import com.google.gson.GsonBuilder;
import java.awt.image.BufferedImage;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

/** Deterministic terminal-only export and visual-metric audit for varied creature bodies. */
public final class NonHumanoidVisualAuditMain {
    private NonHumanoidVisualAuditMain(){}

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        Path repository = Path.of("").toRealPath();
        Path cache = arguments.cache.toRealPath();
        Path output = arguments.output.toAbsolutePath().normalize();
        Path exports = arguments.exports.toAbsolutePath().normalize();
        Main.enforceOutputBoundary(output.getParent(), cache, repository);
        Main.enforceOutputBoundary(exports, cache, repository);
        Files.createDirectories(output.getParent());
        Files.createDirectories(exports);

        Map<String,Object> report = new LinkedHashMap<>();
        report.put("schemaVersion", 1);
        report.put("purpose", "terminal-only broad non-humanoid zero-configuration visual validation");
        report.put("cache", ordered(
            "directory", cache.toString(),
            "dataSha256", Hashing.sha256(cache.resolve("main_file_cache.dat2")),
            "referenceIndexSha256", Hashing.sha256(cache.resolve("main_file_cache.idx255"))));
        report.put("render", ordered(
            "preset", "RSC restrained",
            "cellSize", "128x128",
            "sheetSize", "768x384",
            "supersample", 3,
            "padding", 8,
            "modelScale", 0.90,
            "pitchDegrees", 12.0,
            "directionYawDegrees", SheetDirection.YAW_DEGREES,
            "timelineUnitMillis", 20,
            "tweening", true));

        List<Map<String,Object>> results = new ArrayList<>();
        Map<String,Integer> familyCounts = new LinkedHashMap<>();
        int passed = 0;
        for (int index = 0; index < NonHumanoidVisualMatrix.ENTRIES.size(); index++) {
            NonHumanoidVisualMatrix.Entry entry = NonHumanoidVisualMatrix.ENTRIES.get(index);
            entry.families.forEach(family -> familyCounts.merge(family, 1, Integer::sum));
            System.err.println("Auditing " + (index + 1) + " / " + NonHumanoidVisualMatrix.ENTRIES.size()
                + ": NPC " + entry.npcId + " " + entry.expectedName);
            Map<String,Object> result;
            try {
                result = audit(cache, exports, entry);
                if (((List<?>) result.get("issues")).isEmpty()) passed++;
            } catch (Exception failure) {
                result = new LinkedHashMap<>();
                result.put("npcId", entry.npcId);
                result.put("expectedName", entry.expectedName);
                result.put("families", entry.families);
                result.put("issues", List.of("audit failed closed: " + failure.getClass().getSimpleName() + ": " + failure.getMessage()));
            }
            results.add(result);
        }
        report.put("matrixSize", results.size());
        report.put("familyCounts", familyCounts);
        report.put("cleanResults", passed);
        report.put("flaggedResults", results.size() - passed);
        report.put("results", results);
        try (Writer writer = Files.newBufferedWriter(output)) {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(report, writer);
            writer.write(System.lineSeparator());
        }
        System.out.println("Matrix entries: " + results.size());
        System.out.println("Clean results: " + passed);
        System.out.println("Flagged results: " + (results.size() - passed));
        System.out.println("Wrote " + output);
        System.out.println("Export evidence: " + exports);
    }

    private static Map<String,Object> audit(Path cache, Path exports, NonHumanoidVisualMatrix.Entry entry) throws Exception {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("npcId", entry.npcId);
        result.put("expectedName", entry.expectedName);
        result.put("families", entry.families);
        List<String> issues = new ArrayList<>();
        try (AnimationWorkspace workspace = new AnimationWorkspace(cache, entry.npcId)) {
            SpriteProject project = new SpriteProject();
            project.npcId = entry.npcId;
            AutomaticSheetBuilder.Result setup = AutomaticSheetBuilder.populate(project, workspace);
            result.put("decodedName", workspace.npc.name);
            result.put("nameMatches", entry.expectedName.equals(workspace.npc.name));
            if (!entry.expectedName.equals(workspace.npc.name)) issues.add("cache name differs from matrix expectation");
            result.put("componentModelIds", workspace.npc.modelIds);
            TextureDiagnostics530.Report materials = TextureDiagnostics530.analyze(workspace.baseModel, workspace.npc, workspace.textures);
            result.put("materials", ordered(
                "materialIds", materials.materialIds,
                "supportedMaterialIds", materials.supportedMaterialIds,
                "texturedFaces", materials.texturedFaces,
                "type0Mappings", materials.type0Mappings,
                "advancedMappings", materials.advancedMappingFallbacks,
                "faceLocalMappings", materials.faceLocalMappings,
                "errors", materials.errors));
            if (!materials.errors.isEmpty()) issues.add("material diagnostics reported errors: " + materials.errors);
            result.put("animations", ordered(
                "standingSequenceId", project.standingSequenceId,
                "walkingSequenceId", project.walkingSequenceId,
                "combatSequenceId", project.combatSequenceId,
                "combatCandidateCount", setup.combatCandidateCount,
                "movementCombatFallback", setup.movementCombatFallback,
                "combatScore", setup.combatScore,
                "combatReason", setup.combatReason));

            int assigned = 0;
            List<Map<String,Object>> selections = new ArrayList<>();
            for (int row = 0; row < TargetSheet.ROWS; row++) for (int column = 0; column < TargetSheet.COLUMNS; column++) {
                PoseSelection pose = project.sheet.cells[row][column].pose;
                if (pose != null) assigned++;
                selections.add(ordered("row", row, "column", column,
                    "sequenceId", pose == null ? -1 : pose.sequenceId,
                    "frameIndex", pose == null ? -1 : pose.frameIndex,
                    "cycleOffset", pose == null ? -1 : pose.cycleOffset,
                    "timeMillis", pose == null ? -1 : pose.timeMillis,
                    "source", pose == null ? null : pose.source));
            }
            result.put("assignedCells", assigned);
            result.put("selections", selections);
            if (assigned != TargetSheet.ROWS * TargetSheet.COLUMNS) issues.add("automatic population assigned " + assigned + " of 18 cells");
            if (assigned == 18) {
                Path npcExports = exports.resolve(String.format("npc-%05d", entry.npcId));
                new SheetExporter().export(workspace, project, npcExports);
                Path png = npcExports.resolve("npc-" + entry.npcId + "-rsc-sheet.png");
                Path manifest = npcExports.resolve("npc-" + entry.npcId + "-sheet-diagnostic.json");
                BufferedImage sheet = ImageIO.read(png.toFile());
                if (sheet == null) throw new IllegalStateException("exported PNG cannot be decoded");
                result.put("export", ordered(
                    "png", png.toString(),
                    "pngSha256", Hashing.sha256(png),
                    "manifest", manifest.toString(),
                    "manifestSha256", Hashing.sha256(manifest),
                    "width", sheet.getWidth(),
                    "height", sheet.getHeight()));
                if (sheet.getWidth() != 768 || sheet.getHeight() != 384) issues.add("export dimensions are not 768x384");
                List<Map<String,Object>> cells = new ArrayList<>();
                String[][] hashes = new String[3][6];
                for (int row = 0; row < 3; row++) for (int column = 0; column < 6; column++) {
                    CellMetrics metrics = measure(sheet, column * 128, row * 128, 128, 128);
                    hashes[row][column] = metrics.sha256;
                    cells.add(metrics.asMap(row, column));
                    if (metrics.visiblePixels == 0) issues.add("empty rendered cell " + row + "," + column);
                    if (metrics.edgePixels > 0) issues.add("rendered pixels touch cell edge at " + row + "," + column);
                }
                result.put("cells", cells);
                List<Integer> movementDistinct = new ArrayList<>();
                for (int column = 0; column < 5; column++) movementDistinct.add(distinct(hashes[0][column], hashes[1][column], hashes[2][column]));
                int directionDistinct = distinct(hashes[0][0], hashes[0][1], hashes[0][2], hashes[0][3], hashes[0][4]);
                int combatDistinct = distinct(hashes[0][5], hashes[1][5], hashes[2][5]);
                result.put("distinctness", ordered(
                    "movementPoseCountsByDirection", movementDistinct,
                    "standingDirectionCount", directionDistinct,
                    "combatPoseCount", combatDistinct));
                if (movementDistinct.stream().anyMatch(count -> count < 2)) issues.add("one or more movement directions render fewer than two distinct poses");
                if (directionDistinct < 3) issues.add("standing renders fewer than three distinct directional views");
                if (!setup.movementCombatFallback && combatDistinct < 2) issues.add("detected combat renders fewer than two distinct poses");
            }
        }
        result.put("issues", issues);
        return result;
    }

    private static CellMetrics measure(BufferedImage image, int startX, int startY, int width, int height) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer bytes = ByteBuffer.allocate(4);
        int visible = 0, translucent = 0, edge = 0;
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int argb = image.getRGB(startX + x, startY + y);
            digest.update(bytes.clear().putInt(argb).array());
            int alpha = argb >>> 24;
            if (alpha == 0) continue;
            visible++;
            if (alpha < 255) translucent++;
            minX = Math.min(minX, x); minY = Math.min(minY, y);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
            if (x == 0 || y == 0 || x == width - 1 || y == height - 1) edge++;
        }
        return new CellMetrics(hex(digest.digest()), visible, translucent, edge, minX, minY, maxX, maxY);
    }

    private static int distinct(String... values) { return new LinkedHashSet<>(Arrays.asList(values)).size(); }
    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) out.append(String.format("%02x", value));
        return out.toString();
    }
    private static Map<String,Object> ordered(Object... values) {
        Map<String,Object> out = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) out.put((String) values[i], values[i + 1]);
        return out;
    }

    private static final class CellMetrics {
        final String sha256;
        final int visiblePixels, translucentPixels, edgePixels, minX, minY, maxX, maxY;
        CellMetrics(String sha256, int visiblePixels, int translucentPixels, int edgePixels,
                    int minX, int minY, int maxX, int maxY) {
            this.sha256 = sha256; this.visiblePixels = visiblePixels; this.translucentPixels = translucentPixels;
            this.edgePixels = edgePixels; this.minX = minX; this.minY = minY; this.maxX = maxX; this.maxY = maxY;
        }
        Map<String,Object> asMap(int row, int column) {
            return ordered("row", row, "rowLabel", TargetSheet.ROW_LABELS[row],
                "column", column, "columnLabel", TargetSheet.COLUMN_LABELS[column],
                "yawDegrees", SheetDirection.yawDegrees(column), "argbSha256", sha256,
                "visiblePixels", visiblePixels, "translucentPixels", translucentPixels,
                "edgePixels", edgePixels, "bounds", maxX < 0 ? null : List.of(minX, minY, maxX, maxY),
                "widthOccupancy", maxX < 0 ? 0.0 : (maxX - minX + 1) / 128.0,
                "heightOccupancy", maxY < 0 ? 0.0 : (maxY - minY + 1) / 128.0);
        }
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
            if (parsed.cache == null || parsed.output == null || parsed.exports == null || parsed.output.getParent() == null) usage();
            return parsed;
        }
        static void usage() { throw new IllegalArgumentException("usage: --cache PATH --output REPORT.json --exports DIRECTORY"); }
    }
}
