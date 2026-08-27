package com.botmaker.plugin.api.value;

/**
 * What one {@link ValueType}'s stored text means, and how to write it back — both as storage and as Java
 * source. One codec per type, supplied by whichever plugin registers the type.
 *
 * <h2>Three methods, because there were three answers and two owners</h2>
 *
 * <p>Before the registry, the SDK held the readers and the source writers while the editor held its own
 * parallel copy of the readers plus the normalising rules — two answers to the question "what does
 * {@code "1h30m"} mean?", in two repositories, kept in step by hand. There is one now, and it is the
 * plugin's, which is the only party that can possibly know what a {@code Channel} is.
 *
 * <h2>Every method is total</h2>
 *
 * <p>Nothing here may throw and nothing may return {@code null}. A number that will not parse, a choice that
 * is no longer offered, a duration in a unit nobody knows: each answers the type's own default. That is not
 * defensiveness — <b>a project must still open, and still generate, when its file says something
 * impossible</b>, or it is a project nobody can repair through the editor. A codec that throws takes the
 * whole project down with it.
 *
 * <h2>One item at a time</h2>
 *
 * <p>A value is stored as a list of strings whatever its shape — one entry for an ordinary variable, one per
 * item for a list-shaped one — but a codec sees <b>one item</b>. The shape is composed above it, by
 * {@link ValueCatalog#initializer}, so that a codec is written once and works in both shapes without knowing
 * either. {@code List<Key>} needs nothing from a {@code Key} codec that a single {@code Key} did not.
 *
 * <h2>{@code T} never crosses to the host</h2>
 *
 * <p>The editor holds codecs behind a wildcard and only ever calls them in the composed pair
 * {@code literal(parse(wire))}, so it never names {@code T} and never loads the plugin's class. That is what
 * lets a plugin type exist in a project the host cannot itself model.
 *
 * @param <T> the parsed value; a plugin's own type, and invisible outside the plugin
 */
public interface ValueCodec<T> {

    /** What one stored item means. Total: unreadable text answers this type's default, never an exception. */
    T parse(String wire);

    /**
     * The stored form a freshly created value of this type starts with.
     *
     * <p>The default is {@code store(parse(""))} — "what does empty text mean, written back canonically" —
     * which is the right answer for every type whose default <em>is</em> its empty reading: a number that
     * starts at zero, a flag that starts false, an enum that starts at its first constant. Override it only
     * when the seed is a choice rather than a fallback: the SDK's image template starts at the placeholder
     * every project ships, because an empty chip is a value the bot cannot run on, and no amount of parsing
     * {@code ""} discovers that.
     *
     * <p>Total like everything else here, and a fixed point: {@code store(parse(defaultWire()))} must equal
     * {@code defaultWire()}, or a freshly created value changes the moment it is read back.
     */
    default String defaultWire() {
        return store(parse(""));
    }

    /** The stored form of one item — the input {@link #parse} reads back. Canonical, so a diff is stable. */
    String store(T value);

    /**
     * One item as Java source, ready to initialise a field.
     *
     * <p>Write the <em>parsed</em> value, never the text: {@code new java.awt.Color(255, 0, 0)} rather than
     * {@code Color.decode("#FF0000")}. A generated file must contain no expression that can throw at class
     * initialisation, which is what it means for a bot never to fail to start because of its own
     * configuration.
     *
     * <p>Anything outside {@code java.lang} is written fully qualified <em>unless</em> the type declares an
     * {@link ValueType#importName()}, in which case the simple name is used and the import is arranged.
     */
    String literal(T value);
}
