package com.krystianwsul.checkme.gui.edit

import android.os.Bundle
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable

interface ParentScheduleManager {

    val parent: Parent?
    val parentObservable: Observable<NullableWrapper<Parent>>

    val schedules: List<ScheduleEntry>
    val scheduleObservable: Observable<List<ScheduleEntry>>

    var assignedTo: Set<UserKey>
    val assignedToObservable: Observable<Set<UserKey>>

    val assignedToUsers: Map<UserKey, EditViewModel.UserData>

    val changed: Boolean

    fun setNewParent(newParent: Parent?)
    fun clearParent()
    fun clearParentAndReplaceSchedules()

    fun addSchedule(scheduleEntry: ScheduleEntry)
    fun setSchedule(position: Int, scheduleEntry: ScheduleEntry)
    fun removeSchedule(schedulePosition: Int)

    fun saveState(): Bundle

    fun removeAssignedTo(userKey: UserKey)

    interface Callbacks {

        fun getInitialParent(): Parent?

        fun storeParent(parentKey: EditViewModel.ParentKey?)
    }

    sealed interface Parent {

        val name: String
        val parentKey: EditViewModel.ParentKey
        val projectUsers: Map<UserKey, EditViewModel.UserData>
        val projectKey: ProjectKey<*>
        val hasMultipleInstances: Boolean?
        val clearParentTaskData: Triple<Project?, List<EditViewModel.ScheduleDataWrapper>, Set<UserKey>>?
        val compatibleWithSchedule: Boolean

        data class Project(
            override val name: String,
            override val parentKey: EditViewModel.ParentKey.Project,
            override val projectUsers: Map<UserKey, EditViewModel.UserData>,
            override val projectKey: ProjectKey.Shared,
        ) : Parent {

            override val hasMultipleInstances: Boolean? = null

            override val clearParentTaskData: Triple<Project?, List<EditViewModel.ScheduleDataWrapper>, Set<UserKey>>? = null

            override val compatibleWithSchedule = true
        }

        data class Task(
            override val name: String,
            override val parentKey: EditViewModel.ParentKey.Task,
            override val projectKey: ProjectKey<*>,
            override val hasMultipleInstances: Boolean?,
            override val clearParentTaskData: Triple<Project?, List<EditViewModel.ScheduleDataWrapper>, Set<UserKey>>?,
            val topLevelTaskIsSingleSchedule: Boolean,
        ) : Parent {

            override val projectUsers = mapOf<UserKey, EditViewModel.UserData>()

            override val compatibleWithSchedule = false
        }
    }
}