package com.botmaker.plugin.api;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Grabbing pixels — the service most of the interesting editors are built on. Picking a region, sampling a
 * colour and grabbing a frame are all "show the user the screen and let them point at it", and none of them
 * is something a plugin can do for itself: the host owns the overlay, the window list and the capture
 * backend for the platform it is running on.
 *
 * <p><b>Every method here is asynchronous, and the callback is the whole reason.</b> Each of these hides the
 * editor's window, puts an overlay over the screen and waits for the user — which cannot block the JavaFX
 * thread the editor is running on. The callback is invoked on the JavaFX application thread, and is
 * <em>not</em> invoked at all if the user cancels: an editor leaves its slot as it found it in that case,
 * rather than writing a default.
 */
public interface Capture {

    /**
     * Lets the user drag out a screen region, then reports it in virtual-screen coordinates — the same
     * origin a bot's own matches use, so what the editor writes is what the bot will see.
     */
    void selectRegion(Consumer<Region> onSelected);

    /** Lets the user point at a pixel, then reports its colour. */
    void sampleColor(Consumer<Color> onSampled);

    /**
     * Grabs one frame of the project's configured capture source — the target window, monitor or emulator
     * screen — so an editor can show the user what the bot sees rather than what the desktop looks like.
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
