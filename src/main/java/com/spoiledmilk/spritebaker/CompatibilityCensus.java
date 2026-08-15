package com.spoiledmilk.spritebaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic aggregate and per-definition census payload. */
public final class CompatibilityCensus {
    private static final Pattern OPERATION = Pattern.compile("procedural operation (\\d+)");
    private static final Pattern MODEL_CLUSTER = Pattern.compile("^model \\d+: (.*)$");

    public final int schemaVersion = 1;
    public final Map<String,Object> cache;
    public final int definitionCount;
    public final Map<String,Integer> categories;
    public final Map<String,Integer> failureReasons;
    public final Map<String,Integer> unsupportedOperationBlockers;
    public final Map<String,Integer> modelFailureClusters;
    public final List<NpcCompatibility> definitions;

    CompatibilityCensus(Map<String,Object> cache, List<NpcCompatibility> definitions) {
        this.cache = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(cache));
        this.definitions = List.copyOf(definitions);
        definitionCount = definitions.size();
        TreeMap<String,Integer> categoryCounts = new TreeMap<>();
        TreeMap<String,Integer> reasons = new TreeMap<>();
        TreeMap<String,Integer> operations = new TreeMap<>();
        TreeMap<String,Integer> modelClusters = new TreeMap<>();
        for (NpcCompatibility result : definitions) {
            categoryCounts.merge(result.categoryId, 1, Integer::sum);
            if (result.category != NpcCompatibility.Category.READY) reasons.merge(result.reason, 1, Integer::sum);
            Matcher operation = OPERATION.matcher(result.reason);
            while (operation.find()) operations.merge(operation.group(1), 1, Integer::sum);
            if (result.category == NpcCompatibility.Category.UNSUPPORTED_MODEL) {
                Matcher cluster = MODEL_CLUSTER.matcher(result.reason);
                modelClusters.merge(normalizeModelFailure(cluster.matches() ? cluster.group(1) : result.reason), 1, Integer::sum);
            }
        }
        categories = ordered(categoryCounts);
        failureReasons = ordered(reasons);
        unsupportedOperationBlockers = ordered(operations);
        modelFailureClusters = ordered(modelClusters);
    }

    static CompatibilityCensus collect(NpcCatalog catalog, Map<String,Object> cache,
                                       NpcCatalog.Progress progress) throws java.io.IOException {
        List<NpcCompatibility> results = new ArrayList<>(catalog.size());
        List<Integer> ids = catalog.ids(0, catalog.size());
        for (int i = 0; i < ids.size(); i++) {
            results.add(catalog.assess(ids.get(i)));
            if (progress != null && (i % 64 == 0 || i + 1 == ids.size())) progress.update(i + 1, ids.size());
        }
        return new CompatibilityCensus(cache, results);
    }

    private static <K,V> Map<K,V> ordered(Map<K,V> source) {
        return java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(source));
    }

    private static String normalizeModelFailure(String failure) {
        if (failure.startsWith("IllegalArgumentException: newPosition > limit:")) {
            return "IllegalArgumentException: newPosition > limit";
        }
        return failure;
    }
}
