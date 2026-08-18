package com.spoiledmilk.spritebaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Immutable, cache-independent NPC browser matching semantics. */
public final class NpcSearchCriteria {
    public enum MatchMode { ALL("All"), ANY("Any"); public final String label; MatchMode(String label){this.label=label;} public String toString(){return label;} }
    public enum Tag {
        AUTOMATIC_ANIMATIONS("Automatic animations available"),
        NEEDS_MANUAL_ANIMATIONS("Needs manual animation selection"),
        MULTIPART_MODEL("Multipart model"),
        USES_RECOLORS("Uses recolors"),
        USES_RETEXTURES("Uses retextures"),
        ALTERED_MODEL_SCALE("Altered model scale"),
        MORPH_INTERNAL("Morph/internal definition");
        public final String label;
        Tag(String label){this.label=label;}
        public String toString(){return label;}
    }

    public final String text;
    public final MatchMode matchMode;
    public final Set<Tag> tags;
    private final List<String> terms;
    private final Integer exactId;

    public NpcSearchCriteria(String text, MatchMode matchMode, Set<Tag> tags) {
        this.text = text == null ? "" : text.trim();
        this.matchMode = matchMode == null ? MatchMode.ALL : matchMode;
        this.tags = tags == null || tags.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(tags));
        terms = terms(this.text);
        exactId = exactId(this.text);
    }

    public boolean isEmpty(){return terms.isEmpty()&&tags.isEmpty();}
    public Integer exactId(){return exactId;}

    public boolean matches(NpcCatalogEntry entry) {
        if (exactId != null && entry.id != exactId) return false;
        if (exactId == null && !terms.isEmpty()) {
            String haystack=(entry.id+" "+entry.name).toLowerCase(Locale.ROOT);
            for(String term:terms)if(!haystack.contains(term))return false;
        }
        if(tags.isEmpty())return true;
        if(matchMode==MatchMode.ALL){for(Tag tag:tags)if(!entry.has(tag))return false;return true;}
        for(Tag tag:tags)if(entry.has(tag))return true;
        return false;
    }

    private static List<String> terms(String text){
        if(text.isBlank())return List.of();
        List<String> out=new ArrayList<>();
        for(String term:text.toLowerCase(Locale.ROOT).split("\\s+"))if(!term.isBlank())out.add(term);
        return List.copyOf(out);
    }
    private static Integer exactId(String text){try{return text.isEmpty()?null:Integer.valueOf(text);}catch(NumberFormatException ignored){return null;}}
}
