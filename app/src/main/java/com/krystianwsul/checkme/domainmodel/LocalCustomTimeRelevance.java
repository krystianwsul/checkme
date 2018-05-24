package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;

import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime;

public class LocalCustomTimeRelevance {
    @NonNull
    private final LocalCustomTime mLocalCustomTime;

    private boolean mRelevant = false;

    LocalCustomTimeRelevance(@NonNull LocalCustomTime localCustomTime) {
        mLocalCustomTime = localCustomTime;
    }

    void setRelevant() {
        mRelevant = true;
    }

    boolean getRelevant() {
        return mRelevant;
    }

    @NonNull
    LocalCustomTime getLocalCustomTime() {
        return mLocalCustomTime;
    }
}
