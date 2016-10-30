package com.krystianwsul.checkme.domainmodel;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime;
import com.krystianwsul.checkme.domainmodel.local.LocalFactory;
import com.krystianwsul.checkme.domainmodel.local.LocalInstance;
import com.krystianwsul.checkme.domainmodel.local.LocalTask;
import com.krystianwsul.checkme.domainmodel.local.LocalTaskHierarchy;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.RemoteFactory;
import com.krystianwsul.checkme.firebase.RemoteInstance;
import com.krystianwsul.checkme.firebase.RemoteTask;
import com.krystianwsul.checkme.firebase.RemoteTaskHierarchy;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity;
import com.krystianwsul.checkme.gui.instances.ShowNotificationGroupActivity;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.loaders.EditInstanceLoader;
import com.krystianwsul.checkme.loaders.EditInstancesLoader;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.loaders.ShowCustomTimeLoader;
import com.krystianwsul.checkme.loaders.ShowCustomTimesLoader;
import com.krystianwsul.checkme.loaders.ShowGroupLoader;
import com.krystianwsul.checkme.loaders.ShowInstanceLoader;
import com.krystianwsul.checkme.loaders.ShowTaskLoader;
import com.krystianwsul.checkme.loaders.TaskListLoader;
import com.krystianwsul.checkme.notifications.GroupNotificationDeleteService;
import com.krystianwsul.checkme.notifications.InstanceDoneService;
import com.krystianwsul.checkme.notifications.InstanceHourService;
import com.krystianwsul.checkme.notifications.InstanceNotificationDeleteService;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord;
import com.krystianwsul.checkme.persistencemodel.PersistenceManger;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.Utils;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMilli;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.Time;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressLint("UseSparseArrays")
public class DomainFactory {
    private static DomainFactory sDomainFactory;

    private static ExactTimeStamp sStart;
    private static ExactTimeStamp sRead;
    private static ExactTimeStamp sStop;

    @Nullable
    private UserData mUserData;

    @Nullable
    private Map<String, UserData> mFriends;

    @Nullable
    private Query mRecordQuery;

    @Nullable
    private ValueEventListener mRecordListener;

    @Nullable
    private Query mFriendQuery;

    @Nullable
    private ValueEventListener mFriendListener;

    @NonNull
    private final LocalFactory mLocalFactory;

    @Nullable
    private RemoteFactory mRemoteFactory;

    public static synchronized DomainFactory getDomainFactory(Context context) {
        Assert.assertTrue(context != null);

        if (sDomainFactory == null) {
            sStart = ExactTimeStamp.getNow();

            sDomainFactory = new DomainFactory(context);

            sRead = ExactTimeStamp.getNow();

            sDomainFactory.initialize();

            sStop = ExactTimeStamp.getNow();
        }

        return sDomainFactory;
    }

    private DomainFactory(Context context) {
        mLocalFactory = LocalFactory.getInstance(context);
    }

    DomainFactory(PersistenceManger persistenceManger) {
        Assert.assertTrue(persistenceManger != null);

        mLocalFactory = new LocalFactory(persistenceManger);
    }

    private void initialize() {
        mLocalFactory.initialize(this);
    }

    public long getReadMillis() {
        return (sRead.getLong() - sStart.getLong());
    }

    public long getInstantiateMillis() {
        return (sStop.getLong() - sRead.getLong());
    }

    public synchronized void reset(@NonNull Context context) {
        UserData userData = mUserData;
        clearUserData(context);

        sDomainFactory = null;
        mLocalFactory.reset();

        if (userData != null)
            setUserData(context, userData);

        ObserverHolder.getObserverHolder().notifyDomainObservers(new ArrayList<>());

        ObserverHolder.getObserverHolder().clear();
    }

    public int getTaskCount() {
        return getTasks().size();
    }

    public int getInstanceCount() {
        return getExistingInstances().size();
    }

    public int getCustomTimeCount() {
        return getCustomTimes().size();
    }

    private void save(@NonNull Context context, int dataId) {
        ArrayList<Integer> dataIds = new ArrayList<>();
        dataIds.add(dataId);
        save(context, dataIds);
    }

    private void save(@NonNull Context context, @NonNull ArrayList<Integer> dataIds) {
        mLocalFactory.save(context);

        if (mRemoteFactory != null)
            mRemoteFactory.save();

        ObserverHolder.getObserverHolder().notifyDomainObservers(dataIds);
    }

    // firebase

    public synchronized void setUserData(@NonNull Context context, @NonNull UserData userData) {
        if (mUserData != null) {
            Assert.assertTrue(mRecordQuery != null);
            Assert.assertTrue(mFriendQuery != null);

            if (mUserData.equals(userData))
                return;

            clearUserData(context);
        }

        Assert.assertTrue(mUserData == null);

        Assert.assertTrue(mRecordQuery == null);
        Assert.assertTrue(mRecordListener == null);

        Assert.assertTrue(mFriendQuery == null);
        Assert.assertTrue(mFriendListener == null);

        mUserData = userData;

        mRecordQuery = DatabaseWrapper.getTaskRecordsQuery(userData);
        mRecordListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("asdf", "DomainFactory.mRecordListener.onDataChange, dataSnapshot: " + dataSnapshot);
                Assert.assertTrue(dataSnapshot != null);

                setRemoteTaskRecords(context.getApplicationContext(), dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Assert.assertTrue(databaseError != null);
                Log.e("asdf", "DomainFactory.mRecordListener.onCancelled", databaseError.toException());

                MyCrashlytics.logException(databaseError.toException());
            }
        };
        mRecordQuery.addValueEventListener(mRecordListener);

        mFriendQuery = DatabaseWrapper.getFriendsQuery(mUserData);
        mFriendListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("asdf", "DomainFactory.mFriendListener.onDataChange, dataSnapshot: " + dataSnapshot);
                Assert.assertTrue(dataSnapshot != null);

