package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class SpriteQualityMetricsTest {
    @Test void detectsAnIsolatedDarkSpeckleAndInteriorTransitions() {
        BufferedImage image = solid(5, 5, 0xffa08060);
        image.setRGB(2, 2, 0xff000000);
        SpriteQualityMetrics.Metrics metrics = SpriteQualityMetrics.measure(image);
        assertEquals(1, metrics.blackPixels);
        assertEquals(1, metrics.isolatedDarkPixels);
        assertEquals(4, metrics.interiorTransitions);
    }

    @Test void alphaComparisonMeasuresSilhouetteWithoutConsideringRgb() {
        BufferedImage first = solid(2, 1, 0xffff0000);
        BufferedImage second = solid(2, 1, 0xff00ff00);
        assertEquals(0, SpriteQualityMetrics.alphaMismatches(first, second));
        second.setRGB(1, 0, 0);
        assertEquals(1, SpriteQualityMetrics.alphaMismatches(first, second));
    }

    private static BufferedImage solid(int width, int height, int color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) image.setRGB(x, y, color);
        return image;
    }
}
