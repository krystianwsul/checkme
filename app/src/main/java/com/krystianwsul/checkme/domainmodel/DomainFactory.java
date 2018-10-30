package com.krystianwsul.checkme.domainmodel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.krystianwsul.checkme.MyApplication;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime;
import com.krystianwsul.checkme.domainmodel.local.LocalInstance;
import com.krystianwsul.checkme.domainmodel.local.LocalTask;
import com.krystianwsul.checkme.domainmodel.local.LocalTaskHierarchy;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.RemoteFriendFactory;
import com.krystianwsul.checkme.firebase.RemoteProject;
import com.krystianwsul.checkme.firebase.RemoteProjectFactory;
import com.krystianwsul.checkme.firebase.RemoteProjectUser;
import com.krystianwsul.checkme.firebase.RemoteRootUser;
import com.krystianwsul.checkme.firebase.RemoteTask;
import com.krystianwsul.checkme.firebase.RemoteTaskHierarchy;
import com.krystianwsul.checkme.firebase.json.UserWrapper;
import com.krystianwsul.checkme.firebase.records.RemoteRootUserRecord;
import com.krystianwsul.checkme.gui.HierarchyData;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment;
import com.krystianwsul.checkme.gui.tasks.TaskListFragment;
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord;
import com.krystianwsul.checkme.persistencemodel.SaveService;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.ScheduleKey;
import com.krystianwsul.checkme.utils.TaskHierarchyKey;
import com.krystianwsul.checkme.utils.TaskKey;
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
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel;
import com.krystianwsul.checkme.viewmodels.DayViewModel;
import com.krystianwsul.checkme.viewmodels.EditInstanceViewModel;
import com.krystianwsul.checkme.viewmodels.EditInstancesViewModel;
import com.krystianwsul.checkme.viewmodels.FriendListViewModel;
import com.krystianwsul.checkme.viewmodels.MainViewModel;
import com.krystianwsul.checkme.viewmodels.ProjectListViewModel;
import com.krystianwsul.checkme.viewmodels.ShowCustomTimeViewModel;
import com.krystianwsul.checkme.viewmodels.ShowCustomTimesViewModel;
import com.krystianwsul.checkme.viewmodels.ShowGroupViewModel;
import com.krystianwsul.checkme.viewmodels.ShowInstanceViewModel;
import com.krystianwsul.checkme.viewmodels.ShowNotificationGroupViewModel;
import com.krystianwsul.checkme.viewmodels.ShowProjectViewModel;
import com.krystianwsul.checkme.viewmodels.ShowTaskInstancesViewModel;
import com.krystianwsul.checkme.viewmodels.ShowTaskViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

@SuppressLint("UseSparseArrays")
public class DomainFactory {

    private final KotlinDomainFactory kotlinDomainFactory;

    private static void check(boolean value) {
        if (!value) throw new IllegalStateException();
    }

    DomainFactory(@NonNull KotlinDomainFactory kotlinDomainFactory) {
        this.kotlinDomainFactory = kotlinDomainFactory;
    }

    // misc

    public boolean isHoldingWakeLock() {
        return kotlinDomainFactory.getTickData() != null && kotlinDomainFactory.getTickData().getWakelock().isHeld();
    }

    public synchronized void reset(@NonNull Context context, @NonNull SaveService.Source source) {
        UserInfo userInfo = kotlinDomainFactory.getUserInfo();
        clearUserInfo(context);

        KotlinDomainFactory.Companion.set_kotlinDomainFactory(null);
        kotlinDomainFactory.localFactory.reset();

        if (userInfo != null)
            setUserInfo(context, source, userInfo);

        ObserverHolder.INSTANCE.notifyDomainObservers(new ArrayList<>());

        ObserverHolder.INSTANCE.clear();
    }

    public int getTaskCount() {
        int count = kotlinDomainFactory.localFactory.getTaskCount();
        if (kotlinDomainFactory.getRemoteProjectFactory() != null)
            //noinspection ConstantConditions
            count += kotlinDomainFactory.getRemoteProjectFactory().getTaskCount();
        return count;
    }

    public int getInstanceCount() {
        int count = kotlinDomainFactory.localFactory.getInstanceCount();
        if (kotlinDomainFactory.getRemoteProjectFactory() != null)
            count += kotlinDomainFactory.getRemoteProjectFactory().getInstanceCount();
        return count;
    }

    public int getCustomTimeCount() {
        return kotlinDomainFactory.getCustomTimes().size();
    }

    void save(@NonNull Context context, int dataId, @NonNull SaveService.Source source) {
        ArrayList<Integer> dataIds = new ArrayList<>();
        dataIds.add(dataId);
        save(context, dataIds, source);
    }

    private void save(@NonNull Context context, @NonNull List<Integer> dataIds, @NonNull SaveService.Source source) {
        if (kotlinDomainFactory.getSkipSave())
            return;

        kotlinDomainFactory.localFactory.save(context, source);

        if (kotlinDomainFactory.getRemoteProjectFactory() != null)
            kotlinDomainFactory.getRemoteProjectFactory().save();

        ObserverHolder.INSTANCE.notifyDomainObservers(dataIds);
    }

    // firebase

