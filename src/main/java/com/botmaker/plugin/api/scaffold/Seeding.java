package com.botmaker.plugin.api.scaffold;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One file a plugin wants a seed written as: what to call it, and what fills each of its holes.
 *
 * <p>A seed class is a <em>shape</em> and this is one <em>instance</em> of it. The distinction is the whole
 * reason this type exists: an activity template is one seed and a project has five activities, so a host asks
 * for a list and writes a file per element. Without that, a plugin shipping one template could seed one file.
 *
 * <h2>Why the values are nested here rather than keyed globally</h2>
 *
 * <p>Each instance carries <b>both</b> its name and its own values, so {@code "outcomes"} means <em>this
 * file's</em> outcomes and nothing more. The alternative — one map from key to values across the whole seed —
 * forces every key to encode which instance it belongs to, and a compound key is a second identity that the
 * name already is. Here there is no compound key because there is nothing to compound.
 *
 * <h2>The key, and why it is not the name</h2>
 *
 * <p>{@link #key()} is the plugin's own stable identifier and is never shown to anyone. It exists for exactly
 * one moment: the user renames the thing this seeds. A host that matched on {@link #name()} alone would see a
 * file that vanished and a file that appeared, and would write a fresh seed over the top of work somebody
 * did — so it matches on the key, finds the file it wrote for that key last time, and performs a rename:
 * the type, the file, and every reference in the user's own source. Whatever a plugin uses as its own
 * identity is the right value here, so long as it survives a rename.
 *
 * <h2>Nothing here throws</h2>
 *
 * <p>Every component is normalised rather than rejected — a {@code null} name becomes {@code ""}, a
 * {@code null} value list becomes empty — because the thing that judges a seeding is
 * {@code ScaffoldPlan}, which collects what it cannot use and builds the rest. A record that threw would
 * make a plugin's mistake into a project that will not open, which is the outcome every catalog in this
 * module is written to avoid.
 *
 * @param key    the plugin's own stable identity for this instance, unchanged by a rename; never shown
 * @param name   what the seed's {@link ClassName} type is called here, and what {@code {name}} in
 *               {@link Scaffold#path()} resolves to
 * @param values what fills each {@link EnumValues} hole, keyed by {@link EnumValues#value()}. A hole this map
 *               says nothing about keeps the constants the seed itself declares, which is what lets a seed
 *               compile on its own.
 */
public record Seeding(String key, String name, Map<String, List<String>> values) {

    public Seeding {
        key = key == null ? "" : key.trim();
        name = name == null ? "" : name.trim();
        values = copy(values);
    }

    /** A seeding of a seed with no {@link EnumValues} holes — a name and nothing to fill. */
    public Seeding(String key, String name) {
        this(key, name, Map.of());
    }

    /**
     * A deep, null-tolerant copy.
     *
     * <p>Each hole's <em>constants</em> keep the order they were given, because they become an enum's
     * constants in that order and an enum whose members reshuffle between two runs of one project is a diff
     * nobody asked for. The holes themselves are looked up by key, so their own order means nothing.
     */
    private static Map<String, List<String>> copy(Map<String, List<String>> values) {
        if (values == null || values.isEmpty()) return Map.of();
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> hole : values.entrySet()) {
            if (hole.getKey() == null) continue;
            List<String> constants = new ArrayList<>();
            if (hole.getValue() != null) {
                for (String constant : hole.getValue()) {
                    if (constant != null) constants.add(constant.trim());
                }
            }
            out.put(hole.getKey().trim(), List.copyOf(constants));
        }
        return Map.copyOf(out);
    }

    /**
     * What fills the hole keyed {@code key}, or {@code null} when this seeding says nothing about it — which
     * is different from an empty list, and the difference decides whether the seed's own constants stand.
     */
    public List<String> valuesFor(String key) {
        return values.get(key);
    }
}
