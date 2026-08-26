package com.botmaker.plugin.api;

/**
 * A rectangle in screen pixels, as {@link Capture} reports one.
 *
 * <p>A type of this module's own rather than the SDK's {@code Rect}: this module must not depend on any
 * plugin, and a second plugin capturing a region would otherwise be made to speak the first one's geometry.
 * Converting it is one constructor call at the one place an editor needs it.
 *
 * <p>Integers because every producer is a pixel and every consumer is an input event delivered at a whole
 * pixel — the same reason the SDK's geometry types are.
 */
public record Region(int x, int y, int width, int height) {

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }
}
