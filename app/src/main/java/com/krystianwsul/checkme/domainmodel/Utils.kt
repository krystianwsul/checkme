package com.krystianwsul.checkme.domainmodel

import com.google.firebase.auth.FirebaseUser
import com.krystianwsul.checkme.viewmodels.DomainData
import com.krystianwsul.checkme.viewmodels.DomainResult
import com.krystianwsul.common.domain.UserInfo
import com.krystianwsul.common.firebase.models.ImageState

fun FirebaseUser.toUserInfo() = UserInfo(email!!, displayName!!, uid)

fun ImageState.toImageLoader() = ImageLoader(this)

fun <T : DomainData> getDomainResultInterrupting(action: () -> T): DomainResult<T> {
    return try {
        DomainResult.Completed(action())
    } catch (domainInterruptedException: DomainInterruptedException) {
        DomainResult.Interrupted()
    }
}