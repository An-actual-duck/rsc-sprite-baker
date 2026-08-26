package com.spoiledmilk.spritebaker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Durable machine-local visual profiles, kept separate from portable project data. */
public final class SavedLookProfiles {
    public int schemaVersion=1;
    public List<Profile> profiles=new ArrayList<>();

    public static Path defaultFile(){return Path.of(System.getProperty("user.home"),".rsc-sprite-baker","look-profiles.json");}

    public static SavedLookProfiles load(Path path){
        if(!Files.isRegularFile(path))return new SavedLookProfiles();
        try(Reader reader=Files.newBufferedReader(path)){
            SavedLookProfiles loaded=gson().fromJson(reader,SavedLookProfiles.class);
            if(loaded==null)return new SavedLookProfiles();
            loaded.normalize();
            return loaded;
        }catch(Exception ignored){return new SavedLookProfiles();}
    }

    public List<String> names(){List<String> names=new ArrayList<>();for(Profile profile:profiles)names.add(profile.name);return List.copyOf(names);}

    public VisualSettings settings(String name){
        String key=key(name);if(key==null)return null;
        for(Profile profile:profiles)if(key.equals(key(profile.name)))return profile.settings.copy();
        return null;
    }

    public boolean contains(String name){return settings(name)!=null;}

    public void saveProfile(Path path,String requestedName,VisualSettings settings)throws IOException{
        String name=validName(requestedName);
        if(settings==null)throw new IllegalArgumentException("Settings are required.");
        settings.validate();
        if(contains(name))throw new IllegalArgumentException("A profile named ‘"+name+"’ already exists.");
        SavedLookProfiles updated=new SavedLookProfiles();updated.profiles=new ArrayList<>(profiles);
        VisualSettings snapshot=settings.copy();snapshot.preset=name;updated.profiles.add(new Profile(name,snapshot));updated.save(path);
        profiles=updated.profiles;
    }

    private void save(Path path)throws IOException{
        Path absolute=path.toAbsolutePath();Path parent=absolute.getParent();if(parent!=null)Files.createDirectories(parent);
        Path temporary=Files.createTempFile(parent,absolute.getFileName().toString(),".tmp");
        try{
            try(Writer writer=Files.newBufferedWriter(temporary)){gson().toJson(this,writer);writer.write(System.lineSeparator());}
            try{Files.move(temporary,absolute,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ignored){Files.move(temporary,absolute,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }

    private void normalize(){
        schemaVersion=1;List<Profile> valid=new ArrayList<>();Set<String> names=new LinkedHashSet<>();
        if(profiles!=null)for(Profile profile:profiles)try{
            if(profile==null||profile.settings==null)continue;
            profile.name=validName(profile.name);profile.settings.validate();String key=key(profile.name);
            if(names.add(key)){profile.settings.preset=profile.name;valid.add(profile);}
        }catch(RuntimeException ignored){}
        profiles=valid;
    }

    static String validName(String requestedName){
        String name=requestedName==null?"":requestedName.trim();
        if(name.isEmpty())throw new IllegalArgumentException("Enter a profile name.");
        if(VisualPresetCatalog.isReservedName(name))throw new IllegalArgumentException("‘"+name+"’ is reserved for a built-in Look choice.");
        return name;
    }

    private static String key(String name){return name==null?null:name.trim().toLowerCase(Locale.ROOT);}
    private static Gson gson(){return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();}

    public static final class Profile {
        public String name;public VisualSettings settings;
        public Profile(){}
        Profile(String name,VisualSettings settings){this.name=name;this.settings=settings;}
    }
}
