package com.krystianwsul.checkme.firebase.managers

import com.krystianwsul.checkme.firebase.loaders.ChangeType

class ChangeWrapper<T : Any>(
        val changeType: ChangeType,
        val data: T
)