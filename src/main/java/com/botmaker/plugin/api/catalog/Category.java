package com.botmaker.plugin.api.catalog;

import java.util.Objects;

/**
 * The group a facade is filed under in the block palette.
 *
 * <p>A record rather than an enum, because a plugin defines its own: a closed set here would mean every new
 * plugin's contributions land in {@code OTHER} until this module is released again, which is exactly the
 * versioning tax the contract exists to avoid. The constants below are the ones the default plugin uses and
 * are offered so two plugins contributing to the same group agree on its {@link #id()} rather than on its
 * spelling.
 *
 * @param id    a stable identifier, compared for equality and never shown
 * @param label the name the user reads
 */
public record Category(String id, String label) {

    public Category {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        if (id.isBlank()) {
            throw new IllegalArgumentException("a category id must not be blank");
        }
    }

    public static final Category VISION = new Category("vision", "Vision");
    public static final Category INTERACTION = new Category("interaction", "Interaction");
    public static final Category CAPTURE = new Category("capture", "Capture");
    public static final Category LAUNCH = new Category("launch", "Launch");
    public static final Category EMULATOR = new Category("emulator", "Emulator");
    public static final Category GEOMETRY = new Category("geometry", "Geometry");
    public static final Category BOT = new Category("bot", "Bot");
    public static final Category UTIL = new Category("util", "Utilities");

    /** A category with the label derived from the id — {@code Category.of("audio")} reads "audio". */
    public static Category of(String id) {
        return new Category(id, id);
    }

    /**
     * A category with an explicit label, falling back to the id when the label is blank.
     *
     * <p>This is the shape a generated catalog uses: an annotation element cannot be left out, so "no label
     * given" arrives as the empty string rather than as an absence, and one overload absorbs both cases.
     */
    public static Category of(String id, String label) {
        return label == null || label.isBlank() ? of(id) : new Category(id, label);
    }
}
