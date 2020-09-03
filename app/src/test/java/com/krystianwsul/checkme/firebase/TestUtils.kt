package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.loaders.EmissionChecker
import com.krystianwsul.common.firebase.ChangeType
import org.junit.Assert


@ExperimentalStdlibApi
fun EmissionChecker<ChangeType>.checkChangeType(changeType: ChangeType, action: () -> Unit) {
    addHandler {
        Assert.assertTrue(
                "$name expected $changeType: actual: $it",
                it == changeType
        )
    }
    action()
    checkEmpty()
}

@ExperimentalStdlibApi
fun EmissionChecker<ChangeType>.checkRemote(action: () -> Unit) = checkChangeType(ChangeType.REMOTE, action)

@ExperimentalStdlibApi
fun EmissionChecker<ChangeType>.checkLocal(action: () -> Unit) = checkChangeType(ChangeType.LOCAL, action)