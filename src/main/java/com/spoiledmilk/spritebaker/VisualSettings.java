package com.spoiledmilk.spritebaker;

/** Persisted Phase-3 camera, framing, lighting and color controls. */
public final class VisualSettings {
    public int cellWidth = 128;
    public int cellHeight = 128;
    public int supersample = 3;
    public int padding = 8;
    public double modelScale = 0.90;
    public double pitchDegrees = 12.0;
    public double yawOffsetDegrees = 0.0;
    public double verticalOffsetPixels = 0.0;
    public double ambient = 0.52;
    public double diffuse = 0.40;
    public double lightAzimuthDegrees = -125.0;
    public double lightElevationDegrees = 45.0;
    public String palette = PaletteReducer.UNMODIFIED;
    public String dithering = PaletteReducer.NO_DITHER;
    public double ditherStrength = 0.30;
    public String preset = "Original colors";

    public void validate() {
        if (cellWidth < 16 || cellWidth > 512 || cellHeight < 16 || cellHeight > 512) {
            throw new IllegalArgumentException("cell dimensions must be between 16 and 512 pixels");
        }
        if (supersample < 1 || supersample > 8) throw new IllegalArgumentException("supersample must be 1..8");
        if ((long) cellWidth * supersample > 4096 || (long) cellHeight * supersample > 4096) throw new IllegalArgumentException("internal cell dimensions must not exceed 4096 pixels");
        if (padding < 0 || padding * 2 >= Math.min(cellWidth, cellHeight)) throw new IllegalArgumentException("padding does not fit cell");
        if (modelScale <= 0 || modelScale > 4) throw new IllegalArgumentException("model scale must be in (0, 4]");
        if (pitchDegrees < -89 || pitchDegrees > 89) throw new IllegalArgumentException("pitch must be -89..89 degrees");
        if (verticalOffsetPixels < -512 || verticalOffsetPixels > 512) throw new IllegalArgumentException("vertical offset must be -512..512 pixels");
        if (ambient < 0 || ambient > 1 || diffuse < 0 || diffuse > 1) throw new IllegalArgumentException("lighting must be 0..1");
        if (lightElevationDegrees < -90 || lightElevationDegrees > 90) throw new IllegalArgumentException("light elevation must be -90..90 degrees");
        if (ditherStrength < 0 || ditherStrength > 1) throw new IllegalArgumentException("dither strength must be 0..1");
        PaletteReducer.validate(palette, dithering);
    }

    public void applyPreset(String name) {
        preset = name;
        switch (name) {
            case "Original colors":
                pitchDegrees = 12; modelScale = 0.9; verticalOffsetPixels = 0;
                ambient = 0.52; diffuse = 0.40; lightAzimuthDegrees = -125; lightElevationDegrees = 45;
                palette = PaletteReducer.UNMODIFIED; dithering = PaletteReducer.NO_DITHER; break;
            case "Unmodified studio":
                pitchDegrees = 15; modelScale = 0.92; verticalOffsetPixels = 0;
                ambient = 0.45; diffuse = 0.55; lightAzimuthDegrees = -117; lightElevationDegrees = 41;
                palette = PaletteReducer.UNMODIFIED; dithering = PaletteReducer.NO_DITHER; break;
            case "RSC crisp":
                pitchDegrees = 12; modelScale = 0.9; verticalOffsetPixels = 0;
                ambient = 0.52; diffuse = 0.40; lightAzimuthDegrees = -125; lightElevationDegrees = 45;
                palette = PaletteReducer.RSC_125; dithering = PaletteReducer.NO_DITHER; break;
            case "RSC restrained":
                pitchDegrees = 12; modelScale = 0.9; verticalOffsetPixels = 0;
                ambient = 0.52; diffuse = 0.40; lightAzimuthDegrees = -125; lightElevationDegrees = 45;
                palette = PaletteReducer.RSC_125; dithering = PaletteReducer.ORDERED_4X4; ditherStrength = 0.30; break;
            case "RSC coarse":
                pitchDegrees = 10; modelScale = 0.88; verticalOffsetPixels = 0;
                ambient = 0.56; diffuse = 0.34; lightAzimuthDegrees = -130; lightElevationDegrees = 48;
                palette = PaletteReducer.RSC_64; dithering = PaletteReducer.ORDERED_4X4; ditherStrength = 0.40; break;
            default: throw new IllegalArgumentException("unknown visual preset: " + name);
        }
    }

    public double[] lightDirection() {
        double azimuth = Math.toRadians(lightAzimuthDegrees);
        double elevation = Math.toRadians(lightElevationDegrees);
        double horizontal = Math.cos(elevation);
        return new double[] {horizontal * Math.cos(azimuth), Math.sin(elevation), horizontal * Math.sin(azimuth)};
    }
}
