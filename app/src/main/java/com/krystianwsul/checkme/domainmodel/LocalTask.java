package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Stream;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.persistencemodel.TaskRecord;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocalTask extends Task {
    @NonNull
    private final TaskRecord mTaskRecord;

    @NonNull
    private final ArrayList<Schedule> mSchedules = new ArrayList<>();

    public LocalTask(@NonNull DomainFactory domainFactory, @NonNull TaskRecord taskRecord) {
        super(domainFactory);

        mTaskRecord = taskRecord;
    }

    public void addSchedules(@NonNull List<Schedule> schedules) {
        mSchedules.addAll(schedules);
    }

    @NonNull
    @Override
    public String getName() {
        return mTaskRecord.getName();
    }

    @Override
    public void setName(@NonNull String name, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        mTaskRecord.setName(name);
        mTaskRecord.setNote(note);
    }

    public int getId() {
        return mTaskRecord.getId();
    }

    @NonNull
    @Override
    public ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mTaskRecord.getStartTime());
    }

    @Nullable
    @Override
    public ExactTimeStamp getEndExactTimeStamp() {
        if (mTaskRecord.getEndTime() != null)
            return new ExactTimeStamp(mTaskRecord.getEndTime());
        else
            return null;
    }

    @Override
    protected void setMyEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        mTaskRecord.setEndTime(now.getLong());
    }

    @Nullable
    @Override
    public Date getOldestVisible() {
        if (mTaskRecord.getOldestVisibleYear() != null) {
            Assert.assertTrue(mTaskRecord.getOldestVisibleMonth() != null);
            Assert.assertTrue(mTaskRecord.getOldestVisibleDay() != null);

            return new Date(mTaskRecord.getOldestVisibleYear(), mTaskRecord.getOldestVisibleMonth(), mTaskRecord.getOldestVisibleDay());
        } else {
            Assert.assertTrue(mTaskRecord.getOldestVisibleMonth() == null);
            Assert.assertTrue(mTaskRecord.getOldestVisibleDay() == null);

            return null;
        }
    }

    @Override
    protected void setOldestVisible(@NonNull Date date) {
        mTaskRecord.setOldestVisibleYear(date.getYear());
        mTaskRecord.setOldestVisibleMonth(date.getMonth());
        mTaskRecord.setOldestVisibleDay(date.getDay());
    }

    @Override
    public void setRelevant() {
        mTaskRecord.setRelevant(false);
    }

    @Nullable
    @Override
    public String getNote() {
        return mTaskRecord.getNote();
    }

    @NonNull
    @Override
    public TaskKey getTaskKey() {
        return new TaskKey(mTaskRecord.getId());
    }

    @Override
    public void createChildTask(@NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note) {
        mDomainFactory.createLocalChildTask(now, getTaskKey(), name, note);
    }

    @NonNull
    @Override
    public Collection<Schedule> getSchedules() {
        return mSchedules;
    }

    @NonNull
    @Override
    public Set<String> getRecordOf() {
        return new HashSet<>();
    }

    void delete() {
        Stream.of(getSchedules())
                .forEach(Schedule::delete);

        mDomainFactory.getLocalFactory().deleteTask(this);

        mTaskRecord.delete();
    }

    @NonNull
    @Override
    protected Task updateFriends(@NonNull Set<String> friends, @NonNull Context context, @NonNull ExactTimeStamp now) {
        if (friends.isEmpty()) {
            return this;
        } else {
            UserData userData = MainActivity.getUserData();
            Assert.assertTrue(userData != null);

            friends.add(UserData.getKey(userData.email));

            return mDomainFactory.convertLocalToRemote(context, now, this, friends);
        }
    }

    @Override
    protected void addSchedules(@NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(!scheduleDatas.isEmpty());

        List<Schedule> schedules = mDomainFactory.createSchedules(this, scheduleDatas, now);
        Assert.assertTrue(!schedules.isEmpty());

        addSchedules(schedules);
    }

    @Override
    protected void addChild(@NonNull Task childTask, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(childTask instanceof LocalTask);

        mDomainFactory.createTaskHierarchy(this, (LocalTask) childTask, now);
    }
}
