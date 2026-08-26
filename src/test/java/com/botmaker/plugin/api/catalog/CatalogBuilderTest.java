package com.botmaker.plugin.api.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The one thing in this module with behaviour, and the one claim the whole design rests on: that a method
 * reference carries enough identity to name an <em>overload</em>, not merely a name.
 *
 * <p>If that were false the catalog would silently offer {@code click(Point)} when it meant
 * {@code click(Rect)}, and nothing downstream could tell.
 */
class CatalogBuilderTest {

    /** Stands in for an SDK facade: an overload set, an arity pair, and a no-argument member. */
    @SuppressWarnings("unused")
    static final class Facade {
        static void capture() {
        }

        static void moveTo(String target) {
        }

        static void click(String target) {
        }

        static void click(Integer times) {
        }

        static void click(String target, Integer times) {
        }

        int width() {
            return 0;
        }
    }

    private static final Category TEST = Category.of("test");

    @Test
    void aReferenceCarriesItsDeclaringClassNameAndDescriptor() {
        MemberId id = MemberId.of((M1<String>) Facade::moveTo);

        assertEquals(Facade.class, id.declaringClass());
        assertEquals("moveTo", id.name());
        assertEquals(List.of("java.lang.String"), id.parameterTypeNames());
        assertEquals("moveTo(java.lang.String)", id.signature());
        assertFalse(id.isConstructor());
    }

    @Test
    void twoOverloadsOfOneNameAreTwoDistinctIds() {
        MemberId byString = MemberId.of((M1<String>) Facade::click);
        MemberId byInteger = MemberId.of((M1<Integer>) Facade::click);

        assertEquals(byString.name(), byInteger.name());
        assertFalse(byString.equals(byInteger), "the descriptor is what tells the two apart: " + byString);
        assertEquals(List.of("java.lang.String"), byString.parameterTypeNames());
        assertEquals(List.of("java.lang.Integer"), byInteger.parameterTypeNames());
    }

    @Test
    void anInstanceMemberPutsItsReceiverInTheFirstTypePositionButNotInTheDescriptor() {
        MemberId id = MemberId.of((M1<Facade>) Facade::width);

        assertEquals("width", id.name());
        assertEquals(List.of(), id.parameterTypeNames(),
                "the receiver is the reference's shape, not the method's own parameter list");
    }

    @Test
    void theBuilderKeepsDeclarationOrderForFacadesAndMembers() {
        PaletteCatalog catalog = PaletteCatalog.builder()
                .facade(Facade.class, TEST)
                .add(Facade::capture)
                .<String>add(Facade::click)
                .<String, Integer>add(Facade::click)
                .build();

        FacadeEntry facade = catalog.facade(Facade.class).orElseThrow();
        assertEquals(List.of("capture", "click", "click"),
                facade.members().stream().map(m -> m.id().name()).toList());
        assertEquals(2, facade.overloads("click").size(), "both arities are offered, separately");
    }

    @Test
    void aLaterVersionReopensAFacadeAndAppends() {
        PaletteCatalog first = PaletteCatalog.builder()
                .facade(Facade.class, TEST)
                .add(Facade::capture)
                .build();

        PaletteCatalog second = first.toBuilder()
                .facade(Facade.class, TEST)
                .<String>add(Facade::moveTo)
                .build();

        assertEquals(1, second.facades().size(), "reopening is not redeclaring");
        assertEquals(List.of("capture", "moveTo"),
                second.facade(Facade.class).orElseThrow().members().stream().map(m -> m.id().name()).toList());
        assertEquals(1, first.facade(Facade.class).orElseThrow().members().size(),
                "the catalog it was derived from is immutable");
    }

    @Test
    void dropRemovesExactlyTheNamedOverload() {
        PaletteCatalog catalog = PaletteCatalog.builder()
                .facade(Facade.class, TEST)
                .<String>add(Facade::click)
                .<Integer>add(Facade::click)
                .build()
                .toBuilder()
                .<Integer>drop(Facade::click)
                .build();

        FacadeEntry facade = catalog.facade(Facade.class).orElseThrow();
        assertEquals(1, facade.members().size());
        assertEquals(List.of("java.lang.String"), facade.members().getFirst().id().parameterTypeNames());
    }

    @Test
    void aMemberFiledUnderTheWrongFacadeIsRefused() {
        CatalogBuilder builder = PaletteCatalog.builder().facade(String.class, TEST);

        assertThrows(IllegalArgumentException.class, () -> builder.add(Facade::capture));
    }

    @Test
    void addingBeforeOpeningAFacadeIsRefused() {
        CatalogBuilder builder = PaletteCatalog.builder();

        assertThrows(IllegalStateException.class, () -> builder.add(Facade::capture));
    }

    @Test
    void theSameOverloadOfferedTwiceIsRefused() {
        CatalogBuilder builder = PaletteCatalog.builder()
                .facade(Facade.class, TEST)
                .<String>add(Facade::click);

        assertThrows(IllegalArgumentException.class, () -> builder.<String>add(Facade::click));
    }

    @Test
    void aLabelFallsBackToTheMembersOwnName() {
        PaletteCatalog catalog = PaletteCatalog.builder()
                .facade(Facade.class, TEST)
                .add(Facade::capture)
                .label("Take a screenshot")
                .<String>add(Facade::moveTo)
                .build();

        List<MemberEntry> members = catalog.facade(Facade.class).orElseThrow().members();
        assertEquals("Take a screenshot", members.get(0).displayLabel());
        assertEquals("moveTo", members.get(1).displayLabel());
        assertEquals("Facade", catalog.facade(Facade.class).orElseThrow().displayLabel());
    }

    @Test
    void anEmptyCatalogIsTheDeclinedToCurateAnswer() {
        assertTrue(PaletteCatalog.empty().isEmpty());
        assertTrue(PaletteCatalog.empty().facades().isEmpty());
        assertFalse(PaletteCatalog.empty().offers(Facade.class));
    }

    @Test
    void mergingIsAdditiveAndAbsorbsAnOverlap() {
        PaletteCatalog host = PaletteCatalog.builder()
                .facade(Facade.class, TEST)
                .add(Facade::capture)
                .build();
        PaletteCatalog plugin = PaletteCatalog.builder()
                .facade(Facade.class, TEST)
                .add(Facade::capture)
                .<String>add(Facade::moveTo)
                .build();

        FacadeEntry merged = host.mergedWith(plugin).facade(Facade.class).orElseThrow();

        assertEquals(List.of("capture", "moveTo"), merged.members().stream().map(m -> m.id().name()).toList());
    }

    @Test
    void aHandWrittenImplementationNamesNothingAndSaysSo() {
        M0 notAReference = new M0() {
            @Override
            public void call() {
            }
        };

        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> MemberId.of(notAReference));
        assertTrue(thrown.getMessage().contains("method reference"), thrown.getMessage());
    }
}
