package com.krystianwsul.common.firebase.models.interval

import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.models.NoScheduleOrParent
import com.krystianwsul.common.firebase.models.ProjectTaskHierarchy
import com.krystianwsul.common.firebase.models.Task
import com.krystianwsul.common.firebase.models.schedule.Schedule
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
            start: ExactTimeStamp.Local,
            end: ExactTimeStamp.Local? = null,
            taskHierarchies: Collection<ProjectTaskHierarchy<ProjectType.Private>> = setOf(),
            scheduleList: List<Schedule> = listOf(),
            noScheduleOrParentList: List<NoScheduleOrParent> = listOf(),
    ): Task<ProjectType.Private> {
        return mockk(relaxed = true) {
            every { startExactTimeStamp } returns start
            every { startExactTimeStampOffset } returns start.toOffset()

            every { endExactTimeStamp } returns end
            every { endExactTimeStampOffset } returns end?.toOffset()

            every { parentTaskHierarchies } returns taskHierarchies.toSet()
            every { schedules } returns scheduleList
            every { noScheduleOrParents } returns noScheduleOrParentList
        }
    }

    private fun taskHierarchyMock(
            start: ExactTimeStamp.Local,
            end: ExactTimeStamp.Local? = null,
    ): ProjectTaskHierarchy<ProjectType.Private> {
        return mockk(relaxed = true) {
            every { startExactTimeStamp } returns start
            every { startExactTimeStampOffset } returns start.toOffset()

            every { endExactTimeStamp } returns end
            every { endExactTimeStampOffset } returns end?.toOffset()
        }
    }

    private fun scheduleMock(start: ExactTimeStamp.Local, end: ExactTimeStamp.Local? = null): Schedule {
        return mockk(relaxed = true) {
            every { startExactTimeStamp } returns start
            every { startExactTimeStampOffset } returns start.toOffset()

            every { endExactTimeStamp } returns end
            every { endExactTimeStampOffset } returns end?.toOffset()
        }
    }

    private fun noScheduleOrParentMock(
            start: ExactTimeStamp.Local,
            end: ExactTimeStamp.Local? = null,
    ): NoScheduleOrParent {
        return mockk(relaxed = true) {
            every { startExactTimeStamp } returns start
            every { startExactTimeStampOffset } returns start.toOffset()

            every { endExactTimeStamp } returns end
            every { endExactTimeStampOffset } returns end?.toOffset()
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
        val taskStartExactTimeStamp = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())

        val task = taskMock(taskStartExactTimeStamp)

        task.check(Interval.Current(noScheduleMock(), taskStartExactTimeStamp.toOffset()))
    }

    @Test
    fun testChild() {
        val taskStartExactTimeStamp = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())
        val taskHierarchy = taskHierarchyMock(taskStartExactTimeStamp)

        val task = taskMock(
                taskStartExactTimeStamp,
                taskHierarchies = listOf(taskHierarchy)
        )

        task.check(Interval.Current(Type.Child(taskHierarchy), taskStartExactTimeStamp.toOffset()))
    }

    @Test
    fun testNothingThenChild() {
        val taskStartExactTimeStamp = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())
        val hierarchyStartExactTimeStamp = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val taskHierarchy = taskHierarchyMock(hierarchyStartExactTimeStamp)

        val task = taskMock(
                taskStartExactTimeStamp,
                taskHierarchies = listOf(taskHierarchy)
        )

        task.check(
                Interval.Ended(
                        Type.NoSchedule(),
                        taskStartExactTimeStamp.toOffset(),
                        hierarchyStartExactTimeStamp.toOffset()
                ),
                Interval.Current(Type.Child(taskHierarchy), hierarchyStartExactTimeStamp.toOffset())
        )
    }

    @Test
    fun testNothingChildNothingSchedule() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())

        val hierarchyStart = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val nothingStart = ExactTimeStamp.Local(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy = taskHierarchyMock(hierarchyStart, nothingStart)

        val scheduleStart = ExactTimeStamp.Local(date, HourMinute(12, 3).toHourMilli())
        val schedule = scheduleMock(scheduleStart)

        val task = taskMock(
                taskStart,
                taskHierarchies = listOf(taskHierarchy),
                scheduleList = listOf(schedule)
        )

        task.check(
                Interval.Ended(Type.NoSchedule(), taskStart.toOffset(), hierarchyStart.toOffset()),
                Interval.Ended(Type.Child(taskHierarchy), hierarchyStart.toOffset(), nothingStart.toOffset()),
                Interval.Ended(Type.NoSchedule(), nothingStart.toOffset(), scheduleStart.toOffset()),
                Interval.Current(Type.Schedule(listOf(schedule)), scheduleStart.toOffset())
        )
    }

    @Test
    fun testNothingChildNothingScheduleNothing() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())

        val hierarchyStart = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val nothingStart1 = ExactTimeStamp.Local(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy = taskHierarchyMock(hierarchyStart, nothingStart1)

        val scheduleStart = ExactTimeStamp.Local(date, HourMinute(12, 3).toHourMilli())
        val nothingStart2 = ExactTimeStamp.Local(date, HourMinute(12, 4).toHourMilli())
        val schedule = scheduleMock(scheduleStart, nothingStart2)

        val task = taskMock(
                taskStart,
                taskHierarchies = listOf(taskHierarchy),
                scheduleList = listOf(schedule)
        )

        task.check(
                Interval.Ended(Type.NoSchedule(), taskStart.toOffset(), hierarchyStart.toOffset()),
                Interval.Ended(Type.Child(taskHierarchy), hierarchyStart.toOffset(), nothingStart1.toOffset()),
                Interval.Ended(Type.NoSchedule(), nothingStart1.toOffset(), scheduleStart.toOffset()),
                Interval.Ended(Type.Schedule(listOf(schedule)), scheduleStart.toOffset(), nothingStart2.toOffset()),
                Interval.Current(Type.NoSchedule(), nothingStart2.toOffset())
        )
    }

    @Test
    fun testNothingEnd() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())
        val taskEnd = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())

        val task = taskMock(taskStart, taskEnd)

        task.check(Interval.Ended(noScheduleMock(), taskStart.toOffset(), taskEnd.toOffset()))
    }

    @Test
    fun testChildEnd() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())
        val taskEnd = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val taskHierarchy = taskHierarchyMock(taskStart, taskEnd)

        val task = taskMock(
                taskStart,
                taskEnd,
                listOf(taskHierarchy)
        )

        task.check(Interval.Ended(Type.Child(taskHierarchy), taskStart.toOffset(), taskEnd.toOffset()))
    }

    @Test
    fun testNothingThenChildEnd() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())
        val hierarchyStart = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val taskEnd = ExactTimeStamp.Local(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy = taskHierarchyMock(hierarchyStart, taskEnd)

        val task = taskMock(
                taskStart,
                taskEnd,
                listOf(taskHierarchy)
        )

        task.check(
                Interval.Ended(Type.NoSchedule(), taskStart.toOffset(), hierarchyStart.toOffset()),
                Interval.Ended(Type.Child(taskHierarchy), hierarchyStart.toOffset(), taskEnd.toOffset())
        )
    }

    @Test
    fun testNothingChildNothingScheduleEnd() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())

        val hierarchyStart = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val nothingStart = ExactTimeStamp.Local(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy = taskHierarchyMock(hierarchyStart, nothingStart)

        val scheduleStart = ExactTimeStamp.Local(date, HourMinute(12, 3).toHourMilli())
        val taskEnd = ExactTimeStamp.Local(date, HourMinute(12, 4).toHourMilli())
        val schedule = scheduleMock(scheduleStart, taskEnd)

        val task = taskMock(
                taskStart,
                taskEnd,
                listOf(taskHierarchy),
                listOf(schedule)
        )

        task.check(
                Interval.Ended(Type.NoSchedule(), taskStart.toOffset(), hierarchyStart.toOffset()),
                Interval.Ended(Type.Child(taskHierarchy), hierarchyStart.toOffset(), nothingStart.toOffset()),
                Interval.Ended(Type.NoSchedule(), nothingStart.toOffset(), scheduleStart.toOffset()),
                Interval.Ended(Type.Schedule(listOf(schedule)), scheduleStart.toOffset(), taskEnd.toOffset())
        )
    }

    @Test
    fun testNothingChildNothingScheduleNothingEnd() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())

        val hierarchyStart = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val nothingStart1 = ExactTimeStamp.Local(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy = taskHierarchyMock(hierarchyStart, nothingStart1)

        val scheduleStart = ExactTimeStamp.Local(date, HourMinute(12, 3).toHourMilli())
        val nothingStart2 = ExactTimeStamp.Local(date, HourMinute(12, 4).toHourMilli())
        val schedule = scheduleMock(scheduleStart, nothingStart2)

        val taskEnd = ExactTimeStamp.Local(date, HourMinute(12, 5).toHourMilli())

        val task = taskMock(
                taskStart,
                taskEnd,
                listOf(taskHierarchy),
                listOf(schedule)
        )

        task.check(
                Interval.Ended(Type.NoSchedule(), taskStart.toOffset(), hierarchyStart.toOffset()),
                Interval.Ended(Type.Child(taskHierarchy), hierarchyStart.toOffset(), nothingStart1.toOffset()),
                Interval.Ended(Type.NoSchedule(), nothingStart1.toOffset(), scheduleStart.toOffset()),
                Interval.Ended(Type.Schedule(listOf(schedule)), scheduleStart.toOffset(), nothingStart2.toOffset()),
                Interval.Ended(Type.NoSchedule(), nothingStart2.toOffset(), taskEnd.toOffset())
        )
    }

    @Test
    fun testChildCurrentOverlapScheduleCurrentOverlapChild() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())
        val taskHierarchy1 = taskHierarchyMock(taskStart)

        val scheduleStart = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val schedule = scheduleMock(scheduleStart)

        val hierarchy2Start = ExactTimeStamp.Local(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy2 = taskHierarchyMock(hierarchy2Start)

        val task = taskMock(
                taskStart,
                taskHierarchies = listOf(taskHierarchy1, taskHierarchy2),
                scheduleList = listOf(schedule)
        )

        task.check(
                Interval.Ended(Type.Child(taskHierarchy1), taskStart.toOffset(), scheduleStart.toOffset()),
                Interval.Ended(Type.Schedule(listOf(schedule)), scheduleStart.toOffset(), hierarchy2Start.toOffset()),
                Interval.Current(Type.Child(taskHierarchy2), hierarchy2Start.toOffset())
        )
    }

    @Test
    fun testChildEndedOverlapScheduleEndedOverlapChild() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())
        val futureEnd = ExactTimeStamp.Local(date, HourMinute(13, 0).toHourMilli())
        val taskHierarchy1 = taskHierarchyMock(taskStart, futureEnd)

        val scheduleStart = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val schedule = scheduleMock(scheduleStart, futureEnd)

        val hierarchy2Start = ExactTimeStamp.Local(date, HourMinute(12, 2).toHourMilli())
        val taskHierarchy2 = taskHierarchyMock(hierarchy2Start, futureEnd)

        val task = taskMock(
                taskStart,
                taskHierarchies = listOf(taskHierarchy1, taskHierarchy2),
                scheduleList = listOf(schedule)
        )

        task.check(
                Interval.Ended(Type.Child(taskHierarchy1), taskStart.toOffset(), scheduleStart.toOffset()),
                Interval.Ended(Type.Schedule(listOf(schedule)), scheduleStart.toOffset(), hierarchy2Start.toOffset()),
                Interval.Ended(Type.Child(taskHierarchy2), hierarchy2Start.toOffset(), futureEnd.toOffset()),
                Interval.Current(Type.NoSchedule(), futureEnd.toOffset())
        )
    }

    @Test
    fun testNothingScheduleTwoStarts() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())

        val schedule1Start = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val schedule1 = scheduleMock(schedule1Start)

        val schedule2Start = ExactTimeStamp.Local(date, HourMinute(12, 2).toHourMilli())
        val schedule2 = scheduleMock(schedule2Start)

        val task = taskMock(
                taskStart,
                scheduleList = listOf(schedule1, schedule2)
        )

        task.check(
                Interval.Ended(Type.NoSchedule(), taskStart.toOffset(), schedule1Start.toOffset()),
                Interval.Current(Type.Schedule(listOf(schedule1, schedule2)), schedule1Start.toOffset())
        )
    }

    @Test
    fun testScheduleOneEnd() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())

        val schedule1End = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val schedule1 = scheduleMock(taskStart, schedule1End)

        val schedule2 = scheduleMock(taskStart)

        val task = taskMock(
                taskStart,
                scheduleList = listOf(schedule1, schedule2)
        )

        task.check(Interval.Current(Type.Schedule(listOf(schedule1, schedule2)), taskStart.toOffset()))
    }

    @Test
    fun testScheduleTwoEnds() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())

        val schedule1End = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val schedule1 = scheduleMock(taskStart, schedule1End)

        val schedule2End = ExactTimeStamp.Local(date, HourMinute(12, 2).toHourMilli())
        val schedule2 = scheduleMock(taskStart, schedule2End)

        val task = taskMock(
                taskStart,
                scheduleList = listOf(schedule1, schedule2)
        )

        task.check(
                Interval.Ended(Type.Schedule(listOf(schedule1, schedule2)), taskStart.toOffset(), schedule2End.toOffset()),
                Interval.Current(Type.NoSchedule(), schedule2End.toOffset())
        )
    }

    @Test
    fun testScheduleRemoved() {
        val taskStart = ExactTimeStamp.Local(date, HourMinute(12, 0).toHourMilli())

        val scheduleEnd = ExactTimeStamp.Local(date, HourMinute(12, 1).toHourMilli())
        val schedule = scheduleMock(taskStart, scheduleEnd)

        val noScheduleOrParent = noScheduleOrParentMock(scheduleEnd)

        val task = taskMock(
                taskStart,
                scheduleList = listOf(schedule),
                noScheduleOrParentList = listOf(noScheduleOrParent)
        )

        task.check(
                Interval.Ended(Type.Schedule(listOf(schedule)), taskStart.toOffset(), scheduleEnd.toOffset()),
                Interval.Current(Type.NoSchedule(noScheduleOrParent), scheduleEnd.toOffset())
        )
    }
}