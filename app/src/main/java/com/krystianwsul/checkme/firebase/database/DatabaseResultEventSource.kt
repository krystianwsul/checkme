package com.krystianwsul.checkme.firebase.database

import io.reactivex.rxjava3.core.Observable

interface DatabaseResultEventSource {

    val onDequeued: Observable<Unit>
}