package com.example.krystianwsul.organizator.domainmodel;

import android.content.Context;
import android.text.TextUtils;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.example.krystianwsul.organizator.persistencemodel.TaskRecord;
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class Task {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private final TaskRecord mTaskRecord;
    private final ArrayList<Schedule> mSchedules = new ArrayList<>();

    private ExactTimeStamp mOldestRelevant; // 24 hack

    Task(DomainFactory domainFactory, TaskRecord taskRecord) {
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(taskRecord != null);

        mDomainFactoryReference = new WeakReference<>(domainFactory);
        mTaskRecord = taskRecord;
    }

    void addSchedules(ArrayList<Schedule> schedules) {
        Assert.assertTrue(schedules != null);
        mSchedules.addAll(schedules);
    }

    void addSchedule(Schedule schedule) {
        Assert.assertTrue(schedule != null);
        mSchedules.add(schedule);
    }

    public String getScheduleText(Context context, ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(current(exactTimeStamp));

        Schedule currentSchedule = getCurrentSchedule(exactTimeStamp);
        if (isRootTask(exactTimeStamp)) {
            Assert.assertTrue(currentSchedule != null);
            Assert.assertTrue(currentSchedule.current(exactTimeStamp));
            return currentSchedule.getTaskText(context);
        } else {
            Assert.assertTrue(currentSchedule == null);
            return null;
        }
    }

    public Schedule getCurrentSchedule(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(current(exactTimeStamp));

        ArrayList<Schedule> currentSchedules = new ArrayList<>();
        for (Schedule schedule : mSchedules)
            if (schedule.current(exactTimeStamp))
                currentSchedules.add(schedule);

        if (currentSchedules.isEmpty()) {
            Assert.assertTrue(!isRootTask(exactTimeStamp));
            return null;
        } else {
            Assert.assertTrue(currentSchedules.size() == 1);
            Assert.assertTrue(isRootTask(exactTimeStamp));
            return currentSchedules.get(0);
        }
    }

    public String getName() {
        return mTaskRecord.getName();
    }

    void setName(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        mTaskRecord.setName(name);
    }

    public ArrayList<Task> getChildTasks(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(current(exactTimeStamp));

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        return domainFactory.getChildTasks(this, exactTimeStamp);
    }

    Task getParentTask(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(current(exactTimeStamp));

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        return domainFactory.getParentTask(this, exactTimeStamp);
    }

    public int getId() {
        return mTaskRecord.getId();
    }

    public boolean isRootTask(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(current(exactTimeStamp));

        return (getParentTask(exactTimeStamp) == null);
    }

    private ExactTimeStamp getStartExactTimeStamp() {
        return new ExactTimeStamp(mTaskRecord.getStartTime());
    }

    public ExactTimeStamp getEndExactTimeStamp() {
        if (mTaskRecord.getEndTime() != null)
            return new ExactTimeStamp(mTaskRecord.getEndTime());
        else
            return null;
    }

    void setEndExactTimeStamp(ExactTimeStamp endExactTimeStamp) {
        Assert.assertTrue(endExactTimeStamp != null);
        Assert.assertTrue(current(endExactTimeStamp));

        if (isRootTask(endExactTimeStamp))
            setScheduleEndExactTimeStamp(endExactTimeStamp);
        else
            Assert.assertTrue(getCurrentSchedule(endExactTimeStamp) == null);

        for (Task childTask : getChildTasks(endExactTimeStamp)) {
            Assert.assertTrue(childTask != null);
            childTask.setEndExactTimeStamp(endExactTimeStamp);
        }

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        domainFactory.setParentHierarchyEndTimeStamp(this, endExactTimeStamp);

        mTaskRecord.setEndTime(endExactTimeStamp.getLong());
    }

    void setScheduleEndExactTimeStamp(ExactTimeStamp endExactTimeStamp) {
        Assert.assertTrue(endExactTimeStamp != null);
        Assert.assertTrue(current(endExactTimeStamp));

        Schedule currentSchedule = getCurrentSchedule(endExactTimeStamp);
        Assert.assertTrue(currentSchedule != null);
        Assert.assertTrue(currentSchedule.current(endExactTimeStamp));

        currentSchedule.setEndExactTimeStamp(endExactTimeStamp);
    }

    public boolean current(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);

        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    ArrayList<Instance> getInstances(ExactTimeStamp startExactTimeStamp, ExactTimeStamp endExactTimeStamp) {
        Assert.assertTrue(endExactTimeStamp != null);

        ExactTimeStamp myStartExactTimeStamp = (startExactTimeStamp != null ? startExactTimeStamp : mOldestRelevant); // 24 hack

        ArrayList<Instance> instances = new ArrayList<>();
        for (Schedule schedule : mSchedules)
            instances.addAll(schedule.getInstances(myStartExactTimeStamp, endExactTimeStamp));

        if (startExactTimeStamp == null) {
            Optional<Instance> optional = Stream.of(instances)
                    .filter(Instance::isRelevant)
                    .min((lhs, rhs) -> lhs.getScheduleDateTime().compareTo(rhs.getScheduleDateTime()));

            if (optional.isPresent()) {
                mOldestRelevant = optional.get().getScheduleDateTime().getTimeStamp().toExactTimeStamp();

                ExactTimeStamp now = ExactTimeStamp.getNow();
                if (mOldestRelevant.compareTo(now) > 0)
                    mOldestRelevant = now;
            } else {
                mOldestRelevant = ExactTimeStamp.getNow();
            }
        }

        return instances;
    }
}
