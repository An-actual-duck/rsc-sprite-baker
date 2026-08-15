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
    public static DesktopSession transientSession(Path cache,Path exportDirectory,int npcId)throws IOException{
        if(npcId<0)throw new IOException("NPC ID must be zero or greater");
        SpriteProject project=new SpriteProject();project.npcId=npcId;
        return DesktopSession.transientDesktop(validateCache(cache),validateExport(exportDirectory),project);
    }
    public static Path validateCache(Path path)throws IOException{
        Path directory=path.toRealPath();if(!Files.isDirectory(directory))throw new IOException("cache path is not a directory: "+directory);
        if(!Files.isRegularFile(directory.resolve("main_file_cache.dat2"))||!Files.isRegularFile(directory.resolve("main_file_cache.idx255")))throw new IOException("not a JS5 cache directory (dat2/idx255 missing): "+directory);
        return directory;
    }
    private static Path validateProjectDestination(Path path)throws IOException{Path normalized=path.toAbsolutePath().normalize();Path parent=normalized.getParent();if(parent==null)throw new IOException("project needs a parent directory");validateCreatableDirectory(parent,"Project location");Files.createDirectories(parent);if(Files.isDirectory(normalized))throw new IOException("project path is a directory: "+normalized);return normalized;}
    private static Path validateExport(Path path)throws IOException{Path normalized=path.toAbsolutePath().normalize();Files.createDirectories(normalized);if(!Files.isDirectory(normalized))throw new IOException("export path is not a directory: "+normalized);return normalized;}
    static Path validateCreatableDirectory(Path path,String label)throws IOException{
        Path normalized=path.toAbsolutePath().normalize();
        if(Files.exists(normalized)){
            if(!Files.isDirectory(normalized))throw new IOException(label+" must be a directory.");
            if(!Files.isWritable(normalized))throw new IOException(label+" is not writable.");
            return normalized;
        }
        Path ancestor=normalized.getParent();
        while(ancestor!=null&&!Files.exists(ancestor))ancestor=ancestor.getParent();
        if(ancestor==null||!Files.isDirectory(ancestor)||!Files.isWritable(ancestor))throw new IOException(label+" cannot be created here.");
        return normalized;
    }
}
