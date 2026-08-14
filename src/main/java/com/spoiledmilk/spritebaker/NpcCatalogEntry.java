package com.spoiledmilk.spritebaker;

import java.util.Arrays;

public final class NpcCatalogEntry {
    public final int id;public final String name;public final int[] modelIds;public final int standingAnimation,walkingAnimation,renderAnimation;
    NpcCatalogEntry(NpcDefinition530 npc){id=npc.id;name=npc.name;modelIds=npc.modelIds.clone();standingAnimation=npc.standingAnimation;walkingAnimation=npc.walkingAnimation;renderAnimation=npc.renderAnimation;}
    public String toString(){return id+" — "+name+" "+Arrays.toString(modelIds);}
}
