package com.krystianwsul.common.firebase.models

import com.krystianwsul.common.firebase.json.UserJson

interface RemoteRootUserProperties {

    val id: String
    val name: String
    val email: String
    val userJson: UserJson
    val photoUrl: String?
    val projectIds: Set<String>

    fun removeFriend(friendId: String)

    fun addProject(projectId: String)
    fun removeProject(projectId: String)
}