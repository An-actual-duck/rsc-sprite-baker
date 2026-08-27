package com.spoiledmilk.spritebaker;

import java.nio.file.Path;

/** Packaging-only generator and archive-inspection validator. */
public final class CombatRoleManifestMain {
    private CombatRoleManifestMain(){}
    public static void main(String[] args)throws Exception{
        if(args.length==2&&"--validate".equals(args[0])){CombatRoleManifest manifest=CombatRoleManifest.load(Path.of(args[1]));System.out.println("Combat-role manifest valid: "+manifest.entries.size()+" NPCs; entries SHA-256 "+manifest.entriesSha256);return;}
        if(args.length==6&&"--source".equals(args[0])&&"--revision".equals(args[2])&&"--output".equals(args[4])){CombatRoleManifest manifest=CombatRoleManifest.derive(Path.of(args[1]),args[3]);manifest.write(Path.of(args[5]));System.out.println("Combat-role manifest generated: "+manifest.entries.size()+" NPCs; entries SHA-256 "+manifest.entriesSha256);return;}
        throw new IllegalArgumentException("usage: --source NPC_CONFIG.json --revision GIT_COMMIT --output MANIFEST.json | --validate MANIFEST.json");
    }
}
