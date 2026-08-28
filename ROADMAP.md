# ROADMAP — botmaker-studio-api

The running engineering log. `CHANGELOG.md` is the short, per-release answer; this is the detail and the
reasoning.

## Done

### 2026-08-29 — the toolbar surface, and the one member that could not be a String

Plugin platform, phase 13a. `ToolbarItem`, `ToolbarGroup`, `EnabledWhen`, `ActionContext` and
`StudioPlugin.toolbarItems()`. The fifth contribution surface, and the first one where the plugin hands over
**data** rather than a `Node`.

- **Why data here and a `Node` for `SlotEditor`.** A bespoke image picker cannot be described as a record; a
  button can. What describing it buys is everything a *shared* bar has to own centrally — the group, the
  order, the separators, the packing, the overflow menu, the icon box, the theme. A surface returning nodes
  would hand all of that to each plugin, and two plugins would produce a bar with two button heights.
- **The label is a `Supplier<String>`, and so is the icon.** This is the one place the sketch was wrong and
  the correction is the useful part: the host's own bar already had a Capture Target button that relabels
  from project state, a Launch Target button that does the same *and* resolves a game's real title and cover
  art on a background thread. A record of `String` would have described a toolbar nobody has. The cost is an
  obligation on the plugin — cheap and pure, called during layout on the FX thread — stated in the javadoc
  where somebody will actually read it.
- **A fourth contributable group, `TOOLS`, was added while porting the host's own bar.** With
  `PROJECT`/`AUTHORING`/`RUN` alone, today's hand-arranged reading order could not be reproduced: the
  instruments (Input, Templates, Overlay, Record, Resources) sit *after* the run cluster. That the four
  groups reproduce the existing sequence exactly was the acceptance test for the port, and it is also the
  argument that the groups are real rather than invented.
- **`ToolbarGroup.STUDIO` is refused, not re-homed.** The host's own section, for what would still make sense
  with every plugin uninstalled. A plugin quietly moved out of it would sit where a user reads the
  application rather than their project, so the drop is loud and names the plugin.
- **`EnabledWhen` is a closed set rather than a predicate**, and the reason is cost rather than taste: a
  `BooleanSupplier` is somebody else's code called once per item per plugin on every state change, inside a
  layout pass, with no way for the host to know what it reads. Four states the host already broadcasts make
  it a switch. An item wanting more says why in a dialog when pressed — better than a greyed button with no
  explanation.
- **No toggle kind and no read-out kind**, though the host has one of each. Adding a contract member for the
  host's only instance of a thing is exactly what the `Assets`/`SourceChoice` reversal recorded, so Studio's
  debug toggle and resolution label stay hand-built nodes placed beside the described ones.
