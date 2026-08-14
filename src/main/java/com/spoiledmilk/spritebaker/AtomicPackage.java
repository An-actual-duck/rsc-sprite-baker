package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

/** Stages and atomically publishes only directories owned by this package contract. */
final class AtomicPackage implements AutoCloseable {
    static final String MARKER=".sprite-baker-handoff-v1";
    final Path destination,staging;private Path backup;private boolean published;
    AtomicPackage(Path destination)throws IOException{
        this.destination=destination.toAbsolutePath().normalize();Path parent=this.destination.getParent();if(parent==null)throw new IOException("output directory needs a parent");Files.createDirectories(parent);
        if(Files.isSymbolicLink(this.destination))throw new IOException("output directory must not be a symbolic link: "+this.destination);
        if(Files.exists(this.destination)&&(!Files.isDirectory(this.destination)||!Files.isRegularFile(this.destination.resolve(MARKER))))throw new IOException("refusing to replace output not owned by Sprite Baker: "+this.destination);
        staging=Files.createTempDirectory(parent,"."+this.destination.getFileName()+".staging-");Files.writeString(staging.resolve(MARKER),"schemaVersion=1\n");
    }
    void publish()throws IOException{
        if(Files.exists(destination)){backup=destination.resolveSibling("."+destination.getFileName()+".backup-"+Long.toUnsignedString(System.nanoTime()));atomicMove(destination,backup);}
        try{atomicMove(staging,destination);published=true;}catch(IOException failure){if(backup!=null&&Files.exists(backup))try{atomicMove(backup,destination);}catch(IOException rollback){failure.addSuppressed(rollback);}throw failure;}
        if(backup!=null)deleteTree(backup);
    }
    static void atomicWrite(Path path,String text)throws IOException{Path absolute=path.toAbsolutePath().normalize(),parent=absolute.getParent();if(parent==null)throw new IOException("report path needs a parent");Files.createDirectories(parent);Path temporary=Files.createTempFile(parent,"."+absolute.getFileName()+".",".tmp");try{Files.writeString(temporary,text);try{Files.move(temporary,absolute,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){throw new IOException("filesystem does not support atomic report publication: "+absolute,e);}}finally{Files.deleteIfExists(temporary);}}
    private static void atomicMove(Path from,Path to)throws IOException{try{Files.move(from,to,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){throw new IOException("filesystem does not support atomic package publication from "+from+" to "+to,e);}}
    static void deleteTree(Path root)throws IOException{if(!Files.exists(root))return;try(java.util.stream.Stream<Path> paths=Files.walk(root)){Path[] ordered=paths.sorted(Comparator.reverseOrder()).toArray(Path[]::new);for(Path path:ordered)Files.deleteIfExists(path);}}
    @Override public void close()throws IOException{if(!published)deleteTree(staging);}
}
