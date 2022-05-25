package com.krystianwsul.checkme.firebase

import com.krystianwsul.common.firebase.records.project.OwnedProjectRecord
import com.krystianwsul.common.firebase.records.task.RootTaskRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey

class TestUserCustomTimeProviderSource : UserCustomTimeProviderSource {

    override fun getUserCustomTimeProvider(projectRecord: OwnedProjectRecord<*>): JsonTime.UserCustomTimeProvider {
        return object : JsonTime.UserCustomTimeProvider {

            override fun tryGetUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User? {
                TODO("Not yet implemented")
            }
        }
    }

    override fun getUserCustomTimeProvider(rootTaskRecord: RootTaskRecord): JsonTime.UserCustomTimeProvider {
        TODO("Not yet implemented")
    }
}