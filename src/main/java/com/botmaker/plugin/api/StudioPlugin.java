package com.botmaker.plugin.api;

import com.botmaker.plugin.api.catalog.PaletteCatalog;
import com.botmaker.plugin.api.catalog.ScaffoldCatalog;
import com.botmaker.plugin.api.scaffold.Seeding;
import com.botmaker.plugin.api.value.ValueCatalog;
import com.botmaker.plugin.api.value.ValueType;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
 *       text field". They serve both places the host edits a value: a slot in a bot's Java, and a row in
 *       the Parameters window.</li>
 *   <li><b>value types</b> — {@link #valueTypes()}: the types a project variable may hold, and what their
 *       stored text means.</li>
 *   <li><b>parameters</b> — {@link #parameters(String)}: the sections of the Parameters window this plugin
 *       owns, and the generated class each one's values become fields of.</li>
 *   <li><b>toolbar</b> &mdash; {@link #toolbarItems()}: buttons, contributed as data. The host owns the
 *       grouping, the order, the packing and the overflow menu; a plugin owns what a press does.</li>
 *   <li><b>seed files</b> &mdash; {@link #scaffold(String)} and {@link #seedings(String, Path)}: real
 *       compiling classes this plugin ships, and which instances of them a given project wants. The host owns
 *       the parse, the substitution and the path; the plugin owns the file and the data behind it.</li>
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

    /**
     * The value types this plugin registers, with the codec that says what each one's stored text means.
     *
     * <p>This is the surface that makes the vocabulary <b>open</b>. It was a closed enum in the SDK until
     * 2026-08-27, which is right for one plugin and wrong for two: a plugin wanting a {@code Channel}
     * variable would have needed a constant granted in somebody else's enum. Now it declares one.
     *
     * <p><b>The id is the identity, and it is what the project file holds.</b> The host merges every
     * plugin's catalog by {@link ValueType#id()} and refuses two plugins claiming one id, because a project
     * that opens differently depending on which plugin loaded first is not a project. Prefix an id that is
     * not obviously yours. A type whose plugin is absent is not an error either — the value keeps its raw
     * text, renders read-only and declines to emit — so uninstalling a plugin costs the user nothing but
     * the ability to edit.
     */
    default ValueCatalog valueTypes() {
        return ValueCatalog.empty();
    }

    /**
     * The sections this plugin owns in the Parameters window, at the version a project pins.
     *
     * <p>A plugin declares the <em>sections</em>; the user declares the values in them. Each group names a
     * heading, a key the project file files a variable under, and the generated class those variables become
     * fields of — see {@link ParameterGroup}. Returning nothing, the default, means this plugin has no
     * parameters of its own, which is the ordinary case for a plugin that only contributes a palette.
     *
     * <p>The argument is read exactly as {@link #catalog(String)}'s is: the pinned version as the project's
     * pom spells it, interpreted by the plugin alone. A plugin whose parameters class was introduced in a
     * later version may answer nothing for an older pin, and the host will then show that project no section
     * for it — which is the truth, since the jar the bot compiles against has no such class.
     *
     * @param pinnedVersion the version of this plugin the open project depends on; never {@code null}
     */
    default List<ParameterGroup> parameters(String pinnedVersion) {
        return List.of();
    }

    /**
     * The toolbar buttons this plugin contributes.
     *
     * <p>Data, not nodes — see {@link ToolbarItem} for why, and for the rule that the label is a supplier so
     * a button may say what the project currently holds. The host groups, orders, packs, overflows and
     * themes them; this decides only what is offered and what a press does.
     *
     * <p>{@link ToolbarGroup#STUDIO} is <b>refused</b>, with the plugin named. It is the host's own section,
     * for the things that would still make sense with every plugin uninstalled, and an item quietly re-homed
     * out of it would be worse than a refusal: a user reads that part of the bar as the application rather
     * than as their project.
     *
     * <p>Called once when a project's plugins are bound, not on every layout. A plugin whose set of items
     * depends on state should return them all and let a supplier or an {@link EnabledWhen} say which apply.
     */
    default List<ToolbarItem> toolbarItems() {
        return List.of();
    }

    /**
     * The seed files this plugin ships — real, compiling classes in its own build, each written into a
     * project once.
     *
     * <p>The sibling of {@link #catalog(String)}, and deliberately so: a plugin names its seeds as class
     * literals and everything else is read off them, so the list is checked by javac and a renamed seed
     * nobody re-catalogued does not compile. See {@link ScaffoldCatalog} for what is read, and
     * {@link com.botmaker.plugin.api.scaffold} for the marks that say what a host may substitute.
     *
     * <p><b>The file is written once; the marked regions are maintained.</b> A seed is written when the thing
     * it seeds is created and is never written again — from that moment every line of it belongs to the user,
     * including the lines this plugin wrote. What a host may still touch afterwards is exactly what the marks
     * name: the substituted type name and the substituted enum constants, rewritten in place in the user's own
     * file. Everything else, and every {@link com.botmaker.plugin.api.scaffold.Editable} body above all, is
     * theirs for good. A file whose contents <em>as a whole</em> follow from project data is not a seed and
     * must not be shipped as one — describe that data and read it at runtime, because a file rewritten from
     * data is a file its user cannot edit.
     *
     * <p>The argument is read exactly as {@link #catalog(String)}'s and {@link #parameters(String)}'s are:
     * the pinned version as the project's pom spells it, interpreted by the plugin alone. A seed calling API
     * a pinned version does not have would not compile in the project it landed in, so a plugin may answer
     * {@link ScaffoldCatalog#empty()} for a version it does not want to vouch for.
     *
     * @param pinnedVersion the version of this plugin the open project depends on; never {@code null}
     */
    default ScaffoldCatalog scaffold(String pinnedVersion) {
        return ScaffoldCatalog.empty();
    }

    /**
     * Which instances of this plugin's seeds a given project wants, keyed by the seed's <b>unresolved</b>
     * {@link com.botmaker.plugin.api.scaffold.Scaffold#path()}.
     *
     * <p>{@link #scaffold(String)} answers what shapes exist; this answers how many files there are and what
     * goes in them. The two are separate calls because they change on different clocks — a plugin's seeds
     * change when the plugin is released, and a project's instances change every time the user adds something.
     *
     * <p><b>The plugin reads its own data.</b> That is what {@code projectDir} is for and it is the whole
     * shape of this surface: the host knows a project is open and nothing about what is in it, so a plugin
     * storing five activities in a file of its own is the only thing that can say there are five files to
     * write. A host that knew would be a host that had learned one plugin's vocabulary.
     *
     * <p><b>Every instance carries a key, and the key is not the name.</b> See {@link Seeding} — the key is
     * this plugin's own stable identity, and it is what turns a user's rename into a rename rather than into
     * one orphaned file plus one fresh seed written over their work. A plugin whose data has ids should hand
     * those over unchanged.
     *
     * <p>Called whenever the host reconciles a project — on open and after a change — so it must be cheap
     * enough to run often and must not assume it is called once. Answering {@code Map.of()} is the ordinary
     * state of a project that wants none of this plugin's seeds, and is not an error.
     *
     * <p>Nothing here is trusted blindly: the host crosses this with the catalog through
     * {@link com.botmaker.plugin.api.catalog.ScaffoldPlan}, which refuses a name that is not a Java
     * identifier, a duplicate constant, a key no seed declares and two instances resolving to one file — each
     * as a line in {@code problems()} rather than as a throw.
     *
     * @param pinnedVersion the version of this plugin the open project depends on; never {@code null}
     * @param projectDir    the root of the open project — the plugin's own files are under it, and reading
     *                      them is how this question gets answered
     */
    default Map<String, List<Seeding>> seedings(String pinnedVersion, Path projectDir) {
        return Map.of();
    }
}
