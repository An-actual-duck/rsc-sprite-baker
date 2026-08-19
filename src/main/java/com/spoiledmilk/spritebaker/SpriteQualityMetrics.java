package com.spoiledmilk.spritebaker;

import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Set;

/** Deterministic terminal metrics for sprite-material visual regressions. */
public final class SpriteQualityMetrics {
    private SpriteQualityMetrics() { }

    public static Metrics measure(BufferedImage image) {
        int visible = 0, black = 0, isolatedDark = 0, transitions = 0;
        Set<Integer> colors = new LinkedHashSet<>();
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int pixel = image.getRGB(x, y);
            if ((pixel >>> 24) == 0) continue;
            visible++;
            int rgb = pixel & 0xffffff;
            colors.add(rgb);
            if (rgb == 0) black++;
            if (isolatedDark(image, x, y, pixel)) isolatedDark++;
            if (x + 1 < image.getWidth() && strongTransition(pixel, image.getRGB(x + 1, y))) transitions++;
            if (y + 1 < image.getHeight() && strongTransition(pixel, image.getRGB(x, y + 1))) transitions++;
        }
        return new Metrics(visible, black, isolatedDark, transitions, colors.size());
    }

    public static int alphaMismatches(BufferedImage first, BufferedImage second) {
        if (first.getWidth() != second.getWidth() || first.getHeight() != second.getHeight()) {
            throw new IllegalArgumentException("image dimensions differ");
        }
        int mismatch = 0;
        for (int y = 0; y < first.getHeight(); y++) for (int x = 0; x < first.getWidth(); x++) {
            if ((first.getRGB(x, y) >>> 24) != (second.getRGB(x, y) >>> 24)) mismatch++;
        }
        return mismatch;
    }

    private static boolean isolatedDark(BufferedImage image, int x, int y, int pixel) {
        if (MaterialStylizer.luminance(pixel) > 32) return false;
        int[] neighbors = new int[8];
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            if (dx == 0 && dy == 0) continue;
            int px = x + dx, py = y + dy;
            if (px < 0 || py < 0 || px >= image.getWidth() || py >= image.getHeight()) continue;
            int neighbor = image.getRGB(px, py);
            if ((neighbor >>> 24) == 0) continue;
            neighbors[count++] = MaterialStylizer.luminance(neighbor);
        }
        if (count < 5) return false;
        java.util.Arrays.sort(neighbors, 0, count);
        return neighbors[count / 2] >= 64;
    }

    private static boolean strongTransition(int first, int second) {
        if ((first >>> 24) == 0 || (second >>> 24) == 0) return false;
        int dr = ((first >>> 16) & 255) - ((second >>> 16) & 255);
        int dg = ((first >>> 8) & 255) - ((second >>> 8) & 255);
        int db = (first & 255) - (second & 255);
        return dr * dr + dg * dg + db * db >= 48 * 48;
    }

    public static final class Metrics {
        public final int visiblePixels, blackPixels, isolatedDarkPixels, interiorTransitions, distinctRgb;
        Metrics(int visiblePixels, int blackPixels, int isolatedDarkPixels,
                int interiorTransitions, int distinctRgb) {
            this.visiblePixels = visiblePixels;
            this.blackPixels = blackPixels;
            this.isolatedDarkPixels = isolatedDarkPixels;
            this.interiorTransitions = interiorTransitions;
            this.distinctRgb = distinctRgb;
        }
    }
}
