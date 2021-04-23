package com.krystianwsul.checkme.firebase.roottask

import com.krystianwsul.checkme.firebase.CustomTimeCoordinator
import com.krystianwsul.checkme.firebase.ProjectUserCustomTimeProviderSource
import com.krystianwsul.checkme.firebase.factories.MyUserFactory
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.UserKey
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles

interface RootTaskUserCustomTimeProviderSource {

    // emit only remote changes
    fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord): Single<JsonTime.UserCustomTimeProvider>

    class Impl(
            private val myUserKey: UserKey,
            private val myUserFactorySingle: Single<MyUserFactory>,
            private val customTimeCoordinator: CustomTimeCoordinator,
    ) : RootTaskUserCustomTimeProviderSource {

        override fun getUserCustomTimeProvider(
                rootTaskRecord: RootTaskRecord,
        ): Single<JsonTime.UserCustomTimeProvider> {
            val customTimeKeys = getUserCustomTimeKeys(rootTaskRecord)
            val userKeys = customTimeKeys.map { it.userKey }.toSet()
            val foreignUserKeys = userKeys - myUserKey

            return Singles.zip(
                    myUserFactorySingle,
                    customTimeCoordinator.getCustomTimes(rootTaskRecord.taskKey, foreignUserKeys),
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

        private fun getUserCustomTimeKeys(taskRecord: RootTaskRecord) =
                ProjectUserCustomTimeProviderSource.getUserCustomTimeKeys(taskRecord, false).toSet()
    }
}