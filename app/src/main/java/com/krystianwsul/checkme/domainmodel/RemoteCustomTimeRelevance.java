package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;

import com.krystianwsul.checkme.firebase.RemoteCustomTime;

public class RemoteCustomTimeRelevance {
    @NonNull
    private final RemoteCustomTime mRemoteCustomTime;

    private boolean mRelevant = false;

    RemoteCustomTimeRelevance(@NonNull RemoteCustomTime remoteCustomTime) {
        mRemoteCustomTime = remoteCustomTime;
    }

    void setRelevant() {
        mRelevant = true;
    }

    boolean getRelevant() {
        return mRelevant;
    }

    @NonNull
    RemoteCustomTime getRemoteCustomTime() {
        return mRemoteCustomTime;
    }
}
