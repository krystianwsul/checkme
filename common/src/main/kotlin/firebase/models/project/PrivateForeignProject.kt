package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.firebase.models.cache.ClearableInvalidatableManager
import com.krystianwsul.common.firebase.models.users.ProjectUser
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.PrivateForeignProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

class PrivateForeignProject(
    override val projectRecord: PrivateForeignProjectRecord,
    userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
) : ForeignProject<ProjectType.Private>,
    PrivateProject,
    JsonTime.CustomTimeProvider by JsonTime.CustomTimeProvider.getForRootTask(userCustomTimeProvider) {

    override val assignedToHelper = AssignedToHelper.Private

    override val clearableInvalidatableManager = ClearableInvalidatableManager()

    override val projectKey = projectRecord.projectKey

    override fun getAssignedTo(userKeys: Set<UserKey>) = mapOf<UserKey, ProjectUser>()
}