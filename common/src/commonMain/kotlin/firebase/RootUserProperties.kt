package com.krystianwsul.common.firebase

import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.UserKey

interface RootUserProperties {

    val id: UserKey
    val name: String
    val email: String
    val userJson: UserJson
    val photoUrl: String?
    val projectIds: Set<ProjectKey.Shared>
    val friends: Set<UserKey>

    fun addFriend(userKey: UserKey)
    fun removeFriend(userKey: UserKey)

    fun removeFriendOf(userKey: UserKey)

    fun addProject(projectKey: ProjectKey.Shared)
    fun removeProject(projectKey: ProjectKey<ProjectType.Shared>): Boolean
}