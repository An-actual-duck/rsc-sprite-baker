package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class VisualSettingsTest {
    @Test void zeroConfigurationDefaultsPreserveOriginalColors(){VisualSettings settings=new VisualSettings();assertEquals("Original colors",settings.preset);assertEquals(PaletteReducer.UNMODIFIED,settings.palette);assertEquals(PaletteReducer.NO_DITHER,settings.dithering);}
    @Test void presetsAreCompleteAndValidated(){VisualSettings settings=new VisualSettings();settings.applyPreset("RSC coarse");assertEquals(PaletteReducer.RSC_64,settings.palette);assertEquals(PaletteReducer.ORDERED_4X4,settings.dithering);assertDoesNotThrow(settings::validate);settings.applyPreset("Unmodified studio");assertEquals(PaletteReducer.UNMODIFIED,settings.palette);assertEquals(PaletteReducer.NO_DITHER,settings.dithering);}
    @Test void lightDirectionIsNormalized(){VisualSettings settings=new VisualSettings();double[] light=settings.lightDirection();assertEquals(1,Math.sqrt(light[0]*light[0]+light[1]*light[1]+light[2]*light[2]),1e-12);}
    @Test void rejectsUnsafeDimensions(){VisualSettings settings=new VisualSettings();settings.cellWidth=8;assertThrows(IllegalArgumentException.class,settings::validate);}
}
