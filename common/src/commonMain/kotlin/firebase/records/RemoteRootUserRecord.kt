package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.RemoteRootUserProperties


open class RemoteRootUserRecord(create: Boolean, override val createObject: UserWrapper) : RemoteRecord(create), RemoteRootUserProperties {

    companion object {

        const val USER_DATA = "userData"
        private const val FRIEND_OF = "friendOf"
        const val PROJECTS = "projects"
    }

    final override val userJson by lazy { createObject.userData }

    override val id by lazy { UserData.getKey(userJson.email) }

    final override val key by lazy { id }

    override var name by Committer(userJson::name, "$key/$USER_DATA")

    override val email by lazy { userJson.email }

    override val photoUrl get() = userJson.photoUrl

    override val projectIds
        get() = createObject.projects
                .keys
                .toSet()

    override fun removeFriend(friendId: String) {
        check(friendId.isNotEmpty())

        val friendOf = createObject.friendOf
        check(friendOf.containsKey(friendId))
        checkNotNull(friendOf[friendId])

        friendOf.remove(friendId)

        addValue("$key/$FRIEND_OF/$friendId", null)
    }

    override fun deleteFromParent() = throw UnsupportedOperationException()

    override fun addProject(projectId: String) {
        if (!createObject.projects.containsKey(projectId)) {
            createObject.projects[projectId] = true

            addValue("$key/$PROJECTS/$projectId", true)
        }
    }

    override fun removeProject(projectId: String) {
        if (createObject.projects.containsKey(projectId)) {
            createObject.projects.remove(projectId)

            addValue("$key/$PROJECTS/$projectId", null)
        }
    }
}
