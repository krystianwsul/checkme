package com.krystianwsul.checkme.firebase.factories

import com.krystianwsul.checkme.firebase.loaders.FriendsLoader
import com.krystianwsul.checkme.firebase.managers.AndroidRootUserManager
import com.krystianwsul.checkme.firebase.managers.StrangerProjectManager
import com.krystianwsul.checkme.utils.mapNotNull
import com.krystianwsul.checkme.utils.publishImmediate
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.ReasonWrapper
import com.krystianwsul.common.firebase.UserLoadReason
import com.krystianwsul.common.firebase.json.users.UserJson
import com.krystianwsul.common.firebase.json.users.UserWrapper
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.users.RootUser
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge

class FriendsFactory(
    private val friendsLoader: FriendsLoader,
    initialFriendsEvent: FriendsLoader.InitialFriendsEvent,
    domainDisposable: CompositeDisposable,
    databaseWrapper: DatabaseWrapper,
    private val rootModelChangeManager: RootModelChangeManager,
) : JsonTime.UserCustomTimeProvider {

    private val rootUserManager = AndroidRootUserManager(initialFriendsEvent.userWrapperDatas, databaseWrapper)
    private val strangerProjectManager = StrangerProjectManager()

    val userMap = rootUserManager.records
        .mapValues { it.value.newValue(RootUser(it.value.value)) }
        .toMutableMap()

    private fun getFriendMap() = userMap.filter { it.value.userLoadReason == UserLoadReason.FRIEND }

    fun getFriends() = getFriendMap().values.map { it.value }

    val remoteChanges: Observable<Unit>

    init {
        val addChangeFriendRemoteChanges = friendsLoader.addChangeFriendEvents
            .mapNotNull { rootUserManager.set(it.userWrapperData) }
            .map { reasonWrapper ->
                val userKey = reasonWrapper.value.userKey

                userMap[userKey]?.value
                    ?.clearableInvalidatableManager
                    ?.clear()

                rootModelChangeManager.invalidateUsers()

                userMap[userKey] = reasonWrapper.newValue(RootUser(reasonWrapper.value))
            }

        val removeFriendsRemoteChanges = friendsLoader.removeFriendEvents
            .doOnNext {
                it.userKeys.forEach {
                    rootUserManager.remove(it)

                    userMap[it]?.value
                        ?.clearableInvalidatableManager
                        ?.clear()

                    rootModelChangeManager.invalidateUsers()

                    userMap.remove(it)
                }
            }
            .filter { it.userChangeType == ChangeType.REMOTE } // filtering out events for internal changes
            .map { }

        remoteChanges = listOf(
            addChangeFriendRemoteChanges,
            removeFriendsRemoteChanges,
        ).merge().publishImmediate(domainDisposable)
    }

    fun save(values: MutableMap<String, Any?>) {
        strangerProjectManager.save(values)
        rootUserManager.save(values)
    }

    fun getUserJsons(friendIds: Set<UserKey>): Map<UserKey, UserJson> {
        val friendMap = getFriendMap()

        check(friendIds.all { friendMap.containsKey(it) })

        return friendMap.entries
            .filter { friendIds.contains(it.key) }
            .associateBy({ it.key }, { it.value.value.userJson })
    }

    fun getFriend(friendId: UserKey): RootUser {
        val friendMap = getFriendMap()
        check(friendMap.containsKey(friendId))

        return friendMap.getValue(friendId).value
    }

    fun updateProjects(
        projectId: ProjectKey.Shared,
        addedUsers: Set<UserKey>,
        removedUsers: Set<UserKey>,
    ) {
        val addedFriends = addedUsers.mapNotNull(userMap::get)
        val addedStrangers = addedUsers - addedFriends.map { it.value.userKey }.toSet()

        val removedFriends = removedUsers.mapNotNull(userMap::get)
        val removedStrangers = removedUsers - removedFriends.map { it.value.userKey }.toSet()

        addedFriends.forEach { it.value.addProject(projectId) }
        removedFriends.forEach { it.value.removeProject(projectId) }

        strangerProjectManager.updateStrangerProjects(projectId, addedStrangers, removedStrangers)
    }

    fun addFriend(userKey: UserKey, userWrapper: UserWrapper) {
        check(!userMap.containsKey(userKey))

        val rootUserRecord = rootUserManager.addFriend(userKey, userWrapper)
        friendsLoader.userKeyStore.addFriend(rootUserRecord)

        val existingReasonWrapper = userMap[userKey]
        if (existingReasonWrapper != null) {
            check(existingReasonWrapper.userLoadReason == UserLoadReason.CUSTOM_TIMES)

            userMap[userKey] = existingReasonWrapper.copy(userLoadReason = UserLoadReason.FRIEND)
        } else {
            userMap[userKey] = ReasonWrapper(UserLoadReason.FRIEND, RootUser(rootUserRecord))
        }

        check(userMap.containsKey(rootUserRecord.userKey))
    }

    override fun tryGetUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User? {
        return userMap[userCustomTimeKey.userKey]
            ?.value
            ?.tryGetUserCustomTime(userCustomTimeKey)
    }
}
