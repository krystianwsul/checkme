package com.krystianwsul.checkme.gui.edit

import android.os.Parcelable
import com.krystianwsul.common.utils.UserKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParentScheduleState(
        val parentKey: EditViewModel.ParentKey?,
        val schedules: List<ScheduleEntry>,
        val assignedTo: Set<UserKey>,
) : Parcelable {

    companion object {

        fun create(
                parentKey: EditViewModel.ParentKey?,
                assignedTo: Set<UserKey>,
                schedules: List<ScheduleEntry>? = null,
        ) = ParentScheduleState(parentKey, schedules.orEmpty().toMutableList(), assignedTo)
    }

    override fun hashCode() = (parentKey?.hashCode() ?: 0) * 32 + getScheduleDatas().hashCode()

    fun getScheduleDatas() = schedules.map { it.scheduleDataWrapper }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true

        if (other !is ParentScheduleState) return false

        return parentKey == other.parentKey
                && getScheduleDatas() == other.getScheduleDatas()
                && assignedTo == other.assignedTo
    }
}