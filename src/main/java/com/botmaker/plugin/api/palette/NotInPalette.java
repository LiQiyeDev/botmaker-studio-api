package com.botmaker.plugin.api.palette;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This member of a {@link Facade} is <em>not</em> proposed in the palette.
 *
 * <p><b>The unit is the member name, not the overload.</b> A palette entry is one name; the menu shows a
 * single lead shape for it and puts the rest behind a submenu, so every overload of a name is reachable
 * from the one entry. There is therefore nothing an overload could usefully be excluded <em>from</em>, and
 * this annotation never means "prefer a different shape of the same call" — that is
 * {@link PaletteDefault}, which says which shape leads. Mark <b>one</b> overload and the whole name goes;
 * marking a second is an error, because the second mark can only restate the first or contradict it.
 *
 * <p><b>Hiding is not deprecating.</b> The member stays public, stays supported and stays under whatever
 * compatibility contract its module carries; a bot that already calls it keeps compiling, and the editor
 * keeps recognising the call it finds in the source. All this says is that the menus should not suggest it
 * to somebody who has not already found it — which is the distinction a palette needs and an API contract
 * has no way to express.
 *
 * <p>Typical reasons, all of them real:
 * <ul>
 *   <li>a member that exists for the runtime rather than for a bot author — lifecycle, teardown, plumbing;
 *   <li>a member returning the very detail its own facade exists to spare a bot from handling;
 *   <li>a member a clearer pair of named calls supersedes, where offering all three as equals would
 *       contradict the author's own advice.
 * </ul>
 *
 * <p>{@code Object} overrides ({@code toString}, {@code equals}, {@code hashCode}) and the synthetic
 * {@code values()} / {@code valueOf(String)} of an enum need no annotation:
 * {@link com.botmaker.plugin.api.catalog.CatalogBuilder#addAll()} never offers them.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface NotInPalette {

    /**
     * Why, in the author's own words. Never shown to a user — the member is simply absent — but it is the
     * only record of the judgement, and the next person to wonder why a public method is missing from a menu
     * reads it here.
     *
     * <p>It reads as a statement about the <em>name</em>, since that is what is being hidden, even though it
     * is written on one overload of it.
     */
    String value() default "";
}
