package com.botmaker.plugin.api.palette;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a class as a palette facade: the plugin's catalog offers every public method it declares, except
 * the ones marked {@link com.botmaker.plugin.api.meta.Internal}.
 *
 * <p>It is also half of the classification every class under a plugin's declared surface roots must carry:
 * {@code @Facade} or {@code @Internal}, never both, never neither.
 *
 * <p>Read by an annotation processor at compile time, not at runtime — see the
 * {@linkplain com.botmaker.plugin.api.palette package documentation} for why the retention differs from the
 * member annotations'.
 *
 * <h2>Why every element is a {@code String}</h2>
 *
 * <p>{@link #category()} and {@link #role()} name values that are, respectively, a record and an enum in
 * {@code com.botmaker.plugin.api.catalog} — and neither can be an annotation element type here. An
 * annotation element's type must be resolvable wherever the annotation is <em>applied</em>, and the
 * interesting application sites are in modules that depend on this one; more decisively, a {@code Category}
 * is deliberately open (a plugin defines its own), so no closed element type could express it at all. So
 * they are strings, and the processor validates them: an unparseable {@link #role()}, a blank
 * {@link #category()}, or two facades disagreeing about one category's label are all javac errors, in the
 * same build, on the annotation.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Facade {

    /**
     * The {@link com.botmaker.plugin.api.catalog.Category#id() category id} this facade is filed under.
     * Facades sharing an id land in one menu group, so the id — not the label — is what two plugins must
     * agree on.
     */
    String category();

    /**
     * The category's user-visible name. Only one facade in a category need give it; the rest may leave it
     * blank and inherit. Two facades giving <em>different</em> non-blank labels for one id is an error, since
     * a menu group cannot have two names.
     */
    String categoryLabel() default "";

    /** A glyph for the facade's own menu entry. */
    String icon() default "";

    /** The facade's user-visible name; blank means its simple name. */
    String label() default "";

    /**
     * The name of a {@link com.botmaker.plugin.api.catalog.FacadeRole} constant — {@code MENU} (offered in
     * the palette), {@code HIDDEN} (recognised and curated, never proposed) or {@code VALUE} (a value type,
     * not a facade). Validated by the processor.
     */
    String role() default "MENU";

    /**
     * Where the facade sits among its peers. Facades are ordered by this number and then by simple name, so
     * a group can be laid out without every member of it being renumbered when one is inserted — leave gaps.
     *
     * <p>Member order within a facade needs no element: the processor reads declaration order from the
     * source and emits it, which is the order the author already wrote.
     */
    int order() default 100;
}
