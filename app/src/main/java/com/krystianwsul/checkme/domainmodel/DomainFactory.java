package com.krystianwsul.checkme.domainmodel;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

@SuppressLint("UseSparseArrays")
public class DomainFactory {

    private final KotlinDomainFactory kotlinDomainFactory;

    private static void check(boolean value) {
        if (!value) throw new IllegalStateException();
    }

    DomainFactory(@NonNull KotlinDomainFactory kotlinDomainFactory) {
        this.kotlinDomainFactory = kotlinDomainFactory;
    }

    // misc

    // firebase

    // gets

    // sets

    // internal
}