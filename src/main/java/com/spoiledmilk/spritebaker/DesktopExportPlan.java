package com.spoiledmilk.spritebaker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;

/** Deterministic, human-readable output paths for the zero-config desktop. */
final class DesktopExportPlan {
    final Path directory;
    final Path png;
    final Path manifest;

    private DesktopExportPlan(Path directory, Path png, Path manifest) {
        this.directory = directory;
        this.png = png;
        this.manifest = manifest;
    }

    static DesktopExportPlan forNpc(Path directory, int npcId, String npcName) {
        Path normalized = directory.toAbsolutePath().normalize();
        String base = "npc-" + npcId + "-" + slug(npcName);
        return new DesktopExportPlan(normalized,
            normalized.resolve(base + "-rsc-sheet.png"),
            normalized.resolve(base + "-sheet-provenance.json"));
    }

    static DesktopExportPlan legacy(Path directory, int npcId) {
        Path normalized = directory.toAbsolutePath().normalize();
        return new DesktopExportPlan(normalized,
            normalized.resolve("npc-" + npcId + "-rsc-sheet.png"),
            normalized.resolve("npc-" + npcId + "-sheet-diagnostic.json"));
    }

    boolean wouldOverwrite() {
        return Files.exists(png) || Files.exists(manifest);
    }

    private static String slug(String value) {
        String source = value == null ? "npc" : Normalizer.normalize(value, Normalizer.Form.NFKD);
        String slug = source.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? "npc" : slug;
    }
}
