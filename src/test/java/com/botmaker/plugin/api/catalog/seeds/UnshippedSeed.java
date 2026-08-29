package com.botmaker.plugin.api.catalog.seeds;

import com.botmaker.plugin.api.scaffold.Scaffold;

/**
 * A seed whose class compiles and whose source was <b>not</b> copied beside it — deliberately no resource
 * twin. This is the misconfigured-build failure, and the one this catalog exists to make loud: everything
 * about it looks right until a host tries to write the file.
 */
@Scaffold(path = "src/main/java/{package}/Unshipped.java")
public class UnshippedSeed {
}
