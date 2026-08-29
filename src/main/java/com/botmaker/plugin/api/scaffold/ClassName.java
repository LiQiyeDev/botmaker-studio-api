package com.botmaker.plugin.api.scaffold;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a seed's own type name as substituted: the host writes the user's name where this class's name
 * stands, and {@code {name}} in {@link Scaffold#path()} resolves to the same thing.
 *
 * <p><b>Intent, not addressing.</b> This says a name <em>may</em> be replaced; it cannot say where the name
 * sits in the source text, because reflection knows a type exists and nothing about its position. The host
 * parses the seed and rewrites the node it actually finds. Both are needed and neither substitutes for the
 * other: without the mark a host would be guessing which names are the plugin's to change, and without the
 * parse it would be matching tokens.
 *
 * <p>Only a top-level type carries this. A nested type's name is part of the shape a user writes down
 * ({@code Mining.Outcome}), so renaming the outer type carries it along and renaming the inner one separately
 * would break source nobody asked to change.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ClassName {
}
