/**
 * What a plugin says <em>about</em> its own surface, as opposed to what the surface is.
 *
 * <p>Today that is one annotation, {@link com.botmaker.plugin.api.meta.Internal}, and the classification it
 * completes: between it and {@link com.botmaker.plugin.api.palette.Facade}, every class a plugin compiles has
 * an answer to "is this surface, and is it offered". The two annotations sit in different packages on
 * purpose — one is a palette verdict, the other is a compatibility claim, and only the second is a thing a
 * plugin with no palette at all still needs.
 *
 * <p>The compatibility <em>vocabulary</em> is here too, as of 1.2.0:
 * {@link com.botmaker.plugin.api.meta.ReplacedBy}, {@link com.botmaker.plugin.api.meta.Replaces} and
 * {@link com.botmaker.plugin.api.meta.Since}. They lived in {@code com.botmaker.sdk.api.meta}, which made
 * them the SDK's rather than every plugin's, while a plugin's own type renames want exactly the same
 * machinery. The old spellings survive one minor as {@code @Deprecated(forRemoval = true)} shims pointing
 * here, and the processor and Studio both read either — so the move is a deprecation window rather than a
 * flag day, and the pointer pair carries its own move, which is the fairest test it could have.
 */
package com.botmaker.plugin.api.meta;
