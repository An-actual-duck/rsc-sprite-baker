package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SavedLookProfilesTest {
    @Test void saveLoadRoundTripRestoresEveryVisualValueAndSurfaceColors(@TempDir Path dir)throws Exception{
        Path file=dir.resolve("settings/look-profiles.json");VisualSettings expected=customSettings();
        SavedLookProfiles profiles=new SavedLookProfiles();profiles.saveProfile(file,"  My shaded look  ",expected);
        SavedLookProfiles loaded=SavedLookProfiles.load(file);assertEquals(java.util.List.of("My shaded look"),loaded.names());
        VisualSettings saved=loaded.settings("my SHADED look");assertNotNull(saved);assertVisualEquals(expected,saved);
        VisualSettings restored=new VisualSettings();restored.materialStyle=MaterialStylizer.NONE;restored.copyFrom(saved);
        assertVisualEquals(expected,restored);assertEquals(MaterialStylizer.RSC_RAMPS,restored.materialStyle);assertEquals("My shaded look",restored.preset);
    }

    @Test void blankReservedAndDuplicateNamesAreRejectedClearly(@TempDir Path dir)throws Exception{
        Path file=dir.resolve("looks.json");SavedLookProfiles profiles=new SavedLookProfiles();VisualSettings settings=customSettings();
        assertTrue(assertThrows(IllegalArgumentException.class,()->profiles.saveProfile(file,"   ",settings)).getMessage().contains("profile name"));
        assertTrue(assertThrows(IllegalArgumentException.class,()->profiles.saveProfile(file,"material",settings)).getMessage().contains("reserved"));
        profiles.saveProfile(file,"Favorite",settings);
        assertTrue(assertThrows(IllegalArgumentException.class,()->profiles.saveProfile(file," favorite ",settings)).getMessage().contains("already exists"));
        assertEquals(java.util.List.of("Favorite"),profiles.names());
    }

    @Test void missingMalformedAndInvalidSettingsRecoverWithoutLosingValidProfiles(@TempDir Path dir)throws Exception{
        Path missing=dir.resolve("missing.json");assertTrue(SavedLookProfiles.load(missing).names().isEmpty());
        Path missingList=dir.resolve("missing-list.json");Files.writeString(missingList,"{\"schemaVersion\":1}");assertTrue(SavedLookProfiles.load(missingList).names().isEmpty());
        Path malformed=dir.resolve("malformed.json");Files.writeString(malformed,"not-json");assertTrue(SavedLookProfiles.load(malformed).names().isEmpty());
        Path mixed=dir.resolve("mixed.json");Files.writeString(mixed,"{\"profiles\":[{\"name\":\"Missing settings\"},{\"name\":\"Broken\",\"settings\":{\"cellWidth\":2}},{\"name\":\"Compatible\",\"settings\":{}}]}");
        SavedLookProfiles loaded=SavedLookProfiles.load(mixed);assertEquals(java.util.List.of("Compatible"),loaded.names());
        assertEquals(MaterialStylizer.NONE,loaded.settings("Compatible").materialStyle);
    }

    private static VisualSettings customSettings(){VisualSettings v=new VisualSettings();v.cellWidth=192;v.cellHeight=160;v.supersample=4;v.padding=11;v.modelScale=1.35;v.pitchDegrees=22;v.yawOffsetDegrees=-15;v.verticalOffsetPixels=7;v.ambient=.34;v.diffuse=.66;v.lightAzimuthDegrees=85;v.lightElevationDegrees=-20;v.colorVariation=.45;v.textureDetail=.6;v.colorIntensity=1.2;v.shadowDepth=.75;v.palette=PaletteReducer.RSC_64;v.dithering=PaletteReducer.ORDERED_4X4;v.ditherStrength=.55;v.materialStyle=MaterialStylizer.RSC_RAMPS;v.preset=VisualPresetCatalog.CUSTOM;return v;}
    private static void assertVisualEquals(VisualSettings expected,VisualSettings actual){assertEquals(expected.cellWidth,actual.cellWidth);assertEquals(expected.cellHeight,actual.cellHeight);assertEquals(expected.supersample,actual.supersample);assertEquals(expected.padding,actual.padding);assertEquals(expected.modelScale,actual.modelScale);assertEquals(expected.pitchDegrees,actual.pitchDegrees);assertEquals(expected.yawOffsetDegrees,actual.yawOffsetDegrees);assertEquals(expected.verticalOffsetPixels,actual.verticalOffsetPixels);assertEquals(expected.ambient,actual.ambient);assertEquals(expected.diffuse,actual.diffuse);assertEquals(expected.lightAzimuthDegrees,actual.lightAzimuthDegrees);assertEquals(expected.lightElevationDegrees,actual.lightElevationDegrees);assertEquals(expected.colorVariation,actual.colorVariation);assertEquals(expected.textureDetail,actual.textureDetail);assertEquals(expected.colorIntensity,actual.colorIntensity);assertEquals(expected.shadowDepth,actual.shadowDepth);assertEquals(expected.palette,actual.palette);assertEquals(expected.dithering,actual.dithering);assertEquals(expected.ditherStrength,actual.ditherStrength);assertEquals(expected.materialStyle,actual.materialStyle);}
}
