package com.spoiledmilk.spritebaker;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Editor-facing preset catalog that keeps legacy saved names loadable without advertising them. */
public final class VisualPresetCatalog {
    public static final String ORIGINAL="Original colors";
    public static final String MATERIAL="RSC material";
    public static final String CUSTOM="Custom";
    private static final Set<String> APPLICABLE=Set.of(ORIGINAL,MATERIAL,"Unmodified studio","RSC crisp","RSC restrained","RSC coarse");

    private VisualPresetCatalog(){ }

    public static String[] editorChoices(String savedPreset){
        return editorChoices(savedPreset,List.of());
    }

    public static String[] editorChoices(String savedPreset,List<String> userProfiles){
        LinkedHashSet<String> choices=new LinkedHashSet<>();choices.add(ORIGINAL);choices.add(MATERIAL);
        if(userProfiles!=null)for(String profile:userProfiles)if(profile!=null&&!profile.isBlank()&&!isReservedName(profile))choices.add(profile);
        if(savedPreset!=null&&!savedPreset.isBlank()&&!ORIGINAL.equals(savedPreset)&&!MATERIAL.equals(savedPreset)&&!CUSTOM.equals(savedPreset))choices.add(savedPreset);
        choices.add(CUSTOM);return choices.toArray(new String[0]);
    }

    public static boolean isApplicable(String preset){return APPLICABLE.contains(preset);}

    public static boolean isReservedName(String name){
        if(name==null)return false;String key=name.trim().toLowerCase(Locale.ROOT);
        if(CUSTOM.toLowerCase(Locale.ROOT).equals(key)||"original".equals(key)||"material".equals(key))return true;
        for(String preset:APPLICABLE)if(preset.toLowerCase(Locale.ROOT).equals(key))return true;
        return false;
    }

    public static String displayName(String preset){
        if(ORIGINAL.equals(preset))return "Original";if(MATERIAL.equals(preset))return "Material";if(CUSTOM.equals(preset))return CUSTOM;
        return preset+" (legacy project preset)";
    }

    public static String displayName(String preset,List<String> userProfiles){
        if(userProfiles!=null)for(String profile:userProfiles)if(profile.equals(preset))return profile;
        return displayName(preset);
    }
}
