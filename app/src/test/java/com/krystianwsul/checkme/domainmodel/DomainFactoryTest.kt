package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.persistencemodel.PersistenceManger
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.Matchers.*
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(value = [(TextUtils::class), (Log::class), (Context::class), (ContentValues::class), (SystemClock::class)])
class DomainFactoryTest {

    companion object {

        @BeforeClass
        @JvmStatic
        fun setUpStatic() {
            NotificationWrapper.instance = object : NotificationWrapper() {

                override fun cancelNotification(id: Int) = Unit

                override fun notifyInstance(domainFactory: DomainFactory, instance: Instance, silent: Boolean, now: ExactTimeStamp) = Unit

                override fun notifyGroup(domainFactory: DomainFactory, instances: Collection<Instance>, silent: Boolean, now: ExactTimeStamp) = Unit

                override fun cleanGroup(lastNotificationId: Int?) = Unit

                override fun updateAlarm(nextAlarm: TimeStamp?) = Unit
            }

            SaveService.Factory.instance = object : SaveService.Factory() {

                override fun startService(persistenceManger: PersistenceManger, source: SaveService.Source) = Unit
            }
        }
    }

    @Mock
    private val mContext: Context? = null

    @Mock
    private val mSharedPreferences: SharedPreferences? = null

    @Mock
    private val mEditor: SharedPreferences.Editor? = null

    private fun newPersistenceManger() = PersistenceManger()

