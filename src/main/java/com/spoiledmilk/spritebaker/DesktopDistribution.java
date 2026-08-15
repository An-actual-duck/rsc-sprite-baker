package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Discovers the self-contained desktop layout without storing machine paths in projects. */
final class DesktopDistribution {
    static final String CACHE_ENV = "RSC_SPRITE_BAKER_CACHE";
    static final String HOME_ENV = "RSC_SPRITE_BAKER_HOME";
    static final String EXPORT_ENV = "RSC_SPRITE_BAKER_EXPORTS";
    static final String CACHE_PROPERTY = "rsc.spriteBaker.cache";
    static final String HOME_PROPERTY = "rsc.spriteBaker.home";
    static final String EXPORT_PROPERTY = "rsc.spriteBaker.exports";

    final Path applicationRoot;
    final Path cacheDirectory;
    final Path exportDirectory;

    private DesktopDistribution(Path root, Path cache, Path export) {
        applicationRoot = root;
        cacheDirectory = cache;
        exportDirectory = export;
    }

    static DesktopDistribution discover(String[] args) throws IOException {
        return discover(args, System.getenv(), System.getProperties(), inferredCodeLocation(), Path.of("").toAbsolutePath());
    }

    static DesktopDistribution discover(String[] args, Map<String,String> environment, Properties properties, Path codeLocation, Path workingDirectory) throws IOException {
        Path root = configuredPath(properties.getProperty(HOME_PROPERTY), environment.get(HOME_ENV));
        if (root == null) root = inferApplicationRoot(codeLocation);
        root = root.toAbsolutePath().normalize();

        Path commandLineCache = parseCacheArgument(args);
        Path configuredCache = configuredPath(properties.getProperty(CACHE_PROPERTY), environment.get(CACHE_ENV));
        Path cache;
        if (commandLineCache != null) cache = requireCache(commandLineCache, "--cache");
        else if (configuredCache != null) cache = requireCache(configuredCache, "configured cache");
        else {
            LinkedHashSet<Path> candidates = new LinkedHashSet<>();
            candidates.add(root.resolve("cache"));
            candidates.add(root.resolveSibling("2009scape").resolve("Server/data/cache"));
            Path working = workingDirectory.toAbsolutePath().normalize();
            candidates.add(working.resolve("cache"));
            candidates.add(working.resolve("2009scape/Server/data/cache"));
            Path parent = working.getParent();
            if (parent != null) candidates.add(parent.resolve("2009scape/Server/data/cache"));
            cache = firstCache(candidates);
            if (cache == null) throw new IOException("Bundled cache not found. Expected a cache/ folder beside the application containing main_file_cache.dat2 and main_file_cache.idx255. Advanced override: --cache PATH or " + CACHE_ENV + ". Checked: " + candidates);
        }

        Path export = configuredPath(properties.getProperty(EXPORT_PROPERTY), environment.get(EXPORT_ENV));
        if (export == null) export = root.resolve("exports");
        export = export.toAbsolutePath().normalize();
        if (export.startsWith(cache)) throw new IOException("Export directory must remain outside the read-only cache: " + export);
        Files.createDirectories(export);
        if (!Files.isDirectory(export) || !Files.isWritable(export)) throw new IOException("Export directory is not writable: " + export);
        return new DesktopDistribution(root, cache, export);
    }

    private static Path parseCacheArgument(String[] args) throws IOException {
        Path found = null;
        for (int i = 0; i < args.length; i++) {
            if (!"--cache".equals(args[i]) || i + 1 >= args.length) throw new IOException("Desktop usage: [--cache PATH]");
            if (found != null) throw new IOException("--cache may be supplied only once");
            found = parsePath(args[++i], "--cache");
        }
        return found;
    }

    private static Path configuredPath(String property, String environment) throws IOException {
        String value = property != null && !property.isBlank() ? property : environment;
        return value == null || value.isBlank() ? null : parsePath(value, "configured path");
    }

    private static Path parsePath(String value, String label) throws IOException {
        try { return Path.of(value).toAbsolutePath().normalize(); }
        catch (InvalidPathException e) { throw new IOException(label + " is not a valid path", e); }
    }

    private static Path requireCache(Path candidate, String source) throws IOException {
        Path validated = validCache(candidate);
        if (validated == null) throw new IOException(source + " does not identify a JS5 cache containing main_file_cache.dat2 and main_file_cache.idx255: " + candidate);
        return validated;
    }

    private static Path firstCache(Iterable<Path> candidates) {
        for (Path candidate : candidates) {
            Path valid = validCache(candidate);
            if (valid != null) return valid;
        }
        return null;
    }

    private static Path validCache(Path candidate) {
        try {
            Path directory = candidate.toRealPath();
            return Files.isDirectory(directory)
                && Files.isRegularFile(directory.resolve("main_file_cache.dat2"))
                && Files.isRegularFile(directory.resolve("main_file_cache.idx255")) ? directory : null;
        } catch (IOException ignored) { return null; }
    }

    static Path inferApplicationRoot(Path codeLocation) {
        Path location = codeLocation.toAbsolutePath().normalize();
        if (Files.isRegularFile(location)) return location.getParent();
        if (location.getFileName() != null && "classes".equals(location.getFileName().toString())) {
            Path target = location.getParent();
            if (target != null && target.getFileName() != null && "target".equals(target.getFileName().toString())) return target.getParent();
        }
        if (location.getFileName() != null && "target".equals(location.getFileName().toString())) return location.getParent();
        return location;
    }

    private static Path inferredCodeLocation() throws IOException {
        try { return Path.of(DesktopMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()); }
        catch (URISyntaxException | RuntimeException e) { throw new IOException("Unable to determine application location", e); }
    }
}
