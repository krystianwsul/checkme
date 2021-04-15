package com.krystianwsul.checkme.firebase.loaders

import android.util.Base64
import com.krystianwsul.common.firebase.ChangeType
import com.krystianwsul.common.firebase.ChangeWrapper
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertTrue

@ExperimentalStdlibApi
fun <T : Any> EmissionChecker<ChangeWrapper<T>>.checkChangeType(changeType: ChangeType, action: () -> Unit) {
    addHandler {
        assertTrue(
                "$name expected $changeType: actual: ${it.changeType}",
                it.changeType == changeType
        )
    }
    action()
    checkEmpty()
}

fun <T : Any> EmissionChecker<T>.checkOne(action: () -> Unit) {
    addHandler { }
    action()
    checkEmpty()
}

@ExperimentalStdlibApi
fun <T : Any> EmissionChecker<ChangeWrapper<T>>.checkRemote(action: () -> Unit) = checkChangeType(ChangeType.REMOTE, action)

@ExperimentalStdlibApi
fun <T : Any> EmissionChecker<ChangeWrapper<T>>.checkLocal(action: () -> Unit) = checkChangeType(ChangeType.LOCAL, action)

fun mockBase64() {
    mockkStatic(Base64::class)
    every { Base64.encodeToString(any(), any()) } returns "key"
}