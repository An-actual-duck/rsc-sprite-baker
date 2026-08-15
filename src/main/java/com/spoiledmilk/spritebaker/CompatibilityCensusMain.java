package com.spoiledmilk.spritebaker;

import com.google.gson.GsonBuilder;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Terminal entry point for an exhaustive deterministic NPC compatibility census. */
public final class CompatibilityCensusMain {
    private CompatibilityCensusMain(){}

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        Path cache = arguments.cache.toRealPath();
        Path output = arguments.output.toAbsolutePath().normalize();
        Main.enforceOutputBoundary(output.getParent(), cache, Path.of("").toRealPath());
        Files.createDirectories(output.getParent());
        Map<String,Object> identity = new LinkedHashMap<>();
        identity.put("directory", cache.toString());
        identity.put("dataSha256", Hashing.sha256(cache.resolve("main_file_cache.dat2")));
        identity.put("referenceIndexSha256", Hashing.sha256(cache.resolve("main_file_cache.idx255")));
        try (NpcCatalog catalog = new NpcCatalog(cache)) {
            CompatibilityCensus census = CompatibilityCensus.collect(catalog, identity,
                (complete,total) -> { if (complete == total || complete % 512 == 1) System.err.println("Scanned " + complete + " / " + total); });
            try (Writer writer = Files.newBufferedWriter(output)) {
                new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(census, writer);
                writer.write(System.lineSeparator());
            }
            System.out.println("Definitions: " + census.definitionCount);
            census.categories.forEach((category,count) -> System.out.println(category + ": " + count));
            System.out.println("Unsupported operation blockers: " + census.unsupportedOperationBlockers);
            System.out.println("Model failure clusters: " + census.modelFailureClusters);
            System.out.println("Wrote " + output);
        }
    }

    private static final class Arguments {
        Path cache, output;
        static Arguments parse(String[] args) {
            Arguments parsed = new Arguments();
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 >= args.length) usage();
                if ("--cache".equals(args[i])) parsed.cache = Path.of(args[i + 1]);
                else if ("--output".equals(args[i])) parsed.output = Path.of(args[i + 1]);
                else usage();
            }
            if (parsed.cache == null || parsed.output == null || parsed.output.getParent() == null) usage();
            return parsed;
        }
        static void usage() { throw new IllegalArgumentException("usage: --cache PATH --output REPORT.json"); }
    }
}
