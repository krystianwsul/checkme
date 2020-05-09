package com.krystianwsul.checkme.gui.tasks

import android.os.Parcelable
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import kotlinx.android.parcel.Parcelize
import kotlin.random.Random

@Parcelize
data class ScheduleEntry(
        val scheduleDataWrapper: CreateTaskViewModel.ScheduleDataWrapper,
        var id: Int = Random.nextInt(),
        val error: String? = null
) : Parcelable
