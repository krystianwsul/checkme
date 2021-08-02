package com.krystianwsul.checkme.firebase

import com.krystianwsul.common.firebase.records.project.ProjectRecord
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import io.reactivex.rxjava3.core.Observable

class TestUserCustomTimeProviderSource : UserCustomTimeProviderSource {

    override fun getUserCustomTimeProvider(projectRecord: ProjectRecord<*>): JsonTime.UserCustomTimeProvider {
        return object : JsonTime.UserCustomTimeProvider {

            override fun tryGetUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User? {
                TODO("Not yet implemented")
            }
        }
    }

    override fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord): JsonTime.UserCustomTimeProvider {
        TODO("Not yet implemented")
    }

    override fun hasCustomTimes(rootTaskRecord: RootTaskRecord): Boolean {
        TODO("Not yet implemented")
    }

    override fun getTimeChangeObservable(): Observable<Unit> {
        TODO("Not yet implemented")
    }
}