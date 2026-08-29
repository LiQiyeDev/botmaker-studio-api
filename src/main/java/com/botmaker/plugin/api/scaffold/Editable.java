package com.botmaker.plugin.api.scaffold;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method whose <b>body is the user's</b> — the signature is the plugin's, everything between the
 * braces is theirs, and an editor must let them change it however locked the surrounding file is.
 *
 * <p>This is the inverse of every other mark here. The others say <em>this spot is mine to fill in</em>; this
 * one says <em>this spot is never mine again</em>. An activity's {@code run()} is the case it exists for: the
 * seed ships it as a {@code TODO} returning a default, and filling it in is the only reason the user opened
 * the file.
 *
 * <h2>Why the plugin says it and not the host</h2>
 *
 * <p>Because the plugin wrote the method. A host that decides this for itself is keeping a second copy of a
 * fact it does not own, and the copy is kept by matching names — which is how a user's own overload with the
 * same name as a seed's method comes to be locked out of their own file. Whoever writes the seed states which
 * of its methods they are handing over, and it travels with the method it describes.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Editable {

    /** A sentence for the user, shown where an editor explains why the rest of the file is locked. */
    String value() default "";
}