                setFriendRecords(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Assert.assertTrue(databaseError != null);
                Log.e("asdf", "DomainFactory.mFriendListener.onCancelled", databaseError.toException());

                MyCrashlytics.logException(databaseError.toException());
            }
        };
        mFriendQuery.addValueEventListener(mFriendListener);
    }

    public synchronized void clearUserData(@NonNull Context context) {
        ExactTimeStamp now = ExactTimeStamp.getNow();

        if (mUserData == null) {
            Assert.assertTrue(mRecordQuery == null);
            Assert.assertTrue(mRecordListener == null);
            Assert.assertTrue(mFriendQuery == null);
            Assert.assertTrue(mFriendListener == null);
        } else {
            Assert.assertTrue(mRecordQuery != null);
            Assert.assertTrue(mRecordListener != null);
            Assert.assertTrue(mFriendQuery != null);
            Assert.assertTrue(mFriendListener != null);

            mLocalFactory.clearRemoteCustomTimeRecords();

            mRemoteFactory = null;
            mFriends = null;

            mUserData = null;

            mRecordQuery.removeEventListener(mRecordListener);
            mRecordQuery = null;
            mRecordListener = null;

            mFriendQuery.removeEventListener(mFriendListener);
            mFriendQuery = null;
            mFriendListener = null;

            updateNotifications(context, new ArrayList<>(), now);

            ObserverHolder.getObserverHolder().notifyDomainObservers(new ArrayList<>());
        }
    }

    private synchronized void setRemoteTaskRecords(@NonNull Context context, @NonNull DataSnapshot dataSnapshot) {
        Assert.assertTrue(mUserData != null);

        mRemoteFactory = new RemoteFactory(this, dataSnapshot.getChildren(), mUserData);

        updateNotifications(context, new ArrayList<>(), ExactTimeStamp.getNow());

        ObserverHolder.getObserverHolder().notifyDomainObservers(new ArrayList<>());
    }

    private synchronized void setFriendRecords(@NonNull DataSnapshot dataSnapshot) {
        mFriends = Stream.of(dataSnapshot.getChildren())
                .map(child -> child.child("userData"))
                .map(userData -> userData.getValue(UserData.class))
                .collect(Collectors.toMap(userData -> UserData.getKey(userData.email), userData -> userData));

        ObserverHolder.getObserverHolder().notifyDomainObservers(new ArrayList<>());
    }

    // gets

    @SuppressWarnings("EmptyMethod")
    private void fakeDelay() {
        /*
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {

        }
        */
    }

    @NonNull
    public synchronized EditInstanceLoader.Data getEditInstanceData(@NonNull InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getEditInstanceData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Map<CustomTimeKey, CustomTime> currentCustomTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance.isRootInstance(now));
        Assert.assertTrue(instance.getDone() == null);

        if (instance.getInstanceTimePair().mCustomTimeKey != null) {
            CustomTime customTime = getCustomTime(instance.getInstanceTimePair().mCustomTimeKey);

            currentCustomTimes.put(customTime.getCustomTimeKey(), customTime);
        }

        Map<CustomTimeKey, EditInstanceLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : currentCustomTimes.values())
            customTimeDatas.put(customTime.getCustomTimeKey(), new EditInstanceLoader.CustomTimeData(customTime.getCustomTimeKey(), customTime.getName(), customTime.getHourMinutes()));

        return new EditInstanceLoader.Data(instance.getInstanceKey(), instance.getInstanceDate(), instance.getInstanceTimePair(), instance.getName(), customTimeDatas);
    }

    @NonNull
    public synchronized EditInstancesLoader.Data getEditInstancesData(@NonNull ArrayList<InstanceKey> instanceKeys) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getEditInstancesData");

        Assert.assertTrue(instanceKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Map<CustomTimeKey, CustomTime> currentCustomTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        HashMap<InstanceKey, EditInstancesLoader.InstanceData> instanceDatas = new HashMap<>();

        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = getInstance(instanceKey);
            Assert.assertTrue(instance.isRootInstance(now));
            Assert.assertTrue(instance.getDone() == null);

            instanceDatas.put(instanceKey, new EditInstancesLoader.InstanceData(instance.getInstanceDate(), instance.getName()));

            if (instance.getInstanceTimePair().mCustomTimeKey != null) {
                CustomTime customTime = getCustomTime(instance.getInstanceTimePair().mCustomTimeKey);

                currentCustomTimes.put(customTime.getCustomTimeKey(), customTime);
            }
        }

        Map<CustomTimeKey, EditInstancesLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : currentCustomTimes.values())
            customTimeDatas.put(customTime.getCustomTimeKey(), new EditInstancesLoader.CustomTimeData(customTime.getCustomTimeKey(), customTime.getName(), customTime.getHourMinutes()));

        return new EditInstancesLoader.Data(instanceDatas, customTimeDatas);
    }

    @NonNull
    public synchronized ShowCustomTimeLoader.Data getShowCustomTimeData(int localCustomTimeId) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowCustomTimeData");

        LocalCustomTime localCustomTime = mLocalFactory.getLocalCustomTime(localCustomTimeId);

        HashMap<DayOfWeek, HourMinute> hourMinutes = new HashMap<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values())
            hourMinutes.put(dayOfWeek, localCustomTime.getHourMinute(dayOfWeek));

        return new ShowCustomTimeLoader.Data(localCustomTime.getId(), localCustomTime.getName(), hourMinutes);
    }

    @NonNull
    public synchronized ShowCustomTimesLoader.Data getShowCustomTimesData() {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowCustomTimesData");

        List<LocalCustomTime> currentCustomTimes = getCurrentCustomTimes();

        ArrayList<ShowCustomTimesLoader.CustomTimeData> entries = new ArrayList<>();
        for (LocalCustomTime localCustomTime : currentCustomTimes) {
            Assert.assertTrue(localCustomTime != null);

            entries.add(new ShowCustomTimesLoader.CustomTimeData(localCustomTime.getId(), localCustomTime.getName()));
        }

        return new ShowCustomTimesLoader.Data(entries);
    }

    @NonNull
    public synchronized GroupListLoader.Data getGroupListData(@NonNull Context context, int position, @NonNull MainActivity.TimeRange timeRange) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getGroupListData");

        Assert.assertTrue(position >= 0);

        ExactTimeStamp startExactTimeStamp;
        ExactTimeStamp endExactTimeStamp;

        if (position == 0) {
            startExactTimeStamp = null;
        } else {
            Calendar startCalendar = Calendar.getInstance();

            switch (timeRange) {
                case DAY:
                    startCalendar.add(Calendar.DATE, position);
                    break;
                case WEEK:
                    startCalendar.add(Calendar.WEEK_OF_YEAR, position);
                    startCalendar.set(Calendar.DAY_OF_WEEK, startCalendar.getFirstDayOfWeek());
                    break;
                case MONTH:
                    startCalendar.add(Calendar.MONTH, position);
                    startCalendar.set(Calendar.DAY_OF_MONTH, 1);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            startExactTimeStamp = new ExactTimeStamp(new Date(startCalendar), new HourMilli(0, 0, 0, 0));
        }

        Calendar endCalendar = Calendar.getInstance();

        switch (timeRange) {
            case DAY:
                endCalendar.add(Calendar.DATE, position + 1);
                break;
            case WEEK:
                endCalendar.add(Calendar.WEEK_OF_YEAR, position + 1);
                endCalendar.set(Calendar.DAY_OF_WEEK, endCalendar.getFirstDayOfWeek());
                break;
            case MONTH:
                endCalendar.add(Calendar.MONTH, position + 1);
                endCalendar.set(Calendar.DAY_OF_MONTH, 1);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        endExactTimeStamp = new ExactTimeStamp(new Date(endCalendar), new HourMilli(0, 0, 0, 0));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<Instance> currentInstances = getRootInstances(startExactTimeStamp, endExactTimeStamp, now);

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        List<GroupListLoader.TaskData> taskDatas = null;
        if (position == 0) {
            taskDatas = Stream.of(getTasks())
                    .filter(task -> task.current(now))
                    .filter(task -> task.isVisible(now))
                    .filter(task -> task.isRootTask(now))
                    .filter(task -> task.getCurrentSchedules(now).isEmpty())
                    .map(task -> new GroupListLoader.TaskData(task.getTaskKey(), task.getName(), getChildTaskDatas(task, now), task.getStartExactTimeStamp()))
                    .collect(Collectors.toList());
        }

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, null, taskDatas, null);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances) {
            Task task = getTask(instance.getTaskKey());

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), data, instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), task.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(instance, now, instanceData));
            instanceDatas.put(instanceData.InstanceKey, instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    @NonNull
    public synchronized ShowGroupLoader.Data getShowGroupData(@NonNull Context context, @NonNull TimeStamp timeStamp) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowGroupData");

        Calendar endCalendar = timeStamp.getCalendar();
        endCalendar.add(Calendar.MINUTE, 1);
        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<Instance> rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now);

        List<Instance> currentInstances = Stream.of(rootInstances)
                .filter(instance -> instance.getInstanceDateTime().getTimeStamp().compareTo(timeStamp) == 0)
                .collect(Collectors.toList());

        Date date = timeStamp.getDate();
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        HourMinute hourMinute = timeStamp.getHourMinute();

        Time time = null;
        for (CustomTime customTime : getCurrentCustomTimes())
            if (customTime.getHourMinute(dayOfWeek).equals(hourMinute))
                time = customTime;
        if (time == null)
            time = new NormalTime(hourMinute);

        String displayText = new DateTime(date, time).getDisplayText(context);

        return new ShowGroupLoader.Data(displayText, !currentInstances.isEmpty());
    }

    @NonNull
    public synchronized GroupListLoader.Data getGroupListData(@NonNull TimeStamp timeStamp) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getGroupListData");

        Calendar endCalendar = timeStamp.getCalendar();
        endCalendar.add(Calendar.MINUTE, 1);
        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<Instance> rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now);

        List<Instance> currentInstances = Stream.of(rootInstances)
                .filter(instance -> instance.getInstanceDateTime().getTimeStamp().compareTo(timeStamp) == 0)
                .collect(Collectors.toList());

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, null, null, null);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances) {
            Task task = getTask(instance.getTaskKey());

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(instance.getDone(), instance.getInstanceKey(), null, instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), data, instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), task.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(instance, now, instanceData));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    @NonNull
    public synchronized GroupListLoader.Data getGroupListData(@NonNull InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getGroupListData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        Map<TaskKey, Task> taskMap = getTaskMap();
        if (taskMap.containsKey(instanceKey.mTaskKey)) {
            Task task = taskMap.get(instanceKey.mTaskKey);
            Assert.assertTrue(task != null);

            Instance instance = getInstance(instanceKey);

            GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, task.current(now), null, task.getNote());

            for (Instance childInstance : instance.getChildInstances(now)) {
                Task childTask = getTask(childInstance.getTaskKey());

                Boolean isRootTask = (childTask.current(now) ? childTask.isRootTask(now) : null);

                GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(childInstance.getDone(), childInstance.getInstanceKey(), null, childInstance.getName(), childInstance.getInstanceDateTime().getTimeStamp(), childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), data, childInstance.getInstanceDateTime().getTime().getTimePair(), childTask.getNote(), childTask.getStartExactTimeStamp());
                instanceData.setChildren(getChildInstanceDatas(childInstance, now, instanceData));
                instanceDatas.put(childInstance.getInstanceKey(), instanceData);
            }

            data.setInstanceDatas(instanceDatas);

            return data;
        } else { // todo probably should wrap all data in this loader in a nullable object at some point
            GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, false, null, null);

            data.setInstanceDatas(new HashMap<>());

            return data;
        }
    }

    @NonNull
    public synchronized GroupListLoader.Data getGroupListData(@NonNull Context context, @NonNull ArrayList<InstanceKey> instanceKeys) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getGroupListData");

        Assert.assertTrue(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        ArrayList<Instance> instances = new ArrayList<>();
        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = getInstance(instanceKey);

            if (instance.isRootInstance(now))
                instances.add(instance);
        }

        Collections.sort(instances, (lhs, rhs) -> lhs.getInstanceDateTime().compareTo(rhs.getInstanceDateTime()));

        List<GroupListLoader.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListLoader.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        GroupListLoader.Data data = new GroupListLoader.Data(customTimeDatas, null, null, null);

        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : instances) {
            Task task = instance.getTask();

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), data, instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), task.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(instance, now, instanceData));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        data.setInstanceDatas(instanceDatas);

        return data;
    }

    @NonNull
    public synchronized ShowInstanceLoader.Data getShowInstanceData(@NonNull Context context, @NonNull InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowInstanceData");

        if (!getTaskMap().containsKey(instanceKey.mTaskKey))
            return new ShowInstanceLoader.Data(null);

        Task task = getTask(instanceKey.mTaskKey);
        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);
        return new ShowInstanceLoader.Data(new ShowInstanceLoader.InstanceData(instance.getInstanceKey(), instance.getName(), instance.getDisplayText(context, now), instance.getDone() != null, task.current(now), instance.isRootInstance(now), isRootTask));
    }

    @NonNull
    public synchronized CreateTaskLoader.Data getCreateTaskData(@Nullable TaskKey taskKey, @NonNull Context context, @NonNull List<TaskKey> excludedTaskKeys) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getCreateTaskData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Map<CustomTimeKey, CustomTime> customTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        CreateTaskLoader.TaskData taskData = null;
        if (taskKey != null) {
            Task task = getTask(taskKey);

            TaskKey parentTaskKey;
            List<CreateTaskLoader.ScheduleData> scheduleDatas = null;

            if (task.isRootTask(now)) {
                List<Schedule> schedules = task.getCurrentSchedules(now);

                parentTaskKey = null;

                if (!schedules.isEmpty()) {
                    scheduleDatas = new ArrayList<>();

                    for (Schedule schedule : schedules) {
                        Assert.assertTrue(schedule != null);
                        Assert.assertTrue(schedule.current(now));

                        switch (schedule.getType()) {
                            case SINGLE: {
                                SingleSchedule singleSchedule = (SingleSchedule) schedule;

                                scheduleDatas.add(new CreateTaskLoader.SingleScheduleData(singleSchedule.getDate(), singleSchedule.getTime().getTimePair()));

                                CustomTime weeklyCustomTime = singleSchedule.getTime().getPair().first;
                                if (weeklyCustomTime != null)
                                    customTimes.put(weeklyCustomTime.getCustomTimeKey(), weeklyCustomTime);
                                break;
                            }
                            case DAILY: {
                                DailySchedule dailySchedule = (DailySchedule) schedule;

                                Time time = dailySchedule.getTime();

                                scheduleDatas.add(new CreateTaskLoader.DailyScheduleData(time.getTimePair()));

                                CustomTime dailyCustomTime = time.getPair().first;
                                if (dailyCustomTime != null)
                                    customTimes.put(dailyCustomTime.getCustomTimeKey(), dailyCustomTime);

                                break;
                            }
                            case WEEKLY: {
                                WeeklySchedule weeklySchedule = (WeeklySchedule) schedule;

                                Pair<DayOfWeek, Time> pair = weeklySchedule.getDayOfWeekTime();

                                scheduleDatas.add(new CreateTaskLoader.WeeklyScheduleData(pair.first, pair.second.getTimePair()));

                                CustomTime weeklyCustomTime = pair.second.getPair().first;
                                if (weeklyCustomTime != null)
                                    customTimes.put(weeklyCustomTime.getCustomTimeKey(), weeklyCustomTime);

                                break;
                            }
                            case MONTHLY_DAY: {
                                MonthlyDaySchedule monthlyDaySchedule = (MonthlyDaySchedule) schedule;

                                scheduleDatas.add(new CreateTaskLoader.MonthlyDayScheduleData(monthlyDaySchedule.getDayOfMonth(), monthlyDaySchedule.getBeginningOfMonth(), monthlyDaySchedule.getTime().getTimePair()));

                                CustomTime weeklyCustomTime = monthlyDaySchedule.getTime().getPair().first;
                                if (weeklyCustomTime != null)
                                    customTimes.put(weeklyCustomTime.getCustomTimeKey(), weeklyCustomTime);

                                break;
                            }
                            case MONTHLY_WEEK: {
                                MonthlyWeekSchedule monthlyWeekSchedule = (MonthlyWeekSchedule) schedule;

                                scheduleDatas.add(new CreateTaskLoader.MonthlyWeekScheduleData(monthlyWeekSchedule.getDayOfMonth(), monthlyWeekSchedule.getDayOfWeek(), monthlyWeekSchedule.getBeginningOfMonth(), monthlyWeekSchedule.getTime().getTimePair()));

                                CustomTime weeklyCustomTime = monthlyWeekSchedule.getTime().getPair().first;
                                if (weeklyCustomTime != null)
                                    customTimes.put(weeklyCustomTime.getCustomTimeKey(), weeklyCustomTime);

                                break;
                            }
                            default: {
                                throw new UnsupportedOperationException();
                            }
                        }

                    }
                }
            } else {
                Task parentTask = task.getParentTask(now);
                Assert.assertTrue(parentTask != null);

                parentTaskKey = parentTask.getTaskKey();
            }

            Set<UserData> friends;
            if (task.getRecordOf().isEmpty() || (parentTaskKey != null)) {
                friends = new HashSet<>();
            } else {
                Assert.assertTrue(mFriends != null);

                friends = Stream.of(mFriends.values())
                        .filter(userData -> task.getRecordOf().contains(UserData.getKey(userData.email)))
                        .collect(Collectors.toSet());
            }

            taskData = new CreateTaskLoader.TaskData(task.getName(), parentTaskKey, scheduleDatas, task.getNote(), friends);
        }

        Map<TaskKey, CreateTaskLoader.TaskTreeData> taskDatas = getTaskDatas(context, now, excludedTaskKeys);

        @SuppressLint("UseSparseArrays") HashMap<CustomTimeKey, CreateTaskLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes.values())
            customTimeDatas.put(customTime.getCustomTimeKey(), new CreateTaskLoader.CustomTimeData(customTime.getCustomTimeKey(), customTime.getName(), customTime.getHourMinutes()));

        Set<UserData> friends = (mFriends != null ? new HashSet<>(mFriends.values()) : new HashSet<>());

        return new CreateTaskLoader.Data(taskData, taskDatas, customTimeDatas, friends);
    }

    @NonNull
    public synchronized ShowTaskLoader.Data getShowTaskData(@NonNull TaskKey taskKey, @NonNull Context context) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowTaskData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = getTask(taskKey);
        Assert.assertTrue(task.current(now));

        return new ShowTaskLoader.Data(task.isRootTask(now), task.getName(), task.getScheduleText(context, now), task.getTaskKey());
    }

    @NonNull
    public synchronized TaskListLoader.Data getTaskListData(@NonNull Context context, @Nullable TaskKey taskKey) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getTaskListData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        return getTaskListData(now, context, taskKey);
    }

    // sets

    public synchronized void setInstanceDateTime(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.log("DomainFactory.setInstanceDateTime");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setInstanceDateTime(instanceDate, instanceTimePair, now);

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);
    }

    public synchronized void setInstancesDateTime(@NonNull Context context, int dataId, @NonNull Set<InstanceKey> instanceKeys, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.log("DomainFactory.setInstancesDateTime");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(instanceKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = getInstance(instanceKey);
            instance.setInstanceDateTime(instanceDate, instanceTimePair, now);
        }

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);
    }

    public synchronized void setInstanceAddHour(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceAddHour");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        instance.setInstanceDateTime(date, new TimePair(hourMinute), now);
        instance.setNotificationShown(false, now);

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);
    }

    public synchronized void setInstanceNotificationDone(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotificationDone");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setDone(true, now);
        instance.setNotificationShown(false, now);
        instance.setNotified(now);

        save(context, dataId);
    }

    @NonNull
    public synchronized ExactTimeStamp setInstancesDone(@NonNull Context context, int dataId, @NonNull List<InstanceKey> instanceKeys) {
        MyCrashlytics.log("DomainFactory.setInstancesDone");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Stream.of(instanceKeys).forEach(instanceKey -> {
            Assert.assertTrue(instanceKey != null);

            Instance instance = getInstance(instanceKey);

            instance.setDone(true, now);
        });

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);

        return now;
    }

    public synchronized ExactTimeStamp setInstanceDone(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey, boolean done) {
        MyCrashlytics.log("DomainFactory.setInstanceDone");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Instance instance = setInstanceDone(now, instanceKey, done);

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);

        return instance.getDone();
    }

    public synchronized void setInstancesNotified(@NonNull Context context, int dataId, @NonNull ArrayList<InstanceKey> instanceKeys) {
        MyCrashlytics.log("DomainFactory.setInstancesNotified");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = getInstance(instanceKey);

            instance.setNotified(now);
            instance.setNotificationShown(false, now);
        }

        save(context, dataId);
    }

    public synchronized void setInstanceNotified(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotified");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setNotified(now);
        instance.setNotificationShown(false, now);

        save(context, dataId);
    }

    public synchronized void createScheduleRootTask(@NonNull Context context, int dataId, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @NonNull List<UserData> friendEntries) {
        MyCrashlytics.log("DomainFactory.createScheduleRootTask");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        if (friendEntries.isEmpty()) {
            mLocalFactory.createScheduleRootTask(this, now, name, scheduleDatas, note);
        } else {
            Assert.assertTrue(mRemoteFactory != null);

            mRemoteFactory.createScheduleRootTask(now, name, scheduleDatas, note, Utils.userDatasToKeys(friendEntries));
        }

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);
    }

    @NonNull
    public synchronized TaskKey updateScheduleTask(@NonNull Context context, int dataId, @NonNull TaskKey taskKey, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @NonNull List<UserData> friendEntries) {
        MyCrashlytics.log("DomainFactory.updateScheduleTask");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = getTask(taskKey);
        Assert.assertTrue(task.current(now));

        task = task.updateFriends(Utils.userDatasToKeys(friendEntries), context, now);

        List<TaskKey> taskKeys = new ArrayList<>();
        taskKeys.add(task.getTaskKey());

        task.setName(name, note);

        if (task.isRootTask(now)) {
            List<Schedule> schedules = task.getCurrentSchedules(now);

            Stream.of(schedules)
                    .forEach(schedule -> schedule.setEndExactTimeStamp(now));
        } else {
            TaskHierarchy taskHierarchy = getParentTaskHierarchy(task, now);
            Assert.assertTrue(taskHierarchy != null);

            taskHierarchy.setEndExactTimeStamp(now);

            taskKeys.add(taskHierarchy.getParentTaskKey());
        }

        task.addSchedules(scheduleDatas, now);

        updateNotifications(context, taskKeys, now);

        save(context, dataId);

        return task.getTaskKey();
    }

    public synchronized void createScheduleJoinRootTask(@NonNull Context context, int dataId, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note, @NonNull List<UserData> friendEntries) {
        MyCrashlytics.log("DomainFactory.createScheduleJoinRootTask");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());
        Assert.assertTrue(joinTaskKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Set<String> mergedFriends = new HashSet<>(Utils.userDatasToKeys(friendEntries));

        List<Task> joinTasks = Stream.of(joinTaskKeys)
                .map(this::getTask)
                .collect(Collectors.toList());

        for (Task task : joinTasks)
            mergedFriends.addAll(task.getRecordOf());

        Task newParentTask;
        if (mUserData != null) {
            Assert.assertTrue(mRemoteFactory != null);

            mergedFriends.remove(UserData.getKey(mUserData.email));

            newParentTask = mRemoteFactory.createScheduleRootTask(now, name, scheduleDatas, note, mergedFriends);
        } else {
            Assert.assertTrue(mergedFriends.isEmpty());

            newParentTask = mLocalFactory.createScheduleRootTask(this, now, name, scheduleDatas, note);
        }

        joinTasks = Stream.of(joinTasks)
                .map(joinTask -> joinTask.updateFriends(mergedFriends, context, now))
                .collect(Collectors.toList());

        List<TaskKey> taskKeys = Stream.of(joinTasks)
                .map(task -> task.getParentTask(now))
                .filter(parentTask -> parentTask != null)
                .map(Task::getTaskKey)
                .collect(Collectors.toList());

        joinTasks(newParentTask, joinTasks, now);

        updateNotifications(context, taskKeys, now);

        save(context, dataId);
    }

    public synchronized void createChildTask(@NonNull Context context, int dataId, @NonNull TaskKey parentTaskKey, @NonNull String name, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.createChildTask");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task parentTask = getTask(parentTaskKey);
        Assert.assertTrue(parentTask.current(now));

        parentTask.createChildTask(now, name, note);

        updateNotifications(context, Collections.singletonList(parentTaskKey), now);

        save(context, dataId);
    }

    public synchronized void createJoinChildTask(@NonNull Context context, int dataId, @NonNull TaskKey parentTaskKey, @NonNull String name, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.createJoinChildTask");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task parentTask = getTask(parentTaskKey);
        Assert.assertTrue(parentTask.current(now));

        Set<String> mergedFriends = new HashSet<>(parentTask.getRecordOf());

        List<Task> joinTasks = Stream.of(joinTaskKeys)
                .map(this::getTask)
                .collect(Collectors.toList());

        for (Task task : joinTasks)
            mergedFriends.addAll(task.getRecordOf());

        parentTask = parentTask.updateFriends(mergedFriends, context, now);

        joinTasks = Stream.of(joinTasks)
                .map(joinTask -> joinTask.updateFriends(mergedFriends, context, now))
                .collect(Collectors.toList());

        List<TaskKey> taskKeys = Stream.of(joinTasks)
                .map(task -> task.getParentTask(now))
                .filter(joinParentTask -> joinParentTask != null)
                .map(Task::getTaskKey)
                .collect(Collectors.toList());

        taskKeys.add(parentTask.getTaskKey());

        Task childTask = parentTask.createChildTask(now, name, note);

        joinTasks(childTask, joinTasks, now);

        updateNotifications(context, taskKeys, now);

        save(context, dataId);
    }

    @NonNull
    public synchronized TaskKey updateChildTask(@NonNull Context context, int dataId, @NonNull TaskKey taskKey, @NonNull String name, @NonNull TaskKey parentTaskKey, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.updateChildTask");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<TaskKey> taskKeys = new ArrayList<>();

        Task task = getTask(taskKey);
        Assert.assertTrue(task.current(now));

        Task newParentTask = getTask(parentTaskKey);
        Assert.assertTrue(task.current(now));

        Set<String> mergedFriends = new HashSet<>(task.getRecordOf());
        mergedFriends.addAll(newParentTask.getRecordOf());
        if (mUserData != null) {
            mergedFriends.remove(UserData.getKey(mUserData.email));
        } else {
            Assert.assertTrue(mergedFriends.isEmpty());
        }

        task = task.updateFriends(new HashSet<>(mergedFriends), context, now);
        newParentTask = newParentTask.updateFriends(mergedFriends, context, now);

        taskKeys.add(task.getTaskKey());

        task.setName(name, note);

        taskKeys.add(newParentTask.getTaskKey());

        Task oldParentTask = task.getParentTask(now);
        if (oldParentTask == null) {
            Stream.of(task.getCurrentSchedules(now))
                    .forEach(schedule -> schedule.setEndExactTimeStamp(now));

            newParentTask.addChild(task, now);
        } else if (oldParentTask != newParentTask) {
            TaskHierarchy oldTaskHierarchy = getParentTaskHierarchy(task, now);
            Assert.assertTrue(oldTaskHierarchy != null);

            oldTaskHierarchy.setEndExactTimeStamp(now);

            taskKeys.add(oldTaskHierarchy.getParentTaskKey());
        }

        updateNotifications(context, taskKeys, now);

        save(context, dataId);

        return task.getTaskKey();
    }

    public synchronized void setTaskEndTimeStamp(@NonNull Context context, @NonNull ArrayList<Integer> dataIds, @NonNull TaskKey taskKey) {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamp");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!dataIds.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = getTask(taskKey);
        Assert.assertTrue(task.current(now));

        task.setEndExactTimeStamp(now);

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataIds);
    }

    public synchronized void setTaskEndTimeStamps(@NonNull Context context, int dataId, @NonNull ArrayList<TaskKey> taskKeys) {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!taskKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        for (TaskKey taskKey : taskKeys) {
            Assert.assertTrue(taskKey != null);

            Task task = getTask(taskKey);
            Assert.assertTrue(task.current(now));

            task.setEndExactTimeStamp(now);
        }

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);
    }

    public synchronized int createCustomTime(@NonNull Context context, @NonNull String name, @NonNull Map<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.log("DomainFactory.createCustomTime");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));

        Assert.assertTrue(hourMinutes.get(DayOfWeek.SUNDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.MONDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.TUESDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.WEDNESDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.THURSDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.FRIDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.SATURDAY) != null);

        LocalCustomTime localCustomTime = mLocalFactory.createLocalCustomTime(name, hourMinutes);

        save(context, 0);

        return localCustomTime.getId();
    }

    public synchronized void updateCustomTime(@NonNull Context context, int dataId, int localCustomTimeId, @NonNull String name, @NonNull Map<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.log("DomainFactory.updateCustomTime");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));

        LocalCustomTime localCustomTime = mLocalFactory.getLocalCustomTime(localCustomTimeId);

        localCustomTime.setName(name);

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            HourMinute hourMinute = hourMinutes.get(dayOfWeek);
            Assert.assertTrue(hourMinute != null);

            if (hourMinute.compareTo(localCustomTime.getHourMinute(dayOfWeek)) != 0)
                localCustomTime.setHourMinute(dayOfWeek, hourMinute);
        }

        save(context, dataId);
    }

    public synchronized void setCustomTimeCurrent(@NonNull Context context, int dataId, @NonNull List<Integer> localCustomTimeIds) {
        MyCrashlytics.log("DomainFactory.setCustomTimeCurrent");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!localCustomTimeIds.isEmpty());

        for (int localCustomTimeId : localCustomTimeIds) {
            LocalCustomTime localCustomTime = mLocalFactory.getLocalCustomTime(localCustomTimeId);

            localCustomTime.setCurrent();
        }

        save(context, dataId);
    }

    public synchronized void createRootTask(@NonNull Context context, int dataId, @NonNull String name, @Nullable String note, @NonNull List<UserData> friendEntries) {
        MyCrashlytics.log("DomainFactory.createRootTask");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        if (friendEntries.isEmpty()) {
            mLocalFactory.createLocalTaskHelper(this, name, now, note);
        } else {
            Assert.assertTrue(mRemoteFactory != null);

            mRemoteFactory.createRemoteTaskHelper(now, name, note, Utils.userDatasToKeys(friendEntries));
        }

        updateNotifications(context, new ArrayList<>(), now);

        save(context, dataId);
    }

    public synchronized void createJoinRootTask(@NonNull Context context, int dataId, @NonNull String name, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note, @NonNull List<UserData> friendEntries) {
        MyCrashlytics.log("DomainFactory.createJoinRootTask");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Set<String> mergedFriends = new HashSet<>(Utils.userDatasToKeys(friendEntries));

        List<Task> joinTasks = Stream.of(joinTaskKeys)
                .map(this::getTask)
                .collect(Collectors.toList());

        for (Task task : joinTasks)
            mergedFriends.addAll(task.getRecordOf());

        Task newParentTask;
        if (mUserData != null) {
            Assert.assertTrue(mRemoteFactory != null);

            mergedFriends.remove(UserData.getKey(mUserData.email));

            newParentTask = mRemoteFactory.createRemoteTaskHelper(now, name, note, mergedFriends);
        } else {
            Assert.assertTrue(mergedFriends.isEmpty());

            newParentTask = mLocalFactory.createLocalTaskHelper(this, name, now, note);
        }

        joinTasks = Stream.of(joinTasks)
                .map(joinTask -> joinTask.updateFriends(mergedFriends, context, now))
                .collect(Collectors.toList());

        List<TaskKey> taskKeys = Stream.of(joinTasks)
                .map(task -> task.getParentTask(now))
                .filter(parentTask -> parentTask != null)
                .map(Task::getTaskKey)
                .collect(Collectors.toList());

        joinTasks(newParentTask, joinTasks, now);

        updateNotifications(context, taskKeys, now);

        save(context, dataId);
    }

    @NonNull
    public synchronized TaskKey updateRootTask(@NonNull Context context, int dataId, @NonNull TaskKey taskKey, @NonNull String name, @Nullable String note, @NonNull List<UserData> friendEntries) {
        MyCrashlytics.log("DomainFactory.updateRootTask");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = getTask(taskKey);
        Assert.assertTrue(task.current(now));

        task = task.updateFriends(Utils.userDatasToKeys(friendEntries), context, now);

        task.setName(name, note);

        List<TaskKey> taskKeys = new ArrayList<>();

        TaskHierarchy taskHierarchy = getParentTaskHierarchy(task, now);
        if (taskHierarchy != null) {
            taskHierarchy.setEndExactTimeStamp(now);
            taskKeys.add(taskHierarchy.getParentTaskKey());
        }

        Stream.of(task.getCurrentSchedules(now))
                .forEach(schedule -> schedule.setEndExactTimeStamp(now));

        updateNotifications(context, taskKeys, now);

        save(context, dataId);

        return task.getTaskKey();
    }

    public synchronized void updateNotifications(@NonNull Context context, boolean silent, boolean registering, @NonNull List<TaskKey> taskKeys) {
        MyCrashlytics.log("DomainFactory.updateNotifications");
        Assert.assertTrue(mRemoteFactory == null || !mRemoteFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        updateNotifications(context, silent, registering, taskKeys, now, new ArrayList<>());

        Irrelevant irrelevant = setIrrelevant(now);

        save(context, 0);

        removeIrrelevant(irrelevant);
    }

    // internal

    @NonNull
    private List<Instance> getExistingInstances(@NonNull Task task) {
        TaskKey taskKey = task.getTaskKey();

        return Stream.of(getExistingInstances())
                .filter(instance -> instance.getTaskKey().equals(taskKey))
                .collect(Collectors.toList());
    }

    @Nullable
    Instance getExistingInstance(@NonNull Task task, @NonNull DateTime scheduleDateTime) {
        List<Instance> taskInstances = getExistingInstances(task);

        ArrayList<Instance> instances = new ArrayList<>();
        for (Instance instance : taskInstances) {
            Assert.assertTrue(instance != null);
            if (instance.getScheduleDateTime().compareTo(scheduleDateTime) == 0)
                instances.add(instance);
        }

        if (!instances.isEmpty()) {
            Assert.assertTrue(instances.size() == 1);
            return instances.get(0);
        } else {
            return null;
        }
    }

    @NonNull
    public String getRemoteCustomTimeId(@NonNull CustomTimeKey customTimeKey) {
        if (!TextUtils.isEmpty(customTimeKey.mRemoteCustomTimeId)) {
            Assert.assertTrue(customTimeKey.mLocalCustomTimeId == null);

            return customTimeKey.mRemoteCustomTimeId;
        } else {
            Assert.assertTrue(customTimeKey.mLocalCustomTimeId != null);

            LocalCustomTime localCustomTime = mLocalFactory.getLocalCustomTime(customTimeKey.mLocalCustomTimeId);

            Assert.assertTrue(localCustomTime.hasRemoteRecord());

            return localCustomTime.getRemoteId();
        }
    }

    @NonNull
    public Instance getInstance(@NonNull Task task, @NonNull DateTime scheduleDateTime) {
        Instance existingInstance = getExistingInstance(task, scheduleDateTime);

        if (existingInstance != null) {
            return existingInstance;
        } else {
            if (task.getTaskKey().mLocalTaskId != null) {
                Assert.assertTrue(TextUtils.isEmpty(task.getTaskKey().mRemoteTaskId));

                return new LocalInstance(this, task.getTaskKey().mLocalTaskId, scheduleDateTime);
            } else {
                Assert.assertTrue(!TextUtils.isEmpty(task.getTaskKey().mRemoteTaskId));

                String remoteCustomTimeId;
                Integer hour;
                Integer minute;

                CustomTimeKey customTimeKey = scheduleDateTime.getTime().getTimePair().mCustomTimeKey;
                HourMinute hourMinute = scheduleDateTime.getTime().getTimePair().mHourMinute;

                if (customTimeKey != null) {
                    Assert.assertTrue(hourMinute == null);

                    remoteCustomTimeId = getRemoteCustomTimeId(customTimeKey);

                    hour = null;
                    minute = null;
                } else {
                    Assert.assertTrue(hourMinute != null);

                    remoteCustomTimeId = null;

                    hour = hourMinute.getHour();
                    minute = hourMinute.getMinute();
                }

                InstanceShownRecord instanceShownRecord = mLocalFactory.getInstanceShownRecord(task.getTaskKey().mRemoteTaskId, scheduleDateTime.getDate().getYear(), scheduleDateTime.getDate().getMonth(), scheduleDateTime.getDate().getDay(), remoteCustomTimeId, hour, minute);

                return new RemoteInstance(this, task.getTaskKey().mRemoteTaskId, scheduleDateTime, instanceShownRecord);
            }
        }
    }

    @NonNull
    List<Instance> getPastInstances(@NonNull Task task, @NonNull ExactTimeStamp now) {
        Map<InstanceKey, Instance> allInstances = new HashMap<>();

        allInstances.putAll(Stream.of(getExistingInstances())
                .filter(instance -> instance.getTaskKey().equals(task.getTaskKey()))
                .filter(instance -> instance.getScheduleDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) <= 0)
                .collect(Collectors.toMap(Instance::getInstanceKey, instance -> instance)));

        allInstances.putAll(Stream.of(task.getInstances(null, now.plusOne(), now))
                .collect(Collectors.toMap(Instance::getInstanceKey, instance -> instance)));

        return new ArrayList<>(allInstances.values());
    }

    @NonNull
    private List<Instance> getRootInstances(@Nullable ExactTimeStamp startExactTimeStamp, @NonNull ExactTimeStamp endExactTimeStamp, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(startExactTimeStamp == null || startExactTimeStamp.compareTo(endExactTimeStamp) < 0);

        Map<InstanceKey, Instance> allInstances = new HashMap<>();

        for (Instance instance : getExistingInstances()) {
            ExactTimeStamp instanceExactTimeStamp = instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp();

            if (startExactTimeStamp != null && startExactTimeStamp.compareTo(instanceExactTimeStamp) > 0)
                continue;

            if (endExactTimeStamp.compareTo(instanceExactTimeStamp) <= 0)
                continue;

            allInstances.put(instance.getInstanceKey(), instance);
        }

        for (Task task : getTasks()) {
            for (Instance instance : task.getInstances(startExactTimeStamp, endExactTimeStamp, now)) {
                ExactTimeStamp instanceExactTimeStamp = instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp();

                if (startExactTimeStamp != null && startExactTimeStamp.compareTo(instanceExactTimeStamp) > 0)
                    continue;

                if (endExactTimeStamp.compareTo(instanceExactTimeStamp) <= 0)
                    continue;

                allInstances.put(instance.getInstanceKey(), instance);
            }
        }

        return Stream.of(allInstances.values())
                .filter(instance -> instance.isRootInstance(now))
                .filter(instance -> instance.isVisible(now))
                .collect(Collectors.toList());
    }

    @NonNull
    public Time getTime(@NonNull TimePair timePair) {
        if (timePair.mHourMinute != null) {
            Assert.assertTrue(timePair.mCustomTimeKey == null);

            return new NormalTime(timePair.mHourMinute);
        } else {
            Assert.assertTrue(timePair.mCustomTimeKey != null);

            return getCustomTime(timePair.mCustomTimeKey);
        }
    }

    @NonNull
    private DateTime getDateTime(@NonNull Date date, @NonNull TimePair timePair) {
        return new DateTime(date, getTime(timePair));
    }

    @NonNull
    private Instance getInstance(@NonNull InstanceKey instanceKey) {
        Task task = getTask(instanceKey.mTaskKey);

        DateTime scheduleDateTime = getDateTime(instanceKey.ScheduleDate, instanceKey.ScheduleTimePair);

        return getInstance(task, scheduleDateTime);
    }

    @NonNull
    List<TaskHierarchy> getParentTaskHierarchies(Task childTask) {
        Assert.assertTrue(childTask != null);

        return Stream.of(getTaskHierarchies())
                .filter(taskHierarchy -> taskHierarchy.getChildTask() == childTask)
                .collect(Collectors.toList());
    }

    @Nullable
    Task getParentTask(@NonNull Task childTask, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(childTask.current(exactTimeStamp));

        TaskHierarchy parentTaskHierarchy = getParentTaskHierarchy(childTask, exactTimeStamp);
        if (parentTaskHierarchy == null) {
            return null;
        } else {
            Assert.assertTrue(parentTaskHierarchy.current(exactTimeStamp));
            Task parentTask = parentTaskHierarchy.getParentTask();
            Assert.assertTrue(parentTask.current(exactTimeStamp));
            return parentTask;
        }
    }

    @NonNull
    public CustomTime getCustomTime(@NonNull CustomTimeKey customTimeKey) {
        if (customTimeKey.mLocalCustomTimeId != null) {
            Assert.assertTrue(TextUtils.isEmpty(customTimeKey.mRemoteCustomTimeId));

            return mLocalFactory.getLocalCustomTime(customTimeKey.mLocalCustomTimeId);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(customTimeKey.mRemoteCustomTimeId));
            Assert.assertTrue(mRemoteFactory != null);

            return mRemoteFactory.getRemoteCustomTime(customTimeKey.mRemoteCustomTimeId);
        }
    }

    @NonNull
    private List<LocalCustomTime> getCurrentCustomTimes() {
        return mLocalFactory.getCurrentCustomTimes();
    }

    @NonNull
    private HashMap<InstanceKey, GroupListLoader.InstanceData> getChildInstanceDatas(@NonNull Instance instance, @NonNull ExactTimeStamp now, @NonNull GroupListLoader.InstanceDataParent instanceDataParent) {
        HashMap<InstanceKey, GroupListLoader.InstanceData> instanceDatas = new HashMap<>();

        for (Instance childInstance : instance.getChildInstances(now)) {
            Task childTask = getTask(childInstance.getTaskKey());

            Boolean isRootTask = (childTask.current(now) ? childTask.isRootTask(now) : null);

            GroupListLoader.InstanceData instanceData = new GroupListLoader.InstanceData(childInstance.getDone(), childInstance.getInstanceKey(), null, childInstance.getName(), childInstance.getInstanceDateTime().getTimeStamp(), childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), instanceDataParent, childInstance.getInstanceDateTime().getTime().getTimePair(), childTask.getNote(), childTask.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(childInstance, now, instanceData));
            instanceDatas.put(childInstance.getInstanceKey(), instanceData);
        }

        return instanceDatas;
    }

    @NonNull
    private Map<TaskKey, CreateTaskLoader.TaskTreeData> getChildTaskDatas(@NonNull ExactTimeStamp now, @NonNull Task parentTask, @NonNull Context context, @NonNull List<TaskKey> excludedTaskKeys) {
        return Stream.of(parentTask.getChildTasks(now))
                .filterNot(childTask -> excludedTaskKeys.contains(childTask.getTaskKey()))
                .collect(Collectors.toMap(Task::getTaskKey, childTask -> new CreateTaskLoader.TaskTreeData(childTask.getName(), getChildTaskDatas(now, childTask, context, excludedTaskKeys), childTask.getTaskKey(), childTask.getScheduleText(context, now), childTask.getNote(), childTask.getStartExactTimeStamp())));
    }

    @NonNull
    private Map<TaskKey, CreateTaskLoader.TaskTreeData> getTaskDatas(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull List<TaskKey> excludedTaskKeys) {
        return Stream.of(getTasks())
                .filter(task -> task.current(now))
                .filter(task -> task.isVisible(now))
                .filter(task -> task.isRootTask(now))
                .filterNot(task -> excludedTaskKeys.contains(task.getTaskKey()))
                .collect(Collectors.toMap(Task::getTaskKey, task -> new CreateTaskLoader.TaskTreeData(task.getName(), getChildTaskDatas(now, task, context, excludedTaskKeys), task.getTaskKey(), task.getScheduleText(context, now), task.getNote(), task.getStartExactTimeStamp())));
    }

    // notifications

    @SuppressLint("NewApi")
    private void setExact(@NonNull Context context, long time, @NonNull PendingIntent pendingIntent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
    }

    private void notifyInstance(@NonNull Context context, @NonNull Instance instance, boolean silent, @NonNull ExactTimeStamp now) {
        Task task = instance.getTask();
        int notificationId = instance.getNotificationId();
        InstanceKey instanceKey = instance.getInstanceKey();

        Intent deleteIntent = InstanceNotificationDeleteService.getIntent(context, instanceKey);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(context, notificationId, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent contentIntent = ShowInstanceActivity.getNotificationIntent(context, instanceKey);
        PendingIntent pendingContentIntent = PendingIntent.getActivity(context, notificationId, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        ArrayList<NotificationCompat.Action> actions = new ArrayList<>();

        Intent doneIntent = InstanceDoneService.getIntent(context, instanceKey, notificationId);
        PendingIntent pendingDoneIntent = PendingIntent.getService(context, notificationId, doneIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        actions.add(new NotificationCompat.Action.Builder(R.drawable.ic_done_white_24dp, context.getString(R.string.done), pendingDoneIntent).build());

        Intent hourIntent = InstanceHourService.getIntent(context, instanceKey, notificationId);
        PendingIntent pendingHourIntent = PendingIntent.getService(context, notificationId, hourIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        actions.add(new NotificationCompat.Action.Builder(R.drawable.ic_alarm_white_24dp, context.getString(R.string.hour), pendingHourIntent).build());

        List<Instance> childInstances = instance.getChildInstances(now);

        String text;
        NotificationCompat.Style style;
        if (!childInstances.isEmpty()) {
            Stream<Instance> notDone = Stream.of(childInstances)
                    .filter(childInstance -> childInstance.getDone() == null)
                    .sortBy(childInstance -> childInstance.getTask().getStartExactTimeStamp());

            //noinspection ConstantConditions
            Stream<Instance> done = Stream.of(childInstances)
                    .filter(childInstance -> childInstance.getDone() != null)
                    .sortBy(childInstance -> -childInstance.getDone().getLong());

            List<String> children = Stream.concat(notDone, done)
                    .map(Instance::getName)
                    .collect(Collectors.toList());

            text = TextUtils.join(", ", children);
            style = getInboxStyle(context, children);
        } else if (!TextUtils.isEmpty(task.getNote())) {
            text = task.getNote();

            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.bigText(task.getNote());

            style = bigTextStyle;
        } else {
            text = null;
            style = null;
        }

        notify(context, instance.getName(), text, notificationId, pendingDeleteIntent, pendingContentIntent, silent, actions, instance.getInstanceDateTime().getTimeStamp().getLong(), style, true);
    }

    private void notifyGroup(@NonNull Context context, @NonNull Collection<Instance> instances, boolean silent, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(instances.size() > TickService.MAX_NOTIFICATIONS);

        ArrayList<String> names = new ArrayList<>();
        ArrayList<InstanceKey> instanceKeys = new ArrayList<>();
        for (Instance instance : instances) {
            names.add(instance.getName());
            instanceKeys.add(instance.getInstanceKey());
        }

        Intent deleteIntent = GroupNotificationDeleteService.getIntent(context, instanceKeys);
        PendingIntent pendingDeleteIntent = PendingIntent.getService(context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent contentIntent = ShowNotificationGroupActivity.getIntent(context, instanceKeys);
        PendingIntent pendingContentIntent = PendingIntent.getActivity(context, 0, contentIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.InboxStyle inboxStyle = getInboxStyle(context, Stream.of(instances)
                .sorted((lhs, rhs) -> {
                    int timeStampComparison = lhs.getInstanceDateTime().getTimeStamp().compareTo(rhs.getInstanceDateTime().getTimeStamp());
                    if (timeStampComparison != 0)
                        return timeStampComparison;

                    return lhs.getTask().getStartExactTimeStamp().compareTo(rhs.getTask().getStartExactTimeStamp());
                })
                .map(notificationInstanceData -> notificationInstanceData.getName() + " (" + notificationInstanceData.getDisplayText(context, now) + ")")
                .collect(Collectors.toList()));

        notify(context, instances.size() + " " + context.getString(R.string.multiple_reminders), TextUtils.join(", ", names), 0, pendingDeleteIntent, pendingContentIntent, silent, new ArrayList<>(), null, inboxStyle, false);
    }

    @NonNull
    private NotificationCompat.InboxStyle getInboxStyle(@NonNull Context context, @NonNull List<String> lines) {
        Assert.assertTrue(!lines.isEmpty());

        int max = 5;

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        Stream.of(lines)
                .limit(max)
                .forEach(inboxStyle::addLine);

        int extraCount = lines.size() - max;

        if (extraCount > 0)
            inboxStyle.setSummaryText("+" + extraCount + " " + context.getString(R.string.more));

        return inboxStyle;
    }

    private void notify(@NonNull Context context, @NonNull String title, @Nullable String text, int notificationId, @NonNull PendingIntent deleteIntent, @NonNull PendingIntent contentIntent, boolean silent, @NonNull List<NotificationCompat.Action> actions, @Nullable Long when, @Nullable NotificationCompat.Style style, boolean autoCancel) {
        Assert.assertTrue(!TextUtils.isEmpty(title));

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = (new NotificationCompat.Builder(context))
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ikona_bez)
                .setDeleteIntent(deleteIntent)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (!TextUtils.isEmpty(text))
            builder.setContentText(text);

        if (!silent)
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

        Assert.assertTrue(actions.size() <= 3);

        Stream.of(actions)
                .forEach(builder::addAction);

        if (when != null)
            builder.setWhen(when);

        if (style != null)
            builder.setStyle(style);

        if (autoCancel)
            builder.setAutoCancel(true);

        Notification notification = builder.build();

        if (!silent)
            notification.defaults |= Notification.DEFAULT_VIBRATE;

        notificationManager.notify(notificationId, notification);
    }

    @NonNull
    public RemoteTask convertLocalToRemote(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull LocalTask startingLocalTask, @NonNull Set<String> recordOf) {
        Assert.assertTrue(mRemoteFactory != null);
        Assert.assertTrue(mUserData != null);

        LocalToRemoteConversion localToRemoteConversion = new LocalToRemoteConversion();
        mLocalFactory.convertLocalToRemoteHelper(localToRemoteConversion, startingLocalTask, recordOf);

        updateNotifications(context, true, false, new ArrayList<>(), now, Stream.of(localToRemoteConversion.mLocalTasks.values())
                .map(Task::getTaskKey)
                .collect(Collectors.toList()));

        for (LocalTask localTask : localToRemoteConversion.mLocalTasks.values()) {
            Assert.assertTrue(localTask != null);

            RemoteTask remoteTask = mRemoteFactory.copyLocalTask(localTask, recordOf);
            localToRemoteConversion.mRemoteTasks.put(localTask.getId(), remoteTask);
        }

        for (LocalTaskHierarchy localTaskHierarchy : localToRemoteConversion.mLocalTaskHierarchies) {
            Assert.assertTrue(localTaskHierarchy != null);

            RemoteTask parentRemoteTask = localToRemoteConversion.mRemoteTasks.get(localTaskHierarchy.getParentTaskId());
            Assert.assertTrue(parentRemoteTask != null);

            RemoteTask childRemoteTask = localToRemoteConversion.mRemoteTasks.get(localTaskHierarchy.getChildTaskId());
            Assert.assertTrue(childRemoteTask != null);

            RemoteTaskHierarchy remoteTaskHierarchy = mRemoteFactory.copyLocalTaskHierarchy(localTaskHierarchy, recordOf, parentRemoteTask.getId(), childRemoteTask.getId());
            localToRemoteConversion.mRemoteTaskHierarchies.add(remoteTaskHierarchy);
        }

        for (LocalInstance localInstance : localToRemoteConversion.mLocalInstances) {
            Assert.assertTrue(localInstance != null);

            RemoteTask remoteTask = localToRemoteConversion.mRemoteTasks.get(localInstance.getTaskId());
            Assert.assertTrue(remoteTask != null);

            RemoteInstance remoteInstance = mRemoteFactory.copyLocalInstance(localInstance, recordOf, remoteTask.getId());
            localToRemoteConversion.mRemoteInstances.add(remoteInstance);
        }

        Stream.of(localToRemoteConversion.mLocalTasks.values())
                .forEach(LocalTask::delete);

        Stream.of(localToRemoteConversion.mLocalTaskHierarchies)
                .forEach(LocalTaskHierarchy::delete);

        Stream.of(localToRemoteConversion.mLocalInstances)
                .forEach(LocalInstance::delete);

        RemoteTask remoteTask = localToRemoteConversion.mRemoteTasks.get(startingLocalTask.getId());
        Assert.assertTrue(remoteTask != null);

        return remoteTask;
    }

    private void joinTasks(@NonNull Task newParentTask, @NonNull List<Task> joinTasks, @NonNull ExactTimeStamp now) {
        Assert.assertTrue(newParentTask.current(now));
        Assert.assertTrue(joinTasks.size() > 1);

        for (Task joinTask : joinTasks) {
            Assert.assertTrue(joinTask != null);
            Assert.assertTrue(joinTask.current(now));

            if (joinTask.isRootTask(now)) {
                Stream.of(joinTask.getCurrentSchedules(now))
                        .forEach(schedule -> schedule.setEndExactTimeStamp(now));
            } else {
                TaskHierarchy taskHierarchy = getParentTaskHierarchy(joinTask, now);
                Assert.assertTrue(taskHierarchy != null);

                taskHierarchy.setEndExactTimeStamp(now);
            }

            newParentTask.addChild(joinTask, now);
        }
    }

    @NonNull
    private List<TaskHierarchy> getTaskHierarchies() {
        List<TaskHierarchy> taskHierarchies = new ArrayList<>(mLocalFactory.getTaskHierarchies());

        if (mRemoteFactory != null)
            taskHierarchies.addAll(mRemoteFactory.getTaskHierarchies());

        return taskHierarchies;
    }

    @Nullable
    TaskHierarchy getParentTaskHierarchy(@NonNull Task childTask, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(childTask.current(exactTimeStamp));

        TaskKey childTaskKey = childTask.getTaskKey();

        List<TaskHierarchy> taskHierarchies = Stream.of(getTaskHierarchies())
                .filter(taskHierarchy -> taskHierarchy.current(exactTimeStamp))
                .filter(taskHierarchy -> taskHierarchy.getChildTaskKey().equals(childTaskKey))
                .collect(Collectors.toList());

        if (taskHierarchies.isEmpty()) {
            return null;
        } else {
            Assert.assertTrue(taskHierarchies.size() == 1);
            return taskHierarchies.get(0);
        }
    }

    @NonNull
    private List<Task> getTasks() {
        List<Task> tasks = new ArrayList<>(mLocalFactory.getTasks());

        if (mRemoteFactory != null)
            tasks.addAll(mRemoteFactory.getTasks().values());

        return tasks;
    }

    @NonNull
    private Map<TaskKey, Task> getTaskMap() {
        return Stream.of(getTasks())
                .collect(Collectors.toMap(Task::getTaskKey, task -> task));
    }

    @NonNull
    private List<CustomTime> getCustomTimes() {
        List<CustomTime> customTimes = new ArrayList<>(mLocalFactory.getLocalCustomTimes());

        if (mRemoteFactory != null)
            customTimes.addAll(mRemoteFactory.getRemoteCustomTimes());

        return customTimes;
    }

    @NonNull
    Task getTask(@NonNull TaskKey taskKey) {
        Map<TaskKey, Task> tasks = getTaskMap();
        Assert.assertTrue(tasks.containsKey(taskKey));

        Task task = tasks.get(taskKey);
        Assert.assertTrue(task != null);

        return task;
    }

    @NonNull
    List<Task> getChildTasks(@NonNull Task parentTask, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(parentTask.current(exactTimeStamp));

        return Stream.of(getChildTaskHierarchies(parentTask))
                .filter(taskHierarchy -> taskHierarchy.current(exactTimeStamp))
                .map(TaskHierarchy::getChildTask)
                .filter(childTask -> childTask.current(exactTimeStamp))
                .sortBy(Task::getStartExactTimeStamp)
                .collect(Collectors.toList());
    }

    @NonNull
    List<TaskHierarchy> getChildTaskHierarchies(@NonNull Task parentTask) {
        TaskKey parentTaskKey = parentTask.getTaskKey();

        return Stream.of(getTaskHierarchies())
                .filter(taskHierarchy -> taskHierarchy.getParentTaskKey().equals(parentTaskKey))
                .collect(Collectors.toList());
    }

    @NonNull
    private List<TaskListLoader.ChildTaskData> getChildTaskDatas(@NonNull Task parentTask, @NonNull ExactTimeStamp now, @NonNull Context context) {
        return Stream.of(parentTask.getChildTasks(now))
                .sortBy(Task::getStartExactTimeStamp)
                .map(childTask -> new TaskListLoader.ChildTaskData(childTask.getName(), childTask.getScheduleText(context, now), getChildTaskDatas(childTask, now, context), childTask.getNote(), childTask.getStartExactTimeStamp(), childTask.getTaskKey()))
                .collect(Collectors.toList());
    }

    @Nullable
    public RemoteFactory getRemoteFactory() {
        return mRemoteFactory;
    }

    @NonNull
    public LocalFactory getLocalFactory() {
        return mLocalFactory;
    }

    @NonNull
    private List<Instance> getExistingInstances() {
        List<Instance> instances = new ArrayList<>(mLocalFactory.getExistingInstances());

        if (mRemoteFactory != null)
            instances.addAll(mRemoteFactory.getExistingInstances());

        return instances;
    }

    @Nullable
    public Map<String, UserData> getFriends() {
        return mFriends;
    }

    @NonNull
    private List<GroupListLoader.TaskData> getChildTaskDatas(@NonNull Task parentTask, @NonNull ExactTimeStamp now) {
        return Stream.of(parentTask.getChildTasks(now))
                .map(childTask -> new GroupListLoader.TaskData(childTask.getTaskKey(), childTask.getName(), getChildTaskDatas(childTask, now), childTask.getStartExactTimeStamp()))
                .collect(Collectors.toList());
    }

    @NonNull
    TaskListLoader.Data getTaskListData(@NonNull ExactTimeStamp now, @NonNull Context context, @Nullable TaskKey taskKey) {
        List<TaskListLoader.ChildTaskData> childTaskDatas;
        String note;

        if (taskKey != null) {
            Task parentTask = getTask(taskKey);

            List<Task> tasks = parentTask.getChildTasks(now);
            childTaskDatas = Stream.of(tasks)
                    .map(task -> new TaskListLoader.ChildTaskData(task.getName(), task.getScheduleText(context, now), getChildTaskDatas(task, now, context), task.getNote(), task.getStartExactTimeStamp(), task.getTaskKey()))
                    .collect(Collectors.toList());

            note = parentTask.getNote();
        } else {
            childTaskDatas = Stream.of(getTasks())
                    .filter(task -> task.current(now))
                    .filter(task -> task.isVisible(now))
                    .filter(task -> task.isRootTask(now))
                    .map(task -> new TaskListLoader.ChildTaskData(task.getName(), task.getScheduleText(context, now), getChildTaskDatas(task, now, context), task.getNote(), task.getStartExactTimeStamp(), task.getTaskKey()))
                    .collect(Collectors.toList());

            note = null;
        }

        Collections.sort(childTaskDatas, (TaskListLoader.ChildTaskData lhs, TaskListLoader.ChildTaskData rhs) -> lhs.mStartExactTimeStamp.compareTo(rhs.mStartExactTimeStamp));
        if (taskKey == null)
            Collections.reverse(childTaskDatas);

        return new TaskListLoader.Data(childTaskDatas, note);
    }

    @NonNull
    Instance setInstanceDone(@NonNull ExactTimeStamp now, @NonNull InstanceKey instanceKey, boolean done) {
        Instance instance = getInstance(instanceKey);

        instance.setDone(done, now);

        return instance;
    }

    @NonNull
    Irrelevant setIrrelevant(@NonNull ExactTimeStamp now) {
        for (Task task : getTasks())
            task.updateOldestVisible(now);

        // relevant hack
        Map<TaskKey, TaskRelevance> taskRelevances = Stream.of(getTasks()).collect(Collectors.toMap(Task::getTaskKey, TaskRelevance::new));
        Map<InstanceKey, InstanceRelevance> instanceRelevances = Stream.of(getExistingInstances()).collect(Collectors.toMap(Instance::getInstanceKey, InstanceRelevance::new));
        Map<Integer, CustomTimeRelevance> customTimeRelevances = Stream.of(mLocalFactory.getLocalCustomTimes()).collect(Collectors.toMap(LocalCustomTime::getId, CustomTimeRelevance::new));

        Stream.of(getTasks())
                .filter(task -> task.current(now))
                .filter(task -> task.isRootTask(now))
                .filter(task -> task.isVisible(now))
                .map(Task::getTaskKey)
                .map(taskRelevances::get)
                .forEach(taskRelevance -> taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

        Stream.of(getRootInstances(null, now.plusOne(), now))
                .map(Instance::getInstanceKey)
                .map(instanceRelevances::get)
                .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

        Stream.of(getExistingInstances())
                .filter(instance -> instance.isRootInstance(now))
                .filter(instance -> instance.isVisible(now))
                .map(Instance::getInstanceKey)
                .map(instanceRelevances::get)
                .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

        Stream.of(getCurrentCustomTimes())
                .map(LocalCustomTime::getId)
                .map(customTimeRelevances::get)
                .forEach(CustomTimeRelevance::setRelevant);

        List<Task> relevantTasks = Stream.of(taskRelevances.values())
                .filter(TaskRelevance::getRelevant)
                .map(TaskRelevance::getTask)
                .collect(Collectors.toList());

        List<Task> irrelevantTasks = getTasks();
        irrelevantTasks.removeAll(relevantTasks);

        Assert.assertTrue(Stream.of(irrelevantTasks)
                .noneMatch(task -> task.isVisible(now)));

        List<Instance> relevantExistingInstances = Stream.of(instanceRelevances.values())
                .filter(InstanceRelevance::getRelevant)
                .map(InstanceRelevance::getInstance)
                .filter(Instance::exists)
                .collect(Collectors.toList());

        List<Instance> irrelevantExistingInstances = getExistingInstances();
        irrelevantExistingInstances.removeAll(relevantExistingInstances);

        Assert.assertTrue(Stream.of(irrelevantExistingInstances)
                .noneMatch(instance -> instance.isVisible(now)));

        List<LocalCustomTime> relevantCustomTimes = Stream.of(customTimeRelevances.values())
                .filter(CustomTimeRelevance::getRelevant)
                .map(CustomTimeRelevance::getCustomTime)
                .collect(Collectors.toList());

        List<LocalCustomTime> irrelevantCustomTimes = new ArrayList<>(mLocalFactory.getLocalCustomTimes());
        irrelevantCustomTimes.removeAll(relevantCustomTimes);

        Assert.assertTrue(Stream.of(irrelevantCustomTimes)
                .noneMatch(LocalCustomTime::getCurrent));

        Stream.of(irrelevantTasks)
                .forEach(Task::setRelevant);

        Stream.of(irrelevantExistingInstances)
                .forEach(Instance::setRelevant);

        Stream.of(irrelevantCustomTimes)
                .forEach(LocalCustomTime::setRelevant);

        return new Irrelevant(irrelevantCustomTimes, irrelevantTasks, irrelevantExistingInstances);
    }

    void removeIrrelevant(@NonNull Irrelevant irrelevant) {
        if (mRemoteFactory != null) {
            mRemoteFactory.removeIrrelevant(irrelevant);

            mLocalFactory.deleteInstanceShownRecords(mRemoteFactory.getTasks().keySet());
        } else {
            Assert.assertTrue(Stream.of(irrelevant.mTasks).noneMatch(task -> task instanceof RemoteTask));
            Assert.assertTrue(Stream.of(irrelevant.mInstances).noneMatch(instance -> instance instanceof RemoteInstance));
        }

        mLocalFactory.removeIrrelevant(irrelevant);
    }

    private void updateNotifications(@NonNull Context context, @NonNull List<TaskKey> taskKeys, @NonNull ExactTimeStamp now) {
        updateNotifications(context, true, false, taskKeys, now, new ArrayList<>());
    }

    private void updateNotifications(@NonNull Context context, boolean silent, boolean registering, @NonNull List<TaskKey> taskKeys, @NonNull ExactTimeStamp now, @NonNull List<TaskKey> removedTaskKeys) {
        if (!silent) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(TickService.TICK_PREFERENCES, Context.MODE_PRIVATE);
            sharedPreferences.edit().putLong(TickService.LAST_TICK_KEY, ExactTimeStamp.getNow().getLong()).apply();
        }

        List<Instance> rootInstances = getRootInstances(null, now.plusOne(), now); // 24 hack

        Map<InstanceKey, Instance> notificationInstances = Stream.of(rootInstances)
                .filter(instance -> (instance.getDone() == null) && !instance.getNotified() && instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) <= 0)
                .filterNot(instance -> removedTaskKeys.contains(instance.getTaskKey()))
                .collect(Collectors.toMap(Instance::getInstanceKey, instance -> instance));

        HashSet<InstanceKey> shownInstanceKeys = new HashSet<>(Stream.of(getExistingInstances())
                .filter(Instance::getNotificationShown)
                .map(Instance::getInstanceKey)
                .collect(Collectors.toSet()));

        Map<InstanceKey, Pair<Integer, InstanceShownRecord>> instanceShownRecordNotificationDatas = new HashMap<>();
        for (InstanceShownRecord instanceShownRecord : mLocalFactory.getInstanceShownRecords()) {
            if (!instanceShownRecord.getNotificationShown())
                continue;

            Date scheduleDate = new Date(instanceShownRecord.getScheduleYear(), instanceShownRecord.getScheduleMonth(), instanceShownRecord.getScheduleDay());
            String remoteCustomTimeId = instanceShownRecord.getScheduleCustomTimeId();

            CustomTimeKey customTimeKey;
            HourMinute hourMinute;

            if (!TextUtils.isEmpty(remoteCustomTimeId)) {
                Assert.assertTrue(instanceShownRecord.getScheduleHour() == null);
                Assert.assertTrue(instanceShownRecord.getScheduleMinute() == null);

                customTimeKey = getCustomTimeKey(remoteCustomTimeId);
                hourMinute = null;
            } else {
                Assert.assertTrue(instanceShownRecord.getScheduleHour() != null);
                Assert.assertTrue(instanceShownRecord.getScheduleMinute() != null);

                customTimeKey = null;
                hourMinute = new HourMinute(instanceShownRecord.getScheduleHour(), instanceShownRecord.getScheduleMinute());
            }

            TaskKey taskKey = new TaskKey(instanceShownRecord.getTaskId());

            InstanceKey instanceKey = new InstanceKey(taskKey, scheduleDate, new TimePair(customTimeKey, hourMinute));

            shownInstanceKeys.add(instanceKey);

            instanceShownRecordNotificationDatas.put(instanceKey, new Pair<>(Instance.getNotificationId(scheduleDate, customTimeKey, hourMinute, taskKey), instanceShownRecord));
        }

        List<InstanceKey> showInstanceKeys = Stream.of(notificationInstances.keySet())
                .filter(instanceKey -> !shownInstanceKeys.contains(instanceKey))
                .collect(Collectors.toList());

        Set<InstanceKey> hideInstanceKeys = Stream.of(shownInstanceKeys)
                .filter(instanceKey -> !notificationInstances.containsKey(instanceKey))
                .collect(Collectors.toSet());

        for (InstanceKey showInstanceKey : showInstanceKeys) {
            Assert.assertTrue(showInstanceKey != null);

            Instance showInstance = getInstance(showInstanceKey);

            showInstance.setNotificationShown(true, now);
        }

        Set<TaskKey> allTaskKeys = getTaskMap().keySet();

        for (InstanceKey hideInstanceKey : hideInstanceKeys) {
            Assert.assertTrue(hideInstanceKey != null);

            if (allTaskKeys.contains(hideInstanceKey.mTaskKey)) {
                Instance hideInstance = getInstance(hideInstanceKey);

                hideInstance.setNotificationShown(false, now);
            } else {
                Assert.assertTrue(instanceShownRecordNotificationDatas.containsKey(hideInstanceKey));

                instanceShownRecordNotificationDatas.get(hideInstanceKey).second.setNotificationShown(false);
            }
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Assert.assertTrue(notificationManager != null);

        if (registering) {
            Assert.assertTrue(silent);

            if (notificationInstances.size() > TickService.MAX_NOTIFICATIONS) { // show group
                notifyGroup(context, notificationInstances.values(), true, now);
            } else { // show instances
                for (Instance instance : notificationInstances.values()) {
                    Assert.assertTrue(instance != null);

                    notifyInstance(context, instance, true, now);
                }
            }
        } else {
            if (notificationInstances.size() > TickService.MAX_NOTIFICATIONS) { // show group
                if (shownInstanceKeys.size() > TickService.MAX_NOTIFICATIONS) { // group shown
                    if (!showInstanceKeys.isEmpty() || !hideInstanceKeys.isEmpty()) {
                        notifyGroup(context, notificationInstances.values(), silent, now);
                    } else if (Stream.of(notificationInstances.values()).anyMatch(instance -> updateInstance(taskKeys, instance, now))) {
                        notifyGroup(context, notificationInstances.values(), true, now);
                    }
                } else { // instances shown
                    for (InstanceKey shownInstanceKey : shownInstanceKeys) {
                        if (allTaskKeys.contains(shownInstanceKey.mTaskKey)) {
                            Instance shownInstance = getInstance(shownInstanceKey);

                            notificationManager.cancel(shownInstance.getNotificationId());
                        } else {
                            Assert.assertTrue(instanceShownRecordNotificationDatas.containsKey(shownInstanceKey));

                            int notificationId = instanceShownRecordNotificationDatas.get(shownInstanceKey).first;

                            notificationManager.cancel(notificationId);
                        }
                    }

                    notifyGroup(context, notificationInstances.values(), silent, now);
                }
            } else { // show instances
                if (shownInstanceKeys.size() > TickService.MAX_NOTIFICATIONS) { // group shown
                    notificationManager.cancel(0);

                    for (Instance instance : notificationInstances.values()) {
                        Assert.assertTrue(instance != null);

                        notifyInstance(context, instance, silent, now);
                    }
                } else { // instances shown
                    for (InstanceKey hideInstanceKey : hideInstanceKeys) {
                        if (allTaskKeys.contains(hideInstanceKey.mTaskKey)) {
                            Instance instance = getInstance(hideInstanceKey);

                            notificationManager.cancel(instance.getNotificationId());
                        } else {
                            Assert.assertTrue(instanceShownRecordNotificationDatas.containsKey(hideInstanceKey));

                            int notificationId = instanceShownRecordNotificationDatas.get(hideInstanceKey).first;

                            notificationManager.cancel(notificationId);
                        }
                    }

                    for (InstanceKey showInstanceKey : showInstanceKeys) {
                        Instance instance = notificationInstances.get(showInstanceKey);
                        Assert.assertTrue(instance != null);

                        notifyInstance(context, instance, silent, now);
                    }

                    Stream.of(notificationInstances.values())
                            .filter(instance -> updateInstance(taskKeys, instance, now))
                            .filter(instance -> !showInstanceKeys.contains(instance.getInstanceKey()))
                            .forEach(instance -> notifyInstance(context, instance, true, now));
                }
            }
        }

        TimeStamp nextAlarm = null;
        for (Instance existingInstance : getExistingInstances()) {
            TimeStamp instanceTimeStamp = existingInstance.getInstanceDateTime().getTimeStamp();
            if (instanceTimeStamp.toExactTimeStamp().compareTo(now) > 0)
                if (nextAlarm == null || instanceTimeStamp.compareTo(nextAlarm) < 0)
                    nextAlarm = instanceTimeStamp;
        }

        for (Task task : getTasks()) {
            if (task.current(now) && task.isRootTask(now)) {
                List<Schedule> schedules = task.getCurrentSchedules(now);

                Optional<TimeStamp> optional = Stream.of(schedules)
                        .map(schedule -> schedule.getNextAlarm(now))
                        .filter(timeStamp -> timeStamp != null)
                        .min(TimeStamp::compareTo);

                if (optional.isPresent()) {
                    TimeStamp scheduleTimeStamp = optional.get();
                    Assert.assertTrue(scheduleTimeStamp != null);
                    Assert.assertTrue(scheduleTimeStamp.toExactTimeStamp().compareTo(now) > 0);

                    if (nextAlarm == null || scheduleTimeStamp.compareTo(nextAlarm) < 0)
                        nextAlarm = scheduleTimeStamp;
                }
            }
        }

        if (nextAlarm != null) {
            Intent nextIntent = TickService.getIntent(context, false, false, new ArrayList<>());

            PendingIntent pendingIntent = PendingIntent.getService(context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Assert.assertTrue(pendingIntent != null);

            setExact(context, nextAlarm.getLong(), pendingIntent);
        }
    }

    private boolean updateInstance(@NonNull List<TaskKey> taskKeys, @NonNull Instance instance, @NonNull ExactTimeStamp now) {
        return (taskKeys.contains(instance.getTaskKey()) || Stream.of(instance.getChildInstances(now)).anyMatch(childInstance -> taskKeys.contains(childInstance.getTaskKey())));
    }

    public static class Irrelevant {
        @NonNull
        public final List<LocalCustomTime> mCustomTimes;

        @NonNull
        public final List<Task> mTasks;

        @NonNull
        public final List<Instance> mInstances;

        Irrelevant(@NonNull List<LocalCustomTime> customTimes, @NonNull List<Task> tasks, @NonNull List<Instance> instances) {
            mCustomTimes = customTimes;
            mTasks = tasks;
            mInstances = instances;
        }
    }

    private class TaskRelevance {
        @NonNull
        private final Task mTask;
        private boolean mRelevant = false;

        TaskRelevance(@NonNull Task task) {
            mTask = task;
        }

        void setRelevant(@NonNull Map<TaskKey, TaskRelevance> taskRelevances, @NonNull Map<InstanceKey, InstanceRelevance> instanceRelevances, @NonNull Map<Integer, CustomTimeRelevance> customTimeRelevances, @NonNull ExactTimeStamp now) {
            if (mRelevant)
                return;

            mRelevant = true;

            TaskKey taskKey = mTask.getTaskKey();

            // mark parents relevant
            Stream.of(getTaskHierarchies())
                    .filter(taskHierarchy -> taskHierarchy.getChildTaskKey().equals(taskKey))
                    .map(TaskHierarchy::getParentTaskKey)
                    .map(taskRelevances::get)
                    .forEach(taskRelevance -> taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            // mark children relevant
            Stream.of(getTaskHierarchies())
                    .filter(taskHierarchy -> taskHierarchy.getParentTaskKey().equals(taskKey))
                    .map(TaskHierarchy::getChildTaskKey)
                    .map(taskRelevances::get)
                    .forEach(taskRelevance -> taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            Date oldestVisible = mTask.getOldestVisible();
            Assert.assertTrue(oldestVisible != null);

            // mark instances relevant
            Stream.of(getPastInstances(mTask, now))
                    .filter(instance -> instance.getScheduleDateTime().getDate().compareTo(oldestVisible) >= 0)
                    .map(instance -> {
                        InstanceKey instanceKey = instance.getInstanceKey();

                        if (!instanceRelevances.containsKey(instanceKey))
                            instanceRelevances.put(instanceKey, new InstanceRelevance(instance));

                        return instanceRelevances.get(instanceKey);
                    })
                    .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            Stream.of(getExistingInstances(mTask))
                    .filter(instance -> instance.getScheduleDateTime().getDate().compareTo(oldestVisible) >= 0)
                    .map(Instance::getInstanceKey)
                    .map(instanceRelevances::get)
                    .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            // mark custom times relevant
            if (mTask.current(now))
                Stream.of(mTask.getCurrentSchedules(now))
                        .map(Schedule::getCustomTimeKey)
                        .filter(customTimeKey -> customTimeKey != null && customTimeKey.mLocalCustomTimeId != null)
                        .map(customTimeKey -> customTimeRelevances.get(customTimeKey.mLocalCustomTimeId))
                        .forEach(CustomTimeRelevance::setRelevant);
        }

        boolean getRelevant() {
            return mRelevant;
        }

        public Task getTask() {
            return mTask;
        }
    }

    private static class InstanceRelevance {
        private final Instance mInstance;
        private boolean mRelevant = false;

        InstanceRelevance(@NonNull Instance instance) {
            mInstance = instance;
        }

        void setRelevant(@NonNull Map<TaskKey, TaskRelevance> taskRelevances, @NonNull Map<InstanceKey, InstanceRelevance> instanceRelevances, @NonNull Map<Integer, CustomTimeRelevance> customTimeRelevances, @NonNull ExactTimeStamp now) {
            if (mRelevant)
                return;

            mRelevant = true;

            // set task relevant
            TaskRelevance taskRelevance = taskRelevances.get(mInstance.getTaskKey());
            Assert.assertTrue(taskRelevance != null);

            taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now);

            // set parent instance relevant
            if (!mInstance.isRootInstance(now)) {
                Instance parentInstance = mInstance.getParentInstance(now);
                Assert.assertTrue(parentInstance != null);

                InstanceKey parentInstanceKey = parentInstance.getInstanceKey();

                if (!instanceRelevances.containsKey(parentInstanceKey))
                    instanceRelevances.put(parentInstanceKey, new InstanceRelevance(parentInstance));

                InstanceRelevance parentInstanceRelevance = instanceRelevances.get(parentInstanceKey);
                Assert.assertTrue(parentInstanceRelevance != null);

                parentInstanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now);
            }

            // set child instances relevant
            Stream.of(mInstance.getChildInstances(now))
                    .map(instance -> {
                        InstanceKey instanceKey = instance.getInstanceKey();

                        if (!instanceRelevances.containsKey(instanceKey))
                            instanceRelevances.put(instanceKey, new InstanceRelevance(instance));

                        return instanceRelevances.get(instanceKey);
                    })
                    .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            // set custom time relevant
            CustomTimeKey scheduleCustomTimeKey = mInstance.getScheduleCustomTimeKey();
            if (scheduleCustomTimeKey != null && scheduleCustomTimeKey.mLocalCustomTimeId != null) {
                CustomTimeRelevance customTimeRelevance = customTimeRelevances.get(scheduleCustomTimeKey.mLocalCustomTimeId);
                Assert.assertTrue(customTimeRelevance != null);

                customTimeRelevance.setRelevant();
            }

            // set custom time relevant
            CustomTimeKey instanceCustomTimeId = mInstance.getInstanceCustomTimeKey();
            if (instanceCustomTimeId != null && instanceCustomTimeId.mLocalCustomTimeId != null) {
                CustomTimeRelevance customTimeRelevance = customTimeRelevances.get(instanceCustomTimeId.mLocalCustomTimeId);
                Assert.assertTrue(customTimeRelevance != null);

                customTimeRelevance.setRelevant();
            }
        }

        boolean getRelevant() {
            return mRelevant;
        }

        public Instance getInstance() {
            return mInstance;
        }
    }

    @NonNull
    public CustomTimeKey getCustomTimeKey(@NonNull String remoteCustomTimeId) {
        LocalCustomTime localCustomTime = mLocalFactory.getLocalCustomTime(remoteCustomTimeId);

        if (localCustomTime == null) {
            return new CustomTimeKey(remoteCustomTimeId);
        } else {
            return localCustomTime.getCustomTimeKey();
        }
    }

    private static class CustomTimeRelevance {
        @NonNull
        private final LocalCustomTime mCustomTime;

        private boolean mRelevant = false;

        CustomTimeRelevance(@NonNull LocalCustomTime localCustomTime) {
            mCustomTime = localCustomTime;
        }

        void setRelevant() {
            mRelevant = true;
        }

        boolean getRelevant() {
            return mRelevant;
        }

        @NonNull
        LocalCustomTime getCustomTime() {
            return mCustomTime;
        }
    }

    public static class LocalToRemoteConversion {
        public final Map<Integer, LocalTask> mLocalTasks = new HashMap<>();
        public final List<LocalTaskHierarchy> mLocalTaskHierarchies = new ArrayList<>();
        public final List<LocalInstance> mLocalInstances = new ArrayList<>();

        final Map<Integer, RemoteTask> mRemoteTasks = new HashMap<>();
        final List<RemoteTaskHierarchy> mRemoteTaskHierarchies = new ArrayList<>();
        final List<RemoteInstance> mRemoteInstances = new ArrayList<>();
    }
}
