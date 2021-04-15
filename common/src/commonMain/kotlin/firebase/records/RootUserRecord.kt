package com.krystianwsul.common.firebase.records

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.RootUserProperties
import com.krystianwsul.common.firebase.json.PrivateCustomTimeJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey


open class RootUserRecord(
        private val databaseWrapper: DatabaseWrapper,
        create: Boolean,
        final override val createObject: UserWrapper,
        override val userKey: UserKey,
) : RemoteRecord(create), RootUserProperties {

    companion object {

        const val USER_DATA = "userData"
        private const val FRIENDS = "friends"
        const val PROJECTS = "projects"
    }

    final override val userJson by lazy { createObject.userData }

    final override val userWrapper = createObject

    final override val key by lazy { userKey.key }

    override val children get() = customTimeRecords.values.toList()

    override var name by Committer(userJson::name, "$key/$USER_DATA")

    override val email by lazy { userJson.email }

    override val photoUrl get() = userJson.photoUrl

    override val projectIds
        get() = createObject.projects
                .keys
                .map { ProjectKey.Shared(it) }
                .toSet()

    override val friends
        get() = createObject.friends
                .keys
                .map(::UserKey)
                .toSet()

    val customTimeRecords = userWrapper.customTimes
            .entries
            .associate { (key, customTimeJson) ->
                val customTimeId = CustomTimeId.User(key)
                val customTimeRecord = UserCustomTimeRecord(customTimeId, this, customTimeJson)

                customTimeId to customTimeRecord
            }
            .toMutableMap()

    override fun addFriend(userKey: UserKey) {
        val friendId = userKey.key
        check(!createObject.friends.containsKey(friendId))

        createObject.friends[friendId] = true

        addValue("$key/$FRIENDS/$friendId", true)
    }

    override fun removeFriend(userKey: UserKey) {
        val friendId = userKey.key

        val friends = createObject.friends
        check(friends.containsKey(friendId))
        checkNotNull(friends[friendId])

        friends.remove(friendId)

        addValue("$key/$FRIENDS/$friendId", null)
    }

    override fun deleteFromParent() = throw UnsupportedOperationException()

    override fun addProject(projectKey: ProjectKey.Shared) {
        val projectId = projectKey.key

        if (!createObject.projects.containsKey(projectId)) {
            createObject.projects[projectId] = true

            addValue("$key/$PROJECTS/$projectId", true)
        }
    }

    override fun removeProject(projectKey: ProjectKey.Shared): Boolean {
        val projectId = projectKey.key

        return if (createObject.projects.containsKey(projectId)) {
            createObject.projects.remove(projectId)

            addValue("$key/$PROJECTS/$projectId", null)

            true
        } else {
            false
        }
    }

    fun newCustomTimeId() = CustomTimeId.User(databaseWrapper.newRootUserCustomTimeId(userKey))

    fun newCustomTimeRecord(customTimeJson: PrivateCustomTimeJson): UserCustomTimeRecord {
        val remoteCustomTimeRecord = UserCustomTimeRecord(this, customTimeJson)
        check(!customTimeRecords.containsKey(remoteCustomTimeRecord.id))

        userWrapper.customTimes[remoteCustomTimeRecord.key] = customTimeJson
        customTimeRecords[remoteCustomTimeRecord.id] = remoteCustomTimeRecord

        return remoteCustomTimeRecord
    }
}
