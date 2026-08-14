package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Headless, testable first-run and project lifecycle validation used by the Swing launcher. */
public final class DesktopWorkflow {
    private DesktopWorkflow(){}
    public static DesktopSession create(Path cache,Path projectFile,Path exportDirectory,int npcId)throws IOException{
        Path checkedCache=validateCache(cache);Path checkedProject=validateProjectDestination(projectFile);Path checkedExport=validateExport(exportDirectory);
        SpriteProject project=new SpriteProject();project.npcId=npcId;project.save(checkedProject);
        return new DesktopSession(checkedCache,checkedProject,checkedExport,project);
    }
    public static DesktopSession open(Path cache,Path projectFile,Path exportDirectory)throws IOException{
        Path checkedCache=validateCache(cache);Path checkedProject=projectFile.toAbsolutePath().normalize();
        if(!Files.isRegularFile(checkedProject))throw new IOException("project file does not exist: "+checkedProject);
        SpriteProject project=SpriteProject.load(checkedProject);return new DesktopSession(checkedCache,checkedProject,validateExport(exportDirectory),project);
    }
    public static Path validateCache(Path path)throws IOException{
        Path directory=path.toRealPath();if(!Files.isDirectory(directory))throw new IOException("cache path is not a directory: "+directory);
        if(!Files.isRegularFile(directory.resolve("main_file_cache.dat2"))||!Files.isRegularFile(directory.resolve("main_file_cache.idx255")))throw new IOException("not a JS5 cache directory (dat2/idx255 missing): "+directory);
        return directory;
    }
    private static Path validateProjectDestination(Path path)throws IOException{Path normalized=path.toAbsolutePath().normalize();Path parent=normalized.getParent();if(parent==null)throw new IOException("project needs a parent directory");Files.createDirectories(parent);return normalized;}
    private static Path validateExport(Path path)throws IOException{Path normalized=path.toAbsolutePath().normalize();Files.createDirectories(normalized);if(!Files.isDirectory(normalized))throw new IOException("export path is not a directory: "+normalized);return normalized;}
}
