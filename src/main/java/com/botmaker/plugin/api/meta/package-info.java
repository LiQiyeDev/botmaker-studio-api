/**
 * What a plugin says <em>about</em> its own surface, as opposed to what the surface is.
 *
 * <p>Today that is one annotation, {@link com.botmaker.plugin.api.meta.Internal}, and the classification it
 * completes: between it and {@link com.botmaker.plugin.api.palette.Facade}, every class a plugin compiles has
 * an answer to "is this surface, and is it offered". The two annotations sit in different packages on
 * purpose — one is a palette verdict, the other is a compatibility claim, and only the second is a thing a
 * plugin with no palette at all still needs.
 *
 * <p>This package is where the compatibility <em>vocabulary</em> is going: {@code @ReplacedBy},
 * {@code @Replaces} and {@code @Since} live in {@code com.botmaker.sdk.api.meta} today, which makes them the
 * SDK's rather than every plugin's, and a plugin's own type renames want exactly the same machinery. The
 * processor already reads both spellings so that move can be a deprecation window rather than a flag day.
 */
package com.botmaker.plugin.api.meta;
