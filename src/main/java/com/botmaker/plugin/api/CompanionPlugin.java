package com.botmaker.plugin.api;

import java.util.List;

/**
 * A plugin that does something <em>beside</em> the user's code rather than shaping it.
 *
 * <h2>Why there are two plugin interfaces</h2>
 *
 * <p>{@link StudioPlugin} is about what a bot's source says: the palette proposes members to call, slot
 * editors decide how a value is written down, value types decide what a project variable may hold,
 * parameters decide what becomes a field. Every one of those answers a question about code, and answering it
 * needs a JavaFX {@code Node} and a live syntax tree — a {@link SlotEditor} literally returns one.
 *
 * <p>A companion plugin answers none of them. It offers a button, opens something of its own, watches the
 * bot run, and releases what it held when the project closes. The Remote Pilot is the worked example: it
 * streams what the bot sees to a phone and drives the run, and it never reads or writes a line of the
 * project's Java.
 *
 * <p><b>The rule for deciding which interface a new surface belongs on:</b> if it decides what the user's
 * code says, it is {@link StudioPlugin}; if it does something beside the code, it is here. A surface that
 * seems to want both is the signal that it is two surfaces.
 *
 * <h2>What the split buys</h2>
 *
 * <p>Every member here is expressible as data — a string, a record, a notification. Nothing on this
 * interface needs a {@code Node}, a {@code Scene} or a {@code Window}, which is what makes a companion
 * plugin implementable by something that is not running inside this JVM at all. That is the point of the
 * separation and not a coincidence of the current member list: <b>a member that cannot cross a process
 * boundary does not belong on this interface.</b> A plugin that needs to draw into the editor is a
 * {@link StudioPlugin}.
 *
 * <p>Nothing about this interface <em>requires</em> a separate process. A companion plugin declared in an
 * ordinary {@code META-INF/services} file is loaded, constructed and called exactly like a
 * {@link StudioPlugin}, in-process, and that is how the first ones work.
 *
 * <h2>Versioning</h2>
 *
 * <p>Same rule as {@link StudioPlugin}: every method but {@link #id()} is {@code default}, because a
 * plugin's compiled {@code .class} files cannot be rewritten by anybody. A method a plugin has not
 * implemented means "nothing to contribute", never {@code AbstractMethodError}.
 *
 * <p><b>One class may implement both, and should not.</b> Nothing forbids it and the host handles it — a
 * plugin appearing in both sets is asked once and told once, by object identity. But
 * {@link #displayName()}, {@link #toolbarItems()} and {@link #projectClosing()} are declared on both
 * interfaces, so javac refuses to inherit either default and forces all three to be written out; and a
 * class answering questions from two unrelated subjects is the shape this separation exists to undo. The
 * SDK ships two classes: {@code SdkPlugin} for the palette, the editors and the value types, and
 * {@code PilotCompanion} for the Remote Pilot.
 */
public interface CompanionPlugin {

    /**
     * A stable identifier for this plugin, used to attribute a contribution and to order the host's merge.
     *
     * <p>Shares one namespace with {@link StudioPlugin#id()} — an id is a plugin's name, not a name per
     * interface, and one plugin implementing both answers the same string to both. Never shown as-is; see
     * {@link #displayName()}.
     */
    String id();

    /** The name a user reads. Defaults to {@link #id()}. */
    default String displayName() {
        return id();
    }

    /**
     * The buttons this plugin offers on the host's toolbar, as data.
     *
     * <p>Identical in meaning and in constraints to {@link StudioPlugin#toolbarItems()}, including the
     * refusal of {@link ToolbarGroup#STUDIO}: the host owns the grouping, the order, the packing and the
     * overflow menu, and a plugin owns what a press does. The two sets are merged into one bar.
     *
     * <p>For most companion plugins this is the only way in — a companion contributes no palette entry and
     * no editor, so without a button the user has no way to reach it.
     */
    default List<ToolbarItem> toolbarItems() {
        return List.of();
    }

    /**
     * The open project is closing — release anything held on its behalf.
     *
     * <p>Same contract as {@link StudioPlugin#projectClosing()}, and rather more likely to matter here: a
     * companion plugin is the kind that binds a port, starts a nested display or spawns a child process,
     * because those are the things done beside the code rather than to it.
     *
     * <p>Called once per bind, before the plugin's classloader is closed. The instance is reused across
     * projects, so this says <em>this project is over</em>, never <em>you are being discarded</em>. Throwing
     * is caught and reported; the host carries on binding the next project.
     */
    default void projectClosing() {
    }
}
