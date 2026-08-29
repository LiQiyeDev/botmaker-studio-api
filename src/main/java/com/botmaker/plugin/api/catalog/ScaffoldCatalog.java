package com.botmaker.plugin.api.catalog;

import com.botmaker.plugin.api.scaffold.ClassName;
import com.botmaker.plugin.api.scaffold.Editable;
import com.botmaker.plugin.api.scaffold.EnumValues;
import com.botmaker.plugin.api.scaffold.Scaffold;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The seed files one plugin ships: real compiling classes it hands over as class literals, each with its own
 * source and the marks saying what a host may substitute in it.
 *
 * <p>The sibling of {@link PaletteCatalog} in every respect that matters. A plugin names its seeds once —
 * {@code ScaffoldCatalog.of(ActivityTemplate.class, GoHome.class)} — and everything else is read off them, so
 * the list is compiler-checked and a renamed seed that nobody re-catalogued does not build. The alternative
 * is a list of file names in a resource somewhere, which is the arrangement every part of this platform has
 * already replaced once.
 *
 * <h2>What a seed is, and the line this class exists to hold</h2>
 *
 * <p>A seed is written into a project <b>once</b>, when the thing it seeds is created, and never written
 * again. From that moment every line of it is the user's, and the only parts a host still touches are the
 * ones the marks name — a substituted type name, a substituted enum's constants — rewritten in place. A file
 * whose contents <em>as a whole</em> follow from project data is not a seed: that data belongs in the
 * project's own file, read at runtime. The difference is not stylistic — a file rewritten from data is a file
 * a user cannot edit, which is the problem this surface exists downstream of.
 *
 * <p>This catalog answers what shapes a plugin ships. <b>Which instances a project wants is a separate
 * question</b>, answered by {@code StudioPlugin.seedings} and crossed with this one by {@link ScaffoldPlan} —
 * one seed is one shape, and a project with five activities wants five files from it.
 *
 * <h2>What this catalog does not do</h2>
 *
 * <p><b>It does not parse Java.</b> Reflection can say a type or a member exists; where its text sits is a
 * question only a parser answers, and this module has no parser and will not gain one. So the annotations
 * carry <em>intent</em> — this name may be replaced, this enum's constants may be replaced, this body is the
 * user's — and the host locates each one by parsing {@link ScaffoldEntry#source}. The split is the same one
 * {@link PaletteCatalog} makes: the contract reflects, the host renders.
 *
 * <h2>A malformed catalog degrades; it never throws</h2>
 *
 * <p>{@link #of(Class[])} collects what it cannot make sense of into {@link #problems()} and builds
 * everything else — the rule {@code PaletteCatalog.of} and {@code ValueCatalog.merge} already follow, for the
 * reason they follow it: this runs while a project is being created or opened, and one seed missing is
 * recoverable where a project that will not open is not. A host should log {@code problems()} once; a test
 * should assert it is empty.
 */
public record ScaffoldCatalog(List<ScaffoldEntry> seeds, List<String> problems) {

    private static final ScaffoldCatalog EMPTY = new ScaffoldCatalog(List.of(), List.of());

    /** The two placeholders {@link Scaffold#path()} may carry. Anything else in a path is literal text. */
    private static final String NAME_PLACEHOLDER = "{name}";

    public ScaffoldCatalog {
        seeds = List.copyOf(seeds);
        problems = List.copyOf(problems);
    }

    public ScaffoldCatalog(List<ScaffoldEntry> seeds) {
        this(seeds, List.of());
    }

    /**
     * A catalog that seeds nothing — what a plugin contributing no seed files inherits, and the honest answer
     * for a version whose seeds a plugin does not want to vouch for.
     */
    public static ScaffoldCatalog empty() {
        return EMPTY;
    }

    // ---------------------------------------------------------------- construction

    /**
     * Catalogues these classes, reading {@link Scaffold} and the substitution marks off each, and reading
     * each one's {@code .java} from beside its class file.
     *
     * <p><b>The source read is what makes this more than a list.</b> A seed's class compiling proves nothing
     * about whether its source reached the jar — that is a build configuration, and a misconfigured one fails
     * silently, leaving a plugin that catalogues four seeds and can write none of them. Reading it here turns
     * that into one line in {@link #problems()} naming the class.
     *
     * <p>Seeds keep the order the plugin gave them. There is no sort: unlike a palette, nothing here is laid
     * out for a human to read, and creation order is the only order a plugin can reason about.
     */
    public static ScaffoldCatalog of(Class<?>... seeds) {
        List<String> problems = new ArrayList<>();
        List<ScaffoldEntry> entries = new ArrayList<>(seeds == null ? 0 : seeds.length);
        Map<String, Class<?>> claimedPaths = new HashMap<>();

        for (Class<?> type : seeds == null ? new Class<?>[0] : seeds) {
            ScaffoldEntry entry = entry(type, problems);
            if (entry == null) continue;
            if (!entry.path().contains(NAME_PLACEHOLDER)) {
                Class<?> claimant = claimedPaths.putIfAbsent(entry.path(), type);
                if (claimant != null) {
                    problems.add(type.getName() + " seeds '" + entry.path() + "', which "
                            + claimant.getName() + " already seeds");
                    continue;
                }
            }
            entries.add(entry);
        }
        return new ScaffoldCatalog(entries, problems);
    }

    /** One seed, or {@code null} with a line in {@code problems} saying why it could not be one. */
    private static ScaffoldEntry entry(Class<?> type, List<String> problems) {
        if (type == null) {
            problems.add("null was catalogued as a seed");
            return null;
        }
        Scaffold scaffold = type.getAnnotation(Scaffold.class);
        if (scaffold == null) {
            problems.add(type.getName() + " was catalogued but carries no @Scaffold");
            return null;
        }
        if (type.getEnclosingClass() != null) {
            problems.add(type.getName() + " is a nested type; a seed is a file, so it must be top level");
            return null;
        }
        String path = scaffold.path().trim();
        if (path.isEmpty()) {
            problems.add(type.getName() + " gives a blank @Scaffold path");
            return null;
        }
        boolean renamesType = type.isAnnotationPresent(ClassName.class);
        if (path.contains(NAME_PLACEHOLDER) && !renamesType) {
            problems.add(type.getName() + " seeds '" + path + "' but carries no @ClassName, so the file "
                    + "would be named for something its own type is not");
            return null;
        }
        String source = source(type);
        if (source == null) {
            problems.add(type.getName() + " has no " + type.getSimpleName() + ".java beside its class file; "
                    + "the build must copy seed sources into the jar as resources");
            return null;
        }
        return new ScaffoldEntry(type, path, source, scaffold.description().trim(), renamesType,
                enums(type, problems), editable(type, problems));
    }

    /**
     * The seed's own source, read from the classloader beside its {@code .class}, or {@code null}.
     *
     * <p>Everything is best effort and nothing throws: a jar that will not open is one seed missing with a
     * line saying so, and never a project that cannot be created.
     */
    private static String source(Class<?> type) {
        try (InputStream in = type.getResourceAsStream(type.getSimpleName() + ".java")) {
            if (in == null) return null;
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return text.isBlank() ? null : text;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * The substituted enums, ordered by key, with a duplicate key reported rather than resolved.
     *
     * <p><b>By key, and not in declaration order</b>, which is what this said until a test caught it:
     * {@code getDeclaredClasses()} promises no order at all, so the "first" enum claiming a duplicated key
     * was whichever one that JVM happened to hand back first. Ordering here costs nothing — a hole is
     * addressed by its key and never by its position, unlike a palette's members, which is why
     * {@link SourceOrder} exists for those and is not needed here — and it buys the one thing that matters:
     * a plugin with a malformed seed gets the same problem, naming the same enum, on every machine.
     */
    private static List<ScaffoldEntry.EnumHole> enums(Class<?> type, List<String> problems) {
        List<ScaffoldEntry.EnumHole> candidates = new ArrayList<>();
        for (Class<?> nested : type.getDeclaredClasses()) {
            EnumValues values = nested.getAnnotation(EnumValues.class);
            if (values == null) continue;
            if (!nested.isEnum()) {
                problems.add(nested.getName() + " carries @EnumValues but is not an enum");
                continue;
            }
            String key = values.value().trim();
            if (key.isEmpty()) {
                problems.add(nested.getName() + " gives a blank @EnumValues key");
                continue;
            }
            candidates.add(new ScaffoldEntry.EnumHole(key, nested.getSimpleName()));
        }
        // Sorted before the duplicate check, so which of two enums claiming one key survives is arbitrary but
        // stable. Arbitrary is acceptable because the seed is already being reported as malformed; unstable
        // would not be, since it makes a plugin's own test pass on its author's machine and fail in CI.
        candidates.sort(Comparator.comparing(ScaffoldEntry.EnumHole::key)
                .thenComparing(ScaffoldEntry.EnumHole::enumName));

        List<ScaffoldEntry.EnumHole> holes = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (ScaffoldEntry.EnumHole hole : candidates) {
            if (keys.add(hole.key())) holes.add(hole);
            else problems.add(type.getName() + " has two enums keyed '" + hole.key() + "'");
        }
        return holes;
    }

    /**
     * The names of methods whose bodies are the user's.
     *
     * <p>Names, not signatures, because that is the granularity an editor locks at — and an overload set is
     * either all handed over or all kept, since a seed that hands over one of two same-named methods is
     * describing a distinction no user reading the file can see.
     *
     * <p>Sorted, for the reason {@link #enums} is: {@code getDeclaredMethods()} promises no order, this list
     * is only ever read through {@link ScaffoldEntry#isEditable}, and a stable list is one a plugin's own test
     * can assert against.
     */
    private static List<String> editable(Class<?> type, List<String> problems) {
        List<String> names = new ArrayList<>();
        List<String> reported = new ArrayList<>();
        for (Method method : type.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Editable.class)) continue;
            if (Modifier.isAbstract(method.getModifiers())) {
                reported.add(type.getName() + "." + method.getName()
                        + " is @Editable but abstract; there is no body to hand over");
                continue;
            }
            if (!names.contains(method.getName())) names.add(method.getName());
        }
        reported.sort(Comparator.naturalOrder());
        problems.addAll(reported);
        names.sort(Comparator.naturalOrder());
        return names;
    }

    // ---------------------------------------------------------------- lookup

    /** The seed at {@code path}, unresolved, or {@code null} — the lookup a host does when restoring one. */
    public ScaffoldEntry seed(String path) {
        for (ScaffoldEntry entry : seeds) {
            if (entry.path().equals(path)) return entry;
        }
        return null;
    }

    /** Whether this catalog contributes nothing — the state {@link #empty()} names. */
    public boolean isEmpty() {
        return seeds.isEmpty();
    }
}
