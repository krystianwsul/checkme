package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.krystianwsul.checkme.domainmodel.local.LocalTask;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.persistencemodel.PersistenceManger;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMilli;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static org.mockito.Matchers.any;

@SuppressWarnings({"UnnecessaryLocalVariable", "ConstantConditions"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class, android.util.Log.class, Context.class})
public class DomainFactoryTest {
    @Mock
    private Context mContext;

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

        Task rootTask = domainFactory.getLocalFactory().createScheduleRootTask(domainFactory, startExactTimeStamp, "root task", Collections.singletonList(new CreateTaskLoader.SingleScheduleData(scheduleDate, new TimePair(scheduleHourMinute))), null);

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

        rootInstance = domainFactory.setInstanceDone(doneExactTimeStamp, rootInstance.getInstanceKey(), true);

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

        Assert.assertTrue(rootTask.getOldestVisible().equals(nextDayAfterDate));

        Assert.assertTrue(!rootTask.isVisible(nextDayAfterExactTimeStamp));

        Assert.assertTrue(!rootInstance.isVisible(nextDayAfterExactTimeStamp));

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

        LocalTask rootLocalTask = domainFactory.getLocalFactory().createScheduleRootTask(domainFactory, startExactTimeStamp, "root task", Collections.singletonList(new CreateTaskLoader.SingleScheduleData(scheduleDate, new TimePair(scheduleHourMinute))), null);

        Assert.assertTrue(rootLocalTask.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.get(0).Children.isEmpty());

        LocalTask childLocalTaskDone = domainFactory.getLocalFactory().createChildTask(domainFactory, startExactTimeStamp, rootLocalTask, "child task done", null);

        Assert.assertTrue(childLocalTaskDone.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.get(0).Children.size() == 1);

        LocalTask childLocalTaskExists = domainFactory.getLocalFactory().createChildTask(domainFactory, startExactTimeStamp, rootLocalTask, "child task exists", null);

        Assert.assertTrue(childLocalTaskExists.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.get(0).Children.size() == 2);

