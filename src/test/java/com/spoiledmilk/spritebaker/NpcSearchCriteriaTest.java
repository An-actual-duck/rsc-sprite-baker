package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class NpcSearchCriteriaTest {
    @Test void metadataTagsAreDerivedFromResolvedBasAndDefinitionFields(){
        NpcDefinition530 npc=new NpcDefinition530(72);npc.name="Mountain Troll";npc.modelIds=new int[]{1,2};npc.renderAnimation=42;
        npc.recolorFrom=new short[]{3};npc.recolorTo=new short[]{4};npc.retextureFrom=new short[]{5};npc.retextureTo=new short[]{6};npc.widthScale=140;
        RenderAnimation530 bas=new RenderAnimation530(42);bas.standingAnimation=286;bas.walkingAnimation=283;
        NpcCatalogEntry entry=new NpcCatalogEntry(npc,bas);
        assertTrue(entry.tags().containsAll(Set.of(NpcSearchCriteria.Tag.AUTOMATIC_ANIMATIONS,NpcSearchCriteria.Tag.MULTIPART_MODEL,
            NpcSearchCriteria.Tag.USES_RECOLORS,NpcSearchCriteria.Tag.USES_RETEXTURES,NpcSearchCriteria.Tag.ALTERED_MODEL_SCALE)));
        assertFalse(entry.has(NpcSearchCriteria.Tag.NEEDS_MANUAL_ANIMATIONS));
        assertFalse(entry.has(NpcSearchCriteria.Tag.MORPH_INTERNAL));
    }

    @Test void absentResolvedLocomotionAndInternalDefinitionsHaveIndependentTags(){
        NpcDefinition530 npc=new NpcDefinition530(9);npc.name="Internal";npc.morphDefinition=true;
        NpcCatalogEntry entry=new NpcCatalogEntry(npc,null);
        assertTrue(entry.has(NpcSearchCriteria.Tag.NEEDS_MANUAL_ANIMATIONS));
        assertTrue(entry.has(NpcSearchCriteria.Tag.MORPH_INTERNAL));
        assertFalse(entry.has(NpcSearchCriteria.Tag.AUTOMATIC_ANIMATIONS));
    }

    @Test void allRequiresEveryTagAndTextTermsAreAlwaysAnded(){
        NpcCatalogEntry entry=entry();
        NpcSearchCriteria matching=new NpcSearchCriteria("king dragon",NpcSearchCriteria.MatchMode.ALL,
            Set.of(NpcSearchCriteria.Tag.AUTOMATIC_ANIMATIONS,NpcSearchCriteria.Tag.MULTIPART_MODEL));
        NpcSearchCriteria missingTag=new NpcSearchCriteria("king dragon",NpcSearchCriteria.MatchMode.ALL,
            Set.of(NpcSearchCriteria.Tag.AUTOMATIC_ANIMATIONS,NpcSearchCriteria.Tag.USES_RETEXTURES));
        NpcSearchCriteria missingText=new NpcSearchCriteria("red dragon",NpcSearchCriteria.MatchMode.ALL,
            Set.of(NpcSearchCriteria.Tag.AUTOMATIC_ANIMATIONS));
        assertTrue(matching.matches(entry));assertFalse(missingTag.matches(entry));assertFalse(missingText.matches(entry));
    }

    @Test void anyRequiresOneTagButStillRequiresEveryTextTerm(){
        NpcCatalogEntry entry=entry();
        assertTrue(new NpcSearchCriteria("black dragon",NpcSearchCriteria.MatchMode.ANY,
            Set.of(NpcSearchCriteria.Tag.USES_RETEXTURES,NpcSearchCriteria.Tag.MULTIPART_MODEL)).matches(entry));
        assertFalse(new NpcSearchCriteria("red dragon",NpcSearchCriteria.MatchMode.ANY,
            Set.of(NpcSearchCriteria.Tag.MULTIPART_MODEL)).matches(entry));
    }

    @Test void blankTextWithTagsFiltersWhileCompletelyBlankCriteriaStayEmpty(){
        NpcCatalogEntry entry=entry();
        NpcSearchCriteria tagsOnly=new NpcSearchCriteria("  ",NpcSearchCriteria.MatchMode.ALL,Set.of(NpcSearchCriteria.Tag.USES_RECOLORS));
        assertFalse(tagsOnly.isEmpty());assertTrue(tagsOnly.matches(entry));
        assertTrue(new NpcSearchCriteria("",NpcSearchCriteria.MatchMode.ALL,Set.of()).isEmpty());
    }

    @Test void anEntireNumericQueryIsAnExactIdRatherThanANameFragment(){
        NpcCatalogEntry entry=entry();
        assertEquals(50,new NpcSearchCriteria("50",NpcSearchCriteria.MatchMode.ALL,Set.of()).exactId());
        assertTrue(new NpcSearchCriteria("50",NpcSearchCriteria.MatchMode.ALL,Set.of()).matches(entry));
        assertFalse(new NpcSearchCriteria("5",NpcSearchCriteria.MatchMode.ALL,Set.of()).matches(entry));
    }

    private static NpcCatalogEntry entry(){
        NpcDefinition530 npc=new NpcDefinition530(50);npc.name="King Black Dragon";npc.modelIds=new int[]{1,2};npc.standingAnimation=90;npc.walkingAnimation=79;npc.recolorFrom=new short[]{1};npc.recolorTo=new short[]{2};return new NpcCatalogEntry(npc,null);
    }
}
