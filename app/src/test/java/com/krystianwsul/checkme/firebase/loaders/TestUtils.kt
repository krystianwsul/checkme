package com.krystianwsul.checkme.firebase.loaders

import com.krystianwsul.checkme.firebase.managers.ChangeWrapper
import com.krystianwsul.common.firebase.ChangeType
import org.junit.Assert

@ExperimentalStdlibApi
fun <T : Any> EmissionChecker<ChangeWrapper<T>>.checkChangeType(changeType: ChangeType) {
    addHandler {
        Assert.assertTrue(it.changeType == changeType)
    }
}

@ExperimentalStdlibApi
fun <T : Any> EmissionChecker<ChangeWrapper<T>>.checkRemote() = checkChangeType(ChangeType.REMOTE)

@ExperimentalStdlibApi
fun <T : Any> EmissionChecker<ChangeWrapper<T>>.checkLocal() = checkChangeType(ChangeType.LOCAL)