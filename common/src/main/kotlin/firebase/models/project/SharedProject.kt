package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.firebase.models.users.ProjectUser
import com.krystianwsul.common.utils.ProjectKey

sealed interface SharedProject {

    val projectKey: ProjectKey.Shared

    val name: String

    val users: Collection<ProjectUser>
}