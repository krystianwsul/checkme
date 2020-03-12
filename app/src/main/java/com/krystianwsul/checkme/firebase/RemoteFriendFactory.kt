package com.krystianwsul.checkme.firebase

import com.google.firebase.database.DataSnapshot
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.managers.AndroidRemoteRootUserManager
import com.krystianwsul.checkme.firebase.managers.StrangerProjectManager
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.models.RemoteRootUser
import com.krystianwsul.common.firebase.records.RemoteRootUserRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey

class RemoteFriendFactory(domainFactory: DomainFactory, children: Iterable<DataSnapshot>) {

    companion object {

        private fun Map<UserKey, RemoteRootUserRecord>.toRootUsers() = values.map { RemoteRootUser(it) }
                .associateBy { it.id }
                .toMutableMap()
    }

    private val remoteFriendManager = AndroidRemoteRootUserManager(children)
    private val strangerProjectManager = StrangerProjectManager(domainFactory)

    private var _friends = remoteFriendManager.remoteRootUserRecords.toRootUsers()

    var isSaved
        get() = remoteFriendManager.isSaved
        set(value) {
            remoteFriendManager.isSaved = value
        }

    val friends: Collection<RemoteRootUser> get() = _friends.values

    fun save(): Boolean {
        strangerProjectManager.save()

        return remoteFriendManager.save()
    }

    fun getUserJsons(friendIds: Set<UserKey>): MutableMap<UserKey, UserJson> {
        check(friendIds.all { _friends.containsKey(it) })

        return _friends.entries
                .filter { friendIds.contains(it.key) }
                .associateBy({ it.key }, { it.value.userJson })
                .toMutableMap()
    }

    fun getFriend(friendId: UserKey): RemoteRootUser {
        check(_friends.containsKey(friendId))

        return _friends[friendId]!!
    }

    fun removeFriend(userKey: UserKey, friendId: UserKey) {
        check(_friends.containsKey(friendId))

        _friends[friendId]!!.removeFriend(userKey)

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

    fun onNewSnapshot(children: Iterable<DataSnapshot>) {
        _friends = remoteFriendManager.onNewSnapshot(children).toRootUsers()
    }
}
