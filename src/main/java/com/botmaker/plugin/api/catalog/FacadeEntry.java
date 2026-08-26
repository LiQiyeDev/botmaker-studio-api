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
 * @param type     the facade class
 * @param category the group it is filed under
 * @param label    what to show, or {@code null} for the class's simple name
 * @param members  the offered members, in declaration order
 */
public record FacadeEntry(Class<?> type, Category category, String label, List<MemberEntry> members) {

    public FacadeEntry {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(category, "category");
        members = List.copyOf(members);
    }

    public String simpleName() {
        return type.getSimpleName();
    }

    public String qualifiedName() {
        return type.getName();
    }

    /** The label if one was given, the class's simple name otherwise. Never {@code null}. */
    public String displayLabel() {
        return label != null && !label.isBlank() ? label : simpleName();
    }

    public Optional<MemberEntry> member(String name) {
        return members.stream().filter(m -> m.id().name().equals(name)).findFirst();
    }

    /** Every offered member of this facade with the given name — one per offered overload. */
    public List<MemberEntry> overloads(String name) {
        return members.stream().filter(m -> m.id().name().equals(name)).toList();
    }

    public boolean offers(String memberName) {
        return members.stream().anyMatch(m -> m.id().name().equals(memberName));
    }
}
