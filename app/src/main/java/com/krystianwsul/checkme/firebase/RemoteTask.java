package com.krystianwsul.checkme.firebase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.Schedule;
import com.krystianwsul.checkme.domainmodel.Task;
import com.krystianwsul.checkme.firebase.records.RemoteTaskRecord;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    private RemoteFactory getRemoteFactory() {
        RemoteFactory remoteFactory = mDomainFactory.getRemoteFactory();
        Assert.assertTrue(remoteFactory != null);

        return remoteFactory;
    }

    @NonNull
    @Override
    protected Collection<Schedule> getSchedules() {
        if (getRemoteFactory().mRemoteSchedules.containsKey(mRemoteTaskRecord.getId()))
            return getRemoteFactory().mRemoteSchedules.get(mRemoteTaskRecord.getId());
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
    @Override
    public Set<String> getRecordOf() {
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
        getRemoteFactory().createChildTask(mDomainFactory, this, now, name, note);
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
        Stream.of(getSchedules()).forEach(Schedule::delete);

        mRemoteTaskRecord.delete();
    }

    @Override
    public void setName(@NonNull String name, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mRemoteTaskRecord.setName(name);
        mRemoteTaskRecord.setNote(note);
    }

    @NonNull
    @Override
    protected Task updateFriends(@NonNull Set<String> friends, @NonNull Context context, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(mDomainFactory.getFriends() != null);

        UserData userData = MainActivity.getUserData();
        Assert.assertTrue(userData != null);

        String myKey = UserData.getKey(userData.email);
        Assert.assertTrue(!friends.contains(myKey));

        Set<String> allFriends = mDomainFactory.getFriends().keySet();
        Assert.assertTrue(!allFriends.contains(myKey));

        Set<String> oldFriends = Stream.of(getRecordOf())
                .filter(allFriends::contains)
                .filterNot(myKey::equals)
                .collect(Collectors.toSet());

        Set<String> addedFriends = Stream.of(friends)
                .filterNot(oldFriends::contains)
                .collect(Collectors.toSet());
        Assert.assertTrue(!addedFriends.contains(myKey));

        Set<String> removedFriends = Stream.of(oldFriends)
                .filterNot(friends::contains)
                .collect(Collectors.toSet());
        Assert.assertTrue(!removedFriends.contains(myKey));

        getRemoteFactory().updateRecordOf(this, addedFriends, removedFriends);

        return this;
    }

    void updateRecordOf(@NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        Stream.of(getSchedules())
                .forEach(schedule -> schedule.updateRecordOf(addedFriends, removedFriends));

        mRemoteTaskRecord.updateRecordOf(addedFriends, removedFriends);
    }

    @Override
    protected void addSchedules(@NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @NonNull ExactTimeStamp now) {
        getRemoteFactory().createSchedules(mDomainFactory, getRecordOf(), getId(), now, scheduleDatas);
    }

    @Override
    protected void addChild(@NonNull Task childTask, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(childTask instanceof RemoteTask);

        getRemoteFactory().createTaskHierarchy(mDomainFactory, this, (RemoteTask) childTask, now);
    }
}
