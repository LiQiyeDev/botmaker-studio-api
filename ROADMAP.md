# ROADMAP — botmaker-studio-api

The running engineering log. `CHANGELOG.md` is the short, per-release answer; this is the detail and the
reasoning.

## Done

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
