package com.botmaker.plugin.api.value;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules a host and a plugin both rely on, held here rather than only in the SDK's tests.
 *
 * <p>The SDK exercises this package thoroughly as its first consumer — but as <em>a</em> consumer, with its
 * own seventeen types registered. What is checked here is what holds for a catalog nobody has seen yet: that
 * identity is the id, that every lookup is total, that an unknown type keeps its value and declines to emit,
 * and that merging two plugins' catalogs cannot throw.
 */
class ValueVocabularyTest {

    private static final ValueType TEXT = ValueType.of(ValueCatalog.TEXT_ID).label("Text").source("String")
            .build();
    private static final ValueType CHANNEL = ValueType.of("discord.Channel").label("Channel")
            .source("Channel").importing("com.example.discord.Channel").build();

    private static final ValueCodec<String> ECHO = new ValueCodec<>() {
        @Override
        public String parse(String wire) {
            return wire == null ? "" : wire;
        }

        @Override
        public String store(String value) {
            return value;
        }

        @Override
        public String literal(String value) {
            return "\"" + value + "\"";
        }
    };

    private static ValueCatalog catalog() {
        return ValueCatalog.builder().add(TEXT, ECHO).add(CHANNEL, ECHO).build();
    }

    /**
     * Two classloaders hold two copies of one class, so object identity answers a question nobody asked. The
     * id is what the project file holds, and it is the only comparison that survives the crossing.
     */
    @Test
    void identityIsTheIdAndNotTheObject() {
        ValueType again = ValueType.of("discord.Channel").label("Something else entirely")
                .source("SomethingElse").build();
        assertEquals(CHANNEL, again);
        assertEquals(CHANNEL.hashCode(), again.hashCode());
        assertFalse(CHANNEL == again, "two builds are two objects; that must not make them two types");
    }

    /** An id nothing registered is a missing plugin, not a missing value. */
    @Test
    void anUnregisteredIdKeepsItsNameAndDeclinesToEmit() {
        ValueCatalog c = ValueCatalog.builder().add(TEXT, ECHO).build();

        ValueType unknown = c.type("discord.Channel");
        assertFalse(unknown.known());
        assertEquals("discord.Channel", unknown.id());
        assertFalse(c.knows("discord.Channel"));
        assertTrue(c.codec("discord.Channel").isEmpty());
        assertTrue(c.initializer(ValueChoice.of(unknown), List.of("#general")).isEmpty(),
                "a guessed literal is worse than a missing field: it compiles and means something else");
        assertTrue(c.imports(ValueChoice.of(unknown)).isEmpty());
    }

    /** An absent id is a file older than the vocabulary, which has always read as text. */
    @Test
    void anAbsentIdIsTextRatherThanUnknown() {
        ValueCatalog c = catalog();
        assertEquals(TEXT, c.type(null));
        assertEquals(TEXT, c.type(""));
        assertEquals(TEXT, c.type("   "));
        assertEquals(TEXT, c.type("  TEXT  "), "and a padded id still resolves");
    }

    /** A catalog that registered no text still answers every lookup — nothing here may throw. */
    @Test
    void everyLookupIsTotalEvenOnACatalogThatKnowsNothing() {
        ValueCatalog c = ValueCatalog.empty();
        assertFalse(c.type(null).known());
        assertFalse(c.text().known());
        assertTrue(c.types().isEmpty());
        assertTrue(c.codec("anything").isEmpty());
    }

    /**
     * Shape is composed above the codec, so one per-item codec serves all four shapes. That is why
     * {@link ValueCodec} takes a {@code String} and not the whole stored list.
     */
    @Test
    void oneItemCodecServesEveryShape() {
        ValueCatalog c = catalog();
        assertEquals("\"a\"", c.initializer(ValueChoice.of(TEXT), List.of("a")).orElseThrow());
        assertTrue(c.initializer(ValueChoice.listOf(TEXT), List.of("a", "b")).orElseThrow()
                .contains("\"b\""), "a list initializer is the item literals, composed");
    }

    /**
     * Merging is left-biased and never throws. A generation collision is a hard error because refusing costs
     * a regenerate; refusing to merge would cost the user every project that has a plugin installed.
     */
    @Test
    void mergingTwoCatalogsCannotFail() {
        ValueType theirText = ValueType.of(ValueCatalog.TEXT_ID).label("Their text").source("String").build();
        ValueCatalog mine = catalog();
        ValueCatalog theirs = ValueCatalog.builder().add(theirText, ECHO).build();

        ValueCatalog merged = mine.merge(theirs);
        assertEquals("Text", merged.type(ValueCatalog.TEXT_ID).label(), "the left side keeps the id it claimed");
        assertTrue(merged.knows("discord.Channel"), "and nothing from either side is dropped");
        assertEquals(List.of(ValueCatalog.TEXT_ID), mine.clashesWith(theirs),
                "the clash is reported rather than thrown, so a host can say which plugin lost");
    }

    /** The impossible shape can only come from a file, and a file that says it must still open. */
    @Test
    void aClosedSetCannotCarryAnAuthorWrittenSubset() {
        ValueType flag = ValueType.of("YES_NO").source("boolean").primitive().closedSet().build();
        assertEquals(ValueShape.ONE, new ValueChoice(flag, ValueShape.ONE_OF).shape());
        assertEquals(ValueShape.ONE_OF, new ValueChoice(TEXT, ValueShape.ONE_OF).shape());
    }

    @Test
    void everyWireParseIsTotal() {
        ValueCatalog c = catalog();
        assertEquals(ValueShape.ONE, ValueShape.fromWire("A_SHAPE_FROM_THE_FUTURE"));
        assertEquals(ValueShape.ONE, ValueShape.fromWire(null));
        assertEquals(Visibility.EDITOR_ONLY, Visibility.fromId("neither"));
        assertEquals(Visibility.EDITOR_ONLY, Visibility.fromId(null));
        assertSame(Range.NONE, Range.NONE);
        assertTrue(new Range("", "  ").isEmpty(), "a blank bound is no bound");
        assertEquals(TEXT, ValueChoice.fromWire(c, null, null, null).type());
    }
}
