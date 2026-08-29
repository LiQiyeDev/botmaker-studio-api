package com.botmaker.plugin.api.catalog;

import com.botmaker.plugin.api.catalog.seeds.GoodSeed;
import com.botmaker.plugin.api.catalog.seeds.HoleySeed;
import com.botmaker.plugin.api.catalog.seeds.MalformedSeed;
import com.botmaker.plugin.api.catalog.seeds.PlainSeed;
import com.botmaker.plugin.api.catalog.seeds.UnshippedSeed;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The claims {@link ScaffoldCatalog#of(Class[])} rests on.
 *
 * <p>The one worth stating out loud is that <b>the seeds in this package compile</b>. That is not test
 * scaffolding, it is the property the whole surface is built to have: a seed is checked by javac on every
 * build of the plugin that ships it, where a generator's output is checked only when somebody runs it. The
 * assertions below are about everything javac cannot see — that the source reached the jar beside the class,
 * that a mark sits on something it can apply to, and that a mistake in one seed does not take the others down.
 */
class ScaffoldCatalogTest {

    @Test
    void readsAWellFormedSeedWholeOffItsAnnotations() {
        ScaffoldEntry seed = ScaffoldCatalog.of(GoodSeed.class).seeds().get(0);

        assertEquals("src/main/java/{package}/activities/{name}.java", seed.path());
        assertEquals("one thing the bot can do", seed.description());
        assertEquals("GoodSeed", seed.templateName());
        assertTrue(seed.renamesType());
        assertEquals(List.of("outcomes"), seed.enums().stream().map(ScaffoldEntry.EnumHole::key).toList());
        assertEquals("Outcome", seed.enums().get(0).enumName());
        assertTrue(seed.isEditable("run"));
        assertFalse(seed.isEditable("name"));
    }

    @Test
    void readsTheSeedsOwnSourceFromBesideItsClassFile() {
        // The property a seed's class compiling says nothing about: whether the build copied the .java in.
        String source = ScaffoldCatalog.of(GoodSeed.class).seeds().get(0).source();

        assertNotNull(source);
        assertTrue(source.contains("public class GoodSeed"), source);
        assertTrue(source.contains("enum Outcome"), source);
    }

    @Test
    void aSeedWhoseSourceDidNotShipIsOneLineRatherThanOneSilence() {
        ScaffoldCatalog catalog = ScaffoldCatalog.of(UnshippedSeed.class);

        assertTrue(catalog.isEmpty());
        assertEquals(1, catalog.problems().size());
        assertTrue(catalog.problems().get(0).contains("UnshippedSeed.java"), catalog.problems().toString());
    }

    @Test
    void aPathThatVariesByNameNeedsAClassNameToVaryWith() {
        ScaffoldCatalog catalog = ScaffoldCatalog.of(MalformedSeed.class);

        assertTrue(catalog.isEmpty());
        assertTrue(catalog.problems().get(0).contains("@ClassName"), catalog.problems().toString());
    }

    @Test
    void aClassNobodyMarkedIsReportedRatherThanAssumed() {
        ScaffoldCatalog catalog = ScaffoldCatalog.of(String.class);

        assertTrue(catalog.isEmpty());
        assertTrue(catalog.problems().get(0).contains("no @Scaffold"), catalog.problems().toString());
    }

    @Test
    void aMalformedSeedDoesNotTakeTheGoodOnesWithIt() {
        ScaffoldCatalog catalog = ScaffoldCatalog.of(GoodSeed.class, MalformedSeed.class, PlainSeed.class);

        // This runs while a project is opening; one seed missing is recoverable, a project that will not open
        // is not — the rule PaletteCatalog.of and ValueCatalog.merge already follow.
        assertEquals(2, catalog.seeds().size());
        assertEquals(1, catalog.problems().size());
    }

    @Test
    void seedsKeepTheOrderThePluginGaveThem() {
        ScaffoldCatalog catalog = ScaffoldCatalog.of(PlainSeed.class, GoodSeed.class);

        assertEquals(List.of("PlainSeed", "GoodSeed"),
                catalog.seeds().stream().map(ScaffoldEntry::templateName).toList());
    }

    @Test
    void everyWrongMarkIsReportedAndTheSeedSurvivesThemAll() {
        ScaffoldCatalog catalog = ScaffoldCatalog.of(HoleySeed.class);
        ScaffoldEntry seed = catalog.seeds().get(0);

        // A mark on a non-enum, a blank key, the second of two enums claiming one key, and a body handed over
        // that does not exist: four lines, and the one well-formed hole still reaches a host.
        assertEquals(4, catalog.problems().size(), catalog.problems().toString());
        assertEquals(List.of("steps"), seed.enums().stream().map(ScaffoldEntry.EnumHole::key).toList());
        assertEquals("First", seed.enums().get(0).enumName());
    }

    @Test
    void anEditableMethodWithNoBodyIsReported() {
        ScaffoldCatalog catalog = ScaffoldCatalog.of(HoleySeed.class);
        ScaffoldEntry seed = catalog.seeds().get(0);

        assertTrue(seed.isEditable("real"));
        assertFalse(seed.isEditable("nothingToHandOver"));
        assertTrue(catalog.problems().stream().anyMatch(p -> p.contains("nothingToHandOver")),
                catalog.problems().toString());
    }

    @Test
    void twoSeedsClaimingOneFixedPathAreRefusedRatherThanOverwritten() {
        ScaffoldCatalog catalog = ScaffoldCatalog.of(PlainSeed.class, PlainSeed.class);

        assertEquals(1, catalog.seeds().size());
        assertTrue(catalog.problems().get(0).contains("already seeds"), catalog.problems().toString());
    }

    @Test
    void looksASeedUpByItsUnresolvedPath() {
        ScaffoldCatalog catalog = ScaffoldCatalog.of(GoodSeed.class, PlainSeed.class);

        assertNotNull(catalog.seed("src/main/java/{package}/GoHome.java"));
        assertNull(catalog.seed("src/main/java/com/demo/GoHome.java"));
    }

    @Test
    void nothingCataloguedIsNotAnError() {
        assertTrue(ScaffoldCatalog.empty().isEmpty());
        assertTrue(ScaffoldCatalog.of().problems().isEmpty());
    }
}
