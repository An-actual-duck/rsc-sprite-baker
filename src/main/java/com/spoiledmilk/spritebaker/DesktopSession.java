package com.spoiledmilk.spritebaker;

import java.nio.file.Path;

/** Desktop workflow paths. Cache and export paths stay outside the portable project. */
public final class DesktopSession {
    public final Path cacheDirectory;
    public Path projectFile;
    public Path exportDirectory;
    public final SpriteProject project;
    public final boolean transientDesktop;
    public boolean dirty;

    DesktopSession(Path cacheDirectory,Path projectFile,Path exportDirectory,SpriteProject project){
        this(cacheDirectory,projectFile,exportDirectory,project,false);
    }

    private DesktopSession(Path cacheDirectory,Path projectFile,Path exportDirectory,SpriteProject project,boolean transientDesktop){
        this.cacheDirectory=cacheDirectory;this.projectFile=projectFile;this.exportDirectory=exportDirectory;this.project=project;this.transientDesktop=transientDesktop;
    }

    static DesktopSession transientDesktop(Path cacheDirectory,Path exportDirectory,SpriteProject project){
        return new DesktopSession(cacheDirectory,null,exportDirectory,project,true);
    }
}
