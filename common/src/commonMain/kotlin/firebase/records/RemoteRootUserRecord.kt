package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.UserData
import com.krystianwsul.common.firebase.json.UserWrapper


open class RemoteRootUserRecord(create: Boolean, override val createObject: UserWrapper) : RemoteRecord(create) {

    companion object {

        const val USER_DATA = "userData"
        private const val FRIEND_OF = "friendOf"
        const val PROJECTS = "projects"
    }

    val userJson by lazy { createObject.userData }

    val id by lazy { UserData.getKey(userJson.email) }

    final override val key by lazy { id }

    var name by Committer(userJson::name, "$key/$USER_DATA")

    val email by lazy { userJson.email }

    open val photoUrl get() = userJson.photoUrl

    fun removeFriendOf(friendId: String) {
        check(friendId.isNotEmpty())

        val friendOf = createObject.friendOf
        check(friendOf.containsKey(friendId))
        checkNotNull(friendOf[friendId])

        friendOf.remove(friendId)

        addValue("$key/$FRIEND_OF/$friendId", null)
    }

    override fun deleteFromParent() = throw UnsupportedOperationException()

    fun addProject(projectId: String) {
        if (!createObject.projects.containsKey(projectId)) {
            createObject.projects[projectId] = true

            addValue("$key/$PROJECTS/$projectId", true)
        }
    }

    fun removeProject(projectId: String) {
        if (createObject.projects.containsKey(projectId)) {
            createObject.projects.remove(projectId)

            addValue("$key/$PROJECTS/$projectId", null)
        }
    }
}
