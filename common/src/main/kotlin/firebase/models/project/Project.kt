package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.firebase.models.cache.ClearableInvalidatableManager
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.users.OwnedProjectUser
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

interface Project<T : ProjectType> : JsonTime.CustomTimeProvider {

    val assignedToHelper: AssignedToHelper

    val rootModelChangeManager: RootModelChangeManager

    val clearableInvalidatableManager: ClearableInvalidatableManager

    val projectRecord: ProjectRecord<T>

    val projectKey: ProjectKey<T>

    fun getAssignedTo(userKeys: Set<UserKey>): Map<UserKey, OwnedProjectUser>
}
