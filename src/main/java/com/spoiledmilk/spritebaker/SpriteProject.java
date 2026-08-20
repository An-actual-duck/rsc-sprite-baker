package com.spoiledmilk.spritebaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SpriteProject {
    public int schemaVersion=3;
    public int npcId;
    public int standingSequenceId=-1, walkingSequenceId=-1, combatSequenceId=-1;
    public boolean tweening=true;
    public boolean mirroredPreview;
    public VisualSettings visual=new VisualSettings();
    public TargetSheet sheet=new TargetSheet();

    public static SpriteProject load(Path path) throws IOException {
        try(Reader reader=Files.newBufferedReader(path)){
            SpriteProject project=gson().fromJson(reader,SpriteProject.class);
            if(project.visual==null)project.visual=new VisualSettings();
            if(project.sheet==null)project.sheet=new TargetSheet();
            project.schemaVersion=3;
            return project;
        }
    }
    public void save(Path path) throws IOException {
        Path parent=path.toAbsolutePath().getParent(); if(parent!=null)Files.createDirectories(parent);
        try(Writer writer=Files.newBufferedWriter(path)){gson().toJson(this,writer);writer.write(System.lineSeparator());}
    }
    public SpriteProject copy(){return gson().fromJson(gson().toJson(this),SpriteProject.class);}
    private static Gson gson(){return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();}
}
