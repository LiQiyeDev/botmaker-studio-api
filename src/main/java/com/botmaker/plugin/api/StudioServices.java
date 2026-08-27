package com.botmaker.plugin.api;

import java.nio.file.Path;

/**
 * The host facilities a plugin may use, and deliberately no more than that.
 *
 * <p><b>The test for adding one is that the host is the only possible source of it</b> — not that the host
 * happens to have written it first. Which project is open, what theme the user chose, which window owns a
 * dialog, and the overlay that goes over every window on the screen: a plugin cannot answer any of those, so
 * they are here. Everything else it can do for itself, because {@code botmaker-shared} is published and any
 * plugin may depend on it — enumerating monitors, windows and emulator instances, grabbing pixels from them,
 * reading a launcher's installed-game library — and because the files under {@link #resourcesDir()} are
 * ordinary files.
 *
 * <p><b>The rule this replaced, and why.</b> Until 2026-08-27 the test was "a real editor needed it", and it
 * grew an {@code Assets} service (the project's named pictures), a {@code Capture.SourceChoice} vocabulary and
 * a {@code Frame}/{@code Sample} pair — every one of them a concept belonging to the SDK's <em>own</em> API:
 * a named picture is {@code ImageTemplate}'s, a capture source is {@code CaptureSource}'s, a launcher is
 * {@code Game}'s. Putting them here let plugin #1 reach through the contract for its own vocabulary, which no
 * second plugin could do — the back door this module exists to close. They were deleted rather than
 * generalised, because generalising them would only have moved the same privilege behind a wider name.
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
