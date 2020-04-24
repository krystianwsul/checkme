package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.DatabaseEvent
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.firebase.managers.AndroidRemoteRootUserManager
import com.krystianwsul.checkme.firebase.managers.StrangerProjectManager
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey

class RemoteFriendFactory(children: Iterable<Snapshot>) {

    companion object {

        private fun Map<UserKey, Pair<RootUserRecord, Boolean>>.toRootUsers() = values.map { RootUser(it.first) }
                .associateBy { it.id }
                .toMutableMap()
    }

    private val remoteFriendManager = AndroidRemoteRootUserManager(children)
    private val strangerProjectManager = StrangerProjectManager()

    private var _friends = remoteFriendManager.remoteRootUserRecords.toRootUsers()

    val isSaved get() = remoteFriendManager.isSaved

    val friends: Collection<RootUser> get() = _friends.values

    fun save(values: MutableMap<String, Any?>) {
        strangerProjectManager.save(values)
        remoteFriendManager.save(values)
    }

    fun getUserJsons(friendIds: Set<UserKey>): MutableMap<UserKey, UserJson> {
        check(friendIds.all { _friends.containsKey(it) })

        return _friends.entries
                .filter { friendIds.contains(it.key) }
                .associateBy({ it.key }, { it.value.userJson })
                .toMutableMap()
    }

    fun getFriend(friendId: UserKey): RootUser {
        check(_friends.containsKey(friendId))

        return _friends[friendId]!!
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

    fun onDatabaseEvent(databaseEvent: DatabaseEvent): Boolean {
        val userKey = UserKey(databaseEvent.key)
        val friendPair = remoteFriendManager.remoteRootUserRecords[userKey]

        if (friendPair?.second == true) {
            remoteFriendManager.remoteRootUserRecords[userKey] = Pair(friendPair.first, false)

            return true
        } else {
            when (databaseEvent) {
                is DatabaseEvent.AddChange -> {
                    val remoteFriendRecord = remoteFriendManager.setFriend(databaseEvent.dataSnapshot)

                    _friends[userKey] = RootUser(remoteFriendRecord)
                }
                is DatabaseEvent.Remove -> {
                    remoteFriendManager.removeFriend(userKey)
                    _friends.remove(userKey)
                }
            }

            return false
        }
    }

    fun addFriend(userKey: UserKey, userWrapper: UserWrapper) {
        check(!_friends.containsKey(userKey))

        _friends[userKey] = RootUser(remoteFriendManager.addFriend(userKey, userWrapper))
    }
}
