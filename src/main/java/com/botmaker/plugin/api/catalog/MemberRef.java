package com.botmaker.plugin.api.catalog;

import java.io.Serializable;

/**
 * The marker every method-reference shape in this package extends, and the reason a catalog can be written
 * with {@code Mouse::click} instead of {@code "click"}.
 *
 * <p>A lambda or method reference whose target type is {@code Serializable} is compiled with a synthetic
 * {@code writeReplace()} returning a {@link java.lang.invoke.SerializedLambda}, which carries the declaring
 * class, the member name and the JVM descriptor of the method the reference points at. {@link MemberId}
 * reads exactly that, and never actually serialises anything — the {@code Serializable} supertype is a
 * request to javac, not a statement about persistence.
 *
 * <p>What this buys is the whole point of the catalog: a member named by a reference <em>must compile</em>,
 * so a catalog naming a member that was renamed or deleted fails the build rather than a menu at runtime,
 * and the descriptor resolves overloads exactly rather than by name.
 *
 * <h2>Why every shape returns {@code void}</h2>
 * A method reference to a value-returning method is compatible with a void-returning functional interface —
 * the value is simply discarded. So a value-returning shape (an {@code F1<A, R>} beside {@link M1}) would
 * make every {@code add} call on a value-returning member ambiguous, which is most of them. There is
 * therefore <em>one</em> shape per arity, {@link M0} through {@link M5}, and
 * {@link CatalogBuilder#add(M0) add} is overloaded by arity alone — which javac can resolve even for an
 * overloaded (inexact) method reference, since applicability is decided on arity before anything else.
 *
 * <p>An instance method reference puts its receiver in the first type position
 * ({@code ImageTemplate::width} is an {@code M1<ImageTemplate>}), and a constructor reference
 * ({@code ImageTemplate::new}) reports its name as {@code <init>}.
 */
public interface MemberRef extends Serializable {
}
