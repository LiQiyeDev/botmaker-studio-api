/**
 * Curation written on the member it curates.
 *
 * <p>A {@link com.botmaker.plugin.api.catalog.PaletteCatalog} can be built entirely by hand — see
 * {@link com.botmaker.plugin.api.catalog.CatalogBuilder} — and for a small plugin that is the right shape.
 * It stops being the right shape at scale: the BotMaker SDK's own catalog was 620 lines of
 * {@code .<ImageTemplateGroup, CaptureSource, Double>add(ImageFinder::findCompare)}, where the type
 * witnesses exist only to pick an overload and read as noise, and where a member <em>added</em> to a facade
 * is invisible until somebody remembers to name it.
 *
 * <p>These three annotations invert that. A facade declares itself with {@link
 * com.botmaker.plugin.api.palette.Facade}, every public method it declares is offered, and the exceptions
 * are written on the exceptions:
 *
 * <pre>{@code
 * @Facade(category = "vision", categoryLabel = "Vision", icon = "🔍", order = 20)
 * public final class ImageFinder {
 *
 *     public static MatchResult find(ImageTemplate t) { … }        // offered
 *
 *     @NotInPalette("the confidence override belongs in BotSettings")
 *     public static MatchResult find(ImageTemplate t, double c) { … }
 *
 *     @PaletteLabel("Find any of…")
 *     public static MatchResult findAny(ImageTemplate... t) { … }
 * }
 * }</pre>
 *
 * <p><b>Opt-out, not opt-in, and that is the whole point.</b> Under the old hand-written catalog a new public
 * method defaulted to <em>absent from the menus</em>, which is a silent outcome — the method exists, compiles
 * and is supported, and nobody notices it was never proposed. Here it defaults to offered, and declining it
 * is a deliberate line of source carrying a reason.
 *
 * <h2>Retention, and why it differs between them</h2>
 *
 * <p>{@link com.botmaker.plugin.api.palette.Facade} is read only by an annotation processor, which runs at
 * compile time, so it is {@code CLASS}-retained and never appears in reflection. The two member annotations
 * are {@code RUNTIME}-retained because {@link com.botmaker.plugin.api.catalog.CatalogBuilder#addAll()} reads
 * them off the real {@code Class<?>} — which is also what makes them impossible to get wrong: an annotation
 * cannot be attached to a method that does not exist, so curation cannot go stale the way a string in a
 * hand-written list can.
 *
 * <p>Runtime retention on classes a bot loads is safe. A bot does not depend on this module (the SDK's
 * dependency on it is {@code optional}, so it is never transitive), and the JVM parses annotations lazily and
 * silently omits any whose type cannot be resolved. A bot that never reflects over its own facades never
 * looks; a bot that does gets the facade's methods without these.
 */
package com.botmaker.plugin.api.palette;
