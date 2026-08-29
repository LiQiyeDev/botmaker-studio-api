/**
 * Seed files, written on the source they seed.
 *
 * <p>A plugin names its seeds once, as class literals, and everything else is read off them — the same
 * arrangement {@link com.botmaker.plugin.api.palette} uses for curation, and for the same reason: a class
 * literal is checked by javac, so a seed that is renamed and not re-catalogued does not compile.
 *
 * <pre>{@code
 * @Scaffold(path = "src/main/java/{package}/activities/{name}.java")
 * @ClassName
 * public class ActivityTemplate extends Activity<ActivityTemplate.Outcome> {
 *
 *     @EnumValues("outcomes")
 *     public enum Outcome { NEXT }
 *
 *     @Editable("what this activity does")
 *     @Override
 *     public Outcome run() {
 *         return Outcome.NEXT;
 *     }
 * }
 *
 * // elsewhere, in the plugin:
 * ScaffoldCatalog.of(ActivityTemplate.class, GoHome.class, Popups.class);
 * }</pre>
 *
 * <h2>Real source, checked by the plugin's own build</h2>
 *
 * <p>The seed above compiles. It is on the plugin's compile path like any other class, so javac checks its
 * imports, its supertype, its overrides and its return type on every build — which is the property that
 * source assembled from strings can never have, however carefully the strings are assembled. A generator's
 * output is checked when somebody runs it; a seed's is checked when somebody builds.
 *
 * <h2>Intent and addressing are two different jobs</h2>
 *
 * <p>These annotations say <b>what may be replaced</b>. They cannot say <b>where</b>: reflection knows that a
 * type or a member exists and nothing whatever about its position in the source text. The host parses the
 * seed and rewrites the node it finds, so a substitution targets a real parsed span rather than a matched
 * token. Neither half substitutes for the other — without the marks a host would be guessing which names are
 * the plugin's to change, and without the parse it would be running a search and replace over somebody's
 * Java.
 *
 * <h2>The file is written once; the marks are maintained</h2>
 *
 * <p>This is the line that keeps the surface honest, and it is worth stating precisely before anybody reaches
 * for it. A seed is <b>written</b> when the thing it seeds is created and never written again; from that
 * moment every line of it is the user's, including the lines the plugin wrote. What a host may still touch is
 * exactly what these marks name and nothing else — the type's name, and a marked enum's constants — rewritten
 * in place, in the user's own file, by a parse of <em>that</em> file rather than of the seed.
 *
 * <p><b>Both halves are load-bearing and it took a working implementation to learn why.</b> Write-once alone
 * cannot hold: a user who adds an outcome on the flow canvas would get a file whose enum no longer lists it,
 * and a project that does not compile. Maintained-everywhere cannot hold either: it is regeneration wearing a
 * new word, and it destroys the body the user opened the file to write. The marks are where the line between
 * the two is drawn, and they are drawn by the plugin that wrote the file rather than guessed at by the host.
 *
 * <p>A file whose contents <em>as a whole</em> follow from project data is still not a seed — describe the
 * data and read it at runtime, because a file rewritten from data is a file a user cannot edit, and that is
 * the problem this surface exists downstream of rather than a use for it.
 *
 * <h2>Runtime retention</h2>
 *
 * <p>All four are {@code RUNTIME}-retained, because the plugin reflects its own classes to build the
 * catalog — the same reasoning as the palette annotations, arrived at there the hard way after an annotation
 * processor was tried and deleted.
 */
package com.botmaker.plugin.api.scaffold;
