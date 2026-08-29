package com.botmaker.plugin.api.catalog;

import java.util.List;

/**
 * One seed file: the class that <em>is</em> it, its source as text, where it lands, and what a host may
 * substitute in it.
 *
 * <p>A record, like {@link FacadeEntry} and unlike {@code ValueCatalog.Entry}, because nothing outside this
 * module constructs one — a plugin hands over class literals and gets these back. The caveat that made the
 * other one a class still applies and is worth knowing: a public record's canonical constructor is part of
 * its binary signature, so gaining a component would break a plugin already compiled against it. Read that as
 * a reason to get the components right rather than as a reason to add one later.
 *
 * @param type            the seed class itself, named by the plugin as a class literal
 * @param path            {@link com.botmaker.plugin.api.scaffold.Scaffold#path()}, unresolved — see
 *                        {@link #resolvePath}
 * @param source          the seed's own {@code .java}, read from beside its class file
 * @param description     the plugin's sentence about what this seeds, or {@code ""}
 * @param renamesType     whether the top-level type carries
 *                        {@link com.botmaker.plugin.api.scaffold.ClassName}
 * @param enums           the substituted enums, ordered by key — reflection promises no declaration order for
 *                        nested types, and a hole is addressed by its key rather than by its position
 * @param editableMethods the names of methods marked {@link com.botmaker.plugin.api.scaffold.Editable} —
 *                        bodies that are the user's however locked the file
 */
public record ScaffoldEntry(Class<?> type, String path, String source, String description,
                            boolean renamesType, List<EnumHole> enums, List<String> editableMethods) {

    public ScaffoldEntry {
        enums = List.copyOf(enums);
        editableMethods = List.copyOf(editableMethods);
    }

    /**
     * One substituted enum: which list fills it, and the simple name of the enum it fills.
     *
     * @param key      {@link com.botmaker.plugin.api.scaffold.EnumValues#value()} — what supplies the
     *                 constants
     * @param enumName the enum's simple name, which is what a parse of {@link #source} will match on
     */
    public record EnumHole(String key, String enumName) {}

    /** The seed class's simple name — the name a substitution replaces. */
    public String templateName() {
        return type.getSimpleName();
    }

    /**
     * {@link #path} with its two placeholders filled: {@code {package}} becomes the package as a directory,
     * {@code {name}} becomes {@code name}.
     *
     * <p>Here rather than in each host, because a path resolved two ways is two answers to the question of
     * which file is which — and the answer decides whether a seed collides with one that already exists.
     * A {@code null} or blank argument fills as an empty string, so this is total: a path is never half
     * resolved with a literal {@code {name}} left in it, which would land a file nobody could open.
     */
    public String resolvePath(String packageName, String name) {
        String directory = packageName == null ? "" : packageName.replace('.', '/');
        return path.replace("{package}", directory).replace("{name}", name == null ? "" : name);
    }

    /** Whether {@code method} is one whose body belongs to the user. */
    public boolean isEditable(String method) {
        return editableMethods.contains(method);
    }
}
