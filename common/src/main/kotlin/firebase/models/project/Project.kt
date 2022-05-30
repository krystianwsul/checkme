package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.firebase.models.cache.ClearableInvalidatableManager
import com.krystianwsul.common.firebase.models.users.ProjectUser
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

sealed interface Project<T : ProjectType> : JsonTime.CustomTimeProvider {

    val assignedToHelper: AssignedToHelper

    val clearableInvalidatableManager: ClearableInvalidatableManager

    val projectRecord: ProjectRecord<T>

    val projectKey: ProjectKey<T>

    val users: Collection<ProjectUser>

    fun getAssignedTo(userKeys: Set<UserKey>): Map<UserKey, ProjectUser>
}
