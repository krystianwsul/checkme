package com.krystianwsul.checkme.domainmodel;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.persistencemodel.PersistenceManger;
import com.krystianwsul.checkme.persistencemodel.SaveService;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;

@SuppressWarnings({"UnnecessaryLocalVariable", "ConstantConditions"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class, android.util.Log.class, Context.class, ContentValues.class})
public class DomainFactoryTest {
    @Mock
    private Context mContext;

    @Mock
    private PendingIntent mPendingIntent;

    @BeforeClass
    public static void setUpStatic() {
        NotificationWrapper.setInstance(new NotificationWrapper() {
            @Override
            public void cancel(@NonNull Context context, int id) {

            }

            @Override
            public void notifyInstance(@NonNull Context context, @NonNull Instance instance, boolean silent, @NonNull ExactTimeStamp now) {

            }

            @Override
            public void notifyGroup(@NonNull Context context, @NonNull Collection<Instance> instances, boolean silent, @NonNull ExactTimeStamp now) {

            }

            @Override
            public void setAlarm(@NonNull Context context, @NonNull TimeStamp nextAlarm) {

            }
        });

        SaveService.Factory.setInstance(new SaveService.Factory() {
            @Override
            public void startService(@NonNull Context context, @NonNull PersistenceManger persistenceManger) {

            }
        });

        MyCrashlytics.initialize();
    }

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(TextUtils.class);
        PowerMockito.mockStatic(Log.class);

        PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                CharSequence a = (CharSequence) invocation.getArguments()[0];
                return !(a != null && a.length() > 0);
            }
        });
    }

    @SuppressWarnings("EmptyMethod")
    @After
    public void tearDown() throws Exception {

    }
    @Test
    public void testRelevantSingleNoChildren() throws Exception {
        PersistenceManger persistenceManger = new PersistenceManger();

        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        Date startDate = new Date(2016, 1, 1);
        HourMilli startHourMilli = new HourMilli(0, 0, 0, 0);

        ExactTimeStamp startExactTimeStamp = new ExactTimeStamp(startDate, startHourMilli);

        Date scheduleDate = startDate;
        HourMinute scheduleHourMinute = new HourMinute(2, 0);

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.isEmpty());

        Task rootTask = domainFactory.createScheduleRootTask(mContext, startExactTimeStamp, 0, "root task", Collections.singletonList(new CreateTaskLoader.SingleScheduleData(scheduleDate, new TimePair(scheduleHourMinute))), null, new ArrayList<>());

        Assert.assertTrue(rootTask.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.get(0).Children.isEmpty());

        DateTime scheduleDateTime = new DateTime(startDate, new NormalTime(scheduleHourMinute));

        Instance rootInstance = domainFactory.getInstance(rootTask, scheduleDateTime);

        Assert.assertTrue(!rootInstance.exists());
        Assert.assertTrue(rootInstance.isVisible(startExactTimeStamp));

        Date doneDate = startDate;
        HourMilli doneHourMilli = new HourMilli(1, 0, 0, 0);

        ExactTimeStamp doneExactTimeStamp = new ExactTimeStamp(doneDate, doneHourMilli);

        rootInstance = domainFactory.setInstanceDone(doneExactTimeStamp, mContext, rootInstance.getInstanceKey(), true);

        Assert.assertTrue(rootInstance.exists());

        Date nextDayBeforeDate = new Date(2016, 1, 2);
        HourMilli nextDayBeforeHourMilli = new HourMilli(0, 0, 0, 0);

        ExactTimeStamp nextDayBeforeExactTimeStamp = new ExactTimeStamp(nextDayBeforeDate, nextDayBeforeHourMilli);

        DomainFactory.Irrelevant irrelevantBefore = domainFactory.setIrrelevant(nextDayBeforeExactTimeStamp);
        Assert.assertTrue(irrelevantBefore.mCustomTimes.isEmpty());
        Assert.assertTrue(irrelevantBefore.mTasks.isEmpty());
        Assert.assertTrue(irrelevantBefore.mInstances.isEmpty());

        Assert.assertTrue(rootTask.getOldestVisible().equals(startDate));

        Assert.assertTrue(domainFactory.getTaskListData(nextDayBeforeExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(nextDayBeforeExactTimeStamp, mContext, null).mChildTaskDatas.get(0).Children.isEmpty());

        Date nextDayAfterDate = nextDayBeforeDate;
        HourMilli nextDayAfterHourMilli = new HourMilli(2, 0, 0, 0);

        ExactTimeStamp nextDayAfterExactTimeStamp = new ExactTimeStamp(nextDayAfterDate, nextDayAfterHourMilli);

        DomainFactory.Irrelevant irrelevantAfter = domainFactory.setIrrelevant(nextDayAfterExactTimeStamp);
        Assert.assertTrue(irrelevantAfter.mCustomTimes.isEmpty());
        Assert.assertTrue(irrelevantAfter.mTasks.size() == 1);
        Assert.assertTrue(irrelevantAfter.mInstances.size() == 1);

        domainFactory.removeIrrelevant(irrelevantAfter);

        Assert.assertTrue(domainFactory.getTaskListData(nextDayAfterExactTimeStamp, mContext, null).mChildTaskDatas.isEmpty());
    }

    @Test
    public void testRelevantSingleWithChildren() throws Exception {
        PersistenceManger persistenceManger = new PersistenceManger();

        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        Date startDate = new Date(2016, 1, 1);
        HourMilli startHourMilli = new HourMilli(0, 0, 0, 0);

        ExactTimeStamp startExactTimeStamp = new ExactTimeStamp(startDate, startHourMilli);

        Date scheduleDate = startDate;
        HourMinute scheduleHourMinute = new HourMinute(2, 0);

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.isEmpty());

        Task rootTask = domainFactory.createScheduleRootTask(mContext, startExactTimeStamp, 0, "root task", Collections.singletonList(new CreateTaskLoader.SingleScheduleData(scheduleDate, new TimePair(scheduleHourMinute))), null, new ArrayList<>());

        Assert.assertTrue(rootTask.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.get(0).Children.isEmpty());

        Task childTaskDone = domainFactory.createChildTask(mContext, startExactTimeStamp, 0, rootTask.getTaskKey(), "child task done", null);

        Assert.assertTrue(childTaskDone.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.get(0).Children.size() == 1);

        Task childTaskExists = domainFactory.createChildTask(mContext, startExactTimeStamp, 0, rootTask.getTaskKey(), "child task exists", null);

        Assert.assertTrue(childTaskExists.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.get(0).Children.size() == 2);

        Task childTaskDoesntExist = domainFactory.createChildTask(mContext, startExactTimeStamp, 0, rootTask.getTaskKey(), "child task doesn't exist", null);
        Assert.assertTrue(childTaskDoesntExist.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.get(0).Children.size() == 3);

        DateTime scheduleDateTime = new DateTime(startDate, new NormalTime(scheduleHourMinute));

        Instance rootInstance = domainFactory.getInstance(rootTask, scheduleDateTime);

        Assert.assertTrue(!rootInstance.exists());
        Assert.assertTrue(rootInstance.isVisible(startExactTimeStamp));

        Date doneDate = startDate;
        HourMilli doneHourMilli = new HourMilli(1, 0, 0, 0);

        ExactTimeStamp doneExactTimeStamp = new ExactTimeStamp(doneDate, doneHourMilli);

        rootInstance = domainFactory.setInstanceDone(doneExactTimeStamp, mContext, rootInstance.getInstanceKey(), true);
        Assert.assertTrue(rootInstance.exists());

        Instance childInstanceDone = domainFactory.getInstance(childTaskDone, scheduleDateTime);
        Assert.assertTrue(!childInstanceDone.exists());
        Assert.assertTrue(childInstanceDone.isVisible(doneExactTimeStamp));

        childInstanceDone = domainFactory.setInstanceDone(doneExactTimeStamp, mContext, childInstanceDone.getInstanceKey(), true);
        Assert.assertTrue(childInstanceDone.exists());

        Instance childInstanceExists = domainFactory.getInstance(childTaskExists, scheduleDateTime);
        Assert.assertTrue(!childInstanceExists.exists());
        Assert.assertTrue(childInstanceExists.isVisible(doneExactTimeStamp));

        childInstanceExists = domainFactory.setInstanceDone(doneExactTimeStamp, mContext, childInstanceExists.getInstanceKey(), true);
        Assert.assertTrue(childInstanceExists.exists());

        childInstanceExists = domainFactory.setInstanceDone(doneExactTimeStamp, mContext, childInstanceExists.getInstanceKey(), false);
        Assert.assertTrue(childInstanceExists.exists());

        Instance childInstanceDoesntExist = domainFactory.getInstance(childTaskDoesntExist, scheduleDateTime);
        Assert.assertTrue(!childInstanceDoesntExist.exists());
        Assert.assertTrue(childInstanceDoesntExist.isVisible(doneExactTimeStamp));

        Date nextDayBeforeDate = new Date(2016, 1, 2);
        HourMilli nextDayBeforeHourMilli = new HourMilli(0, 0, 0, 0);

        ExactTimeStamp nextDayBeforeExactTimeStamp = new ExactTimeStamp(nextDayBeforeDate, nextDayBeforeHourMilli);

        DomainFactory.Irrelevant irrelevantBefore = domainFactory.setIrrelevant(nextDayBeforeExactTimeStamp);
        Assert.assertTrue(irrelevantBefore.mCustomTimes.isEmpty());
        Assert.assertTrue(irrelevantBefore.mTasks.isEmpty());
        Assert.assertTrue(irrelevantBefore.mInstances.isEmpty());

        Assert.assertTrue(childTaskDone.getOldestVisible().equals(startDate));
        Assert.assertTrue(rootTask.getOldestVisible().equals(startDate));

        Assert.assertTrue(domainFactory.getTaskListData(nextDayBeforeExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(nextDayBeforeExactTimeStamp, mContext, null).mChildTaskDatas.get(0).Children.size() == 3);

        Date nextDayAfterDate = nextDayBeforeDate;
        HourMilli nextDayAfterHourMilli = new HourMilli(2, 0, 0, 0);

        ExactTimeStamp nextDayAfterExactTimeStamp = new ExactTimeStamp(nextDayAfterDate, nextDayAfterHourMilli);

        DomainFactory.Irrelevant irrelevantAfter = domainFactory.setIrrelevant(nextDayAfterExactTimeStamp);
        Assert.assertTrue(irrelevantAfter.mCustomTimes.isEmpty());
        Assert.assertTrue(irrelevantAfter.mTasks.size() == 4);
        Assert.assertTrue(irrelevantAfter.mInstances.size() == 3);

        domainFactory.removeIrrelevant(irrelevantAfter);

        Assert.assertTrue(domainFactory.getTaskListData(nextDayAfterExactTimeStamp, mContext, null).mChildTaskDatas.isEmpty());
    }

    @Test
    public void testRelevantSingleAndNoReminderNextDay() {
        PersistenceManger persistenceManger = new PersistenceManger();

        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        Date startDate = new Date(2016, 1, 1);
        ExactTimeStamp startExactTimeStamp = new ExactTimeStamp(startDate, new HourMilli(1, 0, 0, 0));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.isEmpty());

        Task singleTask = domainFactory.createScheduleRootTask(mContext, startExactTimeStamp, 0, "single", Collections.singletonList(new CreateTaskLoader.SingleScheduleData(new Date(2016, 1, 1), new TimePair(new HourMinute(2, 0)))), null, new ArrayList<>());

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);

        Task noReminderTask = domainFactory.createRootTask(mContext, startExactTimeStamp, 0, "no reminder", null, new ArrayList<>());

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.size() == 2);

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

        domainFactory.updateChildTask(mContext, new ExactTimeStamp(nextDay, new HourMilli(4, 0, 0, 0)), 0, noReminderTask.getTaskKey(), noReminderTask.getName(), singleTask.getTaskKey(), noReminderTask.getNote());
        Assert.assertTrue(irrelevantNextDayBefore.mTasks.isEmpty());
        Assert.assertTrue(irrelevantNextDayBefore.mInstances.isEmpty());

        Assert.assertTrue(singleTask.getOldestVisible().equals(startDate));
        Assert.assertTrue(noReminderTask.getOldestVisible().equals(nextDay));

        Assert.assertTrue(domainFactory.getTaskListData(new ExactTimeStamp(nextDay, new HourMilli(5, 0, 0, 0)), mContext, null).mChildTaskDatas.size() == 1);
    }

    @Test
    public void testJoinLeavesPreviousInstances() {
        PersistenceManger persistenceManger = new PersistenceManger();

        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        Date startDate = new Date(2016, 11, 9);
        HourMilli startHourMilli = new HourMilli(1, 0, 0, 0);

        ExactTimeStamp startExactTimeStamp = new ExactTimeStamp(startDate, startHourMilli);

        Assert.assertTrue(domainFactory.getGroupListData(mContext, startExactTimeStamp, 0, MainActivity.TimeRange.DAY).InstanceDatas.isEmpty());

        Date singleDate = startDate;
        TimePair singleTimePair = new TimePair(new HourMinute(2, 0));

        CreateTaskLoader.SingleScheduleData singleData = new CreateTaskLoader.SingleScheduleData(singleDate, singleTimePair);

        Task singleTask1 = domainFactory.createScheduleRootTask(mContext, startExactTimeStamp, 0, "singleTask1", Collections.singletonList(singleData), null, new ArrayList<>());
        Task singleTask2 = domainFactory.createScheduleRootTask(mContext, startExactTimeStamp, 0, "singleTask2", Collections.singletonList(singleData), null, new ArrayList<>());

        GroupListLoader.Data twoInstancesData = domainFactory.getGroupListData(mContext, new ExactTimeStamp(singleDate, new HourMilli(2, 0, 0, 0)), 0, MainActivity.TimeRange.DAY);
        Assert.assertTrue(twoInstancesData.InstanceDatas.size() == 2);

        ExactTimeStamp doneExactTimeStamp = new ExactTimeStamp(startDate, new HourMilli(3, 0, 0, 0));

        domainFactory.setInstanceDone(doneExactTimeStamp, mContext, twoInstancesData.InstanceDatas.values().iterator().next().InstanceKey, true);
        domainFactory.setInstanceDone(doneExactTimeStamp, mContext, twoInstancesData.InstanceDatas.values().iterator().next().InstanceKey, false);

        ExactTimeStamp joinExactTimeStamp = new ExactTimeStamp(singleDate, new HourMilli(4, 0, 0, 0));

        CreateTaskLoader.SingleScheduleData joinData = new CreateTaskLoader.SingleScheduleData(singleDate, new TimePair(new HourMinute(5, 0)));

        List<TaskKey> joinTaskKeys = Arrays.asList(singleTask1.getTaskKey(), singleTask2.getTaskKey());

        domainFactory.createScheduleJoinRootTask(mContext, joinExactTimeStamp, 0, "joinTask", Collections.singletonList(joinData), joinTaskKeys, null, new ArrayList<>());

        GroupListLoader.Data data = domainFactory.getGroupListData(mContext, new ExactTimeStamp(singleDate, new HourMilli(6, 0, 0, 0)), 0, MainActivity.TimeRange.DAY);

        Assert.assertTrue(data.InstanceDatas.size() == 3);
    }
}