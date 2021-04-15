package com.krystianwsul.checkme.firebase

import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import io.reactivex.rxjava3.core.Observable

class TestUserCustomTimeProviderSource : UserCustomTimeProviderSource {

    override fun observeUserCustomTimeProvider(projectRecord: ProjectRecord<*>): Observable<JsonTime.UserCustomTimeProvider> {
        return Observable.just(
                object : JsonTime.UserCustomTimeProvider {

                    override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
                        TODO("Not yet implemented")
                    }
                }
        )
    }
}