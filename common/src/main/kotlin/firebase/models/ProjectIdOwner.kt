package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.utils.ProjectKey

interface ProjectIdOwner {

    fun updateProject(projectKey: ProjectKey<*>)
}