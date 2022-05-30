package com.krystianwsul.common.firebase.models.project

import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType

sealed interface SharedProject : Project<ProjectType.Shared> {

    override val projectKey: ProjectKey.Shared

    val name: String
}