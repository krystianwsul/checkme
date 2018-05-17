package com.krystianwsul.checkme.domainmodel;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Stream;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.loaders.DayLoader;
import com.krystianwsul.checkme.persistencemodel.PersistenceManger;
import com.krystianwsul.checkme.persistencemodel.SaveService;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMilli;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;

@SuppressWarnings({"UnnecessaryLocalVariable", "ConstantConditions", "CanBeFinal"})
@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {TextUtils.class, Log.class, Context.class, ContentValues.class, SystemClock.class})
public class DomainFactoryTest {
    @Mock
    private Context mContext;

    @Mock
    private SharedPreferences mSharedPreferences;

    @Mock
    private SharedPreferences.Editor mEditor;

    @BeforeClass
    public static void setUpStatic() {
        NotificationWrapper.Companion.setInstance(new NotificationWrapper() {
            @Override
            public void cancelNotification(int id) {

            }

            @Override
            public void notifyInstance(@NonNull DomainFactory domainFactory, @NonNull Instance instance, boolean silent, @NonNull ExactTimeStamp now) {

            }

            @Override
            public void notifyGroup(@NonNull DomainFactory domainFactory, @NonNull Collection<? extends Instance> instances, boolean silent, @NonNull ExactTimeStamp now) {

            }

            @Override
            public void cleanGroup(@Nullable Integer lastNotificationId) {

            }

            @Override
            public void updateAlarm(TimeStamp nextAlarm) {

            }
        });

        SaveService.Factory.Companion.setInstance(new SaveService.Factory() {
            @Override
            public void startService(@NonNull Context context, @NonNull PersistenceManger persistenceManger, @NonNull SaveService.Source source) {

            }
        });
    }

    private PersistenceManger newPersistenceManger() {
        return new PersistenceManger();
    }

    @SuppressLint("CommitPrefEdits")
    @Before
    public void setUp() {
        PowerMockito.mockStatic(TextUtils.class);
        PowerMockito.mockStatic(Log.class);
        PowerMockito.mockStatic(SystemClock.class);

        PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(invocation -> {
            CharSequence a = (CharSequence) invocation.getArguments()[0];
            return !(a != null && a.length() > 0);
        });

        PowerMockito.when(TextUtils.split(anyString(), anyString())).thenReturn(new String[]{});

        PowerMockito.when(SystemClock.elapsedRealtime()).thenAnswer(new Answer<Long>() {
            private long mCounter = 0;

            @Override
            public Long answer(InvocationOnMock invocation) {
                return mCounter++;
            }
        });

        Mockito.when(mContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mSharedPreferences);

        Mockito.when(mSharedPreferences.getString(anyString(), anyString())).thenAnswer(invocation -> invocation.getArguments()[1]);

        Mockito.when(mSharedPreferences.edit()).thenReturn(mEditor);
        Mockito.when(mEditor.putLong(anyString(), anyLong())).thenReturn(mEditor);
    }

    @SuppressWarnings("EmptyMethod")
    @After
    public void tearDown() {

    }

    @Test
    public void testRelevantSingleNoChildren() {
        PersistenceManger persistenceManger = newPersistenceManger();

        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        Date startDate = new Date(2016, 1, 1);
        HourMilli startHourMilli = new HourMilli(0, 0, 0, 0);

        ExactTimeStamp startExactTimeStamp = new ExactTimeStamp(startDate, startHourMilli);

        Date scheduleDate = startDate;
        HourMinute scheduleHourMinute = new HourMinute(2, 0);

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().isEmpty());

        Task rootTask = domainFactory.createScheduleRootTask(mContext, startExactTimeStamp, 0, SaveService.Source.GUI, "root task", Collections.singletonList(new CreateTaskLoader.ScheduleData.SingleScheduleData(scheduleDate, new TimePair(scheduleHourMinute))), null, null);

