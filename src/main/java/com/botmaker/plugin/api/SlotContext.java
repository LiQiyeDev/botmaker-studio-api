package com.botmaker.plugin.api;

/**
 * Everything an editor is told about the slot it is editing, and the one way it writes back.
 *
 * <p>The whole contract crosses as strings. That is not a simplification of a richer model — it is what the
 * host already does: fifteen of its nineteen built-in editors hand back Java source text and let the host
 * re-parse it, and a slot's current contents were already exposed as a {@code String}. So a plugin never
 * sees a syntax tree, the host's parser never becomes plugin surface, and this module depends on no parsing
 * library at all.
 *
 * <p><b>It is a {@link ValueContext} with a call site.</b> The supertype is the half that is true of every
 * value the host edits — a type, the current value, a way to write it back — and this interface adds the
 * half that only source code has. An editor written against {@link ValueContext} therefore works in the
 * Parameters window <em>and</em> here; one written against this interface works only where there is Java to
 * replace. Prefer the supertype unless the call site is genuinely what chooses the editor.
 *
 * <p>{@link #value()} and {@link #set} are inherited and are the same value {@link #currentSource()} spells:
 * a one-element list holding the slot's Java expression. Writing through either is the same edit, and
 * {@link #replaceWith} is the one that can also add imports.
 */
public interface SlotContext extends ValueContext {

    /**
     * The Java source currently in the slot — {@code "new Rect(12, 40, 300, 80)"}, {@code "\"gold.png\""},
     * or empty for a slot never filled in.
     *
     * <p>An editor parses this itself when it wants to open showing the current value. Failing to parse it
     * is normal (the user may have typed anything) and must degrade to a default, never to an exception.
     */
    String currentSource();

    /**
     * The simple name of the type declaring the called method — {@code "Game"} — or {@code null} when the
     * host could not resolve the call.
     *
     * <p>Present because a few editors are chosen by <em>where</em> a value is used rather than by its type:
     * a Steam app id and a window title are both {@code String}.
     */
    String enclosingClass();

    /** The name of the called method — {@code "launchSteam"} — or {@code null} when it is unresolved. */
    String enclosingMethod();

    /** The zero-based position of this slot in the call's argument list, or {@code -1} if it is not an argument. */
    int argIndex();

    /**
     * Replaces the slot's contents with a Java expression, adding any imports it needs.
     *
     * <p>The expression is source text the host re-parses; write it as a user would type it. Name the
     * imports as fully-qualified type names — the host adds the ones that are missing and skips the ones
     * already present, and an import that turns out to be unnecessary is dropped rather than left behind.
     * Passing a fully-qualified expression and no imports is always safe.
     *
     * <p>Call it on the JavaFX application thread. Calling it more than once is fine — each call replaces
     * what the previous one wrote — which is what lets a live editor track a drag.
     *
     * @param javaExpression a Java expression, never a statement and never blank
     * @param importsNeeded  fully-qualified type names the expression refers to by simple name
     */
    void replaceWith(String javaExpression, String... importsNeeded);

    /** Always {@code this}: a slot is its own call site. */
    @Override
    default SlotContext asSlot() {
        return this;
    }
}
