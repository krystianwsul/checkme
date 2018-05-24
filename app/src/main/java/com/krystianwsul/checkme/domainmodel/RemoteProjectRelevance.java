package com.krystianwsul.checkme.domainmodel;

import android.support.annotation.NonNull;

import com.krystianwsul.checkme.firebase.RemoteProject;

public class RemoteProjectRelevance {
    @NonNull
    private final RemoteProject mRemoteProject;

    private boolean mRelevant = false;

    RemoteProjectRelevance(@NonNull RemoteProject remoteProject) {
        mRemoteProject = remoteProject;
    }

    public void setRelevant() {
        mRelevant = true;
    }

    boolean getRelevant() {
        return mRelevant;
    }

    @NonNull
    RemoteProject getRemoteProject() {
        return mRemoteProject;
    }
}
