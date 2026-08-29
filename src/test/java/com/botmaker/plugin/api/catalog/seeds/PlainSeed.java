package com.botmaker.plugin.api.catalog.seeds;

import com.botmaker.plugin.api.scaffold.Scaffold;

/** A seed with a fixed name and a fixed path — the {@code GoHome} shape: no substitution at all. */
@Scaffold(path = "src/main/java/{package}/GoHome.java")
public class PlainSeed {

    public void run() {
    }
}
