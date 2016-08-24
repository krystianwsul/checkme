package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.persistencemodel.TaskRecord;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMili;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class Task {
    private final WeakReference<DomainFactory> mDomainFactoryReference;

    private final TaskRecord mTaskRecord;
    private final ArrayList<Schedule> mSchedules = new ArrayList<>();

    Task(DomainFactory domainFactory, TaskRecord taskRecord) {
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(taskRecord != null);

        mDomainFactoryReference = new WeakReference<>(domainFactory);
        mTaskRecord = taskRecord;
    }

    void addSchedules(List<Schedule> schedules) {
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

        List<Schedule> currentSchedules = getCurrentSchedules(exactTimeStamp);
        Assert.assertTrue(currentSchedules != null);

        if (isRootTask(exactTimeStamp)) {
            if (currentSchedules.isEmpty())
                return null;

            Assert.assertTrue(Stream.of(currentSchedules)
                    .allMatch(schedule -> schedule.current(exactTimeStamp)));

            return Stream.of(currentSchedules)
                    .map(schedule -> schedule.getTaskText(context))
                    .collect(Collectors.joining(", "));
        } else {
            Assert.assertTrue(currentSchedules.isEmpty());
            return null;
        }
    }

    public List<Schedule> getCurrentSchedules(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);
        Assert.assertTrue(current(exactTimeStamp));

        return Stream.of(mSchedules)
                .filter(schedule -> schedule.current(exactTimeStamp))
                .collect(Collectors.toList());
    }

    public String getName() {
        return mTaskRecord.getName();
    }

    void setName(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        mTaskRecord.setName(name);
    }

    public List<Task> getChildTasks(ExactTimeStamp exactTimeStamp) {
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

        if (isRootTask(endExactTimeStamp)) {
            List<Schedule> schedules = getCurrentSchedules(endExactTimeStamp);
            Assert.assertTrue(schedules != null);

            if (schedules.isEmpty()) {
                Assert.assertTrue(Stream.of(schedules)
                        .allMatch(schedule -> schedule.current(endExactTimeStamp)));

                Stream.of(schedules)
                        .forEach(schedule -> schedule.setEndExactTimeStamp(endExactTimeStamp));
            }
        } else {
            Assert.assertTrue(getCurrentSchedules(endExactTimeStamp).isEmpty());
        }

        for (Task childTask : getChildTasks(endExactTimeStamp)) {
            Assert.assertTrue(childTask != null);
            childTask.setEndExactTimeStamp(endExactTimeStamp);
        }

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        domainFactory.setParentHierarchyEndTimeStamp(this, endExactTimeStamp);

        mTaskRecord.setEndTime(endExactTimeStamp.getLong());
    }

    public boolean current(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);

        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    public boolean notDeleted(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);

        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0);
    }

    Date getOldestVisible() {
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

    List<Instance> getInstances(ExactTimeStamp startExactTimeStamp, ExactTimeStamp endExactTimeStamp, ExactTimeStamp now) {
        Assert.assertTrue(endExactTimeStamp != null);
        Assert.assertTrue(now != null);

        if (startExactTimeStamp == null) { // 24 hack
            Date oldestVisible = getOldestVisible();
            if (oldestVisible != null) {
                HourMili zero = new HourMili(0, 0, 0, 0);
                startExactTimeStamp = new ExactTimeStamp(oldestVisible, zero);
            }
        }

        List<Instance> instances = new ArrayList<>();
        for (Schedule schedule : mSchedules)
            instances.addAll(schedule.getInstances(this, startExactTimeStamp, endExactTimeStamp));

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        List<TaskHierarchy> taskHierarchies = domainFactory.getParentTaskHierarchies(this);
        Assert.assertTrue(taskHierarchies != null);

        ExactTimeStamp finalStartExactTimeStamp = startExactTimeStamp;

        instances.addAll(Stream.of(taskHierarchies)
                .map(TaskHierarchy::getParentTask)
                .map(task -> task.getInstances(finalStartExactTimeStamp, endExactTimeStamp, now))
                .flatMap(Stream::of)
                .map(instance -> instance.getChildInstances(now))
                .flatMap(Stream::of)
                .filter(instance -> instance.getTaskId() == getId())
                .collect(Collectors.toList()));

        return instances;
    }

    void updateOldestVisible(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        // 24 hack
        List<Instance> instances = domainFactory.getPastInstances(this, now);

        Optional<Instance> optional = Stream.of(instances)
                .filter(instance -> instance.isVisible(now))
                .min((lhs, rhs) -> lhs.getScheduleDateTime().compareTo(rhs.getScheduleDateTime()));

        Date oldestVisible;

        if (optional.isPresent()) {
            oldestVisible = optional.get().getScheduleDateTime().getDate();

            if (oldestVisible.compareTo(now.getDate()) > 0)
                oldestVisible = now.getDate();
        } else {
            oldestVisible = now.getDate();
        }

        mTaskRecord.setOldestVisibleYear(oldestVisible.getYear());
        mTaskRecord.setOldestVisibleMonth(oldestVisible.getMonth());
        mTaskRecord.setOldestVisibleDay(oldestVisible.getDay());
    }

    private Task getRootTask(ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(exactTimeStamp != null);

        Task parentTask = getParentTask(exactTimeStamp);
        if (parentTask == null)
            return this;
        else
            return parentTask.getRootTask(exactTimeStamp);
    }

    boolean isVisible(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        if (current(now)) {
            Task rootTask = getRootTask(now);

            List<Schedule> schedules = rootTask.getCurrentSchedules(now);
            Assert.assertTrue(schedules != null);

            if (schedules.isEmpty())
                return true;

            if (Stream.of(schedules).anyMatch(schedule -> schedule.getType() != ScheduleType.SINGLE))
                return true;

            if (Stream.of(schedules)
                    .map(schedule -> (SingleSchedule) schedule)
                    .anyMatch(schedule -> schedule.getInstance(this).isVisible(now)))
                return true;
        }

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        if (Stream.of(domainFactory.getExistingInstances(this))
                .anyMatch(instance -> instance.isRelevant(now)))
            return true;

        //noinspection RedundantIfStatement
        if (Stream.of(domainFactory.getPastInstances(this, now))
                .anyMatch(instance -> instance.isRelevant(now)))
            return true;

        return false;
    }

    public boolean isRelevant(ExactTimeStamp now) {
        Assert.assertTrue(now != null);

        if (isVisible(now))
            return true;

        DomainFactory domainFactory = mDomainFactoryReference.get();
        Assert.assertTrue(domainFactory != null);

        //noinspection RedundantIfStatement
        if (Stream.of(domainFactory.getChildTaskHierarchies(this))
                .anyMatch(taskHierarchy -> taskHierarchy.getChildTask().isRelevant(now)))
            return true;

        return false;
    }

    public void setRelevant() {
        mTaskRecord.setRelevant(false);
    }

    public boolean usesCustomTime(ExactTimeStamp now, CustomTime customTime) {
        Assert.assertTrue(now != null);
        Assert.assertTrue(customTime != null);

        if (!current(now))
            return false;

        if (!isRootTask(now))
            return false;

        List<Schedule> schedules = getCurrentSchedules(now);
        Assert.assertTrue(schedules != null);

        return Stream.of(schedules)
                .anyMatch(schedule -> schedule.usesCustomTime(customTime));
    }
}
