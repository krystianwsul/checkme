package com.krystianwsul.checkme.gui.edit

import android.os.Parcelable
import com.krystianwsul.common.utils.ScheduleData
import com.krystianwsul.common.utils.UserKey
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParentScheduleState(val schedules: List<ScheduleEntry>, val assignedTo: Set<UserKey> = emptySet()) : Parcelable {

    companion object {

        val empty by lazy { create(setOf()) }

        fun create(
            assignedTo: Set<UserKey>,
            schedules: List<ScheduleEntry>? = null,
        ) = ParentScheduleState(schedules.orEmpty().toMutableList(), assignedTo)
    }

    constructor(scheduleData: ScheduleData.Single) : this(listOf(ScheduleEntry(ScheduleDataWrapper.Single(scheduleData))))

    override fun hashCode() = getScheduleDatas().hashCode()

    fun getScheduleDatas() = schedules.map { it.scheduleDataWrapper }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true

        if (other !is ParentScheduleState) return false

        return getScheduleDatas() == other.getScheduleDatas() && assignedTo == other.assignedTo
    }
}