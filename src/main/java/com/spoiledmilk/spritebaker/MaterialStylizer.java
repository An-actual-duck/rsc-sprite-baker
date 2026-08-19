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
    private static final int[][] SHADOW_DITHER = {
        {0, 8, 2, 10}, {12, 4, 14, 6}, {3, 11, 1, 9}, {15, 7, 13, 5}
    };

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
        double[] shading = new double[width * height];
        double[] depth = new double[width * height];
        int center = factor / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int centerX = x * factor + center;
                int centerY = y * factor + center;
                int centerPixel = source.image.getRGB(centerX, centerY);
                if ((centerPixel >>> 24) == 0) continue;
                int sampleIndex = detailSurfaceSample(source, x * factor, y * factor, factor,
                    centerY * source.image.getWidth() + centerX);
                int surface = source.surfaces[sampleIndex];
                int index = y * width + x;
                surfaces[y * width + x] = surface;
                facets[index] = source.facets[sampleIndex];
                depth[index] = source.depth[sampleIndex];
                lighting[index] = robustBlockLighting(source, source.lighting,
                    x * factor, y * factor, factor, surface);
                shading[index] = robustBlockLighting(source, source.shading,
                    x * factor, y * factor, factor, surface);
                reduced.setRGB(x, y, robustBlockColor(source, x * factor, y * factor,
                    factor, surface, centerPixel >>> 24));
            }
        }
        BufferedImage cleaned = suppressIsolatedDarkPixels(reduced, surfaces);
        double[] details = supportedDarkDetails(cleaned, surfaces);
        BufferedImage smoothed = restoreDetails(medianWithinSurface(cleaned, surfaces), cleaned, details);
        // A single small, material-bounded pass joins tiny coplanar facets without
        // washing limb and body illumination into one universal midtone.
        double[] broadShading = averageLighting(shading, surfaces, width, height);
        Map<Integer,Integer> baseRamps = materialBaseRamps(smoothed, surfaces, lighting);
        capBrightSurfaceRamps(baseRamps, surfaces);
        double[] contactShadows = depthOcclusionShadows(surfaces, facets, depth, width, height);
        double[] edgeShadows = directionalInnerShadows(reduced, surfaces, width, height,
            source.lightScreenX, source.lightScreenY);
        for (int index = 0; index < contactShadows.length; index++)
            contactShadows[index] = Math.max(details[index],
                Math.max(contactShadows[index], edgeShadows[index]));
        return ramp(smoothed, surfaces, broadShading, baseRamps, contactShadows);
    }

    private static int detailSurfaceSample(StaticRenderer.RasterFrame source, int startX, int startY,
                                           int factor, int centerIndex) {
        int centerSurface = source.surfaces[centerIndex];
        if (factor == 1 || centerSurface == 0) return centerIndex;
        int[] ids = new int[factor * factor], counts = new int[factor * factor], samples = new int[factor * factor];
        int unique = 0, sourceWidth = source.image.getWidth();
        for (int dy = 0; dy < factor; dy++) for (int dx = 0; dx < factor; dx++) {
            int index = (startY + dy) * sourceWidth + startX + dx;
            int surface = source.surfaces[index];
            if (surface == 0 || (source.image.getRGB(startX + dx, startY + dy) >>> 24) == 0) continue;
            int slot = 0;while (slot < unique && ids[slot] != surface) slot++;
            if (slot == unique) { ids[unique] = surface;samples[unique] = index;unique++; }
            counts[slot]++;
        }
        int centerColor = robustBlockColor(source, startX, startY, factor, centerSurface, 255);
        int selected = centerIndex, selectedLight = luminance(centerColor);
        int minimumCoverage = Math.max(2, factor / 2);
        for (int slot = 0; slot < unique; slot++) if (ids[slot] != centerSurface && counts[slot] >= minimumCoverage) {
            int candidate = robustBlockColor(source, startX, startY, factor, ids[slot], 255);
            int candidateLight = luminance(candidate);
            if (candidateLight + 28 <= selectedLight) {
                selected = samples[slot];selectedLight = candidateLight;
            }
        }
        return selected;
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

    private static double robustBlockLighting(StaticRenderer.RasterFrame source, double[] input,
                                              int startX, int startY,
                                              int factor, int surface) {
        double[] values = new double[factor * factor];
        int count = 0, width = source.image.getWidth();
        for (int dy = 0; dy < factor; dy++) for (int dx = 0; dx < factor; dx++) {
            int index = (startY + dy) * width + startX + dx;
            if (source.surfaces[index] == surface && (source.image.getRGB(startX + dx, startY + dy) >>> 24) != 0) {
                values[count++] = input[index];
            }
        }
        if (count == 0) return .72;
        Arrays.sort(values, 0, count);
        return values[count / 2];
    }

    private static double[] averageLighting(double[] source, int[] surfaces, int width, int height) {
        double[] output = source.clone();
        for (int y = 2; y < height - 2; y++) for (int x = 2; x < width - 2; x++) {
            int surface = surfaces[y * width + x];
            if (surface == 0) continue;
            int count = 0;double sum=0;
            for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
                int index = (y + dy) * width + x + dx;
                if (surfaces[index] == surface){sum+=source[index];count++;}
            }
            if (count >= 5) output[y * width + x] = sum / count;
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
                                      Map<Integer,Integer> baseRamps, double[] contactShadows) {
        int width = source.getWidth(), height = source.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Map<Integer,Double> lightReferences = materialLightReferences(surfaces, lighting);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int pixel = source.getRGB(x, y), alpha = pixel >>> 24;
            if (alpha == 0) continue;
            float[] hsb = Color.RGBtoHSB((pixel >>> 16) & 255, (pixel >>> 8) & 255, pixel & 255, null);
            float hue = Math.round(hsb[0] * 12f) / 12f;
            float saturation = hsb[1] < .16f ? 0f : hsb[1] < .52f ? .42f : .78f;
            double shade = lighting[y * width + x];
            int rampIndex = baseRamps.get(surfaces[y * width + x]);
            double reference = lightReferences.get(surfaces[y * width + x]);
            double illuminationShadow = illuminationShadow(reference - shade);
            double shadow = Math.min(3, Math.max(illuminationShadow, contactShadows[y * width + x]));
            int steps = ditheredShadowSteps(shadow, x, y);
            int baseLight = LIGHTNESS_RAMP[Math.max(0, Math.min(LIGHTNESS_RAMP.length - 1, rampIndex))];
            int light = shadowLight(baseLight, steps);
            int rgb = rgbAtLuminance(hue, saturation, Math.max(34, light));
            output.setRGB(x, y, (alpha << 24) | rgb);
        }
        return output;
    }

    static int shadowLight(int baseLight, int steps) {
        double light = baseLight;
        for (int step = 0; step < steps; step++) light *= .72;
        return Math.max(16, (int)Math.round(light));
    }

    static int rgbAtLuminance(float hue,float saturation,int target){
        int full=Color.HSBtoRGB(hue,saturation,1)&0xffffff;
        if(luminance(full)<target){
            double lowMix=0,highMix=1;
            for(int iteration=0;iteration<12;iteration++){
                double mix=(lowMix+highMix)/2;int rgb=mix(full,0xffffff,mix);
                if(luminance(rgb)<target)lowMix=mix;else highMix=mix;
            }
            int lowRgb=mix(full,0xffffff,lowMix),highRgb=mix(full,0xffffff,highMix);
            return Math.abs(luminance(lowRgb)-target)<=Math.abs(luminance(highRgb)-target)?lowRgb:highRgb;
        }
        float low=0,high=1;
        for(int iteration=0;iteration<12;iteration++){
            float value=(low+high)/2;
            int rgb=Color.HSBtoRGB(hue,saturation,value)&0xffffff;
            if(luminance(rgb)<target)low=value;else high=value;
        }
        int lowRgb=Color.HSBtoRGB(hue,saturation,low)&0xffffff;
        int highRgb=Color.HSBtoRGB(hue,saturation,high)&0xffffff;
        return Math.abs(luminance(lowRgb)-target)<=Math.abs(luminance(highRgb)-target)?lowRgb:highRgb;
    }

    private static int mix(int first,int second,double amount){
        int red=(int)Math.round(((first>>>16)&255)*(1-amount)+((second>>>16)&255)*amount);
        int green=(int)Math.round(((first>>>8)&255)*(1-amount)+((second>>>8)&255)*amount);
        int blue=(int)Math.round((first&255)*(1-amount)+(second&255)*amount);
        return(red<<16)|(green<<8)|blue;
    }

    private static void capBrightSurfaceRamps(Map<Integer,Integer> ramps,int[] surfaces){
        Map<Integer,int[]> familyHistograms=new HashMap<>();
        Map<Integer,Integer> familyCounts=new HashMap<>();
        Map<Integer,Integer> surfaceCounts=new HashMap<>();
        for(int surface:surfaces)if(surface!=0)surfaceCounts.merge(surface,1,Integer::sum);
        for(Map.Entry<Integer,Integer> entry:surfaceCounts.entrySet()){
            int family=surfaceFamily(entry.getKey());
            familyHistograms.computeIfAbsent(family,ignored->new int[LIGHTNESS_RAMP.length])
                [ramps.get(entry.getKey())]+=entry.getValue();
            familyCounts.merge(family,entry.getValue(),Integer::sum);
        }
        Map<Integer,Integer> ceilings=new HashMap<>();
        for(Map.Entry<Integer,int[]> entry:familyHistograms.entrySet()){
            int target=(familyCounts.get(entry.getKey())*3+3)/4,accumulated=0,index=0;
            for(;index<entry.getValue().length-1;index++){accumulated+=entry.getValue()[index];if(accumulated>=target)break;}
            ceilings.put(entry.getKey(),index);
        }
        for(Map.Entry<Integer,Integer> entry:ramps.entrySet())
            entry.setValue(Math.min(entry.getValue(),ceilings.get(surfaceFamily(entry.getKey()))));
    }

    private static int surfaceFamily(int surface){
        if(surface>=0x10000&&surface<0x20000)return 0x10000+((surface-0x10000)&0xff80);
        return surface;
    }

    private static double[] supportedDarkDetails(BufferedImage image, int[] surfaces) {
        int width=image.getWidth(),height=image.getHeight();double[] details=new double[surfaces.length];
        int[] values=new int[25];
        for(int y=2;y<height-2;y++)for(int x=2;x<width-2;x++){
            int index=y*width+x,surface=surfaces[index],pixel=image.getRGB(x,y);if(surface==0||(pixel>>>24)==0)continue;
            int count=0;for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++){
                int neighbor=(y+dy)*width+x+dx;if(surfaces[neighbor]==surface)values[count++]=luminance(image.getRGB(x+dx,y+dy));
            }
            if(count<8)continue;Arrays.sort(values,0,count);int median=values[count/2],current=luminance(pixel),support=0;
            if(median-current<28)continue;
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)if(dx!=0||dy!=0){int neighbor=(y+dy)*width+x+dx;if(surfaces[neighbor]==surface&&luminance(image.getRGB(x+dx,y+dy))<=median-20)support++;}
            if(support>0)details[index]=median-current>=60?2:1;
        }
        return details;
    }

    private static BufferedImage restoreDetails(BufferedImage smoothed,BufferedImage source,double[] details){
        BufferedImage output=copy(smoothed);int width=source.getWidth();
        for(int index=0;index<details.length;index++)if(details[index]>0)output.setRGB(index%width,index/width,source.getRGB(index%width,index/width));
        return output;
    }

    static double illuminationShadow(double lightDeficit) {
        if (lightDeficit < .12) return 0;
        return lightDeficit < .28 ? 1 : 2;
    }

    static double[] directionalInnerShadows(BufferedImage image, int[] surfaces, int width,
                                             int height, double lightX, double lightY) {
        if (image.getWidth() != width || image.getHeight() != height || surfaces.length != width * height)
            throw new IllegalArgumentException("directional-shadow buffers differ");
        double length = Math.sqrt(lightX * lightX + lightY * lightY);
        double[] shadow = new double[surfaces.length];
        if (length == 0) return shadow;
        double towardDarkX = -lightX / length;
        double towardDarkY = -lightY / length;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int index = y * width + x;
            if (surfaces[index] == 0 || (image.getRGB(x, y) >>> 24) == 0) continue;
            for (int dy = -1; dy <= 1 && shadow[index] == 0; dy++) for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int px = x + dx, py = y + dy;
                boolean transparent = px < 0 || py < 0 || px >= width || py >= height
                    || (image.getRGB(px, py) >>> 24) == 0;
                if (transparent && (dx * towardDarkX + dy * towardDarkY) / Math.sqrt(dx * dx + dy * dy) > .45) {
                    shadow[index] = 1;
                    break;
                }
            }
        }
        double[] previous = shadow;
        for (int distance = 1; distance <= 2; distance++) {
            double[] expanded = previous.clone();
            for (int y = 1; y < height - 1; y++) for (int x = 1; x < width - 1; x++) {
                int index = y * width + x;
                if (surfaces[index] == 0 || previous[index] > 0) continue;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int neighbor = (y + dy) * width + x + dx;
                    if (previous[neighbor] > .5 && surfaces[neighbor] == surfaces[index])
                        expanded[index] = Math.max(expanded[index], previous[neighbor] - .25);
                }
            }
            previous = expanded;
        }
        return previous;
    }

    private static Map<Integer,Double> materialLightReferences(int[] surfaces, double[] lighting) {
        Map<Integer,Double> references = new HashMap<>();
        for (int index = 0; index < surfaces.length; index++) if (surfaces[index] != 0) {
            references.merge(surfaces[index], lighting[index], Math::max);
        }
        return references;
    }

    static double[] depthOcclusionShadows(int[] surfaces, int[] facets, double[] depth,
                                          int width, int height) {
        if (surfaces.length != width * height || facets.length != surfaces.length
            || depth.length != surfaces.length) throw new IllegalArgumentException("occlusion buffers differ");
        double minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < surfaces.length; index++) if (surfaces[index] != 0) {
            minimum = Math.min(minimum, depth[index]);
            maximum = Math.max(maximum, depth[index]);
        }
        boolean[] boundary = new boolean[surfaces.length];
        double[] shadow = new double[surfaces.length];
        if (!Double.isFinite(minimum)) return shadow;
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
                    shadow[index] = 2;
                    break;
                }
            }
        }
        double[] previous = shadow;
        for (int distance = 1; distance <= 3; distance++) {
            double[] expanded = previous.clone();
            for (int y = 1; y < height - 1; y++) for (int x = 1; x < width - 1; x++) {
                int index = y * width + x;
                if (surfaces[index] == 0 || shadow[index] > 0) continue;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int neighbor = (y + dy) * width + x + dx;
                    if (previous[neighbor] > .5 && facets[neighbor] == facets[index]
                        && Math.abs(depth[neighbor] - depth[index]) <= threshold) {
                        expanded[index] = Math.max(expanded[index], previous[neighbor] - .5);
                    }
                }
            }
            previous = expanded;
        }
        return previous;
    }

    static int ditheredShadowSteps(double shadow, int x, int y) {
        if (shadow <= 0) return 0;
        int solid = (int) Math.floor(shadow);
        double fraction = shadow - solid;
        if (fraction > .5) return solid + 1;
        double threshold = (SHADOW_DITHER[y & 3][x & 3] + .5) / 16.0;
        return solid + (fraction >= threshold ? 1 : 0);
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

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        output.setRGB(0, 0, source.getWidth(), source.getHeight(),
            source.getRGB(0, 0, source.getWidth(), source.getHeight(), null, 0, source.getWidth()),
            0, source.getWidth());
        return output;
    }
}
