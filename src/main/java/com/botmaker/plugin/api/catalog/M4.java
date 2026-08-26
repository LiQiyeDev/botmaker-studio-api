package com.botmaker.plugin.api.catalog;

/** A four-argument member. See {@link MemberRef}. */
@FunctionalInterface
public interface M4<A, B, C, D> extends MemberRef {
    void call(A a, B b, C c, D d);
}
