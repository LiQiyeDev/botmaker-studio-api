package com.botmaker.plugin.api.catalog.seeds;

import com.botmaker.plugin.api.scaffold.ClassName;
import com.botmaker.plugin.api.scaffold.Editable;
import com.botmaker.plugin.api.scaffold.EnumValues;
import com.botmaker.plugin.api.scaffold.Scaffold;

/**
 * A well-formed seed, and the shape the surface is designed around: a name the host substitutes, an enum
 * whose constants it substitutes, and one body it hands over.
 *
 * <p>It compiles here, in this module's own build, which is the property the whole surface exists for. Its
 * source is also copied into {@code src/test/resources} under the same package — which is not test
 * scaffolding but the arrangement a real plugin's build must make, since a seed's class compiling says
 * nothing about whether its source reached the jar.
 */
@Scaffold(path = "src/main/java/{package}/activities/{name}.java",
        description = "one thing the bot can do")
@ClassName
public class GoodSeed {

    @EnumValues("outcomes")
    public enum Outcome { NEXT }

    @Editable("how to do it")
    public Outcome run() {
        return Outcome.NEXT;
    }

    public String name() {
        return getClass().getSimpleName();
    }
}
