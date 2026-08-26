package com.botmaker.plugin.api;

import javafx.scene.Node;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A UI for editing one argument of a call — a colour swatch instead of {@code 0xFF00AA}, a region picker
 * instead of {@code new Rect(12, 40, 300, 80)}.
 *
 * <p>Two methods, because that is all the host needs: does this editor claim the slot, and what does it
 * look like. Everything an editor needs to answer either question is on {@link SlotContext}, and everything
 * it needs to <em>write back</em> is {@link SlotContext#replaceWith(String, String...)} — a Java expression
 * as text. No syntax tree crosses this boundary in either direction, which is what keeps the host's editor
 * internals out of the contract.
 *
 * <p>{@link #matches(SlotContext)} is called for every slot the user opens, so it must be cheap: read the
 * type, read the enclosing method name, decide. Do the work in {@link #create(SlotContext)}.
 */
public interface SlotEditor {

    /** Whether this editor wants to render the slot. */
    boolean matches(SlotContext ctx);

    /**
     * Builds the editor's UI. Called only after {@link #matches} returned {@code true}, and always on the
     * JavaFX application thread.
     */
    Node create(SlotContext ctx);

    /** An editor from two lambdas, for the common case where neither half needs state. */
    static SlotEditor of(Predicate<SlotContext> matches, Function<SlotContext, Node> create) {
        return new SlotEditor() {
            @Override
            public boolean matches(SlotContext ctx) {
                return matches.test(ctx);
            }

            @Override
            public Node create(SlotContext ctx) {
                return create.apply(ctx);
            }
        };
    }
}
