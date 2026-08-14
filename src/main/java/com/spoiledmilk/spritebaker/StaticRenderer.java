package com.spoiledmilk.spritebaker;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import net.runelite.cache.definitions.ModelDefinition;

/** Minimal deterministic, headless, untextured triangle renderer. */
public final class StaticRenderer {
    public static final int WIDTH = 256;
    public static final int HEIGHT = 256;
    public static final int PADDING = 16;
    public static final double YAW_DEGREES = 25.0;
    public static final double PITCH_DEGREES = 15.0;
    public static final double[] LIGHT_DIRECTION = {-0.35, 0.65, -0.68};
    public static final double AMBIENT_LIGHT = 0.45;
    public static final double DIFFUSE_LIGHT = 0.55;

    public BufferedImage render(List<ModelDefinition> models, NpcDefinition530 npc) {
        if (models.isEmpty()) {
            throw new IllegalArgumentException("at least one model is required");
        }

        int totalVertices = models.stream().mapToInt(model -> model.vertexCount).sum();
        double[] projectedX = new double[totalVertices];
        double[] projectedY = new double[totalVertices];
        double[] depth = new double[totalVertices];

        double yaw = Math.toRadians(YAW_DEGREES);
        double pitch = Math.toRadians(PITCH_DEGREES);
        int vertexOffset = 0;
        for (ModelDefinition model : models) {
            for (int i = 0; i < model.vertexCount; i++) {
                double x = model.vertexX[i] * npc.widthScale / 128.0;
                double up = -model.vertexY[i] * npc.heightScale / 128.0;
                double z = model.vertexZ[i] * npc.widthScale / 128.0;
                double cameraX = Math.cos(yaw) * x + Math.sin(yaw) * z;
                double cameraDepth = -Math.sin(yaw) * x + Math.cos(yaw) * z;
                projectedX[vertexOffset + i] = cameraX;
                projectedY[vertexOffset + i] = up * Math.cos(pitch) - cameraDepth * Math.sin(pitch);
                depth[vertexOffset + i] = up * Math.sin(pitch) + cameraDepth * Math.cos(pitch);
            }
            vertexOffset += model.vertexCount;
        }

        double minX = Arrays.stream(projectedX).min().orElseThrow();
        double maxX = Arrays.stream(projectedX).max().orElseThrow();
        double minY = Arrays.stream(projectedY).min().orElseThrow();
        double maxY = Arrays.stream(projectedY).max().orElseThrow();
        double scale = Math.min(
            (WIDTH - PADDING * 2.0) / Math.max(1.0, maxX - minX),
            (HEIGHT - PADDING * 2.0) / Math.max(1.0, maxY - minY));
        double centerX = (minX + maxX) / 2.0;

        double[] screenX = new double[totalVertices];
        double[] screenY = new double[totalVertices];
        for (int i = 0; i < totalVertices; i++) {
            screenX[i] = WIDTH / 2.0 + (projectedX[i] - centerX) * scale;
            screenY[i] = HEIGHT - PADDING - (projectedY[i] - minY) * scale;
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        double[] zBuffer = new double[WIDTH * HEIGHT];
        Arrays.fill(zBuffer, Double.NEGATIVE_INFINITY);
        vertexOffset = 0;
        for (ModelDefinition model : models) {
            rejectTextures(model);
            for (int face = 0; face < model.faceCount; face++) {
                int a = vertexOffset + model.faceIndices1[face];
                int b = vertexOffset + model.faceIndices2[face];
                int c = vertexOffset + model.faceIndices3[face];
                int packedColor = recolor(model.faceColors[face], npc);
                int rgb = litColor(packedColor, model, face, npc);
                int alpha = model.faceTransparencies == null
                    ? 255 : 255 - Byte.toUnsignedInt(model.faceTransparencies[face]);
                rasterize(image, zBuffer, screenX, screenY, depth, a, b, c, (alpha << 24) | rgb);
            }
            vertexOffset += model.vertexCount;
        }
        return image;
    }

    private static void rejectTextures(ModelDefinition model) {
        if (model.faceTextures == null) {
            return;
        }
        for (short texture : model.faceTextures) {
            if (texture != -1) {
                throw new UnsupportedOperationException("model " + model.id + " contains textured faces");
            }
        }
    }

    private static int recolor(short source, NpcDefinition530 npc) {
        for (int i = 0; i < npc.recolorFrom.length; i++) {
            if (source == npc.recolorFrom[i]) {
                return Short.toUnsignedInt(npc.recolorTo[i]);
            }
        }
        return Short.toUnsignedInt(source);
    }

    private static int litColor(int packedHsl, ModelDefinition model, int face, NpcDefinition530 npc) {
        int ia = model.faceIndices1[face];
        int ib = model.faceIndices2[face];
        int ic = model.faceIndices3[face];
        double ax = model.vertexX[ib] - model.vertexX[ia];
        double ay = -(model.vertexY[ib] - model.vertexY[ia]);
        double az = model.vertexZ[ib] - model.vertexZ[ia];
        double bx = model.vertexX[ic] - model.vertexX[ia];
        double by = -(model.vertexY[ic] - model.vertexY[ia]);
        double bz = model.vertexZ[ic] - model.vertexZ[ia];
        double nx = ay * bz - az * by;
        double ny = az * bx - ax * bz;
        double nz = ax * by - ay * bx;
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        double lambert = length == 0 ? 0 : Math.abs(
            (nx * LIGHT_DIRECTION[0] + ny * LIGHT_DIRECTION[1] + nz * LIGHT_DIRECTION[2]) / length);
        double npcAdjustment = npc.ambient / 512.0 + npc.contrast / 4096.0;
        double brightness = clamp(AMBIENT_LIGHT + DIFFUSE_LIGHT * lambert + npcAdjustment, 0.15, 1.0);
        int base = packedHslToRgb(packedHsl);
        int red = (int) Math.round(((base >>> 16) & 255) * brightness);
        int green = (int) Math.round(((base >>> 8) & 255) * brightness);
        int blue = (int) Math.round((base & 255) * brightness);
        return (red << 16) | (green << 8) | blue;
    }

    static int packedHslToRgb(int packed) {
        double hue = ((packed >>> 10) & 63) / 64.0;
        double saturation = ((packed >>> 7) & 7) / 8.0;
        double lightness = (packed & 127) / 128.0;
        double red;
        double green;
        double blue;
        if (saturation == 0) {
            red = green = blue = lightness;
        } else {
            double q = lightness < 0.5
                ? lightness * (1 + saturation)
                : lightness + saturation - lightness * saturation;
            double p = 2 * lightness - q;
            red = hueToRgb(p, q, hue + 1.0 / 3.0);
            green = hueToRgb(p, q, hue);
            blue = hueToRgb(p, q, hue - 1.0 / 3.0);
        }
        return ((int) Math.round(red * 255) << 16)
            | ((int) Math.round(green * 255) << 8)
            | (int) Math.round(blue * 255);
    }

    private static double hueToRgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0 / 6.0) return p + (q - p) * 6 * t;
        if (t < 1.0 / 2.0) return q;
        if (t < 2.0 / 3.0) return p + (q - p) * (2.0 / 3.0 - t) * 6;
        return p;
    }

    private static void rasterize(BufferedImage image, double[] zBuffer,
                                  double[] x, double[] y, double[] z,
                                  int a, int b, int c, int argb) {
        double area = edge(x[a], y[a], x[b], y[b], x[c], y[c]);
        if (Math.abs(area) < 0.00001 || (argb >>> 24) == 0) {
            return;
        }
        int minX = Math.max(0, (int) Math.floor(Math.min(x[a], Math.min(x[b], x[c]))));
        int maxX = Math.min(WIDTH - 1, (int) Math.ceil(Math.max(x[a], Math.max(x[b], x[c]))));
        int minY = Math.max(0, (int) Math.floor(Math.min(y[a], Math.min(y[b], y[c]))));
        int maxY = Math.min(HEIGHT - 1, (int) Math.ceil(Math.max(y[a], Math.max(y[b], y[c]))));
        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                double sampleX = px + 0.5;
                double sampleY = py + 0.5;
                double wa = edge(x[b], y[b], x[c], y[c], sampleX, sampleY) / area;
                double wb = edge(x[c], y[c], x[a], y[a], sampleX, sampleY) / area;
                double wc = 1.0 - wa - wb;
                if (wa < -0.000001 || wb < -0.000001 || wc < -0.000001) {
                    continue;
                }
                double pixelDepth = wa * z[a] + wb * z[b] + wc * z[c];
                int index = py * WIDTH + px;
                if (pixelDepth > zBuffer[index]) {
                    zBuffer[index] = pixelDepth;
                    image.setRGB(px, py, argb);
                }
            }
        }
    }

    private static double edge(double ax, double ay, double bx, double by, double px, double py) {
        return (px - ax) * (by - ay) - (py - ay) * (bx - ax);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
