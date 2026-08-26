package com.botmaker.plugin.api.catalog;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One type a plugin offers, its category, and the members of it worth proposing — in the order the plugin
 * declared them, which is the order the menu shows.
 *
 * <p>The type is a real {@link Class} rather than a name, and that is load-bearing in two places. It is what
 * makes the catalog fail the build when a facade is renamed, and it is what lets the editor answer "does the
 * SDK own this simple name?" — the deduction that decides whether {@code Point} in a bot's source means the
 * SDK's or {@code java.awt}'s — without a hand-mirrored list of fully-qualified names.
 *
 * <p><b>Present means curated.</b> A type in the catalog offers exactly the members it lists and nothing
 * else; a type absent from the catalog is not offered at all. An entry with an empty member list is
 * therefore a verdict rather than an omission — it is how an enum whose constants are the whole point, or a
 * type reached only as a variable, is catalogued for its identity without proposing any of its methods.
 *
 * @param type     the facade class
 * @param category the group it is filed under
 * @param role     how far into the editor it reaches
 * @param icon     a menu glyph, or {@code null} for the editor's own fallback
 * @param label    what to show, or {@code null} for the class's simple name
 * @param members  the offered members, in declaration order
 */
public record FacadeEntry(Class<?> type, Category category, FacadeRole role, String icon, String label,
                          List<MemberEntry> members) {

    public FacadeEntry {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(role, "role");
        members = List.copyOf(members);
    }

    public String simpleName() {
        return type.getSimpleName();
    }

    public String qualifiedName() {
        return type.getName();
    }

    /** True for {@link FacadeRole#MENU} and {@link FacadeRole#HIDDEN} — the recognition set. */
    public boolean isFacade() {
        return role != FacadeRole.VALUE;
    }

    /** True for {@link FacadeRole#MENU} alone — the set the insert menus show. */
    public boolean inMenus() {
        return role == FacadeRole.MENU;
    }

    /** The label if one was given, the class's simple name otherwise. Never {@code null}. */
    public String displayLabel() {
        return label != null && !label.isBlank() ? label : simpleName();
    }

    /**
     * The <b>lead</b> shape of this member name — what picking the name out of a menu inserts.
     *
     * <p>A palette entry is a name, not an overload, so a name with four shapes is one entry: this is the
     * one it offers first and {@link #overloads(String)} is the submenu behind it. The lead is first in
     * {@link #members()} by construction rather than by a flag, which is why nothing here has to be
     * declared twice.
     */
    public Optional<MemberEntry> member(String name) {
        return members.stream().filter(m -> m.id().name().equals(name)).findFirst();
    }

    /**
     * Every offered member of this facade with the given name — one per overload, the lead first and the
     * rest in the order the menu should list them behind it.
     */
    public List<MemberEntry> overloads(String name) {
        return members.stream().filter(m -> m.id().name().equals(name)).toList();
    }

    public boolean offers(String memberName) {
        return members.stream().anyMatch(m -> m.id().name().equals(memberName));
    }
}
