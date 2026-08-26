package com.botmaker.plugin.api.catalog;

/** A two-argument member: {@code Vision::find} with {@code (ImageTemplate, Precision)}. See {@link MemberRef}. */
@FunctionalInterface
public interface M2<A, B> extends MemberRef {
    void call(A a, B b);
}
