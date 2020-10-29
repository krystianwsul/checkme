package com.krystianwsul.checkme.domainmodel

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.krystianwsul.checkme.viewmodels.DomainData
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.interrupt.DomainInterruptedException

fun FirebaseUser.toUserInfo() = UserInfo(email!!, displayName!!, uid)

fun ImageState.toImageLoader() = ImageLoader(this)

fun <T : DomainData> getDomainResultInterrupting(action: () -> T): DomainResult<T> {
    return try {
        DomainResult.Completed(action())
    } catch (domainInterruptedException: DomainInterruptedException) {
        Log.e("asdf", "domain interrupted", domainInterruptedException)

        DomainResult.Interrupted()
    }
}

fun <T> Sequence<T>.takeAndHasMore(n: Int): Pair<List<T>, Boolean> {
    val elementsPlusOne = take(n + 1).toList()

    val elements = elementsPlusOne.take(n)

    val hasMore = elements.size < elementsPlusOne.size

    return Pair(elements, hasMore)
}