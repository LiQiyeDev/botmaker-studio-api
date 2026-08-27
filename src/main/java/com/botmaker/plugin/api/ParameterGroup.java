package com.botmaker.plugin.api;

/**
 * One section of the Parameters window, and the generated class its values become fields of.
 *
 * <p>A bot's parameters are the values its user configures — a delay, a colour, a list of hotkeys — and
 * they are <b>project data</b>: the user declares them, not the plugin. What a plugin declares here is the
 * <em>section</em> they are filed under: a heading in the window, an owner for the file they are generated
 * into, and a namespace for their names.
 *
 * <p><b>One window, one section per group — not a window per plugin.</b> A user configuring a bot is
 * configuring one thing; making them hunt through three windows because three plugins are installed would
 * be the plugin architecture leaking into the product. The host renders the sections in the order the
 * plugins are loaded, which puts the default plugin's first.
 *
 * <p><b>The class is whole-file owned.</b> Each group generates exactly one file, written by exactly one
 * plugin; two groups claiming one {@link #className()} is a composition error the host refuses, in the same
 * way it refuses two plugins claiming one value-type id. That ownership is what dissolves the flat
 * namespace parameters used to live in: names are unique within a group, because they are fields of that
 * group's own class, and two plugins may both offer a {@code timeout}.
 *
 * @param id        the group's stable key, held in the project file against every variable filed under it.
 *                  Never shown to a user and never changed once projects exist — it is what tells the host
 *                  which variables are still yours after a rename. The default plugin's is {@code ""}, so
 *                  that every project written before groups existed reads back as its own.
 * @param title     the section heading, in the user's words — {@code "Parameters"}, {@code "Discord"}.
 *                  Blank falls back to the id.
 * @param className the simple name of the generated class, a valid Java identifier — {@code "Parameters"},
 *                  {@code "DiscordParameters"}. It is what a bot writes down to read a value, so it is as
 *                  much API as anything in the palette.
 */
public record ParameterGroup(String id, String title, String className) {

    /** The default plugin's group id. Empty, so a project file written before groups existed reads as it. */
    public static final String DEFAULT_ID = "";

    public ParameterGroup {
        id = id == null ? DEFAULT_ID : id.trim();
        className = className == null ? "" : className.trim();
        if (className.isEmpty()) {
            throw new IllegalArgumentException("a parameter group must name the class it generates");
        }
        if (title == null || title.isBlank()) title = id.isEmpty() ? className : id;
    }

    /** A group whose heading is its class name — the ordinary case. */
    public static ParameterGroup of(String id, String className) {
        return new ParameterGroup(id, className, className);
    }
}
