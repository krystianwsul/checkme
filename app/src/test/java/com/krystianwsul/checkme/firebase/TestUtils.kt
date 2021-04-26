package com.krystianwsul.checkme.firebase

import com.krystianwsul.checkme.firebase.loaders.EmissionChecker
import com.krystianwsul.common.firebase.ChangeType
import org.junit.Assert


fun <T : Any?> EmissionChecker<ChangeType>.checkChangeType(changeType: ChangeType, action: () -> T): T {
    addHandler {
        Assert.assertTrue(
                "$name expected $changeType: actual: $it",
                it == changeType
        )
    }
    val ret = action()
    checkEmpty()
    return ret
}

fun <T : Any?> EmissionChecker<ChangeType>.checkRemote(action: () -> T) = checkChangeType(ChangeType.REMOTE, action)

@ExperimentalStdlibApi
fun <T : Any?> EmissionChecker<ChangeType>.checkLocal(action: () -> T) = checkChangeType(ChangeType.LOCAL, action)