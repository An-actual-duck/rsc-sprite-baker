package com.spoiledmilk.spritebaker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Set;

/** Edge- and material-aware reduction used only by the explicit RSC material preset. */
public final class MaterialStylizer {
    public static final String NONE = "Original material detail";
    public static final String RSC_RAMPS = "RSC material ramps";
    private static final int[] LIGHTNESS_RAMP = {40, 68, 104, 148, 204, 236};

    private MaterialStylizer() { }

    static void validate(String style) {
        if (!Set.of(NONE, RSC_RAMPS).contains(style)) {
            throw new IllegalArgumentException("unknown material style: " + style);
        }
    }

    static BufferedImage reduce(StaticRenderer.RasterFrame source, int width, int height, int factor) {
        if (source.image.getWidth() != width * factor || source.image.getHeight() != height * factor) {
            throw new IllegalArgumentException("source dimensions do not match integer reduction factor");
        }
        BufferedImage reduced = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] surfaces = new int[width * height];
        int center = factor / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int centerX = x * factor + center;
                int centerY = y * factor + center;
                int centerPixel = source.image.getRGB(centerX, centerY);
                if ((centerPixel >>> 24) == 0) continue;
                int surface = source.surfaces[centerY * source.image.getWidth() + centerX];
                surfaces[y * width + x] = surface;
                reduced.setRGB(x, y, robustBlockColor(source, x * factor, y * factor,
                    factor, surface, centerPixel >>> 24));
            }
        }
        BufferedImage cleaned = suppressIsolatedDarkPixels(reduced, surfaces);
        BufferedImage smoothed = medianWithinSurface(medianWithinSurface(cleaned, surfaces), surfaces);
        return suppressIsolatedDarkPixels(ramp(smoothed, surfaces), surfaces);
    }

    private static int robustBlockColor(StaticRenderer.RasterFrame source, int startX, int startY,
                                        int factor, int surface, int alpha) {
        int[] red = new int[factor * factor];
        int[] green = new int[factor * factor];
        int[] blue = new int[factor * factor];
        int count = 0;
        int width = source.image.getWidth();
        for (int dy = 0; dy < factor; dy++) {
            for (int dx = 0; dx < factor; dx++) {
                int index = (startY + dy) * width + startX + dx;
                int pixel = source.image.getRGB(startX + dx, startY + dy);
                if ((pixel >>> 24) == 0 || source.surfaces[index] != surface) continue;
                red[count] = (pixel >>> 16) & 255;
                green[count] = (pixel >>> 8) & 255;
                blue[count] = pixel & 255;
                count++;
            }
        }
        if (count == 0) return 0;
        Arrays.sort(red, 0, count);
        Arrays.sort(green, 0, count);
        Arrays.sort(blue, 0, count);
        int middle = count / 2;
        return (alpha << 24) | (red[middle] << 16) | (green[middle] << 8) | blue[middle];
    }

    private static BufferedImage suppressIsolatedDarkPixels(BufferedImage source, int[] surfaces) {
        int width = source.getWidth(), height = source.getHeight();
        BufferedImage output = copy(source);
        int[] luminance = new int[8];
        int[] colors = new int[8];
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int pixel = source.getRGB(x, y);
                if ((pixel >>> 24) == 0) continue;
                int surface = surfaces[y * width + x], count = 0;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int index = (y + dy) * width + x + dx;
                    int neighbor = source.getRGB(x + dx, y + dy);
                    if ((neighbor >>> 24) == 0 || surfaces[index] != surface) continue;
                    colors[count] = neighbor;
                    luminance[count++] = luminance(neighbor);
                }
                if (count < 5) continue;
                Arrays.sort(luminance, 0, count);
                int median = luminance[count / 2];
                if (median - luminance(pixel) < 42) continue;
                int replacement = closestLuminance(colors, count, median);
                output.setRGB(x, y, (pixel & 0xff000000) | (replacement & 0xffffff));
            }
        }
        return output;
    }

    private static BufferedImage ramp(BufferedImage source, int[] surfaces) {
        int width = source.getWidth(), height = source.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int pixel = source.getRGB(x, y), alpha = pixel >>> 24;
            if (alpha == 0) continue;
            float[] hsb = Color.RGBtoHSB((pixel >>> 16) & 255, (pixel >>> 8) & 255, pixel & 255, null);
            float hue = Math.round(hsb[0] * 12f) / 12f;
            float saturation = hsb[1] < .16f ? 0f : hsb[1] < .52f ? .42f : .78f;
            int light = nearestRamp(luminance(pixel));
            if (isOuterEdge(x, y, width, height, surfaces) && light > LIGHTNESS_RAMP[1]) {
                light = nearestRamp((int) Math.round(light * .88));
            }
            int rgb = liftMinimumLuminance(Color.HSBtoRGB(hue, saturation, light / 255f) & 0xffffff, 36);
            output.setRGB(x, y, (alpha << 24) | rgb);
        }
        return output;
    }

    private static boolean isOuterEdge(int x, int y, int width, int height, int[] surfaces) {
        return x == 0 || y == 0 || x == width - 1 || y == height - 1
            || surfaces[y * width + x - 1] == 0
            || surfaces[y * width + x + 1] == 0
            || surfaces[(y - 1) * width + x] == 0
            || surfaces[(y + 1) * width + x] == 0;
    }

    private static BufferedImage medianWithinSurface(BufferedImage source, int[] surfaces) {
        int width = source.getWidth(), height = source.getHeight();
        BufferedImage output = copy(source);
        int[] red = new int[9], green = new int[9], blue = new int[9];
        for (int y = 1; y < height - 1; y++) for (int x = 1; x < width - 1; x++) {
            int center = source.getRGB(x, y);
            if ((center >>> 24) == 0) continue;
            int surface = surfaces[y * width + x], count = 0;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                int index = (y + dy) * width + x + dx;
                int pixel = source.getRGB(x + dx, y + dy);
                if ((pixel >>> 24) == 0 || surfaces[index] != surface) continue;
                red[count] = (pixel >>> 16) & 255;
                green[count] = (pixel >>> 8) & 255;
                blue[count] = pixel & 255;
                count++;
            }
            if (count < 5) continue;
            Arrays.sort(red, 0, count);
            Arrays.sort(green, 0, count);
            Arrays.sort(blue, 0, count);
            output.setRGB(x, y, (center & 0xff000000) | (red[count / 2] << 16)
                | (green[count / 2] << 8) | blue[count / 2]);
        }
        return output;
    }

    private static int nearestRamp(int value) {
        int best = LIGHTNESS_RAMP[0];
        for (int candidate : LIGHTNESS_RAMP) {
            if (Math.abs(candidate - value) < Math.abs(best - value)) best = candidate;
        }
        return best;
    }

    private static int closestLuminance(int[] colors, int count, int target) {
        int best = colors[0], distance = Math.abs(luminance(best) - target);
        for (int i = 1; i < count; i++) {
            int candidateDistance = Math.abs(luminance(colors[i]) - target);
            if (candidateDistance < distance) {
                best = colors[i];
                distance = candidateDistance;
            }
        }
        return best;
    }

    static int luminance(int pixel) {
        return (((pixel >>> 16) & 255) * 299 + ((pixel >>> 8) & 255) * 587 + (pixel & 255) * 114) / 1000;
    }

    private static int liftMinimumLuminance(int rgb, int minimum) {
        int luminance = luminance(rgb);
        if (luminance >= minimum) return rgb;
        int amount = minimum - luminance;
        int red = Math.min(255, ((rgb >>> 16) & 255) + amount);
        int green = Math.min(255, ((rgb >>> 8) & 255) + amount);
        int blue = Math.min(255, (rgb & 255) + amount);
        return (red << 16) | (green << 8) | blue;
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        output.setRGB(0, 0, source.getWidth(), source.getHeight(),
            source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth()),
            0, source.getWidth());
        return output;
    }
}
