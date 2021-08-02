package com.krystianwsul.checkme.firebase

import androidx.annotation.VisibleForTesting
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.loaders.FriendsLoader
import com.krystianwsul.checkme.utils.getCurrentValue
import com.krystianwsul.checkme.utils.tryGetCurrentValue
import com.krystianwsul.common.firebase.records.project.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface UserCustomTimeProviderSource {

    fun getUserCustomTimeProvider(projectRecord: ProjectRecord<*>): JsonTime.UserCustomTimeProvider
    fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord): JsonTime.UserCustomTimeProvider

    fun hasCustomTimes(rootTaskRecord: RootTaskRecord): Boolean // todo dependencies final cleanup

    fun getTimeChangeObservable(): Observable<Unit> // todo dependencies final cleanup

    class Impl(
        private val myUserKey: UserKey,
        private val myUserFactorySingle: Single<MyUserFactory>,
        private val friendsLoader: FriendsLoader,
        private val friendsFactorySingle: Single<FriendsFactory>,
    ) : UserCustomTimeProviderSource {

        private val userCustomTimeProvider = UserCustomTimeProvider(myUserFactorySingle, friendsFactorySingle)

        private fun getForeignUserKeys(customTimeKeys: Set<CustomTimeKey.User>): Set<UserKey> {
            val userKeys = customTimeKeys.map { it.userKey }.toSet()
            return userKeys - myUserKey
        }

        override fun getUserCustomTimeProvider(
            projectRecord: ProjectRecord<*>,
        ): JsonTime.UserCustomTimeProvider {
            val customTimeKeys = getUserCustomTimeKeys(projectRecord)
            val foreignUserKeys = getForeignUserKeys(customTimeKeys)

            return when (projectRecord) {
                is PrivateProjectRecord -> {
                    check(foreignUserKeys.isEmpty())

                    userCustomTimeProvider
                }
                is SharedProjectRecord -> getUserCustomTimeProvider(foreignUserKeys) {
                    friendsLoader.userKeyStore.requestCustomTimeUsers(projectRecord.projectKey, foreignUserKeys)
                }
                else -> throw IllegalArgumentException()
            }
        }

        private fun getUserCustomTimeKeys(projectRecord: ProjectRecord<*>): Set<CustomTimeKey.User> {
            return projectRecord.taskRecords
                .values
                .flatMap { it.getUserCustomTimeKeys() }
                .toSet()
        }

        private fun getForeignUserKeysFromRecord(rootTaskRecord: RootTaskRecord) =
            getForeignUserKeys(rootTaskRecord.getUserCustomTimeKeys())

        override fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord): JsonTime.UserCustomTimeProvider {
            val foreignUserKeys = getForeignUserKeysFromRecord(rootTaskRecord)

            return getUserCustomTimeProvider(foreignUserKeys) {
                friendsLoader.userKeyStore.requestCustomTimeUsers(rootTaskRecord.taskKey, foreignUserKeys)
            }
        }

        private fun getUserCustomTimeProvider(
            foreignUserKeys: Set<UserKey>,
            notEmptyCallback: () -> Unit,
        ): JsonTime.UserCustomTimeProvider {
            if (foreignUserKeys.isNotEmpty()) notEmptyCallback()

            return UserCustomTimeProvider(myUserFactorySingle, friendsFactorySingle)
        }

        private class UserCustomTimeProvider(
            private val myUserFactorySingle: Single<MyUserFactory>,
            private val friendsFactorySingle: Single<FriendsFactory>,
        ) : JsonTime.UserCustomTimeProvider {

            private val myUserFactory by lazy { myUserFactorySingle.getCurrentValue() }
            private val friendsFactory by lazy { friendsFactorySingle.getCurrentValue() }

            override fun tryGetUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User? {
                val provider = if (userCustomTimeKey.userKey == myUserFactory.user.userKey)
                    myUserFactory.user
                else
                    friendsFactory

                return provider.tryGetUserCustomTime(userCustomTimeKey)
            }
        }


        @VisibleForTesting
        fun getCustomTimes(foreignUserKeys: Set<UserKey>): Single<FriendsFactory> {
            check(myUserKey !in foreignUserKeys)

            return friendsFactorySingle.flatMap { friendsFactory ->
                Observable.just(Unit)
                    .concatWith(friendsFactory.changeTypes.map { })
                    .filter { friendsFactory.hasUserKeys(foreignUserKeys) }
                    .firstOrError()
                    .map { friendsFactory }
            }
        }

        override fun hasCustomTimes(rootTaskRecord: RootTaskRecord): Boolean {
            val foreignUserKeys = getForeignUserKeysFromRecord(rootTaskRecord)
            if (foreignUserKeys.isEmpty()) return true

            val friendsFactory = friendsFactorySingle.tryGetCurrentValue() ?: return false
            return friendsFactory.hasUserKeys(foreignUserKeys)
        }

        override fun getTimeChangeObservable(): Observable<Unit> {
            return friendsFactorySingle.flatMapObservable { it.changeTypes }.map { }
        }
    }
}