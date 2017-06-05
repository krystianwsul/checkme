package com.krystianwsul.treeadapter;

import android.support.annotation.NonNull;

public abstract class InitializationException extends RuntimeException {
    InitializationException(@NonNull String message) {
        super(message);
    }
}
