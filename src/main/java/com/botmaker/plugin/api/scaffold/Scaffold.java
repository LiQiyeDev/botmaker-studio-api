package com.botmaker.plugin.api.scaffold;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a class as one of this plugin's <b>seed files</b>: {@link
 * com.botmaker.plugin.api.catalog.ScaffoldCatalog#of(Class[]) ScaffoldCatalog.of} catalogues it, and a host
 * writes it into a new project once.
 *
 * <p>The class is <b>real, compiling source in the plugin's own build</b>, not a string and not a fenced
 * template. That is the whole point of this surface: a seed that stops compiling is a red build in the plugin
 * that ships it, on the day it breaks, rather than a file that fails to compile later in a project its user
 * did not think they were editing.
 *
 * <h2>Written once, then the user's — except where a mark says otherwise</h2>
 *
 * <p>A seed is <b>not</b> a derived file. It is written when the thing it seeds is created and never written
 * again, and from that moment every line of it belongs to the user — including the lines this plugin wrote.
 * The exception is the marks: {@link ClassName} and {@link EnumValues} name regions a host keeps in step
 * afterwards, by parsing the user's file and rewriting those nodes alone. Nothing else is touched, and
 * {@link Editable} says which bodies may never be.
 *
 * <p>A file whose contents <em>as a whole</em> follow from project data is not a seed and must not be one:
 * describe that data and read it at runtime instead. The distinction is what stops this surface from becoming
 * the generated-file problem it was introduced to replace.
 *
 * <h2>The path, and its two placeholders</h2>
 *
 * <p>{@link #path()} is project-relative and may contain exactly two placeholders — {@code {package}}, the
 * project's base package as a directory, and {@code {name}}, the substituted type name. Nothing else is
 * interpreted; this is a naming convention, not a templating language, and the host resolves it.
 *
 * <p>Using {@code {name}} without {@link ClassName} on the type is reported as a problem: the path would
 * vary while the file's own type name did not, which produces a Java file whose public class does not match
 * its file name.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Scaffold {

    /**
     * Where the seed lands, relative to the project root — for example
     * {@code "src/main/java/{package}/activities/{name}.java"}.
     *
     * <p>Two seeds resolving to the same literal path are reported rather than silently overwriting one
     * another, which is the whole-file ownership rule caught one step earlier than it would otherwise be.
     */
    String path();

    /** A sentence for whoever reads the catalog — what this seed is for. Optional. */
    String description() default "";
}
