package com.spoiledmilk.spritebaker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopPreferencesTest {
    @Test void recentProjectsAreBoundedDeduplicatedAndMachineLocal(@TempDir Path dir)throws Exception{DesktopPreferences p=new DesktopPreferences();for(int i=0;i<12;i++)p.remember(session(dir,i));assertEquals(10,p.recentProjects.size());DesktopSession latest=session(dir,11);p.remember(latest);assertEquals(10,p.recentProjects.size());assertEquals("p11.json",Path.of(p.recentProjects.get(0).projectFile).getFileName().toString());Path file=dir.resolve("settings/preferences.json");p.save(file);DesktopPreferences loaded=DesktopPreferences.load(file);assertEquals(10,loaded.recentProjects.size());assertEquals(11,loaded.recentProjects.get(0).npcId);}
    @Test void malformedPreferencesRecoverAsEmpty(@TempDir Path dir)throws Exception{Path file=dir.resolve("bad.json");Files.writeString(file,"not-json");assertTrue(DesktopPreferences.load(file).recentProjects.isEmpty());}
    private static DesktopSession session(Path dir,int id)throws Exception{Path cache=dir.resolve("c"+id);Files.createDirectories(cache);SpriteProject project=new SpriteProject();project.npcId=id;return new DesktopSession(cache,dir.resolve("p"+id+".json"),dir.resolve("e"+id),project);}
}
