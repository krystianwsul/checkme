package com.krystianwsul.common.firebase.models.schedule


import com.krystianwsul.common.FeatureFlagManager
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.records.schedule.YearlyScheduleRecord
import com.krystianwsul.common.time.*
import com.krystianwsul.common.utils.ScheduleType
import com.soywiz.klock.days
import com.soywiz.klock.plus

class YearlySchedule(topLevelTask: Task, override val repeatingScheduleRecord: YearlyScheduleRecord) :
        RepeatingSchedule(topLevelTask) {

    override val scheduleRecord get() = repeatingScheduleRecord

    val month get() = repeatingScheduleRecord.month
    val day get() = repeatingScheduleRecord.day

    override val scheduleType = ScheduleType.YEARLY

    override val dateTimeSequenceGenerator: DateTimeSequenceGenerator = ProxyDateTimeSequenceGenerator(
        YearlyDateTimeSequenceGenerator(),
        NewDateTimeSequenceGenerator(),
        FeatureFlagManager.Flag.NEW_YEARLY_SCHEDULE,
    )

    fun getDateInYear(year: Int) = Date(year, month, day)

    private fun containsDate(date: Date): Boolean {
        val dateThisYear = getDateInYear(date.year)

        return dateThisYear == date
    }

    private inner class YearlyDateTimeSequenceGenerator : DailyDateTimeSequenceGenerator() {

        override fun containsDate(date: Date) = this@YearlySchedule.containsDate(date)
    }

    class ProxyDateTimeSequenceGenerator(
        private val oldDateTimeSequenceGenerator: DateTimeSequenceGenerator,
        private val newDateTimeSequenceGenerator: DateTimeSequenceGenerator,
        private val flag: FeatureFlagManager.Flag,
    ) : DateTimeSequenceGenerator {

        override fun generate(startExactTimeStamp: ExactTimeStamp, endExactTimeStamp: ExactTimeStamp?): Sequence<DateTime> {
            val generator = if (FeatureFlagManager.getFlag(flag)) {
                newDateTimeSequenceGenerator
            } else {
                oldDateTimeSequenceGenerator
            }

            return generator.generate(startExactTimeStamp, endExactTimeStamp)
        }
    }

    private inner class NewDateTimeSequenceGenerator : DateTimeSequenceGenerator {

        fun getNextValidDate(startDateSoy: DateSoy): DateSoy {
            val date = startDateSoy.toDate()

            return if (containsDate(date)) {
                startDateSoy
            } else {
                getDateInYear(date.year + 1).toDateSoy()
            }
        }

        override fun generate(
            startExactTimeStamp: ExactTimeStamp,
            endExactTimeStamp: ExactTimeStamp?,
        ): Sequence<DateTime> {
            val startSoyDate = startExactTimeStamp.date.toDateSoy()
            var currentSoyDate = getNextValidDate(startSoyDate)

            val endSoyDate = endExactTimeStamp?.date?.toDateSoy()

            return generateSequence {
                var endHourMilli: HourMilli? = null
                if (endSoyDate != null) {
                    val comparison = currentSoyDate.compareTo(endSoyDate)
                    if (comparison > 0) { // passed the end
                        return@generateSequence null
                    } else if (comparison == 0) { // last day
                        endHourMilli = endExactTimeStamp.hourMilli
                    }
                }

                // first day
                val startHourMilli = if (startSoyDate == currentSoyDate) startExactTimeStamp.hourMilli else null

                val date = currentSoyDate.toDate()
                currentSoyDate = getNextValidDate(currentSoyDate + 1.days)

                getDateTimeInDate(date, startHourMilli, endHourMilli) ?: Unit
            }.filterIsInstance<DateTime>()
        }

        private fun getDateTimeInDate(
            date: Date,
            startHourMilli: HourMilli?,
            endHourMilli: HourMilli?,
        ): DateTime? {
            if (!hasInstanceInDate(date, startHourMilli, endHourMilli)) return null

            return DateTime(date, time)
        }

        private fun hasInstanceInDate(
            date: Date,
            startHourMilli: HourMilli?,
            endHourMilli: HourMilli?,
        ): Boolean {
            check(containsDate(date))

            val hourMilli by lazy { time.getHourMinute(date.dayOfWeek).toHourMilli() }

            if (startHourMilli != null && startHourMilli > hourMilli) return false
            if (endHourMilli != null && endHourMilli <= hourMilli) return false

            return true
        }
    }
}
