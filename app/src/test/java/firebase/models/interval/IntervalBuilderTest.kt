package firebase.models.interval

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.models.Schedule
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.models.TaskHierarchy
import com.krystianwsul.common.firebase.models.interval.IntervalBuilder
import com.krystianwsul.common.firebase.models.interval.IntervalBuilder.Interval
import com.krystianwsul.common.firebase.models.interval.IntervalBuilder.Type
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.ProjectType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class IntervalBuilderTest {

    companion object {

        private val date = Date(2020, 5, 17)
    }

    private fun taskMock(
            start: ExactTimeStamp,
            end: ExactTimeStamp? = null,
            taskHierarchies: Collection<TaskHierarchy<ProjectType.Private>> = setOf(),
            scheduleList: List<Schedule<ProjectType.Private>> = listOf()
    ): Task<ProjectType.Private> {
        return mockk(relaxed = true) {
            every { startExactTimeStamp } returns start
            every { endExactTimeStamp } returns end
            every { parentTaskHierarchies } returns taskHierarchies.toSet()
            every { schedules } returns scheduleList
        }
    }

    private fun taskHierarchyMock(
            start: ExactTimeStamp,
            end: ExactTimeStamp? = null
    ): TaskHierarchy<ProjectType.Private> {
        return mockk(relaxed = true) {
            every { startExactTimeStamp } returns start
            every { endExactTimeStamp } returns end
        }
    }

    private fun scheduleMock(
            start: ExactTimeStamp,
            end: ExactTimeStamp? = null
    ): Schedule<ProjectType.Private> {
        return mockk(relaxed = true) {
            every { startExactTimeStamp } returns start
            every { endExactTimeStamp } returns end
        }
    }

    private fun noScheduleMock() = Type.NoSchedule<ProjectType.Private>()

    private fun Task<ProjectType.Private>.check(vararg expected: Interval<ProjectType.Private>) {
        val actual = IntervalBuilder.build(this)
        assertEquals(expected.toList(), actual)
    }

    @Before
    fun before() {
        ErrorLogger.instance = mockk(relaxed = true)
    }

    @Test
    fun testNothing() {
        val taskStartExactTimeStamp = ExactTimeStamp(date, HourMinute(12, 0).toHourMilli())

        val task = taskMock(taskStartExactTimeStamp)

        task.check(Interval.Current(noScheduleMock(), taskStartExactTimeStamp))
    }

    @Test
    fun testChild() {
        val taskStartExactTimeStamp = ExactTimeStamp(date, HourMinute(12, 0).toHourMilli())
        val taskHierarchy = taskHierarchyMock(taskStartExactTimeStamp)

        val task = taskMock(
                taskStartExactTimeStamp,
                taskHierarchies = listOf(taskHierarchy)
        )

        task.check(Interval.Current(Type.Child(taskHierarchy), taskStartExactTimeStamp))
    }

    @Test
    fun testNothingThenChild() {
        val taskStartExactTimeStamp = ExactTimeStamp(date, HourMinute(12, 0).toHourMilli())
        val hierarchyStartExactTimeStamp = ExactTimeStamp(date, HourMinute(12, 1).toHourMilli())
        val taskHierarchy = taskHierarchyMock(hierarchyStartExactTimeStamp)

        val task = taskMock(
                taskStartExactTimeStamp,
                taskHierarchies = listOf(taskHierarchy)
        )

        task.check(
                Interval.Ended(Type.NoSchedule(), taskStartExactTimeStamp, hierarchyStartExactTimeStamp),
                Interval.Current(Type.Child(taskHierarchy), hierarchyStartExactTimeStamp)
        )
    }

    @Test
    fun testNothingChildNothingSchedule() {
        val taskStart = ExactTimeStamp(date, HourMinute(12, 0).toHourMilli())

        val hierarchyStart = ExactTimeStamp(date, HourMinute(12, 1).toHourMilli())
        val nothingStart = ExactTimeStamp(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy = taskHierarchyMock(hierarchyStart, nothingStart)

        val scheduleStart = ExactTimeStamp(date, HourMinute(12, 3).toHourMilli())
        val schedule = scheduleMock(scheduleStart)

        val task = taskMock(
                taskStart,
                taskHierarchies = listOf(taskHierarchy),
                scheduleList = listOf(schedule)
        )

        task.check(
                Interval.Ended(Type.NoSchedule(), taskStart, hierarchyStart),
                Interval.Ended(Type.Child(taskHierarchy), hierarchyStart, nothingStart),
                Interval.Ended(Type.NoSchedule(), nothingStart, scheduleStart),
                Interval.Current(Type.Schedule(listOf(schedule)), scheduleStart)
        )
    }

    @Test
    fun testNothingChildNothingScheduleNothing() {
        val taskStart = ExactTimeStamp(date, HourMinute(12, 0).toHourMilli())

        val hierarchyStart = ExactTimeStamp(date, HourMinute(12, 1).toHourMilli())
        val nothingStart1 = ExactTimeStamp(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy = taskHierarchyMock(hierarchyStart, nothingStart1)

        val scheduleStart = ExactTimeStamp(date, HourMinute(12, 3).toHourMilli())
        val nothingStart2 = ExactTimeStamp(date, HourMinute(12, 4).toHourMilli())
        val schedule = scheduleMock(scheduleStart, nothingStart2)

        val task = taskMock(
                taskStart,
                taskHierarchies = listOf(taskHierarchy),
                scheduleList = listOf(schedule)
        )

        task.check(
                Interval.Ended(Type.NoSchedule(), taskStart, hierarchyStart),
                Interval.Ended(Type.Child(taskHierarchy), hierarchyStart, nothingStart1),
                Interval.Ended(Type.NoSchedule(), nothingStart1, scheduleStart),
                Interval.Ended(Type.Schedule(listOf(schedule)), scheduleStart, nothingStart2),
                Interval.Current(Type.NoSchedule(), nothingStart2)
        )
    }

    @Test
    fun testNothingEnd() {
        val taskStart = ExactTimeStamp(date, HourMinute(12, 0).toHourMilli())
        val taskEnd = ExactTimeStamp(date, HourMinute(12, 1).toHourMilli())

        val task = taskMock(taskStart, taskEnd)

        task.check(Interval.Ended(noScheduleMock(), taskStart, taskEnd))
    }

    @Test
    fun testChildEnd() {
        val taskStart = ExactTimeStamp(date, HourMinute(12, 0).toHourMilli())
        val taskEnd = ExactTimeStamp(date, HourMinute(12, 1).toHourMilli())
        val taskHierarchy = taskHierarchyMock(taskStart, taskEnd)

        val task = taskMock(
                taskStart,
                taskEnd,
                listOf(taskHierarchy)
        )

        task.check(Interval.Ended(Type.Child(taskHierarchy), taskStart, taskEnd))
    }

    @Test
    fun testNothingThenChildEnd() {
        val taskStart = ExactTimeStamp(date, HourMinute(12, 0).toHourMilli())
        val hierarchyStart = ExactTimeStamp(date, HourMinute(12, 1).toHourMilli())
        val taskEnd = ExactTimeStamp(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy = taskHierarchyMock(hierarchyStart, taskEnd)

        val task = taskMock(
                taskStart,
                taskEnd,
                listOf(taskHierarchy)
        )

        task.check(
                Interval.Ended(Type.NoSchedule(), taskStart, hierarchyStart),
                Interval.Ended(Type.Child(taskHierarchy), hierarchyStart, taskEnd)
        )
    }

    @Test
    fun testNothingChildNothingScheduleEnd() {
        val taskStart = ExactTimeStamp(date, HourMinute(12, 0).toHourMilli())

        val hierarchyStart = ExactTimeStamp(date, HourMinute(12, 1).toHourMilli())
        val nothingStart = ExactTimeStamp(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy = taskHierarchyMock(hierarchyStart, nothingStart)

        val scheduleStart = ExactTimeStamp(date, HourMinute(12, 3).toHourMilli())
        val taskEnd = ExactTimeStamp(date, HourMinute(12, 4).toHourMilli())
        val schedule = scheduleMock(scheduleStart, taskEnd)

        val task = taskMock(
                taskStart,
                taskEnd,
                listOf(taskHierarchy),
                listOf(schedule)
        )

        task.check(
                Interval.Ended(Type.NoSchedule(), taskStart, hierarchyStart),
                Interval.Ended(Type.Child(taskHierarchy), hierarchyStart, nothingStart),
                Interval.Ended(Type.NoSchedule(), nothingStart, scheduleStart),
                Interval.Ended(Type.Schedule(listOf(schedule)), scheduleStart, taskEnd)
        )
    }

    @Test
    fun testNothingChildNothingScheduleNothingEnd() {
        val taskStart = ExactTimeStamp(date, HourMinute(12, 0).toHourMilli())

        val hierarchyStart = ExactTimeStamp(date, HourMinute(12, 1).toHourMilli())
        val nothingStart1 = ExactTimeStamp(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy = taskHierarchyMock(hierarchyStart, nothingStart1)

        val scheduleStart = ExactTimeStamp(date, HourMinute(12, 3).toHourMilli())
        val nothingStart2 = ExactTimeStamp(date, HourMinute(12, 4).toHourMilli())
        val schedule = scheduleMock(scheduleStart, nothingStart2)

        val taskEnd = ExactTimeStamp(date, HourMinute(12, 5).toHourMilli())

        val task = taskMock(
                taskStart,
                taskEnd,
                listOf(taskHierarchy),
                listOf(schedule)
        )

        task.check(
                Interval.Ended(Type.NoSchedule(), taskStart, hierarchyStart),
                Interval.Ended(Type.Child(taskHierarchy), hierarchyStart, nothingStart1),
                Interval.Ended(Type.NoSchedule(), nothingStart1, scheduleStart),
                Interval.Ended(Type.Schedule(listOf(schedule)), scheduleStart, nothingStart2),
                Interval.Ended(Type.NoSchedule(), nothingStart2, taskEnd)
        )
    }

    @Test
    fun testChildCurrentOverlapScheduleCurrentOverlapChild() {
        val taskStart = ExactTimeStamp(date, HourMinute(12, 0).toHourMilli())
        val taskHierarchy1 = taskHierarchyMock(taskStart)

        val scheduleStart = ExactTimeStamp(date, HourMinute(12, 1).toHourMilli())
        val schedule = scheduleMock(scheduleStart)

        val hierarchy2Start = ExactTimeStamp(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy2 = taskHierarchyMock(hierarchy2Start)

        val task = taskMock(
                taskStart,
                taskHierarchies = listOf(taskHierarchy1, taskHierarchy2),
                scheduleList = listOf(schedule)
        )

        task.check(
                Interval.Ended(Type.Child(taskHierarchy1), taskStart, scheduleStart),
                Interval.Ended(Type.Schedule(listOf(schedule)), scheduleStart, hierarchy2Start),
                Interval.Current(Type.Child(taskHierarchy2), hierarchy2Start)
        )
    }

    @Test
    fun testChildEndedOverlapScheduleEndedOverlapChild() {
        val taskStart = ExactTimeStamp(date, HourMinute(12, 0).toHourMilli())
        val futureEnd = ExactTimeStamp(date, HourMinute(13, 0).toHourMilli())
        val taskHierarchy1 = taskHierarchyMock(taskStart, futureEnd)

        val scheduleStart = ExactTimeStamp(date, HourMinute(12, 1).toHourMilli())
        val schedule = scheduleMock(scheduleStart, futureEnd)

        val hierarchy2Start = ExactTimeStamp(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy2 = taskHierarchyMock(hierarchy2Start, futureEnd)

        val task = taskMock(
                taskStart,
                taskHierarchies = listOf(taskHierarchy1, taskHierarchy2),
                scheduleList = listOf(schedule)
        )

        task.check(
                Interval.Ended(Type.Child(taskHierarchy1), taskStart, scheduleStart),
                Interval.Ended(Type.Schedule(listOf(schedule)), scheduleStart, hierarchy2Start),
                Interval.Ended(Type.Child(taskHierarchy2), hierarchy2Start, futureEnd),
                Interval.Current(Type.NoSchedule(), futureEnd)
        )
    }

    // todo group task test group of multiple schedules with various starts and ends
}