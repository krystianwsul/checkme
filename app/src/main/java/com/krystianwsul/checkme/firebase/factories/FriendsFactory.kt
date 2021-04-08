package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.FriendsLoader
import com.krystianwsul.checkme.firebase.managers.AndroidRootUserManager
import com.krystianwsul.checkme.firebase.managers.StrangerProjectManager
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge

class FriendsFactory(
        private val friendsLoader: FriendsLoader,
        initialFriendsEvent: FriendsLoader.InitialFriendsEvent,
        domainDisposable: CompositeDisposable,
) {

    companion object {

        private fun Map<UserKey, RootUserRecord>.toRootUsers() = mapValues { RootUser(it.value) }.toMutableMap()
    }

    private val rootUserManager = AndroidRootUserManager(initialFriendsEvent.snapshots)
    private val strangerProjectManager = StrangerProjectManager()

    private var _friends = rootUserManager.records.toRootUsers()

    val isSaved get() = rootUserManager.isSaved

    val friends: Collection<RootUser> get() = _friends.values

    val changeTypes: Observable<ChangeType>

    val savedList get() = rootUserManager.savedList

    init {
        val addChangeFriendChangeTypes = friendsLoader.addChangeFriendEvents
                .mapNotNull { rootUserManager.set(it.snapshot) }
                .map { (changeType, rootUserRecord) ->
                    _friends[rootUserRecord.userKey] = RootUser(rootUserRecord)

                    changeType
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
        ).merge()
                .filter { it == ChangeType.REMOTE } // filtering out events for internal changes
                .publishImmediate(domainDisposable)
    }

    fun save(values: MutableMap<String, Any?>) {
        strangerProjectManager.save(values)
        rootUserManager.save(values)
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

    fun updateProjects(
            projectId: ProjectKey.Shared,
            addedUsers: Set<UserKey>,
            removedUsers: Set<UserKey>,
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

        val rootUserRecord = rootUserManager.addFriend(userKey, userWrapper)
        friendsLoader.addFriend(rootUserRecord)

        check(_friends.containsKey(rootUserRecord.userKey))
    }
}
