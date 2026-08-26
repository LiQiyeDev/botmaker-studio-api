package com.botmaker.plugin.api.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds a {@link PaletteCatalog}, and is the shape the whole design exists to make possible:
 *
 * <pre>{@code
 * PaletteCatalog.builder()
 *     .facade(Mouse.class, Category.INTERACTION)
 *         .add(Mouse::moveTo)                 // a unique name — the reference is exact, no witness needed
 *         .<Point>add(Mouse::click)           // an overload set — the witness IS the documentation
 *         .<Rect>add(Mouse::click)
 *     .facade(Vision.class, Category.VISION)
 *         .<ImageTemplate, Precision>add(Vision::find)
 *     .build();
 * }</pre>
 *
 * <p>Everything a catalog names must compile, so a renamed or deleted member breaks the build rather than a
 * menu. See {@link MemberRef} for why the shapes all return {@code void} and why {@code add} is overloaded
 * by arity alone.
 *
 * <p><b>Order is declaration order</b>, for facades and for members within a facade, and it is what the menu
 * shows. A catalog written as "the previous version plus deltas" therefore appends: the new members land
 * after the old ones, which is where a user of the previous version expects to find them unchanged.
 *
 * <p>Not thread-safe, and not meant to be: a catalog is built once, in a static initialiser.
 */
public final class CatalogBuilder {

    /** A facade under construction — the mutable twin of {@link FacadeEntry}. */
    private static final class Draft {
        private final Class<?> type;
        private Category category;
        private String label;
        private final List<MemberEntry> members = new ArrayList<>();

        Draft(Class<?> type, Category category, String label) {
            this.type = type;
            this.category = category;
            this.label = label;
        }

        FacadeEntry freeze() {
            return new FacadeEntry(type, category, label, members);
        }
    }

    private final Map<Class<?>, Draft> drafts = new LinkedHashMap<>();
    private Draft current;

    CatalogBuilder() {
    }

    CatalogBuilder(List<FacadeEntry> existing) {
        for (FacadeEntry entry : existing) {
            Draft draft = new Draft(entry.type(), entry.category(), entry.label());
            draft.members.addAll(entry.members());
            drafts.put(entry.type(), draft);
        }
    }

    // ---------------------------------------------------------------- facades

    /**
     * Opens a facade, so the {@code add} calls that follow attach to it. Naming a facade that is already in
     * the catalog <em>reopens</em> it rather than replacing it — which is what makes the previous-plus-deltas
     * spelling work: a later version reopens {@code Mouse} and appends the members it gained.
     *
     * <p>Reopening also updates the category, so a facade can be refiled without being rewritten.
     */
    public CatalogBuilder facade(Class<?> type, Category category) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(category, "category");
        Draft draft = drafts.computeIfAbsent(type, t -> new Draft(t, category, null));
        draft.category = category;
        current = draft;
        return this;
    }

    /** Labels the open facade. */
    public CatalogBuilder facadeLabel(String label) {
        open().label = label;
        return this;
    }

    /**
     * Removes a facade and everything it offered. The removal a <em>curation</em> change makes — the type is
     * still public and still supported, it is simply no longer proposed.
     */
    public CatalogBuilder dropFacade(Class<?> type) {
        Draft removed = drafts.remove(type);
        if (current == removed) {
            current = null;
        }
        return this;
    }

    // ---------------------------------------------------------------- members

    public CatalogBuilder add(M0 ref) {
        return added(ref);
    }

    public <A> CatalogBuilder add(M1<A> ref) {
        return added(ref);
    }

    public <A, B> CatalogBuilder add(M2<A, B> ref) {
        return added(ref);
    }

    public <A, B, C> CatalogBuilder add(M3<A, B, C> ref) {
        return added(ref);
    }

    public <A, B, C, D> CatalogBuilder add(M4<A, B, C, D> ref) {
        return added(ref);
    }

    public <A, B, C, D, E> CatalogBuilder add(M5<A, B, C, D, E> ref) {
        return added(ref);
    }

    /**
     * Labels the member added last — {@code .add(Time::sleepMillis).label("Wait (milliseconds)")}.
     *
     * @throws IllegalStateException if nothing has been added to the open facade yet
     */
    public CatalogBuilder label(String label) {
        Draft draft = open();
        if (draft.members.isEmpty()) {
            throw new IllegalStateException("label() has no member to label on " + draft.type.getName());
        }
        int last = draft.members.size() - 1;
        draft.members.set(last, draft.members.get(last).withLabel(label));
        return this;
    }

    public CatalogBuilder drop(M0 ref) {
        return dropped(ref);
    }

    public <A> CatalogBuilder drop(M1<A> ref) {
        return dropped(ref);
    }

    public <A, B> CatalogBuilder drop(M2<A, B> ref) {
        return dropped(ref);
    }

    public <A, B, C> CatalogBuilder drop(M3<A, B, C> ref) {
        return dropped(ref);
    }

    public <A, B, C, D> CatalogBuilder drop(M4<A, B, C, D> ref) {
        return dropped(ref);
    }

    public <A, B, C, D, E> CatalogBuilder drop(M5<A, B, C, D, E> ref) {
        return dropped(ref);
    }

    // ---------------------------------------------------------------- internals

    private CatalogBuilder added(MemberRef ref) {
        MemberId id = MemberId.of(ref);
        Draft draft = open();
        if (!draft.type.equals(id.declaringClass())) {
            // Caught here rather than at render time: a member filed under the wrong facade produces a menu
            // entry that inserts a call nobody can compile, and the mistake is invisible in the source.
            throw new IllegalArgumentException(
                    id + " was added under facade " + draft.type.getName() + "; open its own facade first");
        }
        if (draft.members.stream().anyMatch(m -> m.id().equals(id))) {
            throw new IllegalArgumentException(id + " is offered twice by " + draft.type.getName());
        }
        draft.members.add(new MemberEntry(id));
        return this;
    }

    /**
     * Appends an already-resolved entry, ignoring one the open facade already offers. Used by
     * {@link PaletteCatalog#mergedWith} — a merge has real {@link MemberEntry}s in hand rather than method
     * references, and two plugins declaring the same member is an overlap to absorb, not an error.
     */
    CatalogBuilder addEntry(MemberEntry entry) {
        Draft draft = open();
        if (draft.members.stream().noneMatch(m -> m.id().equals(entry.id()))) {
            draft.members.add(entry);
        }
        return this;
    }

    private CatalogBuilder dropped(MemberRef ref) {
        MemberId id = MemberId.of(ref);
        Draft draft = drafts.get(id.declaringClass());
        if (draft == null || !draft.members.removeIf(m -> m.id().equals(id))) {
            throw new IllegalArgumentException(id + " cannot be dropped: it is not offered");
        }
        return this;
    }

    private Draft open() {
        if (current == null) {
            throw new IllegalStateException("open a facade first: .facade(SomeFacade.class, Category.…)");
        }
        return current;
    }

    public PaletteCatalog build() {
        List<FacadeEntry> entries = drafts.values().stream().map(Draft::freeze).toList();
        return new PaletteCatalog(entries);
    }
}
