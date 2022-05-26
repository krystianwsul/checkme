package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.firebase.models.cache.ClearableInvalidatableManager
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.users.ProjectUser
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.time.DateTime
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.InstanceScheduleKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

abstract class Project<T : ProjectType>(
    val assignedToHelper: AssignedToHelper,
    val rootModelChangeManager: RootModelChangeManager,
) : JsonTime.CustomTimeProvider {

    val clearableInvalidatableManager = ClearableInvalidatableManager()

    abstract val projectRecord: ProjectRecord<T>

    abstract val projectKey: ProjectKey<T>

    fun getTime(timePair: TimePair) = timePair.customTimeKey
        ?.let(::getCustomTime)
        ?: Time.Normal(timePair.hourMinute!!)

    fun getDateTime(instanceScheduleKey: InstanceScheduleKey) =
        DateTime(instanceScheduleKey.scheduleDate, getTime(instanceScheduleKey.scheduleTimePair))

    abstract fun getAssignedTo(userKeys: Set<UserKey>): Map<UserKey, ProjectUser>
}
