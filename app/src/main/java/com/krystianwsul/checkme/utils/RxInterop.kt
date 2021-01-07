package com.krystianwsul.checkme.utils

import hu.akarnokd.rxjava3.bridge.RxJavaBridge
import io.reactivex.Observable

fun <T : Any> Observable<T>.toV3() = RxJavaBridge.toV3Observable(this)!!