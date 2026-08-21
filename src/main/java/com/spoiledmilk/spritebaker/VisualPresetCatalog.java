package com.spoiledmilk.spritebaker;

import java.util.LinkedHashSet;
import java.util.Set;

/** Editor-facing preset catalog that keeps legacy saved names loadable without advertising them. */
public final class VisualPresetCatalog {
    public static final String ORIGINAL="Original colors";
    public static final String MATERIAL="RSC material";
    public static final String CUSTOM="Custom";
    private static final Set<String> APPLICABLE=Set.of(ORIGINAL,MATERIAL,"Unmodified studio","RSC crisp","RSC restrained","RSC coarse");

    private VisualPresetCatalog(){ }

    public static String[] editorChoices(String savedPreset){
        LinkedHashSet<String> choices=new LinkedHashSet<>();choices.add(ORIGINAL);choices.add(MATERIAL);
        if(savedPreset!=null&&!savedPreset.isBlank()&&!ORIGINAL.equals(savedPreset)&&!MATERIAL.equals(savedPreset)&&!CUSTOM.equals(savedPreset))choices.add(savedPreset);
        choices.add(CUSTOM);return choices.toArray(new String[0]);
    }

    public static boolean isApplicable(String preset){return APPLICABLE.contains(preset);}

    public static String displayName(String preset){
        if(ORIGINAL.equals(preset))return "Original";if(MATERIAL.equals(preset))return "Material";if(CUSTOM.equals(preset))return CUSTOM;
        return preset+" (legacy project preset)";
    }
}
