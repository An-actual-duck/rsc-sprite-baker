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

    @Test void supportedDarkSubpixelFeatureSurvivesButOneSpeckDoesNot() {
        StaticRenderer.RasterFrame feature=detailBlock(2),speck=detailBlock(1);
        BufferedImage preserved=MaterialStylizer.reduce(feature,1,1,3);
        BufferedImage rejected=MaterialStylizer.reduce(speck,1,1,3);
        assertTrue(MaterialStylizer.luminance(preserved.getRGB(0,0))+28
            < MaterialStylizer.luminance(rejected.getRGB(0,0)));
    }

    @Test void everyBaseToneHasDistinctDarkerShadowSteps() {
        for(int base:new int[]{40,68,104,148,204,236}){
            assertTrue(MaterialStylizer.shadowLight(base,1)<base);
            assertTrue(MaterialStylizer.shadowLight(base,2)<MaterialStylizer.shadowLight(base,1));
        }
    }

    @Test void rampHuesHaveEqualPerceptualLuminanceInsteadOfYellowHighlights() {
        for(float hue:new float[]{0f,1f/6f,1f/3f,2f/3f}){
            int rgb=MaterialStylizer.rgbAtLuminance(hue,.78f,104);
            assertEquals(104,MaterialStylizer.luminance(rgb),1,
                "hue must not change the requested shade luminance");
        }
    }

    @Test void smallBrightFaceVariantCannotOutshineItsPackedHslFamily() {
        BufferedImage image=new BufferedImage(10,1,BufferedImage.TYPE_INT_ARGB);int[] surfaces=new int[10];
        for(int x=0;x<10;x++){
            boolean highlight=x>=8;image.setRGB(x,0,highlight?0xffeeeeee:0xff777777);
            surfaces[x]=0x10000+(highlight?120:64);
        }
        BufferedImage output=MaterialStylizer.reduce(new StaticRenderer.RasterFrame(image,surfaces),10,1,1);
        int body=MaterialStylizer.luminance(output.getRGB(0,0));
        assertTrue(MaterialStylizer.luminance(output.getRGB(9,0))<=body);
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

    @Test void depthOverlapDarkensOnlyTheFartherInteriorSurface() {
        BufferedImage image = new BufferedImage(13, 3, BufferedImage.TYPE_INT_ARGB);
        int[] surfaces = new int[39], facets = new int[39];
        double[] lighting = new double[39], depth = new double[39];
        Arrays.fill(surfaces, 4);
        Arrays.fill(facets, 1);
        Arrays.fill(lighting, .78);
        for (int y = 0; y < 3; y++) for (int x = 0; x < 13; x++) image.setRGB(x, y, 0xff8c6048);
        for (int y = 0; y < 3; y++) {
            facets[y * 13 + 6] = 2;
            depth[y * 13 + 6] = 10;
        }
        BufferedImage output = MaterialStylizer.reduce(
            new StaticRenderer.RasterFrame(image, surfaces, facets, lighting, depth), 13, 3, 1);
        int farBody = MaterialStylizer.luminance(output.getRGB(0, 1));
        int contactBody = MaterialStylizer.luminance(output.getRGB(5, 1));
        int foreground = MaterialStylizer.luminance(output.getRGB(6, 1));
        assertTrue(contactBody < farBody);
        assertTrue(contactBody < foreground);
        assertEquals(farBody, MaterialStylizer.luminance(output.getRGB(12, 1)));
    }

    @Test void contactShadowHasDarkCoreAndShortDitheredFalloff() {
        int width = 13, height = 3;
        int[] surfaces = new int[width * height], facets = new int[width * height];
        double[] depth = new double[width * height];
        Arrays.fill(surfaces, 4);Arrays.fill(facets, 1);
        for (int y = 0; y < height; y++) {
            facets[y * width + 6] = 2;depth[y * width + 6] = 10;
        }
        double[] shadow = MaterialStylizer.depthOcclusionShadows(
            surfaces, facets, depth, width, height);
        assertTrue(shadow[1 * width + 5] > shadow[1 * width + 4]);
        assertTrue(shadow[1 * width + 4] > shadow[1 * width + 3]);
        assertEquals(0, shadow[1 * width]);
        assertEquals(0, shadow[1 * width + 6]);
    }

    @Test void shadingDitherIsLocalizedDeterministicAndUsesAdjacentRampSteps() {
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) {
            assertEquals(0, MaterialStylizer.ditheredShadowSteps(0, x, y));
            assertEquals(1, MaterialStylizer.ditheredShadowSteps(1, x, y));
        }
        int lighter = 0, darker = 0;
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) {
            int first = MaterialStylizer.ditheredShadowSteps(.5, x, y);
            assertEquals(first, MaterialStylizer.ditheredShadowSteps(.5, x, y));
            if (first == 0) lighter++; else if (first == 1) darker++;
            else throw new AssertionError("fractional shadow skipped an adjacent ramp");
        }
        assertEquals(8, lighter);assertEquals(8, darker);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++)
            assertEquals(1, MaterialStylizer.ditheredShadowSteps(.75, x, y),
                "a mostly dark transition must become solid instead of leaving pale flecks");
    }

    @Test void illuminationUsesDitherOnlyBetweenSolidShadeBands() {
        assertEquals(0, MaterialStylizer.illuminationShadow(.08));
        assertEquals(1, MaterialStylizer.illuminationShadow(.16));
        assertEquals(1, MaterialStylizer.illuminationShadow(.25));
        assertEquals(2, MaterialStylizer.illuminationShadow(.30));
    }

    @Test void directionalInnerShadowDarkensOnlyTheLightOpposedSilhouette() {
        BufferedImage image=new BufferedImage(9,9,BufferedImage.TYPE_INT_ARGB);int[] surfaces=new int[81];
        for(int y=2;y<=6;y++)for(int x=2;x<=6;x++){image.setRGB(x,y,0xffa08060);surfaces[y*9+x]=4;}
        double[] shadow=MaterialStylizer.directionalInnerShadows(image,surfaces,9,9,-1,-1);
        assertEquals(0,shadow[2*9+2]);
        assertTrue(shadow[6*9+6]>0);
        assertTrue(shadow[5*9+5]>0);
        assertTrue(shadow[4*9+4]>0);
        assertEquals(0,shadow[3*9+3]);
        assertEquals(0,shadow[7*9+7]);
    }

    @Test void transparentSilhouetteEdgeDoesNotReceiveAnOutline() {
        BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        int[] surfaces = new int[25], facets = new int[25];
        double[] lighting = new double[25], depth = new double[25];
        for (int y = 1; y < 4; y++) for (int x = 1; x < 4; x++) {
            int index = y * 5 + x;
            image.setRGB(x, y, 0xff8c6048);surfaces[index] = 4;facets[index] = 1;lighting[index] = .72;
        }
        BufferedImage output = MaterialStylizer.reduce(
            new StaticRenderer.RasterFrame(image, surfaces, facets, lighting, depth), 5, 5, 1);
        assertEquals(output.getRGB(1, 2), output.getRGB(2, 2));
    }

    @Test void signedLightingAddsShadowsButNeverPromotesHighlights() {
        BufferedImage image = new BufferedImage(9, 3, BufferedImage.TYPE_INT_ARGB);
        int[] surfaces = new int[27], facets = new int[27];
        double[] sourceLighting = new double[27], signedShading = new double[27], depth = new double[27];
        Arrays.fill(surfaces, 4);Arrays.fill(sourceLighting, .9);
        for (int y = 0; y < 3; y++) for (int x = 0; x < 9; x++) {
            int index = y * 9 + x;image.setRGB(x, y, 0xffa25a36);facets[index] = x / 3 + 1;
            signedShading[index] = x < 3 ? .55 : x < 6 ? .75 : .9;
        }
        BufferedImage output = MaterialStylizer.reduce(new StaticRenderer.RasterFrame(
            image, surfaces, facets, sourceLighting, signedShading, depth), 3, 1, 3);
        int shadow = MaterialStylizer.luminance(output.getRGB(0, 0));
        int mid = MaterialStylizer.luminance(output.getRGB(1, 0));
        int lightFacing = MaterialStylizer.luminance(output.getRGB(2, 0));
        assertTrue(shadow < mid);assertTrue(mid < lightFacing);
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
    private static StaticRenderer.RasterFrame detailBlock(int darkPixels){
        BufferedImage image=new BufferedImage(3,3,BufferedImage.TYPE_INT_ARGB);int[] surfaces=new int[9];
        Arrays.fill(surfaces,7);for(int i=0;i<9;i++)image.setRGB(i%3,i/3,0xffb08060);
        for(int i=0;i<darkPixels;i++){image.setRGB(i,0,0xff201814);surfaces[i]=8;}
        return new StaticRenderer.RasterFrame(image,surfaces);
    }
}
