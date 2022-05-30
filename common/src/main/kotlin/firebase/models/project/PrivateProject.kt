package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.firebase.models.users.ProjectUser
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

sealed interface PrivateProject : Project<ProjectType.Private> {

    override val projectKey: ProjectKey.Private

    val ownerName: String

    override val users get() = emptyList<ProjectUser>()
}