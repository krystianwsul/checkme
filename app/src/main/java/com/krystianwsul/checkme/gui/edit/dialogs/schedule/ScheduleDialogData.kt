package com.krystianwsul.checkme.gui.edit.dialogs.schedule

import android.os.Parcelable
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.checkme.gui.edit.ScheduleEntry
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.TimePairPersist
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.ScheduleType
import kotlinx.parcelize.Parcelize
import kotlin.random.Random

@Parcelize
data class ScheduleDialogData(
        var date: Date,
        var daysOfWeek: Set<DayOfWeek>,
        var monthlyDay: Boolean,
        var monthDayNumber: Int,
        var monthWeekNumber: Int,
        var monthWeekDay: DayOfWeek,
        var beginningOfMonth: Boolean,
        val timePairPersist: TimePairPersist,
        var scheduleType: ScheduleType,
        var from: Date?,
        var until: Date?,
        var interval: Int,
) : Parcelable {

    companion object {

        const val MAX_MONTH_DAY = 28
    }

    init {
        check(monthDayNumber > 0)
        check(monthDayNumber <= MAX_MONTH_DAY)
        check(monthWeekNumber > 0)
        check(monthWeekNumber < 5)
    }

    fun toScheduleEntry(id: Int? = null) =
            ScheduleEntry(
                    when (scheduleType) {
                        ScheduleType.SINGLE -> EditViewModel.ScheduleDataWrapper.Single(
                                ScheduleData.Single(
                                        date,
                                        timePairPersist.timePair
                                )
                        )
                        ScheduleType.WEEKLY -> EditViewModel.ScheduleDataWrapper.Weekly(
                                ScheduleData.Weekly(
                                        daysOfWeek,
                                        timePairPersist.timePair,
                                        from,
                                        until,
                                        interval
                                )
                        )
                        ScheduleType.MONTHLY_DAY -> EditViewModel.ScheduleDataWrapper.MonthlyDay(
                                ScheduleData.MonthlyDay(
                                        monthDayNumber,
                                        beginningOfMonth,
                                        timePairPersist.timePair,
                                        from,
                                        until
                                )
                        )
                        ScheduleType.MONTHLY_WEEK -> EditViewModel.ScheduleDataWrapper.MonthlyWeek(
                                ScheduleData.MonthlyWeek(
                                        monthWeekNumber,
                                        monthWeekDay,
                                        beginningOfMonth,
                                        timePairPersist.timePair,
                                        from,
                                        until
                                )
                        )
                        ScheduleType.YEARLY -> EditViewModel.ScheduleDataWrapper.Yearly(
                                ScheduleData.Yearly(
                                        date.month,
                                        date.day,
                                        timePairPersist.timePair,
                                        from,
                                        until
                                )
                        )
                    },
                    id ?: Random.nextInt()
            )
}