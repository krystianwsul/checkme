package com.krystianwsul.checkme.utils

import hu.akarnokd.rxjava3.bridge.RxJavaBridge
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

fun <T : Any> Observable<T>.toV3() = RxJavaBridge.toV3Observable(this)!!
fun <T : Any> Single<T>.toV3() = RxJavaBridge.toV3Single(this)!!
fun Completable.toV3() = RxJavaBridge.toV3Completable(this)!!