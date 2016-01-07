package com.example.krystianwsul.organizator.domainmodel;

import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.persistencemodel.CustomTimeRecord;
import com.example.krystianwsul.organizator.persistencemodel.DailyScheduleTimeRecord;
import com.example.krystianwsul.organizator.persistencemodel.InstanceRecord;
import com.example.krystianwsul.organizator.persistencemodel.PersistenceManger;
import com.example.krystianwsul.organizator.persistencemodel.ScheduleRecord;
import com.example.krystianwsul.organizator.persistencemodel.SingleScheduleDateTimeRecord;
import com.example.krystianwsul.organizator.persistencemodel.TaskHierarchyRecord;
import com.example.krystianwsul.organizator.persistencemodel.TaskRecord;
import com.example.krystianwsul.organizator.persistencemodel.WeeklyScheduleDayOfWeekTimeRecord;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DateTime;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.Time;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class DomainFactory {
    private static DomainFactory mInstance;

    private final CustomTimeFactory mCustomTimeFactory = new CustomTimeFactory();
    private final TaskFactory mTaskFactory = new TaskFactory();
    private final InstanceFactory mInstanceFactory = new InstanceFactory();

    public static DomainFactory getInstance() {
        if (mInstance == null)
            mInstance = new DomainFactory();
        return mInstance;
    }

    private DomainFactory() {

    }

    public CustomTimeFactory getCustomTimeFactory() {
        return mCustomTimeFactory;
    }

    public TaskFactory getTaskFactory() {
        return mTaskFactory;
    }

    public InstanceFactory getInstanceFactory() {
        return mInstanceFactory;
    }

    public class InstanceFactory {
        private final ArrayList<Instance> mExistingInstances = new ArrayList<>();

        InstanceFactory() {
            Collection<InstanceRecord> instanceRecords = PersistenceManger.getInstance().getInstanceRecords();
            Assert.assertTrue(instanceRecords != null);

            for (InstanceRecord instanceRecord : instanceRecords) {
                Task task = mTaskFactory.getTask(instanceRecord.getTaskId());
                Assert.assertTrue(task != null);

                Instance instance = new Instance(task, instanceRecord);
                mExistingInstances.add(instance);
            }
        }

        void addExistingInstance(Instance instance) {
            Assert.assertTrue(instance != null);
            mExistingInstances.add(instance);
        }

        public Instance getInstance(Task task, DateTime scheduleDateTime) {
            Assert.assertTrue(task != null);
            Assert.assertTrue(scheduleDateTime != null);

            ArrayList<Instance> instances = new ArrayList<>();
            for (Instance instance : mExistingInstances) {
                Assert.assertTrue(instance != null);
                if (instance.getTaskId() == task.getId() && instance.getScheduleDateTime().compareTo(scheduleDateTime) == 0)
                    instances.add(instance);
            }

            if (!instances.isEmpty()) {
                Assert.assertTrue(instances.size() == 1);
                return instances.get(0);
            } else {
                return new Instance(task, scheduleDateTime);
            }
        }

        private ArrayList<Instance> getRootInstances(TimeStamp startTimeStamp, TimeStamp endTimeStamp) {
            Assert.assertTrue(endTimeStamp != null);
            Assert.assertTrue(startTimeStamp == null || startTimeStamp.compareTo(endTimeStamp) < 0);

            HashSet<Instance> allInstances = new HashSet<>();
            allInstances.addAll(mExistingInstances);

            Collection<Task> tasks = mTaskFactory.getTasks();

            for (Task task : tasks)
                allInstances.addAll(task.getInstances(startTimeStamp, endTimeStamp));

            ArrayList<Instance> rootInstances = new ArrayList<>();
            for (Instance instance : allInstances)
                if (instance.isRootInstance())
                    rootInstances.add(instance);

            return rootInstances;
        }

        public ArrayList<Instance> getCurrentInstances() {
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.add(Calendar.DATE, 2);
            Date endDate = new Date(endCalendar);

            return getRootInstances(null, new TimeStamp(endDate, new HourMinute(0, 0)));
        }

        public ArrayList<Instance> getNotificationInstances() {
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.add(Calendar.MINUTE, 1);

            TimeStamp endTimeStamp = new TimeStamp(endCalendar);

            ArrayList<Instance> rootInstances = getRootInstances(null, endTimeStamp);

            ArrayList<Instance> notificationInstances = new ArrayList<>();
            for (Instance instance : rootInstances) {
                if (instance.getDone() == null && !instance.getNotified() && instance.getInstanceDateTime().getTimeStamp().compareTo(endTimeStamp) < 0)
                    notificationInstances.add(instance);
            }
            return notificationInstances;
        }

        public ArrayList<Instance> getCurrentInstances(TimeStamp timeStamp) {
            Calendar endCalendar = timeStamp.getCalendar();
            endCalendar.add(Calendar.MINUTE, 1);
            TimeStamp endTimeStamp = new TimeStamp(endCalendar);

            ArrayList<Instance> rootInstances = getRootInstances(timeStamp, endTimeStamp);

            ArrayList<Instance> currentInstances = new ArrayList<>();
            for (Instance instance : rootInstances)
                if (instance.getInstanceDateTime().getTimeStamp().compareTo(timeStamp) == 0)
                    currentInstances.add(instance);

            return currentInstances;
        }

        public ArrayList<Instance> getShownInstances() {
            ArrayList<Instance> shownInstances = new ArrayList<>();

            for (Instance instance : mExistingInstances)
                if (instance.getNotificationShown())
                    shownInstances.add(instance);

            return shownInstances;
        }
    }

    public class TaskFactory {
        private final HashMap<Integer, Task> mTasks = new HashMap<>();
        private final HashMap<Integer, TaskHierarchy> mTaskHierarchies = new HashMap<>();

        TaskFactory() {
            PersistenceManger persistenceManger = PersistenceManger.getInstance();

            Collection<TaskRecord> taskRecords = persistenceManger.getTaskRecords();
            Assert.assertTrue(taskRecords != null);

            for (TaskRecord taskRecord : taskRecords) {
                Assert.assertTrue(taskRecord != null);

                Task task = new Task(taskRecord);

                ArrayList<Schedule> schedules = loadSchedules(task);
                Assert.assertTrue(schedules != null);

                task.addSchedules(schedules);

                Assert.assertTrue(!mTasks.containsKey(task.getId()));
                mTasks.put(task.getId(), task);
            }

            Collection<TaskHierarchyRecord> taskHierarchyRecords = persistenceManger.getTaskHierarchyRecords();
            Assert.assertTrue(taskHierarchyRecords != null);

            for (TaskHierarchyRecord taskHierarchyRecord : taskHierarchyRecords) {
                Assert.assertTrue(taskHierarchyRecord != null);

                Task parentTask = getTask(taskHierarchyRecord.getParentTaskId());
                Assert.assertTrue(parentTask != null);

                Task childTask = getTask(taskHierarchyRecord.getChildTaskId());
                Assert.assertTrue(childTask != null);

                TaskHierarchy taskHierarchy = new TaskHierarchy(taskHierarchyRecord, parentTask, childTask);

                Assert.assertTrue(!mTaskHierarchies.containsKey(taskHierarchy.getId()));
                mTaskHierarchies.put(taskHierarchy.getId(), taskHierarchy);
            }
        }

        private ArrayList<Schedule> loadSchedules(Task task) {
            Assert.assertTrue(task != null);

            ArrayList<ScheduleRecord> scheduleRecords = PersistenceManger.getInstance().getScheduleRecords(task);
            Assert.assertTrue(scheduleRecords != null);

            ArrayList<Schedule> schedules = new ArrayList<>();

            for (ScheduleRecord scheduleRecord : scheduleRecords) {
                Assert.assertTrue(scheduleRecord.getType() >= 0);
                Assert.assertTrue(scheduleRecord.getType() < Schedule.ScheduleType.values().length);

                Schedule.ScheduleType scheduleType = Schedule.ScheduleType.values()[scheduleRecord.getType()];

                switch (scheduleType) {
                    case SINGLE:
                        schedules.add(loadSingleSchedule(scheduleRecord, task));
                        break;
                    case DAILY:
                        schedules.add(loadDailySchedule(scheduleRecord, task));
                        break;
                    case WEEKLY:
                        schedules.add(loadWeeklySchedule(scheduleRecord, task));
                        break;
                    default:
                        throw new IndexOutOfBoundsException("unknown schedule type");
                }
            }

            return schedules;
        }

        private Schedule loadSingleSchedule(ScheduleRecord scheduleRecord, Task rootTask) {
            Assert.assertTrue(scheduleRecord != null);
            Assert.assertTrue(rootTask != null);

            SingleSchedule singleSchedule = new SingleSchedule(scheduleRecord, rootTask);

            SingleScheduleDateTimeRecord singleScheduleDateTimeRecord = PersistenceManger.getInstance().getSingleScheduleDateTimeRecord(singleSchedule);
            Assert.assertTrue(singleScheduleDateTimeRecord != null);

            SingleScheduleDateTime singleScheduleDateTime = new SingleScheduleDateTime(singleScheduleDateTimeRecord);
            singleSchedule.setSingleScheduleDateTime(singleScheduleDateTime);

            return singleSchedule;
        }

        private Schedule loadDailySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
            Assert.assertTrue(scheduleRecord != null);
            Assert.assertTrue(rootTask != null);

            PersistenceManger persistenceManger = PersistenceManger.getInstance();

            DailySchedule dailySchedule = new DailySchedule(scheduleRecord, rootTask);

            ArrayList<DailyScheduleTimeRecord> dailyScheduleTimeRecords = persistenceManger.getDailyScheduleTimeRecords(dailySchedule);
            Assert.assertTrue(dailyScheduleTimeRecords != null);
            Assert.assertTrue(!dailyScheduleTimeRecords.isEmpty());

            for (DailyScheduleTimeRecord dailyScheduleTimeRecord : dailyScheduleTimeRecords) {
                DailyScheduleTime dailyScheduleTime = new DailyScheduleTime(dailyScheduleTimeRecord);
                dailySchedule.addDailyScheduleTime(dailyScheduleTime);
            }

            return dailySchedule;
        }

        private Schedule loadWeeklySchedule(ScheduleRecord scheduleRecord, Task rootTask) {
            Assert.assertTrue(scheduleRecord != null);
            Assert.assertTrue(rootTask != null);

            PersistenceManger persistenceManger = PersistenceManger.getInstance();

            WeeklySchedule weeklySchedule = new WeeklySchedule(scheduleRecord, rootTask);

            ArrayList<WeeklyScheduleDayOfWeekTimeRecord> weeklyScheduleDayOfWeekTimeRecords = persistenceManger.getWeeklyScheduleDayOfWeekTimeRecords(weeklySchedule);
            Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecords != null);
            Assert.assertTrue(!weeklyScheduleDayOfWeekTimeRecords.isEmpty());

            for (WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord : weeklyScheduleDayOfWeekTimeRecords) {
                WeeklyScheduleDayOfWeekTime weeklyScheduleDayOfWeekTime = new WeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTimeRecord);
                weeklySchedule.addWeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTime);
            }

            return weeklySchedule;
        }

        public Collection<Task> getTasks() {
            return mTasks.values();
        }

        public ArrayList<Task> getRootTasks(TimeStamp timeStamp) {
            Assert.assertTrue(timeStamp != null);

            ArrayList<Task> rootTasks = new ArrayList<>();
            for (Task task : mTasks.values())
                if (task.current(timeStamp) && task.isRootTask(timeStamp))
                    rootTasks.add(task);

            return rootTasks;
        }

        public Task getTask(int taskId) {
            return mTasks.get(taskId);
        }

        public Task createRootTask(String name, TimeStamp startTimeStamp) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(startTimeStamp != null);

            TaskRecord taskRecord = PersistenceManger.getInstance().createTaskRecord(name, startTimeStamp);
            Assert.assertTrue(taskRecord != null);

            Task rootTask = new Task(taskRecord);

            Assert.assertTrue(!mTasks.containsKey(rootTask.getId()));
            mTasks.put(rootTask.getId(), rootTask);

            return rootTask;
        }

        public void createChildTask(Task parentTask, String name, TimeStamp startTimeStamp) {
            Assert.assertTrue(parentTask != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(startTimeStamp != null);
            Assert.assertTrue(parentTask.current(startTimeStamp));

            TaskRecord childTaskRecord = PersistenceManger.getInstance().createTaskRecord(name, startTimeStamp);
            Assert.assertTrue(childTaskRecord != null);

            Task childTask = new Task(childTaskRecord);
            Assert.assertTrue(!mTasks.containsKey(childTask.getId()));
            mTasks.put(childTask.getId(), childTask);

            createTaskHierarchy(parentTask, childTask, startTimeStamp);
        }

        public void joinTasks(Task rootTask, ArrayList<Task> childTasks, TimeStamp timeStamp) {
            Assert.assertTrue(rootTask != null);
            Assert.assertTrue(rootTask.current(timeStamp));
            Assert.assertTrue(rootTask.isRootTask(timeStamp));
            Assert.assertTrue(childTasks != null);
            Assert.assertTrue(childTasks.size() > 1);

            for (Task childTask : childTasks) {
                Assert.assertTrue(childTask != null);
                Assert.assertTrue(childTask.current(timeStamp));
                Assert.assertTrue(childTask.isRootTask(timeStamp));

                childTask.setScheduleEndTimeStamp(timeStamp);

                createTaskHierarchy(rootTask, childTask, timeStamp);
            }
        }

        private void createTaskHierarchy(Task parentTask, Task childTask, TimeStamp startTimeStamp) {
            Assert.assertTrue(startTimeStamp != null);
            Assert.assertTrue(parentTask != null);
            Assert.assertTrue(parentTask.current(startTimeStamp));
            Assert.assertTrue(childTask != null);
            Assert.assertTrue(childTask.current(startTimeStamp));

            TaskHierarchyRecord taskHierarchyRecord = PersistenceManger.getInstance().createTaskHierarchyRecord(parentTask, childTask, startTimeStamp);
            Assert.assertTrue(taskHierarchyRecord != null);

            TaskHierarchy taskHierarchy = new TaskHierarchy(taskHierarchyRecord, parentTask, childTask);
            Assert.assertTrue(!mTaskHierarchies.containsKey(taskHierarchy.getId()));
            mTaskHierarchies.put(taskHierarchy.getId(), taskHierarchy);
        }

        public SingleSchedule createSingleSchedule(Task rootTask, Date date, Time time, TimeStamp startTimeStamp) {
            Assert.assertTrue(rootTask != null);
            Assert.assertTrue(date != null);
            Assert.assertTrue(time != null);
            Assert.assertTrue(startTimeStamp != null);
            Assert.assertTrue(new DateTime(date, time).getTimeStamp().compareTo(startTimeStamp) >= 0);

            PersistenceManger persistenceManger = PersistenceManger.getInstance();

            ScheduleRecord scheduleRecord = persistenceManger.createScheduleRecord(rootTask, Schedule.ScheduleType.SINGLE, startTimeStamp);
            Assert.assertTrue(scheduleRecord != null);

            SingleSchedule singleSchedule = new SingleSchedule(scheduleRecord, rootTask);

            SingleScheduleDateTimeRecord singleScheduleDateTimeRecord = persistenceManger.createSingleScheduleDateTimeRecord(singleSchedule, date, time);
            Assert.assertTrue(singleScheduleDateTimeRecord != null);

            singleSchedule.setSingleScheduleDateTime(new SingleScheduleDateTime(singleScheduleDateTimeRecord));

            return singleSchedule;
        }

        public DailySchedule createDailySchedule(Task rootTask, ArrayList<Time> times, TimeStamp startTimeStamp) {
            Assert.assertTrue(rootTask != null);
            Assert.assertTrue(times != null);
            Assert.assertTrue(!times.isEmpty());
            Assert.assertTrue(startTimeStamp != null);
            Assert.assertTrue(rootTask.current(startTimeStamp));

            PersistenceManger persistenceManger = PersistenceManger.getInstance();

            ScheduleRecord scheduleRecord = persistenceManger.createScheduleRecord(rootTask, Schedule.ScheduleType.DAILY, startTimeStamp);
            Assert.assertTrue(scheduleRecord != null);

            DailySchedule dailySchedule = new DailySchedule(scheduleRecord, rootTask);

            for (Time time : times) {
                Assert.assertTrue(time != null);

                DailyScheduleTimeRecord dailyScheduleTimeRecord = persistenceManger.createDailyScheduleTimeRecord(dailySchedule, time);
                Assert.assertTrue(dailyScheduleTimeRecord != null);

                dailySchedule.addDailyScheduleTime(new DailyScheduleTime(dailyScheduleTimeRecord));
            }

            return dailySchedule;
        }

        public WeeklySchedule createWeeklySchedule(Task rootTask, ArrayList<Pair<DayOfWeek, Time>> dayOfWeekTimePairs, TimeStamp startTimeStamp) {
            Assert.assertTrue(rootTask != null);
            Assert.assertTrue(dayOfWeekTimePairs != null);
            Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());
            Assert.assertTrue(startTimeStamp != null);
            Assert.assertTrue(rootTask.current(startTimeStamp));

            PersistenceManger persistenceManger = PersistenceManger.getInstance();

            ScheduleRecord scheduleRecord = persistenceManger.createScheduleRecord(rootTask, Schedule.ScheduleType.WEEKLY, startTimeStamp);
            Assert.assertTrue(scheduleRecord != null);

            WeeklySchedule weeklySchedule = new WeeklySchedule(scheduleRecord, rootTask);

            for (Pair<DayOfWeek, Time> dayOfWeekTimePair : dayOfWeekTimePairs) {
                Assert.assertTrue(dayOfWeekTimePair != null);

                DayOfWeek dayOfWeek = dayOfWeekTimePair.first;
                Time time = dayOfWeekTimePair.second;

                Assert.assertTrue(dayOfWeek != null);
                Assert.assertTrue(time != null);

                WeeklyScheduleDayOfWeekTimeRecord weeklyScheduleDayOfWeekTimeRecord = persistenceManger.createWeeklyScheduleDayOfWeekTimeRecord(weeklySchedule, dayOfWeek, time);
                Assert.assertTrue(weeklyScheduleDayOfWeekTimeRecord != null);

                weeklySchedule.addWeeklyScheduleDayOfWeekTime(new WeeklyScheduleDayOfWeekTime(weeklyScheduleDayOfWeekTimeRecord));
            }

            return weeklySchedule;
        }

        ArrayList<Task> getChildTasks(Task parentTask, TimeStamp timeStamp) {
            Assert.assertTrue(timeStamp != null);
            Assert.assertTrue(parentTask != null);
            Assert.assertTrue(parentTask.current(timeStamp));

            ArrayList<Task> childTasks = new ArrayList<>();
            for (TaskHierarchy taskHierarchy : mTaskHierarchies.values())
                if (taskHierarchy.current(timeStamp) && taskHierarchy.getChildTask().current(timeStamp) && taskHierarchy.getParentTask() == parentTask)
                    childTasks.add(taskHierarchy.getChildTask());
            return childTasks;
        }

        Task getParentTask(Task childTask, TimeStamp timeStamp) {
            Assert.assertTrue(timeStamp != null);
            Assert.assertTrue(childTask != null);
            Assert.assertTrue(childTask.current(timeStamp));

            TaskHierarchy parentTaskHierarchy = getParentTaskHierarchy(childTask, timeStamp);
            if (parentTaskHierarchy == null) {
                return null;
            } else {
                Assert.assertTrue(parentTaskHierarchy.current(timeStamp));
                Task parentTask = parentTaskHierarchy.getParentTask();
                Assert.assertTrue(parentTask.current(timeStamp));
                return parentTask;
            }
        }

        private TaskHierarchy getParentTaskHierarchy(Task childTask, TimeStamp timeStamp) {
            Assert.assertTrue(childTask != null);
            Assert.assertTrue(timeStamp != null);
            Assert.assertTrue(childTask.current(timeStamp));

            ArrayList<TaskHierarchy> taskHierarchies = new ArrayList<>();
            for (TaskHierarchy taskHierarchy : mTaskHierarchies.values()) {
                Assert.assertTrue(taskHierarchy != null);

                if (!taskHierarchy.current(timeStamp))
                    continue;

                if (taskHierarchy.getChildTask() != childTask)
                    continue;

                taskHierarchies.add(taskHierarchy);
            }

            if (taskHierarchies.isEmpty()) {
                return null;
            } else {
                Assert.assertTrue(taskHierarchies.size() == 1);
                return taskHierarchies.get(0);
            }
        }

        void setParentHierarchyEndTimeStamp(Task childTask, TimeStamp endTimeStamp) {
            Assert.assertTrue(childTask != null);
            Assert.assertTrue(endTimeStamp != null);
            Assert.assertTrue(childTask.current(endTimeStamp));

            TaskHierarchy parentTaskHierarchy = getParentTaskHierarchy(childTask, endTimeStamp);
            if (parentTaskHierarchy != null) {
                Assert.assertTrue(parentTaskHierarchy.current(endTimeStamp));
                parentTaskHierarchy.setEndTimeStamp(endTimeStamp);
            }
        }
    }

    public class CustomTimeFactory {
        private final HashMap<Integer, CustomTime> mCustomTimes = new HashMap<>();

        CustomTimeFactory() {
            Collection<CustomTimeRecord> customTimeRecords = PersistenceManger.getInstance().getCustomTimeRecords();
            Assert.assertTrue(customTimeRecords != null);

            for (CustomTimeRecord customTimeRecord : customTimeRecords) {
                Assert.assertTrue(customTimeRecord != null);

                CustomTime customTime = new CustomTime(customTimeRecord);
                mCustomTimes.put(customTime.getId(), customTime);
            }
        }

        public CustomTime getCustomTime(int customTimeId) {
            Assert.assertTrue(mCustomTimes.containsKey(customTimeId));
            return mCustomTimes.get(customTimeId);
        }

        public CustomTime getCustomTime(DayOfWeek dayOfWeek, HourMinute hourMinute) {
            for (CustomTime customTime : mCustomTimes.values())
                if (customTime.getHourMinute(dayOfWeek).equals(hourMinute))
                    return customTime;
            return null;
        }

        public Collection<CustomTime> getCustomTimes() {
            return mCustomTimes.values();
        }

        public void createCustomTime(String name, HashMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes != null);

            Assert.assertTrue(hourMinutes.get(DayOfWeek.SUNDAY) != null);
            Assert.assertTrue(hourMinutes.get(DayOfWeek.MONDAY) != null);
            Assert.assertTrue(hourMinutes.get(DayOfWeek.TUESDAY) != null);
            Assert.assertTrue(hourMinutes.get(DayOfWeek.WEDNESDAY) != null);
            Assert.assertTrue(hourMinutes.get(DayOfWeek.THURSDAY) != null);
            Assert.assertTrue(hourMinutes.get(DayOfWeek.FRIDAY) != null);
            Assert.assertTrue(hourMinutes.get(DayOfWeek.SATURDAY) != null);

            CustomTimeRecord customTimeRecord = PersistenceManger.getInstance().createCustomTimeRecord(name, hourMinutes);
            Assert.assertTrue(customTimeRecord != null);

            CustomTime customTime = new CustomTime(customTimeRecord);
            mCustomTimes.put(customTime.getId(), customTime);
        }
    }
}
