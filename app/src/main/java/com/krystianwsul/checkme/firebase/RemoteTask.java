package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;

public class RemoteTask {
    @NonNull
    private final RemoteTaskRecord mRemoteTaskRecord;

    public RemoteTask(@NonNull RemoteTaskRecord remoteTaskRecord) {
        mRemoteTaskRecord = remoteTaskRecord;
    }

    @NonNull
    public String getName() {
        return mRemoteTaskRecord.getName();
    }
}
