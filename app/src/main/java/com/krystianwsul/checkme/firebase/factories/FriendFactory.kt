package com.krystianwsul.checkme.firebase.factories

import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.firebase.DatabaseEvent
import com.krystianwsul.checkme.firebase.loaders.Snapshot
import com.krystianwsul.checkme.firebase.managers.AndroidRootUserManager
import com.krystianwsul.checkme.firebase.managers.StrangerProjectManager
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey

class FriendFactory(children: Iterable<Snapshot>) {

    companion object {

        private fun Map<UserKey, Pair<RootUserRecord, Boolean>>.toRootUsers() =
                mapValues { RootUser(it.value.first) }.toMutableMap()
    }

    private val rootUserManager = AndroidRootUserManager(children)
    private val strangerProjectManager = StrangerProjectManager()

    private var _friends = rootUserManager.rootUserRecords.toRootUsers()

    val isSaved get() = rootUserManager.isSaved

    val friends: Collection<RootUser> get() = _friends.values

    val changeTypes = PublishRelay.create<ChangeType>()

    fun save(values: MutableMap<String, Any?>) {
        strangerProjectManager.save(values)
        rootUserManager.save(values)
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

    fun onDatabaseEvent(databaseEvent: DatabaseEvent) {
        val userKey = UserKey(databaseEvent.key)
        val friendPair = rootUserManager.rootUserRecords[userKey]

        if (friendPair?.second == true) {
            rootUserManager.rootUserRecords[userKey] = Pair(friendPair.first, false)

            changeTypes.accept(ChangeType.LOCAL)
        } else {
            when (databaseEvent) {
                is DatabaseEvent.AddChange -> {
                    val remoteFriendRecord = rootUserManager.setFriend(databaseEvent.dataSnapshot)

                    _friends[userKey] = RootUser(remoteFriendRecord)
                }
                is DatabaseEvent.Remove -> {
                    rootUserManager.removeFriend(userKey)
                    _friends.remove(userKey)
                }
            }

            changeTypes.accept(ChangeType.REMOTE)
        }
    }

    fun addFriend(userKey: UserKey, userWrapper: UserWrapper) {
        check(!_friends.containsKey(userKey))

        _friends[userKey] = RootUser(rootUserManager.addFriend(userKey, userWrapper))
    }
}
