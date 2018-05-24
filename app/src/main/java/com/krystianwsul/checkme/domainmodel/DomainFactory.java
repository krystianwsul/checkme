package com.krystianwsul.checkme.domainmodel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.krystianwsul.checkme.MyApplication;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.domainmodel.local.LocalCustomTime;
import com.krystianwsul.checkme.domainmodel.local.LocalFactory;
import com.krystianwsul.checkme.domainmodel.local.LocalInstance;
import com.krystianwsul.checkme.domainmodel.local.LocalTask;
import com.krystianwsul.checkme.domainmodel.local.LocalTaskHierarchy;
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
import com.krystianwsul.checkme.firebase.json.UserJson;
import com.krystianwsul.checkme.firebase.json.UserWrapper;
import com.krystianwsul.checkme.firebase.records.RemoteRootUserRecord;
import com.krystianwsul.checkme.gui.HierarchyData;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment;
import com.krystianwsul.checkme.gui.tasks.TaskListFragment;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.loaders.DayLoader;
import com.krystianwsul.checkme.loaders.EditInstanceLoader;
import com.krystianwsul.checkme.loaders.EditInstancesLoader;
import com.krystianwsul.checkme.loaders.FriendListLoader;
import com.krystianwsul.checkme.loaders.MainLoader;
import com.krystianwsul.checkme.loaders.ProjectListLoader;
import com.krystianwsul.checkme.loaders.ShowCustomTimeLoader;
import com.krystianwsul.checkme.loaders.ShowCustomTimesLoader;
import com.krystianwsul.checkme.loaders.ShowGroupLoader;
import com.krystianwsul.checkme.loaders.ShowInstanceLoader;
import com.krystianwsul.checkme.loaders.ShowNotificationGroupLoader;
import com.krystianwsul.checkme.loaders.ShowProjectLoader;
import com.krystianwsul.checkme.loaders.ShowTaskInstancesLoader;
import com.krystianwsul.checkme.loaders.ShowTaskLoader;
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

import junit.framework.Assert;

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

@SuppressLint("UseSparseArrays")
public class DomainFactory {
    private static DomainFactory sDomainFactory;

    private static ExactTimeStamp sStart;
    private static ExactTimeStamp sRead;
    private static ExactTimeStamp sStop;

    @Nullable
    private UserInfo mUserInfo;

    @Nullable
    private RemoteFriendFactory mRemoteFriendFactory;

    @Nullable
    private Query mRecordQuery;

    @Nullable
    private ValueEventListener mRecordListener;

    @Nullable
    private Query mFriendQuery;

    @Nullable
    private ValueEventListener mFriendListener;

    @Nullable
    private Query mUserQuery;

    @Nullable
    private ValueEventListener mUserListener;

    @NonNull
    private final LocalFactory mLocalFactory;

    @Nullable
    private RemoteProjectFactory mRemoteProjectFactory;

    @Nullable
    private RemoteRootUser mRemoteRootUser;

    @NonNull
    private final List<FirebaseListener> mNotTickFirebaseListeners = new ArrayList<>();

    @NonNull
    private final List<FirebaseListener> mFriendFirebaseListeners = new ArrayList<>();

    @Nullable
    private TickData mTickData = null;

    private boolean mSkipSave = false;

    private final Map<InstanceKey, Long> mLastNotificationBeeps = new HashMap<>();

    @NonNull
    public static synchronized DomainFactory getDomainFactory() {
        if (sDomainFactory == null) {
            sStart = ExactTimeStamp.Companion.getNow();

            sDomainFactory = new DomainFactory();

            sRead = ExactTimeStamp.Companion.getNow();

            sDomainFactory.initialize();

            sStop = ExactTimeStamp.Companion.getNow();
        }

        return sDomainFactory;
    }

    private DomainFactory() {
        mLocalFactory = LocalFactory.Companion.getInstance();
    }

    DomainFactory(@NonNull PersistenceManger persistenceManger) {
        mLocalFactory = new LocalFactory(persistenceManger);
    }

    private void initialize() {
        mLocalFactory.initialize(this);
    }

    public boolean isHoldingWakeLock() {
        return mTickData != null && mTickData.mWakelock.isHeld();
    }

    public long getReadMillis() {
        return (sRead.getLong() - sStart.getLong());
    }

    public long getInstantiateMillis() {
        return (sStop.getLong() - sRead.getLong());
    }

    public synchronized void reset(@NonNull Context context, @NonNull SaveService.Source source) {
        UserInfo userInfo = mUserInfo;
        clearUserInfo(context);

        sDomainFactory = null;
        mLocalFactory.reset();

        if (userInfo != null)
            setUserInfo(context, source, userInfo);

        ObserverHolder.INSTANCE.notifyDomainObservers(new ArrayList<>());

        ObserverHolder.INSTANCE.clear();
    }

    public int getTaskCount() {
        int count = mLocalFactory.getTaskCount();
        if (mRemoteProjectFactory != null)
            count += mRemoteProjectFactory.getTaskCount();
        return count;
    }

    public int getInstanceCount() {
        int count = mLocalFactory.getInstanceCount();
        if (mRemoteProjectFactory != null)
            count += mRemoteProjectFactory.getInstanceCount();
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
        if (mSkipSave)
            return;

        mLocalFactory.save(context, source);

        if (mRemoteProjectFactory != null)
            mRemoteProjectFactory.save();

        ObserverHolder.INSTANCE.notifyDomainObservers(dataIds);
    }

    // firebase

