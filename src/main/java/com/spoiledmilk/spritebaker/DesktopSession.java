package com.spoiledmilk.spritebaker;

import java.nio.file.Path;

/** Paths selected by the desktop shell. Cache and export paths stay outside the portable project. */
public final class DesktopSession {
    public final Path cacheDirectory;
    public Path projectFile;
    public Path exportDirectory;
    public final SpriteProject project;
    public boolean dirty;

    DesktopSession(Path cacheDirectory,Path projectFile,Path exportDirectory,SpriteProject project){
        this.cacheDirectory=cacheDirectory;this.projectFile=projectFile;this.exportDirectory=exportDirectory;this.project=project;
    }
}
