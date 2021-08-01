package com.krystianwsul.checkme.firebase

import com.jakewharton.rxrelay3.BehaviorRelay

object LoadStatus { // todo dependencies

    private val counterRelay = BehaviorRelay.createDefault(0)
    val isLoadingObservable get() = counterRelay.map { it > 0 }!!

    fun incrementCounter() {
        check(counterRelay.value!! >= 0)

        counterRelay.accept(counterRelay.value!! + 1)
    }

    fun decrementCounter() {
        check(counterRelay.value!! > 0)

        counterRelay.accept(counterRelay.value!! - 1)
    }
}