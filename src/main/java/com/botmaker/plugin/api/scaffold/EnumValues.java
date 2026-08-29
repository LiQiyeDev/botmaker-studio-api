package com.botmaker.plugin.api.scaffold;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an enum inside a seed whose <b>constant list</b> the host substitutes — an activity's outcomes being
 * the case this exists for.
 *
 * <p>The constants written in the seed are a working default, not a placeholder: the seed compiles as it
 * stands, which is what makes a seed checkable at all. A host that substitutes nothing leaves a file that
 * still builds.
 *
 * <h2>Why the constants stay a real enum</h2>
 *
 * <p>Because the user's own code names them — {@code return Outcome.BAG_FULL;} — and a string there is a typo
 * nothing catches until the bot runs. Everything else a flow needs is data read at runtime; this one is not,
 * and that is the reason a seed exists rather than nothing at all.
 *
 * <h2>The key</h2>
 *
 * <p>{@link #value()} names <em>which</em> list fills this enum, so a seed carrying two substituted enums is
 * expressible and a host is never guessing from position. Two enums in one seed sharing a key is reported.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnumValues {

    /** What fills this enum — {@code "outcomes"}, say. Names a list the host knows how to supply. */
    String value();
}
