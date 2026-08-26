package com.botmaker.plugin.api.catalog;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What a plugin offers the block palette, for one version of that plugin: which types, in which groups, in
 * which order, and which of their members are worth proposing.
 *
 * <h2>What a catalog does not answer</h2>
 * <b>Presence.</b> A catalog is written with method references, so it can only name members that still
 * compile in the build that declares it — which means the catalog for an <em>older</em> version cannot name
 * a member that has since been deleted. Whether a member exists in the jar a given bot actually pins is
 * therefore still answered by scanning that jar, and the catalog answers only curation, order and labels.
 *
 * <p>The two compose in the safe direction: the palette is the <em>intersection</em>, so a bot pinned to an
 * old version may occasionally be offered slightly less than that version truly had, and never more. Being
 * offered a member that does not exist is a bot that will not compile; being offered one member fewer is a
 * menu entry somebody types by hand.
 *
 * <p>Instances are immutable. Build one with {@link #builder()}, or derive one from an existing catalog with
 * {@link #toBuilder()} — the previous version plus this version's deltas, which is how a per-version catalog
 * stays a few lines per release instead of a full restatement.
 */
public record PaletteCatalog(List<FacadeEntry> facades) {

    private static final PaletteCatalog EMPTY = new PaletteCatalog(List.of());

    public PaletteCatalog {
        facades = List.copyOf(facades);
    }

    /**
     * A catalog that offers nothing — the answer a plugin gives for a version it does not recognise, and the
     * default a plugin that contributes no palette at all inherits.
     *
     * <p>Read it as "this plugin declined to curate", never as "this plugin offers no members": a consumer
     * that sees an empty catalog should fall back to offering everything the jar contains, which is what
     * Studio has done since curation-by-annotation was removed.
     */
    public static PaletteCatalog empty() {
        return EMPTY;
    }

    public static CatalogBuilder builder() {
        return new CatalogBuilder();
    }

    /** This catalog's contents, reopened for editing — the previous-plus-deltas spelling. */
    public CatalogBuilder toBuilder() {
        return new CatalogBuilder(facades);
    }

    public boolean isEmpty() {
        return facades.isEmpty();
    }

    /** The facade with this fully-qualified name, if it is offered. */
    public Optional<FacadeEntry> facade(String qualifiedName) {
        return facades.stream().filter(f -> f.qualifiedName().equals(qualifiedName)).findFirst();
    }

    /**
     * The facade with this simple name, if exactly one is offered under it.
     *
     * <p>Empty when two facades share a simple name — the case an editor must not guess at, since the two
     * need different imports.
     */
    public Optional<FacadeEntry> facadeBySimpleName(String simpleName) {
        List<FacadeEntry> matches = facades.stream().filter(f -> f.simpleName().equals(simpleName)).toList();
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    public Optional<FacadeEntry> facade(Class<?> type) {
        return facades.stream().filter(f -> f.type().equals(type)).findFirst();
    }

    /** Every offered facade, in declaration order — the order the menu shows. */
    public List<FacadeEntry> facadesIn(Category category) {
        return facades.stream().filter(f -> f.category().equals(category)).toList();
    }

    /** The categories that have at least one facade, in the order their first facade was declared. */
    public List<Category> categories() {
        Set<Category> seen = new LinkedHashSet<>();
        facades.forEach(f -> seen.add(f.category()));
        return List.copyOf(seen);
    }

    public boolean offers(Class<?> type) {
        return facade(type).isPresent();
    }

    public boolean offers(Class<?> type, String memberName) {
        return facade(type).filter(f -> f.offers(memberName)).isPresent();
    }

    /**
     * Merges another catalog over this one — later wins on a facade both declare, and its members are
     * appended to the ones already there rather than replacing them.
     *
     * <p>This is how a host composes several plugins into one palette. It is deliberately additive: a plugin
     * curates its own surface and has no business removing another's.
     */
    public PaletteCatalog mergedWith(PaletteCatalog other) {
        if (other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return other;
        }
        CatalogBuilder merged = toBuilder();
        for (FacadeEntry entry : other.facades()) {
            merged.facade(entry.type(), entry.category());
            if (entry.label() != null) {
                merged.facadeLabel(entry.label());
            }
            for (MemberEntry member : entry.members()) {
                merged.addEntry(member);
            }
        }
        return merged.build();
    }
}
