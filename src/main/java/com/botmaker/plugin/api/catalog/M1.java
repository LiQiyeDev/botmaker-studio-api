package com.botmaker.plugin.api.catalog;

/**
 * A one-argument member: {@code Mouse::moveTo} (static, one parameter) or {@code ImageTemplate::width}
 * (instance, no parameters — the receiver takes the slot). See {@link MemberRef}.
 */
@FunctionalInterface
public interface M1<A> extends MemberRef {
    void call(A a);
}
