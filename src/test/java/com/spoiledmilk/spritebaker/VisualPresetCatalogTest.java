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
}
