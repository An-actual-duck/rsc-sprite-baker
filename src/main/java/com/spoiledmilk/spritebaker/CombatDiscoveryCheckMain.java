package com.spoiledmilk.spritebaker;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** Packaged-layout smoke check used by the distribution inspector. */
public final class CombatDiscoveryCheckMain {
    private CombatDiscoveryCheckMain(){}
    public static void main(String[] args)throws Exception{
        if(args.length!=6||!"--cache".equals(args[0])||!"--npc".equals(args[2])||!"--require".equals(args[4]))throw new IllegalArgumentException("usage: --cache PATH --npc ID --require ID,ID,...");
        Path cache=Path.of(args[1]);int npc=Integer.parseInt(args[3]);Set<Integer> required=new LinkedHashSet<>();for(String value:args[5].split(","))required.add(Integer.parseInt(value));
        try(AnimationWorkspace workspace=new AnimationWorkspace(cache,npc)){AnimationDiscovery.CombatDiscovery discovery=AnimationDiscovery.discoverCombat(workspace);Set<Integer> actual=new LinkedHashSet<>();for(CombatCandidate candidate:discovery.candidates)actual.add(candidate.sequenceId);if(!actual.containsAll(required))throw new IllegalStateException("NPC "+npc+" missing required combat sequences "+difference(required,actual)+"; found "+actual+"; "+discovery.summary());System.out.println("Packaged combat discovery valid: NPC "+npc+" required="+required+" found="+actual+"; "+discovery.provenance);}
    }
    private static Set<Integer> difference(Set<Integer> required,Set<Integer> actual){Set<Integer> missing=new LinkedHashSet<>(required);missing.removeAll(actual);return missing;}
}
