package com.example.krystianwsul.organizator.domainmodel;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.persistencemodel.TaskRecord;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class Task {
    private final TaskRecord mTaskRecord;
    private final ArrayList<Schedule> mSchedules = new ArrayList<>();

    Task(TaskRecord taskRecord) {
        Assert.assertTrue(taskRecord != null);
        mTaskRecord = taskRecord;
    }

    public void addSchedules(ArrayList<Schedule> schedules) {
        Assert.assertTrue(schedules != null);
        mSchedules.addAll(schedules);
    }

    public void addSchedule(Schedule schedule) {
        Assert.assertTrue(schedule != null);
        mSchedules.add(schedule);
    }

    public String getScheduleText(Context context, TimeStamp timeStamp) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(timeStamp != null);
        Assert.assertTrue(current(timeStamp));

        Schedule currentSchedule = getCurrentSchedule(timeStamp);
        if (isRootTask(timeStamp)) {
            Assert.assertTrue(currentSchedule != null);
            Assert.assertTrue(currentSchedule.current(timeStamp));
            return currentSchedule.getTaskText(context);
        } else {
            Assert.assertTrue(currentSchedule == null);
            return null;
        }
    }

    public Schedule getCurrentSchedule(TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);
        Assert.assertTrue(current(timeStamp));

        ArrayList<Schedule> currentSchedules = new ArrayList<>();
        for (Schedule schedule : mSchedules)
            if (schedule.current(timeStamp))
                currentSchedules.add(schedule);

        if (currentSchedules.isEmpty()) {
            Assert.assertTrue(!isRootTask(timeStamp));
            return null;
        } else {
            Assert.assertTrue(currentSchedules.size() == 1);
            Assert.assertTrue(isRootTask(timeStamp));
            return currentSchedules.get(0);
        }
    }

    public String getName() {
        return mTaskRecord.getName();
    }

    public void setName(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        mTaskRecord.setName(name);
    }

    public ArrayList<Task> getChildTasks(TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);
        Assert.assertTrue(current(timeStamp));

        return DomainFactory.getInstance().getTaskFactory().getChildTasks(this, timeStamp);
    }

    public Task getParentTask(TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);
        Assert.assertTrue(current(timeStamp));

        return DomainFactory.getInstance().getTaskFactory().getParentTask(this, timeStamp);
    }

    public int getId() {
        return mTaskRecord.getId();
    }

    public boolean isRootTask(TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);
        Assert.assertTrue(current(timeStamp));

        return (getParentTask(timeStamp) == null);
    }

    private TimeStamp getStartTimeStamp() {
        return new TimeStamp(mTaskRecord.getStartTime());
    }

    private TimeStamp getEndTimeStamp() {
        if (mTaskRecord.getEndTime() != null)
            return new TimeStamp(mTaskRecord.getEndTime());
        else
            return null;
    }

    public void setEndTimeStamp(TimeStamp endTimeStamp) {
        Assert.assertTrue(endTimeStamp != null);
        Assert.assertTrue(current(endTimeStamp));

        if (isRootTask(endTimeStamp))
            setScheduleEndTimeStamp(endTimeStamp);
        else
            Assert.assertTrue(getCurrentSchedule(endTimeStamp) == null);

        for (Task childTask : getChildTasks(endTimeStamp)) {
            Assert.assertTrue(childTask != null);
            childTask.setEndTimeStamp(endTimeStamp);
        }

        DomainFactory.getInstance().getTaskFactory().setParentHierarchyEndTimeStamp(this, endTimeStamp);

        mTaskRecord.setEndTime(endTimeStamp.getLong());
    }

    public void setScheduleEndTimeStamp(TimeStamp endTimeStamp) {
        Assert.assertTrue(endTimeStamp != null);
        Assert.assertTrue(current(endTimeStamp));

        Schedule currentSchedule = getCurrentSchedule(endTimeStamp);
        Assert.assertTrue(currentSchedule != null);
        Assert.assertTrue(currentSchedule.current(endTimeStamp));

        currentSchedule.setEndTimeStamp(endTimeStamp);
    }

    public boolean current(TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);

        TimeStamp startTimeStamp = getStartTimeStamp();
        TimeStamp endTimeStamp = getEndTimeStamp();

        return (startTimeStamp.compareTo(timeStamp) <= 0 && (endTimeStamp == null || endTimeStamp.compareTo(timeStamp) > 0));
    }

    public ArrayList<Instance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp) {
        Assert.assertTrue(endTimeStamp != null);

        ArrayList<Instance> instances = new ArrayList<>();
        for (Schedule schedule : mSchedules)
            instances.addAll(schedule.getInstances(startTimeStamp, endTimeStamp));

        return instances;
    }
}
