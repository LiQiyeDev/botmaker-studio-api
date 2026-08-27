package com.botmaker.plugin.api;

import java.util.List;

/**
 * A value being edited, with no source code anywhere in sight.
 *
 * <p>This is the half of {@link SlotContext} that does not need a call site: a type, the value as the
 * project file holds it, a way to write it back, and the host services. It exists because the host edits
 * values in <b>two</b> places and only one of them is source code — a slot in a bot's Java, and a row in
 * the Parameters window — and until this interface there was no way for one editor to serve both. A
 * plugin's editor could be written, and could never appear in the Parameters window, because the only
 * context it could be handed was one built around an argument of a method call.
 *
 * <p><b>The value is a {@code List<String>}, always.</b> Not a {@code String}, and not a typed value: that
 * is what a project file holds, for every {@link com.botmaker.plugin.api.value.ValueShape shape} —
 * a single value is a one-element list, an empty one is a list of none. An editor for a shape it does not
 * understand reads {@link #single()} and is right about the ordinary case.
 *
 * <p><b>The type crosses as a {@link TypeRef}, never as a {@code Class}</b> — see the module's rule 2. A
 * value-vocabulary type reaches an editor as the Java type it is written as
 * ({@code com.botmaker.sdk.api.vision.Precision}, {@code java.time.Duration}), which is deliberately the
 * <em>same</em> discriminator a slot in source uses. That is what makes "one editor, both places" true
 * rather than aspirational: an editor matching on the type matches in both.
 */
public interface ValueContext {

    /**
     * The type of the value being edited.
     *
     * <p>May be {@linkplain TypeRef#isResolved() unresolved} — a slot the host could not type, or a value
     * whose type no loaded plugin registers. An editor must treat that as "not mine" rather than as an
     * error.
     */
    TypeRef type();

    /**
     * The current value, in the wire form the project file holds — never null, possibly empty.
     *
     * <p>Parsing it is the editor's own job, and failing to parse it is normal: the user may have typed
     * anything, and a value may have been written by a newer version of the plugin than the one reading it.
     * Degrade to a default; never throw.
     */
    List<String> value();

    /**
     * Replaces the value with {@code value}, in the same wire form.
     *
     * <p>Call it on the JavaFX application thread. Calling it repeatedly is fine — each call replaces what
     * the previous one wrote — which is what lets a live editor track a drag. Passing an empty list clears
     * the value, which is a legal state everywhere.
     */
    void set(List<String> value);

    /** The host services an editor may use: theming, screen capture, dialogs, and the project's location. */
    StudioServices services();

    /** The first value, or {@code ""} — what an editor for a single-valued shape wants. */
    default String single() {
        List<String> current = value();
        return current == null || current.isEmpty() ? "" : current.getFirst();
    }

    /** Replaces the value with the one item {@code value}. */
    default void set(String value) {
        set(value == null ? List.of() : List.of(value));
    }

    /**
     * This context's source-code half, or {@code null} when there is none.
     *
     * <p>The one place an editor is entitled to ask <em>where</em> it is being shown. A few editors are
     * chosen by the call they sit in rather than by their type (a Steam app id and a window title are both
     * {@code String}), and those need a call site; everything else should not know the difference. It is a
     * method rather than an {@code instanceof} so that the question reads as a question in plugin code, and
     * so the host may one day answer it with something other than "am I an instance of".
     */
    default SlotContext asSlot() {
        return this instanceof SlotContext slot ? slot : null;
    }
}
