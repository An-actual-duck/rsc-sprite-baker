package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MaterialStylizerTest {
    @Test void robustMaterialBlockRejectsOneDarkTexelAndIsPositionStable() {
        StaticRenderer.RasterFrame first = block(0xffb06040, 7, 4);
        StaticRenderer.RasterFrame second = block(0xffb06040, 7, 0);
        BufferedImage a = MaterialStylizer.reduce(first, 1, 1, 3);
        BufferedImage b = MaterialStylizer.reduce(second, 1, 1, 3);
        assertEquals(a.getRGB(0, 0), b.getRGB(0, 0));
        assertTrue(MaterialStylizer.luminance(a.getRGB(0, 0)) >= 60);
    }

    @Test void centerSamplePreservesSilhouetteAndMaterialBoundary() {
        BufferedImage image = new BufferedImage(6, 3, BufferedImage.TYPE_INT_ARGB);
        int[] surfaces = new int[18];
        for (int y = 0; y < 3; y++) for (int x = 0; x < 6; x++) {
            image.setRGB(x, y, x < 3 ? 0xffc04030 : 0xff3060c0);
            surfaces[y * 6 + x] = x < 3 ? 1 : 2;
        }
        image.setRGB(1, 1, 0);
        BufferedImage output = MaterialStylizer.reduce(new StaticRenderer.RasterFrame(image, surfaces), 2, 1, 3);
        assertEquals(0, output.getRGB(0, 0));
        assertTrue(((output.getRGB(1, 0) >>> 16) & 255) < (output.getRGB(1, 0) & 255));
    }

    @Test void flatCleanSurfaceStaysFlatWithControlledRampPalette() {
        BufferedImage image = new BufferedImage(24, 3, BufferedImage.TYPE_INT_ARGB);
        int[] surfaces = new int[72];
        for (int x = 0; x < 24; x++) for (int y = 0; y < 3; y++) {
            int value = 20 + x * 9;
            image.setRGB(x, y, 0xff000000 | (value << 16) | (value / 2 << 8) | value / 3);
            surfaces[y * 24 + x] = 9;
        }
        BufferedImage output = MaterialStylizer.reduce(new StaticRenderer.RasterFrame(image, surfaces), 8, 1, 3);
        long colors = Arrays.stream(output.getRGB(0, 0, 8, 1, null, 0, 8)).distinct().count();
        assertTrue(colors <= 6, "one chroma family should use only the fixed lightness ramp");
    }

    @Test void geometricFaceLightingProducesDistinctShadowAndLightBands() {
        BufferedImage image = new BufferedImage(6, 3, BufferedImage.TYPE_INT_ARGB);
        int[] surfaces = new int[18], facets = new int[18];
        double[] lighting = new double[18];
        for (int y = 0; y < 3; y++) for (int x = 0; x < 6; x++) {
            boolean shadow = x < 3;
            image.setRGB(x, y, shadow ? 0xff633721 : 0xffa25a36);
            surfaces[y * 6 + x] = 4;
            facets[y * 6 + x] = shadow ? 1 : 2;
            lighting[y * 6 + x] = shadow ? .55 : .90;
        }
        BufferedImage output = MaterialStylizer.reduce(
            new StaticRenderer.RasterFrame(image, surfaces, facets, lighting), 2, 1, 3);
        assertTrue(MaterialStylizer.luminance(output.getRGB(0, 0))
            < MaterialStylizer.luminance(output.getRGB(1, 0)));
    }

    @Test void originalStyleRemainsTheDefaultReference() {
        VisualSettings settings = new VisualSettings();
        assertEquals(MaterialStylizer.NONE, settings.materialStyle);
        settings.applyPreset("RSC material");
        assertEquals(MaterialStylizer.RSC_RAMPS, settings.materialStyle);
        assertEquals(PaletteReducer.UNMODIFIED, settings.palette);
        assertEquals(PaletteReducer.NO_DITHER, settings.dithering);
    }

    private static StaticRenderer.RasterFrame block(int color, int surface, int darkIndex) {
        BufferedImage image = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        int[] surfaces = new int[9];
        Arrays.fill(surfaces, surface);
        for (int i = 0; i < 9; i++) image.setRGB(i % 3, i / 3, i == darkIndex ? 0xff010101 : color);
        return new StaticRenderer.RasterFrame(image, surfaces);
    }
}
