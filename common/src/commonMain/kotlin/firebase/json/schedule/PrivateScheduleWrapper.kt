package com.krystianwsul.common.firebase.json.schedule

import com.krystianwsul.common.utils.ProjectType
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmOverloads

@Serializable
data class PrivateScheduleWrapper @JvmOverloads constructor(
        override val singleScheduleJson: PrivateSingleScheduleJson? = null,
        override val weeklyScheduleJson: PrivateWeeklyScheduleJson? = null,
        override val monthlyDayScheduleJson: PrivateMonthlyDayScheduleJson? = null,
        override val monthlyWeekScheduleJson: PrivateMonthlyWeekScheduleJson? = null,
        override val yearlyScheduleJson: PrivateYearlyScheduleJson? = null,
) : ScheduleWrapper<ProjectType.Private>