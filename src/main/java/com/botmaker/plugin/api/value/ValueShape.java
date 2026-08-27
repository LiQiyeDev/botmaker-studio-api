package com.botmaker.plugin.api.value;

/**
 * How many values of a {@link ValueType} a variable holds, and whether the author fixes the set they come
 * from. The axis that crosses the type catalogue rather than multiplying it.
 *
 * <p>In <em>source</em> the axis has only two positions: {@code T} and {@code List<T>}. {@link #ONE} and
 * {@link #ONE_OF} emit identically, and so do {@link #ANY_OF} and {@link #OPEN_LIST} — the difference
 * between the members of each pair is a question about the editor's widget, not about the field.
 *
 * <p>The two list shapes are separate because they were once one, and which one it meant was decided by data
 * the user could not see: tick boxes when the author had written choices down, a free-text box when they had
 * not, under one label. Splitting them makes the question the shape asks the same question the widget
 * answers.
 *
 * <p><b>This enum may grow.</b> It is a contract type, so a plugin switching over it exhaustively will throw
 * when the host adds a constant — every {@code switch} over a shape needs a {@code default}. The four here
 * are the four the editor can draw a widget for; a fifth would arrive with its widget.
 */
public enum ValueShape {

    /** One value, free within its type. */
    ONE("One value", ""),
    /** One value, out of a set the author writes down. */
    ONE_OF("One of…", "One of "),
    /** Several values out of that set — {@code List<T>} in source. */
    ANY_OF("Many of…", "Many of "),
    /** A list the user fills in themselves, out of no set at all — {@code List<T>} too. */
    OPEN_LIST("List of…", "List of ");

    private final String label;
    private final String prefix;

    ValueShape(String label, String prefix) {
        this.label = label;
        this.prefix = prefix;
    }

    /**
     * What a shape control calls this. The words are the contract's rather than each host's because a
     * variable declared {@code ONE_OF} reads the same wherever it is shown, and two hosts inventing their
     * own wording for one stored shape is a difference the user has to translate.
     */
    public String label() {
        return label;
    }

    /** What precedes a type's own label to name the pair — {@code "One of "} before {@code "Point"}. */
    public String prefix() {
        return prefix;
    }

    /** Whether the author writes the set of values down. */
    public boolean hasOptions() {
        return this == ONE_OF || this == ANY_OF;
    }

    /** Whether this is emitted as {@code List<T>}. */
    public boolean isList() {
        return this == ANY_OF || this == OPEN_LIST;
    }

    /**
     * The shape {@code wire} names. Total — a shape a newer host invented reads as {@link #ONE}, one free
     * value, which holds the stored text rather than failing the open.
     */
    public static ValueShape fromWire(String wire) {
        if (wire == null) return ONE;
        String trimmed = wire.trim();
        for (ValueShape candidate : values()) {
            if (candidate.name().equals(trimmed)) return candidate;
        }
        return ONE;
    }
}
