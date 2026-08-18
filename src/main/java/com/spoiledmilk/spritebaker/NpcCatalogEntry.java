package com.spoiledmilk.spritebaker;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class NpcCatalogEntry {
    public final int id;public final String name;public final int[] modelIds;public final int standingAnimation,walkingAnimation,renderAnimation;private final Set<NpcSearchCriteria.Tag> tags;private String compatibility;
    NpcCatalogEntry(NpcDefinition530 npc){this(npc,null);}
    NpcCatalogEntry(NpcDefinition530 npc,RenderAnimation530 bas){
        id=npc.id;name=npc.name;modelIds=npc.modelIds.clone();standingAnimation=npc.standingAnimation;walkingAnimation=npc.walkingAnimation;renderAnimation=npc.renderAnimation;
        int[] automatic=AnimationDiscovery.knownSequences(npc,bas);EnumSet<NpcSearchCriteria.Tag> found=EnumSet.noneOf(NpcSearchCriteria.Tag.class);
        if(automatic[0]>=0&&automatic[1]>=0)found.add(NpcSearchCriteria.Tag.AUTOMATIC_ANIMATIONS);else found.add(NpcSearchCriteria.Tag.NEEDS_MANUAL_ANIMATIONS);
        if(modelIds.length>1)found.add(NpcSearchCriteria.Tag.MULTIPART_MODEL);
        if(npc.recolorFrom.length>0)found.add(NpcSearchCriteria.Tag.USES_RECOLORS);
        if(npc.retextureFrom.length>0)found.add(NpcSearchCriteria.Tag.USES_RETEXTURES);
        if(npc.widthScale!=128||npc.heightScale!=128)found.add(NpcSearchCriteria.Tag.ALTERED_MODEL_SCALE);
        if(npc.morphDefinition||modelIds.length==0)found.add(NpcSearchCriteria.Tag.MORPH_INTERNAL);
        tags=Collections.unmodifiableSet(found);
    }
    public boolean has(NpcSearchCriteria.Tag tag){return tags.contains(tag);}
    public Set<NpcSearchCriteria.Tag> tags(){return tags;}
    void compatibility(String value){compatibility=value;}
    public String toString(){return id+" — "+name+" "+Arrays.toString(modelIds)+(tags.isEmpty()?"":" — "+tags)+(compatibility==null?"":" — "+compatibility);}
}
