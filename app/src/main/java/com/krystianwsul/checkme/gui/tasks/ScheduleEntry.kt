package com.krystianwsul.checkme.gui.tasks

import android.os.Parcelable
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import kotlinx.android.parcel.Parcelize

@Parcelize
class ScheduleEntry(val scheduleData: CreateTaskViewModel.ScheduleData, var error: String? = null) : Parcelable