    @SuppressLint("CommitPrefEdits")
    @Before
    fun setUp() {
        PowerMockito.mockStatic(TextUtils::class.java)
        PowerMockito.mockStatic(Log::class.java)
        PowerMockito.mockStatic(SystemClock::class.java)

        PowerMockito.`when`(TextUtils.isEmpty(any(CharSequence::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as? CharSequence).let { a ->
                !(a != null && a.isNotEmpty())
            }
        }

        PowerMockito.`when`(TextUtils.split(anyString(), anyString())).thenReturn(arrayOf())

        PowerMockito.`when`(SystemClock.elapsedRealtime()).thenAnswer(object : Answer<Long> {

            private var mCounter: Long = 0

            override fun answer(invocation: InvocationOnMock) = mCounter++
        })

        Mockito.`when`(mContext!!.getSharedPreferences(anyString(), anyInt())).thenReturn(mSharedPreferences)

        Mockito.`when`(mSharedPreferences!!.getString(anyString(), anyString())).thenAnswer { invocation -> invocation.arguments[1] }

        Mockito.`when`(mSharedPreferences.edit()).thenReturn(mEditor)
        Mockito.`when`(mEditor!!.putLong(anyString(), anyLong())).thenReturn(mEditor)

        MyApplication.context = mContext
    }

    @After
    fun tearDown() {

    }

    @Test
    fun testRelevantSingleNoChildren() {
        val persistenceManger = newPersistenceManger()
        val domainFactory = DomainFactory(persistenceManger)

        val startDate = Date(2016, 1, 1)
        val startHourMilli = HourMilli(0, 0, 0, 0)

        val startExactTimeStamp = ExactTimeStamp(startDate, startHourMilli)

        val scheduleHourMinute = HourMinute(2, 0)

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas.isEmpty())

        val rootTask = domainFactory.createScheduleRootTask(startExactTimeStamp, 0, SaveService.Source.GUI, "root task", listOf(CreateTaskViewModel.ScheduleData.SingleScheduleData(startDate, TimePair(scheduleHourMinute))), null, null)

        Assert.assertTrue(rootTask.isVisible(startExactTimeStamp))

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas.size == 1)
        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas[0].children.isEmpty())

        val scheduleDateTime = DateTime(startDate, NormalTime(scheduleHourMinute))

        var rootInstance = domainFactory.getInstance(rootTask.taskKey, scheduleDateTime)

        Assert.assertTrue(!rootInstance.exists())
        Assert.assertTrue(rootInstance.isVisible(startExactTimeStamp))

        val doneHourMilli = HourMilli(1, 0, 0, 0)

        val doneExactTimeStamp = ExactTimeStamp(startDate, doneHourMilli)

        rootInstance = domainFactory.setInstanceDone(doneExactTimeStamp, 0, SaveService.Source.GUI, rootInstance.instanceKey, true)

        Assert.assertTrue(rootInstance.exists())

        val nextDayBeforeDate = Date(2016, 1, 2)
        val nextDayBeforeHourMilli = HourMilli(0, 0, 0, 0)

        val nextDayBeforeExactTimeStamp = ExactTimeStamp(nextDayBeforeDate, nextDayBeforeHourMilli)

        val irrelevantBefore = domainFactory.setIrrelevant(nextDayBeforeExactTimeStamp)
        Assert.assertTrue(irrelevantBefore.localCustomTimes.isEmpty())
        Assert.assertTrue(irrelevantBefore.tasks.isEmpty())
        Assert.assertTrue(irrelevantBefore.instances.isEmpty())

        Assert.assertTrue(rootTask.getOldestVisible() == startDate)

        Assert.assertTrue(domainFactory.getMainData(nextDayBeforeExactTimeStamp).childTaskDatas.size == 1)
        Assert.assertTrue(domainFactory.getMainData(nextDayBeforeExactTimeStamp).childTaskDatas[0].children.isEmpty())

        val nextDayAfterHourMilli = HourMilli(2, 0, 0, 0)

        val nextDayAfterExactTimeStamp = ExactTimeStamp(nextDayBeforeDate, nextDayAfterHourMilli)

        val irrelevantAfter = domainFactory.setIrrelevant(nextDayAfterExactTimeStamp)
        Assert.assertTrue(irrelevantAfter.localCustomTimes.isEmpty())
        Assert.assertTrue(irrelevantAfter.tasks.size == 1)
        Assert.assertTrue(irrelevantAfter.instances.size == 1)

        Assert.assertTrue(domainFactory.getMainData(nextDayAfterExactTimeStamp).childTaskDatas.isEmpty())
    }

    @Test
    fun testRelevantSingleWithChildren() {
        val persistenceManger = newPersistenceManger()

        val domainFactory = DomainFactory(persistenceManger)

        val startDate = Date(2016, 1, 1)
        val startHourMilli = HourMilli(0, 0, 0, 0)

        val startExactTimeStamp = ExactTimeStamp(startDate, startHourMilli)

        val scheduleHourMinute = HourMinute(2, 0)

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas.isEmpty())

        val rootTask = domainFactory.createScheduleRootTask(startExactTimeStamp, 0, SaveService.Source.GUI, "root task", listOf(CreateTaskViewModel.ScheduleData.SingleScheduleData(startDate, TimePair(scheduleHourMinute))), null, null)

        Assert.assertTrue(rootTask.isVisible(startExactTimeStamp))

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas.size == 1)
        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas[0].children.isEmpty())

        val childTaskDone = domainFactory.createChildTask(startExactTimeStamp, 0, SaveService.Source.GUI, rootTask.taskKey, "child task done", null)

        Assert.assertTrue(childTaskDone.isVisible(startExactTimeStamp))

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas.size == 1)
        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas[0].children.size == 1)

        val childTaskExists = domainFactory.createChildTask(startExactTimeStamp, 0, SaveService.Source.GUI, rootTask.taskKey, "child task exists", null)

        Assert.assertTrue(childTaskExists.isVisible(startExactTimeStamp))

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas.size == 1)
        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas[0].children.size == 2)

        val childTaskDoesntExist = domainFactory.createChildTask(startExactTimeStamp, 0, SaveService.Source.GUI, rootTask.taskKey, "child task doesn't exist", null)
        Assert.assertTrue(childTaskDoesntExist.isVisible(startExactTimeStamp))

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas.size == 1)
        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas[0].children.size == 3)

        val scheduleDateTime = DateTime(startDate, NormalTime(scheduleHourMinute))

        var rootInstance = domainFactory.getInstance(rootTask.taskKey, scheduleDateTime)

        Assert.assertTrue(!rootInstance.exists())
        Assert.assertTrue(rootInstance.isVisible(startExactTimeStamp))

        val doneHourMilli = HourMilli(1, 0, 0, 0)

        val doneExactTimeStamp = ExactTimeStamp(startDate, doneHourMilli)

        rootInstance = domainFactory.setInstanceDone(doneExactTimeStamp, 0, SaveService.Source.GUI, rootInstance.instanceKey, true)
        Assert.assertTrue(rootInstance.exists())

        var childInstanceDone = domainFactory.getInstance(childTaskDone.taskKey, scheduleDateTime)
        Assert.assertTrue(!childInstanceDone.exists())
        Assert.assertTrue(childInstanceDone.isVisible(doneExactTimeStamp))

        childInstanceDone = domainFactory.setInstanceDone(doneExactTimeStamp, 0, SaveService.Source.GUI, childInstanceDone.instanceKey, true)
        Assert.assertTrue(childInstanceDone.exists())

        var childInstanceExists = domainFactory.getInstance(childTaskExists.taskKey, scheduleDateTime)
        Assert.assertTrue(!childInstanceExists.exists())
        Assert.assertTrue(childInstanceExists.isVisible(doneExactTimeStamp))

        childInstanceExists = domainFactory.setInstanceDone(doneExactTimeStamp, 0, SaveService.Source.GUI, childInstanceExists.instanceKey, true)
        Assert.assertTrue(childInstanceExists.exists())

        childInstanceExists = domainFactory.setInstanceDone(doneExactTimeStamp, 0, SaveService.Source.GUI, childInstanceExists.instanceKey, false)
        Assert.assertTrue(childInstanceExists.exists())

        val childInstanceDoesntExist = domainFactory.getInstance(childTaskDoesntExist.taskKey, scheduleDateTime)
        Assert.assertTrue(!childInstanceDoesntExist.exists())
        Assert.assertTrue(childInstanceDoesntExist.isVisible(doneExactTimeStamp))

        val nextDayBeforeDate = Date(2016, 1, 2)
        val nextDayBeforeHourMilli = HourMilli(0, 0, 0, 0)

        val nextDayBeforeExactTimeStamp = ExactTimeStamp(nextDayBeforeDate, nextDayBeforeHourMilli)

        val irrelevantBefore = domainFactory.setIrrelevant(nextDayBeforeExactTimeStamp)
        Assert.assertTrue(irrelevantBefore.localCustomTimes.isEmpty())
        Assert.assertTrue(irrelevantBefore.tasks.isEmpty())
        Assert.assertTrue(irrelevantBefore.instances.isEmpty())

        Assert.assertTrue(childTaskDone.getOldestVisible() == startDate)
        Assert.assertTrue(rootTask.getOldestVisible() == startDate)

        Assert.assertTrue(domainFactory.getMainData(nextDayBeforeExactTimeStamp).childTaskDatas.size == 1)
        Assert.assertTrue(domainFactory.getMainData(nextDayBeforeExactTimeStamp).childTaskDatas[0].children.size == 3)

        val nextDayAfterHourMilli = HourMilli(2, 0, 0, 0)

        val nextDayAfterExactTimeStamp = ExactTimeStamp(nextDayBeforeDate, nextDayAfterHourMilli)

        val irrelevantAfter = domainFactory.setIrrelevant(nextDayAfterExactTimeStamp)
        Assert.assertTrue(irrelevantAfter.localCustomTimes.isEmpty())
        Assert.assertTrue(irrelevantAfter.tasks.size == 4)
        Assert.assertTrue(irrelevantAfter.instances.size == 3)

        Assert.assertTrue(domainFactory.getMainData(nextDayAfterExactTimeStamp).childTaskDatas.isEmpty())
    }

    @Test
    fun testRelevantSingleAndNoReminderNextDay() {
        val persistenceManger = newPersistenceManger()
        val domainFactory = DomainFactory(persistenceManger)

        val startDate = Date(2016, 1, 1)
        val startExactTimeStamp = ExactTimeStamp(startDate, HourMilli(1, 0, 0, 0))

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas.isEmpty())

        val singleTask = domainFactory.createScheduleRootTask(startExactTimeStamp, 0, SaveService.Source.GUI, "single", listOf(CreateTaskViewModel.ScheduleData.SingleScheduleData(Date(2016, 1, 1), TimePair(HourMinute(2, 0)))), null, null)

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas.size == 1)

        val noReminderTask = domainFactory.createRootTask(startExactTimeStamp, 0, SaveService.Source.GUI, "no reminder", null, null)

        Assert.assertTrue(domainFactory.getMainData(startExactTimeStamp).childTaskDatas.size == 2)

        val irrelevantFirstDay = domainFactory.setIrrelevant(ExactTimeStamp(startDate, HourMilli(3, 0, 0, 0)))
        Assert.assertTrue(irrelevantFirstDay.tasks.isEmpty())
        Assert.assertTrue(irrelevantFirstDay.instances.isEmpty())

        Assert.assertTrue(singleTask.getOldestVisible() == startDate)
        Assert.assertTrue(noReminderTask.getOldestVisible() == startDate)

        val nextDay = Date(2016, 1, 2)

        val irrelevantNextDayBefore = domainFactory.setIrrelevant(ExactTimeStamp(nextDay, HourMilli(3, 0, 0, 0)))
        Assert.assertTrue(irrelevantNextDayBefore.tasks.isEmpty())
        Assert.assertTrue(irrelevantNextDayBefore.instances.isEmpty())

        Assert.assertTrue(singleTask.getOldestVisible() == startDate)
        Assert.assertTrue(noReminderTask.getOldestVisible() == nextDay)

        domainFactory.updateChildTask(ExactTimeStamp(nextDay, HourMilli(4, 0, 0, 0)), 0, SaveService.Source.GUI, noReminderTask.taskKey, noReminderTask.name, singleTask.taskKey, noReminderTask.note)
        Assert.assertTrue(irrelevantNextDayBefore.tasks.isEmpty())
        Assert.assertTrue(irrelevantNextDayBefore.instances.isEmpty())

        Assert.assertTrue(singleTask.getOldestVisible() == startDate)
        Assert.assertTrue(noReminderTask.getOldestVisible() == nextDay)

        Assert.assertTrue(domainFactory.getMainData(ExactTimeStamp(nextDay, HourMilli(5, 0, 0, 0))).childTaskDatas.size == 1)
    }

    @Test
    fun testJoinLeavesPreviousInstances() {
        val persistenceManger = newPersistenceManger()
        val domainFactory = DomainFactory(persistenceManger)

        val startDate = Date(2016, 11, 9)
        val startHourMilli = HourMilli(1, 0, 0, 0)

        val startExactTimeStamp = ExactTimeStamp(startDate, startHourMilli)

        Assert.assertTrue(domainFactory.getGroupListData(startExactTimeStamp, 0, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.isEmpty())

        val singleTimePair = TimePair(HourMinute(2, 0))

        val singleData = CreateTaskViewModel.ScheduleData.SingleScheduleData(startDate, singleTimePair)

        val singleTask1 = domainFactory.createScheduleRootTask(startExactTimeStamp, 0, SaveService.Source.GUI, "singleTask1", listOf(singleData), null, null)
        val singleTask2 = domainFactory.createScheduleRootTask(startExactTimeStamp, 0, SaveService.Source.GUI, "singleTask2", listOf(singleData), null, null)

        val (dataWrapper) = domainFactory.getGroupListData(ExactTimeStamp(startDate, HourMilli(2, 0, 0, 0)), 0, MainActivity.TimeRange.DAY)
        Assert.assertTrue(dataWrapper.instanceDatas.size == 2)

        val doneExactTimeStamp = ExactTimeStamp(startDate, HourMilli(3, 0, 0, 0))

        domainFactory.setInstanceDone(doneExactTimeStamp, 0, SaveService.Source.GUI, dataWrapper.instanceDatas.values.iterator().next().InstanceKey, true)
        domainFactory.setInstanceDone(doneExactTimeStamp, 0, SaveService.Source.GUI, dataWrapper.instanceDatas.values.iterator().next().InstanceKey, false)

        val joinExactTimeStamp = ExactTimeStamp(startDate, HourMilli(4, 0, 0, 0))

        val joinData = CreateTaskViewModel.ScheduleData.SingleScheduleData(startDate, TimePair(HourMinute(5, 0)))

        val joinTaskKeys = Arrays.asList(singleTask1.taskKey, singleTask2.taskKey)

        domainFactory.createScheduleJoinRootTask(joinExactTimeStamp, 0, SaveService.Source.GUI, "joinTask", listOf(joinData), joinTaskKeys, null, null)

        val (dataWrapper1) = domainFactory.getGroupListData(ExactTimeStamp(startDate, HourMilli(6, 0, 0, 0)), 0, MainActivity.TimeRange.DAY)

        Assert.assertTrue(dataWrapper1.instanceDatas.size == 3)
    }

    @Test
    fun testSharedChild() {
        val persistenceManger = newPersistenceManger()
        val domainFactory = DomainFactory(persistenceManger)

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

        val day1 = Date(2016, 1, 1)
        val day2 = Date(2016, 1, 2)

        val range1 = 0
        val range2 = 1

        val hour0 = HourMinute(0, 0)
        val hour1 = HourMinute(1, 0)
        val hour2 = HourMinute(2, 0)
        val hour3 = HourMinute(3, 0)
        val hour4 = HourMinute(4, 0)
        val hour5 = HourMinute(5, 0)
        val hour6 = HourMinute(6, 0)
        val hour7 = HourMinute(7, 0)
        val hour8 = HourMinute(8, 0)
        val hour12 = HourMinute(12, 0)
        val hour13 = HourMinute(13, 0)
        val hour14 = HourMinute(14, 0)
        val hour15 = HourMinute(15, 0)
        val hour16 = HourMinute(16, 0)
        val hour17 = HourMinute(17, 0)
        val hour18 = HourMinute(18, 0)

        val dataId = 0

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour0.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.isEmpty())

        val firstScheduleData = CreateTaskViewModel.ScheduleData.SingleScheduleData(day1, TimePair(hour12))
        val firstTask = domainFactory.createScheduleRootTask(ExactTimeStamp(day1, hour1.toHourMilli()), dataId, SaveService.Source.GUI, "firstTask", listOf(firstScheduleData), null, null)

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour2.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour2.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.isEmpty())

        val childTask = domainFactory.createChildTask(ExactTimeStamp(day1, hour3.toHourMilli()), dataId, SaveService.Source.GUI, firstTask.taskKey, "childTask", null)

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour4.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour4.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.size == 1)

        val secondScheduleData = CreateTaskViewModel.ScheduleData.SingleScheduleData(day2, TimePair(hour12))
        val secondTask = domainFactory.createScheduleRootTask(ExactTimeStamp(day1, hour5.toHourMilli()), dataId, SaveService.Source.GUI, "secondTask", listOf(secondScheduleData), null, null)

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour6.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(!domainFactory.getGroupListData(ExactTimeStamp(day1, hour6.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().Exists)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour6.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour6.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour6.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.values.iterator().next().Done == null)
        Assert.assertTrue(!domainFactory.getGroupListData(ExactTimeStamp(day1, hour6.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.values.iterator().next().Exists)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour6.toHourMilli()), range2, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour6.toHourMilli()), range2, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.isEmpty())

        val childTaskInFirstTaskInstanceKey = domainFactory.getGroupListData(ExactTimeStamp(day1, hour7.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.keys.iterator().next()
        domainFactory.setInstanceDone(ExactTimeStamp(day1, hour7.toHourMilli()), dataId, SaveService.Source.GUI, childTaskInFirstTaskInstanceKey, true)

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour8.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour8.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().Exists)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour8.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour8.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.values.iterator().next().Done != null)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour8.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.values.iterator().next().Exists)

        run {
            val irrelevant = domainFactory.updateNotificationsTick(ExactTimeStamp(day1, hour12.toHourMilli()), SaveService.Source.GUI, false)
            Assert.assertTrue(irrelevant.tasks.isEmpty())
            Assert.assertTrue(irrelevant.instances.isEmpty())
        }

        domainFactory.updateChildTask(ExactTimeStamp(day1, hour13.toHourMilli()), dataId, SaveService.Source.GUI, childTask.taskKey, childTask.name, secondTask.taskKey, childTask.note)

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour14.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour14.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().Done == null)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour14.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour14.toHourMilli()), range2, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour14.toHourMilli()), range2, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.size == 1)

        val firstTaskInstanceKey = domainFactory.getGroupListData(ExactTimeStamp(day1, hour15.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.keys.iterator().next()
        domainFactory.setInstanceDone(ExactTimeStamp(day1, hour15.toHourMilli()), dataId, SaveService.Source.GUI, firstTaskInstanceKey, true)

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour16.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour16.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().Done != null)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour16.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour16.toHourMilli()), range2, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day1, hour16.toHourMilli()), range2, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.size == 1)

        val secondTaskInstanceKey = domainFactory.getGroupListData(ExactTimeStamp(day1, hour16.toHourMilli()), range2, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.keys.iterator().next()

        // works up to here

        run {
            val (dataWrapper) = domainFactory.getGroupListData(ExactTimeStamp(day2, hour0.toHourMilli()), range1, MainActivity.TimeRange.DAY)

            Assert.assertTrue(dataWrapper.instanceDatas.size == 2)
            Assert.assertTrue(dataWrapper.instanceDatas[firstTaskInstanceKey]!!.children.size == 1)
            Assert.assertTrue(dataWrapper.instanceDatas[secondTaskInstanceKey]!!.Done == null)
            Assert.assertTrue(!dataWrapper.instanceDatas[secondTaskInstanceKey]!!.Exists)
            Assert.assertTrue(dataWrapper.instanceDatas[secondTaskInstanceKey]!!.children.size == 1)
        }

        domainFactory.setInstanceDone(ExactTimeStamp(day2, hour1.toHourMilli()), dataId, SaveService.Source.GUI, secondTaskInstanceKey, true)

        run {
            val (dataWrapper) = domainFactory.getGroupListData(ExactTimeStamp(day2, hour2.toHourMilli()), range1, MainActivity.TimeRange.DAY)

            Assert.assertTrue(dataWrapper.instanceDatas.size == 2)
            Assert.assertTrue(dataWrapper.instanceDatas[firstTaskInstanceKey]!!.children.size == 1)
            Assert.assertTrue(dataWrapper.instanceDatas[secondTaskInstanceKey]!!.Done != null)
            Assert.assertTrue(dataWrapper.instanceDatas[secondTaskInstanceKey]!!.Exists)
            Assert.assertTrue(dataWrapper.instanceDatas[secondTaskInstanceKey]!!.children.size == 1)
        }

        run {
            val irrelevant = domainFactory.updateNotificationsTick(ExactTimeStamp(day2, hour16.toHourMilli()), SaveService.Source.GUI, false)
            Assert.assertTrue(irrelevant.tasks.isEmpty())
            Assert.assertTrue(irrelevant.instances.size == 2)
        }

        Assert.assertTrue(domainFactory.getMainData(ExactTimeStamp(day2, hour17.toHourMilli())).childTaskDatas.size == 1)

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day2, hour18.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(day2, hour18.toHourMilli()), range1, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.values.iterator().next().children.size == 1)
    }

    @Test
    fun testChildAddedToInstanceInPast() {
        val persistenceManger = newPersistenceManger()
        val domainFactory = DomainFactory(persistenceManger)

        // hour 0: check no instances
        // hour 1: add parent for hour 3
        // hour 2: check one instance, no children
        // hour 3: tick, check one instance, no children
        // hour 4: add child to parent
        // hour 5: check one instance, one child

        val date = Date(2016, 1, 1)

        val hour0 = HourMinute(0, 0)
        val hour1 = HourMinute(1, 0)
        val hour2 = HourMinute(2, 0)
        val hour3 = HourMinute(3, 0)
        val hour4 = HourMinute(4, 0)
        val hour5 = HourMinute(5, 0)

        val dataId = 0
        val range = 0

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour0.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.isEmpty())

        val scheduleData = CreateTaskViewModel.ScheduleData.SingleScheduleData(date, TimePair(hour3))
        val parentTask = domainFactory.createScheduleRootTask(ExactTimeStamp(date, hour1.toHourMilli()), dataId, SaveService.Source.GUI, "parent", listOf(scheduleData), null, null)

        val (dataWrapper) = domainFactory.getGroupListData(ExactTimeStamp(date, hour2.toHourMilli()), range, MainActivity.TimeRange.DAY)
        Assert.assertTrue(dataWrapper.instanceDatas.size == 1)

        val parentInstanceKey = dataWrapper.instanceDatas.keys.iterator().next()
        Assert.assertTrue(dataWrapper.instanceDatas[parentInstanceKey]!!.children.isEmpty())

        domainFactory.updateNotificationsTick(ExactTimeStamp(date, hour3.toHourMilli()), SaveService.Source.GUI, false)

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour3.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour3.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas[parentInstanceKey]!!.children.isEmpty())

        domainFactory.createChildTask(ExactTimeStamp(date, hour4.toHourMilli()), dataId, SaveService.Source.GUI, parentTask.taskKey, "child", null)

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour5.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour5.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas[parentInstanceKey]!!.children.size == 1)
    }

    @Test
    fun testTaskAddedToParentKeepsOldRootInstance() {
        // hour 0: check no instances
        // hour 1: create task split for hour 2
        // hour 2: notify, check one instance, no children
        // hour 3: create task parent for hour 7
        // hour 4: check two instances, no children
        // hour 5: edit task split, set parent parent
        // hour 6: check two instances, parent has one child

        val persistenceManger = newPersistenceManger()
        val domainFactory = DomainFactory(persistenceManger)

        val date = Date(2016, 1, 1)

        val hour0 = HourMinute(0, 0)
        val hour1 = HourMinute(1, 0)
        val hour2 = HourMinute(2, 0)
        val hour3 = HourMinute(3, 0)
        val hour4 = HourMinute(4, 0)
        val hour5 = HourMinute(5, 0)
        val hour6 = HourMinute(6, 0)
        val hour7 = HourMinute(7, 0)

        val dataId = 0
        val range = 0

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour0.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.isEmpty())

        val splitScheduleData = CreateTaskViewModel.ScheduleData.SingleScheduleData(date, TimePair(hour2))
        val splitTask = domainFactory.createScheduleRootTask(ExactTimeStamp(date, hour1.toHourMilli()), dataId, SaveService.Source.GUI, "split", listOf(splitScheduleData), null, null)

        domainFactory.updateNotificationsTick(ExactTimeStamp(date, hour2.toHourMilli()), SaveService.Source.GUI, false)

        val (dataWrapper1) = domainFactory.getGroupListData(ExactTimeStamp(date, hour2.toHourMilli()), range, MainActivity.TimeRange.DAY)
        Assert.assertTrue(dataWrapper1.instanceDatas.size == 1)

        val splitInstanceKey = dataWrapper1.instanceDatas.keys.iterator().next()
        Assert.assertTrue(dataWrapper1.instanceDatas[splitInstanceKey]!!.children.isEmpty())

        val parentScheduleData = CreateTaskViewModel.ScheduleData.SingleScheduleData(date, TimePair(hour7))
        val parentTask = domainFactory.createScheduleRootTask(ExactTimeStamp(date, hour3.toHourMilli()), dataId, SaveService.Source.GUI, "parent", listOf(parentScheduleData), null, null)

        val (dataWrapper2) = domainFactory.getGroupListData(ExactTimeStamp(date, hour4.toHourMilli()), range, MainActivity.TimeRange.DAY)
        Assert.assertTrue(dataWrapper2.instanceDatas.size == 2)

        val parentInstanceKey = dataWrapper2.instanceDatas
                .keys
                .first { it != splitInstanceKey }

        Assert.assertTrue(dataWrapper2.instanceDatas[splitInstanceKey]!!.children.isEmpty())
        Assert.assertTrue(dataWrapper2.instanceDatas[parentInstanceKey]!!.children.isEmpty())

        domainFactory.updateChildTask(ExactTimeStamp(date, hour5.toHourMilli()), dataId, SaveService.Source.GUI, splitTask.taskKey, splitTask.name, parentTask.taskKey, splitTask.note)

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour6.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 2)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour6.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas[splitInstanceKey]!!.children.isEmpty())
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour6.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas[parentInstanceKey]!!.children.size == 1)
    }

    @Test
    fun testSingleScheduleChanged() {
        // hour 0: check no instances
        // hour 1: create task for hour 5
        // hour 2: check one instance, hour 5
        // hour 3: edit task, hour 6
        // hour 4: check one instance, hour 6

        val persistenceManger = newPersistenceManger()
        val domainFactory = DomainFactory(persistenceManger)

        val date = Date(2016, 1, 1)

        val hour0 = HourMinute(0, 0)
        val hour1 = HourMinute(1, 0)
        val hour2 = HourMinute(2, 0)
        val hour3 = HourMinute(3, 0)
        val hour4 = HourMinute(4, 0)
        val hour5 = HourMinute(5, 0)
        val hour6 = HourMinute(6, 0)

        val dataId = 0
        val range = 0

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour0.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.isEmpty())

        val firstScheduleData = CreateTaskViewModel.ScheduleData.SingleScheduleData(date, TimePair(hour5))
        val task = domainFactory.createScheduleRootTask(ExactTimeStamp(date, hour1.toHourMilli()), dataId, SaveService.Source.GUI, "task", listOf(firstScheduleData), null, null)

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour2.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour2.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.keys.iterator().next().scheduleKey.scheduleTimePair.hourMinute == hour5)

        val secondScheduleData = CreateTaskViewModel.ScheduleData.SingleScheduleData(date, TimePair(hour6))
        domainFactory.updateScheduleTask(ExactTimeStamp(date, hour3.toHourMilli()), dataId, SaveService.Source.GUI, task.taskKey, task.name, listOf(secondScheduleData), task.note, null)

        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour4.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.size == 1)
        Assert.assertTrue(domainFactory.getGroupListData(ExactTimeStamp(date, hour4.toHourMilli()), range, MainActivity.TimeRange.DAY).dataWrapper.instanceDatas.keys.iterator().next().scheduleKey.scheduleTimePair.hourMinute == hour6)
    }
}