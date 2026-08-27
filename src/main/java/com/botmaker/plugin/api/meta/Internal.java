package com.botmaker.plugin.api.meta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Not surface: this type or member is the plugin's own plumbing, freely breakable, and never offered.
 *
 * <p>It is the counterpart to {@link com.botmaker.plugin.api.palette.Facade}, and between them every class a
 * plugin compiles is classified. The processor enforces that for the package roots a plugin declares
 * ({@code -Abotmaker.surface}); a class saying neither is an error, because the alternative to saying is
 * guessing.
 *
 * <h2>What it claims, which is more than "keep it out of the menus"</h2>
 *
 * <p>This annotation replaced {@code @NotInPalette} on 2026-08-27, and the meaning widened deliberately. The
 * old one said only that the menus should not suggest a member, and was emphatic that the member
 * <em>"stays public, stays supported and stays under whatever compatibility contract its module carries"</em>.
 * {@code @Internal} says the stronger thing: <b>not versioned surface</b> — it may be renamed, retyped or
 * deleted without a deprecation, it is owed no {@code @Since} and no {@code @ReplacedBy}, and nothing
 * proposes it.
 *
 * <p>That is what a package called {@code internal} already means, said in a way a second plugin can
 * reproduce without adopting anyone else's package names. It is also what makes the pointer rules scopable:
 * a deprecated {@code @Internal} member needs no redirect, because nothing was promised about it.
 *
 * <h2>Internal and offered are mutually exclusive</h2>
 *
 * <p>A type may not carry both this and {@code @Facade}, and a member of an {@code @Internal} type may not
 * carry {@code @PaletteLabel} or {@code @PaletteDefault}. Both are compile errors rather than discouraged
 * patterns, for one reason: a palette entry's function is to <b>insert a call into a bot's source</b>, so
 * offering a member is the act of making a bot write its name down — which makes the enclosing type surface.
 * A type claiming both is a promise nobody keeps and no gate on either side can see break.
 *
 * <p>The two cases that make the question feel open have answers elsewhere:
 *
 * <ul>
 *   <li><b>"the editor should recognise this call without proposing it"</b> —
 *       {@code @Facade(role = "HIDDEN")}. Recognition never required offering.
 *   <li><b>"a bot legitimately calls this, but the type is plumbing"</b> — the type is misfiled, and the
 *       answer is to move it rather than to annotate around it.
 * </ul>
 *
 * <h2>On a member, the unit is the member name</h2>
 *
 * <p>A palette entry is one name; the menu shows a single lead shape and puts the rest behind a submenu, so
 * every overload of a name is reachable from the one entry and there is nothing an overload could usefully be
 * excluded <em>from</em>. Mark <b>one</b> overload and the whole name goes; marking a second is an error,
 * because it can only restate the first or contradict it. This never means "prefer a different shape of the
 * same call" — that is {@link com.botmaker.plugin.api.palette.PaletteDefault}.
 *
 * <p>{@code Object} overrides ({@code toString}, {@code equals}, {@code hashCode}) and the synthetic
 * {@code values()} / {@code valueOf(String)} of an enum need no annotation:
 * {@link com.botmaker.plugin.api.catalog.CatalogBuilder#addAll()} never offers them.
 *
 * <h2>On a package</h2>
 *
 * <p>Written in a {@code package-info.java}, it classifies every class in that package, and a class may still
 * override it with {@code @Facade}. That is what keeps the completeness gate cheap: a package whose name has
 * always implied this finally says it, once.
 *
 * <h2>Retention</h2>
 *
 * <p>{@code RUNTIME}, because {@link com.botmaker.plugin.api.catalog.CatalogBuilder#addAll()} reads it off
 * the real {@code Class<?>}. That is safe on a bot's classpath: a bot does not depend on this module (the
 * SDK's dependency on it is {@code optional}, so it is never transitive), and the JVM parses annotations
 * lazily and silently omits any whose type cannot be resolved.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
public @interface Internal {

    /**
     * Why, in the author's own words. Never shown to a user — the member is simply absent — but it is the
     * only record of the judgement, and the next person to wonder why a public method is missing from a menu
     * reads it here.
     *
     * <p>On a member it reads as a statement about the <em>name</em>, since that is what is being hidden,
     * even though it is written on one overload of it.
     */
    String value() default "";
}
