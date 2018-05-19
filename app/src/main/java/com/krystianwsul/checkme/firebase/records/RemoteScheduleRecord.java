package com.krystianwsul.checkme.firebase.records;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.json.ScheduleWrapper;

public abstract class RemoteScheduleRecord extends RemoteRecord {
    public static final String SCHEDULES = "schedules";

    @NonNull
    private final String mId;

    @NonNull
    private final RemoteTaskRecord mRemoteTaskRecord;

    @NonNull
    final ScheduleWrapper scheduleWrapper;

    RemoteScheduleRecord(@NonNull String id, @NonNull RemoteTaskRecord remoteTaskRecord, @NonNull ScheduleWrapper scheduleWrapper) {
        super(false);

        mId = id;
        mRemoteTaskRecord = remoteTaskRecord;
        this.scheduleWrapper = scheduleWrapper;
    }

    RemoteScheduleRecord(@NonNull RemoteTaskRecord remoteTaskRecord, @NonNull ScheduleWrapper scheduleWrapper) {
        super(true);

        mId = DatabaseWrapper.INSTANCE.getScheduleRecordId(remoteTaskRecord.getProjectId(), remoteTaskRecord.getId());
        mRemoteTaskRecord = remoteTaskRecord;
        this.scheduleWrapper = scheduleWrapper;
    }

    @NonNull
    @Override
    protected ScheduleWrapper getCreateObject() {
        return scheduleWrapper;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    @Override
    protected String getKey() {
        return mRemoteTaskRecord.getKey() + "/" + SCHEDULES + "/" + mId;
    }

    public abstract long getStartTime();

    @Nullable
    public abstract Long getEndTime();

    @NonNull
    public String getProjectId() {
        return mRemoteTaskRecord.getProjectId();
    }

    @NonNull
    public String getTaskId() {
        return mRemoteTaskRecord.getId();
    }
}
