package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.json.JsonWrapper;

abstract class RemoteScheduleRecord extends RootRemoteRecord {
    RemoteScheduleRecord(@NonNull String id, @NonNull JsonWrapper jsonWrapper) {
        super(id, jsonWrapper);
    }

    RemoteScheduleRecord(@NonNull JsonWrapper jsonWrapper) {
        super(jsonWrapper);
    }

    public abstract long getStartTime();

    @Nullable
    public abstract Long getEndTime();

    @NonNull
    public abstract String getTaskId();
}
