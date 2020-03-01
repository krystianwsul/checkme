package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.utils.ProjectKey

interface RemoteRootUserProperties {

    val id: ProjectKey.Private
    val name: String
    val email: String
    val userJson: UserJson
    val photoUrl: String?
    val projectIds: Set<ProjectKey>

    fun removeFriend(userKey: ProjectKey.Private)

    fun addProject(projectKey: ProjectKey)
    fun removeProject(projectKey: ProjectKey)
}