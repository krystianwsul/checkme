package com.krystianwsul.checkme.gui.edit

import android.os.Parcelable
import com.krystianwsul.checkme.viewmodels.EditViewModel
import kotlinx.parcelize.Parcelize
import kotlin.random.Random

@Parcelize
data class ScheduleEntry(
        val scheduleDataWrapper: EditViewModel.ScheduleDataWrapper,
        var id: Int = Random.nextInt()
) : Parcelable
