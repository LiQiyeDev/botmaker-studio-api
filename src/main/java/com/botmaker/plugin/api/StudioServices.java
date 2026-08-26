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
}
