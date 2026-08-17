package com.spoiledmilk.spritebaker;

import com.google.gson.GsonBuilder;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Terminal-only forensic report for the apparent material operation 255. */
public final class MaterialOpcode255AuditMain {
    private MaterialOpcode255AuditMain(){}
    public static void main(String[] args)throws Exception{Arguments parsed=Arguments.parse(args);Path cache=parsed.cache.toRealPath(),output=parsed.output.toAbsolutePath().normalize();Main.enforceOutputBoundary(output.getParent(),cache,Path.of("").toRealPath());Files.createDirectories(output.getParent());Map<String,Object> report=MaterialOpcode255Audit.collect(cache,(complete,total)->System.err.println("Scanned "+complete+" / "+total));try(Writer writer=Files.newBufferedWriter(output)){new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(report,writer);writer.write(System.lineSeparator());}System.out.println("Affected NPCs: "+report.get("affectedNpcCount"));System.out.println("Affected material graphs: "+report.get("affectedMaterialGraphIds"));System.out.println("Wrote "+output);}
    private static final class Arguments{Path cache,output;static Arguments parse(String[] args){Arguments parsed=new Arguments();for(int i=0;i<args.length;i+=2){if(i+1>=args.length)usage();if("--cache".equals(args[i]))parsed.cache=Path.of(args[i+1]);else if("--output".equals(args[i]))parsed.output=Path.of(args[i+1]);else usage();}if(parsed.cache==null||parsed.output==null||parsed.output.getParent()==null)usage();return parsed;}static void usage(){throw new IllegalArgumentException("usage: --cache PATH --output REPORT.json");}}
}
