package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.UserLoadReason
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.users.RootUserRecord
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import com.krystianwsul.treeadapter.tryGetCurrentValue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign

class UserKeyStore(
        friendKeysObservable: Observable<ChangeWrapper<Set<UserKey>>>,
        domainDisposable: CompositeDisposable,
) {

    private val projectRequestKeyStore = RequestKeyStore<ProjectKey.Shared, UserKey>()
    private val rootTaskRequestKeyStore = RequestKeyStore<TaskKey.Root, UserKey>()

    private val addFriendEvents = PublishRelay.create<FriendEvent.AddFriend>()

    val loadUserDataObservable: Observable<ChangeWrapper<Map<UserKey, LoadUserData>>>

    init {
        val friendKeysChangeEvents = friendKeysObservable.map { FriendEvent.FriendKeysChange(it) }

        val friendEvents = listOf(friendKeysChangeEvents, addFriendEvents).merge()
                .scan(
                        ChangeWrapper<Map<UserKey, LoadUserData>>(ChangeType.LOCAL, mapOf()) // this will be ignored by skip
                ) { oldChangeWrapper, friendEvent ->
                    when (friendEvent) {
                        is FriendEvent.FriendKeysChange -> { // overwrite
                            val changeWrapper = friendEvent.changeWrapper

                            changeWrapper.newData(
                                    changeWrapper.data.associateWith { LoadUserData.Friend(null) }
                            )
                        }
                        is FriendEvent.AddFriend -> { // add to map
                            val newMap = oldChangeWrapper.data.toMutableMap()
                            newMap[friendEvent.rootUserRecord.userKey] = LoadUserData.Friend(AddFriendData(
                                    friendEvent.rootUserRecord.key,
                                    friendEvent.rootUserRecord.userWrapper
                            ))

                            ChangeWrapper(ChangeType.LOCAL, newMap)
                        }
                    }
                }
                .skip(1)
                .map { FriendOrCustomTimeEvent.Friend(it) }

        val mergedRequests = RequestKeyStore.merge(projectRequestKeyStore, rootTaskRequestKeyStore)
                .skip(1)
                .map { FriendOrCustomTimeEvent.CustomTimes(it) }

        loadUserDataObservable = listOf(friendEvents, mergedRequests).merge()
                .scan(OutputAggregate()) { aggregate, friendOrCustomTimeEvent ->
                    // when summing maps, add friends to customTimes, since the former takes priority

                    when (friendOrCustomTimeEvent) {
                        is FriendOrCustomTimeEvent.Friend -> {
                            val output = aggregate.customTimesToMap() + friendOrCustomTimeEvent.changeWrapper.data

                            aggregate.copy(
                                    friendMap = friendOrCustomTimeEvent.changeWrapper.data,
                                    output = ChangeWrapper(friendOrCustomTimeEvent.changeWrapper.changeType, output),
                            )
                        }
                        is FriendOrCustomTimeEvent.CustomTimes -> {
                            val output = OutputAggregate.customTimesToMap(friendOrCustomTimeEvent.userKeys) +
                                    aggregate.friendMap

                            aggregate.copy(
                                    customTimes = friendOrCustomTimeEvent.userKeys,
                                    output = ChangeWrapper(ChangeType.REMOTE, output),
                            )
                        }
                    }
                }
                .skip(1)
                .map { it.output }
                .distinctUntilChanged()
                .replay()
                .apply { domainDisposable += connect() }
    }

    fun addFriend(rootUserRecord: RootUserRecord) {
        checkNotNull(loadUserDataObservable.tryGetCurrentValue())

        addFriendEvents.accept(FriendEvent.AddFriend(rootUserRecord))
    }

    fun requestCustomTimeUsers(projectKey: ProjectKey.Shared, userKeys: Set<UserKey>) {
        checkNotNull(loadUserDataObservable.tryGetCurrentValue())

        projectRequestKeyStore.addRequest(projectKey, userKeys)
    }

    fun onProjectsRemoved(projectKeys: Set<ProjectKey.Shared>) {
        checkNotNull(loadUserDataObservable.tryGetCurrentValue())

        projectRequestKeyStore.onRequestsRemoved(projectKeys)
    }

    fun requestCustomTimeUsers(rootTaskKey: TaskKey.Root, userKeys: Set<UserKey>) {
        checkNotNull(loadUserDataObservable.tryGetCurrentValue())

        rootTaskRequestKeyStore.addRequest(rootTaskKey, userKeys)
    }

    fun onTasksRemoved(rootTaskKeys: Set<TaskKey.Root>) {
        checkNotNull(loadUserDataObservable.tryGetCurrentValue())

        rootTaskRequestKeyStore.onRequestsRemoved(rootTaskKeys)
    }

    sealed class LoadUserData {

        abstract val userLoadReason: UserLoadReason
        abstract val addFriendData: AddFriendData?

        data class Friend(override val addFriendData: AddFriendData?) : LoadUserData() {

            override val userLoadReason = UserLoadReason.FRIEND
        }

        object CustomTimes : LoadUserData() {

            override val userLoadReason = UserLoadReason.CUSTOM_TIMES

            override val addFriendData: AddFriendData? = null
        }
    }

    data class AddFriendData(val key: String, val userWrapper: UserWrapper)

    private sealed class FriendEvent {

        data class FriendKeysChange(val changeWrapper: ChangeWrapper<Set<UserKey>>) : FriendEvent()

        data class AddFriend(val rootUserRecord: RootUserRecord) : FriendEvent()
    }

    private sealed class FriendOrCustomTimeEvent {

        data class Friend(val changeWrapper: ChangeWrapper<Map<UserKey, LoadUserData>>) : FriendOrCustomTimeEvent()

        data class CustomTimes(val userKeys: Set<UserKey>) : FriendOrCustomTimeEvent()
    }

    private data class OutputAggregate(
            val friendMap: Map<UserKey, LoadUserData> = mapOf(),
            val customTimes: Set<UserKey> = setOf(),
            val output: ChangeWrapper<Map<UserKey, LoadUserData>> = ChangeWrapper(ChangeType.LOCAL, mapOf()),
    ) {

        companion object {

            fun customTimesToMap(customTimes: Set<UserKey>) = customTimes.associateWith { LoadUserData.CustomTimes }
        }

        fun customTimesToMap() = customTimesToMap(customTimes)
    }
}