        Assert.assertTrue(rootTask.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().size() == 1);
        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().get(0).getChildren().isEmpty());

        DateTime scheduleDateTime = new DateTime(startDate, new NormalTime(scheduleHourMinute));

        Instance rootInstance = domainFactory.getInstance(rootTask.getTaskKey(), scheduleDateTime);

        Assert.assertTrue(!rootInstance.exists());
        Assert.assertTrue(rootInstance.isVisible(startExactTimeStamp));

        Date doneDate = startDate;
        HourMilli doneHourMilli = new HourMilli(1, 0, 0, 0);

        ExactTimeStamp doneExactTimeStamp = new ExactTimeStamp(doneDate, doneHourMilli);

        rootInstance = domainFactory.setInstanceDone(mContext, doneExactTimeStamp, 0, SaveService.Source.GUI, rootInstance.getInstanceKey(), true);

        Assert.assertTrue(rootInstance.exists());

        Date nextDayBeforeDate = new Date(2016, 1, 2);
        HourMilli nextDayBeforeHourMilli = new HourMilli(0, 0, 0, 0);

        ExactTimeStamp nextDayBeforeExactTimeStamp = new ExactTimeStamp(nextDayBeforeDate, nextDayBeforeHourMilli);

        DomainFactory.Irrelevant irrelevantBefore = domainFactory.setIrrelevant(nextDayBeforeExactTimeStamp);
        Assert.assertTrue(irrelevantBefore.mLocalCustomTimes.isEmpty());
        Assert.assertTrue(irrelevantBefore.mTasks.isEmpty());
        Assert.assertTrue(irrelevantBefore.mInstances.isEmpty());

        Assert.assertTrue(rootTask.getOldestVisible().equals(startDate));

        Assert.assertTrue(domainFactory.getMainData(nextDayBeforeExactTimeStamp, mContext).getChildTaskDatas().size() == 1);
        Assert.assertTrue(domainFactory.getMainData(nextDayBeforeExactTimeStamp, mContext).getChildTaskDatas().get(0).getChildren().isEmpty());

        Date nextDayAfterDate = nextDayBeforeDate;
        HourMilli nextDayAfterHourMilli = new HourMilli(2, 0, 0, 0);

        ExactTimeStamp nextDayAfterExactTimeStamp = new ExactTimeStamp(nextDayAfterDate, nextDayAfterHourMilli);

        DomainFactory.Irrelevant irrelevantAfter = domainFactory.setIrrelevant(nextDayAfterExactTimeStamp);
        Assert.assertTrue(irrelevantAfter.mLocalCustomTimes.isEmpty());
        Assert.assertTrue(irrelevantAfter.mTasks.size() == 1);
        Assert.assertTrue(irrelevantAfter.mInstances.size() == 1);

        Assert.assertTrue(domainFactory.getMainData(nextDayAfterExactTimeStamp, mContext).getChildTaskDatas().isEmpty());
    }

    @Test
    public void testRelevantSingleWithChildren() {
        PersistenceManger persistenceManger = newPersistenceManger();

        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        Date startDate = new Date(2016, 1, 1);
        HourMilli startHourMilli = new HourMilli(0, 0, 0, 0);

        ExactTimeStamp startExactTimeStamp = new ExactTimeStamp(startDate, startHourMilli);

        Date scheduleDate = startDate;
        HourMinute scheduleHourMinute = new HourMinute(2, 0);

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().isEmpty());

        Task rootTask = domainFactory.createScheduleRootTask(mContext, startExactTimeStamp, 0, SaveService.Source.GUI, "root task", Collections.singletonList(new CreateTaskLoader.ScheduleData.SingleScheduleData(scheduleDate, new TimePair(scheduleHourMinute))), null, null);

        Assert.assertTrue(rootTask.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().size() == 1);
        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().get(0).getChildren().isEmpty());

        Task childTaskDone = domainFactory.createChildTask(mContext, startExactTimeStamp, 0, SaveService.Source.GUI, rootTask.getTaskKey(), "child task done", null);

        Assert.assertTrue(childTaskDone.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().size() == 1);
        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().get(0).getChildren().size() == 1);

        Task childTaskExists = domainFactory.createChildTask(mContext, startExactTimeStamp, 0, SaveService.Source.GUI, rootTask.getTaskKey(), "child task exists", null);

        Assert.assertTrue(childTaskExists.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().size() == 1);
        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().get(0).getChildren().size() == 2);

        Task childTaskDoesntExist = domainFactory.createChildTask(mContext, startExactTimeStamp, 0, SaveService.Source.GUI, rootTask.getTaskKey(), "child task doesn't exist", null);
        Assert.assertTrue(childTaskDoesntExist.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().size() == 1);
        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().get(0).getChildren().size() == 3);

        DateTime scheduleDateTime = new DateTime(startDate, new NormalTime(scheduleHourMinute));

        Instance rootInstance = domainFactory.getInstance(rootTask.getTaskKey(), scheduleDateTime);

        Assert.assertTrue(!rootInstance.exists());
        Assert.assertTrue(rootInstance.isVisible(startExactTimeStamp));

        Date doneDate = startDate;
        HourMilli doneHourMilli = new HourMilli(1, 0, 0, 0);

        ExactTimeStamp doneExactTimeStamp = new ExactTimeStamp(doneDate, doneHourMilli);

        rootInstance = domainFactory.setInstanceDone(mContext, doneExactTimeStamp, 0, SaveService.Source.GUI, rootInstance.getInstanceKey(), true);
        Assert.assertTrue(rootInstance.exists());

        Instance childInstanceDone = domainFactory.getInstance(childTaskDone.getTaskKey(), scheduleDateTime);
        Assert.assertTrue(!childInstanceDone.exists());
        Assert.assertTrue(childInstanceDone.isVisible(doneExactTimeStamp));

        childInstanceDone = domainFactory.setInstanceDone(mContext, doneExactTimeStamp, 0, SaveService.Source.GUI, childInstanceDone.getInstanceKey(), true);
        Assert.assertTrue(childInstanceDone.exists());

        Instance childInstanceExists = domainFactory.getInstance(childTaskExists.getTaskKey(), scheduleDateTime);
        Assert.assertTrue(!childInstanceExists.exists());
        Assert.assertTrue(childInstanceExists.isVisible(doneExactTimeStamp));

        childInstanceExists = domainFactory.setInstanceDone(mContext, doneExactTimeStamp, 0, SaveService.Source.GUI, childInstanceExists.getInstanceKey(), true);
        Assert.assertTrue(childInstanceExists.exists());

        childInstanceExists = domainFactory.setInstanceDone(mContext, doneExactTimeStamp, 0, SaveService.Source.GUI, childInstanceExists.getInstanceKey(), false);
        Assert.assertTrue(childInstanceExists.exists());

        Instance childInstanceDoesntExist = domainFactory.getInstance(childTaskDoesntExist.getTaskKey(), scheduleDateTime);
        Assert.assertTrue(!childInstanceDoesntExist.exists());
        Assert.assertTrue(childInstanceDoesntExist.isVisible(doneExactTimeStamp));

        Date nextDayBeforeDate = new Date(2016, 1, 2);
        HourMilli nextDayBeforeHourMilli = new HourMilli(0, 0, 0, 0);

        ExactTimeStamp nextDayBeforeExactTimeStamp = new ExactTimeStamp(nextDayBeforeDate, nextDayBeforeHourMilli);

        DomainFactory.Irrelevant irrelevantBefore = domainFactory.setIrrelevant(nextDayBeforeExactTimeStamp);
        Assert.assertTrue(irrelevantBefore.mLocalCustomTimes.isEmpty());
        Assert.assertTrue(irrelevantBefore.mTasks.isEmpty());
        Assert.assertTrue(irrelevantBefore.mInstances.isEmpty());

        Assert.assertTrue(childTaskDone.getOldestVisible().equals(startDate));
        Assert.assertTrue(rootTask.getOldestVisible().equals(startDate));

        Assert.assertTrue(domainFactory.getMainData(nextDayBeforeExactTimeStamp, mContext).getChildTaskDatas().size() == 1);
        Assert.assertTrue(domainFactory.getMainData(nextDayBeforeExactTimeStamp, mContext).getChildTaskDatas().get(0).getChildren().size() == 3);

        Date nextDayAfterDate = nextDayBeforeDate;
        HourMilli nextDayAfterHourMilli = new HourMilli(2, 0, 0, 0);

        ExactTimeStamp nextDayAfterExactTimeStamp = new ExactTimeStamp(nextDayAfterDate, nextDayAfterHourMilli);

        DomainFactory.Irrelevant irrelevantAfter = domainFactory.setIrrelevant(nextDayAfterExactTimeStamp);
        Assert.assertTrue(irrelevantAfter.mLocalCustomTimes.isEmpty());
        Assert.assertTrue(irrelevantAfter.mTasks.size() == 4);
        Assert.assertTrue(irrelevantAfter.mInstances.size() == 3);

        Assert.assertTrue(domainFactory.getMainData(nextDayAfterExactTimeStamp, mContext).getChildTaskDatas().isEmpty());
    }

    @Test
    public void testRelevantSingleAndNoReminderNextDay() {
        PersistenceManger persistenceManger = newPersistenceManger();

        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        Date startDate = new Date(2016, 1, 1);
        ExactTimeStamp startExactTimeStamp = new ExactTimeStamp(startDate, new HourMilli(1, 0, 0, 0));

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().isEmpty());

        Task singleTask = domainFactory.createScheduleRootTask(mContext, startExactTimeStamp, 0, SaveService.Source.GUI, "single", Collections.singletonList(new CreateTaskLoader.ScheduleData.SingleScheduleData(new Date(2016, 1, 1), new TimePair(new HourMinute(2, 0)))), null, null);

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().size() == 1);

        Task noReminderTask = domainFactory.createRootTask(mContext, startExactTimeStamp, 0, SaveService.Source.GUI, "no reminder", null, null);

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp, mContext).getChildTaskDatas().size() == 2);

        DomainFactory.Irrelevant irrelevantFirstDay = domainFactory.setIrrelevant(new ExactTimeStamp(startDate, new HourMilli(3, 0, 0, 0)));
        Assert.assertTrue(irrelevantFirstDay.mTasks.isEmpty());
        Assert.assertTrue(irrelevantFirstDay.mInstances.isEmpty());

        Assert.assertTrue(singleTask.getOldestVisible().equals(startDate));
        Assert.assertTrue(noReminderTask.getOldestVisible().equals(startDate));

        Date nextDay = new Date(2016, 1, 2);

        DomainFactory.Irrelevant irrelevantNextDayBefore = domainFactory.setIrrelevant(new ExactTimeStamp(nextDay, new HourMilli(3, 0, 0, 0)));
        Assert.assertTrue(irrelevantNextDayBefore.mTasks.isEmpty());
        Assert.assertTrue(irrelevantNextDayBefore.mInstances.isEmpty());

        Assert.assertTrue(singleTask.getOldestVisible().equals(startDate));
        Assert.assertTrue(noReminderTask.getOldestVisible().equals(nextDay));

        domainFactory.updateChildTask(mContext, new ExactTimeStamp(nextDay, new HourMilli(4, 0, 0, 0)), 0, SaveService.Source.GUI, noReminderTask.getTaskKey(), noReminderTask.getName(), singleTask.getTaskKey(), noReminderTask.getNote());
        Assert.assertTrue(irrelevantNextDayBefore.mTasks.isEmpty());
        Assert.assertTrue(irrelevantNextDayBefore.mInstances.isEmpty());

        Assert.assertTrue(singleTask.getOldestVisible().equals(startDate));
        Assert.assertTrue(noReminderTask.getOldestVisible().equals(nextDay));

        Assert.assertTrue(domainFactory.getMainData(new ExactTimeStamp(nextDay, new HourMilli(5, 0, 0, 0)), mContext).getChildTaskDatas().size() == 1);
    }

    @Test
    public void testJoinLeavesPreviousInstances() {
        PersistenceManger persistenceManger = newPersistenceManger();

        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        Date startDate = new Date(2016, 11, 9);
        HourMilli startHourMilli = new HourMilli(1, 0, 0, 0);

        ExactTimeStamp startExactTimeStamp = new ExactTimeStamp(startDate, startHourMilli);

        Assert.assertTrue(domainFactory.getGroupListData(mContext, startExactTimeStamp, 0, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().isEmpty());

        Date singleDate = startDate;
        TimePair singleTimePair = new TimePair(new HourMinute(2, 0));

        CreateTaskLoader.ScheduleData.SingleScheduleData singleData = new CreateTaskLoader.ScheduleData.SingleScheduleData(singleDate, singleTimePair);

        Task singleTask1 = domainFactory.createScheduleRootTask(mContext, startExactTimeStamp, 0, SaveService.Source.GUI, "singleTask1", Collections.singletonList(singleData), null, null);
        Task singleTask2 = domainFactory.createScheduleRootTask(mContext, startExactTimeStamp, 0, SaveService.Source.GUI, "singleTask2", Collections.singletonList(singleData), null, null);

        DayLoader.Data twoInstancesData = domainFactory.getGroupListData(mContext, new ExactTimeStamp(singleDate, new HourMilli(2, 0, 0, 0)), 0, MainActivity.TimeRange.DAY);
        Assert.assertTrue(twoInstancesData.getDataWrapper().getInstanceDatas().size() == 2);

        ExactTimeStamp doneExactTimeStamp = new ExactTimeStamp(startDate, new HourMilli(3, 0, 0, 0));

        domainFactory.setInstanceDone(mContext, doneExactTimeStamp, 0, SaveService.Source.GUI, twoInstancesData.getDataWrapper().getInstanceDatas().values().iterator().next().getInstanceKey(), true);
        domainFactory.setInstanceDone(mContext, doneExactTimeStamp, 0, SaveService.Source.GUI, twoInstancesData.getDataWrapper().getInstanceDatas().values().iterator().next().getInstanceKey(), false);

        ExactTimeStamp joinExactTimeStamp = new ExactTimeStamp(singleDate, new HourMilli(4, 0, 0, 0));

        CreateTaskLoader.ScheduleData.SingleScheduleData joinData = new CreateTaskLoader.ScheduleData.SingleScheduleData(singleDate, new TimePair(new HourMinute(5, 0)));

        List<TaskKey> joinTaskKeys = Arrays.asList(singleTask1.getTaskKey(), singleTask2.getTaskKey());

        domainFactory.createScheduleJoinRootTask(mContext, joinExactTimeStamp, 0, SaveService.Source.GUI, "joinTask", Collections.singletonList(joinData), joinTaskKeys, null, null);

        DayLoader.Data data = domainFactory.getGroupListData(mContext, new ExactTimeStamp(singleDate, new HourMilli(6, 0, 0, 0)), 0, MainActivity.TimeRange.DAY);

        Assert.assertTrue(data.getDataWrapper().getInstanceDatas().size() == 3);
    }

    @Test
    public void testSharedChild() {
        PersistenceManger persistenceManger = newPersistenceManger();
        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        // day 1:

        // hour 0: check no instances for day 1
        // hour 1: create firstTask scheduled for day 1, hour 12
        // hour 2: check 1 instance for day 1, no children
        // hour 3: add childTask to firstTask
        // hour 4: check 1 instance for day 1, 1 child
        // hour 5: create secondTask scheduled for day 2, hour 12
        // hour 6: check 1 instance for range 1 doesn't exist, 1 not done child doesn't exist; 1 instance for range 2, no children
        // hour 7: mark childTask done in secondTask instance
        // hour 8: check 1 instance for range 1 exists, 1 done child exists
        // hour 12: update notifications
        // hour 13: change childTask parent to secondTask
        // hour 14: check 1 not done instance for range 1, 1 child; 1 instance for range 2, 1 child
        // * hour 15: mark firstTask done
        // hour 16: check 1 done instance for range 1

        // day 2:

        // hour 0: check 2 instances for range 1, each with one child, secondTask not done, doesn't exist
        // * hour 1: mark secondTask done
        // hour 2: check 2 instances for range 1, each with one child, secondTask done, exists
        // hour 16: update notifications
        // hour 17: check 1 task
        // hour 18: check 1 instance for range 1

        Date day1 = new Date(2016, 1, 1);
        Date day2 = new Date(2016, 1, 2);

        int range1 = 0;
        int range2 = 1;

        HourMinute hour0 = new HourMinute(0, 0);
        HourMinute hour1 = new HourMinute(1, 0);
        HourMinute hour2 = new HourMinute(2, 0);
        HourMinute hour3 = new HourMinute(3, 0);
        HourMinute hour4 = new HourMinute(4, 0);
        HourMinute hour5 = new HourMinute(5, 0);
        HourMinute hour6 = new HourMinute(6, 0);
        HourMinute hour7 = new HourMinute(7, 0);
        HourMinute hour8 = new HourMinute(8, 0);
        HourMinute hour12 = new HourMinute(12, 0);
        HourMinute hour13 = new HourMinute(13, 0);
        HourMinute hour14 = new HourMinute(14, 0);
        HourMinute hour15 = new HourMinute(15, 0);
        HourMinute hour16 = new HourMinute(16, 0);
        HourMinute hour17 = new HourMinute(17, 0);
        HourMinute hour18 = new HourMinute(18, 0);

        int dataId = 0;

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour0.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().isEmpty());

        CreateTaskLoader.ScheduleData.SingleScheduleData firstScheduleData = new CreateTaskLoader.ScheduleData.SingleScheduleData(day1, new TimePair(hour12));
        Task firstTask = domainFactory.createScheduleRootTask(mContext, new ExactTimeStamp(day1, hour1.toHourMilli()), dataId, SaveService.Source.GUI, "firstTask", Collections.singletonList(firstScheduleData), null, null);

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour2.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour2.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().isEmpty());

        Task childTask = domainFactory.createChildTask(mContext, new ExactTimeStamp(day1, hour3.toHourMilli()), dataId, SaveService.Source.GUI, firstTask.getTaskKey(), "childTask", null);

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour4.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour4.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().size() == 1);

        CreateTaskLoader.ScheduleData.SingleScheduleData secondScheduleData = new CreateTaskLoader.ScheduleData.SingleScheduleData(day2, new TimePair(hour12));
        Task secondTask = domainFactory.createScheduleRootTask(mContext, new ExactTimeStamp(day1, hour5.toHourMilli()), dataId, SaveService.Source.GUI, "secondTask", Collections.singletonList(secondScheduleData), null, null);

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour6.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(!domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour6.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getExists());
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour6.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour6.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour6.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().values().iterator().next().getDone() == null);
        Assert.assertTrue(!domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour6.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().values().iterator().next().getExists());
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour6.toHourMilli()), range2, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour6.toHourMilli()), range2, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().isEmpty());

        InstanceKey childTaskInFirstTaskInstanceKey = domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour7.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().keySet().iterator().next();
        domainFactory.setInstanceDone(mContext, new ExactTimeStamp(day1, hour7.toHourMilli()), dataId, SaveService.Source.GUI, childTaskInFirstTaskInstanceKey, true);

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour8.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour8.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getExists());
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour8.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour8.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().values().iterator().next().getDone() != null);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour8.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().values().iterator().next().getExists());

        {
            DomainFactory.Irrelevant irrelevant = domainFactory.updateNotificationsTick(mContext, new ExactTimeStamp(day1, hour12.toHourMilli()), SaveService.Source.GUI, false);
            Assert.assertTrue(irrelevant.mTasks.isEmpty());
            Assert.assertTrue(irrelevant.mInstances.isEmpty());
        }

        domainFactory.updateChildTask(mContext, new ExactTimeStamp(day1, hour13.toHourMilli()), dataId, SaveService.Source.GUI, childTask.getTaskKey(), childTask.getName(), secondTask.getTaskKey(), childTask.getNote());

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour14.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour14.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getDone() == null);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour14.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour14.toHourMilli()), range2, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour14.toHourMilli()), range2, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().size() == 1);

        InstanceKey firstTaskInstanceKey = domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour15.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().keySet().iterator().next();
        domainFactory.setInstanceDone(mContext, new ExactTimeStamp(day1, hour15.toHourMilli()), dataId, SaveService.Source.GUI, firstTaskInstanceKey, true);

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour16.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour16.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getDone() != null);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour16.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour16.toHourMilli()), range2, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour16.toHourMilli()), range2, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().size() == 1);

        InstanceKey secondTaskInstanceKey = domainFactory.getGroupListData(mContext, new ExactTimeStamp(day1, hour16.toHourMilli()), range2, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().keySet().iterator().next();

        // works up to here

        {
            DayLoader.Data data = domainFactory.getGroupListData(mContext, new ExactTimeStamp(day2, hour0.toHourMilli()), range1, MainActivity.TimeRange.DAY);

            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().size() == 2);
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().get(firstTaskInstanceKey).getChildren().size() == 1);
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().get(secondTaskInstanceKey).getDone() == null);
            Assert.assertTrue(!data.getDataWrapper().getInstanceDatas().get(secondTaskInstanceKey).getExists());
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().get(secondTaskInstanceKey).getChildren().size() == 1);
        }

        domainFactory.setInstanceDone(mContext, new ExactTimeStamp(day2, hour1.toHourMilli()), dataId, SaveService.Source.GUI, secondTaskInstanceKey, true);

        {
            DayLoader.Data data = domainFactory.getGroupListData(mContext, new ExactTimeStamp(day2, hour2.toHourMilli()), range1, MainActivity.TimeRange.DAY);

            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().size() == 2);
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().get(firstTaskInstanceKey).getChildren().size() == 1);
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().get(secondTaskInstanceKey).getDone() != null);
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().get(secondTaskInstanceKey).getExists());
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().get(secondTaskInstanceKey).getChildren().size() == 1);
        }

        {
            DomainFactory.Irrelevant irrelevant = domainFactory.updateNotificationsTick(mContext, new ExactTimeStamp(day2, hour16.toHourMilli()), SaveService.Source.GUI, false);
            Assert.assertTrue(irrelevant.mTasks.isEmpty());
            Assert.assertTrue(irrelevant.mInstances.size() == 2);
        }

        Assert.assertTrue(domainFactory.getMainData(new ExactTimeStamp(day2, hour17.toHourMilli()), mContext).getChildTaskDatas().size() == 1);

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day2, hour18.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(day2, hour18.toHourMilli()), range1, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().values().iterator().next().getChildren().size() == 1);
    }

    @Test
    public void testChildAddedToInstanceInPast() {
        PersistenceManger persistenceManger = newPersistenceManger();
        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        // hour 0: check no instances
        // hour 1: add parent for hour 3
        // hour 2: check one instance, no children
        // hour 3: tick, check one instance, no children
        // hour 4: add child to parent
        // hour 5: check one instance, one child

        Date date = new Date(2016, 1, 1);

        HourMinute hour0 = new HourMinute(0, 0);
        HourMinute hour1 = new HourMinute(1, 0);
        HourMinute hour2 = new HourMinute(2, 0);
        HourMinute hour3 = new HourMinute(3, 0);
        HourMinute hour4 = new HourMinute(4, 0);
        HourMinute hour5 = new HourMinute(5, 0);

        int dataId = 0;
        int range = 0;

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour0.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().isEmpty());

        CreateTaskLoader.ScheduleData.SingleScheduleData scheduleData = new CreateTaskLoader.ScheduleData.SingleScheduleData(date, new TimePair(hour3));
        Task parentTask = domainFactory.createScheduleRootTask(mContext, new ExactTimeStamp(date, hour1.toHourMilli()), dataId, SaveService.Source.GUI, "parent", Collections.singletonList(scheduleData), null, null);

        InstanceKey parentInstanceKey;
        {
            DayLoader.Data data = domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour2.toHourMilli()), range, MainActivity.TimeRange.DAY);
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().size() == 1);

            parentInstanceKey = data.getDataWrapper().getInstanceDatas().keySet().iterator().next();
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().get(parentInstanceKey).getChildren().isEmpty());
        }

        domainFactory.updateNotificationsTick(mContext, new ExactTimeStamp(date, hour3.toHourMilli()), SaveService.Source.GUI, false);

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour3.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour3.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().get(parentInstanceKey).getChildren().isEmpty());

        domainFactory.createChildTask(mContext, new ExactTimeStamp(date, hour4.toHourMilli()), dataId, SaveService.Source.GUI, parentTask.getTaskKey(), "child", null);

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour5.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour5.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().get(parentInstanceKey).getChildren().size() == 1);
    }

    @Test
    public void testTaskAddedToParentKeepsOldRootInstance() {
        // hour 0: check no instances
        // hour 1: create task split for hour 2
        // hour 2: notify, check one instance, no children
        // hour 3: create task parent for hour 7
        // hour 4: check two instances, no children
        // hour 5: edit task split, set parent parent
        // hour 6: check two instances, parent has one child

        PersistenceManger persistenceManger = newPersistenceManger();
        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        Date date = new Date(2016, 1, 1);

        HourMinute hour0 = new HourMinute(0, 0);
        HourMinute hour1 = new HourMinute(1, 0);
        HourMinute hour2 = new HourMinute(2, 0);
        HourMinute hour3 = new HourMinute(3, 0);
        HourMinute hour4 = new HourMinute(4, 0);
        HourMinute hour5 = new HourMinute(5, 0);
        HourMinute hour6 = new HourMinute(6, 0);
        HourMinute hour7 = new HourMinute(7, 0);

        int dataId = 0;
        int range = 0;

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour0.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().isEmpty());

        CreateTaskLoader.ScheduleData.SingleScheduleData splitScheduleData = new CreateTaskLoader.ScheduleData.SingleScheduleData(date, new TimePair(hour2));
        Task splitTask = domainFactory.createScheduleRootTask(mContext, new ExactTimeStamp(date, hour1.toHourMilli()), dataId, SaveService.Source.GUI, "split", Collections.singletonList(splitScheduleData), null, null);

        domainFactory.updateNotificationsTick(mContext, new ExactTimeStamp(date, hour2.toHourMilli()), SaveService.Source.GUI, false);

        InstanceKey splitInstanceKey;
        {
            DayLoader.Data data = domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour2.toHourMilli()), range, MainActivity.TimeRange.DAY);
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().size() == 1);

            splitInstanceKey = data.getDataWrapper().getInstanceDatas().keySet().iterator().next();
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().get(splitInstanceKey).getChildren().isEmpty());
        }

        CreateTaskLoader.ScheduleData.SingleScheduleData parentScheduleData = new CreateTaskLoader.ScheduleData.SingleScheduleData(date, new TimePair(hour7));
        Task parentTask = domainFactory.createScheduleRootTask(mContext, new ExactTimeStamp(date, hour3.toHourMilli()), dataId, SaveService.Source.GUI, "parent", Collections.singletonList(parentScheduleData), null, null);

        InstanceKey parentInstanceKey;
        {
            DayLoader.Data data = domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour4.toHourMilli()), range, MainActivity.TimeRange.DAY);
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().size() == 2);

            parentInstanceKey = Stream.of(data.getDataWrapper().getInstanceDatas().keySet())
                    .filter(instanceKey -> !instanceKey.equals(splitInstanceKey))
                    .findFirst()
                    .get();

            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().get(splitInstanceKey).getChildren().isEmpty());
            Assert.assertTrue(data.getDataWrapper().getInstanceDatas().get(parentInstanceKey).getChildren().isEmpty());
        }

        domainFactory.updateChildTask(mContext, new ExactTimeStamp(date, hour5.toHourMilli()), dataId, SaveService.Source.GUI, splitTask.getTaskKey(), splitTask.getName(), parentTask.getTaskKey(), splitTask.getNote());

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour6.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 2);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour6.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().get(splitInstanceKey).getChildren().isEmpty());
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour6.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().get(parentInstanceKey).getChildren().size() == 1);
    }

    @Test
    public void testSingleScheduleChanged() {
        // hour 0: check no instances
        // hour 1: create task for hour 5
        // hour 2: check one instance, hour 5
        // hour 3: edit task, hour 6
        // hour 4: check one instance, hour 6

        PersistenceManger persistenceManger = newPersistenceManger();
        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        Date date = new Date(2016, 1, 1);

        HourMinute hour0 = new HourMinute(0, 0);
        HourMinute hour1 = new HourMinute(1, 0);
        HourMinute hour2 = new HourMinute(2, 0);
        HourMinute hour3 = new HourMinute(3, 0);
        HourMinute hour4 = new HourMinute(4, 0);
        HourMinute hour5 = new HourMinute(5, 0);
        HourMinute hour6 = new HourMinute(6, 0);

        int dataId = 0;
        int range = 0;

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour0.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().isEmpty());

        CreateTaskLoader.ScheduleData.SingleScheduleData firstScheduleData = new CreateTaskLoader.ScheduleData.SingleScheduleData(date, new TimePair(hour5));
        Task task = domainFactory.createScheduleRootTask(mContext, new ExactTimeStamp(date, hour1.toHourMilli()), dataId, SaveService.Source.GUI, "task", Collections.singletonList(firstScheduleData), null, null);

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour2.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour2.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().keySet().iterator().next().getScheduleKey().getScheduleTimePair().getHourMinute().equals(hour5));

        CreateTaskLoader.ScheduleData.SingleScheduleData secondScheduleData = new CreateTaskLoader.ScheduleData.SingleScheduleData(date, new TimePair(hour6));
        domainFactory.updateScheduleTask(mContext, new ExactTimeStamp(date, hour3.toHourMilli()), dataId, SaveService.Source.GUI, task.getTaskKey(), task.getName(), Collections.singletonList(secondScheduleData), task.getNote(), null);

        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour4.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().size() == 1);
        Assert.assertTrue(domainFactory.getGroupListData(mContext, new ExactTimeStamp(date, hour4.toHourMilli()), range, MainActivity.TimeRange.DAY).getDataWrapper().getInstanceDatas().keySet().iterator().next().getScheduleKey().getScheduleTimePair().getHourMinute().equals(hour6));
    }
}