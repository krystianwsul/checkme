package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey

interface RemoteRootUserProperties {

    val id: UserKey
    val name: String
    val email: String
    val userJson: UserJson
    val photoUrl: String?
    val projectIds: Set<ProjectKey>

    fun removeFriend(userKey: UserKey)

    fun addProject(projectKey: ProjectKey)
    fun removeProject(projectKey: ProjectKey)
}