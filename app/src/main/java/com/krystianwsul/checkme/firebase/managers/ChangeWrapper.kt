package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.common.firebase.ChangeType

data class ChangeWrapper<T : Any>(
        val changeType: ChangeType,
        val data: T
)