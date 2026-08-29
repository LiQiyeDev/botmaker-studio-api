package com.botmaker.plugin.api.catalog;

import com.botmaker.plugin.api.scaffold.EnumValues;
import com.botmaker.plugin.api.scaffold.Scaffold;
import com.botmaker.plugin.api.scaffold.Seeding;

import javax.lang.model.SourceVersion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A plugin's seeds crossed with what it wants written: every file to exist, where it goes, and what fills it.
 *
 * <p>{@link ScaffoldCatalog} answers <em>what shapes this plugin ships</em> and {@link Seeding} answers
 * <em>which instances of them this project wants</em>. Neither is useful alone, and the crossing is where
 * every mistake either plugin-side answer can make becomes visible: a name that is not a Java identifier, two
 * instances claiming one file, a hole filled with a duplicate constant. So the crossing is done <b>here</b>,
 * once, rather than in each host — a host that validated this itself would be a second implementation of a
 * rule the contract states, and the two would disagree about some project on some day.
 *
 * <h2>What this can check, and what it deliberately cannot</h2>
 *
 * <p>Everything here is answerable without parsing Java, and that is the boundary this module does not cross
 * (see {@link ScaffoldCatalog} and the module's rule that no syntax tree passes in either direction). An
 * identifier is a keyword or it is not; two constants are equal or they are not; two paths collide or they do
 * not. What is left to the host is everything that needs the project itself: whether the resolved path
 * collides with a file some <em>other</em> plugin owns, whether the class name collides with a type already
 * in the user's source, and whether the file is already there — which is the question that makes a seed
 * written once rather than written again.
 *
 * <h2>It degrades; it never throws</h2>
 *
 * <p>A rejected file is one file missing and a line in {@link #problems()}; the rest are planned and written.
 * The precedent is {@code PaletteCatalog.of} and {@code ValueCatalog.merge}, and the reason is theirs: this
 * runs while a project is being created or opened, and no malformed contribution may be why a project will
 * not open. A host logs {@code problems()} once; a plugin's own test asserts it is empty.
 */
public record ScaffoldPlan(List<PlannedFile> files, List<String> problems) {

    private static final ScaffoldPlan EMPTY = new ScaffoldPlan(List.of(), List.of());

    public ScaffoldPlan {
        files = List.copyOf(files);
        problems = List.copyOf(problems);
    }

    /** A plan that writes nothing — a plugin with no seeds, or none this project wants. */
    public static ScaffoldPlan empty() {
        return EMPTY;
    }

    /**
     * One file to write: the shape, the instance, and the path the two resolve to.
     *
     * @param seed    the shape, from the catalog
     * @param seeding the instance, from the plugin
     * @param path    {@link ScaffoldEntry#resolvePath} already applied — the identity a host keys a file on,
     *                resolved once here so two hosts cannot resolve it two ways
     */
    public record PlannedFile(ScaffoldEntry seed, Seeding seeding, String path) {

        /** What the seed's own type is called in this file — its {@link Seeding#name()}, or its own name. */
        public String typeName() {
            return seed.renamesType() && !seeding.name().isEmpty() ? seeding.name() : seed.templateName();
        }

        /**
         * The constants for the enum {@code hole} fills, or {@code null} to leave the seed's own alone.
         *
         * <p>{@code null} rather than an empty list, because the two are different instructions: nothing said
         * means the seed's declared constants stand, and an empty list would mean an enum with no constants —
         * which compiles, and which nothing can return.
         */
        public List<String> constantsFor(ScaffoldEntry.EnumHole hole) {
            return seeding.valuesFor(hole.key());
        }
    }

    // ---------------------------------------------------------------- construction

    /**
     * Crosses {@code catalog} with {@code seedings}, keyed by the <b>unresolved</b>
     * {@link Scaffold#path()} — the same key {@link ScaffoldCatalog#seed(String)} looks a seed up by.
     *
     * <p>A seed the map says nothing about contributes no file and is not a problem: a plugin shipping a seed
     * a given project does not want is the ordinary case, not a mistake.
     *
     * @param catalog     what this plugin ships
     * @param packageName the project's base package, for {@code {package}}; blank is tolerated and resolves
     *                    to the project root
     * @param seedings    what this project wants, keyed by unresolved seed path
     */
    public static ScaffoldPlan of(ScaffoldCatalog catalog, String packageName,
                                  Map<String, List<Seeding>> seedings) {
        if (catalog == null || seedings == null || seedings.isEmpty()) return EMPTY;

        List<String> problems = new ArrayList<>(catalog.problems());
        List<PlannedFile> files = new ArrayList<>();
        Map<String, String> claimedPaths = new HashMap<>();

        for (Map.Entry<String, List<Seeding>> wanted : seedings.entrySet()) {
            ScaffoldEntry seed = catalog.seed(wanted.getKey());
            if (seed == null) {
                problems.add("nothing seeds '" + wanted.getKey() + "', so its "
                        + count(wanted.getValue()) + " went nowhere");
                continue;
            }
            plan(seed, wanted.getValue(), packageName, files, claimedPaths, problems);
        }
        return new ScaffoldPlan(files, problems);
    }

    /** Every instance of one seed, with the identity and arity rules that apply across the set. */
    private static void plan(ScaffoldEntry seed, List<Seeding> wanted, String packageName,
                             List<PlannedFile> files, Map<String, String> claimedPaths,
                             List<String> problems) {
        if (wanted == null || wanted.isEmpty()) return;

        // A seed whose path has no {name} resolves to one path however many instances ask for it, so the
        // second is not a collision to report per-file — it is the plugin misreading its own seed.
        if (!seed.renamesType() && wanted.size() > 1) {
            problems.add(seed.type().getName() + " is seeded " + wanted.size() + " times but carries no "
                    + "@ClassName, so every instance would be the same file");
            return;
        }

        Set<String> keys = new HashSet<>();
        for (Seeding seeding : wanted) {
            if (seeding == null) {
                problems.add("null was seeded for " + seed.type().getName());
                continue;
            }
            if (seeding.key().isEmpty()) {
                problems.add(seed.type().getName() + " has an instance with no key; a key is what carries a "
                        + "rename, so an instance without one cannot be written");
                continue;
            }
            if (!keys.add(seeding.key())) {
                problems.add(seed.type().getName() + " is seeded twice under the key '" + seeding.key() + "'");
                continue;
            }
            PlannedFile file = file(seed, seeding, packageName, problems);
            if (file == null) continue;

            String claimant = claimedPaths.putIfAbsent(file.path(), seeding.key());
            if (claimant != null) {
                problems.add("'" + seeding.key() + "' and '" + claimant + "' both resolve to " + file.path());
                continue;
            }
            files.add(file);
        }
    }

    /** One planned file, or {@code null} with a line saying why this instance could not become one. */
    private static PlannedFile file(ScaffoldEntry seed, Seeding seeding, String packageName,
                                    List<String> problems) {
        String name = seeding.name();
        String where = seed.type().getName() + " instance '" + seeding.key() + "'";

        if (seed.renamesType()) {
            if (name.isEmpty()) {
                problems.add(where + " gives no name, and the seed's type name is substituted");
                return null;
            }
            String bad = identifierProblem(name);
            if (bad != null) {
                problems.add(where + " is named '" + name + "', which " + bad);
                return null;
            }
        } else if (!name.isEmpty() && !name.equals(seed.templateName())) {
            // Tolerated rather than refused when it matches: naming a seed after itself is the natural thing
            // to write, and only a name that differs says the plugin believed a substitution would happen.
            problems.add(where + " is named '" + name + "' but the seed carries no @ClassName, so it will be "
                    + "written as " + seed.templateName());
        }

        if (!holes(seed, seeding, where, problems)) return null;
        return new PlannedFile(seed, seeding,
                seed.resolvePath(packageName, seed.renamesType() ? name : seed.templateName()));
    }

    /**
     * Checks every hole this instance fills, and every key it names that no hole answers to.
     *
     * <p>The second half is the typo catcher and is the reason the keys are declared on the seed at all: a
     * plugin that fills {@code "outcome"} where the seed declares {@code "outcomes"} would otherwise get a
     * file whose enum silently kept its default constants, which is a bug that surfaces as a bot doing the
     * wrong thing rather than as anything failing.
     */
    private static boolean holes(ScaffoldEntry seed, Seeding seeding, String where, List<String> problems) {
        Set<String> declared = new LinkedHashSet<>();
        for (ScaffoldEntry.EnumHole hole : seed.enums()) declared.add(hole.key());

        boolean usable = true;
        for (Map.Entry<String, List<String>> filled : seeding.values().entrySet()) {
            if (!declared.contains(filled.getKey())) {
                problems.add(where + " fills '" + filled.getKey() + "', which it declares no @EnumValues for"
                        + (declared.isEmpty() ? "" : " (it declares " + declared + ")"));
                usable = false;
                continue;
            }
            usable &= constants(filled.getKey(), filled.getValue(), where, problems);
        }
        return usable;
    }

    /** One hole's constants: each a usable identifier, and no two the same. */
    private static boolean constants(String key, List<String> constants, String where, List<String> problems) {
        Set<String> seen = new HashSet<>();
        boolean usable = true;
        for (String constant : constants) {
            String bad = identifierProblem(constant);
            if (bad != null) {
                problems.add(where + " fills '" + key + "' with '" + constant + "', which " + bad);
                usable = false;
                continue;
            }
            // javac's own rule, so this is case-sensitive on purpose: NEXT and Next are two constants. That
            // they read alike is a plugin's problem to have an opinion about, not a reason to refuse the file.
            if (!seen.add(constant)) {
                problems.add(where + " fills '" + key + "' with '" + constant + "' twice");
                usable = false;
            }
        }
        return usable;
    }

    /**
     * Why {@code candidate} cannot be written into Java source, or {@code null} when it can.
     *
     * <p>{@link SourceVersion} rather than a hand-written keyword list: the list is long, it grows with the
     * language, and there are already three hand-rolled spellings of it across this project. It also covers
     * the three cases a keyword list misses — {@code true}, {@code false} and {@code null} are literals rather
     * than keywords, and none of them is a name.
     */
    private static String identifierProblem(String candidate) {
        if (candidate == null || candidate.isEmpty()) return "is blank";
        if (!SourceVersion.isIdentifier(candidate)) return "is not a Java identifier";
        if (SourceVersion.isKeyword(candidate)) return "is a Java keyword";
        return null;
    }

    private static String count(List<Seeding> seedings) {
        int size = seedings == null ? 0 : seedings.size();
        return size == 1 ? "1 instance" : size + " instances";
    }

    // ---------------------------------------------------------------- lookup

    /** The file planned at {@code path}, or {@code null} — the lookup a host does before writing one. */
    public PlannedFile at(String path) {
        for (PlannedFile file : files) {
            if (file.path().equals(path)) return file;
        }
        return null;
    }

    /**
     * The file planned for {@code key}, or {@code null}.
     *
     * <p>The lookup that carries a rename: a host holding {@code key -> path} from the last time it wrote
     * asks this for where that key goes <em>now</em>, and a different answer is the rename.
     */
    public PlannedFile forKey(String key) {
        for (PlannedFile file : files) {
            if (file.seeding().key().equals(key)) return file;
        }
        return null;
    }

    /** Whether this plan writes nothing. */
    public boolean isEmpty() {
        return files.isEmpty();
    }
}
