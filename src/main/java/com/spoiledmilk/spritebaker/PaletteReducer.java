package com.spoiledmilk.spritebaker;

import java.awt.image.BufferedImage;
import java.util.Set;

/** Deterministic fixed-cube palette reduction with optional ordered dithering. */
public final class PaletteReducer {
    public static final String UNMODIFIED = "Unmodified color";
    public static final String RSC_125 = "RSC 125-color cube";
    public static final String RSC_64 = "RSC 64-color cube";
    public static final String RSC_27 = "RSC 27-color cube";
    public static final String NO_DITHER = "No dithering";
    public static final String ORDERED_4X4 = "Ordered 4x4";
    private static final int[][] BAYER_4X4 = {
        {0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}
    };

    private PaletteReducer() { }

    static void validate(String palette, String dithering) {
        if (!Set.of(UNMODIFIED, RSC_125, RSC_64, RSC_27).contains(palette)) throw new IllegalArgumentException("unknown palette: " + palette);
        if (!Set.of(NO_DITHER, ORDERED_4X4).contains(dithering)) throw new IllegalArgumentException("unknown dithering: " + dithering);
    }

    public static BufferedImage apply(BufferedImage source, VisualSettings settings) {
        settings.validate();
        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        if (UNMODIFIED.equals(settings.palette)) {
            output.setRGB(0, 0, source.getWidth(), source.getHeight(),
                source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth()), 0, source.getWidth());
            return output;
        }
        int levels = RSC_125.equals(settings.palette) ? 5 : RSC_64.equals(settings.palette) ? 4 : 3;
        double step = 255.0 / (levels - 1);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = argb >>> 24;
                if (alpha == 0) continue;
                double bias = 0;
                if (ORDERED_4X4.equals(settings.dithering)) {
                    bias = ((BAYER_4X4[y & 3][x & 3] + 0.5) / 16.0 - 0.5) * step * settings.ditherStrength;
                }
                int red = quantize((argb >>> 16) & 255, bias, levels);
                int green = quantize((argb >>> 8) & 255, bias, levels);
                int blue = quantize(argb & 255, bias, levels);
                output.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return output;
    }

    private static int quantize(int value, double bias, int levels) {
        int index = (int) Math.round(Math.max(0, Math.min(255, value + bias)) * (levels - 1) / 255.0);
        return (int) Math.round(index * 255.0 / (levels - 1));
    }
}
