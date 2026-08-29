# CLAUDE.md

Guidance for working in **botmaker-studio-api**, the contract a BotMaker Studio plugin compiles against.

Read the umbrella `../CLAUDE.md` first for how the six modules fit together, and
`../docs/refactor/24-plugin-platform.md` for why this module exists at all.

## What this module is

Interfaces and records. Nothing else. It has no implementation, references no Studio type, and depends on
one artifact (`javafx-controls`, `provided`).

- `com.botmaker.plugin.api` — `StudioPlugin`, `SlotEditor`, `SlotContext`, `ValueContext`, `TypeRef`,
  `StudioServices` and the three services it exposes (`Theme`, `Capture`, `Dialogs`) plus `Region`.
- `com.botmaker.plugin.api.catalog` — `PaletteCatalog`, `Category`, `FacadeEntry`, `MemberEntry`,
  `MemberId`, and the package-private `SourceOrder`, plus `ScaffoldCatalog`, `ScaffoldEntry` and
  `ScaffoldPlan`. The *result* types: `PaletteCatalog.of(Class<?>...)` and `ScaffoldCatalog.of(Class<?>...)`
  build them by reflection. `CatalogBuilder`, `MemberRef` and the arity shapes `M0`–`M5` were deleted on
  2026-08-27 — see *The catalog* below.
- `com.botmaker.plugin.api.scaffold` — **`@Scaffold`, `@ClassName`, `@EnumValues`, `@Editable`** and the
  record `Seeding`: the surface by which a plugin puts a file into a *user's project*. See *Seeds* below.
- `com.botmaker.plugin.api.value` — the **value vocabulary**: `ValueType`, `ValueShape`, `ValueChoice`,
  `Visibility`, `Range`, `ValueCodec` and `ValueCatalog`. What a bot's *variable* can be, which is a question
  the contract answers so a plugin can own a type without the SDK granting it one.
