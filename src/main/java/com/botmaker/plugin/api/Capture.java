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

    /**
     * One frozen frame of what the bot will actually look at, and the label of the target it came from.
     *
     * <p>Distinct from {@link #grabFrame} because an editor that <em>searches</em> a frame needs the AWT
     * image the matcher takes, and because the label is what lets it say <em>sampled from Diablo IV</em>
     * rather than leaving the user to guess which of their monitors was read.
     *
     * @param image the pixels, in the source's own coordinates
     * @param label what the frame came from — a window title, {@code Screen 2}, an emulator instance name
     */
    record Frame(BufferedImage image, String label) {}

    /**
     * A colour the user pointed at, together with the frame it was taken from and how much that pixel's
     * neighbourhood varies.
     *
     * <p>{@code spread} is the honest suggested tolerance, and it is on this record because it can only be
     * measured at the moment of sampling: it is the colour distance across the pixels around the one chosen,
     * so a tolerance slider has a number to start from instead of a guess. Zero when the host did not
     * measure it.
     */
    record Sample(Frame frame, java.awt.Color color, double spread) {}

    /**
     * What a capture source <em>is</em>, structurally — never as an expression.
     *
     * <p>The host owns the chooser (it enumerates monitors, windows and emulator instances, and paints the
     * live thumbnails); the plugin owns the vocabulary that a choice is written down in. So the host reports
     * the choice as data and the plugin turns it into whatever its own API spells — which is the only split
     * under which two plugins can both offer a capture-source editor.
     *
     * <p><b>Read {@link #kind()} with a {@code default} arm.</b> The enum may gain a constant in a future
     * release, and an editor that has not been rebuilt must fall back rather than fail.
     *
     * @param kind          which of the shapes below this is
     * @param monitorIndex  the zero-based monitor, for {@link Kind#MONITOR}; {@code -1} otherwise
     * @param windowTitle   the case-insensitive title substring, for {@link Kind#WINDOW}; {@code null} otherwise
     * @param emulatorName  the emulator instance name, for {@link Kind#EMULATOR}; {@code null} otherwise
     * @param region        the sub-rectangle of that source the user narrowed to, or {@code null} for all of it
     */
    record SourceChoice(Kind kind, int monitorIndex, String windowTitle, String emulatorName, Region region) {

        /**
         * The shapes a capture source comes in.
         *
         * <p>{@link #PROJECT_DEFAULT} is not one of the others resolved: it means <em>whatever the project is
         * configured for, now and later</em>, and an editor must write it as a live reference rather than as a
         * snapshot of today's default — otherwise the slot silently freezes the moment the user changes it.
         */
        enum Kind { PROJECT_DEFAULT, DESKTOP, MONITOR, WINDOW, EMULATOR }

        public static SourceChoice projectDefault() {
            return new SourceChoice(Kind.PROJECT_DEFAULT, -1, null, null, null);
        }

        public static SourceChoice desktop(Region region) {
            return new SourceChoice(Kind.DESKTOP, -1, null, null, region);
        }

        public static SourceChoice monitor(int index, Region region) {
            return new SourceChoice(Kind.MONITOR, index, null, null, region);
        }

        public static SourceChoice window(String titleSubstring, Region region) {
            return new SourceChoice(Kind.WINDOW, -1, titleSubstring, null, region);
        }

        public static SourceChoice emulator(String instanceName, Region region) {
            return new SourceChoice(Kind.EMULATOR, -1, null, instanceName, region);
        }
    }

    /**
     * Grabs one frame of the project's configured target, with its label.
     *
     * <p><b>Never a silent desktop fallback.</b> What an editor searches has to be a frame of the thing the
     * bot will look at; grabbing the whole desktop instead would answer a different question and give no sign
     * of it. When there is no target, or the grab comes back blank, the host tells the user which of the two
     * happened and the callback simply never fires.
     */
    void grabTargetFrame(Consumer<Frame> onGrabbed);

    /**
     * The eyedropper: lets the user pick a pixel off a frozen frame of the project's target, rather than out
     * of an OS colour palette.
     *
     * <p>A separate method from {@link #sampleColor} because it answers a different question. That one asks
     * <em>which colour do you want</em>; this one asks <em>what colour is that thing on screen</em> — and an
     * author never wants a colour, they have a pixel and need the value that matches it. Game art is shaded
     * and compressed, so the red of a health bar is never pure red and cannot be found on a swatch grid.
     */
    void sampleFromTarget(Consumer<Sample> onSampled);

    /**
     * Opens the host's visual source chooser — monitors, windows and emulator instances with live thumbnails
     * — and reports what the user picked.
     *
     * <p>Not invoked on cancel, like everything else here.
     */
    void chooseSource(Consumer<SourceChoice> onChosen);

    /**
     * The project's currently configured default source, described the same way a choice is.
     *
     * <p>For an editor that wants to <em>seed</em> itself from the project's target — offering the target
     * window's title as the name of a new image template, say — rather than to write the source down. Never
     * {@code null}: a project with nothing configured answers {@link SourceChoice.Kind#DESKTOP}.
     */
    SourceChoice defaultSource();
}
