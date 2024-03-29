package com.krystianwsul.common.firebase.records.users

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.RootUserProperties
import com.krystianwsul.common.firebase.json.customtimes.UserCustomTimeJson
import com.krystianwsul.common.firebase.json.users.ProjectOrdinalEntryJson
import com.krystianwsul.common.firebase.json.users.UserWrapper
import com.krystianwsul.common.firebase.records.RemoteRecord
import com.krystianwsul.common.firebase.records.customtime.UserCustomTimeRecord
import com.krystianwsul.common.utils.CustomTimeId
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey


open class RootUserRecord(
    protected val databaseWrapper: DatabaseWrapper,
    create: Boolean,
    final override val userWrapper: UserWrapper,
    override val userKey: UserKey,
) : RemoteRecord(create), RootUserProperties {

    companion object {

        const val USER_DATA = "userData"
        private const val FRIENDS = "friends"
        const val PROJECTS = "projects"
        const val ORDINAL_ENTRIES = "ordinalEntries"
    }

    final override val userJson by lazy { userWrapper.userData }

    final override val key by lazy { userKey.key }

    override val children get() = customTimeRecords.values.toList()

    override var name by Committer(userJson::name, "$key/$USER_DATA")

    override val email by lazy { userJson.email }

    override val photoUrl get() = userJson.photoUrl

    override val projectIds
        get() = userWrapper.projects
                .keys
                .map { ProjectKey.Shared(it) }
                .toSet()

    override val friends
        get() = userWrapper.friends
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

    override val createObject
        get() = userWrapper.apply {
            customTimes = customTimeRecords.values
                    .associate { it.id.toString() to it.createObject }
                    .toMutableMap()
        }

    override fun addFriend(userKey: UserKey) {
        val friendId = userKey.key
        check(!userWrapper.friends.containsKey(friendId))

        userWrapper.friends[friendId] = true

        addValue("$key/$FRIENDS/$friendId", true)
    }

    override fun removeFriend(userKey: UserKey) {
        val friendId = userKey.key

        val friends = userWrapper.friends
        check(friends.containsKey(friendId))
        checkNotNull(friends[friendId])

        friends.remove(friendId)

        addValue("$key/$FRIENDS/$friendId", null)
    }

    override fun deleteFromParent() = throw UnsupportedOperationException()

    override fun addProject(projectKey: ProjectKey.Shared) {
        val projectId = projectKey.key

        if (!userWrapper.projects.containsKey(projectId)) {
            userWrapper.projects[projectId] = true

            addValue("$key/$PROJECTS/$projectId", true)
        }
    }

    override fun removeProject(projectKey: ProjectKey.Shared) {
        val projectId = projectKey.key

        if (userWrapper.projects.containsKey(projectId)) {
            userWrapper.projects.remove(projectId)

            addValue("$key/$PROJECTS/$projectId", null)
        }
    }

    fun newCustomTimeId() = CustomTimeId.User(databaseWrapper.newRootUserCustomTimeId(userKey))

    fun newCustomTimeRecord(customTimeJson: UserCustomTimeJson): UserCustomTimeRecord {
        val remoteCustomTimeRecord = UserCustomTimeRecord(this, customTimeJson)
        check(!customTimeRecords.containsKey(remoteCustomTimeRecord.id))

        userWrapper.customTimes[remoteCustomTimeRecord.key] = customTimeJson
        customTimeRecords[remoteCustomTimeRecord.id] = remoteCustomTimeRecord

        return remoteCustomTimeRecord
    }

    // this doesn't diff, but it's fired from the backend, so I'm not too concerned about bandwidth
    fun setOrdinalEntries(projectKey: ProjectKey.Shared, ordinalEntries: Map<String, ProjectOrdinalEntryJson>) {
        userWrapper.ordinalEntries[projectKey.key] = ordinalEntries.toMutableMap()

        addValue("$key/$ORDINAL_ENTRIES/${projectKey.key}", ordinalEntries)
    }
}
