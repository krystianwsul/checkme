package com.example.krystianwsul.organizatortest.domainmodel.tasks;

import android.content.Context;

import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.persistencemodel.TaskRecord;

import junit.framework.Assert;

import java.util.ArrayList;

public class RootTask extends Task {
    ArrayList<Schedule> mSchedules = new ArrayList<>();

    RootTask(TaskRecord taskRecord) {
        super(taskRecord);

        Assert.assertTrue(mTaskRecord.getParentTaskId() == null);
    }

    public void addSchedules(ArrayList<Schedule> schedules) {
        Assert.assertTrue(schedules != null);
        Assert.assertTrue(!schedules.isEmpty());
        mSchedules.addAll(schedules);
    }

    public void addSchedule(Schedule schedule) {
        Assert.assertTrue(schedule != null);
        mSchedules.add(schedule);
    }

    public String getScheduleText(Context context) {
        Assert.assertTrue(!mSchedules.isEmpty());
        return getNewestSchedule().getTaskText(context);
    }

    public Schedule getNewestSchedule() {
        Assert.assertTrue(!mSchedules.isEmpty());

        Schedule newestSchedule = mSchedules.get(0);
        if (newestSchedule.getEndTimeStamp() == null)
            return newestSchedule;

        for (Schedule schedule: mSchedules) {
            TimeStamp endTimeStamp = schedule.getEndTimeStamp();
            if (schedule.getEndTimeStamp() == null)
                return schedule;
            Assert.assertTrue(newestSchedule.getEndTimeStamp() != null);
            if (newestSchedule.getEndTimeStamp().compareTo(schedule.getEndTimeStamp()) < 0)
                newestSchedule = schedule;
        }

        return newestSchedule;
    }

    public void setNewestScheduleEndTimeStamp() {
        Assert.assertTrue(!mSchedules.isEmpty());

        Schedule schedule = getNewestSchedule();
        Assert.assertTrue(schedule != null);
        Assert.assertTrue(schedule.current(TimeStamp.getNow()));

        schedule.setEndTimeStamp();
    }

    public ArrayList<Instance> getInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp) {
        Assert.assertTrue(!mSchedules.isEmpty());
        Assert.assertTrue(endTimeStamp != null);

        ArrayList<Instance> instances = new ArrayList<>();
        for (Schedule schedule : mSchedules)
            instances.addAll(schedule.getInstances(startTimeStamp, endTimeStamp));

        return instances;
    }

    public RootTask getRootTask() {
        return this;
    }

    public boolean isRootTask() {
        return true;
    }

    public Task getParentTask() {
        throw new UnsupportedOperationException("can't get parent task of root task");
    }
}
