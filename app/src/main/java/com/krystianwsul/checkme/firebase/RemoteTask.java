package com.krystianwsul.checkme.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.Schedule;
import com.krystianwsul.checkme.domainmodel.Task;
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class RemoteTask extends Task {
    @NonNull
    private final RemoteTaskRecord mRemoteTaskRecord;

    RemoteTask(@NonNull DomainFactory domainFactory, @NonNull RemoteTaskRecord remoteTaskRecord) {
        super(domainFactory);

        mRemoteTaskRecord = remoteTaskRecord;
    }

    @NonNull
    @Override
    public String getName() {
        return mRemoteTaskRecord.getName();
    }

    @NonNull
    @Override
    protected Collection<Schedule> getSchedules() {
        if (mDomainFactory.getRemoteFactory().mRemoteSchedules.containsKey(mRemoteTaskRecord.getId()))
            return mDomainFactory.getRemoteFactory().mRemoteSchedules.get(mRemoteTaskRecord.getId());
        else
            return new ArrayList<>();
    }

    @NonNull
    @Override
    public ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mRemoteTaskRecord.getStartTime());
    }

    @Nullable
    @Override
    public ExactTimeStamp getEndExactTimeStamp() {
        if (mRemoteTaskRecord.getEndTime() != null)
            return new ExactTimeStamp(mRemoteTaskRecord.getEndTime());
        else
            return null;
    }

    @Nullable
    @Override
    public String getNote() {
        return mRemoteTaskRecord.getNote();
    }

    @NonNull
    @Override
    public TaskKey getTaskKey() {
        return new TaskKey(mRemoteTaskRecord.getId());
    }

    @NonNull
    Set<String> getRecordOf() {
        return mRemoteTaskRecord.getRecordOf();
    }

    @Override
    protected void setMyEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        mRemoteTaskRecord.setEndTime(now.getLong());
    }

    @NonNull
    public String getId() {
        return mRemoteTaskRecord.getId();
    }

    @Override
    public void createChildTask(@NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note) {
        mDomainFactory.getRemoteFactory().createChildTask(mDomainFactory, this, now, name, note);
    }

    @Nullable
    @Override
    public Date getOldestVisible() {
        if (mRemoteTaskRecord.getOldestVisibleYear() != null) {
            Assert.assertTrue(mRemoteTaskRecord.getOldestVisibleMonth() != null);
            Assert.assertTrue(mRemoteTaskRecord.getOldestVisibleDay() != null);

            return new Date(mRemoteTaskRecord.getOldestVisibleYear(), mRemoteTaskRecord.getOldestVisibleMonth(), mRemoteTaskRecord.getOldestVisibleDay());
        } else {
            Assert.assertTrue(mRemoteTaskRecord.getOldestVisibleMonth() == null);
            Assert.assertTrue(mRemoteTaskRecord.getOldestVisibleDay() == null);

            return null;
        }
    }

    @Override
    protected void setOldestVisible(@NonNull Date date) {
        mRemoteTaskRecord.setOldestVisibleYear(date.getYear());
        mRemoteTaskRecord.setOldestVisibleMonth(date.getMonth());
        mRemoteTaskRecord.setOldestVisibleDay(date.getDay());
    }

    @Override
    public void setRelevant() {
        mRemoteTaskRecord.delete();
    }

    @Override
    public void setName(@NonNull String name, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mRemoteTaskRecord.setName(name);
        mRemoteTaskRecord.setNote(note);
    }
}
