package com.krystianwsul.checkme.domainmodel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
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
import com.krystianwsul.checkme.domainmodel.local.LocalFactory;
import com.krystianwsul.checkme.domainmodel.local.LocalInstance;
import com.krystianwsul.checkme.domainmodel.local.LocalTask;
import com.krystianwsul.checkme.domainmodel.local.LocalTaskHierarchy;
import com.krystianwsul.checkme.domainmodel.relevance.InstanceRelevance;
import com.krystianwsul.checkme.domainmodel.relevance.LocalCustomTimeRelevance;
import com.krystianwsul.checkme.domainmodel.relevance.RemoteCustomTimeRelevance;
import com.krystianwsul.checkme.domainmodel.relevance.RemoteProjectRelevance;
import com.krystianwsul.checkme.domainmodel.relevance.TaskRelevance;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.RemoteCustomTime;
import com.krystianwsul.checkme.firebase.RemoteFriendFactory;
import com.krystianwsul.checkme.firebase.RemoteInstance;
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
import com.krystianwsul.checkme.notifications.TickJobIntentService;
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord;
import com.krystianwsul.checkme.persistencemodel.PersistenceManger;
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
import java.util.Arrays;
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
        kotlinDomainFactory.localFactory = LocalFactory.Companion.getInstance();
    }

    DomainFactory(@NonNull KotlinDomainFactory kotlinDomainFactory, @NonNull PersistenceManger persistenceManger) {
        this.kotlinDomainFactory = kotlinDomainFactory;
        kotlinDomainFactory.localFactory = new LocalFactory(persistenceManger);
    }

    void initialize() {
        kotlinDomainFactory.localFactory.initialize(this);
    }

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
        return getCustomTimes().size();
    }

    private void save(@NonNull Context context, int dataId, @NonNull SaveService.Source source) {
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

            updateNotifications(context, now);

            ObserverHolder.INSTANCE.notifyDomainObservers(new ArrayList<>());
        }
    }

    private synchronized void setRemoteTaskRecords(@NonNull Context context, @NonNull DataSnapshot dataSnapshot, @NonNull SaveService.Source source) {
        check(kotlinDomainFactory.getUserInfo() != null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        kotlinDomainFactory.localFactory.clearRemoteCustomTimeRecords();

        boolean firstThereforeSilent = (kotlinDomainFactory.getRemoteProjectFactory() == null);
        kotlinDomainFactory.setRemoteProjectFactory(new RemoteProjectFactory(this, dataSnapshot.getChildren(), kotlinDomainFactory.getUserInfo(), kotlinDomainFactory.localFactory.getUuid(), now));

        RemoteFriendFactory.Companion.tryNotifyFriendListeners(); // assuming they're all getters

        if (kotlinDomainFactory.getTickData() == null && kotlinDomainFactory.getNotTickFirebaseListeners().isEmpty()) {
            updateNotifications(context, firstThereforeSilent, ExactTimeStamp.Companion.getNow(), new ArrayList<>());

            save(context, 0, source);
        } else {
            kotlinDomainFactory.setSkipSave(true);

            if (kotlinDomainFactory.getTickData() == null) {
                updateNotifications(context, firstThereforeSilent, ExactTimeStamp.Companion.getNow(), new ArrayList<>());
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

        Map<CustomTimeKey, CustomTime> currentCustomTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        Instance instance = getInstance(instanceKey);
        check(instance.isRootInstance(now));

        if (instance.getInstanceTimePair().getCustomTimeKey() != null) {
            CustomTime customTime = getCustomTime(instance.getInstanceTimePair().getCustomTimeKey());

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

        Map<CustomTimeKey, CustomTime> currentCustomTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        HashMap<InstanceKey, EditInstancesViewModel.InstanceData> instanceDatas = new HashMap<>();

        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = getInstance(instanceKey);
            check(instance.isRootInstance(now));
            check(instance.getDone() == null);

            instanceDatas.put(instanceKey, new EditInstancesViewModel.InstanceData(instance.getInstanceDateTime(), instance.getName()));

            if (instance.getInstanceTimePair().getCustomTimeKey() != null) {
                CustomTime customTime = getCustomTime(instance.getInstanceTimePair().getCustomTimeKey());

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

        List<LocalCustomTime> currentCustomTimes = getCurrentCustomTimes();

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

        List<Instance> currentInstances = getRootInstances(startExactTimeStamp, endExactTimeStamp, now);

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        List<GroupListFragment.TaskData> taskDatas = null;
        if (position == 0) {
            taskDatas = getTasks()
                    .filter(task -> task.current(now))
                    .filter(task -> task.isVisible(now))
                    .filter(task -> task.isRootTask(now))
                    .filter(task -> task.getCurrentSchedules(now).isEmpty())
                    .map(task -> new GroupListFragment.TaskData(task.getTaskKey(), task.getName(), getChildTaskDatas(task, now), task.getStartExactTimeStamp(), task.getNote()))
                    .collect(Collectors.toList());
        }

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances) {
            Task task = instance.getTask();

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            HashMap<InstanceKey, GroupListFragment.InstanceData> children = getChildInstanceDatas(instance, now);
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
        for (CustomTime customTime : getCurrentCustomTimes())
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

        Task task = getTaskForce(taskKey);
        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

        Collection<Instance> existingInstances = task.getExistingInstances().values();
        List<Instance> pastInstances = task.getInstances(null, now, now);

        Set<Instance> allInstances = new HashSet<>(existingInstances);
        allInstances.addAll(pastInstances);

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = Stream.of(allInstances)
                .collect(Collectors.toMap(Instance::getInstanceKey, instance -> {
                    HashMap<InstanceKey, GroupListFragment.InstanceData> children = getChildInstanceDatas(instance, now);

                    HierarchyData hierarchyData;
                    if (task.isRootTask(now)) {
                        hierarchyData = null;
                    } else {
                        TaskHierarchy taskHierarchy = getParentTaskHierarchy(task, now);
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
            Instance instance = getInstance(instanceKey);

            if (instance.isRootInstance(now))
                instances.add(instance);
        }

        Collections.sort(instances, (lhs, rhs) -> lhs.getInstanceDateTime().compareTo(rhs.getInstanceDateTime()));

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : instances) {
            Task task = instance.getTask();

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            HashMap<InstanceKey, GroupListFragment.InstanceData> children = getChildInstanceDatas(instance, now);
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

        Task task = getTaskIfPresent(instanceKey.getTaskKey());
        if (task == null) return new ShowInstanceViewModel.Data(null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Instance instance = getInstance(instanceKey);
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
                        customTimes.put(customTimeKey, getCustomTime(customTimeKey));

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
                        customTimes.put(customTimeKey, getCustomTime(customTimeKey));

                    break;
                }
                case MONTHLY_DAY: {
                    MonthlyDaySchedule monthlyDaySchedule = (MonthlyDaySchedule) schedule;

                    scheduleDatas.put(new CreateTaskViewModel.ScheduleData.MonthlyDayScheduleData(monthlyDaySchedule.getDayOfMonth(), monthlyDaySchedule.getBeginningOfMonth(), monthlyDaySchedule.getTimePair()), Collections.singletonList(schedule));

                    CustomTimeKey customTimeKey = monthlyDaySchedule.getCustomTimeKey();
                    if (customTimeKey != null)
                        customTimes.put(customTimeKey, getCustomTime(customTimeKey));

                    break;
                }
                case MONTHLY_WEEK: {
                    MonthlyWeekSchedule monthlyWeekSchedule = (MonthlyWeekSchedule) schedule;

                    scheduleDatas.put(new CreateTaskViewModel.ScheduleData.MonthlyWeekScheduleData(monthlyWeekSchedule.getDayOfMonth(), monthlyWeekSchedule.getDayOfWeek(), monthlyWeekSchedule.getBeginningOfMonth(), monthlyWeekSchedule.getTimePair()), Collections.singletonList(schedule));

                    CustomTimeKey customTimeKey = monthlyWeekSchedule.getCustomTimeKey();
                    if (customTimeKey != null)
                        customTimes.put(customTimeKey, getCustomTime(customTimeKey));

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

        Map<CustomTimeKey, CustomTime> customTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        List<TaskKey> excludedTaskKeys = new ArrayList<>();
        if (taskKey != null)
            excludedTaskKeys.add(taskKey);
        else if (joinTaskKeys != null)
            excludedTaskKeys.addAll(joinTaskKeys);

        CreateTaskViewModel.TaskData taskData = null;
        Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> parentTreeDatas;
        if (taskKey != null) {
            Task task = getTaskForce(taskKey);

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

                parentTreeDatas = getProjectTaskTreeDatas(context, now, remoteTask.getRemoteProject(), excludedTaskKeys);
            } else {
                check(task instanceof LocalTask);

                parentTreeDatas = getParentTreeDatas(context, now, excludedTaskKeys);
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

                parentTreeDatas = getProjectTaskTreeDatas(context, now, remoteProject, excludedTaskKeys);
            } else {
                parentTreeDatas = getParentTreeDatas(context, now, excludedTaskKeys);
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

        Task task = getTaskForce(taskKey);
        check(task.current(now));

        List<TaskListFragment.ChildTaskData> childTaskDatas = Stream.of(task.getChildTaskHierarchies(now))
                .map(taskHierarchy -> {
                    Task childTask = taskHierarchy.getChildTask();

                    return new TaskListFragment.ChildTaskData(childTask.getName(), childTask.getScheduleText(context, now), getChildTaskDatas(childTask, now, context), childTask.getNote(), childTask.getStartExactTimeStamp(), childTask.getTaskKey(), new HierarchyData(taskHierarchy.getTaskHierarchyKey(), taskHierarchy.getOrdinal()));
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

        return new MainViewModel.Data(getMainData(now, context));
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

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        instance.setInstanceDateTime(instanceDate, instanceTimePair, now);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, instance.getRemoteNullableProject());
    }

    public synchronized void setInstancesDateTime(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull Set<InstanceKey> instanceKeys, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstancesDateTime");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(instanceKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<Instance> instances = Stream.of(instanceKeys)
                .map(this::getInstance)
                .collect(Collectors.toList());

        Stream.of(instances)
                .forEach(instance -> instance.setInstanceDateTime(instanceDate, instanceTimePair, now));

        Set<RemoteProject> remoteProjects = Stream.of(instances)
                .filter(Instance::belongsToRemoteProject)
                .map(Instance::getRemoteNonNullProject)
                .collect(Collectors.toSet());

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, remoteProjects);
    }

    public synchronized void setInstanceAddHourService(@NonNull Context context, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceAddHourService");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        instance.setInstanceDateTime(date, new TimePair(hourMinute), now);
        instance.setNotificationShown(false, now);

        updateNotifications(context, now);

        save(context, 0, source);

        notifyCloud(context, instance.getRemoteNullableProject());
    }

    public synchronized void setInstanceAddHourActivity(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceAddHourActivity");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        instance.setInstanceDateTime(date, new TimePair(hourMinute), now);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, instance.getRemoteNullableProject());
    }

    public synchronized void setInstancesAddHourActivity(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull Collection<InstanceKey> instanceKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceAddHourActivity");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        List<Instance> instances = Stream.of(instanceKeys)
                .map(this::getInstance)
                .collect(Collectors.toList());

        Stream.of(instances).forEach(instance -> instance.setInstanceDateTime(date, new TimePair(hourMinute), now));

        updateNotifications(context, now);

        save(context, dataId, source);

        @SuppressWarnings("Convert2MethodRef") Set<RemoteProject> remoteProjects = Stream.of(instances).map(Instance::getRemoteNullableProject)
                .filter(remoteProject -> remoteProject != null)
                .collect(Collectors.toSet());

        notifyCloud(context, remoteProjects);
    }

    public synchronized void setInstanceNotificationDone(@NonNull Context context, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceNotificationDone");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        instance.setDone(true, now);
        instance.setNotificationShown(false, now);

        updateNotifications(context, now);

        save(context, 0, source);

        notifyCloud(context, instance.getRemoteNullableProject());
    }

    @NonNull
    public synchronized ExactTimeStamp setInstancesDone(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull List<InstanceKey> instanceKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstancesDone");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<Instance> instances = Stream.of(instanceKeys)
                .map(this::getInstance)
                .collect(Collectors.toList());

        Stream.of(instances)
                .forEach(instance -> instance.setDone(true, now));

        Set<RemoteProject> remoteProjects = Stream.of(instances)
                .filter(Instance::belongsToRemoteProject)
                .map(Instance::getRemoteNonNullProject)
                .collect(Collectors.toSet());

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, remoteProjects);

        return now;
    }

    public synchronized ExactTimeStamp setInstanceDone(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey, boolean done) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceDone");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Instance instance = setInstanceDone(context, now, dataId, source, instanceKey, done);

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
            task = kotlinDomainFactory.localFactory.createScheduleRootTask(this, now, name, scheduleDatas, note);
        } else {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);

            task = kotlinDomainFactory.getRemoteProjectFactory().createScheduleRootTask(now, name, scheduleDatas, note, projectId);
        }

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, task.getRemoteNullableProject());

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

        Task task = getTaskForce(taskKey);
        check(task.current(now));

        task = task.updateProject(context, now, projectId);

        task.setName(name, note);

        if (!task.isRootTask(now)) {
            TaskHierarchy taskHierarchy = getParentTaskHierarchy(task, now);
            check(taskHierarchy != null);

            taskHierarchy.setEndExactTimeStamp(now);
        }

        task.updateSchedules(scheduleDatas, now);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, task.getRemoteNullableProject());

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

        List<Task> joinTasks = Stream.of(joinTaskKeys)
                .map(this::getTaskForce)
                .collect(Collectors.toList());

        Task newParentTask;
        if (!TextUtils.isEmpty(finalProjectId)) {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);
            check(kotlinDomainFactory.getUserInfo() != null);

            newParentTask = kotlinDomainFactory.getRemoteProjectFactory().createScheduleRootTask(now, name, scheduleDatas, note, finalProjectId);
        } else {
            newParentTask = kotlinDomainFactory.localFactory.createScheduleRootTask(this, now, name, scheduleDatas, note);
        }

        joinTasks = Stream.of(joinTasks)
                .map(joinTask -> joinTask.updateProject(context, now, projectId))
                .collect(Collectors.toList());

        joinTasks(newParentTask, joinTasks, now);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, newParentTask.getRemoteNullableProject());
    }

    Task createChildTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey parentTaskKey, @NonNull String name, @Nullable String note) {
        check(!TextUtils.isEmpty(name));

        Task parentTask = getTaskForce(parentTaskKey);
        check(parentTask.current(now));

        Task childTask = parentTask.createChildTask(now, name, note);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, childTask.getRemoteNullableProject());

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

        Task parentTask = getTaskForce(parentTaskKey);
        check(parentTask.current(now));

        List<String> joinProjectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                .distinct()
                .collect(Collectors.toList());
        check(joinProjectIds.size() == 1);

        List<Task> joinTasks = Stream.of(joinTaskKeys)
                .map(this::getTaskForce)
                .collect(Collectors.toList());

        Task childTask = parentTask.createChildTask(now, name, note);

        joinTasks(childTask, joinTasks, now);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, childTask.getRemoteNullableProject());
    }

    @NonNull
    public synchronized TaskKey updateChildTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @NonNull TaskKey parentTaskKey, @Nullable String note) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateChildTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));

        Task task = getTaskForce(taskKey);
        check(task.current(now));

        Task newParentTask = getTaskForce(parentTaskKey);
        check(task.current(now));

        task.setName(name, note);

        Task oldParentTask = task.getParentTask(now);
        if (oldParentTask == null) {
            Stream.of(task.getCurrentSchedules(now))
                    .forEach(schedule -> schedule.setEndExactTimeStamp(now));

            newParentTask.addChild(task, now);
        } else if (oldParentTask != newParentTask) {
            TaskHierarchy oldTaskHierarchy = getParentTaskHierarchy(task, now);
            check(oldTaskHierarchy != null);

            oldTaskHierarchy.setEndExactTimeStamp(now);

            newParentTask.addChild(task, now);
        }

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    public synchronized void setTaskEndTimeStamp(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setTaskEndTimeStamp");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task task = getTaskForce(taskKey);
        check(task.current(now));

        task.setEndExactTimeStamp(now);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, task.getRemoteNullableProject());
    }

    public synchronized void setInstanceOrdinal(int dataId, @NonNull InstanceKey instanceKey, double ordinal) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceOrdinal");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Instance instance = getInstance(instanceKey);

        instance.setOrdinal(ordinal, now);

        updateNotifications(MyApplication.Companion.getInstance(), now);

        save(MyApplication.Companion.getInstance(), dataId, SaveService.Source.GUI);

        notifyCloud(MyApplication.Companion.getInstance(), instance.getRemoteNullableProject());
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

        updateNotifications(MyApplication.Companion.getInstance(), now);

        save(MyApplication.Companion.getInstance(), dataId, SaveService.Source.GUI);

        if (remoteProject != null)
            notifyCloud(MyApplication.Companion.getInstance(), remoteProject);
    }

    public synchronized void setTaskEndTimeStamps(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull ArrayList<TaskKey> taskKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setTaskEndTimeStamps");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!taskKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<Task> tasks = Stream.of(taskKeys)
                .map(this::getTaskForce)
                .collect(Collectors.toList());

        check(Stream.of(tasks)
                .allMatch(task -> task.current(now)));

        Stream.of(tasks)
                .forEach(task -> task.setEndExactTimeStamp(now));

        Set<RemoteProject> remoteProjects = Stream.of(tasks)
                .filter(Task::belongsToRemoteProject)
                .map(Task::getRemoteNonNullProject)
                .collect(Collectors.toSet());

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, remoteProjects);
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

        LocalCustomTime localCustomTime = kotlinDomainFactory.localFactory.createLocalCustomTime(this, name, hourMinutes);

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
            task = kotlinDomainFactory.localFactory.createLocalTaskHelper(this, name, now, note);
        } else {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);

            task = kotlinDomainFactory.getRemoteProjectFactory().createRemoteTaskHelper(now, name, note, projectId);
        }

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, task.getRemoteNullableProject());

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

        List<Task> joinTasks = Stream.of(joinTaskKeys)
                .map(this::getTaskForce)
                .collect(Collectors.toList());

        Task newParentTask;
        if (!TextUtils.isEmpty(finalProjectId)) {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);
            check(kotlinDomainFactory.getUserInfo() != null);

            newParentTask = kotlinDomainFactory.getRemoteProjectFactory().createRemoteTaskHelper(now, name, note, finalProjectId);
        } else {
            newParentTask = kotlinDomainFactory.localFactory.createLocalTaskHelper(this, name, now, note);
        }

        joinTasks = Stream.of(joinTasks)
                .map(joinTask -> joinTask.updateProject(context, now, projectId))
                .collect(Collectors.toList());

        joinTasks(newParentTask, joinTasks, now);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, newParentTask.getRemoteNullableProject());
    }

    @NonNull
    public synchronized TaskKey updateRootTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateRootTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task task = getTaskForce(taskKey);
        check(task.current(now));

        task = task.updateProject(context, now, projectId);

        task.setName(name, note);

        TaskHierarchy taskHierarchy = getParentTaskHierarchy(task, now);
        if (taskHierarchy != null)
            taskHierarchy.setEndExactTimeStamp(now);

        Stream.of(task.getCurrentSchedules(now))
                .forEach(schedule -> schedule.setEndExactTimeStamp(now));

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    @NonNull
    Irrelevant updateNotificationsTick(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull SaveService.Source source, boolean silent) {
        updateNotifications(context, silent, now, new ArrayList<>());

        Irrelevant irrelevant = setIrrelevant(now);

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

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, remoteProject, removedFriends);
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

        notifyCloud(context, remoteProject);
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

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, remoteProjects);
    }

    // internal

    @Nullable
    private Instance getExistingInstanceIfPresent(@NonNull TaskKey taskKey, @NonNull DateTime scheduleDateTime) {
        InstanceKey instanceKey = new InstanceKey(taskKey, scheduleDateTime.getDate(), scheduleDateTime.getTime().getTimePair());

        return getExistingInstanceIfPresent(instanceKey);
    }

    @Nullable
    private Instance getExistingInstanceIfPresent(@NonNull InstanceKey instanceKey) {
        if (instanceKey.getTaskKey().getLocalTaskId() != null) {
            check(TextUtils.isEmpty(instanceKey.getTaskKey().getRemoteProjectId()));
            check(TextUtils.isEmpty(instanceKey.getTaskKey().getRemoteTaskId()));

            return kotlinDomainFactory.localFactory.getExistingInstanceIfPresent(instanceKey);
        } else {
            check(!TextUtils.isEmpty(instanceKey.getTaskKey().getRemoteProjectId()));
            check(!TextUtils.isEmpty(instanceKey.getTaskKey().getRemoteTaskId()));
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);

            return kotlinDomainFactory.getRemoteProjectFactory().getExistingInstanceIfPresent(instanceKey);
        }
    }

    @NonNull
    public String getRemoteCustomTimeId(@NonNull String projectId, @NonNull CustomTimeKey customTimeKey) {
        if (!TextUtils.isEmpty(customTimeKey.getRemoteProjectId())) {
            check(!TextUtils.isEmpty(customTimeKey.getRemoteCustomTimeId()));
            check(customTimeKey.getLocalCustomTimeId() == null);

            check(customTimeKey.getRemoteProjectId().equals(projectId));

            return customTimeKey.getRemoteCustomTimeId();
        } else {
            check(TextUtils.isEmpty(customTimeKey.getRemoteCustomTimeId()));
            check(customTimeKey.getLocalCustomTimeId() != null);

            LocalCustomTime localCustomTime = kotlinDomainFactory.localFactory.getLocalCustomTime(customTimeKey.getLocalCustomTimeId());

            check(localCustomTime.hasRemoteRecord(projectId));

            return localCustomTime.getRemoteId(projectId);
        }
    }

    @NonNull
    private Instance generateInstance(@NonNull TaskKey taskKey, @NonNull DateTime scheduleDateTime) {
        if (taskKey.getLocalTaskId() != null) {
            check(TextUtils.isEmpty(taskKey.getRemoteProjectId()));
            check(TextUtils.isEmpty(taskKey.getRemoteTaskId()));

            return new LocalInstance(this, taskKey.getLocalTaskId(), scheduleDateTime);
        } else {
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);
            check(!TextUtils.isEmpty(taskKey.getRemoteProjectId()));
            check(!TextUtils.isEmpty(taskKey.getRemoteTaskId()));

            String remoteCustomTimeId;
            Integer hour;
            Integer minute;

            CustomTimeKey customTimeKey = scheduleDateTime.getTime().getTimePair().getCustomTimeKey();
            HourMinute hourMinute = scheduleDateTime.getTime().getTimePair().getHourMinute();

            if (customTimeKey != null) {
                check(hourMinute == null);

                remoteCustomTimeId = getRemoteCustomTimeId(taskKey.getRemoteProjectId(), customTimeKey);

                hour = null;
                minute = null;
            } else {
                check(hourMinute != null);

                remoteCustomTimeId = null;

                hour = hourMinute.getHour();
                minute = hourMinute.getMinute();
            }

            InstanceShownRecord instanceShownRecord = kotlinDomainFactory.localFactory.getInstanceShownRecord(taskKey.getRemoteProjectId(), taskKey.getRemoteTaskId(), scheduleDateTime.getDate().getYear(), scheduleDateTime.getDate().getMonth(), scheduleDateTime.getDate().getDay(), remoteCustomTimeId, hour, minute);

            RemoteProject remoteProject = kotlinDomainFactory.getRemoteProjectFactory().getTaskForce(taskKey).getRemoteProject();

            return new RemoteInstance(this, remoteProject, taskKey.getRemoteTaskId(), scheduleDateTime, instanceShownRecord);
        }
    }

    @NonNull
    public Instance getInstance(@NonNull TaskKey taskKey, @NonNull DateTime scheduleDateTime) {
        Instance existingInstance = getExistingInstanceIfPresent(taskKey, scheduleDateTime);

        if (existingInstance != null) {
            return existingInstance;
        } else {
            return generateInstance(taskKey, scheduleDateTime);
        }
    }

    @NonNull
    public Instance getInstance(@NonNull InstanceKey instanceKey) {
        Instance instance = getExistingInstanceIfPresent(instanceKey);
        if (instance != null)
            return instance;

        DateTime dateTime = getDateTime(instanceKey.getScheduleKey().getScheduleDate(), instanceKey.getScheduleKey().getScheduleTimePair());

        return generateInstance(instanceKey.getTaskKey(), dateTime); // DateTime -> timePair
    }

    @NonNull
    public List<Instance> getPastInstances(@NonNull Task task, @NonNull ExactTimeStamp now) {
        Map<InstanceKey, Instance> allInstances = new HashMap<>();

        allInstances.putAll(Stream.of(task.getExistingInstances().values())
                .filter(instance -> instance.getScheduleDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) <= 0)
                .collect(Collectors.toMap(Instance::getInstanceKey, instance -> instance)));

        allInstances.putAll(Stream.of(task.getInstances(null, now.plusOne(), now))
                .collect(Collectors.toMap(Instance::getInstanceKey, instance -> instance)));

        return new ArrayList<>(allInstances.values());
    }

    @NonNull
    private List<Instance> getRootInstances(@Nullable ExactTimeStamp startExactTimeStamp, @NonNull ExactTimeStamp endExactTimeStamp, @NonNull ExactTimeStamp now) {
        check(startExactTimeStamp == null || startExactTimeStamp.compareTo(endExactTimeStamp) < 0);

        Map<InstanceKey, Instance> allInstances = new HashMap<>();

        for (Instance instance : getExistingInstances()) {
            ExactTimeStamp instanceExactTimeStamp = instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp();

            if (startExactTimeStamp != null && startExactTimeStamp.compareTo(instanceExactTimeStamp) > 0)
                continue;

            if (endExactTimeStamp.compareTo(instanceExactTimeStamp) <= 0)
                continue;

            allInstances.put(instance.getInstanceKey(), instance);
        }

        getTasks().forEach(task -> {
            for (Instance instance : task.getInstances(startExactTimeStamp, endExactTimeStamp, now)) {
                ExactTimeStamp instanceExactTimeStamp = instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp();

                if (startExactTimeStamp != null && startExactTimeStamp.compareTo(instanceExactTimeStamp) > 0)
                    continue;

                if (endExactTimeStamp.compareTo(instanceExactTimeStamp) <= 0)
                    continue;

                allInstances.put(instance.getInstanceKey(), instance);
            }
        });

        return Stream.of(allInstances.values())
                .filter(instance -> instance.isRootInstance(now))
                .filter(instance -> instance.isVisible(now))
                .collect(Collectors.toList());
    }

    @NonNull
    public Time getTime(@NonNull TimePair timePair) {
        if (timePair.getHourMinute() != null) {
            check(timePair.getCustomTimeKey() == null);

            return new NormalTime(timePair.getHourMinute());
        } else {
            check(timePair.getCustomTimeKey() != null);

            return getCustomTime(timePair.getCustomTimeKey());
        }
    }

    @NonNull
    private DateTime getDateTime(@NonNull Date date, @NonNull TimePair timePair) {
        return new DateTime(date, getTime(timePair));
    }

    @Nullable
    Task getParentTask(@NonNull Task childTask, @NonNull ExactTimeStamp exactTimeStamp) {
        check(childTask.notDeleted(exactTimeStamp));

        TaskHierarchy parentTaskHierarchy = getParentTaskHierarchy(childTask, exactTimeStamp);
        if (parentTaskHierarchy == null) {
            return null;
        } else {
            check(parentTaskHierarchy.notDeleted(exactTimeStamp));

            Task parentTask = parentTaskHierarchy.getParentTask();
            check(parentTask.notDeleted(exactTimeStamp));

            return parentTask;
        }
    }

    @NonNull
    public CustomTime getCustomTime(@NonNull CustomTimeKey customTimeKey) {
        if (customTimeKey.getLocalCustomTimeId() != null) {
            check(TextUtils.isEmpty(customTimeKey.getRemoteProjectId()));
            check(TextUtils.isEmpty(customTimeKey.getRemoteCustomTimeId()));

            return kotlinDomainFactory.localFactory.getLocalCustomTime(customTimeKey.getLocalCustomTimeId());
        } else {
            check(!TextUtils.isEmpty(customTimeKey.getRemoteProjectId()));
            check(!TextUtils.isEmpty(customTimeKey.getRemoteCustomTimeId()));
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);

            return kotlinDomainFactory.getRemoteProjectFactory().getRemoteCustomTime(customTimeKey.getRemoteProjectId(), customTimeKey.getRemoteCustomTimeId());
        }
    }

    @NonNull
    private List<LocalCustomTime> getCurrentCustomTimes() {
        return kotlinDomainFactory.localFactory.getCurrentCustomTimes();
    }

    @NonNull
    private HashMap<InstanceKey, GroupListFragment.InstanceData> getChildInstanceDatas(@NonNull Instance instance, @NonNull ExactTimeStamp now) {
        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();

        for (kotlin.Pair<Instance, TaskHierarchy> pair : instance.getChildInstances(now)) {
            Instance childInstance = pair.getFirst();
            TaskHierarchy taskHierarchy = pair.getSecond();

            Task childTask = childInstance.getTask();

            Boolean isRootTask = (childTask.current(now) ? childTask.isRootTask(now) : null);

            HashMap<InstanceKey, GroupListFragment.InstanceData> children = getChildInstanceDatas(childInstance, now);
            GroupListFragment.InstanceData instanceData = new GroupListFragment.InstanceData(childInstance.getDone(), childInstance.getInstanceKey(), null, childInstance.getName(), childInstance.getInstanceDateTime().getTimeStamp(), childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), childInstance.getInstanceDateTime().getTime().getTimePair(), childTask.getNote(), children, new HierarchyData(taskHierarchy.getTaskHierarchyKey(), taskHierarchy.getOrdinal()), childInstance.getOrdinal());
            Stream.of(children.values()).forEach(child -> child.setInstanceDataParent(instanceData));
            instanceDatas.put(childInstance.getInstanceKey(), instanceData);
        }

        return instanceDatas;
    }

    @NonNull
    private Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> getChildTaskDatas(@NonNull ExactTimeStamp now, @NonNull Task parentTask, @NonNull Context context, @NonNull List<TaskKey> excludedTaskKeys) {
        return Stream.of(parentTask.getChildTaskHierarchies(now)).filterNot(taskHierarchy -> excludedTaskKeys.contains(taskHierarchy.getChildTaskKey())).collect(Collectors.toMap(taskHierarchy -> new CreateTaskViewModel.ParentKey.TaskParentKey(taskHierarchy.getChildTaskKey()), taskHierarchy -> {
                    Task childTask = taskHierarchy.getChildTask();

            return new CreateTaskViewModel.ParentTreeData(childTask.getName(), getChildTaskDatas(now, childTask, context, excludedTaskKeys), new CreateTaskViewModel.ParentKey.TaskParentKey(childTask.getTaskKey()), childTask.getScheduleText(context, now), childTask.getNote(), new CreateTaskViewModel.SortKey.TaskSortKey(childTask.getStartExactTimeStamp()));
                }));
    }

    @NonNull
    private Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> getParentTreeDatas(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull List<TaskKey> excludedTaskKeys) {
        Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> parentTreeDatas = new HashMap<>();

        parentTreeDatas.putAll(Stream.of(kotlinDomainFactory.localFactory.getTasks())
                .filterNot(task -> excludedTaskKeys.contains(task.getTaskKey()))
                .filter(task -> task.current(now))
                .filter(task -> task.isVisible(now)).filter(task -> task.isRootTask(now)).collect(Collectors.toMap(task -> new CreateTaskViewModel.ParentKey.TaskParentKey(task.getTaskKey()), task -> new CreateTaskViewModel.ParentTreeData(task.getName(), getChildTaskDatas(now, task, context, excludedTaskKeys), new CreateTaskViewModel.ParentKey.TaskParentKey(task.getTaskKey()), task.getScheduleText(context, now), task.getNote(), new CreateTaskViewModel.SortKey.TaskSortKey(task.getStartExactTimeStamp())))));

        if (kotlinDomainFactory.getRemoteProjectFactory() != null) {
            parentTreeDatas.putAll(Stream.of(kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjects().values()).filter(remoteProject -> remoteProject.current(now)).collect(Collectors.toMap(remoteProject -> new CreateTaskViewModel.ParentKey.ProjectParentKey(remoteProject.getId()), remoteProject -> {
                        String users = Stream.of(remoteProject.getUsers())
                                .map(RemoteProjectUser::getName)
                                .collect(Collectors.joining(", "));

                return new CreateTaskViewModel.ParentTreeData(remoteProject.getName(), getProjectTaskTreeDatas(context, now, remoteProject, excludedTaskKeys), new CreateTaskViewModel.ParentKey.ProjectParentKey(remoteProject.getId()), users, null, new CreateTaskViewModel.SortKey.ProjectSortKey(remoteProject.getId()));
                    })));
        }

        return parentTreeDatas;
    }

    @NonNull
    private Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData> getProjectTaskTreeDatas(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull RemoteProject remoteProject, @NonNull List<TaskKey> excludedTaskKeys) {
        return Stream.of(remoteProject.getTasks())
                .filterNot(task -> excludedTaskKeys.contains(task.getTaskKey()))
                .filter(task -> task.current(now))
                .filter(task -> task.isVisible(now)).filter(task -> task.isRootTask(now)).collect(Collectors.toMap(task -> new CreateTaskViewModel.ParentKey.TaskParentKey(task.getTaskKey()), task -> new CreateTaskViewModel.ParentTreeData(task.getName(), getChildTaskDatas(now, task, context, excludedTaskKeys), new CreateTaskViewModel.ParentKey.TaskParentKey(task.getTaskKey()), task.getScheduleText(context, now), task.getNote(), new CreateTaskViewModel.SortKey.TaskSortKey(task.getStartExactTimeStamp()))));
    }

    @NonNull
    public RemoteTask convertLocalToRemote(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull LocalTask startingLocalTask, @NonNull String projectId) {
        check(!TextUtils.isEmpty(projectId));

        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(kotlinDomainFactory.getUserInfo() != null);

        LocalToRemoteConversion localToRemoteConversion = new LocalToRemoteConversion();
        kotlinDomainFactory.localFactory.convertLocalToRemoteHelper(localToRemoteConversion, startingLocalTask);

        updateNotifications(context, true, now, Stream.of(localToRemoteConversion.mLocalTasks.values())
                .map(pair -> pair.getFirst().getTaskKey())
                .collect(Collectors.toList()));

        RemoteProject remoteProject = kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjectForce(projectId);

        for (kotlin.Pair<LocalTask, List<LocalInstance>> pair : localToRemoteConversion.mLocalTasks.values()) {
            check(pair != null);

            RemoteTask remoteTask = remoteProject.copyLocalTask(pair.getFirst(), pair.getSecond(), now);
            localToRemoteConversion.mRemoteTasks.put(pair.getFirst().getId(), remoteTask);
        }

        for (LocalTaskHierarchy localTaskHierarchy : localToRemoteConversion.mLocalTaskHierarchies) {
            check(localTaskHierarchy != null);

            RemoteTask parentRemoteTask = localToRemoteConversion.mRemoteTasks.get(localTaskHierarchy.getParentTaskId());
            check(parentRemoteTask != null);

            RemoteTask childRemoteTask = localToRemoteConversion.mRemoteTasks.get(localTaskHierarchy.getChildTaskId());
            check(childRemoteTask != null);

            RemoteTaskHierarchy remoteTaskHierarchy = remoteProject.copyLocalTaskHierarchy(localTaskHierarchy, parentRemoteTask.getId(), childRemoteTask.getId());
            localToRemoteConversion.mRemoteTaskHierarchies.add(remoteTaskHierarchy);
        }

        for (kotlin.Pair<LocalTask, List<LocalInstance>> pair : localToRemoteConversion.mLocalTasks.values()) {
            Stream.of(pair.getSecond())
                    .forEach(LocalInstance::delete);

            pair.getFirst().delete();
        }

        RemoteTask remoteTask = localToRemoteConversion.mRemoteTasks.get(startingLocalTask.getId());
        check(remoteTask != null);

        return remoteTask;
    }

    private void joinTasks(@NonNull Task newParentTask, @NonNull List<Task> joinTasks, @NonNull ExactTimeStamp now) {
        check(newParentTask.current(now));
        check(joinTasks.size() > 1);

        for (Task joinTask : joinTasks) {
            check(joinTask != null);
            check(joinTask.current(now));

            if (joinTask.isRootTask(now)) {
                Stream.of(joinTask.getCurrentSchedules(now))
                        .forEach(schedule -> schedule.setEndExactTimeStamp(now));
            } else {
                TaskHierarchy taskHierarchy = getParentTaskHierarchy(joinTask, now);
                check(taskHierarchy != null);

                taskHierarchy.setEndExactTimeStamp(now);
            }

            newParentTask.addChild(joinTask, now);
        }
    }

    @Nullable
    TaskHierarchy getParentTaskHierarchy(@NonNull Task childTask, @NonNull ExactTimeStamp exactTimeStamp) {
        if (childTask.current(exactTimeStamp)) {
            check(childTask.notDeleted(exactTimeStamp));

            TaskKey childTaskKey = childTask.getTaskKey();

            List<TaskHierarchy> taskHierarchies = Stream.of(childTask.getTaskHierarchiesByChildTaskKey(childTaskKey))
                    .filter(taskHierarchy -> taskHierarchy.current(exactTimeStamp))
                    .collect(Collectors.toList());

            if (taskHierarchies.isEmpty()) {
                return null;
            } else {
                check(taskHierarchies.size() == 1);
                return taskHierarchies.get(0);
            }
        } else {
            // jeśli child task jeszcze nie istnieje, ale będzie utworzony jako child, zwróć ów przyszły hierarchy
            // żeby można było dodawać child instances do past parent instance

            check(childTask.notDeleted(exactTimeStamp));

            TaskKey childTaskKey = childTask.getTaskKey();

            List<TaskHierarchy> taskHierarchies = Stream.of(childTask.getTaskHierarchiesByChildTaskKey(childTaskKey))
                    .filter(taskHierarchy -> taskHierarchy.getStartExactTimeStamp().equals(childTask.getStartExactTimeStamp()))
                    .collect(Collectors.toList());

            if (taskHierarchies.isEmpty()) {
                return null;
            } else {
                check(taskHierarchies.size() == 1);
                return taskHierarchies.get(0);
            }
        }
    }

    @NonNull
    private Stream<Task> getTasks() {
        if (kotlinDomainFactory.getRemoteProjectFactory() != null) {
            return Stream.concat(Stream.of(kotlinDomainFactory.localFactory.getTasks()), Stream.of(kotlinDomainFactory.getRemoteProjectFactory().getTasks()));
        } else {
            return Stream.of(kotlinDomainFactory.localFactory.getTasks());
        }
    }

    @NonNull
    private List<CustomTime> getCustomTimes() {
        List<CustomTime> customTimes = new ArrayList<>(kotlinDomainFactory.localFactory.getLocalCustomTimes());

        if (kotlinDomainFactory.getRemoteProjectFactory() != null)
            customTimes.addAll(kotlinDomainFactory.getRemoteProjectFactory().getRemoteCustomTimes());

        return customTimes;
    }

    @NonNull
    Task getTaskForce(@NonNull TaskKey taskKey) {
        if (taskKey.getLocalTaskId() != null) {
            check(TextUtils.isEmpty(taskKey.getRemoteTaskId()));

            return kotlinDomainFactory.localFactory.getTaskForce(taskKey.getLocalTaskId());
        } else {
            check(!TextUtils.isEmpty(taskKey.getRemoteTaskId()));
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);

            return kotlinDomainFactory.getRemoteProjectFactory().getTaskForce(taskKey);
        }
    }

    @Nullable
    private Task getTaskIfPresent(@NonNull TaskKey taskKey) {
        if (taskKey.getLocalTaskId() != null) {
            check(TextUtils.isEmpty(taskKey.getRemoteTaskId()));

            return kotlinDomainFactory.localFactory.getTaskIfPresent(taskKey.getLocalTaskId());
        } else {
            check(!TextUtils.isEmpty(taskKey.getRemoteTaskId()));
            check(kotlinDomainFactory.getRemoteProjectFactory() != null);

            return kotlinDomainFactory.getRemoteProjectFactory().getTaskIfPresent(taskKey);
        }
    }

    @NonNull
    List<TaskHierarchy> getChildTaskHierarchies(@NonNull Task parentTask, @NonNull ExactTimeStamp exactTimeStamp) {
        check(parentTask.current(exactTimeStamp));

        return Stream.of(parentTask.getTaskHierarchiesByParentTaskKey(parentTask.getTaskKey()))
                .filter(taskHierarchy -> taskHierarchy.current(exactTimeStamp) && taskHierarchy.getChildTask().current(exactTimeStamp))
                .sortBy(TaskHierarchy::getOrdinal)
                .collect(Collectors.toList());
    }

    @NonNull
    private List<TaskListFragment.ChildTaskData> getChildTaskDatas(@NonNull Task parentTask, @NonNull ExactTimeStamp now, @NonNull Context context) {
        return Stream.of(parentTask.getChildTaskHierarchies(now))
                .sortBy(TaskHierarchy::getOrdinal)
                .map(taskHierarchy -> {
                    Task childTask = taskHierarchy.getChildTask();

                    return new TaskListFragment.ChildTaskData(childTask.getName(), childTask.getScheduleText(context, now), getChildTaskDatas(childTask, now, context), childTask.getNote(), childTask.getStartExactTimeStamp(), childTask.getTaskKey(), new HierarchyData(taskHierarchy.getTaskHierarchyKey(), taskHierarchy.getOrdinal()));
                })
                .collect(Collectors.toList());
    }

    @Nullable
    public RemoteProjectFactory getRemoteFactory() {
        return kotlinDomainFactory.getRemoteProjectFactory();
    }

    @NonNull
    public LocalFactory getLocalFactory() {
        return kotlinDomainFactory.localFactory;
    }

    @NonNull
    private List<Instance> getExistingInstances() {
        List<Instance> instances = new ArrayList<>(kotlinDomainFactory.localFactory.getExistingInstances());

        if (kotlinDomainFactory.getRemoteProjectFactory() != null)
            instances.addAll(kotlinDomainFactory.getRemoteProjectFactory().getExistingInstances());

        return instances;
    }

    @NonNull
    private List<GroupListFragment.TaskData> getChildTaskDatas(@NonNull Task parentTask, @NonNull ExactTimeStamp now) {
        return Stream.of(parentTask.getChildTaskHierarchies(now))
                .map(taskHierarchy -> {
                    Task childTask = taskHierarchy.getChildTask();

                    return new GroupListFragment.TaskData(childTask.getTaskKey(), childTask.getName(), getChildTaskDatas(childTask, now), childTask.getStartExactTimeStamp(), childTask.getNote());
                })
                .collect(Collectors.toList());
    }

    @NonNull
    TaskListFragment.TaskData getMainData(@NonNull ExactTimeStamp now, @NonNull Context context) {
        List<TaskListFragment.ChildTaskData> childTaskDatas;

        childTaskDatas = getTasks()
                .filter(task -> task.current(now))
                .filter(task -> task.isVisible(now))
                .filter(task -> task.isRootTask(now))
                .map(task -> new TaskListFragment.ChildTaskData(task.getName(), task.getScheduleText(context, now), getChildTaskDatas(task, now, context), task.getNote(), task.getStartExactTimeStamp(), task.getTaskKey(), null))
                .collect(Collectors.toList());

        Collections.sort(childTaskDatas, (TaskListFragment.ChildTaskData lhs, TaskListFragment.ChildTaskData rhs) -> -lhs.compareTo(rhs));

        return new TaskListFragment.TaskData(childTaskDatas, null);
    }

    @NonNull
    Instance setInstanceDone(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey, boolean done) {
        Instance instance = getInstance(instanceKey);

        instance.setDone(done, now);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, instance.getRemoteNullableProject());

        return instance;
    }

    @NonNull
    Irrelevant setIrrelevant(@NonNull ExactTimeStamp now) {
        List<Task> tasks = getTasks().collect(Collectors.toList());

        for (Task task : tasks)
            task.updateOldestVisible(now);

        // relevant hack
        Map<TaskKey, TaskRelevance> taskRelevances = Stream.of(tasks).collect(Collectors.toMap(Task::getTaskKey, task -> new TaskRelevance(this, task)));

        List<Instance> existingInstances = getExistingInstances();
        List<Instance> rootInstances = getRootInstances(null, now.plusOne(), now);

        Map<InstanceKey, InstanceRelevance> instanceRelevances = Stream.concat(Stream.of(existingInstances), Stream.of(rootInstances))
                .distinct()
                .collect(Collectors.toMap(Instance::getInstanceKey, InstanceRelevance::new));

        Map<Integer, LocalCustomTimeRelevance> localCustomTimeRelevances = Stream.of(kotlinDomainFactory.localFactory.getLocalCustomTimes()).collect(Collectors.toMap(LocalCustomTime::getId, LocalCustomTimeRelevance::new));

        Stream.of(tasks)
                .filter(task -> task.current(now))
                .filter(task -> task.isRootTask(now))
                .filter(task -> task.isVisible(now))
                .map(Task::getTaskKey)
                .map(taskRelevances::get)
                .forEach(taskRelevance -> taskRelevance.setRelevant(taskRelevances, instanceRelevances, localCustomTimeRelevances, now));

        Stream.of(rootInstances)
                .map(Instance::getInstanceKey)
                .map(instanceRelevances::get)
                .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, localCustomTimeRelevances, now));

        Stream.of(existingInstances)
                .filter(instance -> instance.isRootInstance(now))
                .filter(instance -> instance.isVisible(now))
                .map(Instance::getInstanceKey)
                .map(instanceRelevances::get)
                .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, localCustomTimeRelevances, now));

        Stream.of(getCurrentCustomTimes())
                .map(LocalCustomTime::getId)
                .map(localCustomTimeRelevances::get)
                .forEach(LocalCustomTimeRelevance::setRelevant);

        List<Task> relevantTasks = Stream.of(taskRelevances.values())
                .filter(TaskRelevance::getRelevant)
                .map(TaskRelevance::getTask)
                .collect(Collectors.toList());

        List<Task> irrelevantTasks = new ArrayList<>(tasks);
        irrelevantTasks.removeAll(relevantTasks);

        check(Stream.of(irrelevantTasks)
                .noneMatch(task -> task.isVisible(now)));

        List<Instance> relevantExistingInstances = Stream.of(instanceRelevances.values())
                .filter(InstanceRelevance::getRelevant)
                .map(InstanceRelevance::getInstance)
                .filter(Instance::exists)
                .collect(Collectors.toList());

        List<Instance> irrelevantExistingInstances = new ArrayList<>(existingInstances);
        irrelevantExistingInstances.removeAll(relevantExistingInstances);

        check(Stream.of(irrelevantExistingInstances)
                .noneMatch(instance -> instance.isVisible(now)));

        List<LocalCustomTime> relevantLocalCustomTimes = Stream.of(localCustomTimeRelevances.values())
                .filter(LocalCustomTimeRelevance::getRelevant)
                .map(LocalCustomTimeRelevance::getLocalCustomTime)
                .collect(Collectors.toList());

        List<LocalCustomTime> irrelevantLocalCustomTimes = new ArrayList<>(kotlinDomainFactory.localFactory.getLocalCustomTimes());
        irrelevantLocalCustomTimes.removeAll(relevantLocalCustomTimes);

        check(Stream.of(irrelevantLocalCustomTimes)
                .noneMatch(LocalCustomTime::getCurrent));

        Stream.of(irrelevantExistingInstances)
                .forEach(Instance::delete);

        Stream.of(irrelevantTasks)
                .forEach(Task::delete);

        Stream.of(irrelevantLocalCustomTimes)
                .forEach(LocalCustomTime::delete);

        List<RemoteCustomTime> irrelevantRemoteCustomTimes;
        List<RemoteProject> irrelevantRemoteProjects;
        if (kotlinDomainFactory.getRemoteProjectFactory() != null) {
            List<RemoteCustomTime> remoteCustomTimes = kotlinDomainFactory.getRemoteProjectFactory().getRemoteCustomTimes();
            Map<kotlin.Pair<String, String>, RemoteCustomTimeRelevance> remoteCustomTimeRelevances = Stream.of(remoteCustomTimes).collect(Collectors.toMap(remoteCustomTime -> new kotlin.Pair<>(remoteCustomTime.getProjectId(), remoteCustomTime.getId()), RemoteCustomTimeRelevance::new));

            Collection<RemoteProject> remoteProjects = kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjects().values();
            Map<String, RemoteProjectRelevance> remoteProjectRelevances = Stream.of(remoteProjects)
                    .collect(Collectors.toMap(RemoteProject::getId, RemoteProjectRelevance::new));

            Stream.of(remoteProjects)
                    .filter(remoteProject -> remoteProject.current(now))
                    .map(RemoteProject::getId)
                    .map(remoteProjectRelevances::get)
                    .forEach(RemoteProjectRelevance::setRelevant);

            Stream.of(taskRelevances.values())
                    .filter(TaskRelevance::getRelevant)
                    .forEach(taskRelevance -> taskRelevance.setRemoteRelevant(remoteCustomTimeRelevances, remoteProjectRelevances));

            Stream.of(instanceRelevances.values())
                    .filter(InstanceRelevance::getRelevant)
                    .forEach(instanceRelevance -> instanceRelevance.setRemoteRelevant(remoteCustomTimeRelevances, remoteProjectRelevances));

            List<RemoteCustomTime> relevantRemoteCustomTimes = Stream.of(remoteCustomTimeRelevances.values())
                    .filter(RemoteCustomTimeRelevance::getRelevant)
                    .map(RemoteCustomTimeRelevance::getRemoteCustomTime)
                    .collect(Collectors.toList());

            irrelevantRemoteCustomTimes = new ArrayList<>(remoteCustomTimes);
            irrelevantRemoteCustomTimes.removeAll(relevantRemoteCustomTimes);

            Stream.of(irrelevantRemoteCustomTimes)
                    .forEach(RemoteCustomTime::delete);

            List<RemoteProject> relevantRemoteProjects = Stream.of(remoteProjectRelevances.values())
                    .filter(RemoteProjectRelevance::getRelevant)
                    .map(RemoteProjectRelevance::getRemoteProject)
                    .collect(Collectors.toList());

            irrelevantRemoteProjects = new ArrayList<>(remoteProjects);
            irrelevantRemoteProjects.removeAll(relevantRemoteProjects);

            Stream.of(irrelevantRemoteProjects)
                    .forEach(RemoteProject::delete);
        } else {
            irrelevantRemoteCustomTimes = null;
            irrelevantRemoteProjects = null;
        }

        return new Irrelevant(irrelevantLocalCustomTimes, irrelevantTasks, irrelevantExistingInstances, irrelevantRemoteCustomTimes, irrelevantRemoteProjects);
    }

    private void notifyCloud(@NonNull Context context, @Nullable RemoteProject remoteProject) {
        Set<RemoteProject> remoteProjects = new HashSet<>();
        if (remoteProject != null)
            remoteProjects.add(remoteProject);

        notifyCloud(context, remoteProjects);
    }

    private void notifyCloud(@NonNull Context context, @NonNull Set<RemoteProject> remoteProjects) {
        if (!remoteProjects.isEmpty()) {
            check(kotlinDomainFactory.getUserInfo() != null);

            BackendNotifier.INSTANCE.notify(context, remoteProjects, kotlinDomainFactory.getUserInfo(), new ArrayList<>());
        }
    }

    private void notifyCloud(@NonNull Context context, @NonNull RemoteProject remoteProject, @NonNull Collection<String> userKeys) {
        check(kotlinDomainFactory.getUserInfo() != null);

        Set<RemoteProject> remoteProjects = Collections.singleton(remoteProject);

        BackendNotifier.INSTANCE.notify(context, remoteProjects, kotlinDomainFactory.getUserInfo(), userKeys);
    }

    private void updateNotifications(@NonNull Context context, @NonNull ExactTimeStamp now) {
        updateNotifications(context, true, now, new ArrayList<>());
    }

    @NonNull
    private Set<TaskKey> getTaskKeys() {
        HashSet<TaskKey> taskKeys = new HashSet<>(Stream.of(kotlinDomainFactory.localFactory.getTaskIds()).map(TaskKey::new).collect(Collectors.toList()));

        if (kotlinDomainFactory.getRemoteProjectFactory() != null)
            taskKeys.addAll(kotlinDomainFactory.getRemoteProjectFactory().getTaskKeys());

        return taskKeys;
    }

    private void updateNotifications(@NonNull Context context, boolean silent, @NonNull ExactTimeStamp now, @NonNull List<TaskKey> removedTaskKeys) {
        List<Instance> rootInstances = getRootInstances(null, now.plusOne(), now); // 24 hack

        Map<InstanceKey, Instance> notificationInstances = Stream.of(rootInstances)
                .filter(instance -> (instance.getDone() == null) && !instance.getNotified() && instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) <= 0)
                .filterNot(instance -> removedTaskKeys.contains(instance.getTaskKey()))
                .collect(Collectors.toMap(Instance::getInstanceKey, instance -> instance));

        HashSet<InstanceKey> shownInstanceKeys = new HashSet<>(Stream.of(getExistingInstances())
                .filter(Instance::getNotificationShown)
                .map(Instance::getInstanceKey)
                .collect(Collectors.toSet()));

        Map<InstanceKey, kotlin.Pair<Integer, InstanceShownRecord>> instanceShownRecordNotificationDatas = new HashMap<>();
        for (InstanceShownRecord instanceShownRecord : kotlinDomainFactory.localFactory.getInstanceShownRecords()) {
            if (!instanceShownRecord.getNotificationShown())
                continue;

            Date scheduleDate = new Date(instanceShownRecord.getScheduleYear(), instanceShownRecord.getScheduleMonth(), instanceShownRecord.getScheduleDay());
            String remoteCustomTimeId = instanceShownRecord.getScheduleCustomTimeId();

            CustomTimeKey customTimeKey;
            HourMinute hourMinute;

            if (!TextUtils.isEmpty(remoteCustomTimeId)) {
                check(instanceShownRecord.getScheduleHour() == null);
                check(instanceShownRecord.getScheduleMinute() == null);

                //noinspection ConstantConditions
                customTimeKey = getCustomTimeKey(instanceShownRecord.getProjectId(), remoteCustomTimeId);
                hourMinute = null;
            } else {
                check(instanceShownRecord.getScheduleHour() != null);
                check(instanceShownRecord.getScheduleMinute() != null);

                customTimeKey = null;
                hourMinute = new HourMinute(instanceShownRecord.getScheduleHour(), instanceShownRecord.getScheduleMinute());
            }

            @SuppressWarnings("ConstantConditions") TaskKey taskKey = new TaskKey(instanceShownRecord.getProjectId(), instanceShownRecord.getTaskId());

            InstanceKey instanceKey = new InstanceKey(taskKey, scheduleDate, new TimePair(customTimeKey, hourMinute));

            shownInstanceKeys.add(instanceKey);

            instanceShownRecordNotificationDatas.put(instanceKey, new kotlin.Pair<>(Instance.Companion.getNotificationId(scheduleDate, customTimeKey, hourMinute, taskKey), instanceShownRecord));
        }

        List<InstanceKey> showInstanceKeys = Stream.of(notificationInstances.keySet())
                .filter(instanceKey -> !shownInstanceKeys.contains(instanceKey))
                .collect(Collectors.toList());

        Set<InstanceKey> hideInstanceKeys = Stream.of(shownInstanceKeys)
                .filter(instanceKey -> !notificationInstances.containsKey(instanceKey))
                .collect(Collectors.toSet());

        for (InstanceKey showInstanceKey : showInstanceKeys) {
            check(showInstanceKey != null);

            Instance showInstance = getInstance(showInstanceKey);

            showInstance.setNotificationShown(true, now);
        }

        Set<TaskKey> allTaskKeys = getTaskKeys();

        for (InstanceKey hideInstanceKey : hideInstanceKeys) {
            check(hideInstanceKey != null);

            if (allTaskKeys.contains(hideInstanceKey.getTaskKey())) {
                Instance hideInstance = getInstance(hideInstanceKey);

                hideInstance.setNotificationShown(false, now);
            } else {
                check(instanceShownRecordNotificationDatas.containsKey(hideInstanceKey));

                instanceShownRecordNotificationDatas.get(hideInstanceKey).getSecond().setNotificationShown(false);
            }
        }

        String message = "";

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (notificationInstances.size() > TickJobIntentService.MAX_NOTIFICATIONS) { // show group
                if (shownInstanceKeys.size() > TickJobIntentService.MAX_NOTIFICATIONS) { // group shown
                    if (!showInstanceKeys.isEmpty() || !hideInstanceKeys.isEmpty()) {
                        NotificationWrapper.Companion.getInstance().notifyGroup(this, notificationInstances.values(), silent, now);
                    } else {
                        NotificationWrapper.Companion.getInstance().notifyGroup(this, notificationInstances.values(), true, now);
                    }
                } else { // instances shown
                    for (InstanceKey shownInstanceKey : shownInstanceKeys) {
                        if (allTaskKeys.contains(shownInstanceKey.getTaskKey())) {
                            Instance shownInstance = getInstance(shownInstanceKey);

                            NotificationWrapper.Companion.getInstance().cancelNotification(shownInstance.getNotificationId());
                        } else {
                            check(instanceShownRecordNotificationDatas.containsKey(shownInstanceKey));

                            int notificationId = instanceShownRecordNotificationDatas.get(shownInstanceKey).getFirst();

                            NotificationWrapper.Companion.getInstance().cancelNotification(notificationId);
                        }
                    }

                    NotificationWrapper.Companion.getInstance().notifyGroup(this, notificationInstances.values(), silent, now);
                }
            } else { // show instances
                if (shownInstanceKeys.size() > TickJobIntentService.MAX_NOTIFICATIONS) { // group shown
                    NotificationWrapper.Companion.getInstance().cancelNotification(0);

                    for (Instance instance : notificationInstances.values()) {
                        check(instance != null);

                        notifyInstance(instance, silent, now);
                    }
                } else { // instances shown
                    for (InstanceKey hideInstanceKey : hideInstanceKeys) {
                        if (allTaskKeys.contains(hideInstanceKey.getTaskKey())) {
                            Instance instance = getInstance(hideInstanceKey);

                            NotificationWrapper.Companion.getInstance().cancelNotification(instance.getNotificationId());
                        } else {
                            check(instanceShownRecordNotificationDatas.containsKey(hideInstanceKey));

                            int notificationId = instanceShownRecordNotificationDatas.get(hideInstanceKey).getFirst();

                            NotificationWrapper.Companion.getInstance().cancelNotification(notificationId);
                        }
                    }

                    for (InstanceKey showInstanceKey : showInstanceKeys) {
                        Instance instance = notificationInstances.get(showInstanceKey);
                        check(instance != null);

                        notifyInstance(instance, silent, now);
                    }

                    Stream.of(notificationInstances.values())
                            .filter(instance -> !showInstanceKeys.contains(instance.getInstanceKey()))
                            .forEach(instance -> updateInstance(instance, now));
                }
            }
        } else {
            if (notificationInstances.isEmpty()) {
                message += ", hg";
                NotificationWrapper.Companion.getInstance().cancelNotification(0);
            } else {
                message += ", sg";
                NotificationWrapper.Companion.getInstance().notifyGroup(this, notificationInstances.values(), true, now);
            }

            message += ", hiding " + hideInstanceKeys.size();
            for (InstanceKey hideInstanceKey : hideInstanceKeys) {
                if (allTaskKeys.contains(hideInstanceKey.getTaskKey())) {
                    Instance instance = getInstance(hideInstanceKey);

                    NotificationWrapper.Companion.getInstance().cancelNotification(instance.getNotificationId());
                } else {
                    check(instanceShownRecordNotificationDatas.containsKey(hideInstanceKey));

                    int notificationId = instanceShownRecordNotificationDatas.get(hideInstanceKey).getFirst();

                    NotificationWrapper.Companion.getInstance().cancelNotification(notificationId);
                }
            }

            message += ", s " + showInstanceKeys.size();
            for (InstanceKey showInstanceKey : showInstanceKeys) {
                Instance instance = notificationInstances.get(showInstanceKey);
                check(instance != null);

                notifyInstance(instance, silent, now);
            }

            List<Instance> updateInstances = Stream.of(notificationInstances.values())
                    .filter(instance -> !showInstanceKeys.contains(instance.getInstanceKey()))
                    .collect(Collectors.toList());

            message += ", u " + updateInstances.size();
            Stream.of(updateInstances)
                    .forEach(instance -> updateInstance(instance, now));
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(TickJobIntentService.Companion.getTICK_PREFERENCES(), Context.MODE_PRIVATE);
        check(sharedPreferences != null);

        String tickLog = sharedPreferences.getString(TickJobIntentService.Companion.getTICK_LOG(), "");
        List<String> tickLogArr = Arrays.asList(TextUtils.split(tickLog, "\n"));
        List<String> tickLogArrTrimmed = new ArrayList<>(tickLogArr.subList(Math.max(tickLogArr.size() - 20, 0), tickLogArr.size()));
        tickLogArrTrimmed.add(now.toString() + " s? " + (silent ? "t" : "f") + message);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!silent)
            editor.putLong(TickJobIntentService.Companion.getLAST_TICK_KEY(), now.getLong());

        Optional<TimeStamp> minInstancesTimeStamp = Stream.of(getExistingInstances())
                .map(existingInstance -> existingInstance.getInstanceDateTime().getTimeStamp())
                .filter(timeStamp -> timeStamp.toExactTimeStamp().compareTo(now) > 0)
                .min(TimeStamp::compareTo);

        TimeStamp nextAlarm = null;
        if (minInstancesTimeStamp.isPresent())
            nextAlarm = minInstancesTimeStamp.get();

        //noinspection Convert2MethodRef
        Optional<TimeStamp> minSchedulesTimeStamp = getTasks()
                .filter(task -> task.current(now))
                .filter(task -> task.isRootTask(now))
                .flatMap(task -> Stream.of(task.getCurrentSchedules(now)))
                .map(schedule -> schedule.getNextAlarm(now))
                .filter(timeStamp -> timeStamp != null)
                .min(TimeStamp::compareTo);

        if (minSchedulesTimeStamp.isPresent() && (nextAlarm == null || nextAlarm.compareTo(minSchedulesTimeStamp.get()) > 0))
            nextAlarm = minSchedulesTimeStamp.get();

        NotificationWrapper.Companion.getInstance().updateAlarm(nextAlarm);

        if (nextAlarm != null)
            tickLogArrTrimmed.add("next tick: " + nextAlarm);

        editor.putString(TickJobIntentService.Companion.getTICK_LOG(), TextUtils.join("\n", tickLogArrTrimmed));
        editor.apply();
    }

    private void notifyInstance(@NonNull Instance instance, boolean silent, @NonNull ExactTimeStamp now) {
        long realtime = SystemClock.elapsedRealtime();

        Optional<Long> optional = Stream.of(kotlinDomainFactory.getLastNotificationBeeps().values()).max(Long::compareTo);
        if (optional.isPresent() && realtime - optional.get() < 5000) {
            Log.e("asdf", "skipping notification sound for " + instance.getName());

            silent = true;
        }

        NotificationWrapper.Companion.getInstance().notifyInstance(this, instance, silent, now);

        if (!silent)
            kotlinDomainFactory.getLastNotificationBeeps().put(instance.getInstanceKey(), SystemClock.elapsedRealtime());
    }

    private void updateInstance(@NonNull Instance instance, @NonNull ExactTimeStamp now) {
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

        NotificationWrapper.Companion.getInstance().notifyInstance(this, instance, true, now);
    }

    private void setInstanceNotified(@NonNull InstanceKey instanceKey, @NonNull ExactTimeStamp now) {
        if (instanceKey.getType() == TaskKey.Type.LOCAL) {
            Instance instance = getInstance(instanceKey);

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

        List<Instance> rootInstances = getRootInstances(timeStamp.toExactTimeStamp(), endTimeStamp.toExactTimeStamp(), now);

        List<Instance> currentInstances = Stream.of(rootInstances)
                .filter(instance -> instance.getInstanceDateTime().getTimeStamp().compareTo(timeStamp) == 0)
                .collect(Collectors.toList());

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances) {
            Task task = instance.getTask();

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            HashMap<InstanceKey, GroupListFragment.InstanceData> children = getChildInstanceDatas(instance, now);
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

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        for (kotlin.Pair<Instance, TaskHierarchy> pair : instance.getChildInstances(now)) {
            Instance childInstance = pair.getFirst();
            TaskHierarchy taskHierarchy = pair.getSecond();
            Task childTask = childInstance.getTask();

            Boolean isRootTask = (childTask.current(now) ? childTask.isRootTask(now) : null);

            HashMap<InstanceKey, GroupListFragment.InstanceData> children = getChildInstanceDatas(childInstance, now);
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
