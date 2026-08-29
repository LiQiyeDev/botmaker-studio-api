package com.botmaker.plugin.api.catalog.seeds;

import com.botmaker.plugin.api.scaffold.Editable;
import com.botmaker.plugin.api.scaffold.EnumValues;
import com.botmaker.plugin.api.scaffold.Scaffold;

/**
 * A seed that is writable but whose marks are wrong four ways, each reported and each skipped rather than
 * taking the seed down with it: a mark on a non-enum, a blank key, two enums claiming one key, and a body
 * handed over that does not exist.
 */
@Scaffold(path = "src/main/java/{package}/Holey.java")
public abstract class HoleySeed {

    @EnumValues("outcomes")
    public static class NotAnEnum {
    }

    @EnumValues("  ")
    public enum Blank { A }

    @EnumValues("steps")
    public enum First { A }

    @EnumValues("steps")
    public enum Second { B }

    @Editable
    public abstract void nothingToHandOver();

    @Editable
    public void real() {
    }
}
