package com.botmaker.plugin.api;

import com.botmaker.plugin.api.catalog.PaletteCatalog;

import java.util.List;

/**
 * What a BotMaker Studio plugin is, from the host's side: an id and a set of contributions.
 *
 * <p><b>Every method but {@link #id()} is {@code default}, and that is the versioning rule of the whole
 * platform.</b> A bot's own source can be rewritten when the SDK changes — Studio holds an AST of it — but a
 * plugin's compiled {@code .class} files cannot be rewritten by anybody. So this contract may only gain
 * members that older plugins can ignore. A method a plugin has not implemented returns "nothing to
 * contribute" rather than throwing {@code AbstractMethodError}, which means a plugin built against an
 * earlier release of this module keeps working across every Studio release until a Studio <em>major</em>
 * bump explicitly refuses it.
 *
 * <h2>The contribution surfaces</h2>
 * <ul>
 *   <li><b>palette</b> — {@link #catalog(String)}: the types and members worth proposing, their groups and
 *       their order.</li>
 *   <li><b>slot editors</b> — {@link #slotEditors()}: "for a value of type X, show this UI instead of a
 *       text field".</li>
 *   <li><b>generation</b> — not declared here. A plugin that generates project files owns whole files keyed
 *       by their project-relative path, and contributes them through its own authoring entry point.</li>
 * </ul>
 *
 * <p><b>Panels are deliberately not a surface.</b> A plugin contributes to the editor; it does not
 * contribute editors. The Activity Canvas and every other whole view stays the host's.
 */
public interface StudioPlugin {

    /**
     * A stable identifier for this plugin — {@code "botmaker-sdk"} — used to attribute a contribution and to
     * order the host's merge. Never shown as-is; see {@link #displayName()}.
     */
    String id();

    /** The name a user reads. Defaults to {@link #id()}. */
    default String displayName() {
        return id();
    }

    /**
     * What this plugin offers the palette at the version a project actually pins.
     *
     * <p>The argument is the pinned version <em>as it is written in the project's pom</em> — the plugin
     * decides what that string means, since only it knows its own versioning. A plugin that recognises no
     * such version, or that does not curate at all, returns {@link PaletteCatalog#empty()}, which the host
     * reads as "offer everything the jar contains" rather than "offer nothing".
     *
     * @param pinnedVersion the version of this plugin the open project depends on; never {@code null}, but
     *                      may be a snapshot or a spelling this plugin does not recognise
     */
    default PaletteCatalog catalog(String pinnedVersion) {
        return PaletteCatalog.empty();
    }

    /**
     * The editors this plugin offers for value slots, in the order it wants them consulted.
     *
     * <p>Order matters only within one plugin: the host consults its own editors before any plugin's, so a
     * slot holding a project variable stays a variable no matter what a plugin claims about its type.
     */
    default List<SlotEditor> slotEditors() {
        return List.of();
    }
}
