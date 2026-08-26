package com.botmaker.plugin.api.catalog;

/**
 * A five-argument member — the widest shape the catalog offers.
 *
 * <p>The cap is deliberate rather than arbitrary: a public facade method taking six arguments is a design
 * problem the catalog should surface, not accommodate. If one ever genuinely needs offering, add {@code M6}
 * in the same release as the method, so the two decisions are read together.
 *
 * @see MemberRef
 */
@FunctionalInterface
public interface M5<A, B, C, D, E> extends MemberRef {
    void call(A a, B b, C c, D d, E e);
}
