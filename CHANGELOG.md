# Changelog

What each released version of `botmaker-studio-api` changes, in a few bullets. `ROADMAP.md` stays the
detailed engineering log; this is the short answer, and it is what `release.sh` publishes as the GitHub
Release body.

**`release.sh` refuses to cut a version with no section here** (`check_changelog`, decide pass, before
anything is tagged). If the top section still says `## [Unreleased]`, rename it to the version being cut and
date it.

Sections are `## [x.y.z] — YYYY-MM-DD`, newest first.

**This module's compatibility rule is stricter than any other in the repository, and every entry below should
be read against it:** a plugin's compiled `.class` files cannot be rewritten by anybody, so a change here
that an already-built plugin cannot survive is a **major** change, and one that only a Studio major release
is allowed to make. Additions arrive as `default` methods.

## [Unreleased]

- **Added — `Sources`, reached through `StudioServices.sources()`.** Find and repoint a token sequence across
  the bot's own Java: `find(List<String>)` answers where a needle occurs, `replace(Map, historyLabel,
  reviewNote)` rewrites it in the open buffer *and* on disk and says which files changed. `Sources.NONE` is
  the total no-op a host with no project answers with, so a plugin never null-checks and never asks whether
  rewriting is supported.

  **Needles are matched as tokens, never as text**: `Templates.ORE` matches `Templates . ORE` and does not
  match `Templates.OREX` or `MyTemplates.ORE`; `"images/ore.png"` matches that whole string literal and
  nothing inside a longer one. Tokens rather than a regex on purpose — a regex would hand every plugin the
  power to corrupt a user's source with a bad pattern, and would pin one flavour of regex semantics into a
  surface only a Studio major release may break. Text rather than an AST, equally on purpose: the file that
  most needs a rename to reach it is the one the user has open and half-edited, which does not parse.

  It passes the host-only test on every part: the open buffers are editor state, the walk knows which files
  the bot owns, and the review mark and the history snapshot are the host's own undo model — while what to
  search for stays entirely with whoever owns the thing being renamed. `Use` is **host-constructed only**, so
  it may grow a component later without the `NoSuchMethodError` every already-compiled plugin would take from
  a changed canonical constructor; for the same reason replacements are a `Map<String,String>` rather than a
  record a plugin would call a constructor on.
- **Added — `SlotRun`, reached through `SlotContext.run()`.** Several sibling slots edited as one, for a
  value the author writes as a *run* of arguments — three pictures to match any of. `elements()` are the
  run's Java expressions in order and `replace(List<String>, String...)` writes the whole run, because an
  editor confined to one argument can change an element and never add or remove one. `minimum()` and
  `allowed()` are the host's two narrowings: how few elements the surrounding code still compiles with, and
  the only element sources it will still accept. **Both are opaque Java source** — the host says these
  arguments are one list and what the code around them permits, and the plugin, which knows what the
  strings mean, decodes them itself. `null` for a slot that stands alone, which is nearly every slot.
- **Added — `SlotEditor.preview(ValueContext)`**, `default null`, and a second `SlotEditor.of` overload
  taking it. A small, non-interactive picture of one value, for the one place the host *shows* a value
  without editing it: beside a declared choice, in the list an author picks from. `create` is wrong there
  and so is plain text whenever the stored string is a reference rather than the value — a template name is
  not a picture. Not a new contribution surface: it reuses `matches()`, and its default is exactly what a
  plugin-registered type gets today.
- **Added — `Runs`, reached through `StudioServices.runs()`, and `StudioServices.status(String)`.** The open
  project's bot as a running process: `start`, `stop`, `isRunning`, `pid`, and listeners for run state and
  for telemetry. Every member is host-only for the plainest reason on that interface — the host compiled the
  project, holds its resolved classpath and owns the process — so a plugin can build a feature *around* a
  running bot without the host having to know what the feature is. **Telemetry crosses as one encoded frame,
  not a decoded shape**: the format belongs to whichever runtime the bot is built on, and that runtime
  decodes its own wire, exactly as `SlotContext.currentSource()` passes Java source rather than a syntax
  tree. Bytes rather than text because the wire already is bytes and has one definition; a text rendering
  invented for the contract would be owned by neither end. Both members are `default` and answer `Runs.NONE`
  / do nothing, so a host that runs no bots — the CLI's validator, a test harness — implements neither and a
  plugin never asks whether running is supported.
- **Added — `StudioPlugin.projectClosing()`.** The open project is closing; release anything held on its
  behalf. Called once per bind, on the plugins that were serving the project being left and **before** their
  classloader is closed, so a plugin can still run its own code. It is not a sixth contribution surface —
  it contributes nothing — and it is the one thing a plugin cannot find out for itself: that the project it
  opened a port, a nested display or a child process for is gone. Everything releasable by garbage
  collection needs no implementation. The instance is reused across projects, so this says *this project is
  over*, never *you are being discarded*. `default`, like every method but `id()`.
- **Removed — the scaffold surface.** `com.botmaker.plugin.api.scaffold` (`@Scaffold`, `@ClassName`,
  `@EnumValues`, `@Editable`, `Seeding`), `ScaffoldCatalog`, `ScaffoldEntry`, `ScaffoldPlan` and
  `StudioPlugin.scaffold`/`seedings`. A plugin no longer contributes files to a user's project at all: **a
  project's structure belongs to the user, and a plugin contributes methods a user calls.** A file a plugin
  owns inside somebody's source tree is a file its user cannot freely edit, rename or delete, and the ledger,
  reconciler and rename engine that kept such a file owned were all cost paid to work around that. Never
  released, so no plugin can have compiled against it.
- **Fixed — `ValueCatalog.types()` answers in registration order.** It held its registrations in a
  `Map.copyOf`, whose iteration order is unspecified *and randomised per JVM run*, so every "what type is this
  variable" dropdown came out in a different order each time the host started — contrary to `types()`' own
  javadoc. An unmodifiable `LinkedHashMap` instead. A merge appends rather than reshuffling, so installing a
  second plugin does not reorder the first's types.
- **The toolbar is a contribution surface** — `StudioPlugin.toolbarItems()`, with `ToolbarItem`,
  `ToolbarGroup`, `EnabledWhen` and `ActionContext`. **The plugin contributes data and the host builds the
  node**, deliberately unlike `SlotEditor`, which hands back a `Node`. The difference is expressiveness, not
  consistency: a bespoke image picker cannot be described as data and a button can, and describing it as data
  is what lets the host keep the things a *shared* bar has to own — grouping, ordering, packing, the overflow
  menu, the icon box and the theme. Two plugins returning nodes would produce a bar with two button heights.
  - **The label and the icon are `Supplier`s.** Not a generalisation for its own sake: the host's own bar
    already had two buttons that relabel from project state and a third that resolves a game's real title and
    cover art on a background thread, so a record of `String` would have described a toolbar nobody has. The
    contract's obligation on a plugin is stated where it will be read — cheap and pure, called during layout.
  - **The group is a closed enum the host owns**, and `ToolbarGroup.STUDIO` is **refused** with the plugin
    named. A plugin picks a group and an order within it; it cannot open a section, because a bar whose shape
    depends on install order is a bar nobody can predict.
  - **`EnabledWhen` is a closed set, not a `BooleanSupplier`.** Four states the host already broadcasts, so
    enablement is a switch rather than somebody else's code run once per item per plugin inside a layout pass.
  - **`ActionContext` is three members** and stays that way for the same reason `StudioServices` is five: the
    project's name, this plugin's pinned version, and the host services. Nothing a plugin could answer alone.
- **`StudioServices` is deliberately five members, and the test is strict**: a service belongs there only when
  **the host is the only possible source of it** — which project is open, the theme, the window a dialog is
  owned by, and the screen overlay (`Capture`: `selectRegion`, `pickPoint`, `sampleColor`, `grabFrame`).
  Everything else a plugin does for itself, because `botmaker-shared` is published and any plugin may depend
  on it: enumerating monitors, windows and emulator instances, grabbing their pixels, reading an installed
  library. An `Assets` service and a `Capture.SourceChoice`/`Frame`/`Sample` family were built during
  development and **deleted before this release**; they named one plugin's concepts (a *named picture*, a
  *capture source*) in the contract's own package, which no second plugin could have done. The contract grows
  **capabilities, never vocabularies**.
- **`SlotContext.enclosingSource()` and `replaceEnclosingCall(String, String...)`** — an editor may rewrite the
  whole call its slot sits in, not only the slot. For the case where the choice is a **shape** rather than a
  value: "wait somewhere between 800ms and 2s" is not a different duration from "2s", it is a two-argument
  call where there was a one-argument one. Source text in, source text out, exactly like `replaceWith`; both
  `default`, so an older host leaves the source alone rather than throwing.
- **First cut of the plugin contract.** `StudioPlugin`, `SlotEditor`, `SlotContext`, `TypeRef`,
  `StudioServices` (`Theme`, `Capture`, `Dialogs`, `Region`), and the palette vocabulary under
  `com.botmaker.plugin.api.catalog` (`PaletteCatalog`, `CatalogBuilder`, `Category`, `FacadeEntry`,
  `MemberEntry`, `MemberId`, `MemberRef`, `M0`–`M5`).
- A catalog entry is a **method reference**, not a string and not a class literal: `MemberId` reads the
  declaring class, member name and JVM descriptor out of the reference's `SerializedLambda`, so a catalog
  naming a renamed or deleted member fails the build rather than a menu, and an overload set is resolved
  exactly (`.<Point>add(Mouse::click)`).
- **A facade entry carries its role and its glyph**, not only its category: `FacadeRole` (`MENU` / `HIDDEN` /
  `VALUE`) with `FacadeEntry.isFacade()` / `inMenus()`, an `icon`, `CatalogBuilder.facadeIcon(…)` and
  `PaletteCatalog.withRole(…)`. This is the type-level half of curation — the member list is the other half —
  and it is what lets a host retire a hand-mirrored enum of the plugin's own class list.
- **Present means curated.** A type in a catalog offers exactly the members it lists; a type absent from it is
  not offered. An entry with an empty member list is a verdict, not an omission — how an enum whose constants
  are the whole point is catalogued for its identity without proposing any method.
- **The palette annotations, in `com.botmaker.plugin.api.palette`:** `@Facade` on the type, and
  `@PaletteLabel` / `@PaletteDefault` on the exceptions. Curation is **opt-out** — `CatalogBuilder.addAll()`
  offers every public declared member — and its unit is the member **name**, not the overload: one menu entry
  per name, a lead shape plus a submenu. Their elements are plain `String`s, validated by the processor,
  because an annotation element's type must be visible from the module declaring it — so a contract
  annotation can never take a plugin-defined enum constant.
- **`com.botmaker.plugin.api.meta.@Internal`**, which replaced `@NotInPalette` and says the stronger thing.
  `@NotInPalette` meant only *the menus should not suggest this*; `@Internal` means **not versioned
  surface** — freely breakable, owed no `@Since`, owed no pointer on removal, and never offered. It targets
  types, methods, constructors and **packages**, so a `package-info.java` classifies a whole package at once.
  A type that is both `@Internal` and `@Facade` is a compile error, because offering a member inserts its
  name into a bot's source, which is what makes a type surface. To be recognised without being proposed, use
  `@Facade(role = "HIDDEN")`.
- **The compatibility vocabulary is the contract's, in `com.botmaker.plugin.api.meta`:** `@ReplacedBy`,
  `@Replaces` and `@Since`, joining `@Internal`. They were `com.botmaker.sdk.api.meta`, which made them the
  SDK's rather than every plugin's — while a plugin renaming its own types wants exactly the same machinery,
  and `botmaker-plugin-processor` will check it for any plugin, not only the SDK. Their grammar is unchanged:
  `@ReplacedBy` names `fqn`, `fqn#member` or `fqn#<init>` (`{}` meaning *nothing takes my place*, an explicit
  statement rather than an omission), `@Replaces` names `fqn[#member][(arity)]@<version>` where the version
  is the **last release the old spelling existed in** — the old module's version, when a pointer crosses
  modules. The SDK's spellings survive one minor as `@Deprecated(forRemoval = true)` shims pointing here, and
  the pointer pair therefore carries its own move.
- One dependency, `javafx-controls`, at `provided` scope — a slot editor returns a `javafx.scene.Node`.
  Nothing else, and no parsing library: a plugin writes back Java source as text.
