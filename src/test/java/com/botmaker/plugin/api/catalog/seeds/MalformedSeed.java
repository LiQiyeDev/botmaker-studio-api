package com.botmaker.plugin.api.catalog.seeds;

import com.botmaker.plugin.api.scaffold.Scaffold;

/**
 * A path that varies by {@code {name}} on a type that carries no {@code @ClassName} — so every project would
 * get a file named for its activity holding a public class called {@code MalformedSeed}, which is not Java.
 * It compiles here, which is the point: this is the class of mistake only the catalog can catch.
 *
 * <p>No source twin, deliberately: the path check is fatal and runs first, so the catalog must report the
 * path and never the missing source. Reporting the second mistake for a seed already rejected for the first
 * would send its author looking at their build configuration.
 */
@Scaffold(path = "src/main/java/{package}/{name}.java")
public class MalformedSeed {
}
