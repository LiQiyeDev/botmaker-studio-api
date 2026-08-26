package com.botmaker.plugin.api.catalog;

/** A three-argument member. See {@link MemberRef}. */
@FunctionalInterface
public interface M3<A, B, C> extends MemberRef {
    void call(A a, B b, C c);
}
