package com.krystianwsul.checkme.firebase

import androidx.annotation.VisibleForTesting
import com.krystianwsul.checkme.firebase.factories.FriendsFactory
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.checkme.firebase.loaders.FriendsLoader
import com.krystianwsul.common.firebase.records.project.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.project.SharedProjectRecord
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.firebase.records.task.TaskRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles

interface UserCustomTimeProviderSource {

    companion object {

        fun getUserCustomTimeKeys(taskRecord: TaskRecord, expectProjectKeys: Boolean): List<CustomTimeKey.User> {
            val scheduleCustomTimeKeys = listOf(
                    taskRecord.singleScheduleRecords,
                    taskRecord.weeklyScheduleRecords,
                    taskRecord.monthlyDayScheduleRecords,
                    taskRecord.monthlyWeekScheduleRecords,
                    taskRecord.yearlyScheduleRecords,
            ).flatMap { it.values }.map { it.customTimeKey }

            val instanceCustomTimeKeys: List<CustomTimeKey?> = taskRecord.instanceRecords
                    .values
                    .flatMap {
                        listOf(
                                it.scheduleKey.scheduleTimePair.customTimeKey,
                                it.instanceJsonTime?.getCustomTimeKey(taskRecord.projectCustomTimeIdAndKeyProvider)
                        )
                    }

            val customTimeKeys = listOf(scheduleCustomTimeKeys, instanceCustomTimeKeys).flatten()

            return if (expectProjectKeys)
                customTimeKeys.filterIsInstance<CustomTimeKey.User>()
            else
                customTimeKeys.map { it as CustomTimeKey.User }
        }
    }

    // emit only remote changes
    fun getUserCustomTimeProvider(projectRecord: ProjectRecord<*>): Single<JsonTime.UserCustomTimeProvider>

    // emit only remote changes
    fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord): Single<JsonTime.UserCustomTimeProvider>

    class Impl(
            private val myUserKey: UserKey,
            private val myUserFactorySingle: Single<MyUserFactory>,
            private val friendsLoader: FriendsLoader,
            private val friendsFactorySingle: Single<FriendsFactory>,
    ) : UserCustomTimeProviderSource {

        private fun getForeignUserKeys(customTimeKeys: Set<CustomTimeKey.User>): Set<UserKey> {
            val userKeys = customTimeKeys.map { it.userKey }.toSet()
            return userKeys - myUserKey
        }

        override fun getUserCustomTimeProvider(
                projectRecord: ProjectRecord<*>,
        ): Single<JsonTime.UserCustomTimeProvider> {
            val customTimeKeys = getUserCustomTimeKeys(projectRecord)
            val foreignUserKeys = getForeignUserKeys(customTimeKeys)

            return when (projectRecord) {
                is PrivateProjectRecord -> {
                    check(foreignUserKeys.isEmpty())

                    myUserFactorySingle.map { myUserFactory ->
                        object : JsonTime.UserCustomTimeProvider {

                            override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
                                check(userCustomTimeKey.userKey == myUserKey)

                                return myUserFactory.user.getUserCustomTime(userCustomTimeKey)
                            }
                        }
                    }
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
                    .flatMap { getUserCustomTimeKeys(it, true) }
                    .toSet()
        }

        override fun getUserCustomTimeProvider(
                rootTaskRecord: RootTaskRecord,
        ): Single<JsonTime.UserCustomTimeProvider> {
            val customTimeKeys = getUserCustomTimeKeys(rootTaskRecord)
            val foreignUserKeys = getForeignUserKeys(customTimeKeys)

            return getUserCustomTimeProvider(foreignUserKeys) {
                friendsLoader.userKeyStore.requestCustomTimeUsers(rootTaskRecord.taskKey, foreignUserKeys)
            }
        }

        private fun getUserCustomTimeKeys(taskRecord: RootTaskRecord) =
                getUserCustomTimeKeys(taskRecord, false).toSet()

        private fun getUserCustomTimeProvider(
                foreignUserKeys: Set<UserKey>,
                notEmptyCallback: () -> Unit,
        ): Single<JsonTime.UserCustomTimeProvider> {
            if (foreignUserKeys.isNotEmpty()) notEmptyCallback()

            return Singles.zip(
                    myUserFactorySingle,
                    getCustomTimes(foreignUserKeys),
            ).map { (myUserFactory, friendsFactory) ->
                object : JsonTime.UserCustomTimeProvider {

                    override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
                        val provider = if (userCustomTimeKey.userKey == myUserFactory.user.userKey)
                            myUserFactory.user
                        else
                            friendsFactory

                        return provider.getUserCustomTime(userCustomTimeKey)
                    }
                }
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
    }
}