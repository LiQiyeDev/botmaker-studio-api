package com.botmaker.plugin.api;

import java.nio.file.Path;

/**
 * The host facilities a plugin may use, and deliberately no more than that.
 *
 * <p>Each of the three was derived from what the host's own editors actually reach for — theming, screen
 * capture, native dialogs — rather than from a guess at what a plugin might one day want. A service is added
 * here when a real editor needs it, which keeps the contract small enough to keep compatible.
 */
public interface StudioServices {

    /** The root of the open project — where a plugin reads and writes the files it owns. */
    Path projectDir();

    /**
     * The open project's resources directory, where images and other assets a bot loads by name live.
     *
     * <p>An editor that lets the user pick an image writes the file here and puts its <em>name</em> in the
     * slot, so the bot resolves it at runtime the same way on every machine.
     */
    Path resourcesDir();

    /** Applying the host's current look to a window, dialog or scene a plugin creates. */
    Theme theme();

    /** Grabbing pixels: a screen region, a colour, a frame of the target application. */
    Capture capture();

    /** Native file and directory choosers, and the window a plugin's own dialog should be owned by. */
    Dialogs dialogs();

    /**
     * The named pictures the project has saved, and the host's rules for adding one.
     *
     * <p>Added for the same reason as the other four: a real editor needed it. Picking one of a project's
     * saved images is not a directory listing — naming, tagging and collision are host policy, and two
     * plugins reimplementing them would disagree about one folder.
     *
     * <p>{@code default} like every other method a plugin may meet on an older host: a host that predates
     * this service answers "no pictures" rather than {@code AbstractMethodError}. The empty implementation
     * is honest — an editor that finds nothing to pick from shows nothing to pick from.
     */
    default Assets assets() {
        return new Assets() {
            @Override public java.util.List<Asset> all() { return java.util.List.of(); }
            @Override public java.util.Optional<Asset> byName(String name) { return java.util.Optional.empty(); }
            @Override public java.util.Map<String, java.util.List<String>> byTag() { return java.util.Map.of(); }
            @Override public void saveNew(java.awt.image.BufferedImage image, String suggestedTag,
                                          java.util.function.Consumer<Asset> onSaved) {}
            @Override public void manage() {}
            @Override public Runnable onChanged(Runnable listener) { return () -> {}; }
        };
    }
}
