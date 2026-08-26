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

- **First cut of the plugin contract.** `StudioPlugin`, `SlotEditor`, `SlotContext`, `TypeRef`,
  `StudioServices` (`Theme`, `Capture`, `Dialogs`, `Region`), and the palette vocabulary under
  `com.botmaker.plugin.api.catalog` (`PaletteCatalog`, `CatalogBuilder`, `Category`, `FacadeEntry`,
  `MemberEntry`, `MemberId`, `MemberRef`, `M0`–`M5`).
- A catalog entry is a **method reference**, not a string and not a class literal: `MemberId` reads the
  declaring class, member name and JVM descriptor out of the reference's `SerializedLambda`, so a catalog
  naming a renamed or deleted member fails the build rather than a menu, and an overload set is resolved
  exactly (`.<Point>add(Mouse::click)`).
- One dependency, `javafx-controls`, at `provided` scope — a slot editor returns a `javafx.scene.Node`.
  Nothing else, and no parsing library: a plugin writes back Java source as text.
