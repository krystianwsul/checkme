package com.krystianwsul.checkme.utils

import com.jakewharton.rxrelay3.PublishRelay
import com.jakewharton.rxrelay3.Relay

class SingleParamSingleSource<PARAM, RESULT : Any> {

    private val relayMap = mutableMapOf<PARAM, Relay<RESULT>>()

    fun getSingle(param: PARAM) = relayMap.getOrPut(param) { PublishRelay.create() }.firstOrError()!!

    fun accept(param: PARAM, result: RESULT) {
        relayMap.getValue(param).accept(result)
        relayMap.remove(param)
    }
}