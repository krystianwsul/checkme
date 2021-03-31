package com.krystianwsul.checkme.gui.edit

import android.os.Parcelable
import com.krystianwsul.common.utils.UserKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParentScheduleState(val schedules: List<ScheduleEntry>, val assignedTo: Set<UserKey>) : Parcelable {

    companion object {

        fun create(
                assignedTo: Set<UserKey>,
                schedules: List<ScheduleEntry>? = null,
        ) = ParentScheduleState(schedules.orEmpty().toMutableList(), assignedTo)
    }

    override fun hashCode() = getScheduleDatas().hashCode()

    fun getScheduleDatas() = schedules.map { it.scheduleDataWrapper }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true

        if (other !is ParentScheduleState) return false

        return getScheduleDatas() == other.getScheduleDatas() && assignedTo == other.assignedTo
    }
}