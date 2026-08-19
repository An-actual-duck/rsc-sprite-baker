package com.spoiledmilk.spritebaker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
        int[] facets = new int[width * height];
        double[] lighting = new double[width * height];
        double[] depth = new double[width * height];
        int center = factor / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int centerX = x * factor + center;
                int centerY = y * factor + center;
                int centerPixel = source.image.getRGB(centerX, centerY);
                if ((centerPixel >>> 24) == 0) continue;
                int surface = source.surfaces[centerY * source.image.getWidth() + centerX];
                int index = y * width + x;
                surfaces[y * width + x] = surface;
                facets[index] = source.facets[centerY * source.image.getWidth() + centerX];
                depth[index] = source.depth[centerY * source.image.getWidth() + centerX];
                lighting[y * width + x] = robustBlockLighting(source, x * factor, y * factor, factor, surface);
                reduced.setRGB(x, y, robustBlockColor(source, x * factor, y * factor,
                    factor, surface, centerPixel >>> 24));
            }
        }
        BufferedImage cleaned = suppressIsolatedDarkPixels(reduced, surfaces);
        BufferedImage smoothed = medianWithinSurface(medianWithinSurface(cleaned, surfaces), surfaces);
        double[] broadLighting = averageLighting(averageLighting(lighting, surfaces, width, height),
            surfaces, width, height);
        Map<Integer,Integer> baseRamps = materialBaseRamps(smoothed, surfaces, broadLighting);
        boolean[] occlusion = depthOcclusionBands(surfaces, facets, depth, width, height);
        return suppressIsolatedDarkPixels(ramp(smoothed, surfaces, broadLighting, baseRamps, occlusion), surfaces);
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

    private static double robustBlockLighting(StaticRenderer.RasterFrame source, int startX, int startY,
                                              int factor, int surface) {
        double[] values = new double[factor * factor];
        int count = 0, width = source.image.getWidth();
        for (int dy = 0; dy < factor; dy++) for (int dx = 0; dx < factor; dx++) {
            int index = (startY + dy) * width + startX + dx;
            if (source.surfaces[index] == surface && (source.image.getRGB(startX + dx, startY + dy) >>> 24) != 0) {
                values[count++] = source.lighting[index];
            }
        }
        if (count == 0) return .72;
        Arrays.sort(values, 0, count);
        return values[count / 2];
    }

    private static double[] averageLighting(double[] source, int[] surfaces, int width, int height) {
        double[] output = source.clone();
        for (int y = 3; y < height - 3; y++) for (int x = 3; x < width - 3; x++) {
            if (surfaces[y * width + x] == 0) continue;
            int count = 0;double sum=0;
            for (int dy = -3; dy <= 3; dy++) for (int dx = -3; dx <= 3; dx++) {
                int index = (y + dy) * width + x + dx;
                if (surfaces[index] != 0){sum+=source[index];count++;}
            }
            if (count >= 7) output[y * width + x] = sum / count;
        }
        return output;
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

    private static BufferedImage ramp(BufferedImage source, int[] surfaces, double[] lighting,
                                      Map<Integer,Integer> baseRamps, boolean[] occlusion) {
        int width = source.getWidth(), height = source.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int pixel = source.getRGB(x, y), alpha = pixel >>> 24;
            if (alpha == 0) continue;
            float[] hsb = Color.RGBtoHSB((pixel >>> 16) & 255, (pixel >>> 8) & 255, pixel & 255, null);
            float hue = Math.round(hsb[0] * 12f) / 12f;
            float saturation = hsb[1] < .16f ? 0f : hsb[1] < .52f ? .42f : .78f;
            double shade = lighting[y * width + x];
            int rampIndex = baseRamps.get(surfaces[y * width + x]);
            rampIndex += shade < .64 ? -1 : shade >= .80 ? 1 : 0;
            if (occlusion[y * width + x]) rampIndex--;
            int light = LIGHTNESS_RAMP[Math.max(0, Math.min(LIGHTNESS_RAMP.length - 1, rampIndex))];
            int rgb = liftMinimumLuminance(Color.HSBtoRGB(hue, saturation, light / 255f) & 0xffffff, 36);
            output.setRGB(x, y, (alpha << 24) | rgb);
        }
        return output;
    }

    static boolean[] depthOcclusionBands(int[] surfaces, int[] facets, double[] depth,
                                         int width, int height) {
        if (surfaces.length != width * height || facets.length != surfaces.length
            || depth.length != surfaces.length) throw new IllegalArgumentException("occlusion buffers differ");
        double minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < surfaces.length; index++) if (surfaces[index] != 0) {
            minimum = Math.min(minimum, depth[index]);
            maximum = Math.max(maximum, depth[index]);
        }
        boolean[] boundary = new boolean[surfaces.length];
        if (!Double.isFinite(minimum)) return boundary;
        double threshold = Math.max(.5, (maximum - minimum) * .05);
        for (int y = 1; y < height - 1; y++) for (int x = 1; x < width - 1; x++) {
            int index = y * width + x;
            if (surfaces[index] == 0) continue;
            for (int dy = -1; dy <= 1 && !boundary[index]; dy++) for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int neighbor = (y + dy) * width + x + dx;
                if (surfaces[neighbor] != 0 && facets[neighbor] != facets[index]
                    && depth[neighbor] > depth[index] + threshold) {
                    boundary[index] = true;
                    break;
                }
            }
        }
        boolean[] band = boundary.clone();
        for (int y = 1; y < height - 1; y++) for (int x = 1; x < width - 1; x++) {
            int index = y * width + x;
            if (boundary[index]) continue;
            for (int dy = -1; dy <= 1 && !band[index]; dy++) for (int dx = -1; dx <= 1; dx++) {
                int neighbor = (y + dy) * width + x + dx;
                if (boundary[neighbor] && facets[neighbor] == facets[index]
                    && Math.abs(depth[neighbor] - depth[index]) <= threshold) {
                    band[index] = true;
                    break;
                }
            }
        }
        return band;
    }

    private static Map<Integer,Integer> materialBaseRamps(BufferedImage image, int[] surfaces,
                                                           double[] lighting) {
        Map<Integer,int[]> histograms = new HashMap<>();
        Map<Integer,Integer> counts = new HashMap<>();
        int width = image.getWidth(), height = image.getHeight();
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int index = y * width + x, surface = surfaces[index], pixel = image.getRGB(x, y);
            if (surface == 0 || (pixel >>> 24) == 0) continue;
            int unlit = Math.min(255, (int) Math.round(luminance(pixel) / Math.max(.35, lighting[index])));
            histograms.computeIfAbsent(surface, ignored -> new int[256])[unlit]++;
            counts.merge(surface, 1, Integer::sum);
        }
        Map<Integer,Integer> ramps = new HashMap<>();
        for (Map.Entry<Integer,int[]> entry : histograms.entrySet()) {
            int target = counts.get(entry.getKey()) / 2, accumulated = 0, median = 0;
            for (; median < 255; median++) {
                accumulated += entry.getValue()[median];
                if (accumulated > target) break;
            }
            ramps.put(entry.getKey(), nearestRampIndex(median));
        }
        return ramps;
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
        return LIGHTNESS_RAMP[nearestRampIndex(value)];
    }

    private static int nearestRampIndex(int value) {
        int best = 0;
        for (int index = 1; index < LIGHTNESS_RAMP.length; index++) {
            if (Math.abs(LIGHTNESS_RAMP[index] - value) < Math.abs(LIGHTNESS_RAMP[best] - value)) best = index;
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
