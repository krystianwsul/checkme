package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.common.firebase.records.PrivateProjectRecord
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.firebase.records.SharedProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import io.reactivex.rxjava3.core.Observable

interface UserCustomTimeProviderSource {

    // emit only remote changes
    fun observeUserCustomTimeProvider(projectRecord: ProjectRecord<*>): Observable<JsonTime.UserCustomTimeProvider>

    class Impl(private val myUserFactory: MyUserFactory) : UserCustomTimeProviderSource {

        override fun observeUserCustomTimeProvider(
                projectRecord: ProjectRecord<*>,
        ): Observable<JsonTime.UserCustomTimeProvider> {
            val customTimeKeys = getUserCustomTimeKeys(projectRecord)

            when (projectRecord) {
                is PrivateProjectRecord -> {
                    check(customTimeKeys.all { it.userKey == myUserFactory.userKey })

                    return Observable.just(
                            object : JsonTime.UserCustomTimeProvider {

                                override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
                                    check(userCustomTimeKey.userKey == myUserFactory.userKey)

                                    return myUserFactory.user
                                            .customTimes
                                            .getValue(userCustomTimeKey.customTimeId)
                                }
                            }
                    )
                }
                is SharedProjectRecord -> {
                }
            }

            // todo source consider way to notify that a project is removed

            return Observable.just(
                    object : JsonTime.UserCustomTimeProvider {

                        override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
                            TODO("todo source")
                        }
                    }
            )
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