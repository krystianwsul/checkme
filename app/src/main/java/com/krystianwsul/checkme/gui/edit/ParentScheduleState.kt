package com.krystianwsul.checkme.gui.edit

import android.os.Parcelable
import com.krystianwsul.checkme.viewmodels.EditViewModel
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ParentScheduleState(
        val parentKey: EditViewModel.ParentKey?,
        val schedules: List<ScheduleEntry>
) : Parcelable {

    companion object {

        fun create(
                parentKey: EditViewModel.ParentKey?,
                schedules: List<ScheduleEntry>? = null
        ) = ParentScheduleState(parentKey, schedules.orEmpty().toMutableList())
    }

    override fun hashCode() = (parentKey?.hashCode() ?: 0) * 32 + getScheduleDatas().hashCode()

    fun getScheduleDatas() = schedules.map { it.scheduleDataWrapper }

    override fun equals(other: Any?): Boolean {
        if (other === this)
            return true

        if (other !is ParentScheduleState)
            return false

        return (parentKey == other.parentKey && getScheduleDatas() == other.getScheduleDatas())
    }
}