    public synchronized void setUserInfo(@NonNull Context context, @NonNull SaveService.Source source, @NonNull UserInfo userInfo) {
        if (mUserInfo != null) {
            Assert.assertTrue(mRecordQuery != null);
            Assert.assertTrue(mFriendQuery != null);
            Assert.assertTrue(mUserQuery != null);

            if (mUserInfo.equals(userInfo))
                return;

            clearUserInfo(context);
        }

        Assert.assertTrue(mUserInfo == null);

        Assert.assertTrue(mRecordQuery == null);
        Assert.assertTrue(mRecordListener == null);

        Assert.assertTrue(mFriendQuery == null);
        Assert.assertTrue(mFriendListener == null);

        Assert.assertTrue(mUserQuery == null);
        Assert.assertTrue(mUserListener == null);

        mUserInfo = userInfo;

        Context applicationContext = context.getApplicationContext();
        Assert.assertTrue(applicationContext != null);

        DatabaseWrapper.INSTANCE.setUserInfo(userInfo, mLocalFactory.getUuid());

        mRecordQuery = DatabaseWrapper.INSTANCE.getTaskRecordsQuery(userInfo);
        mRecordListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("asdf", "DomainFactory.mRecordListener.onDataChange, dataSnapshot: " + dataSnapshot);
                Assert.assertTrue(dataSnapshot != null);

                setRemoteTaskRecords(applicationContext, dataSnapshot, source);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Assert.assertTrue(databaseError != null);
                Log.e("asdf", "DomainFactory.mRecordListener.onCancelled", databaseError.toException());

                MyCrashlytics.INSTANCE.logException(databaseError.toException());

                if (mTickData != null) {
                    mTickData.release();
                    mTickData = null;
                }

                mNotTickFirebaseListeners.clear();
                mFriendFirebaseListeners.clear();
            }
        };
        mRecordQuery.addValueEventListener(mRecordListener);

        mFriendQuery = DatabaseWrapper.INSTANCE.getFriendsQuery(mUserInfo);
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

                MyCrashlytics.INSTANCE.logException(databaseError.toException());
            }
        };
        mFriendQuery.addValueEventListener(mFriendListener);

        mUserQuery = DatabaseWrapper.INSTANCE.getUserQuery(userInfo);
        mUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("asdf", "DomainFactory.mUserListener.onDataChange, dataSnapshot: " + dataSnapshot);
                Assert.assertTrue(dataSnapshot != null);

                setUserRecord(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Assert.assertTrue(databaseError != null);
                Log.e("asdf", "DomainFactory.mUserListener.onCancelled", databaseError.toException());

                MyCrashlytics.INSTANCE.logException(databaseError.toException());
            }
        };
        mUserQuery.addValueEventListener(mUserListener);
    }

    public synchronized void clearUserInfo(@NonNull Context context) {
        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        if (mUserInfo == null) {
            Assert.assertTrue(mRecordQuery == null);
            Assert.assertTrue(mRecordListener == null);
            Assert.assertTrue(mFriendQuery == null);
            Assert.assertTrue(mFriendListener == null);
            Assert.assertTrue(mUserQuery == null);
            Assert.assertTrue(mUserListener == null);
        } else {
            Assert.assertTrue(mRecordQuery != null);
            Assert.assertTrue(mRecordListener != null);
            Assert.assertTrue(mFriendQuery != null);
            Assert.assertTrue(mFriendListener != null);
            Assert.assertTrue(mUserQuery != null);
            Assert.assertTrue(mUserListener != null);

            mLocalFactory.clearRemoteCustomTimeRecords();
            Log.e("asdf", "clearing mRemoteProjectFactory", new Exception());

            mRemoteProjectFactory = null;
            mRemoteFriendFactory = null;

            mUserInfo = null;

            mRecordQuery.removeEventListener(mRecordListener);
            mRecordQuery = null;
            mRecordListener = null;

            mFriendQuery.removeEventListener(mFriendListener);
            mFriendQuery = null;
            mFriendListener = null;

            mUserQuery.removeEventListener(mUserListener);
            mUserQuery = null;
            mUserListener = null;

            updateNotifications(context, now);

            ObserverHolder.INSTANCE.notifyDomainObservers(new ArrayList<>());
        }
    }

    private synchronized void setRemoteTaskRecords(@NonNull Context context, @NonNull DataSnapshot dataSnapshot, @NonNull SaveService.Source source) {
        Assert.assertTrue(mUserInfo != null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        mLocalFactory.clearRemoteCustomTimeRecords();

        boolean firstThereforeSilent = (mRemoteProjectFactory == null);
        mRemoteProjectFactory = new RemoteProjectFactory(this, dataSnapshot.getChildren(), mUserInfo, mLocalFactory.getUuid(), now);

        tryNotifyFriendListeners(); // assuming they're all getters

        if (mTickData == null && mNotTickFirebaseListeners.isEmpty()) {
            updateNotifications(context, firstThereforeSilent, ExactTimeStamp.Companion.getNow(), new ArrayList<>());

            save(context, 0, source);
        } else {
            mSkipSave = true;

            if (mTickData == null) {
                updateNotifications(context, firstThereforeSilent, ExactTimeStamp.Companion.getNow(), new ArrayList<>());
            } else {
                updateNotificationsTick(context, source, mTickData.mSilent, mTickData.mSource);

                if (!firstThereforeSilent) {
                    Log.e("asdf", "not first, clearing mTickData");

                    mTickData.release();
                    mTickData = null;
                } else {
                    Log.e("asdf", "first, keeping mTickData");
                }
            }

            Stream.of(mNotTickFirebaseListeners)
                    .forEach(firebaseListener -> firebaseListener.onFirebaseResult(this));
            mNotTickFirebaseListeners.clear();

            mSkipSave = false;

            save(context, 0, source);
        }
    }

    private void tryNotifyFriendListeners() {
        if (mRemoteProjectFactory == null)
            return;

        if (mRemoteFriendFactory == null)
            return;

        Stream.of(mFriendFirebaseListeners)
                .forEach(firebaseListener -> firebaseListener.onFirebaseResult(this));
        mFriendFirebaseListeners.clear();
    }

    private synchronized void setFriendRecords(@NonNull DataSnapshot dataSnapshot) {
        mRemoteFriendFactory = new RemoteFriendFactory(dataSnapshot.getChildren());

        ObserverHolder.INSTANCE.notifyDomainObservers(new ArrayList<>());

        tryNotifyFriendListeners();
    }

    private synchronized void setUserRecord(@NonNull DataSnapshot dataSnapshot) {
        UserWrapper userWrapper = dataSnapshot.getValue(UserWrapper.class);
        Assert.assertTrue(userWrapper != null);

        RemoteRootUserRecord remoteRootUserRecord = new RemoteRootUserRecord(false, userWrapper);
        mRemoteRootUser = new RemoteRootUser(remoteRootUserRecord);
    }

    public synchronized void addFirebaseListener(@NonNull FirebaseListener firebaseListener) {
        Assert.assertTrue(mRemoteProjectFactory == null || mRemoteProjectFactory.isSaved());

        mNotTickFirebaseListeners.add(firebaseListener);
    }

    public synchronized void addFriendFirebaseListener(@NonNull FirebaseListener firebaseListener) {
        Assert.assertTrue(mRemoteProjectFactory == null);
        Assert.assertTrue(mRemoteFriendFactory == null);

        mFriendFirebaseListeners.add(firebaseListener);
    }

    public synchronized void removeFirebaseListener(@NonNull FirebaseListener firebaseListener) {
        mNotTickFirebaseListeners.remove(firebaseListener);
    }

    public synchronized void setFirebaseTickListener(@NonNull Context context, @NonNull SaveService.Source source, @NonNull TickData tickData) {
        Assert.assertTrue(FirebaseAuth.getInstance().getCurrentUser() != null);

        if ((mRemoteProjectFactory != null) && !mRemoteProjectFactory.isSaved() && (mTickData == null)) {
            updateNotificationsTick(context, source, tickData.mSilent, tickData.mSource);

            tickData.release();
        } else {
            if (mTickData != null) {
                mTickData = mergeTickDatas(context, mTickData, tickData);
            } else {
                mTickData = tickData;
            }
        }
    }

    @NonNull
    private static TickData mergeTickDatas(@NonNull Context context, @NonNull TickData oldTickData, @NonNull TickData newTickData) {
        boolean silent = (oldTickData.mSilent && newTickData.mSilent);

        String source = "merged (" + oldTickData + ", " + newTickData + ")";

        oldTickData.releaseWakelock();
        newTickData.releaseWakelock();

        List<TickData.Listener> listeners = new ArrayList<>(oldTickData.listeners);
        listeners.addAll(newTickData.listeners);

        return new TickData(silent, source, context, listeners);
    }

    public synchronized boolean isConnected() {
        return (mRemoteProjectFactory != null);
    }

    public synchronized boolean isSaved() {
        Assert.assertTrue(mRemoteProjectFactory != null);

        return mRemoteProjectFactory.isSaved();
    }

    public synchronized boolean hasFriends() {
        return (mRemoteFriendFactory != null);
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

        MyCrashlytics.INSTANCE.log("DomainFactory.getEditInstanceData");

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Map<CustomTimeKey, CustomTime> currentCustomTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance.isRootInstance(now));

        if (instance.getInstanceTimePair().getCustomTimeKey() != null) {
            CustomTime customTime = getCustomTime(instance.getInstanceTimePair().getCustomTimeKey());

            currentCustomTimes.put(customTime.getCustomTimeKey(), customTime);
        }

        Map<CustomTimeKey, EditInstanceLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : currentCustomTimes.values())
            customTimeDatas.put(customTime.getCustomTimeKey(), new EditInstanceLoader.CustomTimeData(customTime.getCustomTimeKey(), customTime.getName(), customTime.getHourMinutes()));

        return new EditInstanceLoader.Data(instance.getInstanceKey(), instance.getInstanceDate(), instance.getInstanceTimePair(), instance.getName(), customTimeDatas, (instance.getDone() != null), instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) <= 0);
    }

    @NonNull
    public synchronized EditInstancesLoader.Data getEditInstancesData(@NonNull List<InstanceKey> instanceKeys) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getEditInstancesData");

        Assert.assertTrue(instanceKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Map<CustomTimeKey, CustomTime> currentCustomTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        HashMap<InstanceKey, EditInstancesLoader.InstanceData> instanceDatas = new HashMap<>();

        for (InstanceKey instanceKey : instanceKeys) {
            Instance instance = getInstance(instanceKey);
            Assert.assertTrue(instance.isRootInstance(now));
            Assert.assertTrue(instance.getDone() == null);

            instanceDatas.put(instanceKey, new EditInstancesLoader.InstanceData(instance.getInstanceDateTime(), instance.getName()));

            if (instance.getInstanceTimePair().getCustomTimeKey() != null) {
                CustomTime customTime = getCustomTime(instance.getInstanceTimePair().getCustomTimeKey());

                currentCustomTimes.put(customTime.getCustomTimeKey(), customTime);
            }
        }

        Map<CustomTimeKey, EditInstancesLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : currentCustomTimes.values())
            customTimeDatas.put(customTime.getCustomTimeKey(), new EditInstancesLoader.CustomTimeData(customTime.getCustomTimeKey(), customTime.getName(), customTime.getHourMinutes()));

        Boolean showHour = Stream.of(instanceDatas.values()).allMatch(instanceData -> instanceData.getInstanceDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) < 0);

        return new EditInstancesLoader.Data(instanceDatas, customTimeDatas, showHour);
    }

    @NonNull
    public synchronized ShowCustomTimeLoader.Data getShowCustomTimeData(int localCustomTimeId) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowCustomTimeData");

        LocalCustomTime localCustomTime = mLocalFactory.getLocalCustomTime(localCustomTimeId);

        HashMap<DayOfWeek, HourMinute> hourMinutes = new HashMap<>();
        for (DayOfWeek dayOfWeek : DayOfWeek.values())
            hourMinutes.put(dayOfWeek, localCustomTime.getHourMinute(dayOfWeek));

        return new ShowCustomTimeLoader.Data(localCustomTime.getId(), localCustomTime.getName(), hourMinutes);
    }

    @NonNull
    public synchronized ShowCustomTimesLoader.Data getShowCustomTimesData() {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowCustomTimesData");

        List<LocalCustomTime> currentCustomTimes = getCurrentCustomTimes();

        ArrayList<ShowCustomTimesLoader.CustomTimeData> entries = new ArrayList<>();
        for (LocalCustomTime localCustomTime : currentCustomTimes) {
            Assert.assertTrue(localCustomTime != null);

            entries.add(new ShowCustomTimesLoader.CustomTimeData(localCustomTime.getId(), localCustomTime.getName()));
        }

        return new ShowCustomTimesLoader.Data(entries);
    }

    @NonNull
    public synchronized DayLoader.Data getGroupListData(@NonNull Context context, @NonNull ExactTimeStamp now, int position, @NonNull MainActivity.TimeRange timeRange) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowNotificationGroupData");

        Assert.assertTrue(position >= 0);

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
        DayLoader.Data data = new DayLoader.Data(dataWrapper);

        Stream.of(instanceDatas.values()).forEach(instanceData -> instanceData.setInstanceDataParent(dataWrapper));

        Log.e("asdf", "getShowNotificationGroupData returning " + data);
        return data;
    }

    @NonNull
    public synchronized ShowGroupLoader.Data getShowGroupData(@NonNull Context context, @NonNull TimeStamp timeStamp) {
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

        return new ShowGroupLoader.Data(displayText, getGroupListData(timeStamp, now));
    }

    @NonNull
    public synchronized ShowTaskInstancesLoader.Data getShowTaskInstancesData(@NonNull TaskKey taskKey) {
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
                        Assert.assertTrue(taskHierarchy != null);

                        hierarchyData = new HierarchyData(taskHierarchy.getTaskHierarchyKey(), taskHierarchy.getOrdinal());
                    }

                    return new GroupListFragment.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(MyApplication.Companion.getInstance(), now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), children, hierarchyData, instance.getOrdinal());
                }, HashMap::new));

        return new ShowTaskInstancesLoader.Data(new GroupListFragment.DataWrapper(customTimeDatas, task.current(now), null, null, instanceDatas));
    }


    public synchronized ShowNotificationGroupLoader.Data getShowNotificationGroupData(@NonNull Context context, @NonNull Set<InstanceKey> instanceKeys) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowNotificationGroupData");

        Assert.assertTrue(!instanceKeys.isEmpty());

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

        return new ShowNotificationGroupLoader.Data(dataWrapper);
    }

    @NonNull
    public synchronized ShowInstanceLoader.Data getShowInstanceData(@NonNull Context context, @NonNull InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowInstanceData");

        Task task = getTaskIfPresent(instanceKey.getTaskKey());
        if (task == null)
            return new ShowInstanceLoader.Data(null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Instance instance = getInstance(instanceKey);
        if (!task.current(now) && !instance.exists())
            return new ShowInstanceLoader.Data(null);

        return new ShowInstanceLoader.Data(new ShowInstanceLoader.InstanceData(instance.getName(), instance.getDisplayText(context, now), instance.getDone() != null, task.current(now), instance.isRootInstance(now), instance.exists(), getGroupListData(instance, task, now)));
    }

    @NonNull
    kotlin.Pair<Map<CustomTimeKey, CustomTime>, Map<CreateTaskLoader.ScheduleData, List<Schedule>>> getScheduleDatas(List<Schedule> schedules, ExactTimeStamp now) {
        Map<CustomTimeKey, CustomTime> customTimes = new HashMap<>();

        Map<CreateTaskLoader.ScheduleData, List<Schedule>> scheduleDatas = new HashMap<>();

        Map<TimePair, List<WeeklySchedule>> weeklySchedules = new HashMap<>();

        for (Schedule schedule : schedules) {
            Assert.assertTrue(schedule != null);
            Assert.assertTrue(schedule.current(now));

            switch (schedule.getScheduleType()) {
                case SINGLE: {
                    SingleSchedule singleSchedule = (SingleSchedule) schedule;

                    scheduleDatas.put(new CreateTaskLoader.ScheduleData.SingleScheduleData(singleSchedule.getDate(), singleSchedule.getTimePair()), Collections.singletonList(schedule));

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

                    scheduleDatas.put(new CreateTaskLoader.ScheduleData.MonthlyDayScheduleData(monthlyDaySchedule.getDayOfMonth(), monthlyDaySchedule.getBeginningOfMonth(), monthlyDaySchedule.getTimePair()), Collections.singletonList(schedule));

                    CustomTimeKey customTimeKey = monthlyDaySchedule.getCustomTimeKey();
                    if (customTimeKey != null)
                        customTimes.put(customTimeKey, getCustomTime(customTimeKey));

                    break;
                }
                case MONTHLY_WEEK: {
                    MonthlyWeekSchedule monthlyWeekSchedule = (MonthlyWeekSchedule) schedule;

                    scheduleDatas.put(new CreateTaskLoader.ScheduleData.MonthlyWeekScheduleData(monthlyWeekSchedule.getDayOfMonth(), monthlyWeekSchedule.getDayOfWeek(), monthlyWeekSchedule.getBeginningOfMonth(), monthlyWeekSchedule.getTimePair()), Collections.singletonList(schedule));

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
            scheduleDatas.put(new CreateTaskLoader.ScheduleData.WeeklyScheduleData(daysOfWeek, entry.getKey()), new ArrayList<>(entry.getValue()));
        }

        return new kotlin.Pair<>(customTimes, scheduleDatas);
    }

    @NonNull
    public synchronized CreateTaskLoader.Data getCreateTaskData(@Nullable TaskKey taskKey, @NonNull Context context, @Nullable List<TaskKey> joinTaskKeys) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getCreateTaskData");

        Assert.assertTrue(taskKey == null || joinTaskKeys == null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Map<CustomTimeKey, CustomTime> customTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        List<TaskKey> excludedTaskKeys = new ArrayList<>();
        if (taskKey != null)
            excludedTaskKeys.add(taskKey);
        else if (joinTaskKeys != null)
            excludedTaskKeys.addAll(joinTaskKeys);

        CreateTaskLoader.TaskData taskData = null;
        Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> parentTreeDatas;
        if (taskKey != null) {
            Task task = getTaskForce(taskKey);

            CreateTaskLoader.ParentKey.TaskParentKey taskParentKey;
            List<CreateTaskLoader.ScheduleData> scheduleDatas = null;

            if (task.isRootTask(now)) {
                List<Schedule> schedules = task.getCurrentSchedules(now);

                taskParentKey = null;

                if (!schedules.isEmpty()) {
                    kotlin.Pair<Map<CustomTimeKey, CustomTime>, Map<CreateTaskLoader.ScheduleData, List<Schedule>>> pair = getScheduleDatas(schedules, now);
                    customTimes.putAll(pair.getFirst());
                    scheduleDatas = new ArrayList<>(pair.getSecond().keySet());
                }
            } else {
                Task parentTask = task.getParentTask(now);
                Assert.assertTrue(parentTask != null);

                taskParentKey = new CreateTaskLoader.ParentKey.TaskParentKey(parentTask.getTaskKey());
            }

            RemoteProject remoteProject = task.getRemoteNullableProject();
            String projectName = null;
            if (remoteProject != null)
                projectName = remoteProject.getName();

            taskData = new CreateTaskLoader.TaskData(task.getName(), taskParentKey, scheduleDatas, task.getNote(), projectName);

            if (task instanceof RemoteTask) {
                RemoteTask remoteTask = (RemoteTask) task;

                parentTreeDatas = getProjectTaskTreeDatas(context, now, remoteTask.getRemoteProject(), excludedTaskKeys);
            } else {
                Assert.assertTrue(task instanceof LocalTask);

                parentTreeDatas = getParentTreeDatas(context, now, excludedTaskKeys);
            }
        } else {
            String projectId = null;
            if (joinTaskKeys != null) {
                Assert.assertTrue(joinTaskKeys.size() > 1);

                List<String> projectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                        .distinct()
                        .collect(Collectors.toList());

                Assert.assertTrue(projectIds.size() == 1);

                projectId = projectIds.get(0);
            }

            if (!TextUtils.isEmpty(projectId)) {
                Assert.assertTrue(mRemoteProjectFactory != null);

                RemoteProject remoteProject = mRemoteProjectFactory.getRemoteProjectForce(projectId);

                parentTreeDatas = getProjectTaskTreeDatas(context, now, remoteProject, excludedTaskKeys);
            } else {
                parentTreeDatas = getParentTreeDatas(context, now, excludedTaskKeys);
            }
        }

        @SuppressLint("UseSparseArrays") HashMap<CustomTimeKey, CreateTaskLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : customTimes.values())
            customTimeDatas.put(customTime.getCustomTimeKey(), new CreateTaskLoader.CustomTimeData(customTime.getCustomTimeKey(), customTime.getName(), customTime.getHourMinutes()));

        return new CreateTaskLoader.Data(taskData, parentTreeDatas, customTimeDatas);
    }

    @NonNull
    public synchronized ShowTaskLoader.Data getShowTaskData(@NonNull TaskKey taskKey, @NonNull Context context) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowTaskData");

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task task = getTaskForce(taskKey);
        Assert.assertTrue(task.current(now));

        List<TaskListFragment.ChildTaskData> childTaskDatas = Stream.of(task.getChildTaskHierarchies(now))
                .map(taskHierarchy -> {
                    Task childTask = taskHierarchy.getChildTask();

                    return new TaskListFragment.ChildTaskData(childTask.getName(), childTask.getScheduleText(context, now), getChildTaskDatas(childTask, now, context), childTask.getNote(), childTask.getStartExactTimeStamp(), childTask.getTaskKey(), new HierarchyData(taskHierarchy.getTaskHierarchyKey(), taskHierarchy.getOrdinal()));
                })
                .collect(Collectors.toList());
        Collections.sort(childTaskDatas, TaskListFragment.ChildTaskData::compareTo);

        return new ShowTaskLoader.Data(task.getName(), task.getScheduleText(context, now), new TaskListFragment.TaskData(childTaskDatas, task.getNote()), !task.getExistingInstances().isEmpty());
    }

    @NonNull
    public synchronized MainLoader.Data getMainData(@NonNull Context context) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getMainData");

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        return new MainLoader.Data(getMainData(now, context));
    }

    @NonNull
    public synchronized ProjectListLoader.Data getProjectListData() {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getProjectListData");

        Assert.assertTrue(mRemoteProjectFactory != null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        TreeMap<String, ProjectListLoader.ProjectData> projectDatas = Stream.of(mRemoteProjectFactory.getRemoteProjects().values())
                .filter(remoteProject -> remoteProject.current(now))
                .collect(Collectors.toMap(RemoteProject::getId, remoteProject -> {
                    String users = Stream.of(remoteProject.getUsers())
                            .map(RemoteProjectUser::getName)
                            .collect(Collectors.joining(", "));

                    return new ProjectListLoader.ProjectData(remoteProject.getId(), remoteProject.getName(), users);
                }, TreeMap::new));

        return new ProjectListLoader.Data(projectDatas);
    }

    @NonNull
    public synchronized FriendListLoader.Data getFriendListData() {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getFriendListData");

        Assert.assertTrue(mRemoteFriendFactory != null);

        Set<FriendListLoader.UserListData> userListDatas = Stream.of(mRemoteFriendFactory.getFriends())
                .map(remoteRootUser -> new FriendListLoader.UserListData(remoteRootUser.getName(), remoteRootUser.getEmail(), remoteRootUser.getId()))
                .collect(Collectors.toSet());

        return new FriendListLoader.Data(userListDatas);
    }

    @NonNull
    public synchronized ShowProjectLoader.Data getShowProjectData(@Nullable String projectId) {
        fakeDelay();

        MyCrashlytics.INSTANCE.log("DomainFactory.getShowProjectData");

        Assert.assertTrue(mRemoteProjectFactory != null);
        Assert.assertTrue(mUserInfo != null);
        Assert.assertTrue(mRemoteFriendFactory != null);

        Map<String, ShowProjectLoader.UserListData> friendDatas = Stream.of(mRemoteFriendFactory.getFriends())
                .map(remoteRootUser -> new ShowProjectLoader.UserListData(remoteRootUser.getName(), remoteRootUser.getEmail(), remoteRootUser.getId()))
                .collect(Collectors.toMap(ShowProjectLoader.UserListData::getId, userData -> userData));

        String name;
        Set<ShowProjectLoader.UserListData> userListDatas;
        if (!TextUtils.isEmpty(projectId)) {
            RemoteProject remoteProject = mRemoteProjectFactory.getRemoteProjectForce(projectId);

            name = remoteProject.getName();

            userListDatas = Stream.of(remoteProject.getUsers())
                    .filterNot(remoteUser -> remoteUser.getId().equals(mUserInfo.getKey()))
                    .map(remoteUser -> new ShowProjectLoader.UserListData(remoteUser.getName(), remoteUser.getEmail(), remoteUser.getId()))
                    .collect(Collectors.toSet());
        } else {
            name = null;
            userListDatas = new HashSet<>();
        }

        return new ShowProjectLoader.Data(name, userListDatas, friendDatas);
    }

    // sets

    public synchronized void setInstanceDateTime(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceDateTime");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        instance.setInstanceDateTime(instanceDate, instanceTimePair, now);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, instance.getRemoteNullableProject());
    }

    public synchronized void setInstancesDateTime(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull Set<InstanceKey> instanceKeys, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstancesDateTime");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(instanceKeys.size() > 1);

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
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

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
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

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
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

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
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

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
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

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
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Instance instance = setInstanceDone(context, now, dataId, source, instanceKey, done);

        return instance.getDone();
    }

    public synchronized void setInstancesNotified(@NonNull Context context, @NonNull SaveService.Source source, @NonNull List<InstanceKey> instanceKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstancesNotified");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        for (InstanceKey instanceKey : instanceKeys)
            setInstanceNotified(instanceKey, now);

        save(context, 0, source);
    }

    public synchronized void setInstanceNotified(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceNotified");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        setInstanceNotified(instanceKey, ExactTimeStamp.Companion.getNow());

        save(context, dataId, source);
    }

    @NonNull
    Task createScheduleRootTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());

        Task task;
        if (TextUtils.isEmpty(projectId)) {
            task = mLocalFactory.createScheduleRootTask(this, now, name, scheduleDatas, note);
        } else {
            Assert.assertTrue(mRemoteProjectFactory != null);

            task = mRemoteProjectFactory.createScheduleRootTask(now, name, scheduleDatas, note, projectId);
        }

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, task.getRemoteNullableProject());

        return task;
    }

    public synchronized void createScheduleRootTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createScheduleRootTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        createScheduleRootTask(context, now, dataId, source, name, scheduleDatas, note, projectId);
    }

    @NonNull
    TaskKey updateScheduleTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());

        Task task = getTaskForce(taskKey);
        Assert.assertTrue(task.current(now));

        task = task.updateProject(context, now, projectId);

        task.setName(name, note);

        if (!task.isRootTask(now)) {
            TaskHierarchy taskHierarchy = getParentTaskHierarchy(task, now);
            Assert.assertTrue(taskHierarchy != null);

            taskHierarchy.setEndExactTimeStamp(now);
        }

        task.updateSchedules(scheduleDatas, now);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    @NonNull
    public synchronized TaskKey updateScheduleTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateScheduleTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        return updateScheduleTask(context, now, dataId, source, taskKey, name, scheduleDatas, note, projectId);
    }

    public synchronized void createScheduleJoinRootTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createScheduleJoinRootTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());
        Assert.assertTrue(joinTaskKeys.size() > 1);

        List<String> joinProjectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                .distinct()
                .collect(Collectors.toList());
        Assert.assertTrue(joinProjectIds.size() == 1);

        String joinProjectId = joinProjectIds.get(0);

        final String finalProjectId;
        if (!TextUtils.isEmpty(joinProjectId)) {
            Assert.assertTrue(TextUtils.isEmpty(projectId));

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
            Assert.assertTrue(mRemoteProjectFactory != null);
            Assert.assertTrue(mUserInfo != null);

            newParentTask = mRemoteProjectFactory.createScheduleRootTask(now, name, scheduleDatas, note, finalProjectId);
        } else {
            newParentTask = mLocalFactory.createScheduleRootTask(this, now, name, scheduleDatas, note);
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
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Task parentTask = getTaskForce(parentTaskKey);
        Assert.assertTrue(parentTask.current(now));

        Task childTask = parentTask.createChildTask(now, name, note);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, childTask.getRemoteNullableProject());

        return childTask;
    }

    public synchronized void createChildTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey parentTaskKey, @NonNull String name, @Nullable String note) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createChildTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        createChildTask(context, now, dataId, source, parentTaskKey, name, note);
    }

    public synchronized void createJoinChildTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull TaskKey parentTaskKey, @NonNull String name, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createJoinChildTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task parentTask = getTaskForce(parentTaskKey);
        Assert.assertTrue(parentTask.current(now));

        List<String> joinProjectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                .distinct()
                .collect(Collectors.toList());
        Assert.assertTrue(joinProjectIds.size() == 1);

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
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));

        Task task = getTaskForce(taskKey);
        Assert.assertTrue(task.current(now));

        Task newParentTask = getTaskForce(parentTaskKey);
        Assert.assertTrue(task.current(now));

        task.setName(name, note);

        Task oldParentTask = task.getParentTask(now);
        if (oldParentTask == null) {
            Stream.of(task.getCurrentSchedules(now))
                    .forEach(schedule -> schedule.setEndExactTimeStamp(now));

            newParentTask.addChild(task, now);
        } else if (oldParentTask != newParentTask) {
            TaskHierarchy oldTaskHierarchy = getParentTaskHierarchy(task, now);
            Assert.assertTrue(oldTaskHierarchy != null);

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
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task task = getTaskForce(taskKey);
        Assert.assertTrue(task.current(now));

        task.setEndExactTimeStamp(now);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, task.getRemoteNullableProject());
    }

    public synchronized void setInstanceOrdinal(int dataId, @NonNull InstanceKey instanceKey, double ordinal) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setInstanceOrdinal");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Instance instance = getInstance(instanceKey);

        instance.setOrdinal(ordinal, now);

        updateNotifications(MyApplication.Companion.getInstance(), now);

        save(MyApplication.Companion.getInstance(), dataId, SaveService.Source.GUI);

        notifyCloud(MyApplication.Companion.getInstance(), instance.getRemoteNullableProject());
    }

    public synchronized void setTaskHierarchyOrdinal(int dataId, @NonNull HierarchyData hierarchyData) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setTaskHierarchyOrdinal");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        RemoteProject remoteProject;
        TaskHierarchy taskHierarchy;
        if (hierarchyData.getTaskHierarchyKey() instanceof TaskHierarchyKey.LocalTaskHierarchyKey) {
            TaskHierarchyKey.LocalTaskHierarchyKey localTaskHierarchyKey = (TaskHierarchyKey.LocalTaskHierarchyKey) hierarchyData.getTaskHierarchyKey();

            remoteProject = null;
            taskHierarchy = mLocalFactory.getTaskHierarchy(localTaskHierarchyKey);
        } else {
            Assert.assertTrue(hierarchyData.getTaskHierarchyKey() instanceof TaskHierarchyKey.RemoteTaskHierarchyKey);

            TaskHierarchyKey.RemoteTaskHierarchyKey remoteTaskHierarchyKey = (TaskHierarchyKey.RemoteTaskHierarchyKey) hierarchyData.getTaskHierarchyKey();

            remoteProject = mRemoteProjectFactory.getRemoteProjectForce(remoteTaskHierarchyKey.getProjectId());
            taskHierarchy = remoteProject.getTaskHierarchy(remoteTaskHierarchyKey.getTaskHierarchyId());
        }

        Assert.assertTrue(taskHierarchy.current(now));

        taskHierarchy.setOrdinal(hierarchyData.getOrdinal());

        updateNotifications(MyApplication.Companion.getInstance(), now);

        save(MyApplication.Companion.getInstance(), dataId, SaveService.Source.GUI);

        if (remoteProject != null)
            notifyCloud(MyApplication.Companion.getInstance(), remoteProject);
    }

    public synchronized void setTaskEndTimeStamps(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull ArrayList<TaskKey> taskKeys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setTaskEndTimeStamps");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!taskKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<Task> tasks = Stream.of(taskKeys)
                .map(this::getTaskForce)
                .collect(Collectors.toList());

        Assert.assertTrue(Stream.of(tasks)
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
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));

        Assert.assertTrue(hourMinutes.get(DayOfWeek.SUNDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.MONDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.TUESDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.WEDNESDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.THURSDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.FRIDAY) != null);
        Assert.assertTrue(hourMinutes.get(DayOfWeek.SATURDAY) != null);

        LocalCustomTime localCustomTime = mLocalFactory.createLocalCustomTime(this, name, hourMinutes);

        save(context, 0, source);

        return localCustomTime.getId();
    }

    public synchronized void updateCustomTime(@NonNull Context context, int dataId, @NonNull SaveService.Source source, int localCustomTimeId, @NonNull String name, @NonNull Map<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateCustomTime");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));

        LocalCustomTime localCustomTime = mLocalFactory.getLocalCustomTime(localCustomTimeId);

        localCustomTime.setName(name);

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            HourMinute hourMinute = hourMinutes.get(dayOfWeek);
            Assert.assertTrue(hourMinute != null);

            if (hourMinute.compareTo(localCustomTime.getHourMinute(dayOfWeek)) != 0)
                localCustomTime.setHourMinute(dayOfWeek, hourMinute);
        }

        save(context, dataId, source);
    }

    public synchronized void setCustomTimeCurrent(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull List<Integer> localCustomTimeIds) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setCustomTimeCurrent");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!localCustomTimeIds.isEmpty());

        for (int localCustomTimeId : localCustomTimeIds) {
            LocalCustomTime localCustomTime = mLocalFactory.getLocalCustomTime(localCustomTimeId);

            localCustomTime.setCurrent();
        }

        save(context, dataId, source);
    }

    @NonNull
    Task createRootTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull SaveService.Source source, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Task task;
        if (TextUtils.isEmpty(projectId)) {
            task = mLocalFactory.createLocalTaskHelper(this, name, now, note);
        } else {
            Assert.assertTrue(mRemoteProjectFactory != null);

            task = mRemoteProjectFactory.createRemoteTaskHelper(now, name, note, projectId);
        }

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, task.getRemoteNullableProject());

        return task;
    }

    public synchronized void createRootTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createRootTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        createRootTask(context, now, dataId, source, name, note, projectId);
    }

    public synchronized void createJoinRootTask(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createJoinRootTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        List<String> joinProjectIds = Stream.of(joinTaskKeys).map(TaskKey::getRemoteProjectId)
                .distinct()
                .collect(Collectors.toList());
        Assert.assertTrue(joinProjectIds.size() == 1);

        String joinProjectId = joinProjectIds.get(0);

        final String finalProjectId;
        if (!TextUtils.isEmpty(joinProjectId)) {
            Assert.assertTrue(TextUtils.isEmpty(projectId));

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
            Assert.assertTrue(mRemoteProjectFactory != null);
            Assert.assertTrue(mUserInfo != null);

            newParentTask = mRemoteProjectFactory.createRemoteTaskHelper(now, name, note, finalProjectId);
        } else {
            newParentTask = mLocalFactory.createLocalTaskHelper(this, name, now, note);
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
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task task = getTaskForce(taskKey);
        Assert.assertTrue(task.current(now));

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

        if (mRemoteProjectFactory != null)
            mLocalFactory.deleteInstanceShownRecords(mRemoteProjectFactory.getTaskKeys());

        save(context, 0, source);

        return irrelevant;
    }

    public synchronized void updateNotificationsTick(@NonNull Context context, @NonNull SaveService.Source source, boolean silent, @NonNull String sourceName) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateNotificationsTick source: " + sourceName);
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        updateNotificationsTick(context, now, source, silent);
    }

    public synchronized void removeFriends(@NonNull Set<String> keys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.removeFriends");

        Assert.assertTrue(mUserInfo != null);
        Assert.assertTrue(mRemoteProjectFactory != null);
        Assert.assertTrue(mRemoteFriendFactory != null);
        Assert.assertTrue(!mRemoteFriendFactory.isSaved());

        Stream.of(keys)
                .forEach(friendId -> mRemoteFriendFactory.removeFriend(mUserInfo.getKey(), friendId));

        mRemoteFriendFactory.save();
    }

    public synchronized void updateUserInfo(@NonNull Context context, @NonNull SaveService.Source source, @NonNull UserInfo userInfo) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateUserInfo");
        Assert.assertTrue(mUserInfo != null);
        Assert.assertTrue(mRemoteProjectFactory != null);

        if (mUserInfo.equals(userInfo))
            return;

        mUserInfo = userInfo;
        DatabaseWrapper.INSTANCE.setUserInfo(userInfo, mLocalFactory.getUuid());

        mRemoteProjectFactory.updateUserInfo(userInfo);

        save(context, 0, source);
    }

    public synchronized void updateProject(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull String projectId, @NonNull String name, @NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateProject");

        Assert.assertTrue(!TextUtils.isEmpty(projectId));
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(mRemoteProjectFactory != null);
        Assert.assertTrue(mRemoteFriendFactory != null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        RemoteProject remoteProject = mRemoteProjectFactory.getRemoteProjectForce(projectId);

        remoteProject.setName(name);
        remoteProject.updateRecordOf(Stream.of(addedFriends)
                .map(mRemoteFriendFactory::getFriend)
                .collect(Collectors.toSet()), removedFriends);

        updateNotifications(context, now);

        save(context, dataId, source);

        notifyCloud(context, remoteProject, removedFriends);
    }

    public synchronized void createProject(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull Set<String> friends) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createProject");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(mRemoteProjectFactory != null);
        Assert.assertTrue(mUserInfo != null);
        Assert.assertTrue(mRemoteRootUser != null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Set<String> recordOf = new HashSet<>(friends);

        String key = mUserInfo.getKey();
        Assert.assertTrue(!recordOf.contains(key));
        recordOf.add(key);

        RemoteProject remoteProject = mRemoteProjectFactory.createRemoteProject(name, now, recordOf, mRemoteRootUser);

        save(context, dataId, source);

        notifyCloud(context, remoteProject);
    }

    public synchronized void setProjectEndTimeStamps(@NonNull Context context, int dataId, @NonNull SaveService.Source source, @NonNull Set<String> projectIds) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setProjectEndTimeStamps");

        Assert.assertTrue(mRemoteProjectFactory != null);
        Assert.assertTrue(mUserInfo != null);
        Assert.assertTrue(!projectIds.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Set<RemoteProject> remoteProjects = Stream.of(projectIds)
                .map(mRemoteProjectFactory::getRemoteProjectForce)
                .collect(Collectors.toSet());

        Assert.assertTrue(Stream.of(remoteProjects)
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
            Assert.assertTrue(TextUtils.isEmpty(instanceKey.getTaskKey().getRemoteProjectId()));
            Assert.assertTrue(TextUtils.isEmpty(instanceKey.getTaskKey().getRemoteTaskId()));

            return mLocalFactory.getExistingInstanceIfPresent(instanceKey);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(instanceKey.getTaskKey().getRemoteProjectId()));
            Assert.assertTrue(!TextUtils.isEmpty(instanceKey.getTaskKey().getRemoteTaskId()));
            Assert.assertTrue(mRemoteProjectFactory != null);

            return mRemoteProjectFactory.getExistingInstanceIfPresent(instanceKey);
        }
    }

    @NonNull
    public String getRemoteCustomTimeId(@NonNull String projectId, @NonNull CustomTimeKey customTimeKey) {
        if (!TextUtils.isEmpty(customTimeKey.getRemoteProjectId())) {
            Assert.assertTrue(!TextUtils.isEmpty(customTimeKey.getRemoteCustomTimeId()));
            Assert.assertTrue(customTimeKey.getLocalCustomTimeId() == null);

            Assert.assertTrue(customTimeKey.getRemoteProjectId().equals(projectId));

            return customTimeKey.getRemoteCustomTimeId();
        } else {
            Assert.assertTrue(TextUtils.isEmpty(customTimeKey.getRemoteCustomTimeId()));
            Assert.assertTrue(customTimeKey.getLocalCustomTimeId() != null);

            LocalCustomTime localCustomTime = mLocalFactory.getLocalCustomTime(customTimeKey.getLocalCustomTimeId());

            Assert.assertTrue(localCustomTime.hasRemoteRecord(projectId));

            return localCustomTime.getRemoteId(projectId);
        }
    }

    @NonNull
    private Instance generateInstance(@NonNull TaskKey taskKey, @NonNull DateTime scheduleDateTime) {
        if (taskKey.getLocalTaskId() != null) {
            Assert.assertTrue(TextUtils.isEmpty(taskKey.getRemoteProjectId()));
            Assert.assertTrue(TextUtils.isEmpty(taskKey.getRemoteTaskId()));

            return new LocalInstance(this, taskKey.getLocalTaskId(), scheduleDateTime);
        } else {
            Assert.assertTrue(mRemoteProjectFactory != null);
            Assert.assertTrue(!TextUtils.isEmpty(taskKey.getRemoteProjectId()));
            Assert.assertTrue(!TextUtils.isEmpty(taskKey.getRemoteTaskId()));

            String remoteCustomTimeId;
            Integer hour;
            Integer minute;

            CustomTimeKey customTimeKey = scheduleDateTime.getTime().getTimePair().getCustomTimeKey();
            HourMinute hourMinute = scheduleDateTime.getTime().getTimePair().getHourMinute();

            if (customTimeKey != null) {
                Assert.assertTrue(hourMinute == null);

                remoteCustomTimeId = getRemoteCustomTimeId(taskKey.getRemoteProjectId(), customTimeKey);

                hour = null;
                minute = null;
            } else {
                Assert.assertTrue(hourMinute != null);

                remoteCustomTimeId = null;

                hour = hourMinute.getHour();
                minute = hourMinute.getMinute();
            }

            InstanceShownRecord instanceShownRecord = mLocalFactory.getInstanceShownRecord(taskKey.getRemoteProjectId(), taskKey.getRemoteTaskId(), scheduleDateTime.getDate().getYear(), scheduleDateTime.getDate().getMonth(), scheduleDateTime.getDate().getDay(), remoteCustomTimeId, hour, minute);

            RemoteProject remoteProject = mRemoteProjectFactory.getTaskForce(taskKey).getRemoteProject();

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
    List<Instance> getPastInstances(@NonNull Task task, @NonNull ExactTimeStamp now) {
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
            Assert.assertTrue(timePair.getCustomTimeKey() == null);

            return new NormalTime(timePair.getHourMinute());
        } else {
            Assert.assertTrue(timePair.getCustomTimeKey() != null);

            return getCustomTime(timePair.getCustomTimeKey());
        }
    }

    @NonNull
    private DateTime getDateTime(@NonNull Date date, @NonNull TimePair timePair) {
        return new DateTime(date, getTime(timePair));
    }

    @Nullable
    Task getParentTask(@NonNull Task childTask, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(childTask.notDeleted(exactTimeStamp));

        TaskHierarchy parentTaskHierarchy = getParentTaskHierarchy(childTask, exactTimeStamp);
        if (parentTaskHierarchy == null) {
            return null;
        } else {
            Assert.assertTrue(parentTaskHierarchy.notDeleted(exactTimeStamp));

            Task parentTask = parentTaskHierarchy.getParentTask();
            Assert.assertTrue(parentTask.notDeleted(exactTimeStamp));

            return parentTask;
        }
    }

    @NonNull
    public CustomTime getCustomTime(@NonNull CustomTimeKey customTimeKey) {
        if (customTimeKey.getLocalCustomTimeId() != null) {
            Assert.assertTrue(TextUtils.isEmpty(customTimeKey.getRemoteProjectId()));
            Assert.assertTrue(TextUtils.isEmpty(customTimeKey.getRemoteCustomTimeId()));

            return mLocalFactory.getLocalCustomTime(customTimeKey.getLocalCustomTimeId());
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(customTimeKey.getRemoteProjectId()));
            Assert.assertTrue(!TextUtils.isEmpty(customTimeKey.getRemoteCustomTimeId()));
            Assert.assertTrue(mRemoteProjectFactory != null);

            return mRemoteProjectFactory.getRemoteCustomTime(customTimeKey.getRemoteProjectId(), customTimeKey.getRemoteCustomTimeId());
        }
    }

    @NonNull
    private List<LocalCustomTime> getCurrentCustomTimes() {
        return mLocalFactory.getCurrentCustomTimes();
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
    private Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> getChildTaskDatas(@NonNull ExactTimeStamp now, @NonNull Task parentTask, @NonNull Context context, @NonNull List<TaskKey> excludedTaskKeys) {
        return Stream.of(parentTask.getChildTaskHierarchies(now))
                .filterNot(taskHierarchy -> excludedTaskKeys.contains(taskHierarchy.getChildTaskKey()))
                .collect(Collectors.toMap(taskHierarchy -> new CreateTaskLoader.ParentKey.TaskParentKey(taskHierarchy.getChildTaskKey()), taskHierarchy -> {
                    Task childTask = taskHierarchy.getChildTask();

                    return new CreateTaskLoader.ParentTreeData(childTask.getName(), getChildTaskDatas(now, childTask, context, excludedTaskKeys), new CreateTaskLoader.ParentKey.TaskParentKey(childTask.getTaskKey()), childTask.getScheduleText(context, now), childTask.getNote(), new CreateTaskLoader.SortKey.TaskSortKey(childTask.getStartExactTimeStamp()));
                }));
    }

    @NonNull
    private Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> getParentTreeDatas(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull List<TaskKey> excludedTaskKeys) {
        Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> parentTreeDatas = new HashMap<>();

        parentTreeDatas.putAll(Stream.of(mLocalFactory.getTasks())
                .filterNot(task -> excludedTaskKeys.contains(task.getTaskKey()))
                .filter(task -> task.current(now))
                .filter(task -> task.isVisible(now))
                .filter(task -> task.isRootTask(now))
                .collect(Collectors.toMap(task -> new CreateTaskLoader.ParentKey.TaskParentKey(task.getTaskKey()), task -> new CreateTaskLoader.ParentTreeData(task.getName(), getChildTaskDatas(now, task, context, excludedTaskKeys), new CreateTaskLoader.ParentKey.TaskParentKey(task.getTaskKey()), task.getScheduleText(context, now), task.getNote(), new CreateTaskLoader.SortKey.TaskSortKey(task.getStartExactTimeStamp())))));

        if (mRemoteProjectFactory != null) {
            parentTreeDatas.putAll(Stream.of(mRemoteProjectFactory.getRemoteProjects().values())
                    .filter(remoteProject -> remoteProject.current(now))
                    .collect(Collectors.toMap(remoteProject -> new CreateTaskLoader.ParentKey.ProjectParentKey(remoteProject.getId()), remoteProject -> {
                        String users = Stream.of(remoteProject.getUsers())
                                .map(RemoteProjectUser::getName)
                                .collect(Collectors.joining(", "));

                        return new CreateTaskLoader.ParentTreeData(remoteProject.getName(), getProjectTaskTreeDatas(context, now, remoteProject, excludedTaskKeys), new CreateTaskLoader.ParentKey.ProjectParentKey(remoteProject.getId()), users, null, new CreateTaskLoader.SortKey.ProjectSortKey(remoteProject.getId()));
                    })));
        }

        return parentTreeDatas;
    }

    @NonNull
    private Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> getProjectTaskTreeDatas(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull RemoteProject remoteProject, @NonNull List<TaskKey> excludedTaskKeys) {
        return Stream.of(remoteProject.getTasks())
                .filterNot(task -> excludedTaskKeys.contains(task.getTaskKey()))
                .filter(task -> task.current(now))
                .filter(task -> task.isVisible(now))
                .filter(task -> task.isRootTask(now))
                .collect(Collectors.toMap(task -> new CreateTaskLoader.ParentKey.TaskParentKey(task.getTaskKey()), task -> new CreateTaskLoader.ParentTreeData(task.getName(), getChildTaskDatas(now, task, context, excludedTaskKeys), new CreateTaskLoader.ParentKey.TaskParentKey(task.getTaskKey()), task.getScheduleText(context, now), task.getNote(), new CreateTaskLoader.SortKey.TaskSortKey(task.getStartExactTimeStamp()))));
    }

    @NonNull
    public RemoteTask convertLocalToRemote(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull LocalTask startingLocalTask, @NonNull String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(projectId));

        Assert.assertTrue(mRemoteProjectFactory != null);
        Assert.assertTrue(mUserInfo != null);

        LocalToRemoteConversion localToRemoteConversion = new LocalToRemoteConversion();
        mLocalFactory.convertLocalToRemoteHelper(localToRemoteConversion, startingLocalTask);

        updateNotifications(context, true, now, Stream.of(localToRemoteConversion.mLocalTasks.values())
                .map(pair -> pair.getFirst().getTaskKey())
                .collect(Collectors.toList()));

        RemoteProject remoteProject = mRemoteProjectFactory.getRemoteProjectForce(projectId);

        for (kotlin.Pair<LocalTask, List<LocalInstance>> pair : localToRemoteConversion.mLocalTasks.values()) {
            Assert.assertTrue(pair != null);

            RemoteTask remoteTask = remoteProject.copyLocalTask(pair.getFirst(), pair.getSecond(), now);
            localToRemoteConversion.mRemoteTasks.put(pair.getFirst().getId(), remoteTask);
        }

        for (LocalTaskHierarchy localTaskHierarchy : localToRemoteConversion.mLocalTaskHierarchies) {
            Assert.assertTrue(localTaskHierarchy != null);

            RemoteTask parentRemoteTask = localToRemoteConversion.mRemoteTasks.get(localTaskHierarchy.getParentTaskId());
            Assert.assertTrue(parentRemoteTask != null);

            RemoteTask childRemoteTask = localToRemoteConversion.mRemoteTasks.get(localTaskHierarchy.getChildTaskId());
            Assert.assertTrue(childRemoteTask != null);

            RemoteTaskHierarchy remoteTaskHierarchy = remoteProject.copyLocalTaskHierarchy(localTaskHierarchy, parentRemoteTask.getId(), childRemoteTask.getId());
            localToRemoteConversion.mRemoteTaskHierarchies.add(remoteTaskHierarchy);
        }

        for (kotlin.Pair<LocalTask, List<LocalInstance>> pair : localToRemoteConversion.mLocalTasks.values()) {
            Stream.of(pair.getSecond())
                    .forEach(LocalInstance::delete);

            pair.getFirst().delete();
        }

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

    @Nullable
    TaskHierarchy getParentTaskHierarchy(@NonNull Task childTask, @NonNull ExactTimeStamp exactTimeStamp) {
        if (childTask.current(exactTimeStamp)) {
            Assert.assertTrue(childTask.notDeleted(exactTimeStamp));

            TaskKey childTaskKey = childTask.getTaskKey();

            List<TaskHierarchy> taskHierarchies = Stream.of(childTask.getTaskHierarchiesByChildTaskKey(childTaskKey))
                    .filter(taskHierarchy -> taskHierarchy.current(exactTimeStamp))
                    .collect(Collectors.toList());

            if (taskHierarchies.isEmpty()) {
                return null;
            } else {
                Assert.assertTrue(taskHierarchies.size() == 1);
                return taskHierarchies.get(0);
            }
        } else {
            // jeśli child task jeszcze nie istnieje, ale będzie utworzony jako child, zwróć ów przyszły hierarchy
            // żeby można było dodawać child instances do past parent instance

            Assert.assertTrue(childTask.notDeleted(exactTimeStamp));

            TaskKey childTaskKey = childTask.getTaskKey();

            List<TaskHierarchy> taskHierarchies = Stream.of(childTask.getTaskHierarchiesByChildTaskKey(childTaskKey))
                    .filter(taskHierarchy -> taskHierarchy.getStartExactTimeStamp().equals(childTask.getStartExactTimeStamp()))
                    .collect(Collectors.toList());

            if (taskHierarchies.isEmpty()) {
                return null;
            } else {
                Assert.assertTrue(taskHierarchies.size() == 1);
                return taskHierarchies.get(0);
            }
        }
    }

    @NonNull
    private Stream<Task> getTasks() {
        if (mRemoteProjectFactory != null) {
            return Stream.concat(Stream.of(mLocalFactory.getTasks()), Stream.of(mRemoteProjectFactory.getTasks()));
        } else {
            return Stream.of(mLocalFactory.getTasks());
        }
    }

    @NonNull
    private List<CustomTime> getCustomTimes() {
        List<CustomTime> customTimes = new ArrayList<>(mLocalFactory.getLocalCustomTimes());

        if (mRemoteProjectFactory != null)
            customTimes.addAll(mRemoteProjectFactory.getRemoteCustomTimes());

        return customTimes;
    }

    @NonNull
    Task getTaskForce(@NonNull TaskKey taskKey) {
        if (taskKey.getLocalTaskId() != null) {
            Assert.assertTrue(TextUtils.isEmpty(taskKey.getRemoteTaskId()));

            return mLocalFactory.getTaskForce(taskKey.getLocalTaskId());
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(taskKey.getRemoteTaskId()));
            Assert.assertTrue(mRemoteProjectFactory != null);

            return mRemoteProjectFactory.getTaskForce(taskKey);
        }
    }

    @Nullable
    private Task getTaskIfPresent(@NonNull TaskKey taskKey) {
        if (taskKey.getLocalTaskId() != null) {
            Assert.assertTrue(TextUtils.isEmpty(taskKey.getRemoteTaskId()));

            return mLocalFactory.getTaskIfPresent(taskKey.getLocalTaskId());
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(taskKey.getRemoteTaskId()));
            Assert.assertTrue(mRemoteProjectFactory != null);

            return mRemoteProjectFactory.getTaskIfPresent(taskKey);
        }
    }

    @NonNull
    List<TaskHierarchy> getChildTaskHierarchies(@NonNull Task parentTask, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(parentTask.current(exactTimeStamp));

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
        return mRemoteProjectFactory;
    }

    @NonNull
    public LocalFactory getLocalFactory() {
        return mLocalFactory;
    }

    @NonNull
    private List<Instance> getExistingInstances() {
        List<Instance> instances = new ArrayList<>(mLocalFactory.getExistingInstances());

        if (mRemoteProjectFactory != null)
            instances.addAll(mRemoteProjectFactory.getExistingInstances());

        return instances;
    }

    @NonNull
    public Map<String, UserJson> getUserJsons(@NonNull Set<String> friendIds) {
        Assert.assertTrue(mRemoteFriendFactory != null);

        return mRemoteFriendFactory.getUserJsons(friendIds);
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

        Map<Integer, LocalCustomTimeRelevance> localCustomTimeRelevances = Stream.of(mLocalFactory.getLocalCustomTimes()).collect(Collectors.toMap(LocalCustomTime::getId, LocalCustomTimeRelevance::new));

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

        Assert.assertTrue(Stream.of(irrelevantTasks)
                .noneMatch(task -> task.isVisible(now)));

        List<Instance> relevantExistingInstances = Stream.of(instanceRelevances.values())
                .filter(InstanceRelevance::getRelevant)
                .map(InstanceRelevance::getInstance)
                .filter(Instance::exists)
                .collect(Collectors.toList());

        List<Instance> irrelevantExistingInstances = new ArrayList<>(existingInstances);
        irrelevantExistingInstances.removeAll(relevantExistingInstances);

        Assert.assertTrue(Stream.of(irrelevantExistingInstances)
                .noneMatch(instance -> instance.isVisible(now)));

        List<LocalCustomTime> relevantLocalCustomTimes = Stream.of(localCustomTimeRelevances.values())
                .filter(LocalCustomTimeRelevance::getRelevant)
                .map(LocalCustomTimeRelevance::getLocalCustomTime)
                .collect(Collectors.toList());

        List<LocalCustomTime> irrelevantLocalCustomTimes = new ArrayList<>(mLocalFactory.getLocalCustomTimes());
        irrelevantLocalCustomTimes.removeAll(relevantLocalCustomTimes);

        Assert.assertTrue(Stream.of(irrelevantLocalCustomTimes)
                .noneMatch(LocalCustomTime::getCurrent));

        Stream.of(irrelevantExistingInstances)
                .forEach(Instance::delete);

        Stream.of(irrelevantTasks)
                .forEach(Task::delete);

        Stream.of(irrelevantLocalCustomTimes)
                .forEach(LocalCustomTime::delete);

        List<RemoteCustomTime> irrelevantRemoteCustomTimes;
        List<RemoteProject> irrelevantRemoteProjects;
        if (mRemoteProjectFactory != null) {
            List<RemoteCustomTime> remoteCustomTimes = mRemoteProjectFactory.getRemoteCustomTimes();
            Map<kotlin.Pair<String, String>, RemoteCustomTimeRelevance> remoteCustomTimeRelevances = Stream.of(remoteCustomTimes).collect(Collectors.toMap(remoteCustomTime -> new kotlin.Pair<>(remoteCustomTime.getProjectId(), remoteCustomTime.getId()), RemoteCustomTimeRelevance::new));

            Collection<RemoteProject> remoteProjects = mRemoteProjectFactory.getRemoteProjects().values();
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
            Assert.assertTrue(mUserInfo != null);

            BackendNotifier.INSTANCE.notify(context, remoteProjects, mUserInfo, new ArrayList<>());
        }
    }

    private void notifyCloud(@NonNull Context context, @NonNull RemoteProject remoteProject, @NonNull Collection<String> userKeys) {
        Assert.assertTrue(mUserInfo != null);

        Set<RemoteProject> remoteProjects = Collections.singleton(remoteProject);

        BackendNotifier.INSTANCE.notify(context, remoteProjects, mUserInfo, userKeys);
    }

    private void updateNotifications(@NonNull Context context, @NonNull ExactTimeStamp now) {
        updateNotifications(context, true, now, new ArrayList<>());
    }

    @NonNull
    private Set<TaskKey> getTaskKeys() {
        HashSet<TaskKey> taskKeys = new HashSet<>(Stream.of(mLocalFactory.getTaskIds()).map(TaskKey::new).collect(Collectors.toList()));

        if (mRemoteProjectFactory != null)
            taskKeys.addAll(mRemoteProjectFactory.getTaskKeys());

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

                customTimeKey = getCustomTimeKey(instanceShownRecord.getProjectId(), remoteCustomTimeId);
                hourMinute = null;
            } else {
                Assert.assertTrue(instanceShownRecord.getScheduleHour() != null);
                Assert.assertTrue(instanceShownRecord.getScheduleMinute() != null);

                customTimeKey = null;
                hourMinute = new HourMinute(instanceShownRecord.getScheduleHour(), instanceShownRecord.getScheduleMinute());
            }

            TaskKey taskKey = new TaskKey(instanceShownRecord.getProjectId(), instanceShownRecord.getTaskId());

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
            Assert.assertTrue(showInstanceKey != null);

            Instance showInstance = getInstance(showInstanceKey);

            showInstance.setNotificationShown(true, now);
        }

        Set<TaskKey> allTaskKeys = getTaskKeys();

        for (InstanceKey hideInstanceKey : hideInstanceKeys) {
            Assert.assertTrue(hideInstanceKey != null);

            if (allTaskKeys.contains(hideInstanceKey.getTaskKey())) {
                Instance hideInstance = getInstance(hideInstanceKey);

                hideInstance.setNotificationShown(false, now);
            } else {
                Assert.assertTrue(instanceShownRecordNotificationDatas.containsKey(hideInstanceKey));

                instanceShownRecordNotificationDatas.get(hideInstanceKey).getSecond().setNotificationShown(false);
            }
        }

        String message = "";

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (notificationInstances.size() > TickJobIntentService.Companion.getMAX_NOTIFICATIONS()) { // show group
                if (shownInstanceKeys.size() > TickJobIntentService.Companion.getMAX_NOTIFICATIONS()) { // group shown
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
                            Assert.assertTrue(instanceShownRecordNotificationDatas.containsKey(shownInstanceKey));

                            int notificationId = instanceShownRecordNotificationDatas.get(shownInstanceKey).getFirst();

                            NotificationWrapper.Companion.getInstance().cancelNotification(notificationId);
                        }
                    }

                    NotificationWrapper.Companion.getInstance().notifyGroup(this, notificationInstances.values(), silent, now);
                }
            } else { // show instances
                if (shownInstanceKeys.size() > TickJobIntentService.Companion.getMAX_NOTIFICATIONS()) { // group shown
                    NotificationWrapper.Companion.getInstance().cancelNotification(0);

                    for (Instance instance : notificationInstances.values()) {
                        Assert.assertTrue(instance != null);

                        notifyInstance(instance, silent, now);
                    }
                } else { // instances shown
                    for (InstanceKey hideInstanceKey : hideInstanceKeys) {
                        if (allTaskKeys.contains(hideInstanceKey.getTaskKey())) {
                            Instance instance = getInstance(hideInstanceKey);

                            NotificationWrapper.Companion.getInstance().cancelNotification(instance.getNotificationId());
                        } else {
                            Assert.assertTrue(instanceShownRecordNotificationDatas.containsKey(hideInstanceKey));

                            int notificationId = instanceShownRecordNotificationDatas.get(hideInstanceKey).getFirst();

                            NotificationWrapper.Companion.getInstance().cancelNotification(notificationId);
                        }
                    }

                    for (InstanceKey showInstanceKey : showInstanceKeys) {
                        Instance instance = notificationInstances.get(showInstanceKey);
                        Assert.assertTrue(instance != null);

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
                    Assert.assertTrue(instanceShownRecordNotificationDatas.containsKey(hideInstanceKey));

                    int notificationId = instanceShownRecordNotificationDatas.get(hideInstanceKey).getFirst();

                    NotificationWrapper.Companion.getInstance().cancelNotification(notificationId);
                }
            }

            message += ", s " + showInstanceKeys.size();
            for (InstanceKey showInstanceKey : showInstanceKeys) {
                Instance instance = notificationInstances.get(showInstanceKey);
                Assert.assertTrue(instance != null);

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
        Assert.assertTrue(sharedPreferences != null);

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

        Optional<Long> optional = Stream.of(mLastNotificationBeeps.values()).max(Long::compareTo);
        if (optional.isPresent() && realtime - optional.get() < 5000) {
            Log.e("asdf", "skipping notification sound for " + instance.getName());

            silent = true;
        }

        NotificationWrapper.Companion.getInstance().notifyInstance(this, instance, silent, now);

        if (!silent)
            mLastNotificationBeeps.put(instance.getInstanceKey(), SystemClock.elapsedRealtime());
    }

    private void updateInstance(@NonNull Instance instance, @NonNull ExactTimeStamp now) {
        InstanceKey instanceKey = instance.getInstanceKey();

        long realtime = SystemClock.elapsedRealtime();

        if (mLastNotificationBeeps.containsKey(instanceKey)) {
            long then = mLastNotificationBeeps.get(instanceKey);

            Assert.assertTrue(realtime > then);

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
            Assert.assertTrue(!TextUtils.isEmpty(projectId));

            String taskId = taskKey.getRemoteTaskId();
            Assert.assertTrue(!TextUtils.isEmpty(taskId));

            ScheduleKey scheduleKey = instanceKey.getScheduleKey();
            Date scheduleDate = scheduleKey.getScheduleDate();

            Stream<InstanceShownRecord> stream = Stream.of(mLocalFactory.getInstanceShownRecords())
                    .filter(instanceShownRecord -> instanceShownRecord.getProjectId().equals(projectId))
                    .filter(instanceShownRecord -> instanceShownRecord.getTaskId().equals(taskId))
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleYear() == scheduleDate.getYear())
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleMonth() == scheduleDate.getMonth())
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleDay() == scheduleDate.getDay());

            List<InstanceShownRecord> matches;
            if (scheduleKey.getScheduleTimePair().getCustomTimeKey() != null) {
                Assert.assertTrue(scheduleKey.getScheduleTimePair().getHourMinute() == null);

                Assert.assertTrue(scheduleKey.getScheduleTimePair().getCustomTimeKey().getType() == TaskKey.Type.REMOTE); // remote custom time key hack
                Assert.assertTrue(scheduleKey.getScheduleTimePair().getCustomTimeKey().getLocalCustomTimeId() == null);
                Assert.assertTrue(projectId.equals(scheduleKey.getScheduleTimePair().getCustomTimeKey().getRemoteProjectId()));

                String customTimeId = scheduleKey.getScheduleTimePair().getCustomTimeKey().getRemoteCustomTimeId();
                Assert.assertTrue(!TextUtils.isEmpty(customTimeId));

                matches = stream.filter(instanceShownRecord -> customTimeId.equals(instanceShownRecord.getScheduleCustomTimeId()))
                        .collect(Collectors.toList());
            } else {
                Assert.assertTrue(scheduleKey.getScheduleTimePair().getHourMinute() != null);

                HourMinute hourMinute = scheduleKey.getScheduleTimePair().getHourMinute();

                matches = stream.filter(instanceShownRecord -> Integer.valueOf(hourMinute.getHour()).equals(instanceShownRecord.getScheduleHour()))
                        .filter(instanceShownRecord -> Integer.valueOf(hourMinute.getMinute()).equals(instanceShownRecord.getScheduleMinute()))
                        .collect(Collectors.toList());
            }

            Assert.assertTrue(matches.size() == 1);

            InstanceShownRecord instanceShownRecord = matches.get(0);
            Assert.assertTrue(instanceShownRecord != null);

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
        LocalCustomTime localCustomTime = mLocalFactory.getLocalCustomTime(remoteProjectId, remoteCustomTimeId);

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

    public interface FirebaseListener {
        void onFirebaseResult(@NonNull DomainFactory domainFactory);
    }

    public static class TickData {
        private static final String WAKELOCK_TAG = "myWakelockTag";
        final boolean mSilent;

        @NonNull
        private final String mSource;

        @NonNull
        private final PowerManager.WakeLock mWakelock;

        @NonNull
        private final List<Listener> listeners;

        public TickData(boolean silent, @NonNull String source, @NonNull Context context, @NonNull List<Listener> listeners) {
            Assert.assertTrue(!TextUtils.isEmpty(source));

            mSilent = silent;
            mSource = source;
            this.listeners = listeners;

            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            Assert.assertTrue(powerManager != null);

            mWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
            mWakelock.acquire(30 * 1000);
        }

        void releaseWakelock() {
            if (mWakelock.isHeld())
                mWakelock.release();
        }

        void release() {
            for (Listener listener : listeners)
                listener.onTick();

            releaseWakelock();
        }

        public interface Listener {

            void onTick();
        }
    }
}
