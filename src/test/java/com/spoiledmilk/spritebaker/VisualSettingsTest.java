package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class VisualSettingsTest {
    @Test void zeroConfigurationDefaultsPreserveOriginalColors(){VisualSettings settings=new VisualSettings();assertEquals("Original colors",settings.preset);assertEquals(PaletteReducer.UNMODIFIED,settings.palette);assertEquals(PaletteReducer.NO_DITHER,settings.dithering);}
    @Test void presetsAreCompleteAndValidated(){VisualSettings settings=new VisualSettings();settings.applyPreset("RSC coarse");assertEquals(PaletteReducer.RSC_64,settings.palette);assertEquals(PaletteReducer.ORDERED_4X4,settings.dithering);assertDoesNotThrow(settings::validate);settings.applyPreset("RSC material");assertEquals(MaterialStylizer.RSC_RAMPS,settings.materialStyle);assertEquals(PaletteReducer.UNMODIFIED,settings.palette);settings.applyPreset("Unmodified studio");assertEquals(MaterialStylizer.NONE,settings.materialStyle);assertEquals(PaletteReducer.UNMODIFIED,settings.palette);assertEquals(PaletteReducer.NO_DITHER,settings.dithering);assertEquals(.30,settings.ditherStrength);settings.ditherStrength=.95;settings.applyPreset(VisualPresetCatalog.ORIGINAL);assertEquals(.30,settings.ditherStrength);}
    @Test void lightDirectionIsNormalized(){VisualSettings settings=new VisualSettings();double[] light=settings.lightDirection();assertEquals(1,Math.sqrt(light[0]*light[0]+light[1]*light[1]+light[2]*light[2]),1e-12);}
    @Test void rejectsUnsafeDimensionsAndEdgeMargins(){VisualSettings settings=new VisualSettings();settings.cellWidth=8;assertThrows(IllegalArgumentException.class,settings::validate);settings.cellWidth=128;settings.padding=64;assertThrows(IllegalArgumentException.class,settings::validate);}
    @Test void simpleDitheringTogglePreservesSerializedRendererValues(){VisualSettings settings=new VisualSettings();settings.palette=PaletteReducer.RSC_125;assertFalse(settings.ditheringEnabled());assertFalse(settings.ditheringStrengthRelevant());settings.setDitheringEnabled(true);assertTrue(settings.ditheringEnabled());assertTrue(settings.ditheringStrengthRelevant());assertEquals(PaletteReducer.ORDERED_4X4,settings.dithering);settings.palette=PaletteReducer.UNMODIFIED;assertFalse(settings.ditheringStrengthRelevant());settings.setDitheringEnabled(false);assertEquals(PaletteReducer.NO_DITHER,settings.dithering);}
}
