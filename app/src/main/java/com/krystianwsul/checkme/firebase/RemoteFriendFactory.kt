package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.loaders.FactoryProvider
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.firebase.managers.AndroidRemoteRootUserManager
import com.krystianwsul.checkme.firebase.managers.StrangerProjectManager
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey

class RemoteFriendFactory(
        children: Iterable<Snapshot>,
        database: FactoryProvider.Database
) {

    companion object {

        private fun Map<UserKey, RootUserRecord>.toRootUsers() = values.map { RootUser(it) }
                .associateBy { it.id }
                .toMutableMap()
    }

    private val remoteFriendManager = AndroidRemoteRootUserManager(children, database)
    private val strangerProjectManager = StrangerProjectManager()

    private var _friends = remoteFriendManager.remoteRootUserRecords.toRootUsers()

    var isSaved
        get() = remoteFriendManager.isSaved
        set(value) {
            remoteFriendManager.isSaved = value
        }

    val friends: Collection<RootUser> get() = _friends.values

    fun save(values: MutableMap<String, Any?>) {
        strangerProjectManager.save(values)
        remoteFriendManager.save(values)
    }

    fun getUserJsons(friendIds: Set<UserKey>): Map<UserKey, UserJson> {
        check(friendIds.all { _friends.containsKey(it) })

        return _friends.entries
                .filter { friendIds.contains(it.key) }
                .associateBy({ it.key }, { it.value.userJson })
    }

    fun getFriend(friendId: UserKey): RootUser {
        check(_friends.containsKey(friendId))

        return _friends[friendId]!!
    }

    fun removeFriend(userKey: UserKey, friendId: UserKey) {
        check(_friends.containsKey(friendId))

        _friends[friendId]!!.removeFriendOf(userKey)

        _friends.remove(friendId)
    }

    fun updateProjects(
            projectId: ProjectKey.Shared,
            addedUsers: Set<UserKey>,
            removedUsers: Set<UserKey>
    ) {
        val addedFriends = addedUsers.mapNotNull(_friends::get)
        val addedStrangers = addedUsers - addedFriends.map { it.id }

        val removedFriends = removedUsers.mapNotNull(_friends::get)
        val removedStrangers = removedUsers - removedFriends.map { it.id }

        addedFriends.forEach { it.addProject(projectId) }
        removedFriends.forEach { it.removeProject(projectId) }

        strangerProjectManager.updateStrangerProjects(projectId, addedStrangers, removedStrangers)
    }

    fun onNewSnapshot(children: Iterable<Snapshot>) {
        _friends = remoteFriendManager.onNewSnapshot(children).toRootUsers()
    }
}
