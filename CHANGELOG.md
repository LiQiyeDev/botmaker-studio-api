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

- **`Assets` — the project's named pictures**, on `StudioServices.assets()`. An editor that lets the user pick
  one of the images a project has saved needs the host's rules for the set, not a directory listing: which
  names are reserved, how one is sanitised, what tags exist, who is told when it changes. A `default` method,
  so a plugin meeting an older host is told there are no pictures rather than dying with
  `AbstractMethodError`. An asset crosses as a **name and a file**, never as an expression — what a plugin
  writes into a bot's source is its own vocabulary.
- **`Capture` grows what the interesting editors actually need**: `pickPoint` (one pixel under a magnifier —
  not `selectRegion` with the size discarded, because at 1:1 the cursor covers what it is choosing);
  `Frame`/`grabTargetFrame` (AWT pixels plus the label of the target they came from, so an editor can *search*
  a frame and say where it came from); `Sample`/`sampleFromTarget` (the eyedropper, carrying the measured
  spread of the pixel's neighbourhood — the honest suggested tolerance, and the number a tolerance slider has
  never had any way to justify); and `SourceChoice`/`chooseSource`/`defaultSource`.
- **A capture source crosses as data, not as an expression.** The host owns the chooser — it enumerates
  monitors, windows and emulator instances and paints the live thumbnails — and the plugin owns the vocabulary
  a choice is written down in. That is the only split under which two plugins can both offer a capture-source
  editor. Read `SourceChoice.kind()` with a `default` arm: the enum may gain a constant.
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
