package com.botmaker.plugin.api.catalog;

/**
 * How far into the editor a catalogued type reaches.
 *
 * <p>This is the <em>type-level</em> half of curation, and {@link FacadeEntry#members()} is the member-level
 * half. They answer different questions: whether the user can reach a type at all — whether it earns its own
 * submenu in the insert menus — versus, given that they have reached it, which of its members are proposed.
 * Both live here because both are the plugin's editorial call about its own surface; before the plugin
 * platform they were split across two repositories, with the type half hard-coded in the editor.
 *
 * <p>The distinction that reads as a contradiction from the wrong end: a {@link #HIDDEN} or {@link #VALUE}
 * type is still worth cataloguing with a full member list. Its members are reached through a variable's
 * member submenu and through an already-placed block's overload picker, and both consult the member list.
 * Cataloguing such a type does not put it in the insert menus and is not an attempt to.
 */
public enum FacadeRole {

    /** Its own submenu in the insert menus, in declaration order, under {@link FacadeEntry#icon()}. */
    MENU,

    /**
     * Recognised as a call into this plugin's API — a placed call renders with the right chrome and is kept
     * out of the generic "library call" listings — but never offered in the insert menus.
     *
     * <p>The shape this exists for is plumbing the user should not reach for directly: a capture window
     * driven by a picker rather than typed, a watchdog toggled by the generated loop, a guard installed by
     * the entry point.
     */
    HIDDEN,

    /**
     * Not a facade: a value type, record, enum or interface, reached only as a variable's type.
     *
     * <p>Still an <b>import target</b>, and that is why it belongs in the catalog even when it offers no
     * members at all. The catalog holds real {@link Class} objects, so it is what lets the editor decide
     * that {@code Point} in a bot's source means this plugin's and not {@code java.awt}'s.
     */
    VALUE
}
