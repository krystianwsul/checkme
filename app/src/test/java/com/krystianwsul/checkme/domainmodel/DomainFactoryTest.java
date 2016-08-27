package com.krystianwsul.checkme.domainmodel;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.krystianwsul.checkme.persistencemodel.PersistenceManger;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DateTime;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMili;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.NormalTime;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TextUtils.class, android.util.Log.class, Context.class})
public class DomainFactoryTest {
    @Mock
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(TextUtils.class);
        PowerMockito.mockStatic(Log.class);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testUpdateTaskOldestVisible() throws Exception {
        PersistenceManger persistenceManger = new PersistenceManger();

        DomainFactory domainFactory = new DomainFactory(persistenceManger);

        Date startDate = new Date(2016, 1, 1);
        HourMili startHourMili = new HourMili(0, 0, 0, 0);

        ExactTimeStamp startExactTimeStamp = new ExactTimeStamp(startDate, startHourMili);

        Date scheduleDate = startDate;
        HourMinute scheduleHourMinute = new HourMinute(2, 0);

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).TaskDatas.isEmpty());

        Task rootTask = domainFactory.createSingleScheduleRootTask(startExactTimeStamp, "root task", scheduleDate, new TimePair(scheduleHourMinute));
        Assert.assertTrue(rootTask != null);

        Assert.assertTrue(rootTask.isVisible(startExactTimeStamp));
        Assert.assertTrue(rootTask.isRelevant(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).TaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).TaskDatas.get(0).Children.isEmpty());

        Task childTaskDone = domainFactory.createChildTask(startExactTimeStamp, rootTask.getId(), "child task done");
        Assert.assertTrue(childTaskDone != null);

        Assert.assertTrue(childTaskDone.isVisible(startExactTimeStamp));
        Assert.assertTrue(childTaskDone.isRelevant(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).TaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).TaskDatas.get(0).Children.size() == 1);

        Task childTaskExists = domainFactory.createChildTask(startExactTimeStamp, rootTask.getId(), "child task exists");
        Assert.assertTrue(childTaskExists != null);

        Assert.assertTrue(childTaskExists.isVisible(startExactTimeStamp));
        Assert.assertTrue(childTaskExists.isRelevant(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).TaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).TaskDatas.get(0).Children.size() == 2);

        Task childTaskDoesntExist = domainFactory.createChildTask(startExactTimeStamp, rootTask.getId(), "child task doesn't exist");
        Assert.assertTrue(childTaskDoesntExist != null);

        Assert.assertTrue(childTaskDoesntExist.isVisible(startExactTimeStamp));
        Assert.assertTrue(childTaskDoesntExist.isRelevant(startExactTimeStamp));

        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).TaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(startExactTimeStamp, mContext, null).TaskDatas.get(0).Children.size() == 3);

        DateTime scheduleDateTime = new DateTime(startDate, new NormalTime(scheduleHourMinute));

        Instance rootInstance = domainFactory.getInstance(rootTask, scheduleDateTime);
        Assert.assertTrue(rootInstance != null);

        Assert.assertTrue(!rootInstance.exists());
        Assert.assertTrue(rootInstance.isVisible(startExactTimeStamp));
        Assert.assertTrue(rootInstance.isRelevant(startExactTimeStamp));

        Date doneDate = startDate;
        HourMili doneHourMili = new HourMili(1, 0, 0, 0);

        ExactTimeStamp doneExactTimeStamp = new ExactTimeStamp(doneDate, doneHourMili);

        rootInstance = domainFactory.setInstanceDone(doneExactTimeStamp, rootInstance.getInstanceKey(), true);
        Assert.assertTrue(rootInstance != null);

        Assert.assertTrue(rootInstance.exists());

        Instance childInstanceDone = domainFactory.getInstance(childTaskDone, scheduleDateTime);
        Assert.assertTrue(childInstanceDone != null);

        Assert.assertTrue(!childInstanceDone.exists());
        Assert.assertTrue(childInstanceDone.isVisible(doneExactTimeStamp));
        Assert.assertTrue(childInstanceDone.isRelevant(doneExactTimeStamp));

        childInstanceDone = domainFactory.setInstanceDone(doneExactTimeStamp, childInstanceDone.getInstanceKey(), true);
        Assert.assertTrue(childInstanceDone != null);

        Assert.assertTrue(childInstanceDone.exists());

        Instance childInstanceExists = domainFactory.getInstance(childTaskExists, scheduleDateTime);
        Assert.assertTrue(childInstanceExists != null);

        Assert.assertTrue(!childInstanceExists.exists());
        Assert.assertTrue(childInstanceExists.isVisible(doneExactTimeStamp));
        Assert.assertTrue(childInstanceExists.isRelevant(doneExactTimeStamp));

        childInstanceExists = domainFactory.setInstanceDone(doneExactTimeStamp, childInstanceExists.getInstanceKey(), true);
        Assert.assertTrue(childInstanceExists != null);

        Assert.assertTrue(childInstanceExists.exists());

        childInstanceExists = domainFactory.setInstanceDone(doneExactTimeStamp, childInstanceExists.getInstanceKey(), false);
        Assert.assertTrue(childInstanceExists != null);

        Assert.assertTrue(childInstanceExists.exists());

        Instance childInstanceDoesntExist = domainFactory.getInstance(childTaskDoesntExist, scheduleDateTime);
        Assert.assertTrue(childInstanceDoesntExist != null);

        Assert.assertTrue(!childInstanceDoesntExist.exists());
        Assert.assertTrue(childInstanceDoesntExist.isVisible(doneExactTimeStamp));
        Assert.assertTrue(childInstanceDoesntExist.isRelevant(doneExactTimeStamp));

        Date nextDayBeforeDate = new Date(2016, 1, 2);
        HourMili nextDayBeforeHourMili = new HourMili(0, 0, 0, 0);

        ExactTimeStamp nextDayBeforeExactTimeStamp = new ExactTimeStamp(nextDayBeforeDate, nextDayBeforeHourMili);

        DomainFactory.Irrelevant irrelevantBefore = domainFactory.setIrrelevant(nextDayBeforeExactTimeStamp);
        Assert.assertTrue(irrelevantBefore != null);
        Assert.assertTrue(irrelevantBefore.mCustomTimes.isEmpty());
        Assert.assertTrue(irrelevantBefore.mTasks.isEmpty());
        Assert.assertTrue(irrelevantBefore.mInstances.isEmpty());

        Assert.assertTrue(childTaskDone.getOldestVisible().equals(startDate));
        Assert.assertTrue(rootTask.getOldestVisible().equals(startDate));

        Assert.assertTrue(domainFactory.getTaskListData(nextDayBeforeExactTimeStamp, mContext, null).TaskDatas.size() == 1);
        Assert.assertTrue(domainFactory.getTaskListData(nextDayBeforeExactTimeStamp, mContext, null).TaskDatas.get(0).Children.size() == 3);

        Date nextDayAfterDate = nextDayBeforeDate;
        HourMili nextDayAfterHourMili = new HourMili(2, 0, 0, 0);

        ExactTimeStamp nextDayAfterExactTimeStamp = new ExactTimeStamp(nextDayAfterDate, nextDayAfterHourMili);

        DomainFactory.Irrelevant irrelevantAfter = domainFactory.setIrrelevant(nextDayAfterExactTimeStamp);
        Assert.assertTrue(irrelevantAfter != null);
        Assert.assertTrue(irrelevantAfter.mCustomTimes.isEmpty());
        Assert.assertTrue(irrelevantAfter.mTasks.size() == 4);
        Assert.assertTrue(irrelevantAfter.mInstances.size() == 3);

        Assert.assertTrue(childTaskDone.getOldestVisible().equals(nextDayAfterDate));
        Assert.assertTrue(childTaskExists.getOldestVisible().equals(nextDayAfterDate));
        Assert.assertTrue(childTaskDoesntExist.getOldestVisible().equals(nextDayAfterDate));
        Assert.assertTrue(rootTask.getOldestVisible().equals(nextDayAfterDate));

        Assert.assertTrue(!rootTask.isVisible(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childTaskDone.isVisible(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childTaskExists.isVisible(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childTaskDoesntExist.isVisible(nextDayAfterExactTimeStamp));

        Assert.assertTrue(!rootInstance.isVisible(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childInstanceDone.isVisible(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childInstanceExists.isVisible(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childInstanceDoesntExist.isVisible(nextDayAfterExactTimeStamp));

        Assert.assertTrue(!rootTask.isRelevant(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childTaskDone.isRelevant(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childInstanceDone.isRelevant(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childInstanceExists.isRelevant(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!childInstanceDoesntExist.isRelevant(nextDayAfterExactTimeStamp));
        Assert.assertTrue(!rootInstance.isRelevant(nextDayAfterExactTimeStamp));

        domainFactory.removeIrrelevant(irrelevantAfter);

        Assert.assertTrue(domainFactory.getTaskListData(nextDayAfterExactTimeStamp, mContext, null).TaskDatas.isEmpty());
    }
}