package dev.asp1r.stackunlimiter.internal;

public interface LongCountAccess {
    String EXACT_COUNT_KEY = "stack_unlimiter:exact_count";

    long stackUnlimiter$getLongCount();

    void stackUnlimiter$setLongCountUnchecked(long count);
}
