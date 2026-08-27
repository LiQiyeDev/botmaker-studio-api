package com.botmaker.plugin.api;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * The screen overlay — the one thing about pixels a plugin cannot do for itself.
 *
 * <p>Not "grabbing pixels": a plugin that depends on {@code botmaker-shared} can enumerate monitors, windows
 * and emulator instances and grab any of them, and nothing stops it, because shared is published. What it
 * cannot do is put an overlay over the whole screen — <em>including the host's own windows</em> — hide the
 * editor it was invoked from, and hand the result back on the right thread. That is what is here, and the
 * reason the list is four methods rather than a capture API.
 *
 * <p><b>Every method here is asynchronous, and the callback is the whole reason.</b> Each of these hides the
 * editor's window, puts an overlay over the screen and waits for the user — which cannot block the JavaFX
 * thread the editor is running on. The callback is invoked on the JavaFX application thread, and is
 * <em>not</em> invoked at all if the user cancels: an editor leaves its slot as it found it in that case,
 * rather than writing a default.
 *
 * <p><b>Four members were deleted on 2026-08-27</b> — {@code Frame}, {@code Sample}, {@code SourceChoice} and
 * the four methods over them ({@code grabTargetFrame}, {@code sampleFromTarget}, {@code chooseSource},
 * {@code defaultSource}), added weeks earlier because the SDK's editors wanted them. They described a
 * <em>capture source</em>, which is a concept belonging to the SDK's own {@code CaptureSource} API, so the
 * contract was carrying one plugin's vocabulary on its behalf. See {@link StudioServices} for the rule that
 * replaced the one which let them in.
 */
public interface Capture {

    /**
     * Lets the user drag out a screen region, then reports it in virtual-screen coordinates — the same
     * origin a bot's own matches use, so what the editor writes is what the bot will see.
     */
    void selectRegion(Consumer<Region> onSelected);

    /**
     * Lets the user point at a single pixel, then reports where it was in virtual-screen coordinates.
     *
     * <p>Not {@link #selectRegion} with the size thrown away: picking one pixel needs a magnifier that
     * follows the cursor, because at 1:1 the pointer covers the thing it is choosing. Same coordinate origin,
     * so a point and a region written by two editors mean the same thing.
     */
    void pickPoint(Consumer<Region> onPicked);

    /** Lets the user point at a pixel, then reports its colour. */
    void sampleColor(Consumer<Color> onSampled);

    /**
     * Grabs one frame of whatever the host is configured to look at, so an editor can show the user that
     * rather than the desktop.
     *
     * <p>The host decides what that is and does not say — deliberately, since naming it would be naming a
     * capture source, and a source is a plugin's concept rather than the contract's. An editor that needs to
     * know <em>which</em> window it got should grab that window itself through {@code botmaker-shared}.
     */
    void grabFrame(Consumer<Image> onGrabbed);

    /**
     * Converts an AWT image to a JavaFX one.
     *
     * <p>Here because the host's capture stack produces {@link BufferedImage} and every editor that displays
     * a grab needs the conversion; three of the built-in ones did it before this interface existed. Not a
     * general-purpose utility — it is on this interface because capture is where the AWT image comes from.
     */
    Image toFxImage(BufferedImage image);
}
