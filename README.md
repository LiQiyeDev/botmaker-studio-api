# botmaker-studio-api

The contract a **BotMaker Studio plugin** compiles against. Interfaces and records only — no implementation,
no Studio types, no parser.

```xml
<dependency>
    <groupId>com.github.LiQiyeDev</groupId>
    <artifactId>botmaker-studio-api</artifactId>
    <version><!-- a tag --></version>
    <scope>provided</scope>
</dependency>
```

## What a plugin contributes

```java
public final class MyPlugin implements StudioPlugin {

    @Override
    public String id() {
        return "my-plugin";
    }

    @Override
    public PaletteCatalog catalog(String pinnedVersion) {
        return PaletteCatalog.builder()
                .facade(Sound.class, Category.of("audio"))
                    .add(Sound::beep)
                    .<String>add(Sound::play)
                .build();
    }

    @Override
    public List<SlotEditor> slotEditors() {
        return List.of(SlotEditor.of(
                ctx -> ctx.slotType().is(Volume.class),
                ctx -> new VolumeSlider(ctx)));
    }
}
```

Two surfaces, and a third that is not declared here:

| surface | what it is |
|---|---|
| **palette** | which types and members are worth proposing, in which groups and in which order |
| **slot editors** | "for a value of type X, show this UI instead of a text field" |
| **generation** | a plugin that writes project files owns *whole files*, keyed by project-relative path, through its own authoring entry point |

**Panels are deliberately not a surface.** A plugin contributes to the editor; it does not contribute
editors.

## Two things worth knowing before you write one

**A catalog entry is a method reference.** `.add(Mouse::moveTo)` rather than `"moveTo"`. `MemberId` reads
the declaring class, the member name and the JVM descriptor out of the reference's `SerializedLambda`, which
means a catalog naming a member you renamed *fails your build*, and an overload set resolves exactly — with
a type witness where the name is ambiguous:

```java
.add(Mouse::moveTo)          // one member of that name — the reference is exact
.<Point>add(Mouse::click)    // an overload set — the witness is also the documentation
.<Rect>add(Mouse::click)
```

**A slot editor writes back source text.** `SlotContext.replaceWith("new Rect(12, 40, 300, 80)", …)`. No
syntax tree crosses the boundary in either direction, which is why this module depends on nothing but
JavaFX.

## Compatibility

Stricter than anything else in the BotMaker repositories, for one reason: a bot's *source* can be rewritten
when the SDK changes, but a plugin's *bytecode* cannot be rewritten by anybody. So this module changes far
more slowly than the plugins implementing it — new members arrive as `default` methods, and a plugin built
against an older release keeps working until a Studio **major** release explicitly refuses it.

## Building

```bash
mvn test        # 13 tests, all on the catalog
mvn install     # lands at com.github.LiQiyeDev:botmaker-studio-api:0.0.0-SNAPSHOT
```

Published via JitPack, which serves each git tag as `com.github.LiQiyeDev:botmaker-studio-api:<tag>`. The
pom's own `<version>` is cosmetic. Releases are cut from the umbrella repository with
`./release.sh --studio-api <version>`.
