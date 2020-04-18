package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.loaders.EmissionChecker
import com.krystianwsul.common.firebase.ChangeType
import org.junit.Assert


@ExperimentalStdlibApi
fun EmissionChecker<ChangeType>.checkChangeType(changeType: ChangeType) {
    addHandler {
        Assert.assertTrue(
                "$name expected $changeType: actual: ${it}",
                it == changeType
        )
    }
}

@ExperimentalStdlibApi
fun EmissionChecker<ChangeType>.checkRemote() = checkChangeType(ChangeType.REMOTE)

@ExperimentalStdlibApi
fun EmissionChecker<ChangeType>.checkLocal() = checkChangeType(ChangeType.LOCAL)