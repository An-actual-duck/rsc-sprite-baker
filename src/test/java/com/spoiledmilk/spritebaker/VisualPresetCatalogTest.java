package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class VisualPresetCatalogTest {
    @Test void normalEditorOnlyAdvertisesPrimaryLooksAndCustom(){
        assertArrayEquals(new String[]{VisualPresetCatalog.ORIGINAL,VisualPresetCatalog.MATERIAL,VisualPresetCatalog.CUSTOM},VisualPresetCatalog.editorChoices(VisualPresetCatalog.ORIGINAL));
        assertEquals("Original",VisualPresetCatalog.displayName(VisualPresetCatalog.ORIGINAL));assertEquals("Material",VisualPresetCatalog.displayName(VisualPresetCatalog.MATERIAL));
    }

    @Test void savedLegacyPresetRemainsVisibleAndApplicableOnlyForThatProject(){
        assertArrayEquals(new String[]{VisualPresetCatalog.ORIGINAL,VisualPresetCatalog.MATERIAL,"RSC coarse",VisualPresetCatalog.CUSTOM},VisualPresetCatalog.editorChoices("RSC coarse"));
        assertTrue(VisualPresetCatalog.isApplicable("RSC coarse"));assertEquals("RSC coarse (legacy project preset)",VisualPresetCatalog.displayName("RSC coarse"));
        assertArrayEquals(new String[]{VisualPresetCatalog.ORIGINAL,VisualPresetCatalog.MATERIAL,"Future preset",VisualPresetCatalog.CUSTOM},VisualPresetCatalog.editorChoices("Future preset"));
        assertFalse(VisualPresetCatalog.isApplicable("Future preset"));
    }

    @Test void userProfilesAppearBetweenBuiltInsAndCustomWithoutChangingBuiltIns(){
        assertArrayEquals(new String[]{VisualPresetCatalog.ORIGINAL,VisualPresetCatalog.MATERIAL,"Warm outline","Soft material",VisualPresetCatalog.CUSTOM},VisualPresetCatalog.editorChoices(VisualPresetCatalog.CUSTOM,java.util.List.of("Warm outline","Soft material")));
        assertEquals("Warm outline",VisualPresetCatalog.displayName("Warm outline",java.util.List.of("Warm outline")));
        assertEquals("Future preset (legacy project preset)",VisualPresetCatalog.displayName("Future preset",java.util.List.of("Warm outline")));
    }
}
