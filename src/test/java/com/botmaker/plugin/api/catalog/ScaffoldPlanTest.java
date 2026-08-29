package com.botmaker.plugin.api.catalog;

import com.botmaker.plugin.api.catalog.seeds.GoodSeed;
import com.botmaker.plugin.api.catalog.seeds.MalformedSeed;
import com.botmaker.plugin.api.catalog.seeds.PlainSeed;
import com.botmaker.plugin.api.scaffold.Seeding;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What {@link ScaffoldPlan#of} refuses, and what it lets through anyway.
 *
 * <p>Everything asserted here is answerable without parsing Java, which is the boundary the class is drawn
 * on. Two properties matter more than the individual checks:
 *
 * <ul>
 *   <li><b>one rejected file is one file missing</b>, never a throw and never a plan abandoned — the rule
 *       {@code PaletteCatalog.of} and {@code ValueCatalog.merge} already follow, because this runs while a
 *       project is opening;
 *   <li><b>a hole nobody fills keeps the seed's own constants</b>, which is what lets a seed compile on its
 *       own and is therefore the property the whole surface rests on.
 * </ul>
 */
class ScaffoldPlanTest {

    private static final String PACKAGE = "com.demo";

    /** A seed that renames its type and has one hole, and one that does neither. */
    private static ScaffoldCatalog catalog() {
        return ScaffoldCatalog.of(GoodSeed.class, PlainSeed.class);
    }

    private static String goodPath() {
        return "src/main/java/{package}/activities/{name}.java";
    }

    private static String plainPath() {
        return "src/main/java/{package}/GoHome.java";
    }

    private static ScaffoldPlan plan(Map<String, List<Seeding>> seedings) {
        return ScaffoldPlan.of(catalog(), PACKAGE, seedings);
    }

    private static Seeding outcomes(String key, String name, String... constants) {
        return new Seeding(key, name, Map.of("outcomes", List.of(constants)));
    }

    // ---------------------------------------------------------------- the ordinary case

    @Test
    void crossesOneSeedWithEveryInstanceOfIt() {
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(
                outcomes("act-1", "Mining", "NEXT", "BAG_FULL"),
                outcomes("act-2", "Fishing", "NEXT"))));

        assertEquals(List.of(), plan.problems());
        assertEquals(2, plan.files().size());
        assertFalse(plan.isEmpty());
    }

    @Test
    void resolvesThePathFromThePackageAndTheName() {
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(outcomes("act-1", "Mining", "NEXT"))));

        ScaffoldPlan.PlannedFile file = plan.forKey("act-1");
        assertEquals("src/main/java/com/demo/activities/Mining.java", file.path());
        assertEquals("Mining", file.typeName());
        assertSame(file, plan.at("src/main/java/com/demo/activities/Mining.java"));
    }

    @Test
    void aSeedThatRenamesNothingKeepsItsOwnTypeName() {
        ScaffoldPlan plan = plan(Map.of(plainPath(), List.of(new Seeding("gohome", ""))));

        ScaffoldPlan.PlannedFile file = plan.forKey("gohome");
        assertEquals(List.of(), plan.problems());
        assertEquals("PlainSeed", file.typeName());
        assertEquals("src/main/java/com/demo/GoHome.java", file.path());
    }

    @Test
    void anUnfilledHoleKeepsTheSeedsOwnConstants() {
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(new Seeding("act-1", "Mining"))));

        ScaffoldPlan.PlannedFile file = plan.forKey("act-1");
        assertEquals(List.of(), plan.problems());
        // null, not empty: nothing said means the seed's own `enum Outcome { NEXT }` stands, where an empty
        // list would mean an enum with no constants at all.
        assertNull(file.constantsFor(file.seed().enums().get(0)));
    }

    @Test
    void aFilledHoleCarriesItsConstantsInTheOrderGiven() {
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(outcomes("act-1", "Mining", "NEXT", "BAG_FULL"))));

        ScaffoldPlan.PlannedFile file = plan.forKey("act-1");
        assertEquals(List.of("NEXT", "BAG_FULL"), file.constantsFor(file.seed().enums().get(0)));
    }

    @Test
    void anEmptyRequestPlansNothing() {
        assertTrue(ScaffoldPlan.of(catalog(), PACKAGE, Map.of()).isEmpty());
        assertTrue(ScaffoldPlan.of(null, PACKAGE, Map.of(goodPath(), List.of())).isEmpty());
        assertTrue(ScaffoldPlan.empty().problems().isEmpty());
    }

    // ---------------------------------------------------------------- names

    @Test
    void aNameThatIsAKeywordIsRefused() {
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(outcomes("act-1", "class", "NEXT"))));

        assertTrue(plan.isEmpty());
        assertEquals(1, plan.problems().size());
        assertTrue(plan.problems().get(0).contains("keyword"), plan.problems().toString());
    }

    @Test
    void aNameThatIsNotAnIdentifierIsRefused() {
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(outcomes("act-1", "My Activity", "NEXT"))));

        assertTrue(plan.isEmpty());
        assertTrue(plan.problems().get(0).contains("not a Java identifier"), plan.problems().toString());
    }

    @Test
    void aSubstitutedSeedWithNoNameIsRefused() {
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(new Seeding("act-1", ""))));

        assertTrue(plan.isEmpty());
        assertTrue(plan.problems().get(0).contains("gives no name"), plan.problems().toString());
    }

    @Test
    void aNameOnASeedThatSubstitutesNoneIsReportedButStillWritten() {
        ScaffoldPlan plan = plan(Map.of(plainPath(), List.of(new Seeding("gohome", "SomethingElse"))));

        // The plugin believed a substitution would happen and it will not — worth saying, and not worth
        // withholding the file over, since the file it gets is the one the seed describes.
        assertEquals(1, plan.problems().size());
        assertTrue(plan.problems().get(0).contains("no @ClassName"), plan.problems().toString());
        assertEquals(1, plan.files().size());
        assertEquals("PlainSeed", plan.forKey("gohome").typeName());
    }

    @Test
    void namingASeedAfterItselfIsNotAProblem() {
        ScaffoldPlan plan = plan(Map.of(plainPath(), List.of(new Seeding("gohome", "PlainSeed"))));

        assertEquals(List.of(), plan.problems());
        assertEquals(1, plan.files().size());
    }

    // ---------------------------------------------------------------- holes

    @Test
    void aDuplicateConstantIsRefused() {
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(outcomes("act-1", "Mining", "NEXT", "NEXT"))));

        assertTrue(plan.isEmpty());
        assertTrue(plan.problems().get(0).contains("twice"), plan.problems().toString());
    }

    @Test
    void constantsAreComparedTheWayJavacComparesThem() {
        // NEXT and Next are two constants, however alike they read. Refusing them would be this module
        // having an opinion the language does not.
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(outcomes("act-1", "Mining", "NEXT", "Next"))));

        assertEquals(List.of(), plan.problems());
        assertEquals(1, plan.files().size());
    }

    @Test
    void aConstantThatIsNotAnIdentifierIsRefused() {
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(outcomes("act-1", "Mining", "BAG FULL"))));

        assertTrue(plan.isEmpty());
        assertTrue(plan.problems().get(0).contains("not a Java identifier"), plan.problems().toString());
    }

    @Test
    void fillingAKeyNoSeedDeclaresIsRefused() {
        // The typo catcher: "outcome" for "outcomes" would otherwise leave the enum silently at its default,
        // which surfaces as a bot doing the wrong thing rather than as anything failing.
        ScaffoldPlan plan = plan(Map.of(goodPath(),
                List.of(new Seeding("act-1", "Mining", Map.of("outcome", List.of("NEXT"))))));

        assertTrue(plan.isEmpty());
        assertTrue(plan.problems().get(0).contains("outcome"), plan.problems().toString());
    }

    // ---------------------------------------------------------------- identity

    @Test
    void twoInstancesUnderOneKeyAreRefused() {
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(
                outcomes("act-1", "Mining", "NEXT"),
                outcomes("act-1", "Fishing", "NEXT"))));

        assertEquals(1, plan.files().size());
        assertTrue(plan.problems().get(0).contains("twice under the key"), plan.problems().toString());
    }

    @Test
    void anInstanceWithNoKeyIsRefused() {
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(outcomes("", "Mining", "NEXT"))));

        assertTrue(plan.isEmpty());
        assertTrue(plan.problems().get(0).contains("no key"), plan.problems().toString());
    }

    @Test
    void twoKeysResolvingToOneFileAreRefused() {
        ScaffoldPlan plan = plan(Map.of(goodPath(), List.of(
                outcomes("act-1", "Mining", "NEXT"),
                outcomes("act-2", "Mining", "NEXT"))));

        assertEquals(1, plan.files().size());
        assertTrue(plan.problems().get(0).contains("both resolve to"), plan.problems().toString());
    }

    @Test
    void aSeedThatSubstitutesNoNameCanOnlyBeSeededOnce() {
        ScaffoldPlan plan = plan(Map.of(plainPath(), List.of(
                new Seeding("a", ""), new Seeding("b", ""))));

        assertTrue(plan.isEmpty());
        assertTrue(plan.problems().get(0).contains("same file"), plan.problems().toString());
    }

    @Test
    void seedingSomethingNothingShipsIsReported() {
        ScaffoldPlan plan = plan(Map.of("src/main/java/{package}/Nowhere.java",
                List.of(new Seeding("x", "Nowhere"))));

        assertTrue(plan.isEmpty());
        assertTrue(plan.problems().get(0).contains("nothing seeds"), plan.problems().toString());
    }

    // ---------------------------------------------------------------- the catalog's own problems

    @Test
    void aCatalogsProblemsTravelIntoThePlan() {
        // A host reads one list, so a seed rejected before anything was asked of it has to appear in the same
        // place as an instance rejected after.
        ScaffoldCatalog catalog = ScaffoldCatalog.of(GoodSeed.class, MalformedSeed.class);
        ScaffoldPlan plan = ScaffoldPlan.of(catalog, PACKAGE,
                Map.of(goodPath(), List.of(outcomes("act-1", "Mining", "NEXT"))));

        assertEquals(1, plan.files().size());
        assertEquals(catalog.problems(), plan.problems());
        assertFalse(plan.problems().isEmpty());
    }
}
