package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.firebase.models.cache.ClearableInvalidatableManager
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.users.ProjectUser
import com.krystianwsul.common.firebase.records.AssignedToHelper
import com.krystianwsul.common.firebase.records.project.SharedForeignProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

class SharedForeignProject(
    override val projectRecord: SharedForeignProjectRecord,
    userCustomTimeProvider: JsonTime.UserCustomTimeProvider,
    override val rootModelChangeManager: RootModelChangeManager,
) : ForeignProject<ProjectType.Shared>,
    SharedProjectProperties,
    JsonTime.CustomTimeProvider by JsonTime.CustomTimeProvider.getForRootTask(userCustomTimeProvider) {

    override val assignedToHelper = AssignedToHelper.Shared

    override val clearableInvalidatableManager = ClearableInvalidatableManager()

    override val projectKey = projectRecord.projectKey

    override val name get() = projectRecord.name

    private val remoteUsers = projectRecord.userRecords.mapValues { ProjectUser(it.value) }

    override val users get() = remoteUsers.values

    override fun getAssignedTo(userKeys: Set<UserKey>) = remoteUsers.filterKeys { it in userKeys }
}