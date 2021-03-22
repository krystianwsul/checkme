package com.krystianwsul.checkme.gui.edit

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.random.Random

@Parcelize
data class ScheduleEntry(
        val scheduleDataWrapper: EditViewModel.ScheduleDataWrapper,
        var id: Int = Random.nextInt()
) : Parcelable
