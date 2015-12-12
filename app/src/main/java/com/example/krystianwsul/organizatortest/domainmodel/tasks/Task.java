package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;

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
            return null;
        } else {
            Assert.assertTrue(currentSchedules.size() == 1);
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
        return TaskFactory.getInstance().getChildTasks(this, timeStamp);
    }

    Task getParentTask(TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);
        return TaskFactory.getInstance().getParentTask(this, timeStamp);
    }

    public int getId() {
        return mTaskRecord.getId();
    }

    public Task getRootTask(TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);

        Task parentTask = getParentTask(timeStamp);
        if (parentTask == null)
            return this;
        else
            return parentTask.getRootTask(timeStamp);
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

        mTaskRecord.setEndTime(endTimeStamp.getLong());

        setScheduleEndTimeStamp(endTimeStamp);
    }

    public void setScheduleEndTimeStamp(TimeStamp endTimeStamp) {
        Assert.assertTrue(endTimeStamp != null);

        Schedule currentSchedule = getCurrentSchedule(endTimeStamp);
        Assert.assertTrue(currentSchedule != null);

        currentSchedule.setEndTimeStamp(endTimeStamp);
    }

    public boolean current(TimeStamp timeStamp) {
        Assert.assertTrue(timeStamp != null);

        TimeStamp startTimeStamp = getStartTimeStamp();
        TimeStamp endTimeStamp = getEndTimeStamp();

        return (startTimeStamp.compareTo(timeStamp) <= 0 && (endTimeStamp == null || endTimeStamp.compareTo(timeStamp) > 0));
    }

    public ArrayList<Instance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp) {
        Assert.assertTrue(!mSchedules.isEmpty());
        Assert.assertTrue(endTimeStamp != null);

        ArrayList<Instance> instances = new ArrayList<>();
        for (Schedule schedule : mSchedules)
            instances.addAll(schedule.getInstances(startTimeStamp, endTimeStamp));

        return instances;
    }
}
