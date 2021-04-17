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
import com.krystianwsul.common.firebase.json.UserJson
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.models.RootUser
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
) : JsonTime.UserCustomTimeProvider {

    private val rootUserManager = AndroidRootUserManager(initialFriendsEvent.userWrapperDatas, databaseWrapper)
    private val strangerProjectManager = StrangerProjectManager()

    val userMap = rootUserManager.records
            .mapValues { it.value.newValue(RootUser(it.value.value)) }
            .toMutableMap()

    fun hasUserKeys(userKeys: Set<UserKey>) = userKeys.all { it in userMap.keys }

    private fun getFriendMap() = userMap.filter { it.value.userLoadReason == UserLoadReason.FRIEND }

    fun getFriends() = getFriendMap().values.map { it.value }

    val changeTypes: Observable<ChangeType>

    init {
        val addChangeFriendChangeTypes = friendsLoader.addChangeFriendEvents
                .mapNotNull { rootUserManager.set(it.userWrapperData) }
                .map { (changeType, reasonWrapper) ->
                    userMap[reasonWrapper.value.userKey] = reasonWrapper.newValue(RootUser(reasonWrapper.value))

                    changeType
                }

        val removeFriendsChangeTypes = friendsLoader.removeFriendEvents.map {
            it.userKeys.forEach {
                rootUserManager.remove(it)
                userMap.remove(it)
            }

            it.userChangeType
        }

        changeTypes = listOf(addChangeFriendChangeTypes, removeFriendsChangeTypes).merge()
                .filter { it == ChangeType.REMOTE } // filtering out events for internal changes
                .publishImmediate(domainDisposable)
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
        val addedStrangers = addedUsers - addedFriends.map { it.value.userKey }

        val removedFriends = removedUsers.mapNotNull(userMap::get)
        val removedStrangers = removedUsers - removedFriends.map { it.value.userKey }

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

    override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
        return userMap.getValue(userCustomTimeKey.userKey)
                .value
                .getUserCustomTime(userCustomTimeKey)
    }
}