    public synchronized void setUserInfo(@NonNull Context context, @NonNull SaveService.Source source, @NonNull UserInfo userInfo) {
        if (kotlinDomainFactory.getUserInfo() != null) {
            check(kotlinDomainFactory.getRecordQuery() != null);
            check(kotlinDomainFactory.getUserQuery() != null);

            if (kotlinDomainFactory.getUserInfo().equals(userInfo))
                return;

            clearUserInfo(context);
        }

        check(kotlinDomainFactory.getUserInfo() == null);

        check(kotlinDomainFactory.getRecordQuery() == null);
        check(kotlinDomainFactory.getRecordListener() == null);

        check(kotlinDomainFactory.getUserQuery() == null);
        check(kotlinDomainFactory.getUserListener() == null);

        kotlinDomainFactory.setUserInfo(userInfo);

        Context applicationContext = context.getApplicationContext();
        check(applicationContext != null);

        DatabaseWrapper.INSTANCE.setUserInfo(userInfo, kotlinDomainFactory.localFactory.getUuid());

        kotlinDomainFactory.setRecordQuery(DatabaseWrapper.INSTANCE.getTaskRecordsQuery(userInfo));
        kotlinDomainFactory.setRecordListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("asdf", "DomainFactory.kotlinDomainFactory.getMRecordListener().onDataChange, dataSnapshot: " + dataSnapshot);
                check(dataSnapshot != null);

                setRemoteTaskRecords(applicationContext, dataSnapshot, source);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                check(databaseError != null);
                Log.e("asdf", "DomainFactory.kotlinDomainFactory.getMRecordListener().onCancelled", databaseError.toException());

                MyCrashlytics.INSTANCE.logException(databaseError.toException());

                if (kotlinDomainFactory.getTickData() != null) {
                    kotlinDomainFactory.getTickData().release();
                    kotlinDomainFactory.setTickData(null);
                }

                kotlinDomainFactory.getNotTickFirebaseListeners().clear();
                RemoteFriendFactory.Companion.clearFriendListeners();
            }
        });
        kotlinDomainFactory.getRecordQuery().addValueEventListener(kotlinDomainFactory.getRecordListener());

        RemoteFriendFactory.Companion.setListener(kotlinDomainFactory.getUserInfo());

        kotlinDomainFactory.setUserQuery(DatabaseWrapper.INSTANCE.getUserQuery(userInfo));
        kotlinDomainFactory.setUserListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("asdf", "DomainFactory.kotlinDomainFactory.getMUserListener().onDataChange, dataSnapshot: " + dataSnapshot);
                check(dataSnapshot != null);

                setUserRecord(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                check(databaseError != null);
                Log.e("asdf", "DomainFactory.kotlinDomainFactory.getMUserListener().onCancelled", databaseError.toException());

                MyCrashlytics.INSTANCE.logException(databaseError.toException());
            }
        });
        kotlinDomainFactory.getUserQuery().addValueEventListener(kotlinDomainFactory.getUserListener());
    }

    public synchronized void clearUserInfo(@NonNull Context context) {
        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        if (kotlinDomainFactory.getUserInfo() == null) {
            check(kotlinDomainFactory.getRecordQuery() == null);
            check(kotlinDomainFactory.getRecordListener() == null);
            check(kotlinDomainFactory.getUserQuery() == null);
            check(kotlinDomainFactory.getUserListener() == null);
        } else {
            check(kotlinDomainFactory.getRecordQuery() != null);
            check(kotlinDomainFactory.getRecordListener() != null);
            check(kotlinDomainFactory.getUserQuery() != null);
            check(kotlinDomainFactory.getUserListener() != null);

            kotlinDomainFactory.localFactory.clearRemoteCustomTimeRecords();
            Log.e("asdf", "clearing kotlinDomainFactory.getMRemoteProjectFactory()", new Exception());

            kotlinDomainFactory.setRemoteProjectFactory(null);
            RemoteFriendFactory.Companion.setInstance(null);

            kotlinDomainFactory.setUserInfo(null);

            kotlinDomainFactory.getRecordQuery().removeEventListener(kotlinDomainFactory.getRecordListener());
            kotlinDomainFactory.setRecordQuery(null);
            kotlinDomainFactory.setRecordListener(null);

            RemoteFriendFactory.Companion.clearListener();

            kotlinDomainFactory.getUserQuery().removeEventListener(kotlinDomainFactory.getUserListener());
            kotlinDomainFactory.setUserQuery(null);
            kotlinDomainFactory.setUserListener(null);

            kotlinDomainFactory.updateNotifications(context, now);

            ObserverHolder.INSTANCE.notifyDomainObservers(new ArrayList<>());
        }
    }

    private synchronized void setRemoteTaskRecords(@NonNull Context context, @NonNull DataSnapshot dataSnapshot, @NonNull SaveService.Source source) {
        check(kotlinDomainFactory.getUserInfo() != null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        kotlinDomainFactory.localFactory.clearRemoteCustomTimeRecords();

        boolean firstThereforeSilent = (kotlinDomainFactory.getRemoteProjectFactory() == null);
        kotlinDomainFactory.setRemoteProjectFactory(new RemoteProjectFactory(kotlinDomainFactory, dataSnapshot.getChildren(), kotlinDomainFactory.getUserInfo(), kotlinDomainFactory.localFactory.getUuid(), now));

        RemoteFriendFactory.Companion.tryNotifyFriendListeners(); // assuming they're all getters

        if (kotlinDomainFactory.getTickData() == null && kotlinDomainFactory.getNotTickFirebaseListeners().isEmpty()) {
            kotlinDomainFactory.updateNotifications(context, firstThereforeSilent, ExactTimeStamp.Companion.getNow(), new ArrayList<>());

            save(context, 0, source);
        } else {
            kotlinDomainFactory.setSkipSave(true);

            if (kotlinDomainFactory.getTickData() == null) {
                kotlinDomainFactory.updateNotifications(context, firstThereforeSilent, ExactTimeStamp.Companion.getNow(), new ArrayList<>());
            } else {
                updateNotificationsTick(context, source, kotlinDomainFactory.getTickData().getSilent(), kotlinDomainFactory.getTickData().getSource());

                if (!firstThereforeSilent) {
                    Log.e("asdf", "not first, clearing kotlinDomainFactory.getMTickData()");

                    kotlinDomainFactory.getTickData().release();
                    kotlinDomainFactory.setTickData(null);
                } else {
                    Log.e("asdf", "first, keeping kotlinDomainFactory.getMTickData()");
                }
            }

            Stream.of(kotlinDomainFactory.getNotTickFirebaseListeners()).forEach(firebaseListener -> firebaseListener.invoke(this));
            kotlinDomainFactory.getNotTickFirebaseListeners().clear();

            kotlinDomainFactory.setSkipSave(false);

            save(context, 0, source);
        }
    }

    private synchronized void setUserRecord(@NonNull DataSnapshot dataSnapshot) {
        UserWrapper userWrapper = dataSnapshot.getValue(UserWrapper.class);
        check(userWrapper != null);

        RemoteRootUserRecord remoteRootUserRecord = new RemoteRootUserRecord(false, userWrapper);
        kotlinDomainFactory.setRemoteRootUser(new RemoteRootUser(remoteRootUserRecord));
    }

    public synchronized void addFirebaseListener(@NonNull Function1<DomainFactory, Unit> firebaseListener) {
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        kotlinDomainFactory.getNotTickFirebaseListeners().add(firebaseListener);
    }

    public synchronized void removeFirebaseListener(@NonNull Function1<DomainFactory, Unit> firebaseListener) {
        kotlinDomainFactory.getNotTickFirebaseListeners().remove(firebaseListener);
    }

    public synchronized void setFirebaseTickListener(@NonNull Context context, @NonNull SaveService.Source source, @NonNull TickData tickData) {
        check(FirebaseAuth.getInstance().getCurrentUser() != null);

        if ((kotlinDomainFactory.getRemoteProjectFactory() != null) && !kotlinDomainFactory.getRemoteProjectFactory().isSaved() && (kotlinDomainFactory.getTickData() == null)) {
            updateNotificationsTick(context, source, tickData.getSilent(), tickData.getSource());

            tickData.release();
        } else {
            if (kotlinDomainFactory.getTickData() != null) {
                kotlinDomainFactory.setTickData(mergeTickDatas(context, kotlinDomainFactory.getTickData(), tickData));
            } else {
                kotlinDomainFactory.setTickData(tickData);
            }
        }
    }

    @NonNull
    private static TickData mergeTickDatas(@NonNull Context context, @NonNull TickData oldTickData, @NonNull TickData newTickData) {
        boolean silent = (oldTickData.getSilent() && newTickData.getSilent());

        String source = "merged (" + oldTickData + ", " + newTickData + ")";

        oldTickData.releaseWakelock();
        newTickData.releaseWakelock();

        List<Function0<kotlin.Unit>> listeners = new ArrayList<>(oldTickData.getListeners());
        listeners.addAll(newTickData.getListeners());

        return new TickData(silent, source, context, listeners);
    }

    public synchronized boolean isConnected() {
        return (kotlinDomainFactory.getRemoteProjectFactory() != null);
    }

    public synchronized boolean isConnectedAndSaved() {
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);

        return kotlinDomainFactory.getRemoteProjectFactory().isSaved();
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
    public synchronized EditInstanceViewModel.Data getEditInstanceData(@NonNull InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getEditInstanceData");

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Map<CustomTimeKey, CustomTime> currentCustomTimes = Stream.of(kotlinDomainFactory.getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);
        check(instance.isRootInstance(now));

        if (instance.getInstanceTimePair().getCustomTimeKey() != null) {
            CustomTime customTime = kotlinDomainFactory.getCustomTime(instance.getInstanceTimePair().getCustomTimeKey());

            currentCustomTimes.put(customTime.getCustomTimeKey(), customTime);
        }

        Map<CustomTimeKey, EditInstanceViewModel.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : currentCustomTimes.values())
            customTimeDatas.put(customTime.getCustomTimeKey(), new EditInstanceViewModel.CustomTimeData(customTime.getCustomTimeKey(), customTime.getName(), customTime.getHourMinutes()));

        return new EditInstanceViewModel.Data(instance.getInstanceKey(), instance.getInstanceDate(), instance.getInstanceTimePair(), instance.getName(), customTimeDatas, (instance.getDone() != null), instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) <= 0);
    }

    @NonNull
    public synchronized EditInstancesViewModel.Data getEditInstancesData(@NonNull List<InstanceKey> instanceKeys) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getEditInstancesData");

        check(instanceKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Map<CustomTimeKey, CustomTime> currentCustomTimes = Stream.of(kotlinDomainFactory.getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        HashMap<InstanceKey, EditInstancesViewModel.InstanceData> instanceDatas = new HashMap<>();

        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = kotlinDomainFactory.getInstance(instanceKey);
            check(instance.isRootInstance(now));
            check(instance.getDone() == null);

            instanceDatas.put(instanceKey, new EditInstancesViewModel.InstanceData(instance.getInstanceDateTime(), instance.getName()));

            if (instance.getInstanceTimePair().getCustomTimeKey() != null) {
                CustomTime customTime = kotlinDomainFactory.getCustomTime(instance.getInstanceTimePair().getCustomTimeKey());

                currentCustomTimes.put(customTime.getCustomTimeKey(), customTime);
            }
        }

        Map<CustomTimeKey, EditInstancesViewModel.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : currentCustomTimes.values())
            customTimeDatas.put(customTime.getCustomTimeKey(), new EditInstancesViewModel.CustomTimeData(customTime.getCustomTimeKey(), customTime.getName(), customTime.getHourMinutes()));

        Boolean showHour = Stream.of(instanceDatas.values()).allMatch(instanceData -> instanceData.getInstanceDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) < 0);

        return new EditInstancesViewModel.Data(instanceDatas, customTimeDatas, showHour);
    }

    @NonNull
    public synchronized ShowCustomTimeViewModel.Data getShowCustomTimeData(int localCustomTimeId) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowCustomTimeData");

        LocalCustomTime localCustomTime = kotlinDomainFactory.localFactory.getLocalCustomTime(localCustomTimeId);

        HashMap<DayOfWeek, HourMinute> hourMinutes = new HashMap<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values())
            hourMinutes.put(dayOfWeek, localCustomTime.getHourMinute(dayOfWeek));

        return new ShowCustomTimeViewModel.Data(localCustomTime.getId(), localCustomTime.getName(), hourMinutes);
    }

    @NonNull
    public synchronized ShowCustomTimesViewModel.Data getShowCustomTimesData() {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowCustomTimesData");

        List<LocalCustomTime> currentCustomTimes = kotlinDomainFactory.getCurrentCustomTimes();

        ArrayList<ShowCustomTimesViewModel.CustomTimeData> entries = new ArrayList<>();
        for (LocalCustomTime localCustomTime : currentCustomTimes) {
            check(localCustomTime != null);

            entries.add(new ShowCustomTimesViewModel.CustomTimeData(localCustomTime.getId(), localCustomTime.getName()));
        }

        return new ShowCustomTimesViewModel.Data(entries);
    }

    @NonNull
    public synchronized DayViewModel.DayData getGroupListData(@NonNull Context context, @NonNull ExactTimeStamp now, int position, @NonNull MainActivity.TimeRange timeRange) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowNotificationGroupData");

        check(position >= 0);

        ExactTimeStamp startExactTimeStamp;
        ExactTimeStamp endExactTimeStamp;

        if (position == 0) {
            startExactTimeStamp = null;
        } else {
            Calendar startCalendar = now.getCalendar();

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

        Calendar endCalendar = now.getCalendar();

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

        List<Instance> currentInstances = kotlinDomainFactory.getRootInstances(startExactTimeStamp, endExactTimeStamp, now);

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(kotlinDomainFactory.getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        List<GroupListFragment.TaskData> taskDatas = null;
        if (position == 0) {
            taskDatas = kotlinDomainFactory.getTasks()
                    .filter(task -> task.current(now))
                    .filter(task -> task.isVisible(now))
                    .filter(task -> task.isRootTask(now))
                    .filter(task -> task.getCurrentSchedules(now).isEmpty()).map(task -> new GroupListFragment.TaskData(task.getTaskKey(), task.getName(), kotlinDomainFactory.getChildTaskDatas(task, now), task.getStartExactTimeStamp(), task.getNote()))
                    .collect(Collectors.toList());
        }

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances) {
            Task task = instance.getTask();

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            Map<InstanceKey, GroupListFragment.InstanceData> children = kotlinDomainFactory.getChildInstanceDatas(instance, now);
            GroupListFragment.InstanceData instanceData = new GroupListFragment.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), children, null, instance.getOrdinal());
            Stream.of(children.values()).forEach(child -> child.setInstanceDataParent(instanceData));
            instanceDatas.put(instanceData.getInstanceKey(), instanceData);
        }

        GroupListFragment.DataWrapper dataWrapper = new GroupListFragment.DataWrapper(customTimeDatas, null, taskDatas, null, instanceDatas);
        DayViewModel.DayData data = new DayViewModel.DayData(dataWrapper);

        Stream.of(instanceDatas.values()).forEach(instanceData -> instanceData.setInstanceDataParent(dataWrapper));

        Log.e("asdf", "getShowNotificationGroupData returning " + data);
        return data;
    }

    @NonNull
    public synchronized ShowGroupViewModel.Data getShowGroupData(@NonNull Context context, @NonNull TimeStamp timeStamp) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowGroupData");

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Date date = timeStamp.getDate();
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        HourMinute hourMinute = timeStamp.getHourMinute();

        Time time = null;
        for (CustomTime customTime : kotlinDomainFactory.getCurrentCustomTimes())
            if (customTime.getHourMinute(dayOfWeek).equals(hourMinute))
                time = customTime;
        if (time == null)
            time = new NormalTime(hourMinute);

        String displayText = new DateTime(date, time).getDisplayText(context);

        return new ShowGroupViewModel.Data(displayText, getGroupListData(timeStamp, now));
    }

    @NonNull
    public synchronized ShowTaskInstancesViewModel.Data getShowTaskInstancesData(@NonNull TaskKey taskKey) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowTaskInstancesData");

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(kotlinDomainFactory.getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

        Collection<Instance> existingInstances = task.getExistingInstances().values();
        List<Instance> pastInstances = task.getInstances(null, now, now);

        Set<Instance> allInstances = new HashSet<>(existingInstances);
        allInstances.addAll(pastInstances);

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = Stream.of(allInstances)
                .collect(Collectors.toMap(Instance::getInstanceKey, instance -> {
                    Map<InstanceKey, GroupListFragment.InstanceData> children = kotlinDomainFactory.getChildInstanceDatas(instance, now);

                    HierarchyData hierarchyData;
                    if (task.isRootTask(now)) {
                        hierarchyData = null;
                    } else {
                        TaskHierarchy taskHierarchy = kotlinDomainFactory.getParentTaskHierarchy(task, now);
                        check(taskHierarchy != null);

                        hierarchyData = new HierarchyData(taskHierarchy.getTaskHierarchyKey(), taskHierarchy.getOrdinal());
                    }

                    return new GroupListFragment.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(MyApplication.Companion.getInstance(), now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), children, hierarchyData, instance.getOrdinal());
                }, HashMap::new));

        return new ShowTaskInstancesViewModel.Data(new GroupListFragment.DataWrapper(customTimeDatas, task.current(now), null, null, instanceDatas));
    }

    @NonNull
    public synchronized ShowNotificationGroupViewModel.Data getShowNotificationGroupData(@NonNull Context context, @NonNull Set<InstanceKey> instanceKeys) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowNotificationGroupData");

        check(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        ArrayList<Instance> instances = new ArrayList<>();
        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = kotlinDomainFactory.getInstance(instanceKey);

            if (instance.isRootInstance(now))
                instances.add(instance);
        }

        Collections.sort(instances, (lhs, rhs) -> lhs.getInstanceDateTime().compareTo(rhs.getInstanceDateTime()));

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(kotlinDomainFactory.getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : instances) {
            Task task = instance.getTask();

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            Map<InstanceKey, GroupListFragment.InstanceData> children = kotlinDomainFactory.getChildInstanceDatas(instance, now);
            GroupListFragment.InstanceData instanceData = new GroupListFragment.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), children, null, instance.getOrdinal());
            Stream.of(children.values()).forEach(child -> child.setInstanceDataParent(instanceData));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        GroupListFragment.DataWrapper dataWrapper = new GroupListFragment.DataWrapper(customTimeDatas, null, null, null, instanceDatas);

        Stream.of(instanceDatas.values()).forEach(instanceData -> instanceData.setInstanceDataParent(dataWrapper));

        return new ShowNotificationGroupViewModel.Data(dataWrapper);
    }

    @NonNull
    public synchronized ShowInstanceViewModel.Data getShowInstanceData(@NonNull Context context, @NonNull InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowInstanceData");

        Task task = kotlinDomainFactory.getTaskIfPresent(instanceKey.getTaskKey());
        if (task == null) return new ShowInstanceViewModel.Data(null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);
        if (!task.current(now) && !instance.exists()) return new ShowInstanceViewModel.Data(null);

        return new ShowInstanceViewModel.Data(new ShowInstanceViewModel.InstanceData(instance.getName(), instance.getDisplayText(context, now), instance.getDone() != null, task.current(now), instance.isRootInstance(now), instance.exists(), getGroupListData(instance, task, now)));
    }

    @NonNull
    kotlin.Pair<Map<CustomTimeKey, CustomTime>, Map<CreateTaskViewModel.ScheduleData, List<Schedule>>> getScheduleDatas(List<Schedule> schedules, ExactTimeStamp now) {
        Map<CustomTimeKey, CustomTime> customTimes = new HashMap<>();

        Map<CreateTaskViewModel.ScheduleData, List<Schedule>> scheduleDatas = new HashMap<>();

        Map<TimePair, List<WeeklySchedule>> weeklySchedules = new HashMap<>();

        for (Schedule schedule : schedules) {
            check(schedule != null);
            check(schedule.current(now));

            switch (schedule.getScheduleType()) {
                case SINGLE: {
                    SingleSchedule singleSchedule = (SingleSchedule) schedule;

                    scheduleDatas.put(new CreateTaskViewModel.ScheduleData.SingleScheduleData(singleSchedule.getDate(), singleSchedule.getTimePair()), Collections.singletonList(schedule));

                    CustomTimeKey customTimeKey = singleSchedule.getCustomTimeKey();
                    if (customTimeKey != null)
                        customTimes.put(customTimeKey, kotlinDomainFactory.getCustomTime(customTimeKey));

                    break;
                }
                case DAILY: {
                    throw new UnsupportedOperationException();
                }
                case WEEKLY: {
                    WeeklySchedule weeklySchedule = (WeeklySchedule) schedule;

                    TimePair timePair = weeklySchedule.getTimePair();
                    if (!weeklySchedules.containsKey(timePair))
                        weeklySchedules.put(timePair, new ArrayList<>());
                    weeklySchedules.get(timePair).add(weeklySchedule);

                    CustomTimeKey customTimeKey = weeklySchedule.getCustomTimeKey();
                    if (customTimeKey != null)
                        customTimes.put(customTimeKey, kotlinDomainFactory.getCustomTime(customTimeKey));

                    break;
                }
                case MONTHLY_DAY: {
                    MonthlyDaySchedule monthlyDaySchedule = (MonthlyDaySchedule) schedule;

                    scheduleDatas.put(new CreateTaskViewModel.ScheduleData.MonthlyDayScheduleData(monthlyDaySchedule.getDayOfMonth(), monthlyDaySchedule.getBeginningOfMonth(), monthlyDaySchedule.getTimePair()), Collections.singletonList(schedule));

                    CustomTimeKey customTimeKey = monthlyDaySchedule.getCustomTimeKey();
                    if (customTimeKey != null)
                        customTimes.put(customTimeKey, kotlinDomainFactory.getCustomTime(customTimeKey));

                    break;
                }
                case MONTHLY_WEEK: {
                    MonthlyWeekSchedule monthlyWeekSchedule = (MonthlyWeekSchedule) schedule;

                    scheduleDatas.put(new CreateTaskViewModel.ScheduleData.MonthlyWeekScheduleData(monthlyWeekSchedule.getDayOfMonth(), monthlyWeekSchedule.getDayOfWeek(), monthlyWeekSchedule.getBeginningOfMonth(), monthlyWeekSchedule.getTimePair()), Collections.singletonList(schedule));

                    CustomTimeKey customTimeKey = monthlyWeekSchedule.getCustomTimeKey();
                    if (customTimeKey != null)
                        customTimes.put(customTimeKey, kotlinDomainFactory.getCustomTime(customTimeKey));

                    break;
                }
                default: {
                    throw new UnsupportedOperationException();
                }
            }
        }

        for (Map.Entry<TimePair, List<WeeklySchedule>> entry : weeklySchedules.entrySet()) {
            Set<DayOfWeek> daysOfWeek = Stream.of(entry.getValue())
                    .map(WeeklySchedule::getDaysOfWeek)
                    .flatMap(Stream::of)
                    .collect(Collectors.toSet());
            scheduleDatas.put(new CreateTaskViewModel.ScheduleData.WeeklyScheduleData(daysOfWeek, entry.getKey()), new ArrayList<>(entry.getValue()));
        }

        return new kotlin.Pair<>(customTimes, scheduleDatas);
    }

    @NonNull
    public synchronized CreateTaskViewModel.Data getCreateTaskData(@Nullable TaskKey taskKey, @NonNull Context context, @Nullable List<TaskKey> joinTaskKeys) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getCreateTaskData");

        check(taskKey == null || joinTaskKeys == null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Map<CustomTimeKey, CustomTime> customTimes = Stream.of(kotlinDomainFactory.getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        List<TaskKey> excludedTaskKeys = new ArrayList<>();
        if (taskKey != null)
            excludedTaskKeys.add(taskKey);
        else if (joinTaskKeys != null)
            excludedTaskKeys.addAll(joinTaskKeys);

        CreateTaskViewModel.TaskData taskData = null;
        Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> parentTreeDatas;
        if (taskKey != null) {
            Task task = kotlinDomainFactory.getTaskForce(taskKey);

            CreateTaskViewModel.ParentKey.TaskParentKey taskParentKey;
            List<CreateTaskViewModel.ScheduleData> scheduleDatas = null;

            if (task.isRootTask(now)) {
                List<Schedule> schedules = task.getCurrentSchedules(now);

                taskParentKey = null;

                if (!schedules.isEmpty()) {
                    kotlin.Pair<Map<CustomTimeKey, CustomTime>, Map<CreateTaskViewModel.ScheduleData, List<Schedule>>> pair = getScheduleDatas(schedules, now);
                    customTimes.putAll(pair.getFirst());
                    scheduleDatas = new ArrayList<>(pair.getSecond().keySet());
                }
            } else {
                Task parentTask = task.getParentTask(now);
                check(parentTask != null);

                taskParentKey = new CreateTaskViewModel.ParentKey.TaskParentKey(parentTask.getTaskKey());
            }

            RemoteProject remoteProject = task.getRemoteNullableProject();
            String projectName = null;
            if (remoteProject != null)
                projectName = remoteProject.getName();

            taskData = new CreateTaskViewModel.TaskData(task.getName(), taskParentKey, scheduleDatas, task.getNote(), projectName);

            if (task instanceof RemoteTask) {
                RemoteTask remoteTask = (RemoteTask) task;

                parentTreeDatas = kotlinDomainFactory.getProjectTaskTreeDatas(context, now, remoteTask.getRemoteProject(), excludedTaskKeys);
            } else {
                check(task instanceof LocalTask);

                parentTreeDatas = kotlinDomainFactory.getParentTreeDatas(context, now, excludedTaskKeys);
            }
        } else {
            String projectId = null;
            if (joinTaskKeys != null) {
                check(joinTaskKeys.size() > 1);

                List<String> projectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                        .distinct()
                        .collect(Collectors.toList());

                check(projectIds.size() == 1);

                projectId = projectIds.get(0);
            }

            if (!TextUtils.isEmpty(projectId)) {
                check(kotlinDomainFactory.getRemoteProjectFactory() != null);

                RemoteProject remoteProject = kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjectForce(projectId);

                parentTreeDatas = kotlinDomainFactory.getProjectTaskTreeDatas(context, now, remoteProject, excludedTaskKeys);
            } else {
                parentTreeDatas = kotlinDomainFactory.getParentTreeDatas(context, now, excludedTaskKeys);
            }
        }

        @SuppressLint("UseSparseArrays") HashMap<CustomTimeKey, CreateTaskViewModel.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes.values())
            customTimeDatas.put(customTime.getCustomTimeKey(), new CreateTaskViewModel.CustomTimeData(customTime.getCustomTimeKey(), customTime.getName(), customTime.getHourMinutes()));

        return new CreateTaskViewModel.Data(taskData, parentTreeDatas, customTimeDatas);
    }

    @NonNull
    public synchronized ShowTaskViewModel.Data getShowTaskData(@NonNull TaskKey taskKey, @NonNull Context context) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowTaskData");

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        check(task.current(now));

        List<TaskListFragment.ChildTaskData> childTaskDatas = Stream.of(task.getChildTaskHierarchies(now))
                .map(taskHierarchy -> {
                    Task childTask = taskHierarchy.getChildTask();

                    return new TaskListFragment.ChildTaskData(childTask.getName(), childTask.getScheduleText(context, now), kotlinDomainFactory.getChildTaskDatas(childTask, now, context), childTask.getNote(), childTask.getStartExactTimeStamp(), childTask.getTaskKey(), new HierarchyData(taskHierarchy.getTaskHierarchyKey(), taskHierarchy.getOrdinal()));
                })
                .collect(Collectors.toList());
        Collections.sort(childTaskDatas, TaskListFragment.ChildTaskData::compareTo);

        return new ShowTaskViewModel.Data(task.getName(), task.getScheduleText(context, now), new TaskListFragment.TaskData(childTaskDatas, task.getNote()), !task.getExistingInstances().isEmpty());
    }

    @NonNull
    public synchronized MainViewModel.Data getMainData(@NonNull Context context) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getMainData");

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        return new MainViewModel.Data(kotlinDomainFactory.getMainData(now, context));
    }

    @NonNull
    public synchronized ProjectListViewModel.Data getProjectListData() {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getProjectListData");

        check(kotlinDomainFactory.getRemoteProjectFactory() != null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        TreeMap<String, ProjectListViewModel.ProjectData> projectDatas = Stream.of(kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjects().values())
                .filter(remoteProject -> remoteProject.current(now))
                .collect(Collectors.toMap(RemoteProject::getId, remoteProject -> {
                    String users = Stream.of(remoteProject.getUsers())
                            .map(RemoteProjectUser::getName)
                            .collect(Collectors.joining(", "));

                    return new ProjectListViewModel.ProjectData(remoteProject.getId(), remoteProject.getName(), users);
                }, TreeMap::new));

        return new ProjectListViewModel.Data(projectDatas);
    }

    @NonNull
    public synchronized FriendListViewModel.Data getFriendListData() {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getFriendListData");

        check(RemoteFriendFactory.Companion.hasFriends());

        Set<FriendListViewModel.UserListData> userListDatas = Stream.of(RemoteFriendFactory.Companion.getFriends()).map(remoteRootUser -> new FriendListViewModel.UserListData(remoteRootUser.getName(), remoteRootUser.getEmail(), remoteRootUser.getId()))
                .collect(Collectors.toSet());

        return new FriendListViewModel.Data(userListDatas);
    }

    @NonNull
    public synchronized ShowProjectViewModel.Data getShowProjectData(@Nullable String projectId) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowProjectData");

        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(kotlinDomainFactory.getUserInfo() != null);
        check(RemoteFriendFactory.Companion.hasFriends());

        Map<String, ShowProjectViewModel.UserListData> friendDatas = Stream.of(RemoteFriendFactory.Companion.getFriends()).map(remoteRootUser -> new ShowProjectViewModel.UserListData(remoteRootUser.getName(), remoteRootUser.getEmail(), remoteRootUser.getId())).collect(Collectors.toMap(ShowProjectViewModel.UserListData::getId, userData -> userData));

        String name;
        Set<ShowProjectViewModel.UserListData> userListDatas;
        if (!TextUtils.isEmpty(projectId)) {
            RemoteProject remoteProject = kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjectForce(projectId);

            name = remoteProject.getName();

            userListDatas = Stream.of(remoteProject.getUsers()).filterNot(remoteUser -> remoteUser.getId().equals(kotlinDomainFactory.getUserInfo().getKey())).map(remoteUser -> new ShowProjectViewModel.UserListData(remoteUser.getName(), remoteUser.getEmail(), remoteUser.getId()))
                    .collect(Collectors.toSet());
        } else {
            name = null;
            userListDatas = new HashSet<>();
        }

        return new ShowProjectViewModel.Data(name, userListDatas, friendDatas);
    }

    // sets

    public synchronized void setInstanceDateTime(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceDateTime");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        instance.setInstanceDateTime(instanceDate, instanceTimePair, now);

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, instance.getRemoteNullableProject());
    }

    public synchronized void setInstancesDateTime(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull Set<InstanceKey> instanceKeys, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstancesDateTime");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(instanceKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<Instance> instances = Stream.of(instanceKeys).map(kotlinDomainFactory::getInstance)
                .collect(Collectors.toList());

        Stream.of(instances)
                .forEach(instance -> instance.setInstanceDateTime(instanceDate, instanceTimePair, now));

        Set<RemoteProject> remoteProjects = Stream.of(instances)
                .filter(Instance::belongsToRemoteProject)
                .map(Instance::getRemoteNonNullProject)
                .collect(Collectors.toSet());

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, remoteProjects);
    }

    public synchronized void setInstanceAddHourService(@NonNull Context context, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceAddHourService");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        instance.setInstanceDateTime(date, new TimePair(hourMinute), now);
        instance.setNotificationShown(false, now);

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, 0, source);

        kotlinDomainFactory.notifyCloud(context, instance.getRemoteNullableProject());
    }

    public synchronized void setInstanceAddHourActivity(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceAddHourActivity");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        instance.setInstanceDateTime(date, new TimePair(hourMinute), now);

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, instance.getRemoteNullableProject());
    }

    public synchronized void setInstancesAddHourActivity(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull Collection<InstanceKey> instanceKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceAddHourActivity");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        List<Instance> instances = Stream.of(instanceKeys).map(kotlinDomainFactory::getInstance)
                .collect(Collectors.toList());

        Stream.of(instances).forEach(instance -> instance.setInstanceDateTime(date, new TimePair(hourMinute), now));

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        @SuppressWarnings("Convert2MethodRef") Set<RemoteProject> remoteProjects = Stream.of(instances).map(Instance::getRemoteNullableProject)
                .filter(remoteProject -> remoteProject != null)
                .collect(Collectors.toSet());

        kotlinDomainFactory.notifyCloud(context, remoteProjects);
    }

    public synchronized void setInstanceNotificationDone(@NonNull Context context, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceNotificationDone");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        instance.setDone(true, now);
        instance.setNotificationShown(false, now);

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, 0, source);

        kotlinDomainFactory.notifyCloud(context, instance.getRemoteNullableProject());
    }

    @NonNull
    public synchronized ExactTimeStamp setInstancesDone(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull List<InstanceKey> instanceKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstancesDone");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<Instance> instances = Stream.of(instanceKeys).map(kotlinDomainFactory::getInstance)
                .collect(Collectors.toList());

        Stream.of(instances)
                .forEach(instance -> instance.setDone(true, now));

        Set<RemoteProject> remoteProjects = Stream.of(instances)
                .filter(Instance::belongsToRemoteProject)
                .map(Instance::getRemoteNonNullProject)
                .collect(Collectors.toSet());

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, remoteProjects);

        return now;
    }

    public synchronized ExactTimeStamp setInstanceDone(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey, boolean done) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceDone");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Instance instance = kotlinDomainFactory.setInstanceDone(context, now, dataId, source, instanceKey, done);

        return instance.getDone();
    }

    public synchronized void setInstancesNotified(@NonNull Context context, @NonNull SaveService.Source source, @NonNull List<InstanceKey> instanceKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstancesNotified");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        for (InstanceKey instanceKey : instanceKeys)
            setInstanceNotified(instanceKey, now);

        save(context, 0, source);
    }

    public synchronized void setInstanceNotified(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceNotified");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        setInstanceNotified(instanceKey, ExactTimeStamp.Companion.getNow());

        save(context, dataId, source);
    }

    @NonNull
    Task createScheduleRootTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull List<CreateTaskViewModel.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        check(!TextUtils.isEmpty(name));
        check(!scheduleDatas.isEmpty());

        Task task;
        if (TextUtils.isEmpty(projectId)) {
            task = kotlinDomainFactory.localFactory.createScheduleRootTask(kotlinDomainFactory, now, name, scheduleDatas, note);
        } else {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);

            task = kotlinDomainFactory.getRemoteProjectFactory().createScheduleRootTask(now, name, scheduleDatas, note, projectId);
        }

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, task.getRemoteNullableProject());

        return task;
    }

    public synchronized void createScheduleRootTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull List<CreateTaskViewModel.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createScheduleRootTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        createScheduleRootTask(context, now, dataId, source, name, scheduleDatas, note, projectId);
    }

    @NonNull
    TaskKey updateScheduleTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @NonNull List<CreateTaskViewModel.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        check(!TextUtils.isEmpty(name));
        check(!scheduleDatas.isEmpty());

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        check(task.current(now));

        task = task.updateProject(context, now, projectId);

        task.setName(name, note);

        if (!task.isRootTask(now)) {
            TaskHierarchy taskHierarchy = kotlinDomainFactory.getParentTaskHierarchy(task, now);
            check(taskHierarchy != null);

            taskHierarchy.setEndExactTimeStamp(now);
        }

        task.updateSchedules(scheduleDatas, now);

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    @NonNull
    public synchronized TaskKey updateScheduleTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @NonNull List<CreateTaskViewModel.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateScheduleTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));
        check(!scheduleDatas.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        return updateScheduleTask(context, now, dataId, source, taskKey, name, scheduleDatas, note, projectId);
    }

    public synchronized void createScheduleJoinRootTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull List<CreateTaskViewModel.ScheduleData> scheduleDatas, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createScheduleJoinRootTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));
        check(!scheduleDatas.isEmpty());
        check(joinTaskKeys.size() > 1);

        List<String> joinProjectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                .distinct()
                .collect(Collectors.toList());
        check(joinProjectIds.size() == 1);

        String joinProjectId = joinProjectIds.get(0);

        final String finalProjectId;
        if (!TextUtils.isEmpty(joinProjectId)) {
            check(TextUtils.isEmpty(projectId));

            finalProjectId = joinProjectId;
        } else if (!TextUtils.isEmpty(projectId)) {
            finalProjectId = projectId;
        } else {
            finalProjectId = null;
        }

        List<Task> joinTasks = Stream.of(joinTaskKeys).map(kotlinDomainFactory::getTaskForce)
                .collect(Collectors.toList());

        Task newParentTask;
        if (!TextUtils.isEmpty(finalProjectId)) {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);
            check(kotlinDomainFactory.getUserInfo() != null);

            newParentTask = kotlinDomainFactory.getRemoteProjectFactory().createScheduleRootTask(now, name, scheduleDatas, note, finalProjectId);
        } else {
            newParentTask = kotlinDomainFactory.localFactory.createScheduleRootTask(kotlinDomainFactory, now, name, scheduleDatas, note);
        }

        joinTasks = Stream.of(joinTasks)
                .map(joinTask -> joinTask.updateProject(context, now, projectId))
                .collect(Collectors.toList());

        kotlinDomainFactory.joinTasks(newParentTask, joinTasks, now);

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, newParentTask.getRemoteNullableProject());
    }

    Task createChildTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey parentTaskKey, @NonNull String name, @Nullable String note) {
        check(!TextUtils.isEmpty(name));

        Task parentTask = kotlinDomainFactory.getTaskForce(parentTaskKey);
        check(parentTask.current(now));

        Task childTask = parentTask.createChildTask(now, name, note);

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, childTask.getRemoteNullableProject());

        return childTask;
    }

    public synchronized void createChildTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey parentTaskKey, @NonNull String name, @Nullable String note) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createChildTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        createChildTask(context, now, dataId, source, parentTaskKey, name, note);
    }

    public synchronized void createJoinChildTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey parentTaskKey, @NonNull String name, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createJoinChildTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));
        check(joinTaskKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task parentTask = kotlinDomainFactory.getTaskForce(parentTaskKey);
        check(parentTask.current(now));

        List<String> joinProjectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                .distinct()
                .collect(Collectors.toList());
        check(joinProjectIds.size() == 1);

        List<Task> joinTasks = Stream.of(joinTaskKeys).map(kotlinDomainFactory::getTaskForce)
                .collect(Collectors.toList());

        Task childTask = parentTask.createChildTask(now, name, note);

        kotlinDomainFactory.joinTasks(childTask, joinTasks, now);

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, childTask.getRemoteNullableProject());
    }

    @NonNull
    public synchronized TaskKey updateChildTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @NonNull TaskKey parentTaskKey, @Nullable String note) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateChildTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        check(task.current(now));

        Task newParentTask = kotlinDomainFactory.getTaskForce(parentTaskKey);
        check(task.current(now));

        task.setName(name, note);

        Task oldParentTask = task.getParentTask(now);
        if (oldParentTask == null) {
            Stream.of(task.getCurrentSchedules(now))
                    .forEach(schedule -> schedule.setEndExactTimeStamp(now));

            newParentTask.addChild(task, now);
        } else if (oldParentTask != newParentTask) {
            TaskHierarchy oldTaskHierarchy = kotlinDomainFactory.getParentTaskHierarchy(task, now);
            check(oldTaskHierarchy != null);

            oldTaskHierarchy.setEndExactTimeStamp(now);

            newParentTask.addChild(task, now);
        }

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    public synchronized void setTaskEndTimeStamp(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setTaskEndTimeStamp");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        check(task.current(now));

        task.setEndExactTimeStamp(now);

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, task.getRemoteNullableProject());
    }

    public synchronized void setInstanceOrdinal(int dataId, @NonNull InstanceKey instanceKey, double ordinal) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceOrdinal");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Instance instance = kotlinDomainFactory.getInstance(instanceKey);

        instance.setOrdinal(ordinal, now);

        kotlinDomainFactory.updateNotifications(MyApplication.Companion.getInstance(), now);

        save(MyApplication.Companion.getInstance(), dataId, SaveService.Source.GUI);

        kotlinDomainFactory.notifyCloud(MyApplication.Companion.getInstance(), instance.getRemoteNullableProject());
    }

    public synchronized void setTaskHierarchyOrdinal(int dataId, @NonNull HierarchyData hierarchyData) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setTaskHierarchyOrdinal");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        RemoteProject remoteProject;
        TaskHierarchy taskHierarchy;
        if (hierarchyData.getTaskHierarchyKey() instanceof TaskHierarchyKey.LocalTaskHierarchyKey) {
            TaskHierarchyKey.LocalTaskHierarchyKey localTaskHierarchyKey = (TaskHierarchyKey.LocalTaskHierarchyKey) hierarchyData.getTaskHierarchyKey();

            remoteProject = null;
            taskHierarchy = kotlinDomainFactory.localFactory.getTaskHierarchy(localTaskHierarchyKey);
        } else {
            check(hierarchyData.getTaskHierarchyKey() instanceof TaskHierarchyKey.RemoteTaskHierarchyKey);

            TaskHierarchyKey.RemoteTaskHierarchyKey remoteTaskHierarchyKey = (TaskHierarchyKey.RemoteTaskHierarchyKey) hierarchyData.getTaskHierarchyKey();

            remoteProject = kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjectForce(remoteTaskHierarchyKey.getProjectId());
            taskHierarchy = remoteProject.getTaskHierarchy(remoteTaskHierarchyKey.getTaskHierarchyId());
        }

        check(taskHierarchy.current(now));

        taskHierarchy.setOrdinal(hierarchyData.getOrdinal());

        kotlinDomainFactory.updateNotifications(MyApplication.Companion.getInstance(), now);

        save(MyApplication.Companion.getInstance(), dataId, SaveService.Source.GUI);

        if (remoteProject != null)
            kotlinDomainFactory.notifyCloud(MyApplication.Companion.getInstance(), remoteProject);
    }

    public synchronized void setTaskEndTimeStamps(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull ArrayList<TaskKey> taskKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setTaskEndTimeStamps");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!taskKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<Task> tasks = Stream.of(taskKeys).map(kotlinDomainFactory::getTaskForce)
                .collect(Collectors.toList());

        check(Stream.of(tasks)
                .allMatch(task -> task.current(now)));

        Stream.of(tasks)
                .forEach(task -> task.setEndExactTimeStamp(now));

        Set<RemoteProject> remoteProjects = Stream.of(tasks)
                .filter(Task::belongsToRemoteProject)
                .map(Task::getRemoteNonNullProject)
                .collect(Collectors.toSet());

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, remoteProjects);
    }

    public synchronized int createCustomTime(@NonNull Context context, @NonNull SaveService.Source source, @NonNull String name, @NonNull Map<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createCustomTime");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));

        check(hourMinutes.get(DayOfWeek.SUNDAY) != null);
        check(hourMinutes.get(DayOfWeek.MONDAY) != null);
        check(hourMinutes.get(DayOfWeek.TUESDAY) != null);
        check(hourMinutes.get(DayOfWeek.WEDNESDAY) != null);
        check(hourMinutes.get(DayOfWeek.THURSDAY) != null);
        check(hourMinutes.get(DayOfWeek.FRIDAY) != null);
        check(hourMinutes.get(DayOfWeek.SATURDAY) != null);

        LocalCustomTime localCustomTime = kotlinDomainFactory.localFactory.createLocalCustomTime(kotlinDomainFactory, name, hourMinutes);

        save(context, 0, source);

        return localCustomTime.getId();
    }

    public synchronized void updateCustomTime(@NonNull Context context, int dataId, @NonNull SaveService.Source source, int localCustomTimeId, @NonNull String name, @NonNull Map<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateCustomTime");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));

        LocalCustomTime localCustomTime = kotlinDomainFactory.localFactory.getLocalCustomTime(localCustomTimeId);

        localCustomTime.setName(name);

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            HourMinute hourMinute = hourMinutes.get(dayOfWeek);
            check(hourMinute != null);

            if (hourMinute.compareTo(localCustomTime.getHourMinute(dayOfWeek)) != 0)
                localCustomTime.setHourMinute(dayOfWeek, hourMinute);
        }

        save(context, dataId, source);
    }

    public synchronized void setCustomTimeCurrent(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull List<Integer> localCustomTimeIds) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setCustomTimeCurrent");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!localCustomTimeIds.isEmpty());

        for (int localCustomTimeId : localCustomTimeIds) {
            LocalCustomTime localCustomTime = kotlinDomainFactory.localFactory.getLocalCustomTime(localCustomTimeId);

            localCustomTime.setCurrent();
        }

        save(context, dataId, source);
    }

    @NonNull
    Task createRootTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        check(!TextUtils.isEmpty(name));

        Task task;
        if (TextUtils.isEmpty(projectId)) {
            task = kotlinDomainFactory.localFactory.createLocalTaskHelper(kotlinDomainFactory, name, now, note);
        } else {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);

            task = kotlinDomainFactory.getRemoteProjectFactory().createRemoteTaskHelper(now, name, note, projectId);
        }

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, task.getRemoteNullableProject());

        return task;
    }

    public synchronized void createRootTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createRootTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        createRootTask(context, now, dataId, source, name, note, projectId);
    }

    public synchronized void createJoinRootTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createJoinRootTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));
        check(joinTaskKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<String> joinProjectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                .distinct()
                .collect(Collectors.toList());
        check(joinProjectIds.size() == 1);

        String joinProjectId = joinProjectIds.get(0);

        final String finalProjectId;
        if (!TextUtils.isEmpty(joinProjectId)) {
            check(TextUtils.isEmpty(projectId));

            finalProjectId = joinProjectId;
        } else if (!TextUtils.isEmpty(projectId)) {
            finalProjectId = projectId;
        } else {
            finalProjectId = null;
        }

        List<Task> joinTasks = Stream.of(joinTaskKeys).map(kotlinDomainFactory::getTaskForce)
                .collect(Collectors.toList());

        Task newParentTask;
        if (!TextUtils.isEmpty(finalProjectId)) {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);
            check(kotlinDomainFactory.getUserInfo() != null);

            newParentTask = kotlinDomainFactory.getRemoteProjectFactory().createRemoteTaskHelper(now, name, note, finalProjectId);
        } else {
            newParentTask = kotlinDomainFactory.localFactory.createLocalTaskHelper(kotlinDomainFactory, name, now, note);
        }

        joinTasks = Stream.of(joinTasks)
                .map(joinTask -> joinTask.updateProject(context, now, projectId))
                .collect(Collectors.toList());

        kotlinDomainFactory.joinTasks(newParentTask, joinTasks, now);

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, newParentTask.getRemoteNullableProject());
    }

    @NonNull
    public synchronized TaskKey updateRootTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateRootTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        check(task.current(now));

        task = task.updateProject(context, now, projectId);

        task.setName(name, note);

        TaskHierarchy taskHierarchy = kotlinDomainFactory.getParentTaskHierarchy(task, now);
        if (taskHierarchy != null)
            taskHierarchy.setEndExactTimeStamp(now);

        Stream.of(task.getCurrentSchedules(now))
                .forEach(schedule -> schedule.setEndExactTimeStamp(now));

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    @NonNull
    Irrelevant updateNotificationsTick(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull SaveService.Source source, boolean silent) {
        kotlinDomainFactory.updateNotifications(context, silent, now, new ArrayList<>());

        Irrelevant irrelevant = kotlinDomainFactory.setIrrelevant(now);

        if (kotlinDomainFactory.getRemoteProjectFactory() != null)
            kotlinDomainFactory.localFactory.deleteInstanceShownRecords(kotlinDomainFactory.getRemoteProjectFactory().getTaskKeys());

        save(context, 0, source);

        return irrelevant;
    }

    public synchronized void updateNotificationsTick(@NonNull Context context, @NonNull SaveService.Source source, boolean silent, @NonNull String sourceName) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateNotificationsTick source: " + sourceName);
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        updateNotificationsTick(context, now, source, silent);
    }

    public synchronized void removeFriends(@NonNull Set<String> keys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.removeFriends");

        check(kotlinDomainFactory.getUserInfo() != null);
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(RemoteFriendFactory.Companion.hasFriends());
        check(!RemoteFriendFactory.Companion.isSaved());

        Stream.of(keys).forEach(friendId -> RemoteFriendFactory.Companion.removeFriend(kotlinDomainFactory.getUserInfo().getKey(), friendId));

        RemoteFriendFactory.Companion.save();
    }

    public synchronized void updateUserInfo(@NonNull Context context, @NonNull SaveService.Source source, @NonNull UserInfo userInfo) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateUserInfo");
        check(kotlinDomainFactory.getUserInfo() != null);
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);

        if (kotlinDomainFactory.getUserInfo().equals(userInfo))
            return;

        kotlinDomainFactory.setUserInfo(userInfo);
        DatabaseWrapper.INSTANCE.setUserInfo(userInfo, kotlinDomainFactory.localFactory.getUuid());

        kotlinDomainFactory.getRemoteProjectFactory().updateUserInfo(userInfo);

        save(context, 0, source);
    }

    public synchronized void updateProject(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull String projectId, @NonNull String name, @NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateProject");

        check(!TextUtils.isEmpty(projectId));
        check(!TextUtils.isEmpty(name));
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(RemoteFriendFactory.Companion.hasFriends());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        RemoteProject remoteProject = kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjectForce(projectId);

        remoteProject.setName(name);
        remoteProject.updateRecordOf(Stream.of(addedFriends).map(RemoteFriendFactory.Companion::getFriend)
                .collect(Collectors.toSet()), removedFriends);

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, remoteProject, removedFriends);
    }

    public synchronized void createProject(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull Set<String> friends) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createProject");

        check(!TextUtils.isEmpty(name));
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(kotlinDomainFactory.getUserInfo() != null);
        check(kotlinDomainFactory.getRemoteRootUser() != null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Set<String> recordOf = new HashSet<>(friends);

        String key = kotlinDomainFactory.getUserInfo().getKey();
        check(!recordOf.contains(key));
        recordOf.add(key);

        RemoteProject remoteProject = kotlinDomainFactory.getRemoteProjectFactory().createRemoteProject(name, now, recordOf, kotlinDomainFactory.getRemoteRootUser());

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, remoteProject);
    }

    public synchronized void setProjectEndTimeStamps(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull Set<String> projectIds) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setProjectEndTimeStamps");

        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(kotlinDomainFactory.getUserInfo() != null);
        check(!projectIds.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Set<RemoteProject> remoteProjects = Stream.of(projectIds).map(kotlinDomainFactory.getRemoteProjectFactory()::getRemoteProjectForce)
                .collect(Collectors.toSet());

        check(Stream.of(remoteProjects)
                .allMatch(remoteProject -> remoteProject.current(now)));

        Stream.of(remoteProjects)
                .forEach(remoteProject -> remoteProject.setEndExactTimeStamp(now));

        kotlinDomainFactory.updateNotifications(context, now);

        save(context, dataId, source);

        kotlinDomainFactory.notifyCloud(context, remoteProjects);
    }

    // internal

    void notifyInstance(@NonNull Instance instance, boolean silent, @NonNull ExactTimeStamp now) {
        long realtime = SystemClock.elapsedRealtime();

        Optional<Long> optional = Stream.of(kotlinDomainFactory.getLastNotificationBeeps().values()).max(Long::compareTo);
        if (optional.isPresent() && realtime - optional.get() < 5000) {
            Log.e("asdf", "skipping notification sound for " + instance.getName());

            silent = true;
        }

        NotificationWrapper.Companion.getInstance().notifyInstance(kotlinDomainFactory, instance, silent, now);

        if (!silent)
            kotlinDomainFactory.getLastNotificationBeeps().put(instance.getInstanceKey(), SystemClock.elapsedRealtime());
    }

    void updateInstance(@NonNull Instance instance, @NonNull ExactTimeStamp now) {
        InstanceKey instanceKey = instance.getInstanceKey();

        long realtime = SystemClock.elapsedRealtime();

        if (kotlinDomainFactory.getLastNotificationBeeps().containsKey(instanceKey)) {
            long then = kotlinDomainFactory.getLastNotificationBeeps().get(instanceKey);

            check(realtime > then);

            if (realtime - then < 5000) {
                Log.e("asdf", "skipping notification update for " + instance.getName());

                return;
            }
        }

        NotificationWrapper.Companion.getInstance().notifyInstance(kotlinDomainFactory, instance, true, now);
    }

    private void setInstanceNotified(@NonNull InstanceKey instanceKey, @NonNull ExactTimeStamp now) {
        if (instanceKey.getType() == TaskKey.Type.LOCAL) {
            Instance instance = kotlinDomainFactory.getInstance(instanceKey);

            instance.setNotified(now);
            instance.setNotificationShown(false, now);
        } else {
            TaskKey taskKey = instanceKey.getTaskKey();

            String projectId = taskKey.getRemoteProjectId();
            check(!TextUtils.isEmpty(projectId));

            String taskId = taskKey.getRemoteTaskId();
            check(!TextUtils.isEmpty(taskId));

            ScheduleKey scheduleKey = instanceKey.getScheduleKey();
            Date scheduleDate = scheduleKey.getScheduleDate();

            @SuppressWarnings("ConstantConditions") Stream<InstanceShownRecord> stream = Stream.of(kotlinDomainFactory.localFactory.getInstanceShownRecords()).filter(instanceShownRecord -> instanceShownRecord.getProjectId().equals(projectId)).filter(instanceShownRecord -> instanceShownRecord.getTaskId().equals(taskId)).filter(instanceShownRecord -> instanceShownRecord.getScheduleYear() == scheduleDate.getYear()).filter(instanceShownRecord -> instanceShownRecord.getScheduleMonth() == scheduleDate.getMonth()).filter(instanceShownRecord -> instanceShownRecord.getScheduleDay() == scheduleDate.getDay());

            List<InstanceShownRecord> matches;
            if (scheduleKey.getScheduleTimePair().getCustomTimeKey() != null) {
                check(scheduleKey.getScheduleTimePair().getHourMinute() == null);

                check(scheduleKey.getScheduleTimePair().getCustomTimeKey().getType() == TaskKey.Type.REMOTE); // remote custom time key hack
                check(scheduleKey.getScheduleTimePair().getCustomTimeKey().getLocalCustomTimeId() == null);
                check(projectId.equals(scheduleKey.getScheduleTimePair().getCustomTimeKey().getRemoteProjectId()));

                String customTimeId = scheduleKey.getScheduleTimePair().getCustomTimeKey().getRemoteCustomTimeId();
                check(!TextUtils.isEmpty(customTimeId));

                matches = stream.filter(instanceShownRecord -> customTimeId.equals(instanceShownRecord.getScheduleCustomTimeId()))
                        .collect(Collectors.toList());
            } else {
                check(scheduleKey.getScheduleTimePair().getHourMinute() != null);

                HourMinute hourMinute = scheduleKey.getScheduleTimePair().getHourMinute();

                matches = stream.filter(instanceShownRecord -> Integer.valueOf(hourMinute.getHour()).equals(instanceShownRecord.getScheduleHour()))
                        .filter(instanceShownRecord -> Integer.valueOf(hourMinute.getMinute()).equals(instanceShownRecord.getScheduleMinute()))
                        .collect(Collectors.toList());
            }

            check(matches.size() == 1);

            InstanceShownRecord instanceShownRecord = matches.get(0);
            check(instanceShownRecord != null);

            instanceShownRecord.setNotified(true);
            instanceShownRecord.setNotificationShown(false);
        }
    }

    @NonNull
    private GroupListFragment.DataWrapper getGroupListData(@NonNull TimeStamp timeStamp, @NonNull ExactTimeStamp now) {
        Calendar endCalendar = timeStamp.getCalendar();
        endCalendar.add(Calendar.MINUTE, 1);
        TimeStamp endTimeStamp = new TimeStamp(endCalendar);

        List<Instance> rootInstances = kotlinDomainFactory.getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now);

        List<Instance> currentInstances = Stream.of(rootInstances)
                .filter(instance -> instance.getInstanceDateTime().getTimeStamp().compareTo(timeStamp) == 0)
                .collect(Collectors.toList());

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(kotlinDomainFactory.getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances) {
            Task task = instance.getTask();

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            Map<InstanceKey, GroupListFragment.InstanceData> children = kotlinDomainFactory.getChildInstanceDatas(instance, now);
            GroupListFragment.InstanceData instanceData = new GroupListFragment.InstanceData(instance.getDone(), instance.getInstanceKey(), null, instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), children, null, instance.getOrdinal());
            Stream.of(children.values()).forEach(child -> child.setInstanceDataParent(instanceData));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        GroupListFragment.DataWrapper dataWrapper = new GroupListFragment.DataWrapper(customTimeDatas, null, null, null, instanceDatas);

        Stream.of(instanceDatas.values()).forEach(instanceData -> instanceData.setInstanceDataParent(dataWrapper));

        return dataWrapper;
    }

    @NonNull
    private GroupListFragment.DataWrapper getGroupListData(@NonNull Instance instance, @NonNull Task task, @NonNull ExactTimeStamp now) {
        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(kotlinDomainFactory.getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        for (kotlin.Pair<Instance, TaskHierarchy> pair : instance.getChildInstances(now)) {
            Instance childInstance = pair.getFirst();
            TaskHierarchy taskHierarchy = pair.getSecond();
            Task childTask = childInstance.getTask();

            Boolean isRootTask = (childTask.current(now) ? childTask.isRootTask(now) : null);

            Map<InstanceKey, GroupListFragment.InstanceData> children = kotlinDomainFactory.getChildInstanceDatas(childInstance, now);
            GroupListFragment.InstanceData instanceData = new GroupListFragment.InstanceData(childInstance.getDone(), childInstance.getInstanceKey(), null, childInstance.getName(), childInstance.getInstanceDateTime().getTimeStamp(), childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), childInstance.getInstanceDateTime().getTime().getTimePair(), childTask.getNote(), children, new HierarchyData(taskHierarchy.getTaskHierarchyKey(), taskHierarchy.getOrdinal()), childInstance.getOrdinal());
            Stream.of(children.values()).forEach(child -> child.setInstanceDataParent(instanceData));
            instanceDatas.put(childInstance.getInstanceKey(), instanceData);
        }

        GroupListFragment.DataWrapper dataWrapper = new GroupListFragment.DataWrapper(customTimeDatas, task.current(now), null, task.getNote(), instanceDatas);

        Stream.of(instanceDatas.values()).forEach(instanceData -> instanceData.setInstanceDataParent(dataWrapper));

        return dataWrapper;
    }

    @NonNull
    public CustomTimeKey getCustomTimeKey(@NonNull String remoteProjectId, @NonNull String remoteCustomTimeId) {
        LocalCustomTime localCustomTime = kotlinDomainFactory.localFactory.getLocalCustomTime(remoteProjectId, remoteCustomTimeId);

        if (localCustomTime == null) {
            return new CustomTimeKey(remoteProjectId, remoteCustomTimeId);
        } else {
            return localCustomTime.getCustomTimeKey();
        }
    }

    public static class LocalToRemoteConversion {
        public final Map<Integer, kotlin.Pair<LocalTask, List<LocalInstance>>> mLocalTasks = new HashMap<>();
        public final List<LocalTaskHierarchy> mLocalTaskHierarchies = new ArrayList<>();

        final Map<Integer, RemoteTask> mRemoteTasks = new HashMap<>();
        final List<RemoteTaskHierarchy> mRemoteTaskHierarchies = new ArrayList<>();
    }
}
