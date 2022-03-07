package com.krystianwsul.checkme.firebase.database

import io.reactivex.rxjava3.core.Observable

interface DatabaseResultEventSource { // todo queue remove

    val onDequeued: Observable<Unit>
}