package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import com.krystianwsul.common.firebase.UserLoadReason
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.RootUserRecord
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

    private val addFriendEvents = PublishRelay.create<FriendEvent.AddFriend>()
    private val customTimeEvents = PublishRelay.create<CustomTimeEvent>()

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

        val customTimeOutputEvents =
                customTimeEvents.scan(CustomTimeAggregate()) { aggregate, customTimeEvent ->
                    when (customTimeEvent) {
                        is CustomTimeEvent.ProjectAdded -> {
                            val newProjectMap = aggregate.projectMap
                                    .toMutableMap()
                                    .also { it[customTimeEvent.projectKey] = customTimeEvent.userKeys }

                            CustomTimeAggregate(newProjectMap)
                        }
                        is CustomTimeEvent.ProjectsRemoved -> {
                            val newProjectMap = aggregate.projectMap
                                    .toMutableMap()
                                    .also { map ->
                                        customTimeEvent.projectKeys.forEach { map.remove(it) }
                                    }

                            CustomTimeAggregate(newProjectMap)
                        }
                    }
                }
                        .skip(1)
                        .map { FriendOrCustomTimeEvent.CustomTimes(it.output) }

        loadUserDataObservable = listOf(friendEvents, customTimeOutputEvents).merge()
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

        customTimeEvents.accept(CustomTimeEvent.ProjectAdded(projectKey, userKeys))
    }

    fun requestCustomTimeUsers(rootTaskKey: TaskKey.Root, userKeys: Set<UserKey>) {
        checkNotNull(loadUserDataObservable.tryGetCurrentValue())

// todo task fetch        customTimeEvents.accept(CustomTimeEvent.ProjectAdded(projectKey, userKeys))
    }

    fun onProjectsRemoved(projectKeys: Set<ProjectKey.Shared>) {
        checkNotNull(loadUserDataObservable.tryGetCurrentValue())

        customTimeEvents.accept(CustomTimeEvent.ProjectsRemoved(projectKeys))
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

    private sealed class CustomTimeEvent {

        data class ProjectAdded(val projectKey: ProjectKey.Shared, val userKeys: Set<UserKey>) : CustomTimeEvent()

        data class ProjectsRemoved(val projectKeys: Set<ProjectKey.Shared>) : CustomTimeEvent()
    }

    private data class CustomTimeAggregate(val projectMap: Map<ProjectKey.Shared, Set<UserKey>> = mapOf()) {

        val output = projectMap.values
                .flatten()
                .toSet()
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