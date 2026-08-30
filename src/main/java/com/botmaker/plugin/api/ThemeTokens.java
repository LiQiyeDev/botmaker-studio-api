package com.botmaker.plugin.api;

/**
 * The host's current look as data, for a plugin that draws its own user interface somewhere JavaFX cannot
 * reach — a web page it serves, a phone client, a window belonging to a process that is not this one.
 *
 * <p><b>Why this is not {@link Theme}.</b> {@code Theme} applies the host's look to a {@code Scene}, a
 * {@code Stage}, a {@code Dialog} — objects that only exist inside this JVM, on the JavaFX application
 * thread. It is the right answer for a {@link StudioPlugin} drawing an editor, and it is no answer at all for
 * a {@link CompanionPlugin} whose interface is HTML on somebody's phone. That plugin does not want its
 * {@code Scene} styled; it wants to know which colours the user chose so it can style its own.
 *
 * <p>It passes the {@link StudioServices} test without argument: which theme is active is a fact only the
 * host holds, and there is no file, no library and no scan through which a plugin could find it out.
 *
 * <p><b>Colours are CSS strings</b> ({@code "#2C3E50"}), not {@code javafx.scene.paint.Color}, for the same
 * reason the whole record exists: the consumer is as likely to be a stylesheet as a JavaFX property, and a
 * string is the form both can use and the only form that survives a process boundary.
 *
 * <p>The palette is deliberately small — the seven roles Studio actually distinguishes, no more. A plugin
 * needing a shade between two of them should derive it, because a token invented here that the host does not
 * really have would be a constant pretending to be a theme.
 *
 * @param dark        whether the active theme is a dark one; the one bit most clients need before any colour
 * @param background  the window ground
 * @param text        the default foreground on {@code background}
 * @param accent      the primary accent — what a default button or a selection uses
 * @param hover       the accent's hover shade
 * @param error       the colour of something wrong
 * @param warning     the colour of something to be careful about
 * @param success     the colour of something that worked
 * @param fontFamily  the CSS font stack for ordinary text
 * @param monoFamily  the CSS font stack for code
 * @param fontSize    the base font size, in points, that {@code fontFamily} is read at
 */
public record ThemeTokens(
        boolean dark,
        String background,
        String text,
        String accent,
        String hover,
        String error,
        String warning,
        String success,
        String fontFamily,
        String monoFamily,
        double fontSize) {

    /**
     * A neutral light palette, for a host with no theme of its own to report.
     *
     * <p>Real values rather than blanks or {@code null}: a plugin styling a page from this must produce a
     * legible page, and the honest failure of an unthemed host is "it looks generic", never "it renders
     * white on white".
     */
    public static final ThemeTokens DEFAULT = new ThemeTokens(
            false, "#FFFFFF", "#2C3E50", "#3498DB", "#2980B9",
            "#E74C3C", "#F39C12", "#2ECC71",
            "'Segoe UI', sans-serif", "'Consolas', monospace", 11.0);
}
