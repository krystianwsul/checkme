package com.krystianwsul.common.firebase.managers

import com.krystianwsul.common.firebase.DatabaseWrapper
import com.krystianwsul.common.firebase.ReasonWrapper
import com.krystianwsul.common.firebase.UserLoadReason
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.records.RootUserRecord
import com.krystianwsul.common.utils.UserKey

class JsRootUserManager(
        databaseWrapper: DatabaseWrapper,
        userWrappers: Map<String, UserWrapper>,
) : RootUserManager<ReasonWrapper<RootUserRecord>>() {

    init {
        setInitialRecords(
                userWrappers.entries.associate {
                    val userKey = UserKey(it.key)

                    userKey to ReasonWrapper(
                            UserLoadReason.FRIEND, // todo customtime relevance
                            RootUserRecord(databaseWrapper, false, it.value, userKey),
                    )
                }
        )
    }

    override fun valueToRecord(value: ReasonWrapper<RootUserRecord>) = value.value
}