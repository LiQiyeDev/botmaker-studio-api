package com.botmaker.plugin.api;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The named pictures a project keeps — what an editor picks from when a value is "one of the images this
 * project has saved".
 *
 * <p>It is a host service rather than a plugin's own directory listing because naming one is <b>policy the
 * host owns</b>: which names are reserved, how a name is sanitised, what happens when the user picks one
 * that is taken, where the file lands, which tags exist and who is told when the set changes. A plugin that
 * walked {@link StudioServices#resourcesDir()} itself would re-answer all of that, differently, and two
 * plugins would then disagree about the same folder.
 *
 * <p><b>An asset crosses as a name and a file, never as an expression.</b> What a plugin writes into a bot's
 * source — a path string, a constant, a factory call — is the plugin's own vocabulary, exactly as with
 * {@link Capture.SourceChoice}. The host says <em>this picture is called {@code accept_button} and lives
 * here</em>; what that becomes in Java is not its business.
 */
public interface Assets {

    /**
     * One saved picture.
     *
     * @param name the base name the user knows it by, with no extension — {@code accept_button}
     * @param file where it actually is, absolute; relativise it against {@link StudioServices#projectDir()}
     *             to get the path a bot resolves at run time
     */
    record Asset(String name, Path file) {}

    /** Every saved picture, in the order the host presents them. Never {@code null}; empty is ordinary. */
    List<Asset> all();

    /** The picture called {@code name}, or empty when the project has none — which is not an error. */
    Optional<Asset> byName(String name);

    /**
     * The tags the user has organised the set with, each mapped to the names carrying it.
     *
     * <p>For an editor offering a <em>group</em> of pictures rather than one: a tag is how a project says
     * "these six are all the accept button". Empty when the project tags nothing.
     */
    Map<String, List<String>> byTag();

    /**
     * Saves {@code image} as a new picture, asking the user what to call it first.
     *
     * <p>Asynchronous and cancellable like everything on {@link Capture}: {@code onSaved} runs on the JavaFX
     * application thread with the saved asset, and does not run at all if the user backs out. The prompt,
     * the name collision and the reserved-name rules are the host's — an editor hands over pixels and gets
     * back a name.
     *
     * @param suggestedTag a tag to offer pre-filled, or {@code null} for none
     */
    void saveNew(BufferedImage image, String suggestedTag, Consumer<Asset> onSaved);

    /** Opens the host's own manager for this set, for the user who wants to rename or delete rather than pick. */
    void manage();

    /**
     * Registers {@code listener}, run on the JavaFX application thread whenever the set changes — a picture
     * saved, renamed or deleted, by this editor or by anything else.
     *
     * <p>An editor showing a name has to be told, or it goes on showing one that was deleted in another
     * window. The returned {@link Runnable} unregisters; call it when the editor goes away.
     */
    Runnable onChanged(Runnable listener);
}