- `com.botmaker.plugin.api.palette` — **`@Palette`**, **`@Hidden`**, `@PaletteLabel`, `@PaletteDefault`: the
  marks a plugin puts on its own classes, read **at runtime by `PaletteCatalog.of`**. All four are
  `RUNTIME` since 2026-08-27, because the plugin itself reflects on them. Their elements are plain `String`s
  on purpose — an annotation element's type must be visible from the module *declaring* the annotation, so a
  contract annotation can never take a plugin-defined enum constant.

  **Two bits, not three.** `@Palette` = **catalogued** (the recognition set: imports, "does `Point` mean this
  plugin's or `java.awt`'s"). `@Hidden` on the type = **not offered** in an insert menu; on a member = that
  member is not offered. `FacadeRole{MENU,HIDDEN,VALUE}` is deleted: nothing ever distinguished its second
  state from its third, and `VALUE` existed only to work around `@Internal` welding *not-surface* to
  *not-offered*. `@Facade` → `@Palette` (no `role`), `meta.@Internal` → `palette.@Hidden`.
- `com.botmaker.plugin.api.meta` — **`@ReplacedBy` alone**, which arrived from the SDK's `api.meta` in 1.2.0
  because a plugin renaming its own types wants exactly the machinery the SDK already had. A pointer may name
  a target in **another module** — the SDK's shims point here. It is the one annotation still `CLASS`
  retention, and for a reason: **Studio** reads it out of a jar it never loads, and `CLASS` keeps it out of
  every running bot's reflection data.

  **`@Replaces` and `@Since` were deleted on 2026-08-27.** The back edge existed because Studio holds only
  two jars at upgrade, so a bot skipping a release could not see a pointer added on an element later deleted.
  japicmp now enforces **never-delete** on `com.botmaker.sdk.api.**`, so the deprecated element and its
  forward pointer are both still in the target jar and `@ReplacedBy` alone answers every upgrade, chains
  included. `@Since` went by this repo's standing test for a gate: *the question must not already be answered
  by bytecode*.

**These are annotations, not implementation**, so they do not breach the *interfaces and records* rule above.

Studio is the host; `botmaker-sdk` is the first plugin — a *privileged default* plugin, but a plugin, with
no back door. That is what makes the contract honest: if the SDK needs something this module does not
expose, the contract is wrong, not the SDK.

## What may go on `StudioServices` — the host-only rule

**A service belongs here only when the host is the *only* possible source of it.** Not when the host happens
to have written it first, and not when a real editor needed it — that was the old test, and it is what let
the contract grow a vocabulary belonging to one plugin.

Four things pass: **which project is open** (`projectDir`/`resourcesDir`), **the theme the user chose**,
**the window a dialog is owned by**, and **the screen overlay** — an overlay goes over every window on the
screen including the host's own, hides the editor that opened it, and comes back on the right thread. Nothing
else does. A plugin can enumerate monitors, windows and emulator instances, grab pixels from any of them and
read a launcher's installed-game library, because **`botmaker-shared` is published on JitPack and any plugin
may depend on it** — so nothing shared can do is a privilege, and the SDK gets no advantage from being built
in this repository. The files under `resourcesDir()` are ordinary files.

**Six members were deleted on 2026-08-27 for failing this test**, all added weeks earlier because the SDK's
editors wanted them: the whole `Assets` interface and `StudioServices.assets()` (the project's *named
pictures* — that is `ImageTemplate`'s concept), and `Capture`'s `SourceChoice`, `Frame`, `Sample`,
`chooseSource`, `defaultSource`, `grabTargetFrame` and `sampleFromTarget` (a *capture source*, and a sampled
colour with its tolerance — `CaptureSource`'s and the vision API's). A third set never landed: `Launcher`,
`GameChoice`, `EmulatorChoice` and two `Dialogs` methods over them, written and reverted the same day. Each
was justified at the time as "the host owns the policy, the plugin owns what it is written down as" — the
`SourceChoice` split — and the flaw in that reasoning is that **the host only owned the policy because
Studio was written first**. A second plugin could not have added its own `Assets`, so the SDK was reaching
through the contract for its own API. That is the back door this module exists to close.

When a plugin's editor needs something the host has and the contract does not expose, the question to ask is
*could any plugin have built this on shared plus its own files?* If yes, it builds it. If no, and only then,
the contract grows — and it grows a **capability**, never a vocabulary: nothing here may name a concept that
belongs to some plugin's API.

**One member has passed that test since, and it is the worked example of what a capability looks like.**
Phase 12c (2026-08-28) added `SlotContext.enclosingSource()` and `replaceEnclosingCall(String, String...)`,
because a duration editor cannot otherwise offer "wait a random amount": the SDK's humanized wait is
`Wait.between(min, max)` where the fixed one is `Wait.time(x)`, so ticking that box is a change to the
**call**, not to the value in the slot. It passes on both halves of the rule — the bot's syntax tree is
something only the host has, and the signature takes and returns Java source text exactly as `replaceWith`
does, naming no type of anyone's. Compare it with `Capture.SourceChoice`, which also arrived because a real
editor needed it and which named `CaptureSource`'s concept: *a real editor needed it* is not the test, and
never was.

## The three rules that are easy to break

**1. Every method but `StudioPlugin.id()` is `default`, and stays that way.** A bot's source can be
rewritten when the SDK changes — Studio holds an AST of it. A plugin's compiled `.class` files cannot be
rewritten by anyone. So a new member here that an already-built plugin cannot survive is a **major** change,
and only a Studio major release may make one. This is the whole reason the module is separate from the SDK:
it must be allowed to move slower.

**2. Nothing from a plugin may cross as a `Class<?>` the host is expected to load.** The host resolves types
out of the *bot's* classpath, not its own, so a type may be a different version of itself or absent
entirely. `TypeRef` crosses as names and compares by fully-qualified name; that is the comparison that is
actually true across two classloaders. (Inside a catalog a `Class<?>` *is* used — but the plugin holds it,
and it is the plugin's own class.)

**3. No syntax tree, in either direction.** `SlotContext.currentSource()` is a `String` and
`replaceWith(String, String...)` takes one. This was not a simplification imposed on the host — it is what
the host already did, since fifteen of its nineteen built-in editors handed back source text and let it
re-parse. Keeping it that way is what stops the host's parser from becoming plugin surface.

## The value vocabulary, and why `ValueType` is not an enum

`ValueType` is a **final class whose identity is its persisted `id()`**, reached through `ValueType.of(id)`
and registered in a `ValueCatalog`. An enum would have been shorter and is wrong for the same reason the whole
module exists: a plugin wanting a `Channel` variable would need a constant added to somebody else's enum.

- **Compare by `id()`, never by object identity.** Two plugin classloaders each hold their own copy of a
  class; the id is what a project file holds and the only comparison that is true across them.
- **`ValueType.unknown(id)` is a supported state, not an error path.** A type nothing registered keeps its raw
  `List<String>`, renders read-only and declines to emit. That is what a project opened without one of its
  plugins looks like, and refusing the file or coercing the value would destroy a user's data because a jar is
  missing. An **absent** id is different and reads as text — a field older than the vocabulary.
- **`ValueCodec<T>` is per item.** Shape (`ONE`/`ONE_OF`/`ANY_OF`/`OPEN_LIST`) is composed above it by
  `ValueCatalog.initializer`, so a codec is written once and serves all four. `T` never crosses to the host —
  only `literal(parse(wire))` behind a wildcard capture — which is what keeps rule 2 above true for values.
- **`ValueCatalog.merge` is left-biased and never throws.** Deliberately unlike a generation collision, which
  is a hard error: refusing to merge would break every project that has a plugin installed.
- **No Jackson here, and none is coming.** The vocabulary declares the wire *form* — an id out, a total
  factory back — and whoever owns the file supplies the parser (the SDK's `internal/authoring/ValueJson`).
  Adding a JSON library to this module would impose it on every plugin and tie the contract to its
  compatibility rate.
- **`ValueType` and `ValueCatalog.Entry` are classes with builders, not records**, for trap #2 of
  `../docs/refactor/25-compatibility.md`: adding a component to a public record changes its canonical
  constructor descriptor, which is `NoSuchMethodError` in every already-compiled plugin. `ValueChoice` and
  `Range` *are* records and are therefore **frozen** — their components may not grow.

## The catalog, and why it is reflection

`PaletteCatalog.of(Mouse.class, Keyboard.class, …)` — one class literal per facade, and **members are
discovered, never named**. Every public declared method of a `@Palette` class is offered unless something on
it says otherwise, grouped by name, lead shape chosen by `@PaletteDefault` or else fewest parameters, labels
from `@PaletteLabel`, whole name dropped if any overload is `@Hidden`.

**It used to be method references** — `Mouse::click` through a `MemberRef extends Serializable` and a
`SerializedLambda`, built by `CatalogBuilder` with one arity shape `M0`–`M5` per parameter count. All of that
was deleted on 2026-08-27 along with `botmaker-plugin-processor`, and the property it was defended on does
not need saving: *a catalog naming a renamed member does not compile* was answering a problem that only
exists when something names members. Nothing does now. What stays javac-checked is the **class list**,
because it is written as class literals.

The processor also cost something a plugin author outside this repository could not pay: a pom that omitted
`<annotationProcessorPaths>` got no catalog, and nothing said why. Reflection needs no build configuration.

**Three things about `of` that are decisions rather than details:**

- **It degrades, never throws.** Two `@PaletteDefault`s on one name, a `@PaletteLabel` on a `@Hidden` member,
  two facades disagreeing about one category's label, a class with no `@Palette`, a facade whose members
  cannot be read at all (`LinkageError` from an optional dependency the host did not resolve) — each is
  collected into **`problems()`** and the rest of the catalog is built. The precedent is `ValueCatalog.merge`
  and the rule behind both is the same: **no malformed catalog may be the reason a project will not open.**
- **Member order is the class file's, not `getDeclaredMethods()`'s.** javac writes the `methods` table in
  source order and reflection promises nothing, so `SourceOrder` parses the class file's constant pool and
  methods table to recover the author's ordering — the one processor capability reflection alone lacks, and
  the reason the switch reproduced the generated catalog's menus exactly. Every failure path returns an empty
  list and the caller sorts alphabetically: the worst case is a cosmetic menu order.
- **Constructors are not catalogued.** Reflecting them put an `<init>` entry under seven *offered* static
  facades whose public constructor exists only because nobody wrote a private one, and a palette entry
  inserts a call. `MemberId` keeps `of(Constructor)` and `CONSTRUCTOR` for a plugin that wants one.

**What a catalog does not answer: presence.** It describes the build it was reflected from, not the jar the
bot resolves — so whether a member exists in the jar a bot actually pins stays with the host's ClassGraph
scan of that jar, and the catalog answers only curation, order and labels. The two compose as an
intersection, which fails in the safe direction: an old pin may be offered slightly less than it truly had,
never more.

## Seeds — how a plugin writes a file into a user's project

The replacement for `botmaker-sdk`'s `SourceEmitter`, which built nine `.java` files as Java strings that
nothing checked until somebody ran the generator. A **seed** is a real class in the plugin's own build,
marked with what a host may substitute, so it is checked by javac on every build of the plugin that ships it.

**Two calls, because they change on different clocks.** `StudioPlugin.scaffold(pin)` → `ScaffoldCatalog`
answers *what shapes exist* and changes when the plugin is released. `StudioPlugin.seedings(pin, projectDir)`
→ `Map<String, List<Seeding>>` answers *which instances this project wants* and changes every time the user
adds something. `ScaffoldPlan.of` crosses them.

**One seed is one shape; a project wants many files from it.** That is why `Seeding` nests its values inside
the instance rather than the plugin returning one key→values map: `"outcomes"` means *this file's* outcomes,
so the compound key that a global map would force — the key having to encode which activity it belonged to —
never has to exist.

**`Seeding.key` is not `Seeding.name`, and the distinction earns its keep exactly once.** The key is the
plugin's own stable id. When a user renames the thing a seed seeded, a host matching on the *name* sees a
file that vanished and a file that appeared, and writes a fresh seed over somebody's work; matching on the
key, it finds the file it wrote for that key and performs a rename — the type, the file, and the references
in the user's own source. Nothing else needs the key, and nothing else should.

**The file is written once; the marked regions are maintained.** This is the amendment the first draft of
the javadoc needed, and both halves are load-bearing. Write-once alone cannot hold — a user adding an outcome
on the flow canvas would get a file whose enum no longer lists it, and a project that does not compile.
Maintained-everywhere cannot hold either — that is regeneration wearing a new word, and it destroys the body
the user opened the file to write. `SourceEmitter.stub()` had already conceded this in its own javadoc
("SEED — with one exception: `Outcome`"), and Studio's `ActivityStubSync` is the existing hardcoded
implementation of it.

**Intent and addressing are two different jobs, and this module only does the first.** Reflection knows a
type or a member exists and nothing whatever about where its text sits, so the annotations carry intent and
the host parses `ScaffoldEntry.source` to locate it. That is rule 3 above applied to seeds, and it is why
`ScaffoldPlan` validates only what is answerable without a parser: an identifier is a keyword or it is not,
two constants are equal or they are not, two paths collide or they do not. Everything needing the project —
does this path collide with another plugin's file, does this class name collide with a type in the user's
source, does the file already exist — stays with the host.

**Use `javax.lang.model.SourceVersion`, not a keyword list.** `ScaffoldPlan` validates names and constants
with it. There are already three hand-rolled keyword lists in this project (Studio's `VariableNames`,
`FunctionDraft`, and a partial one elsewhere); do not add a fourth, and note that `SourceVersion` also covers
the three cases such a list always misses — `true`, `false` and `null` are literals rather than keywords, and
none of them is a name. It lives in the `java.compiler` module, which is present: Studio's jpackage build
bundles a **full** JDK runtime.

**Reflection promises no order, and this cost a test failure worth remembering.** `enums()` and `editable()`
read `getDeclaredClasses()`/`getDeclaredMethods()`, whose order is unspecified — so which of two enums
claiming one key "won" varied by JVM, which makes a plugin's own test pass on its author's machine and fail
in CI. Both are sorted now, and the javadoc claim of *declaration order* is gone. This is deliberately **not**
`SourceOrder`'s problem: a palette's members are laid out for a human and their order is the author's, while
a hole is addressed by its key and never by its position.

## japicmp, and why it is legitimate here

`mvn verify` compares this build against `botmaker.japicmp.baseline` and **fails on any binary- or
source-incompatible change**, with no ignore list and no exemption annotation. It catches the trap
`../docs/refactor/25-compatibility.md` lists as checked by nothing: adding a component to a public record
changes its canonical constructor descriptor and throws `NoSuchMethodError` in every already-compiled
plugin — source-compatible, binary-incompatible, and until 2026-08-27 carried by a Javadoc sentence.

The SDK's August japicmp gate was deleted because **CI cannot tell an intended break from an accident: it
cannot see the version.** That is an objection to a *conditional* rule. Here the rule is unconditional —
only a Studio major release may break a plugin, and that release edits this block — so there is nothing to
distinguish and the objection does not apply. The module has never been released, so the baseline does not
resolve yet and `ignoreMissingOldVersion` reports instead of failing; set the baseline to the previous tag in
every release commit from the first one onward.

## Style

`../docs/refactor/00-conventions.md` applies. Two things specific to this module:

- **Javadoc is the deliverable.** A plugin author has this module's Javadoc and nothing else — no source of
  the host, no examples inside the repository. Every public member says what it is *for*, and every design
  decision that constrains an implementor (the callback that is not invoked on cancel, the label that falls
  back to the member name, the empty catalog meaning "declined to curate") is written down where they will
  read it.
- **No utility methods that are not about the contract.** `Capture.toFxImage` is here because capture is
  where the AWT image comes from; a general-purpose helper that merely happens to be useful is not.

## Building

```bash
mvn test        # PaletteCatalogTest (10) + ValueVocabularyTest (9) + ScaffoldCatalogTest (12)
                # + ScaffoldPlanTest (21) — the module's only behaviour
mvn verify      # the above plus japicmp against botmaker.japicmp.baseline (see above)
mvn install     # com.github.LiQiyeDev:botmaker-studio-api:0.0.0-SNAPSHOT
```

Published through JitPack, which serves each git tag under `com.github.LiQiyeDev` regardless of this pom's
`groupId`/`version` (so the version is cosmetic). **The maintainer owns the publish** — releases are cut
from the umbrella with `../release.sh --studio-api <version>`.

Unlike `botmaker-session` and `botmaker-sdk` this module runs **no `flatten-maven-plugin` and has no
`.deps.env`**: flatten exists to bake a `-D`-injected `${botmaker.*.version}` into the published pom, and
this module pins no BotMaker upstream to inject. Its `jitpack.yml` is a plain `mvn install`.
