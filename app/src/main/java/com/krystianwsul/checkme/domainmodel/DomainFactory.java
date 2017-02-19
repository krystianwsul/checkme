package com.krystianwsul.checkme.domainmodel;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
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
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;
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
import com.krystianwsul.checkme.loaders.ShowTaskLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.persistencemodel.InstanceShownRecord;
import com.krystianwsul.checkme.persistencemodel.PersistenceManger;
import com.krystianwsul.checkme.utils.CustomTimeKey;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.ScheduleKey;
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
    public static synchronized DomainFactory getDomainFactory(@NonNull Context context) {
        if (sDomainFactory == null) {
            sStart = ExactTimeStamp.getNow();

            sDomainFactory = new DomainFactory(context);

            sRead = ExactTimeStamp.getNow();

            sDomainFactory.initialize();

            sStop = ExactTimeStamp.getNow();
        }

        return sDomainFactory;
    }

    private DomainFactory(@NonNull Context context) {
        mLocalFactory = LocalFactory.getInstance(context);
    }

    DomainFactory(@NonNull PersistenceManger persistenceManger) {
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
        UserInfo userInfo = mUserInfo;
        clearUserInfo(context);

        sDomainFactory = null;
        mLocalFactory.reset();

        if (userInfo != null)
            setUserInfo(context, userInfo);

        ObserverHolder.getObserverHolder().notifyDomainObservers(new ArrayList<>());

        ObserverHolder.getObserverHolder().clear();
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

    private void save(@NonNull Context context, int dataId) {
        ArrayList<Integer> dataIds = new ArrayList<>();
        dataIds.add(dataId);
        save(context, dataIds);
    }

    private void save(@NonNull Context context, @NonNull List<Integer> dataIds) {
        if (mSkipSave)
            return;

        mLocalFactory.save(context);

        if (mRemoteProjectFactory != null)
            mRemoteProjectFactory.save();

        ObserverHolder.getObserverHolder().notifyDomainObservers(dataIds);
    }

    // firebase

    public synchronized void setUserInfo(@NonNull Context context, @NonNull UserInfo userInfo) {
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

        DatabaseWrapper.setUserInfo(userInfo, mLocalFactory.getUuid());

        mRecordQuery = DatabaseWrapper.getTaskRecordsQuery(userInfo);
        mRecordListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("asdf", "DomainFactory.mRecordListener.onDataChange, dataSnapshot: " + dataSnapshot);
                Assert.assertTrue(dataSnapshot != null);

                setRemoteTaskRecords(applicationContext, dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Assert.assertTrue(databaseError != null);
                Log.e("asdf", "DomainFactory.mRecordListener.onCancelled", databaseError.toException());

                MyCrashlytics.logException(databaseError.toException());

                mTickData = null;
                mNotTickFirebaseListeners.clear();
                mFriendFirebaseListeners.clear();
            }
        };
        mRecordQuery.addValueEventListener(mRecordListener);

        mFriendQuery = DatabaseWrapper.getFriendsQuery(mUserInfo);
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

        mUserQuery = DatabaseWrapper.getUserQuery(userInfo);
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

                MyCrashlytics.logException(databaseError.toException());
            }
        };
        mUserQuery.addValueEventListener(mUserListener);
    }

    public synchronized void clearUserInfo(@NonNull Context context) {
        ExactTimeStamp now = ExactTimeStamp.getNow();

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

            ObserverHolder.getObserverHolder().notifyDomainObservers(new ArrayList<>());
        }
    }

    private synchronized void setRemoteTaskRecords(@NonNull Context context, @NonNull DataSnapshot dataSnapshot) {
        Assert.assertTrue(mUserInfo != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        mLocalFactory.clearRemoteCustomTimeRecords();

        boolean firstThereforeSilent = (mRemoteProjectFactory == null);
        mRemoteProjectFactory = new RemoteProjectFactory(this, dataSnapshot.getChildren(), mUserInfo, mLocalFactory.getUuid(), now);

        tryNotifyFriendListeners(); // assuming they're all getters

        if (mTickData == null && mNotTickFirebaseListeners.isEmpty()) {
            updateNotifications(context, firstThereforeSilent, ExactTimeStamp.getNow(), new ArrayList<>());

            save(context, new ArrayList<>());
        } else {
            mSkipSave = true;

            if (mTickData == null) {
                updateNotifications(context, firstThereforeSilent, ExactTimeStamp.getNow(), new ArrayList<>());
            } else {
                updateNotificationsTick(context, mTickData.mSilent, mTickData.mSource);

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

            save(context, new ArrayList<>());
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

        ObserverHolder.getObserverHolder().notifyDomainObservers(new ArrayList<>());

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

    public synchronized void setFirebaseTickListener(@NonNull Context context, @NonNull TickData tickData) {
        Assert.assertTrue(FirebaseAuth.getInstance().getCurrentUser() != null);

        if ((mRemoteProjectFactory != null) && !mRemoteProjectFactory.isSaved() && (mTickData == null)) {
            updateNotificationsTick(context, tickData.mSilent, tickData.mSource);

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

        oldTickData.release();
        newTickData.release();

        return new TickData(silent, source, context);
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

        MyCrashlytics.log("DomainFactory.getEditInstanceData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Map<CustomTimeKey, CustomTime> currentCustomTimes = Stream.of(getCurrentCustomTimes())
                .collect(Collectors.toMap(CustomTime::getCustomTimeKey, customTime -> customTime));

        Instance instance = getInstance(instanceKey);
        Assert.assertTrue(instance.isRootInstance(now));

        if (instance.getInstanceTimePair().mCustomTimeKey != null) {
            CustomTime customTime = getCustomTime(instance.getInstanceTimePair().mCustomTimeKey);

            currentCustomTimes.put(customTime.getCustomTimeKey(), customTime);
        }

        Map<CustomTimeKey, EditInstanceLoader.CustomTimeData> customTimeDatas = new HashMap<>();
        for (CustomTime customTime : currentCustomTimes.values())
            customTimeDatas.put(customTime.getCustomTimeKey(), new EditInstanceLoader.CustomTimeData(customTime.getCustomTimeKey(), customTime.getName(), customTime.getHourMinutes()));

        return new EditInstanceLoader.Data(instance.getInstanceKey(), instance.getInstanceDate(), instance.getInstanceTimePair(), instance.getName(), customTimeDatas, (instance.getDone() != null), instance.getInstanceDateTime().getTimeStamp().toExactTimeStamp().compareTo(now) <= 0);
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

            instanceDatas.put(instanceKey, new EditInstancesLoader.InstanceData(instance.getInstanceDateTime(), instance.getName()));

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
    public synchronized DayLoader.Data getGroupListData(@NonNull Context context, @NonNull ExactTimeStamp now, int position, @NonNull MainActivity.TimeRange timeRange) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowNotificationGroupData");

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

        GroupListFragment.DataWrapper dataWrapper = new GroupListFragment.DataWrapper(customTimeDatas, null, taskDatas, null);
        DayLoader.Data data = new DayLoader.Data(dataWrapper);

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances) {
            Task task = instance.getTask();

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListFragment.InstanceData instanceData = new GroupListFragment.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), dataWrapper, instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), task.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(instance, now, instanceData));
            instanceDatas.put(instanceData.InstanceKey, instanceData);
        }

        dataWrapper.setInstanceDatas(instanceDatas);

        Log.e("asdf", "getShowNotificationGroupData returning " + data);
        return data;
    }

    @NonNull
    public synchronized ShowGroupLoader.Data getShowGroupData(@NonNull Context context, @NonNull TimeStamp timeStamp) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowGroupData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

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
    public synchronized ShowNotificationGroupLoader.Data getShowNotificationGroupData(@NonNull Context context, @NonNull Set<InstanceKey> instanceKeys) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowNotificationGroupData");

        Assert.assertTrue(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

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

        GroupListFragment.DataWrapper dataWrapper = new GroupListFragment.DataWrapper(customTimeDatas, null, null, null);

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : instances) {
            Task task = instance.getTask();

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListFragment.InstanceData instanceData = new GroupListFragment.InstanceData(instance.getDone(), instance.getInstanceKey(), instance.getDisplayText(context, now), instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), dataWrapper, instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), task.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(instance, now, instanceData));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        dataWrapper.setInstanceDatas(instanceDatas);

        return new ShowNotificationGroupLoader.Data(dataWrapper);
    }

    @NonNull
    public synchronized ShowInstanceLoader.Data getShowInstanceData(@NonNull Context context, @NonNull InstanceKey instanceKey) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowInstanceData");

        Task task = getTaskIfPresent(instanceKey.mTaskKey);
        if (task == null)
            return new ShowInstanceLoader.Data(null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Instance instance = getInstance(instanceKey);
        if (!task.current(now) && !instance.exists())
            return new ShowInstanceLoader.Data(null);

        return new ShowInstanceLoader.Data(new ShowInstanceLoader.InstanceData(instance.getName(), instance.getDisplayText(context, now), instance.getDone() != null, task.current(now), instance.isRootInstance(now), instance.exists(), getGroupListData(instance, task, now)));
    }

    @NonNull
    public synchronized CreateTaskLoader.Data getCreateTaskData(@Nullable TaskKey taskKey, @NonNull Context context, @Nullable List<TaskKey> joinTaskKeys) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getCreateTaskData");

        Assert.assertTrue(taskKey == null || joinTaskKeys == null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

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

            CreateTaskLoader.TaskParentKey taskParentKey;
            List<CreateTaskLoader.ScheduleData> scheduleDatas = null;
            String projectName;

            if (task.isRootTask(now)) {
                List<Schedule> schedules = task.getCurrentSchedules(now);

                taskParentKey = null;
                RemoteProject remoteProject = task.getRemoteNullableProject();
                if (remoteProject == null)
                    projectName = null;
                else
                    projectName = remoteProject.getName();

                if (!schedules.isEmpty()) {
                    scheduleDatas = new ArrayList<>();

                    for (Schedule schedule : schedules) {
                        Assert.assertTrue(schedule != null);
                        Assert.assertTrue(schedule.current(now));

                        switch (schedule.getType()) {
                            case SINGLE: {
                                SingleSchedule singleSchedule = (SingleSchedule) schedule;

                                scheduleDatas.add(singleSchedule.getScheduleData());

                                CustomTimeKey customTimeKey = singleSchedule.getCustomTimeKey();
                                if (customTimeKey != null)
                                    customTimes.put(customTimeKey, getCustomTime(customTimeKey));

                                break;
                            }
                            case DAILY: {
                                DailySchedule dailySchedule = (DailySchedule) schedule;

                                scheduleDatas.add(dailySchedule.getScheduleData());

                                CustomTimeKey customTimeKey = dailySchedule.getCustomTimeKey();
                                if (customTimeKey != null)
                                    customTimes.put(customTimeKey, getCustomTime(customTimeKey));

                                break;
                            }
                            case WEEKLY: {
                                WeeklySchedule weeklySchedule = (WeeklySchedule) schedule;

                                scheduleDatas.add(weeklySchedule.getScheduleData());

                                CustomTimeKey customTimeKey = weeklySchedule.getCustomTimeKey();
                                if (customTimeKey != null)
                                    customTimes.put(customTimeKey, getCustomTime(customTimeKey));

                                break;
                            }
                            case MONTHLY_DAY: {
                                MonthlyDaySchedule monthlyDaySchedule = (MonthlyDaySchedule) schedule;

                                scheduleDatas.add(monthlyDaySchedule.getScheduleData());

                                CustomTimeKey customTimeKey = monthlyDaySchedule.getCustomTimeKey();
                                if (customTimeKey != null)
                                    customTimes.put(customTimeKey, getCustomTime(customTimeKey));

                                break;
                            }
                            case MONTHLY_WEEK: {
                                MonthlyWeekSchedule monthlyWeekSchedule = (MonthlyWeekSchedule) schedule;

                                scheduleDatas.add(monthlyWeekSchedule.getScheduleData());

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
                }
            } else {
                Task parentTask = task.getParentTask(now);
                Assert.assertTrue(parentTask != null);

                taskParentKey = new CreateTaskLoader.TaskParentKey(parentTask.getTaskKey());
                projectName = null;
            }

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

                List<String> projectIds = Stream.of(joinTaskKeys)
                        .map(joinTaskKey -> joinTaskKey.mRemoteProjectId)
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

        MyCrashlytics.log("DomainFactory.getShowTaskData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = getTaskForce(taskKey);
        Assert.assertTrue(task.current(now));

        List<TaskListFragment.ChildTaskData> childTaskDatas = Stream.of(task.getChildTasks(now))
                .map(childTask -> new TaskListFragment.ChildTaskData(childTask.getName(), childTask.getScheduleText(context, now), getChildTaskDatas(childTask, now, context), childTask.getNote(), childTask.getStartExactTimeStamp(), childTask.getTaskKey()))
                .collect(Collectors.toList());
        Collections.sort(childTaskDatas, (TaskListFragment.ChildTaskData lhs, TaskListFragment.ChildTaskData rhs) -> lhs.mStartExactTimeStamp.compareTo(rhs.mStartExactTimeStamp));

        return new ShowTaskLoader.Data(task.getName(), task.getScheduleText(context, now), new TaskListFragment.TaskData(childTaskDatas, task.getNote()));
    }

    @NonNull
    public synchronized MainLoader.Data getMainData(@NonNull Context context) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getMainData");

        ExactTimeStamp now = ExactTimeStamp.getNow();

        return new MainLoader.Data(getMainData(now, context, null));
    }

    @NonNull
    public synchronized ProjectListLoader.Data getProjectListData() {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getProjectListData");

        Assert.assertTrue(mRemoteProjectFactory != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        TreeMap<String, ProjectListLoader.ProjectData> projectDatas = Stream.of(mRemoteProjectFactory.getRemoteProjects())
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

        MyCrashlytics.log("DomainFactory.getFriendListData");

        Assert.assertTrue(mRemoteFriendFactory != null);

        Set<FriendListLoader.UserListData> userListDatas = Stream.of(mRemoteFriendFactory.getFriends())
                .map(remoteRootUser -> new FriendListLoader.UserListData(remoteRootUser.getName(), remoteRootUser.getEmail(), remoteRootUser.getId()))
                .collect(Collectors.toSet());

        return new FriendListLoader.Data(userListDatas);
    }

    @NonNull
    public synchronized ShowProjectLoader.Data getShowProjectData(@Nullable String projectId) {
        fakeDelay();

        MyCrashlytics.log("DomainFactory.getShowProjectData");

        Assert.assertTrue(mRemoteProjectFactory != null);
        Assert.assertTrue(mUserInfo != null);
        Assert.assertTrue(mRemoteFriendFactory != null);

        Map<String, ShowProjectLoader.UserListData> friendDatas = Stream.of(mRemoteFriendFactory.getFriends())
                .map(remoteRootUser -> new ShowProjectLoader.UserListData(remoteRootUser.getName(), remoteRootUser.getEmail(), remoteRootUser.getId()))
                .collect(Collectors.toMap(userData -> userData.mId, userData -> userData));

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

    public synchronized void setInstanceDateTime(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.log("DomainFactory.setInstanceDateTime");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setInstanceDateTime(instanceDate, instanceTimePair, now);

        updateNotifications(context, now);

        save(context, dataId);

        notifyCloud(context, instance.getRemoteNullableProject());
    }

    public synchronized void setInstancesDateTime(@NonNull Context context, int dataId, @NonNull Set<InstanceKey> instanceKeys, @NonNull Date instanceDate, @NonNull TimePair instanceTimePair) {
        MyCrashlytics.log("DomainFactory.setInstancesDateTime");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(instanceKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

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

        save(context, dataId);

        notifyCloud(context, remoteProjects);
    }

    public synchronized void setInstanceAddHourService(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceAddHourService");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        instance.setInstanceDateTime(date, new TimePair(hourMinute), now);
        instance.setNotificationShown(false, now);

        updateNotifications(context, now);

        save(context, dataId);

        notifyCloud(context, instance.getRemoteNullableProject());
    }

    public synchronized void setInstanceAddHourActivity(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceAddHourActivity");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();
        Calendar calendar = now.getCalendar();
        calendar.add(Calendar.HOUR_OF_DAY, 1);

        Date date = new Date(calendar);
        HourMinute hourMinute = new HourMinute(calendar);

        instance.setInstanceDateTime(date, new TimePair(hourMinute), now);

        updateNotifications(context, now);

        save(context, dataId);

        notifyCloud(context, instance.getRemoteNullableProject());
    }

    public synchronized void setInstanceNotificationDone(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotificationDone");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Instance instance = getInstance(instanceKey);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        instance.setDone(true, now);
        instance.setNotificationShown(false, now);

        updateNotifications(context, now);

        save(context, dataId);

        notifyCloud(context, instance.getRemoteNullableProject());
    }

    @NonNull
    public synchronized ExactTimeStamp setInstancesDone(@NonNull Context context, int dataId, @NonNull List<InstanceKey> instanceKeys) {
        MyCrashlytics.log("DomainFactory.setInstancesDone");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.getNow();

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

        save(context, dataId);

        notifyCloud(context, remoteProjects);

        return now;
    }

    public synchronized ExactTimeStamp setInstanceDone(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey, boolean done) {
        MyCrashlytics.log("DomainFactory.setInstanceDone");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Instance instance = setInstanceDone(context, now, dataId, instanceKey, done);

        return instance.getDone();
    }

    public synchronized void setInstancesNotified(@NonNull Context context, int dataId, @NonNull List<InstanceKey> instanceKeys) {
        MyCrashlytics.log("DomainFactory.setInstancesNotified");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!instanceKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        for (InstanceKey instanceKey : instanceKeys)
            setInstanceNotified(instanceKey, now);

        save(context, dataId);
    }

    public synchronized void setInstanceNotified(@NonNull Context context, int dataId, @NonNull InstanceKey instanceKey) {
        MyCrashlytics.log("DomainFactory.setInstanceNotified");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        setInstanceNotified(instanceKey, ExactTimeStamp.getNow());

        save(context, dataId);
    }

    @NonNull
    Task createScheduleRootTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
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

        save(context, dataId);

        notifyCloud(context, task.getRemoteNullableProject());

        return task;
    }

    public synchronized void createScheduleRootTask(@NonNull Context context, int dataId, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.log("DomainFactory.createScheduleRootTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        createScheduleRootTask(context, now, dataId, name, scheduleDatas, note, projectId);
    }

    @NonNull
    TaskKey updateScheduleTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull TaskKey taskKey, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
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

        save(context, dataId);

        notifyCloud(context, task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    @NonNull
    public synchronized TaskKey updateScheduleTask(@NonNull Context context, int dataId, @NonNull TaskKey taskKey, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.log("DomainFactory.updateScheduleTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        return updateScheduleTask(context, now, dataId, taskKey, name, scheduleDatas, note, projectId);
    }

    public synchronized void createScheduleJoinRootTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull String name, @NonNull List<CreateTaskLoader.ScheduleData> scheduleDatas, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.log("DomainFactory.createScheduleJoinRootTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(!scheduleDatas.isEmpty());
        Assert.assertTrue(joinTaskKeys.size() > 1);

        List<String> joinProjectIds = Stream.of(joinTaskKeys)
                .map(joinTaskKey -> joinTaskKey.mRemoteProjectId)
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

        save(context, dataId);

        notifyCloud(context, newParentTask.getRemoteNullableProject());
    }

    Task createChildTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull TaskKey parentTaskKey, @NonNull String name, @Nullable String note) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Task parentTask = getTaskForce(parentTaskKey);
        Assert.assertTrue(parentTask.current(now));

        Task childTask = parentTask.createChildTask(now, name, note);

        updateNotifications(context, now);

        save(context, dataId);

        notifyCloud(context, childTask.getRemoteNullableProject());

        return childTask;
    }

    public synchronized void createChildTask(@NonNull Context context, int dataId, @NonNull TaskKey parentTaskKey, @NonNull String name, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.createChildTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        createChildTask(context, now, dataId, parentTaskKey, name, note);
    }

    public synchronized void createJoinChildTask(@NonNull Context context, int dataId, @NonNull TaskKey parentTaskKey, @NonNull String name, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.createJoinChildTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task parentTask = getTaskForce(parentTaskKey);
        Assert.assertTrue(parentTask.current(now));

        List<String> joinProjectIds = Stream.of(joinTaskKeys)
                .map(joinTaskKey -> joinTaskKey.mRemoteProjectId)
                .distinct()
                .collect(Collectors.toList());
        Assert.assertTrue(joinProjectIds.size() == 1);

        List<Task> joinTasks = Stream.of(joinTaskKeys)
                .map(this::getTaskForce)
                .collect(Collectors.toList());

        Task childTask = parentTask.createChildTask(now, name, note);

        joinTasks(childTask, joinTasks, now);

        updateNotifications(context, now);

        save(context, dataId);

        notifyCloud(context, childTask.getRemoteNullableProject());
    }

    @NonNull
    public synchronized TaskKey updateChildTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull TaskKey taskKey, @NonNull String name, @NonNull TaskKey parentTaskKey, @Nullable String note) {
        MyCrashlytics.log("DomainFactory.updateChildTask");
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

        save(context, dataId);

        notifyCloud(context, task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    public synchronized void setTaskEndTimeStamp(@NonNull Context context, int dataId, @NonNull TaskKey taskKey) {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamp");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Task task = getTaskForce(taskKey);
        Assert.assertTrue(task.current(now));

        task.setEndExactTimeStamp(now);

        updateNotifications(context, now);

        save(context, dataId);

        notifyCloud(context, task.getRemoteNullableProject());
    }

    public synchronized void setTaskEndTimeStamps(@NonNull Context context, int dataId, @NonNull ArrayList<TaskKey> taskKeys) {
        MyCrashlytics.log("DomainFactory.setTaskEndTimeStamps");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!taskKeys.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

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

        save(context, dataId);

        notifyCloud(context, remoteProjects);
    }

    public synchronized int createCustomTime(@NonNull Context context, @NonNull String name, @NonNull Map<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.log("DomainFactory.createCustomTime");
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

        save(context, 0);

        return localCustomTime.getId();
    }

    public synchronized void updateCustomTime(@NonNull Context context, int dataId, int localCustomTimeId, @NonNull String name, @NonNull Map<DayOfWeek, HourMinute> hourMinutes) {
        MyCrashlytics.log("DomainFactory.updateCustomTime");
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

        save(context, dataId);
    }

    public synchronized void setCustomTimeCurrent(@NonNull Context context, int dataId, @NonNull List<Integer> localCustomTimeIds) {
        MyCrashlytics.log("DomainFactory.setCustomTimeCurrent");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!localCustomTimeIds.isEmpty());

        for (int localCustomTimeId : localCustomTimeIds) {
            LocalCustomTime localCustomTime = mLocalFactory.getLocalCustomTime(localCustomTimeId);

            localCustomTime.setCurrent();
        }

        save(context, dataId);
    }

    @NonNull
    Task createRootTask(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Task task;
        if (TextUtils.isEmpty(projectId)) {
            task = mLocalFactory.createLocalTaskHelper(this, name, now, note);
        } else {
            Assert.assertTrue(mRemoteProjectFactory != null);

            task = mRemoteProjectFactory.createRemoteTaskHelper(now, name, note, projectId);
        }

        updateNotifications(context, now);

        save(context, dataId);

        notifyCloud(context, task.getRemoteNullableProject());

        return task;
    }

    public synchronized void createRootTask(@NonNull Context context, int dataId, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.log("DomainFactory.createRootTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        createRootTask(context, now, dataId, name, note, projectId);
    }

    public synchronized void createJoinRootTask(@NonNull Context context, int dataId, @NonNull String name, @NonNull List<TaskKey> joinTaskKeys, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.log("DomainFactory.createJoinRootTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskKeys.size() > 1);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        List<String> joinProjectIds = Stream.of(joinTaskKeys)
                .map(joinTaskKey -> joinTaskKey.mRemoteProjectId)
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

        save(context, dataId);

        notifyCloud(context, newParentTask.getRemoteNullableProject());
    }

    @NonNull
    public synchronized TaskKey updateRootTask(@NonNull Context context, int dataId, @NonNull TaskKey taskKey, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.log("DomainFactory.updateRootTask");
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        Assert.assertTrue(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.getNow();

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

        save(context, dataId);

        notifyCloud(context, task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    @NonNull
    Irrelevant updateNotificationsTick(@NonNull Context context, @NonNull ExactTimeStamp now, boolean silent) {
        updateNotifications(context, silent, now, new ArrayList<>());

        Irrelevant irrelevant = setIrrelevant(now);

        if (mRemoteProjectFactory != null)
            mLocalFactory.deleteInstanceShownRecords(mRemoteProjectFactory.getTaskKeys());

        save(context, 0);

        return irrelevant;
    }

    public synchronized void updateNotificationsTick(@NonNull Context context, boolean silent, @NonNull String source) {
        MyCrashlytics.log("DomainFactory.updateNotificationsTick source: " + source);
        Assert.assertTrue(mRemoteProjectFactory == null || !mRemoteProjectFactory.isSaved());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        updateNotificationsTick(context, now, silent);
    }

    public synchronized void removeFriends(@NonNull Set<String> keys) {
        MyCrashlytics.log("DomainFactory.removeFriends");

        Assert.assertTrue(mUserInfo != null);
        Assert.assertTrue(mRemoteProjectFactory != null);
        Assert.assertTrue(mRemoteFriendFactory != null);
        Assert.assertTrue(!mRemoteFriendFactory.isSaved());

        Stream.of(keys)
                .forEach(friendId -> mRemoteFriendFactory.removeFriend(mUserInfo.getKey(), friendId));

        mRemoteFriendFactory.save();
    }

    public synchronized void updateUserInfo(@NonNull Context context, @NonNull UserInfo userInfo) {
        MyCrashlytics.log("DomainFactory.updateUserInfo");
        Assert.assertTrue(mUserInfo != null);
        Assert.assertTrue(mRemoteProjectFactory != null);

        if (mUserInfo.equals(userInfo))
            return;

        mUserInfo = userInfo;
        DatabaseWrapper.setUserInfo(userInfo, mLocalFactory.getUuid());

        mRemoteProjectFactory.updateUserInfo(userInfo);

        save(context, 0);
    }

    public synchronized void updateProject(@NonNull Context context, int dataId, @NonNull String projectId, @NonNull String name, @NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        MyCrashlytics.log("DomainFactory.updateProject");

        Assert.assertTrue(!TextUtils.isEmpty(projectId));
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(mRemoteProjectFactory != null);
        Assert.assertTrue(mRemoteFriendFactory != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        RemoteProject remoteProject = mRemoteProjectFactory.getRemoteProjectForce(projectId);

        remoteProject.setName(name);
        remoteProject.updateRecordOf(Stream.of(addedFriends)
                .map(mRemoteFriendFactory::getFriend)
                .collect(Collectors.toSet()), removedFriends);

        updateNotifications(context, now);

        save(context, dataId);

        notifyCloud(context, remoteProject, removedFriends);
    }

    public synchronized void createProject(@NonNull Context context, int dataId, @NonNull String name, @NonNull Set<String> friends) {
        MyCrashlytics.log("DomainFactory.createProject");

        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(mRemoteProjectFactory != null);
        Assert.assertTrue(mUserInfo != null);
        Assert.assertTrue(mRemoteRootUser != null);

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Set<String> recordOf = new HashSet<>(friends);

        String key = mUserInfo.getKey();
        Assert.assertTrue(!recordOf.contains(key));
        recordOf.add(key);

        RemoteProject remoteProject = mRemoteProjectFactory.createRemoteProject(name, now, recordOf, mRemoteRootUser);

        save(context, dataId);

        notifyCloud(context, remoteProject);
    }

    public synchronized void setProjectEndTimeStamps(@NonNull Context context, int dataId, @NonNull Set<String> projectIds) {
        MyCrashlytics.log("DomainFactory.setProjectEndTimeStamps");

        Assert.assertTrue(mRemoteProjectFactory != null);
        Assert.assertTrue(mUserInfo != null);
        Assert.assertTrue(!projectIds.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.getNow();

        Set<RemoteProject> remoteProjects = Stream.of(projectIds)
                .map(mRemoteProjectFactory::getRemoteProjectForce)
                .collect(Collectors.toSet());

        Assert.assertTrue(Stream.of(remoteProjects)
                .allMatch(remoteProject -> remoteProject.current(now)));

        Stream.of(remoteProjects)
                .forEach(remoteProject -> remoteProject.setEndExactTimeStamp(now));

        updateNotifications(context, now);

        save(context, dataId);

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
        if (instanceKey.mTaskKey.mLocalTaskId != null) {
            Assert.assertTrue(TextUtils.isEmpty(instanceKey.mTaskKey.mRemoteProjectId));
            Assert.assertTrue(TextUtils.isEmpty(instanceKey.mTaskKey.mRemoteTaskId));

            return mLocalFactory.getExistingInstanceIfPresent(instanceKey);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(instanceKey.mTaskKey.mRemoteProjectId));
            Assert.assertTrue(!TextUtils.isEmpty(instanceKey.mTaskKey.mRemoteTaskId));
            Assert.assertTrue(mRemoteProjectFactory != null);

            return mRemoteProjectFactory.getExistingInstanceIfPresent(instanceKey);
        }
    }

    @NonNull
    public String getRemoteCustomTimeId(@NonNull String projectId, @NonNull CustomTimeKey customTimeKey) {
        if (!TextUtils.isEmpty(customTimeKey.mRemoteProjectId)) {
            Assert.assertTrue(!TextUtils.isEmpty(customTimeKey.mRemoteCustomTimeId));
            Assert.assertTrue(customTimeKey.mLocalCustomTimeId == null);

            Assert.assertTrue(customTimeKey.mRemoteProjectId.equals(projectId));

            return customTimeKey.mRemoteCustomTimeId;
        } else {
            Assert.assertTrue(TextUtils.isEmpty(customTimeKey.mRemoteCustomTimeId));
            Assert.assertTrue(customTimeKey.mLocalCustomTimeId != null);

            LocalCustomTime localCustomTime = mLocalFactory.getLocalCustomTime(customTimeKey.mLocalCustomTimeId);

            Assert.assertTrue(localCustomTime.hasRemoteRecord(projectId));

            return localCustomTime.getRemoteId(projectId);
        }
    }

    @NonNull
    private Instance generateInstance(@NonNull TaskKey taskKey, @NonNull DateTime scheduleDateTime) {
        if (taskKey.mLocalTaskId != null) {
            Assert.assertTrue(TextUtils.isEmpty(taskKey.mRemoteProjectId));
            Assert.assertTrue(TextUtils.isEmpty(taskKey.mRemoteTaskId));

            return new LocalInstance(this, taskKey.mLocalTaskId, scheduleDateTime);
        } else {
            Assert.assertTrue(mRemoteProjectFactory != null);
            Assert.assertTrue(!TextUtils.isEmpty(taskKey.mRemoteProjectId));
            Assert.assertTrue(!TextUtils.isEmpty(taskKey.mRemoteTaskId));

            String remoteCustomTimeId;
            Integer hour;
            Integer minute;

            CustomTimeKey customTimeKey = scheduleDateTime.getTime().getTimePair().mCustomTimeKey;
            HourMinute hourMinute = scheduleDateTime.getTime().getTimePair().mHourMinute;

            if (customTimeKey != null) {
                Assert.assertTrue(hourMinute == null);

                remoteCustomTimeId = getRemoteCustomTimeId(taskKey.mRemoteProjectId, customTimeKey);

                hour = null;
                minute = null;
            } else {
                Assert.assertTrue(hourMinute != null);

                remoteCustomTimeId = null;

                hour = hourMinute.getHour();
                minute = hourMinute.getMinute();
            }

            InstanceShownRecord instanceShownRecord = mLocalFactory.getInstanceShownRecord(taskKey.mRemoteProjectId, taskKey.mRemoteTaskId, scheduleDateTime.getDate().getYear(), scheduleDateTime.getDate().getMonth(), scheduleDateTime.getDate().getDay(), remoteCustomTimeId, hour, minute);

            RemoteProject remoteProject = mRemoteProjectFactory.getTaskForce(taskKey).getRemoteProject();

            return new RemoteInstance(this, remoteProject, taskKey.mRemoteTaskId, scheduleDateTime, instanceShownRecord);
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

        DateTime dateTime = getDateTime(instanceKey.mScheduleKey.ScheduleDate, instanceKey.mScheduleKey.ScheduleTimePair);

        return generateInstance(instanceKey.mTaskKey, dateTime); // DateTime -> TimePair
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

    @Nullable
    Task getParentTask(@NonNull Task childTask, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(childTask.notDeleted(exactTimeStamp));

        TaskHierarchy parentTaskHierarchy = getParentTaskHierarchy(childTask, exactTimeStamp);
        if (parentTaskHierarchy == null) {
            return null;
        } else {
            Assert.assertTrue(parentTaskHierarchy.notDeleted(exactTimeStamp));
            Task parentTask = parentTaskHierarchy.getParentTask();
            Assert.assertTrue(parentTask.current(exactTimeStamp));
            return parentTask;
        }
    }

    @NonNull
    public CustomTime getCustomTime(@NonNull CustomTimeKey customTimeKey) {
        if (customTimeKey.mLocalCustomTimeId != null) {
            Assert.assertTrue(TextUtils.isEmpty(customTimeKey.mRemoteProjectId));
            Assert.assertTrue(TextUtils.isEmpty(customTimeKey.mRemoteCustomTimeId));

            return mLocalFactory.getLocalCustomTime(customTimeKey.mLocalCustomTimeId);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(customTimeKey.mRemoteProjectId));
            Assert.assertTrue(!TextUtils.isEmpty(customTimeKey.mRemoteCustomTimeId));
            Assert.assertTrue(mRemoteProjectFactory != null);

            return mRemoteProjectFactory.getRemoteCustomTime(customTimeKey.mRemoteProjectId, customTimeKey.mRemoteCustomTimeId);
        }
    }

    @NonNull
    private List<LocalCustomTime> getCurrentCustomTimes() {
        return mLocalFactory.getCurrentCustomTimes();
    }

    @NonNull
    private HashMap<InstanceKey, GroupListFragment.InstanceData> getChildInstanceDatas(@NonNull Instance instance, @NonNull ExactTimeStamp now, @NonNull GroupListFragment.InstanceDataParent instanceDataParent) {
        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();

        for (Instance childInstance : instance.getChildInstances(now)) {
            Task childTask = childInstance.getTask();

            Boolean isRootTask = (childTask.current(now) ? childTask.isRootTask(now) : null);

            GroupListFragment.InstanceData instanceData = new GroupListFragment.InstanceData(childInstance.getDone(), childInstance.getInstanceKey(), null, childInstance.getName(), childInstance.getInstanceDateTime().getTimeStamp(), childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), instanceDataParent, childInstance.getInstanceDateTime().getTime().getTimePair(), childTask.getNote(), childTask.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(childInstance, now, instanceData));
            instanceDatas.put(childInstance.getInstanceKey(), instanceData);
        }

        return instanceDatas;
    }

    @NonNull
    private Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> getChildTaskDatas(@NonNull ExactTimeStamp now, @NonNull Task parentTask, @NonNull Context context, @NonNull List<TaskKey> excludedTaskKeys) {
        return Stream.of(parentTask.getChildTasks(now))
                .filterNot(childTask -> excludedTaskKeys.contains(childTask.getTaskKey()))
                .collect(Collectors.toMap(childTask -> new CreateTaskLoader.TaskParentKey(childTask.getTaskKey()), childTask -> new CreateTaskLoader.ParentTreeData(childTask.getName(), getChildTaskDatas(now, childTask, context, excludedTaskKeys), new CreateTaskLoader.TaskParentKey(childTask.getTaskKey()), childTask.getScheduleText(context, now), childTask.getNote(), new CreateTaskLoader.TaskSortKey(childTask.getStartExactTimeStamp()))));
    }

    @NonNull
    private Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> getParentTreeDatas(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull List<TaskKey> excludedTaskKeys) {
        Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> parentTreeDatas = new HashMap<>();

        parentTreeDatas.putAll(Stream.of(mLocalFactory.getTasks())
                .filterNot(task -> excludedTaskKeys.contains(task.getTaskKey()))
                .filter(task -> task.current(now))
                .filter(task -> task.isVisible(now))
                .filter(task -> task.isRootTask(now))
                .collect(Collectors.toMap(task -> new CreateTaskLoader.TaskParentKey(task.getTaskKey()), task -> new CreateTaskLoader.ParentTreeData(task.getName(), getChildTaskDatas(now, task, context, excludedTaskKeys), new CreateTaskLoader.TaskParentKey(task.getTaskKey()), task.getScheduleText(context, now), task.getNote(), new CreateTaskLoader.TaskSortKey(task.getStartExactTimeStamp())))));

        if (mRemoteProjectFactory != null) {
            parentTreeDatas.putAll(Stream.of(mRemoteProjectFactory.getRemoteProjects())
                    .filter(remoteProject -> remoteProject.current(now))
                    .collect(Collectors.toMap(remoteProject -> new CreateTaskLoader.ProjectParentKey(remoteProject.getId()), remoteProject -> {
                        String users = Stream.of(remoteProject.getUsers())
                                .map(RemoteProjectUser::getName)
                                .collect(Collectors.joining(", "));

                        return new CreateTaskLoader.ParentTreeData(remoteProject.getName(), getProjectTaskTreeDatas(context, now, remoteProject, excludedTaskKeys), new CreateTaskLoader.ProjectParentKey(remoteProject.getId()), users, null, new CreateTaskLoader.ProjectSortKey(remoteProject.getId()));
                    })));
        }

        return parentTreeDatas;
    }

    @NonNull
    private Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> getProjectTaskTreeDatas(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull RemoteProject remoteProject, @NonNull List<TaskKey> excludedTaskKeys) {
        return Stream.of(remoteProject.getRemoteTasks())
                .filterNot(task -> excludedTaskKeys.contains(task.getTaskKey()))
                .filter(task -> task.current(now))
                .filter(task -> task.isVisible(now))
                .filter(task -> task.isRootTask(now))
                .collect(Collectors.toMap(task -> new CreateTaskLoader.TaskParentKey(task.getTaskKey()), task -> new CreateTaskLoader.ParentTreeData(task.getName(), getChildTaskDatas(now, task, context, excludedTaskKeys), new CreateTaskLoader.TaskParentKey(task.getTaskKey()), task.getScheduleText(context, now), task.getNote(), new CreateTaskLoader.TaskSortKey(task.getStartExactTimeStamp()))));
    }

    @NonNull
    public RemoteTask convertLocalToRemote(@NonNull Context context, @NonNull ExactTimeStamp now, @NonNull LocalTask startingLocalTask, @NonNull String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(projectId));

        Assert.assertTrue(mRemoteProjectFactory != null);
        Assert.assertTrue(mUserInfo != null);

        LocalToRemoteConversion localToRemoteConversion = new LocalToRemoteConversion();
        mLocalFactory.convertLocalToRemoteHelper(localToRemoteConversion, startingLocalTask);

        updateNotifications(context, true, now, Stream.of(localToRemoteConversion.mLocalTasks.values())
                .map(pair -> pair.first.getTaskKey())
                .collect(Collectors.toList()));

        RemoteProject remoteProject = mRemoteProjectFactory.getRemoteProjectForce(projectId);

        for (Pair<LocalTask, List<LocalInstance>> pair : localToRemoteConversion.mLocalTasks.values()) {
            Assert.assertTrue(pair != null);

            RemoteTask remoteTask = remoteProject.copyLocalTask(pair.first, pair.second, now);
            localToRemoteConversion.mRemoteTasks.put(pair.first.getId(), remoteTask);
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

        for (Pair<LocalTask, List<LocalInstance>> pair : localToRemoteConversion.mLocalTasks.values()) {
            Stream.of(pair.second)
                    .forEach(LocalInstance::delete);

            pair.first.delete();
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
            return Stream.concat(Stream.of(mLocalFactory.getTasks()), mRemoteProjectFactory.getTasks());
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
        if (taskKey.mLocalTaskId != null) {
            Assert.assertTrue(TextUtils.isEmpty(taskKey.mRemoteTaskId));

            return mLocalFactory.getTaskForce(taskKey.mLocalTaskId);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(taskKey.mRemoteTaskId));
            Assert.assertTrue(mRemoteProjectFactory != null);

            return mRemoteProjectFactory.getTaskForce(taskKey);
        }
    }

    @Nullable
    private Task getTaskIfPresent(@NonNull TaskKey taskKey) {
        if (taskKey.mLocalTaskId != null) {
            Assert.assertTrue(TextUtils.isEmpty(taskKey.mRemoteTaskId));

            return mLocalFactory.getTaskIfPresent(taskKey.mLocalTaskId);
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(taskKey.mRemoteTaskId));
            Assert.assertTrue(mRemoteProjectFactory != null);

            return mRemoteProjectFactory.getTaskIfPresent(taskKey);
        }
    }

    @NonNull
    List<Task> getChildTasks(@NonNull Task parentTask, @NonNull ExactTimeStamp exactTimeStamp) {
        Assert.assertTrue(parentTask.current(exactTimeStamp));

        return Stream.of(parentTask.getTaskHierarchiesByParentTaskKey(parentTask.getTaskKey()))
                .filter(taskHierarchy -> taskHierarchy.current(exactTimeStamp))
                .map(TaskHierarchy::getChildTask)
                .filter(childTask -> childTask.current(exactTimeStamp))
                .sortBy(Task::getStartExactTimeStamp)
                .collect(Collectors.toList());
    }

    @NonNull
    private List<TaskListFragment.ChildTaskData> getChildTaskDatas(@NonNull Task parentTask, @NonNull ExactTimeStamp now, @NonNull Context context) {
        return Stream.of(parentTask.getChildTasks(now))
                .sortBy(Task::getStartExactTimeStamp)
                .map(childTask -> new TaskListFragment.ChildTaskData(childTask.getName(), childTask.getScheduleText(context, now), getChildTaskDatas(childTask, now, context), childTask.getNote(), childTask.getStartExactTimeStamp(), childTask.getTaskKey()))
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
        return Stream.of(parentTask.getChildTasks(now))
                .map(childTask -> new GroupListFragment.TaskData(childTask.getTaskKey(), childTask.getName(), getChildTaskDatas(childTask, now), childTask.getStartExactTimeStamp(), childTask.getNote()))
                .collect(Collectors.toList());
    }

    @NonNull
    TaskListFragment.TaskData getMainData(@NonNull ExactTimeStamp now, @NonNull Context context, @Nullable TaskKey taskKey) {
        List<TaskListFragment.ChildTaskData> childTaskDatas;
        String note;

        if (taskKey != null) {
            Task task = getTaskForce(taskKey);

            List<Task> childTasks = task.getChildTasks(now);
            childTaskDatas = Stream.of(childTasks)
                    .map(childTask -> new TaskListFragment.ChildTaskData(childTask.getName(), childTask.getScheduleText(context, now), getChildTaskDatas(childTask, now, context), childTask.getNote(), childTask.getStartExactTimeStamp(), childTask.getTaskKey()))
                    .collect(Collectors.toList());

            note = task.getNote();
        } else {
            childTaskDatas = getTasks()
                    .filter(task -> task.current(now))
                    .filter(task -> task.isVisible(now))
                    .filter(task -> task.isRootTask(now))
                    .map(task -> new TaskListFragment.ChildTaskData(task.getName(), task.getScheduleText(context, now), getChildTaskDatas(task, now, context), task.getNote(), task.getStartExactTimeStamp(), task.getTaskKey()))
                    .collect(Collectors.toList());

            note = null;
        }

        Collections.sort(childTaskDatas, (TaskListFragment.ChildTaskData lhs, TaskListFragment.ChildTaskData rhs) -> lhs.mStartExactTimeStamp.compareTo(rhs.mStartExactTimeStamp));
        if (taskKey == null)
            Collections.reverse(childTaskDatas);

        return new TaskListFragment.TaskData(childTaskDatas, note);
    }

    @NonNull
    Instance setInstanceDone(@NonNull Context context, @NonNull ExactTimeStamp now, int dataId, @NonNull InstanceKey instanceKey, boolean done) {
        Instance instance = getInstance(instanceKey);

        instance.setDone(done, now);

        updateNotifications(context, now);

        save(context, dataId);

        notifyCloud(context, instance.getRemoteNullableProject());

        return instance;
    }

    @NonNull
    Irrelevant setIrrelevant(@NonNull ExactTimeStamp now) {
        List<Task> tasks = getTasks().collect(Collectors.toList());

        for (Task task : tasks)
            task.updateOldestVisible(now);

        // relevant hack
        Map<TaskKey, TaskRelevance> taskRelevances = Stream.of(tasks).collect(Collectors.toMap(Task::getTaskKey, TaskRelevance::new));

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
            Map<Pair<String, String>, RemoteCustomTimeRelevance> remoteCustomTimeRelevances = Stream.of(remoteCustomTimes).collect(Collectors.toMap(remoteCustomTime -> Pair.create(remoteCustomTime.getProjectId(), remoteCustomTime.getId()), RemoteCustomTimeRelevance::new));

            Collection<RemoteProject> remoteProjects = mRemoteProjectFactory.getRemoteProjects();
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

            new BackendNotifier(context, remoteProjects, mUserInfo, new ArrayList<>());
        }
    }

    private void notifyCloud(@NonNull Context context, @NonNull RemoteProject remoteProject, @NonNull Collection<String> userKeys) {
        Assert.assertTrue(mUserInfo != null);

        Set<RemoteProject> remoteProjects = Collections.singleton(remoteProject);

        new BackendNotifier(context, remoteProjects, mUserInfo, userKeys);
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

        Set<TaskKey> allTaskKeys = getTaskKeys();

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

        String message = "";

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            if (notificationInstances.size() > TickService.MAX_NOTIFICATIONS) { // show group
                if (shownInstanceKeys.size() > TickService.MAX_NOTIFICATIONS) { // group shown
                    if (!showInstanceKeys.isEmpty() || !hideInstanceKeys.isEmpty()) {
                        NotificationWrapper.getInstance().notifyGroup(context, this, notificationInstances.values(), silent, now, false);
                    } else {
                        NotificationWrapper.getInstance().notifyGroup(context, this, notificationInstances.values(), true, now, false);
                    }
                } else { // instances shown
                    for (InstanceKey shownInstanceKey : shownInstanceKeys) {
                        if (allTaskKeys.contains(shownInstanceKey.mTaskKey)) {
                            Instance shownInstance = getInstance(shownInstanceKey);

                            NotificationWrapper.getInstance().cancelNotification(context, shownInstance.getNotificationId());
                        } else {
                            Assert.assertTrue(instanceShownRecordNotificationDatas.containsKey(shownInstanceKey));

                            int notificationId = instanceShownRecordNotificationDatas.get(shownInstanceKey).first;

                            NotificationWrapper.getInstance().cancelNotification(context, notificationId);
                        }
                    }

                    NotificationWrapper.getInstance().notifyGroup(context, this, notificationInstances.values(), silent, now, false);
                }
            } else { // show instances
                if (shownInstanceKeys.size() > TickService.MAX_NOTIFICATIONS) { // group shown
                    NotificationWrapper.getInstance().cancelNotification(context, 0);

                    for (Instance instance : notificationInstances.values()) {
                        Assert.assertTrue(instance != null);

                        notifyInstance(context, instance, silent, now, false);
                    }
                } else { // instances shown
                    for (InstanceKey hideInstanceKey : hideInstanceKeys) {
                        if (allTaskKeys.contains(hideInstanceKey.mTaskKey)) {
                            Instance instance = getInstance(hideInstanceKey);

                            NotificationWrapper.getInstance().cancelNotification(context, instance.getNotificationId());
                        } else {
                            Assert.assertTrue(instanceShownRecordNotificationDatas.containsKey(hideInstanceKey));

                            int notificationId = instanceShownRecordNotificationDatas.get(hideInstanceKey).first;

                            NotificationWrapper.getInstance().cancelNotification(context, notificationId);
                        }
                    }

                    for (InstanceKey showInstanceKey : showInstanceKeys) {
                        Instance instance = notificationInstances.get(showInstanceKey);
                        Assert.assertTrue(instance != null);

                        notifyInstance(context, instance, silent, now, false);
                    }

                    Stream.of(notificationInstances.values())
                            .filter(instance -> !showInstanceKeys.contains(instance.getInstanceKey()))
                            .forEach(instance -> updateInstance(context, instance, now, false));
                }
            }
        } else {
            if (notificationInstances.isEmpty()) {
                message += ", hg";
                NotificationWrapper.getInstance().cancelNotification(context, 0);
            } else {
                message += ", sg";
                NotificationWrapper.getInstance().notifyGroup(context, this, notificationInstances.values(), true, now, true);
            }

            message += ", hiding " + hideInstanceKeys.size();
            for (InstanceKey hideInstanceKey : hideInstanceKeys) {
                if (allTaskKeys.contains(hideInstanceKey.mTaskKey)) {
                    Instance instance = getInstance(hideInstanceKey);

                    NotificationWrapper.getInstance().cancelNotification(context, instance.getNotificationId());
                } else {
                    Assert.assertTrue(instanceShownRecordNotificationDatas.containsKey(hideInstanceKey));

                    int notificationId = instanceShownRecordNotificationDatas.get(hideInstanceKey).first;

                    NotificationWrapper.getInstance().cancelNotification(context, notificationId);
                }
            }

            message += ", s " + showInstanceKeys.size();
            for (InstanceKey showInstanceKey : showInstanceKeys) {
                Instance instance = notificationInstances.get(showInstanceKey);
                Assert.assertTrue(instance != null);

                notifyInstance(context, instance, silent, now, true);
            }

            List<Instance> updateInstances = Stream.of(notificationInstances.values())
                    .filter(instance -> !showInstanceKeys.contains(instance.getInstanceKey()))
                    .collect(Collectors.toList());

            message += ", u " + updateInstances.size();
            Stream.of(updateInstances)
                    .forEach(instance -> updateInstance(context, instance, now, true));
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(TickService.TICK_PREFERENCES, Context.MODE_PRIVATE);
        Assert.assertTrue(sharedPreferences != null);

        String tickLog = sharedPreferences.getString(TickService.TICK_LOG, "");
        List<String> tickLogArr = Arrays.asList(TextUtils.split(tickLog, "\n"));
        List<String> tickLogArrTrimmed = new ArrayList<>(tickLogArr.subList(Math.max(tickLogArr.size() - 9, 0), tickLogArr.size()));
        tickLogArrTrimmed.add(now.toString() + " s? " + (silent ? "t" : "f") + message);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TickService.TICK_LOG, TextUtils.join("\n", tickLogArrTrimmed));

        if (!silent)
            editor.putLong(TickService.LAST_TICK_KEY, now.getLong());

        editor.apply();

        Optional<TimeStamp> minInstancesTimeStamp = Stream.of(getExistingInstances())
                .map(existingInstance -> existingInstance.getInstanceDateTime().getTimeStamp())
                .filter(timeStamp -> timeStamp.toExactTimeStamp().compareTo(now) > 0)
                .min(TimeStamp::compareTo);

        TimeStamp nextAlarm = null;
        if (minInstancesTimeStamp.isPresent())
            nextAlarm = minInstancesTimeStamp.get();

        Optional<TimeStamp> minSchedulesTimeStamp = getTasks()
                .filter(task -> task.current(now))
                .filter(task -> task.isRootTask(now))
                .flatMap(task -> Stream.of(task.getCurrentSchedules(now)))
                .map(schedule -> schedule.getNextAlarm(now))
                .filter(timeStamp -> timeStamp != null)
                .min(TimeStamp::compareTo);

        if (minSchedulesTimeStamp.isPresent() && (nextAlarm == null || nextAlarm.compareTo(minSchedulesTimeStamp.get()) > 0))
            nextAlarm = minSchedulesTimeStamp.get();

        PendingIntent pendingIntent = NotificationWrapper.getInstance().getPendingIntent(context);
        NotificationWrapper.getInstance().cancelAlarm(context, pendingIntent);

        if (nextAlarm != null) {
            Assert.assertTrue(nextAlarm.toExactTimeStamp().compareTo(now) > 0);

            NotificationWrapper.getInstance().setAlarm(context, pendingIntent, nextAlarm);
        }
    }

    private void notifyInstance(@NonNull Context context, @NonNull Instance instance, boolean silent, @NonNull ExactTimeStamp now, boolean nougat) {
        NotificationWrapper.getInstance().notifyInstance(context, this, instance, silent, now, nougat);

        if (!silent)
            mLastNotificationBeeps.put(instance.getInstanceKey(), SystemClock.elapsedRealtime());
    }

    private void updateInstance(@NonNull Context context, @NonNull Instance instance, @NonNull ExactTimeStamp now, boolean nougat) {
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

        NotificationWrapper.getInstance().notifyInstance(context, this, instance, true, now, nougat);
    }

    private void setInstanceNotified(@NonNull InstanceKey instanceKey, @NonNull ExactTimeStamp now) {
        if (instanceKey.getType() == TaskKey.Type.LOCAL) {
            Instance instance = getInstance(instanceKey);

            instance.setNotified(now);
            instance.setNotificationShown(false, now);
        } else {
            TaskKey taskKey = instanceKey.mTaskKey;

            String projectId = taskKey.mRemoteProjectId;
            Assert.assertTrue(!TextUtils.isEmpty(projectId));

            String taskId = taskKey.mRemoteTaskId;
            Assert.assertTrue(!TextUtils.isEmpty(taskId));

            ScheduleKey scheduleKey = instanceKey.mScheduleKey;
            Date scheduleDate = scheduleKey.ScheduleDate;

            Stream<InstanceShownRecord> stream = Stream.of(mLocalFactory.getInstanceShownRecords())
                    .filter(instanceShownRecord -> instanceShownRecord.getProjectId().equals(projectId))
                    .filter(instanceShownRecord -> instanceShownRecord.getTaskId().equals(taskId))
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleYear() == scheduleDate.getYear())
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleMonth() == scheduleDate.getMonth())
                    .filter(instanceShownRecord -> instanceShownRecord.getScheduleDay() == scheduleDate.getDay());

            List<InstanceShownRecord> matches;
            if (scheduleKey.ScheduleTimePair.mCustomTimeKey != null) {
                Assert.assertTrue(scheduleKey.ScheduleTimePair.mHourMinute == null);

                Assert.assertTrue(scheduleKey.ScheduleTimePair.mCustomTimeKey.getType() == TaskKey.Type.REMOTE); // remote custom time key hack
                Assert.assertTrue(scheduleKey.ScheduleTimePair.mCustomTimeKey.mLocalCustomTimeId == null);
                Assert.assertTrue(projectId.equals(scheduleKey.ScheduleTimePair.mCustomTimeKey.mRemoteProjectId));

                String customTimeId = scheduleKey.ScheduleTimePair.mCustomTimeKey.mRemoteCustomTimeId;
                Assert.assertTrue(!TextUtils.isEmpty(customTimeId));

                matches = stream.filter(instanceShownRecord -> customTimeId.equals(instanceShownRecord.getScheduleCustomTimeId()))
                        .collect(Collectors.toList());
            } else {
                Assert.assertTrue(scheduleKey.ScheduleTimePair.mHourMinute != null);

                HourMinute hourMinute = scheduleKey.ScheduleTimePair.mHourMinute;

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

        GroupListFragment.DataWrapper dataWrapper = new GroupListFragment.DataWrapper(customTimeDatas, null, null, null);

        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();
        for (Instance instance : currentInstances) {
            Task task = instance.getTask();

            Boolean isRootTask = (task.current(now) ? task.isRootTask(now) : null);

            GroupListFragment.InstanceData instanceData = new GroupListFragment.InstanceData(instance.getDone(), instance.getInstanceKey(), null, instance.getName(), instance.getInstanceDateTime().getTimeStamp(), task.current(now), instance.isRootInstance(now), isRootTask, instance.exists(), dataWrapper, instance.getInstanceDateTime().getTime().getTimePair(), task.getNote(), task.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(instance, now, instanceData));
            instanceDatas.put(instance.getInstanceKey(), instanceData);
        }

        dataWrapper.setInstanceDatas(instanceDatas);

        return dataWrapper;
    }

    @NonNull
    private GroupListFragment.DataWrapper getGroupListData(@NonNull Instance instance, @NonNull Task task, @NonNull ExactTimeStamp now) {
        HashMap<InstanceKey, GroupListFragment.InstanceData> instanceDatas = new HashMap<>();

        List<GroupListFragment.CustomTimeData> customTimeDatas = Stream.of(getCurrentCustomTimes())
                .map(customTime -> new GroupListFragment.CustomTimeData(customTime.getName(), customTime.getHourMinutes()))
                .collect(Collectors.toList());

        GroupListFragment.DataWrapper dataWrapper = new GroupListFragment.DataWrapper(customTimeDatas, task.current(now), null, task.getNote());

        for (Instance childInstance : instance.getChildInstances(now)) {
            Task childTask = childInstance.getTask();

            Boolean isRootTask = (childTask.current(now) ? childTask.isRootTask(now) : null);

            GroupListFragment.InstanceData instanceData = new GroupListFragment.InstanceData(childInstance.getDone(), childInstance.getInstanceKey(), null, childInstance.getName(), childInstance.getInstanceDateTime().getTimeStamp(), childTask.current(now), childInstance.isRootInstance(now), isRootTask, childInstance.exists(), dataWrapper, childInstance.getInstanceDateTime().getTime().getTimePair(), childTask.getNote(), childTask.getStartExactTimeStamp());
            instanceData.setChildren(getChildInstanceDatas(childInstance, now, instanceData));
            instanceDatas.put(childInstance.getInstanceKey(), instanceData);
        }

        dataWrapper.setInstanceDatas(instanceDatas);

        return dataWrapper;
    }

    static class Irrelevant {
        @NonNull
        final List<LocalCustomTime> mLocalCustomTimes;

        @NonNull
        final List<Task> mTasks;

        @NonNull
        final List<Instance> mInstances;

        @Nullable
        final List<RemoteCustomTime> mRemoteCustomTimes;

        @Nullable
        final List<RemoteProject> mRemoteProjects;

        Irrelevant(@NonNull List<LocalCustomTime> customTimes, @NonNull List<Task> tasks, @NonNull List<Instance> instances, @Nullable List<RemoteCustomTime> remoteCustomTimes, @Nullable List<RemoteProject> remoteProjects) {
            mLocalCustomTimes = customTimes;
            mTasks = tasks;
            mInstances = instances;
            mRemoteCustomTimes = remoteCustomTimes;
            mRemoteProjects = remoteProjects;
        }
    }

    private class TaskRelevance {
        @NonNull
        private final Task mTask;
        private boolean mRelevant = false;

        TaskRelevance(@NonNull Task task) {
            mTask = task;
        }

        void setRelevant(@NonNull Map<TaskKey, TaskRelevance> taskRelevances, @NonNull Map<InstanceKey, InstanceRelevance> instanceRelevances, @NonNull Map<Integer, LocalCustomTimeRelevance> customTimeRelevances, @NonNull ExactTimeStamp now) {
            if (mRelevant)
                return;

            mRelevant = true;

            TaskKey taskKey = mTask.getTaskKey();

            // mark parents relevant
            Stream.of(mTask.getTaskHierarchiesByChildTaskKey(taskKey))
                    .map(TaskHierarchy::getParentTaskKey)
                    .map(taskRelevances::get)
                    .forEach(taskRelevance -> taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            // mark children relevant
            Stream.of(mTask.getTaskHierarchiesByParentTaskKey(taskKey))
                    .map(TaskHierarchy::getChildTaskKey)
                    .map(taskRelevances::get)
                    .forEach(taskRelevance -> taskRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            Date oldestVisible = mTask.getOldestVisible();
            Assert.assertTrue(oldestVisible != null);

            // mark instances relevant
            Stream.of(getPastInstances(mTask, now))
                    .filter(instance -> instance.getScheduleDate().compareTo(oldestVisible) >= 0)
                    .map(instance -> {
                        InstanceKey instanceKey = instance.getInstanceKey();

                        if (!instanceRelevances.containsKey(instanceKey))
                            instanceRelevances.put(instanceKey, new InstanceRelevance(instance));

                        return instanceRelevances.get(instanceKey);
                    })
                    .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            Stream.of(mTask.getExistingInstances().values())
                    .filter(instance -> instance.getScheduleDate().compareTo(oldestVisible) >= 0)
                    .map(Instance::getInstanceKey)
                    .map(instanceRelevances::get)
                    .forEach(instanceRelevance -> instanceRelevance.setRelevant(taskRelevances, instanceRelevances, customTimeRelevances, now));

            // mark custom times relevant
            Stream.of(mTask.getSchedules())
                    .map(Schedule::getCustomTimeKey)
                    .filter(customTimeKey -> customTimeKey != null && customTimeKey.mLocalCustomTimeId != null)
                    .map(customTimeKey -> customTimeRelevances.get(customTimeKey.mLocalCustomTimeId))
                    .forEach(LocalCustomTimeRelevance::setRelevant);
        }

        boolean getRelevant() {
            return mRelevant;
        }

        public Task getTask() {
            return mTask;
        }

        void setRemoteRelevant(@NonNull Map<Pair<String, String>, RemoteCustomTimeRelevance> remoteCustomTimeRelevances, @NonNull Map<String, RemoteProjectRelevance> remoteProjectRelevances) {
            Assert.assertTrue(mRelevant);

            Stream.of(mTask.getSchedules())
                    .map(Schedule::getRemoteCustomTimeKey)
                    .filter(pair -> pair != null)
                    .map(remoteCustomTimeRelevances::get)
                    .forEach(RemoteCustomTimeRelevance::setRelevant);

            RemoteProject remoteProject = mTask.getRemoteNullableProject();
            if (remoteProject != null)
                remoteProjectRelevances.get(remoteProject.getId()).setRelevant();
        }
    }

    private static class InstanceRelevance {
        private final Instance mInstance;
        private boolean mRelevant = false;

        InstanceRelevance(@NonNull Instance instance) {
            mInstance = instance;
        }

        void setRelevant(@NonNull Map<TaskKey, TaskRelevance> taskRelevances, @NonNull Map<InstanceKey, InstanceRelevance> instanceRelevances, @NonNull Map<Integer, LocalCustomTimeRelevance> customTimeRelevances, @NonNull ExactTimeStamp now) {
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
                LocalCustomTimeRelevance localCustomTimeRelevance = customTimeRelevances.get(scheduleCustomTimeKey.mLocalCustomTimeId);
                Assert.assertTrue(localCustomTimeRelevance != null);

                localCustomTimeRelevance.setRelevant();
            }

            // set custom time relevant
            CustomTimeKey instanceCustomTimeId = mInstance.getInstanceCustomTimeKey();
            if (instanceCustomTimeId != null && instanceCustomTimeId.mLocalCustomTimeId != null) {
                LocalCustomTimeRelevance localCustomTimeRelevance = customTimeRelevances.get(instanceCustomTimeId.mLocalCustomTimeId);
                Assert.assertTrue(localCustomTimeRelevance != null);

                localCustomTimeRelevance.setRelevant();
            }
        }

        void setRemoteRelevant(@NonNull Map<Pair<String, String>, RemoteCustomTimeRelevance> remoteCustomTimeRelevances, @NonNull Map<String, RemoteProjectRelevance> remoteProjectRelevances) {
            Assert.assertTrue(mRelevant);

            Pair<String, String> pair = mInstance.getRemoteCustomTimeKey();
            RemoteProject remoteProject = mInstance.getRemoteNullableProject();
            if (pair != null) {
                Assert.assertTrue(remoteProject != null);

                remoteCustomTimeRelevances.get(pair).setRelevant();
            }

            if (remoteProject != null)
                remoteProjectRelevances.get(remoteProject.getId()).setRelevant();
        }

        boolean getRelevant() {
            return mRelevant;
        }

        public Instance getInstance() {
            return mInstance;
        }
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

    private static class LocalCustomTimeRelevance {
        @NonNull
        private final LocalCustomTime mLocalCustomTime;

        private boolean mRelevant = false;

        LocalCustomTimeRelevance(@NonNull LocalCustomTime localCustomTime) {
            mLocalCustomTime = localCustomTime;
        }

        void setRelevant() {
            mRelevant = true;
        }

        boolean getRelevant() {
            return mRelevant;
        }

        @NonNull
        LocalCustomTime getLocalCustomTime() {
            return mLocalCustomTime;
        }
    }

    private static class RemoteCustomTimeRelevance {
        @NonNull
        private final RemoteCustomTime mRemoteCustomTime;

        private boolean mRelevant = false;

        RemoteCustomTimeRelevance(@NonNull RemoteCustomTime remoteCustomTime) {
            mRemoteCustomTime = remoteCustomTime;
        }

        void setRelevant() {
            mRelevant = true;
        }

        boolean getRelevant() {
            return mRelevant;
        }

        @NonNull
        RemoteCustomTime getRemoteCustomTime() {
            return mRemoteCustomTime;
        }
    }

    private static class RemoteProjectRelevance {
        @NonNull
        private final RemoteProject mRemoteProject;

        private boolean mRelevant = false;

        RemoteProjectRelevance(@NonNull RemoteProject remoteProject) {
            mRemoteProject = remoteProject;
        }

        void setRelevant() {
            mRelevant = true;
        }

        boolean getRelevant() {
            return mRelevant;
        }

        @NonNull
        RemoteProject getRemoteProject() {
            return mRemoteProject;
        }
    }

    public static class LocalToRemoteConversion {
        public final Map<Integer, Pair<LocalTask, List<LocalInstance>>> mLocalTasks = new HashMap<>();
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

        public TickData(boolean silent, @NonNull String source, @NonNull Context context) {
            Assert.assertTrue(!TextUtils.isEmpty(source));

            mSilent = silent;
            mSource = source;

            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
            mWakelock.acquire(30 * 1000);
        }

        void release() {
            if (mWakelock.isHeld())
                mWakelock.release();
        }
    }
}
