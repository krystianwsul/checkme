package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMilli;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class Task {
    @NonNull
    protected final DomainFactory mDomainFactory;

    protected Task(@NonNull DomainFactory domainFactory) {
        mDomainFactory = domainFactory;
    }

    public boolean current(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp startExactTimeStamp = getStartExactTimeStamp();
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (startExactTimeStamp.compareTo(exactTimeStamp) <= 0 && (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0));
    }

    @NonNull
    public abstract ExactTimeStamp getStartExactTimeStamp();

    @NonNull
    public abstract String getName();

    @Nullable
    String getScheduleText(@NonNull Context context, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        List<Schedule> currentSchedules = getCurrentSchedules(exactTimeStamp);

        if (isRootTask(exactTimeStamp)) {
            if (currentSchedules.isEmpty())
                return null;

            Assert.assertTrue(Stream.of(currentSchedules)
                    .allMatch(schedule -> schedule.current(exactTimeStamp)));

            return Stream.of(currentSchedules)
                    .map(schedule -> schedule.getScheduleText(context))
                    .collect(Collectors.joining(", "));
        } else {
            Assert.assertTrue(currentSchedules.isEmpty());
            return null;
        }
    }

    @Nullable
    public abstract String getNote();

    @NonNull
    public abstract TaskKey getTaskKey();

    @NonNull
    List<Task> getChildTasks(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return mDomainFactory.getChildTasks(this, exactTimeStamp);
    }

    boolean notDeleted(@NonNull ExactTimeStamp exactTimeStamp) {
        ExactTimeStamp endExactTimeStamp = getEndExactTimeStamp();

        return (endExactTimeStamp == null || endExactTimeStamp.compareTo(exactTimeStamp) > 0);
    }

    @SuppressWarnings("WeakerAccess") // bo inheritance i testy
    protected boolean isVisible(@NonNull ExactTimeStamp now) {
        if (current(now)) {
            Task rootTask = getRootTask(now);

            List<Schedule> schedules = rootTask.getCurrentSchedules(now);

            if (schedules.isEmpty()) {
                return true;
            }

            if (Stream.of(schedules).anyMatch(schedule -> schedule.isVisible(this, now))) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    private Task getRootTask(@NonNull ExactTimeStamp exactTimeStamp) {
        Task parentTask = getParentTask(exactTimeStamp);
        if (parentTask == null)
            return this;
        else
            return parentTask.getRootTask(exactTimeStamp);
    }

    @NonNull
    protected abstract Collection<Schedule> getSchedules();

    @NonNull
    List<Schedule> getCurrentSchedules(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return Stream.of(getSchedules())
                .filter(schedule -> schedule.current(exactTimeStamp))
                .collect(Collectors.toList());
    }

    boolean isRootTask(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return (getParentTask(exactTimeStamp) == null);
    }

    protected abstract void setMyEndExactTimeStamp(@NonNull ExactTimeStamp now);

    void setEndExactTimeStamp(@NonNull ExactTimeStamp now) {
        Assert.assertTrue(current(now));

        List<Schedule> schedules = getCurrentSchedules(now);
        if (isRootTask(now)) {
            Assert.assertTrue(Stream.of(schedules)
                    .allMatch(schedule -> schedule.current(now)));

            Stream.of(schedules)
                    .forEach(schedule -> schedule.setEndExactTimeStamp(now));
        } else {
            Assert.assertTrue(schedules.isEmpty());
        }

        Stream.of(getChildTasks(now))
                .forEach(childTask -> childTask.setEndExactTimeStamp(now));

        TaskHierarchy parentTaskHierarchy = mDomainFactory.getParentTaskHierarchy(this, now);
        if (parentTaskHierarchy != null) {
            Assert.assertTrue(parentTaskHierarchy.current(now));

            parentTaskHierarchy.setEndExactTimeStamp(now);
        }

        setMyEndExactTimeStamp(now);
    }

    @NonNull
    public abstract Task createChildTask(@NonNull ExactTimeStamp now, @NonNull String name, @Nullable String note);

    @Nullable
    public abstract ExactTimeStamp getEndExactTimeStamp();

    @Nullable
    Task getParentTask(@NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(current(exactTimeStamp));

        return mDomainFactory.getParentTask(this, exactTimeStamp);
    }

    @Nullable
    public abstract Date getOldestVisible();

    void updateOldestVisible(@NonNull ExactTimeStamp now) {
        // 24 hack
        List<Instance> instances = mDomainFactory.getPastInstances(this, now);

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

        setOldestVisible(oldestVisible);
    }

    void correctOldestVisible(@NonNull Date date) {
        Date oldestVisible = getOldestVisible();
        if (oldestVisible != null && date.compareTo(oldestVisible) < 0) {
            Log.e("asdf", getName() + " old oldest: " + oldestVisible + ", new oldest: " + date);

            setOldestVisible(date); // miejmy nadzieję że coś to później zapisze. nota bene: mogą wygenerować się instances dla wcześniej ukończonych czasów
        }
    }

    protected abstract void setOldestVisible(@NonNull Date date);

    @NonNull
    List<Instance> getInstances(@Nullable ExactTimeStamp startExactTimeStamp, @NonNull ExactTimeStamp endExactTimeStamp, @NonNull ExactTimeStamp now) {
        if (startExactTimeStamp == null) { // 24 hack
            Date oldestVisible = getOldestVisible();
            if (oldestVisible != null) {
                HourMilli zero = new HourMilli(0, 0, 0, 0);
                startExactTimeStamp = new ExactTimeStamp(oldestVisible, zero);
            }
        }

        List<Instance> instances = new ArrayList<>();
        for (Schedule schedule : getSchedules())
            instances.addAll(schedule.getInstances(this, startExactTimeStamp, endExactTimeStamp));

        List<TaskHierarchy> taskHierarchies = mDomainFactory.getParentTaskHierarchies(this);

        ExactTimeStamp finalStartExactTimeStamp = startExactTimeStamp;

        instances.addAll(Stream.of(taskHierarchies)
                .map(TaskHierarchy::getParentTask)
                .map(task -> task.getInstances(finalStartExactTimeStamp, endExactTimeStamp, now))
                .flatMap(Stream::of)
                .map(instance -> instance.getChildInstances(now))
                .flatMap(Stream::of)
                .filter(instance -> instance.getTaskKey().equals(getTaskKey()))
                .collect(Collectors.toList()));

        return instances;
    }

    public abstract void delete();

    public abstract void setName(@NonNull String name, @Nullable String note);

    @NonNull
    public abstract Set<String> getRecordOf();

    @NonNull
    protected abstract Task updateFriends(@NonNull Set<String> friends, @NonNull Context context, @NonNull ExactTimeStamp now);

    void updateSchedules(@NonNull List<CreateTaskLoader.ScheduleData> newScheduleDatas, @NonNull ExactTimeStamp now) {
        List<Schedule> removeSchedules = new ArrayList<>();
        List<CreateTaskLoader.ScheduleData> addScheduleDatas = new ArrayList<>(newScheduleDatas);

        List<Schedule> oldSchedules = getCurrentSchedules(now);
        for (Schedule schedule : oldSchedules) {
            if (addScheduleDatas.contains(schedule.getScheduleData())) {
                addScheduleDatas.remove(schedule.getScheduleData());
            } else {
                removeSchedules.add(schedule);
            }
        }

        Stream.of(removeSchedules)
                .forEach(schedule -> schedule.setEndExactTimeStamp(now));

        addSchedules(addScheduleDatas, now);
    }

    protected abstract void addSchedules(@NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @NonNull ExactTimeStamp now);

    public abstract void addChild(@NonNull Task childTask, @NonNull ExactTimeStamp now);

    protected abstract void deleteSchedule(@NonNull Schedule schedule);
}