- **`ActionContext` is three members**, on `StudioServices`' rule: the project's name (a name, not a path —
  a path invites writing into a project directory behind the host's back), the plugin's own pinned version,
  and the services. Nothing a plugin could answer for itself.

**What this surface does not yet carry, and it is the whole of phase 13:** the SDK contributes no items
through it. Its eleven buttons are still Studio's, because their actions are Studio dialogs on Studio's
project services, and moving the *declaration* without the *behaviour* would need an intent registry — which
was offered and declined. The dialogs move instead, once phase 14 gives them a project surface to stand on.

### 2026-08-28 — the GitHub Release is published from here, by JReleaser

- **`jreleaser.yml` and a `release` job in `ci.yml`.** A `v*` tag now publishes this module's GitHub
  Release from its own CI, with the `## [x.y.z]` section of `CHANGELOG.md` as the body. It used to be a
  `gh release create` inside the umbrella's `release.sh`, which keeps everything JReleaser cannot express —
  which modules are being cut, at what versions, in what order, and the tag itself. JReleaser's unit of
  work is one repository; the umbrella's is eight with a dependency order between them.
- **`tools/changelog-section.sh`** — the extractor, moved out of `release.sh` into this repository so that
  the two readers which must not disagree can both reach it: the umbrella's `check_changelog` gate calls it
  before anything is tagged, and the workflow calls it for the notes. A release whose body is extracted by
  a different rule than the one that gated it can pass the gate and then publish something else.
- **Two findings worth keeping, because each reads as a configuration mistake until you hit it.** JReleaser
  **cannot open a submodule**: in the umbrella working copy `.git` is a `gitdir:` FILE and its JGit reports
  *repository not found*, while `--git-root-search` gets past that only by resolving the **umbrella**
  repository — which would attach a module's release to the wrong repo. Hence CI, where a checkout is
  standalone. And `jreleaser-maven-plugin` is not a way round it: it ignores `jreleaser.yml` entirely (its
  model comes from an XML block in the pom) and takes the version from `<version>`, which here is the
  cosmetic `0.0.0-SNAPSHOT` JitPack overrides. The version arrives as `JRELEASER_PROJECT_VERSION`, read off
  the tag.
- **The build is untouched** — no Maven plugin, no lifecycle binding, no pom edit.
- Also found, and now refused in `release.sh`'s decide pass rather than halfway through a run: **this
  repository has no `origin` remote.** It was created inside the umbrella and has never been pushed. Create
  `LiQiyeDev/botmaker-studio-api` before any `--studio-api` release.

### 2026-08-27 — reflection replaces the processor, japicmp replaces the back edge

The annotation set narrows to five and the catalog stops being generated.

- **`@Facade` → `@Palette`** (the `role` element deleted) and **`meta.@Internal` → `palette.@Hidden`**,
  narrowed back to what it actually did. `FacadeRole`, `CatalogBuilder`, `MemberRef` and `M0`–`M5` deleted;
  `FacadeEntry.role` is a `boolean offered` and `isFacade()`/`inMenus()` are gone. **Two bits, not three:**
  every consumer read one, and `VALUE` only ever existed to work around `@Internal` welding *not-surface* to
  *not-offered*. All four palette annotations are `RUNTIME` now, because the plugin reflects on its own.
- **`PaletteCatalog.of(Class<?>...)`** is the entry point. Members are discovered, so the property the
  method-reference builder was defended on — *a catalog naming a renamed member does not compile* — answers
  a problem that no longer exists; the class list stays javac-checked as class literals. It also removed a
  cost only an outside plugin author would have paid: a pom omitting `<annotationProcessorPaths>` got no
  catalog and no diagnostic.
- **`SourceOrder`** (package-private, ~150 lines) parses the class file's constant pool and `methods` table
  to recover declaration order — the one processor capability reflection lacks, and what let the switch
  reproduce the SDK's generated menus exactly (52 facades, same order, every `.order(…)` prefix). Every
  failure path returns an empty list and the caller sorts alphabetically.
- **Load-time validation degrades rather than throws**, collected into `PaletteCatalog.problems()`: two
  `@PaletteDefault`s on one name, a `@PaletteLabel` on a `@Hidden` member, two facades disagreeing about a
  category label, a class with no `@Palette`, and a facade whose members cannot be read at all. That last one
  was found by hitting it — `getDeclaredMethods()` throws `NoClassDefFoundError` when a signature names a
  class the classloader lacks, and it is all-or-nothing, so the facade is reported and offered with no
  members. Precedent: `ValueCatalog.merge`.
- **Constructors are deliberately not catalogued** — reflecting them added an `<init>` entry to seven offered
  static facades. `MemberId` keeps its constructor support.
- **`@Replaces` and `@Since` deleted**; `@ReplacedBy` stays and stays `CLASS`-retention, since Studio reads it
  from a jar it never loads. **japicmp on `verify`**, unconditional, no ignore list: the record-component trap
  from `25-compatibility.md` is checked by something now. The August objection to japicmp — *CI cannot tell an
  intended break from an accident because it cannot see the version* — is about a conditional rule; both new
  uses are unconditional.

### 2026-08-27 — the host services grow to fit the editors (plugin platform, phase 12a)

Phase 12 moves the SDK's thirteen slot editors out of Studio. Surveying them first found that **four cannot
move without this module growing**, and that is the honest way to read every addition below: they are not a
guess at what a plugin might want, they are what an editor already written could not do without.

- **`Assets`**, on `StudioServices.assets()` — the project's named pictures. The `ImageTemplate` and
  `ImageTemplateGroup` editors reached `ImageTemplateLibrary`, `ProjectConfig`, `ProjectSettingsService` and
  `CoreApplicationEvents` for it. It is a service and not a directory listing because **naming one is policy
  the host owns**: reserved names, sanitising, collisions, tags, and who is told when the set changes. Two
  plugins walking `resourcesDir()` themselves would answer all of that differently about one folder.
- **`Capture.pickPoint`** — one pixel under a magnifier. Deliberately not `selectRegion` with the size thrown
  away: at 1:1 the cursor covers the pixel it is choosing.
- **`Capture.Frame` + `grabTargetFrame`** — the AWT pixels and the label of the target they came from.
  `grabFrame` hands over a JavaFX `Image`, which is right for *showing* a frame and useless for *searching*
  one, and a frame with no label leaves the user guessing which monitor was read. Never a silent desktop
  fallback: a frame of the wrong thing answers a question the editor did not ask.
- **`Capture.Sample` + `sampleFromTarget`** — the eyedropper, carrying `spread`. That number can only be
  measured at the moment of sampling (it is how much the chosen pixel's neighbourhood varies) and it is the
  honest suggested tolerance — the justification a ΔE slider has never had.
- **`Capture.SourceChoice` + `chooseSource` + `defaultSource`** — and this one is the design decision worth
  keeping. **A capture source crosses as data, never as an expression.** The host enumerates monitors, windows
  and emulator instances and paints the thumbnails; the plugin decides what a choice is *written down as*. The
  alternative — the host handing back `CaptureSource.window("…")` — would bake one plugin's vocabulary into the
  contract and make a second capture-source editor impossible.

Two shape decisions inside that:

- **`SourceChoice.Kind` is an enum component of a record, not a sealed hierarchy.** A new permitted subtype
  breaks a plugin's `switch` at recompile; a new enum constant with a documented `default` arm does not. The
  Javadoc says to read it with one.
- **`assets()` is `default`; the new `Capture` methods are not.** The rule that every addition must be
  `default` protects plugins from an older *host*, and `StudioServices` is what a plugin holds — so it gets the
  default. `Capture` is host-implemented only; an abstract method there breaks a host at compile time, which is
  a build failure in this repository rather than a user's plugin dying at runtime.

### 2026-08-27 — parameters become a plugin surface (plugin platform, phase 11)

`ValueContext` is new, and it is the AST-free half of what an editor needs: `TypeRef type()`,
`List<String> value()`, `void set(List<String>)`, `StudioServices services()`. `SlotContext` **extends** it
with the source-text half and its `slotType()` is gone — the inherited `type()` was the same question asked
twice. `SlotEditor` widened from `SlotContext` to `ValueContext` in all three of `matches`, `create` and
`of`, and that widening is the whole point: **one editor now serves both the code editor's argument slot and
the Parameters window**, because both are "a value of a Java type", and a plugin writes the predicate once.
`asSlot()` is how an editor that genuinely needs the call site asks for it — null when there is none.

`ParameterGroup(id, title, className)` is new, and `StudioPlugin` gains
`default List<ParameterGroup> parameters(String pinnedVersion)` — empty by default, so the contract's rule
that every method but `id()` is `default` is intact. A group is **one plugin's section of the Parameters
window and one generated file**, which is the whole-file-ownership rule applied to parameters: two plugins
may both offer a `timeout` because they are fields of two classes. `DEFAULT_ID` is `""`, so a project
written before groups existed reads back as the default plugin's — no migration, and the discriminator is
absent rather than wrong.

Nothing here carries a JSON annotation, and that is deliberate: the contract's one dependency is
`javafx-controls` at `provided`, and a serialiser added here would be imposed on every plugin ever written.
The contract declares the wire *form*; whoever owns the file supplies the parser (the SDK's `ValueJson`).

### 2026-08-27 — a value type says where a picker files it (plugin platform, phase 10b)

`ValueType.group()` — a free `String`, `""` for the top level — and `Builder.group(…)`. Studio's picker used
to read the grouping off `BotType.Group`, an enum in Studio, which is exactly the constant a second plugin
could never be granted: a plugin's own types would have arrived in a menu with nowhere to sit. A free string
for the same reason the class is not an enum.

The ordering rule is worth stating because it is the one a second plugin could otherwise use to disturb the
first: types carrying the same group are shown together in **registration** order, and the **first**
registration of a group decides where that group sits. A plugin cannot reorder another's menu by naming its
heading.

### 2026-08-27 — the compatibility vocabulary arrives (plugin platform, phase 8c.4)

`@ReplacedBy`, `@Replaces` and `@Since` are `com.botmaker.plugin.api.meta` now, beside `@Internal`. They were
`com.botmaker.sdk.api.meta`, which made them the SDK's rather than every plugin's — compatibility trap #8:
a second plugin renaming its own public types had no equivalent and its users' bots broke with a bare compile
error. `botmaker-plugin-processor` already checked them by FQN string, so the move is what makes that
checking usable by anybody.

- **The SDK's three survive one minor** as `@Deprecated(forRemoval = true)` shims with `@ReplacedBy` at the
  new FQNs, so the pointer pair's first use is its own move. Nothing about the grammar changed.
- **A pointer may name a target in another module** — which the move is the first instance of. The
  `@version` on such a `@Replaces` entry is the **old** module's: the last release *that* module shipped the
  old spelling in, because the entry is a statement about a spelling being retired.
- **A scan that folds meta-annotations must filter to direct ones.** These three annotate each other now, so
  ClassGraph reports `@Since`'s own `@Replaces` on every element that merely uses `@Since`. `javax.lang.model`
  has no such folding, so the processor is unaffected; a host reading pointers out of a jar is not.

Two earlier arrivals in this module are logged in `../botmaker-sdk/ROADMAP.md` rather than here, because the
work that moved them was the SDK's: the palette annotations and `@Internal` (phases 8 and 8c.2), and the
value vocabulary under `com.botmaker.plugin.api.value` (phase 10a).

### 2026-08-26 — the module exists (plugin platform, phase 5)

Created as the seventh BotMaker repository, first in the umbrella reactor. It holds the contract that lets
Studio become a **plugin host** and `botmaker-sdk` become plugin #1 — a privileged default plugin, but a
plugin, with no back door into the host.

**Why a separate repository rather than a package in the SDK or in Studio.** The contract must be allowed to
version *slower* than either. A bot's source can be rewritten when the SDK changes, because Studio holds an
AST of it; a plugin's compiled `.class` files cannot be rewritten by anybody. Putting the contract in the
SDK would tie its cadence to the SDK's, which changes every week, and a third-party plugin would then depend
on the whole SDK to implement two interfaces.

**What landed:**

- `com.botmaker.plugin.api` — `StudioPlugin` (every method but `id()` a `default`), `SlotEditor`,
  `SlotContext`, `TypeRef`, `StudioServices`, `Theme`, `Capture`, `Dialogs`, `Region`.
- `com.botmaker.plugin.api.catalog` — `PaletteCatalog`, `CatalogBuilder`, `Category`, `FacadeEntry`,
  `MemberEntry`, `MemberId`, `MemberRef`, `M0`–`M5`.
- `CatalogBuilderTest`, 13 tests, all on the one claim the design rests on: that a method reference carries
  enough identity to name an *overload*.

**The decisions worth not re-litigating**, each taken against a named alternative:

- **A slot editor returns a `javafx.scene.Node`**, so the platform is pinned to JavaFX permanently. The
  alternative was a UI-factory abstraction; the editors worth writing (image template, capture source,
  launch target) are bespoke, and a factory able to express them would be larger than JavaFX.
- **A catalog entry is a method reference**, read through `SerializedLambda`. The alternatives were strings
  (nothing checks them) and class literals plus strings (the maintainer's objection: *"I don't like
  literals"*, and the same non-checking on the member half). The reference is checked by javac.
- **One void-returning shape per arity, `M0`–`M5`.** A value-returning shape beside `M1` would make `add`
  ambiguous for most real members, since a reference to a value-returning method is compatible with a
  void-returning interface. Ambiguity within one arity is the caller's, via a type witness.
- **No shaded plugin toolkit** — with the contract at interfaces and records, there is nothing to shade
  against.
- **Panels are not a surface.** Plugins contribute to the editor; they do not contribute editors.
- **No flatten-maven-plugin and no `.deps.env`**, unlike session and the SDK: this module pins no BotMaker
  upstream, so there is nothing to inject and nothing to bake.

## Deferred / next

- **The SDK's per-version catalog** (phase 6) — one frozen class per released `SdkVersion`, built as the
  previous one plus deltas, plus a `release.sh` gate: editing an already-released catalog is only possible
  when a member it names is deleted, so the edit *is* the removal signal.
- **Studio reads the catalog** (phase 7) — `SdkSurfaceService` curation switches over and `palette/SdkType`
  retires.
- **The slot editors move into the SDK** (phase 8) — thirteen of them, plus the Steam/Epic scanners moving
  to `botmaker-shared`.
- **Dynamic plugin loading** — `ServiceLoader` over a `URLClassLoader` built from the resolved artifact.
  A loader, not a redesign, and worth writing once a second plugin exists.
- **A dockable side-panel surface** — one interface (a title and a `Node`), the cheapest honest answer if a
  plugin ever needs to *show* something. Full editor views stay out.
- **Nothing here has a second implementor yet**, and one implementor proves little. The contract's real
  validation is a second plugin; until then the mitigation is that the SDK consumes it as an ordinary
  plugin.
