package com.krystianwsul.checkme.utils

import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.jakewharton.rxrelay3.Relay

class SingleParamSingleSource<PARAM, RESULT : Any>(private val cache: Boolean = false) {

    private val relayMap = mutableMapOf<PARAM, Relay<RESULT>>()

    private fun newRelay(): Relay<RESULT> = if (cache) BehaviorRelay.create() else PublishRelay.create()

    fun getSingle(param: PARAM) = relayMap.getOrPut(param) { newRelay() }.firstOrError()!!

    fun accept(param: PARAM, result: RESULT) {
        if (cache) check((relayMap[param] as? BehaviorRelay<*>)?.hasValue() != true)

        relayMap.getValue(param).accept(result)

        if (!cache) relayMap.remove(param)
    }
}