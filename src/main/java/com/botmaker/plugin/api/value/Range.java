package com.botmaker.plugin.api.value;

/**
 * The declared range of a number variable — <b>both ends optional and independent</b>, both stored as text so
 * a duration bound can be written the way a duration is ({@code "30s"}) rather than as the millisecond count
 * nobody means.
 *
 * <p>Independent is the point: "at most 10" is a sentence a person says, and it was once unsayable because
 * the widget only appeared when both ends were filled in. A missing end is the type's own limit, not a reason
 * to fall back to an unguided text field.
 *
 * <p>Both ends are advice to the editor's widget and a clamp when a value is normalised, never a validation
 * that can fail — a value outside the range is pulled to the nearest bound, because the alternative is a
 * project that refuses to save because of a limit somebody tightened after the fact.
 *
 * <p>To a generator this is inert: it is stored and nothing is emitted from it. It is in the contract because
 * the project file has one owner, and splitting the file by who reads which field is how two authors get
 * created.
 *
 * <p><b>A record, and frozen as one.</b> Adding a component would change the canonical constructor's
 * descriptor and break every plugin already compiled against it. Two ends is what a range is; if a third
 * thing is ever wanted it arrives as a separate type, not as a component here.
 */
public record Range(String min, String max) {

    /** No range declared — the state every number variable starts in. */
    public static final Range NONE = new Range(null, null);

    public Range {
        min = blankToNull(min);
        max = blankToNull(max);
    }

    public boolean isEmpty() {
        return min == null && max == null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
