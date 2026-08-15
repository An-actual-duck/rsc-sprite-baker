package com.spoiledmilk.spritebaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Machine-local recent-project associations; never embedded into portable project files. */
public final class DesktopPreferences {
    private static final int MAX_RECENT=10;
    public List<RecentProject> recentProjects=new ArrayList<>();
    public String lastCacheDirectory;
    public String lastExportDirectory;
    public String lastProjectDirectory;
    public static Path defaultFile(){return Path.of(System.getProperty("user.home"),".rsc-sprite-baker","preferences.json");}
    public static DesktopPreferences load(Path path){if(!Files.isRegularFile(path))return new DesktopPreferences();try(Reader reader=Files.newBufferedReader(path)){DesktopPreferences value=gson().fromJson(reader,DesktopPreferences.class);if(value==null)value=new DesktopPreferences();if(value.recentProjects==null)value.recentProjects=new ArrayList<>();return value;}catch(Exception ignored){return new DesktopPreferences();}}
    public void remember(DesktopSession session){String project=session.projectFile.toAbsolutePath().normalize().toString();recentProjects.removeIf(item->project.equals(item.projectFile));recentProjects.add(0,new RecentProject(project,session.cacheDirectory.toString(),session.exportDirectory.toString(),session.project.npcId));if(recentProjects.size()>MAX_RECENT)recentProjects=new ArrayList<>(recentProjects.subList(0,MAX_RECENT));lastCacheDirectory=session.cacheDirectory.toString();lastExportDirectory=session.exportDirectory.toString();Path parent=session.projectFile.toAbsolutePath().normalize().getParent();lastProjectDirectory=parent==null?null:parent.toString();}
    Path lastCache(){return safePath(lastCacheDirectory);}
    Path lastExport(){return safePath(lastExportDirectory);}
    Path lastProjectDirectory(){return safePath(lastProjectDirectory);}
    public void save(Path path)throws IOException{Path parent=path.toAbsolutePath().getParent();if(parent!=null)Files.createDirectories(parent);try(Writer writer=Files.newBufferedWriter(path)){gson().toJson(this,writer);writer.write(System.lineSeparator());}}
    private static Gson gson(){return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();}
    private static Path safePath(String value){if(value==null||value.isBlank())return null;try{return Path.of(value).toAbsolutePath().normalize();}catch(RuntimeException ignored){return null;}}
    public static final class RecentProject{public String projectFile,cacheDirectory,exportDirectory;public int npcId;public RecentProject(){}RecentProject(String project,String cache,String export,int npc){projectFile=project;cacheDirectory=cache;exportDirectory=export;npcId=npc;}public String toString(){return Path.of(projectFile).getFileName()+" — NPC "+npcId;}}
}
