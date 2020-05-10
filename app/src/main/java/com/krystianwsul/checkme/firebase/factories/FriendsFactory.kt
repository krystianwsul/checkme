package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.FriendsLoader
import com.krystianwsul.checkme.firebase.managers.AndroidRootUserManager
import com.krystianwsul.checkme.firebase.managers.StrangerProjectManager
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.merge

class FriendsFactory(
        friendsLoader: FriendsLoader,
        initialFriendsEvent: FriendsLoader.InitialFriendsEvent,
        domainDisposable: CompositeDisposable
) {

    companion object {

        private fun Map<UserKey, Pair<RootUserRecord, Boolean>>.toRootUsers() =
                mapValues { RootUser(it.value.first) }.toMutableMap()
    }

    private val rootUserManager = AndroidRootUserManager(initialFriendsEvent.snapshots)
    private val strangerProjectManager = StrangerProjectManager()

    private var _friends = rootUserManager.records.toRootUsers()

    val isSaved get() = rootUserManager.isSaved

    val friends: Collection<RootUser> get() = _friends.values

    val changeTypes: Observable<ChangeType>

    val savedList get() = rootUserManager.savedList

    init {
        val addChangeFriendChangeTypes = friendsLoader.addChangeFriendEvents.map {
            val userKey = UserKey(it.snapshot.key)
            val friendPair = rootUserManager.records[userKey]

            if (friendPair?.second == true) {
                rootUserManager.records[userKey] = Pair(friendPair.first, false)

                ChangeType.LOCAL
            } else {
                val remoteFriendRecord = rootUserManager.setFriend(it.snapshot)

                _friends[userKey] = RootUser(remoteFriendRecord)

                ChangeType.REMOTE
            }
        }

        val removeFriendsChangeTypes = friendsLoader.removeFriendEvents.map {
            it.userKeys.forEach {
                rootUserManager.remove(it)
                _friends.remove(it)
            }

            it.userChangeType
        }

        changeTypes = listOf(
                addChangeFriendChangeTypes,
                removeFriendsChangeTypes
        ).merge().publishImmediate(domainDisposable)
    }

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
        val addedStrangers = addedUsers - addedFriends.map { it.userKey }

        val removedFriends = removedUsers.mapNotNull(_friends::get)
        val removedStrangers = removedUsers - removedFriends.map { it.userKey }

        addedFriends.forEach { it.addProject(projectId) }
        removedFriends.forEach { it.removeProject(projectId) }

        strangerProjectManager.updateStrangerProjects(projectId, addedStrangers, removedStrangers)
    }

    fun addFriend(userKey: UserKey, userWrapper: UserWrapper) {
        check(!_friends.containsKey(userKey))

        _friends[userKey] = RootUser(rootUserManager.addFriend(userKey, userWrapper))
    }
}
