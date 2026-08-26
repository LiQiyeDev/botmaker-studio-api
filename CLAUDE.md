# CLAUDE.md

Guidance for working in **botmaker-studio-api**, the contract a BotMaker Studio plugin compiles against.

Read the umbrella `../CLAUDE.md` first for how the six modules fit together, and
`../docs/refactor/24-plugin-platform.md` for why this module exists at all.

## What this module is

Interfaces and records. Nothing else. It has no implementation, references no Studio type, and depends on
one artifact (`javafx-controls`, `provided`).

- `com.botmaker.plugin.api` — `StudioPlugin`, `SlotEditor`, `SlotContext`, `TypeRef`, `StudioServices` and
  the three services it exposes (`Theme`, `Capture`, `Dialogs`) plus `Region`.
- `com.botmaker.plugin.api.catalog` — `PaletteCatalog`, `CatalogBuilder`, `Category`, `FacadeEntry`,
  `MemberEntry`, `MemberId`, `MemberRef` and the arity shapes `M0`–`M5`.

Studio is the host; `botmaker-sdk` is the first plugin — a *privileged default* plugin, but a plugin, with
no back door. That is what makes the contract honest: if the SDK needs something this module does not
expose, the contract is wrong, not the SDK.

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

## The catalog, and why it is method references

A catalog entry names a member with `Mouse::click`, never `"click"` and never `Mouse.class` plus a string.
`MemberRef` extends `Serializable`, so javac gives each reference a synthetic `writeReplace()` returning a
`SerializedLambda`; `MemberId.of` reflects that one method and reads the declaring class, the member name
and the JVM descriptor. Two consequences, both load-bearing:

- **A catalog that names a renamed or deleted member does not compile.** That is the property the deleted
  `api-surface.txt` was trying to buy with a hand-maintained text file.
- **Overloads resolve exactly.** `click(Point)` and `click(Rect)` differ in the descriptor.

**Every shape returns `void`, and there is exactly one per arity.** A method reference to a value-returning
method is compatible with a void-returning functional interface — the value is discarded — so adding a
value-returning shape beside `M1` would make `add` ambiguous for most real members. With one shape per
arity, `add` is overloaded on arity alone, which javac resolves even for an *inexact* (overloaded) method
reference. Ambiguity within one arity is resolved by the caller with a type witness,
`.<Point>add(Mouse::click)` — which doubles as the documentation of which overload was meant.

**Do not add `M6`.** A public facade method taking six arguments is a design problem the catalog should
surface rather than accommodate. If one genuinely needs offering, add the shape in the same release as the
method so the two decisions are read together.

**What a catalog does not answer: presence.** Because entries must compile, the catalog for version 1.1
cannot name a member deleted in 1.3 — so whether a member exists in the jar a bot actually pins stays with
the host's ClassGraph scan of that jar, and the catalog answers only curation, order and labels. The two
compose as an intersection, which fails in the safe direction: an old pin may be offered slightly less than
it truly had, never more.

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
mvn test        # CatalogBuilderTest — 13 tests, the only behaviour in the module
mvn install     # com.github.LiQiyeDev:botmaker-studio-api:0.0.0-SNAPSHOT
```

Published through JitPack, which serves each git tag under `com.github.LiQiyeDev` regardless of this pom's
`groupId`/`version` (so the version is cosmetic). **The maintainer owns the publish** — releases are cut
from the umbrella with `../release.sh --studio-api <version>`.

Unlike `botmaker-session` and `botmaker-sdk` this module runs **no `flatten-maven-plugin` and has no
`.deps.env`**: flatten exists to bake a `-D`-injected `${botmaker.*.version}` into the published pom, and
this module pins no BotMaker upstream to inject. Its `jitpack.yml` is a plain `mvn install`.