        LocalTask childLocalTaskDoesntExist = domainFactory.getLocalFactory().createChildTask(domainFactory, startExactTimeStamp, rootLocalTask, "child task doesn't exist", null);
        Assert.assertTrue(childLocalTaskDoesntExist.isVisible(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.get(0).Children.size() == 3);

        DateTime scheduleDateTime = new DateTime(startDate, new NormalTime(scheduleHourMinute));

        Instance rootInstance = domainFactory.getInstance(rootLocalTask, scheduleDateTime);

        Assert.assertTrue(!rootInstance.exists());
        Assert.assertTrue(rootInstance.isVisible(startExactTimeStamp));

        Date doneDate = startDate;
        HourMilli doneHourMilli = new HourMilli(1, 0, 0, 0);

        ExactTimeStamp doneExactTimeStamp = new ExactTimeStamp(doneDate, doneHourMilli);

        rootInstance = domainFactory.setInstanceDone(doneExactTimeStamp, rootInstance.getInstanceKey(), true);
        Assert.assertTrue(rootInstance.exists());

        Instance childInstanceDone = domainFactory.getInstance(childLocalTaskDone, scheduleDateTime);
        Assert.assertTrue(!childInstanceDone.exists());
        Assert.assertTrue(childInstanceDone.isVisible(doneExactTimeStamp));

        childInstanceDone = domainFactory.setInstanceDone(doneExactTimeStamp, childInstanceDone.getInstanceKey(), true);
        Assert.assertTrue(childInstanceDone.exists());

        Instance childInstanceExists = domainFactory.getInstance(childLocalTaskExists, scheduleDateTime);
        Assert.assertTrue(!childInstanceExists.exists());
        Assert.assertTrue(childInstanceExists.isVisible(doneExactTimeStamp));

        childInstanceExists = domainFactory.setInstanceDone(doneExactTimeStamp, childInstanceExists.getInstanceKey(), true);
        Assert.assertTrue(childInstanceExists.exists());

        childInstanceExists = domainFactory.setInstanceDone(doneExactTimeStamp, childInstanceExists.getInstanceKey(), false);
        Assert.assertTrue(childInstanceExists.exists());

        Instance childInstanceDoesntExist = domainFactory.getInstance(childLocalTaskDoesntExist, scheduleDateTime);
        Assert.assertTrue(!childInstanceDoesntExist.exists());
        Assert.assertTrue(childInstanceDoesntExist.isVisible(doneExactTimeStamp));

        Date nextDayBeforeDate = new Date(2016, 1, 2);
        HourMilli nextDayBeforeHourMilli = new HourMilli(0, 0, 0, 0);

        ExactTimeStamp nextDayBeforeExactTimeStamp = new ExactTimeStamp(nextDayBeforeDate, nextDayBeforeHourMilli);

        DomainFactory.Irrelevant irrelevantBefore = domainFactory.setIrrelevant(nextDayBeforeExactTimeStamp);
        Assert.assertTrue(irrelevantBefore.mCustomTimes.isEmpty());
        Assert.assertTrue(irrelevantBefore.mTasks.isEmpty());
        Assert.assertTrue(irrelevantBefore.mInstances.isEmpty());

        Assert.assertTrue(childLocalTaskDone.getOldestVisible().equals(startDate));
        Assert.assertTrue(rootLocalTask.getOldestVisible().equals(startDate));

        Assert.assertTrue(domainFactory.getTaskListData(nextDayBeforeExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(nextDayBeforeExactTimeStamp, mContext, null).mChildTaskDatas.get(0).Children.size() == 3);

        Date nextDayAfterDate = nextDayBeforeDate;
        HourMilli nextDayAfterHourMilli = new HourMilli(2, 0, 0, 0);

        ExactTimeStamp nextDayAfterExactTimeStamp = new ExactTimeStamp(nextDayAfterDate, nextDayAfterHourMilli);

        DomainFactory.Irrelevant irrelevantAfter = domainFactory.setIrrelevant(nextDayAfterExactTimeStamp);
        Assert.assertTrue(irrelevantAfter.mCustomTimes.isEmpty());
        Assert.assertTrue(irrelevantAfter.mTasks.size() == 4);
        Assert.assertTrue(irrelevantAfter.mInstances.size() == 3);

        Assert.assertTrue(childLocalTaskDone.getOldestVisible().equals(nextDayAfterDate));
        Assert.assertTrue(childLocalTaskExists.getOldestVisible().equals(nextDayAfterDate));
        Assert.assertTrue(childLocalTaskDoesntExist.getOldestVisible().equals(nextDayAfterDate));
        Assert.assertTrue(rootLocalTask.getOldestVisible().equals(nextDayAfterDate));

        Assert.assertTrue(!rootLocalTask.isVisible(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childLocalTaskDone.isVisible(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childLocalTaskExists.isVisible(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childLocalTaskDoesntExist.isVisible(nextDayAfterExactTimeStamp));

        Assert.assertTrue(!rootInstance.isVisible(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childInstanceDone.isVisible(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childInstanceExists.isVisible(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childInstanceDoesntExist.isVisible(nextDayAfterExactTimeStamp));

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

        Task singleTask = domainFactory.getLocalFactory().createScheduleRootTask(domainFactory, startExactTimeStamp, "single", Collections.singletonList(new CreateTaskLoader.SingleScheduleData(new Date(2016, 1, 1), new TimePair(new HourMinute(2, 0)))), null);

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).mChildTaskDatas.size() == 1);

        Task noReminderTask = domainFactory.getLocalFactory().createLocalTaskHelper(domainFactory, "no reminder", startExactTimeStamp, null);

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

        domainFactory.updateChildTask(new ExactTimeStamp(nextDay, new HourMilli(4, 0, 0, 0)), mContext, noReminderTask.getTaskKey(), noReminderTask.getName(), singleTask.getTaskKey(), noReminderTask.getNote());
        Assert.assertTrue(irrelevantNextDayBefore.mTasks.isEmpty());
        Assert.assertTrue(irrelevantNextDayBefore.mInstances.isEmpty());

        Assert.assertTrue(singleTask.getOldestVisible().equals(startDate));
        Assert.assertTrue(noReminderTask.getOldestVisible().equals(nextDay));

        Assert.assertTrue(domainFactory.getTaskListData(new ExactTimeStamp(nextDay, new HourMilli(5, 0, 0, 0)), mContext, null).mChildTaskDatas.size() == 1);
    }
}