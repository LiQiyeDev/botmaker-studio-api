package com.botmaker.plugin.api;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Find and repoint a token sequence across the bot's own Java sources.
 *
 * <p><b>A capability, not a vocabulary.</b> Only the host can supply this, on every part of it: the open
 * buffers are editor state, the walk knows which files the bot owns and which are generated, and the review
 * mark and the history snapshot are the host's own undo model. Nothing in these signatures names a concept
 * belonging to any plugin — a needle is a sequence of Java tokens, which is a fact about Java. What a picture
 * is called, and therefore what to search for, stays entirely with whoever owns the pictures.
 *
 * <p>That split is the whole reason this exists. Rewriting a user's source is host work by construction, and
 * knowing that {@code ore.png} is spelled {@code Templates.ORE} is plugin work by construction, so a plugin
 * that renames something it owns has to be able to say <em>replace this with that</em> without the host
 * knowing what either means.
 *
 * <h2>Needles are matched as tokens, never as text</h2>
 *
 * <p>{@code "Templates.ORE"} matches {@code Templates . ORE} across any whitespace and does <b>not</b> match
 * {@code Templates.OREX} or {@code MyTemplates.ORE}. {@code "\"images/ore.png\""} matches that whole string
 * literal and nothing inside a longer one. So a plugin writes the needle the way a user would type it, and
 * gets the match a compiler would agree with.
 *
 * <p><b>Tokens rather than a regex, deliberately.</b> A regex on this interface would hand every plugin the
 * power to corrupt a user's source with a bad pattern, and would pin one flavour of regex semantics into a
 * surface that may only break on a Studio major bump. Token needles cover what a rename actually needs and
 * nothing else.
 *
 * <p><b>Text rather than an AST, equally deliberately.</b> The file that most needs a rename to reach it is
 * the one the user has open and half-edited, which does not parse. An AST rewrite would skip exactly that
 * file; this does not.
 */
public interface Sources {

    /**
     * One occurrence of a needle.
     *
     * <p><b>Constructed by the host only.</b> A plugin receives these and never builds one, which is what
     * makes it safe for this record to grow a component later: adding one changes the canonical constructor's
     * descriptor, and any already-compiled plugin that had called it would fail with a
     * {@code NoSuchMethodError} against a host it was never rebuilt for.
     *
     * @param file the source file, absolute
     * @param line where it was found, 1-based
     * @param text that line, trimmed — enough to show the user what they are about to break
     */
    record Use(Path file, int line, String text) {}

    /**
     * Every occurrence of any of {@code needles}, in the bot's own sources, open buffers preferred over the
     * files behind them.
     *
     * <p>Never {@code null}; an empty list means nothing refers to the thing being asked about, which is what
     * lets a plugin delete it without asking.
     */
    List<Use> find(List<String> needles);

    /**
     * Replaces each needle with its replacement, in the open buffer <em>and</em> on disk, and returns the
     * files that changed.
     *
     * <p>Both copies, because the editor holds open files in memory and writes them out later: a rewrite that
     * touched only the disk would be silently undone by the next save.
     *
     * @param replacements needle to replacement, applied in iteration order — pass a {@link java.util.LinkedHashMap}
     *                     when one needle could match inside another, since the first one applied wins
     * @param historyLabel snapshots the project to its history under this label first, so the whole rewrite is
     *                     one undo; {@code null} to skip, for a caller that has taken its own snapshot
     * @param reviewNote   marks the enclosing function of each rewritten line for review, carrying this note;
     *                     {@code null} when the rewrite is lossless and nothing about the bot's behaviour
     *                     moved. A rename is lossless — the same picture under a new name. Pointing at a
     *                     <em>different</em> picture is not, and is exactly the change that is invisible in
     *                     the diff a week later.
     * @return the files that changed, in the order they were visited; empty when nothing matched
     */
    List<Path> replace(Map<String, String> replacements, String historyLabel, String reviewNote);

    /**
     * A host with no sources to rewrite: nothing is ever found and nothing is ever changed.
     *
     * <p>Total, like {@link Runs#NONE}, so a plugin never null-checks and never asks whether rewriting is
     * supported. It asks what refers to the thing it is about to rename, is told <em>nothing does</em>, and
     * proceeds — which is the right answer for a host that holds no user code in the first place.
     */
    Sources NONE = new Sources() {

        @Override
        public List<Use> find(List<String> needles) {
            return List.of();
        }

        @Override
        public List<Path> replace(Map<String, String> replacements, String historyLabel, String reviewNote) {
            return List.of();
        }
    };
}
