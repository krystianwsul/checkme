package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Observables

interface UserCustomTimeProviderSource {

    // emit only remote changes
    fun observeUserCustomTimeProvider(projectRecord: ProjectRecord<*>): Observable<JsonTime.UserCustomTimeProvider>

    class Impl(
            private val myUserKey: UserKey,
            private val myUserFactorySingle: Single<MyUserFactory>,
            private val customTimeCoordinator: CustomTimeCoordinator,
    ) : UserCustomTimeProviderSource {

        override fun observeUserCustomTimeProvider(
                projectRecord: ProjectRecord<*>,
        ): Observable<JsonTime.UserCustomTimeProvider> {
            val customTimeKeys = getUserCustomTimeKeys(projectRecord)
            val userKeys = customTimeKeys.map { it.userKey }.toSet()
            val foreignUserKeys = userKeys - myUserKey

            return when (projectRecord) {
                is PrivateProjectRecord -> {
                    check(foreignUserKeys.isEmpty())

                    myUserFactorySingle.toObservable().map { myUserFactory ->
                        object : JsonTime.UserCustomTimeProvider {

                            override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
                                check(userCustomTimeKey.userKey == myUserKey)

                                return myUserFactory.user.getUserCustomTime(userCustomTimeKey)
                            }
                        }
                    }
                }
                is SharedProjectRecord -> {
                    Observables.combineLatest(
                            myUserFactorySingle.toObservable(),
                            customTimeCoordinator.observeCustomTimes(projectRecord.projectKey, foreignUserKeys),
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
                else -> throw IllegalArgumentException()
            }
        }

        private fun getUserCustomTimeKeys(projectRecord: ProjectRecord<*>): Set<CustomTimeKey.User> {
            return projectRecord.taskRecords
                    .values
                    .flatMap {
                        val scheduleCustomTimeKeys = listOf(
                                it.singleScheduleRecords,
                                it.weeklyScheduleRecords,
                                it.monthlyDayScheduleRecords,
                                it.monthlyWeekScheduleRecords,
                                it.yearlyScheduleRecords,
                        ).flatMap { it.values }.map { it.customTimeKey }

                        val instanceCustomTimeKeys: List<CustomTimeKey?> = it.instanceRecords
                                .values
                                .flatMap {
                                    listOf(
                                            it.scheduleKey.scheduleTimePair.customTimeKey,
                                            it.instanceJsonTime?.getCustomTimeKey(projectRecord)
                                    )
                                }

                        listOf(
                                scheduleCustomTimeKeys,
                                instanceCustomTimeKeys,
                        ).flatten().filterIsInstance<CustomTimeKey.User>()
                    }
                    .toSet()
        }
    }
}