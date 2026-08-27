package com.botmaker.plugin.api.catalog;

import com.botmaker.plugin.api.meta.Internal;
import com.botmaker.plugin.api.palette.PaletteDefault;
import com.botmaker.plugin.api.palette.PaletteLabel;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
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
 * <h2>Two ways to name members, and the default runs the other way</h2>
 *
 * <p>The {@code add} calls above are the <b>opt-in</b> form: precise, compiler-checked, and the only form a
 * plugin cataloguing a facade it does not own can use. Their cost is that a member <em>added</em> to a facade
 * is missing from the menus until somebody names it, silently.
 *
 * <p>{@link #addAll()} is the <b>opt-out</b> form, and is what a plugin cataloguing its own classes should
 * reach for: every public method the facade declares is offered, and the exceptions are written on the
 * exceptions with {@link com.botmaker.plugin.api.meta.Internal}. The two compose freely — {@code add}
 * before or after {@code addAll} — and the annotations are read by
 * {@link com.botmaker.plugin.api.palette.Facade an annotation processor} that emits exactly the calls below.
 *
 * <p>Not thread-safe, and not meant to be: a catalog is built once, in a static initialiser.
 */
public final class CatalogBuilder {

    /** A facade under construction — the mutable twin of {@link FacadeEntry}. */
    private static final class Draft {
        private final Class<?> type;
        private Category category;
        private FacadeRole role;
        private String icon;
        private String label;
        private final List<MemberEntry> members = new ArrayList<>();

        Draft(Class<?> type, Category category, FacadeRole role, String icon, String label) {
            this.type = type;
            this.category = category;
            this.role = role;
            this.icon = icon;
            this.label = label;
        }

        FacadeEntry freeze() {
            return new FacadeEntry(type, category, role, icon, label, members);
        }
    }

    private final Map<Class<?>, Draft> drafts = new LinkedHashMap<>();
    private Draft current;

    CatalogBuilder() {
    }

    CatalogBuilder(List<FacadeEntry> existing) {
        for (FacadeEntry entry : existing) {
            Draft draft = new Draft(entry.type(), entry.category(), entry.role(), entry.icon(),
                    entry.label());
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
     * <p>Reopening also updates the category and the role, so a facade can be refiled or demoted without
     * being rewritten.
     */
    public CatalogBuilder facade(Class<?> type, Category category) {
        return facade(type, category, FacadeRole.MENU);
    }

    /** As {@link #facade(Class, Category)}, choosing how far into the editor the type reaches. */
    public CatalogBuilder facade(Class<?> type, Category category, FacadeRole role) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(role, "role");
        Draft draft = drafts.computeIfAbsent(type, t -> new Draft(t, category, role, null, null));
        draft.category = category;
        draft.role = role;
        current = draft;
        return this;
    }

    /** Gives the open facade a menu glyph. */
    public CatalogBuilder facadeIcon(String icon) {
        open().icon = icon;
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

    // ---------------------------------------------------------------- members, wholesale

    /**
     * Offers every public method the open facade <em>declares</em>, except those it declines.
     *
     * <p>This is the opposite default from {@link #add(M0)}, and the difference matters more than it looks.
     * Naming members one at a time means a method <em>added</em> to a facade is absent from the menus until
     * somebody remembers to name it — a silent outcome, since the method exists and compiles and nobody is
     * told it was never proposed. Here it is offered the moment it is written, and declining it is a
     * deliberate {@link com.botmaker.plugin.api.meta.Internal} on the method itself.
     *
     * <p><b>Overloads are grouped, never dropped.</b> The unit of curation is the member <em>name</em>: every
     * overload of an offered name is offered, adjacent, with the lead shape first and the rest in
     * parameter-count order behind it. {@link FacadeEntry#member(String)} is therefore the shape the menu
     * inserts and {@link FacadeEntry#overloads(String)} the submenu, and neither needed a new record
     * component to say so — the lead is simply the first entry for its name. The lead is the narrowest shape
     * unless a {@link com.botmaker.plugin.api.palette.PaletteDefault} names another, and a
     * {@link com.botmaker.plugin.api.meta.Internal} on any one overload drops the whole name.
     *
     * <p><b>What it never offers, with no annotation needed:</b> {@code toString()}, {@code equals(Object)}
     * and {@code hashCode()}; an enum's synthetic {@code values()} and {@code valueOf(String)}; synthetic and
     * bridge methods; and anything the open facade already offers, so {@code addAll()} composes with the
     * hand-written {@code add} calls in either order.
     *
     * <p><b>Declared, not inherited.</b> A public method a facade merely inherits belongs to the supertype
     * that declared it, and that type catalogues itself or is not catalogued at all — otherwise one member
     * appears under every subtype's menu with a different owner each time.
     *
     * <p>{@link java.lang.Class#getDeclaredMethods()} returns members in no specified order, so names are
     * sorted alphabetically and each name's overloads by parameter count then descriptor. That is stable but
     * arbitrary; {@link #order(String...)} is how a facade gets the order its author actually wrote.
     */
    public CatalogBuilder addAll() {
        Draft draft = open();

        // Grouped by name, because a name is what the palette offers: one lead shape plus a submenu. A name
        // any overload declines is dropped whole — checked across the group, never per method.
        Map<String, List<Method>> byName = new LinkedHashMap<>();
        for (Method method : draft.type.getDeclaredMethods()) {
            if (eligible(draft.type, method)) {
                byName.computeIfAbsent(method.getName(), n -> new ArrayList<>()).add(method);
            }
        }

        List<String> names = new ArrayList<>(byName.keySet());
        names.sort(Comparator.naturalOrder());
        for (String name : names) {
            List<Method> overloads = byName.get(name);
            if (overloads.stream().anyMatch(m -> m.isAnnotationPresent(Internal.class))) {
                continue;
            }
            // The lead first, then the rest narrowest-first. An author's PaletteDefault is the only thing
            // that can beat parameter count, and it settles a whole family in one line.
            overloads.sort(Comparator.comparing((Method m) -> m.isAnnotationPresent(PaletteDefault.class) ? 0 : 1)
                    .thenComparing(Method::getParameterCount)
                    .thenComparing(m -> MemberId.of(m).descriptor()));
            String label = label(overloads);
            for (Method method : overloads) {
                MemberId id = MemberId.of(method);
                if (draft.members.stream().anyMatch(m -> m.id().equals(id))) {
                    continue;
                }
                draft.members.add(new MemberEntry(id, label));
            }
        }
        return this;
    }

    /**
     * A name's label — written on any one of its overloads, since it labels the name. The first one found
     * wins; the processor is what refuses two overloads that disagree, because only it can say so at the
     * line that caused it.
     */
    private static String label(List<Method> overloads) {
        for (Method method : overloads) {
            PaletteLabel label = method.getAnnotation(PaletteLabel.class);
            if (label != null) {
                return label.value();
            }
        }
        return null;
    }

    /**
     * Whether {@link #addAll()} considers this method at all — see its documentation for each clause. This is
     * per-method and structural; the editorial verdict ({@code @Internal}) is taken per <em>name</em>, in
     * {@code addAll} itself.
     */
    private static boolean eligible(Class<?> facade, Method method) {
        if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic() || method.isBridge()) {
            return false;
        }
        if (facade.isEnum() && (method.getName().equals("values") && method.getParameterCount() == 0
                || method.getName().equals("valueOf") && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == String.class)) {
            return false;
        }
        return switch (method.getName()) {
            case "toString" -> method.getParameterCount() != 0;
            case "hashCode" -> method.getParameterCount() != 0;
            case "equals" -> !(method.getParameterCount() == 1 && method.getParameterTypes()[0] == Object.class);
            default -> true;
        };
    }

    /**
     * Puts the named members first, in the order given, leaving everything else after them in the order it is
     * already in.
     *
     * <p>This is how a facade recovers the order its author wrote. {@link #addAll()} can only sort — the
     * reflective API does not report declaration order — so a generated catalog passes declaration order
     * here, read from the source at compile time by the annotation processor.
     *
     * <p>A name nothing offers is ignored rather than refused: this is positional guidance over a set that
     * curation may legitimately have narrowed, not a second place to state what exists.
     */
    public CatalogBuilder order(String... names) {
        Draft draft = open();
        List<String> wanted = List.of(names);
        List<MemberEntry> reordered = new ArrayList<>(draft.members.size());
        for (String name : wanted) {
            for (MemberEntry member : draft.members) {
                if (member.id().name().equals(name)) {
                    reordered.add(member);
                }
            }
        }
        for (MemberEntry member : draft.members) {
            if (!wanted.contains(member.id().name())) {
                reordered.add(member);
            }
        }
        draft.members.clear();
        draft.members.addAll(reordered);
        return this;
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
