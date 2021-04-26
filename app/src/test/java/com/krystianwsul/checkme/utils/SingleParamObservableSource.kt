package com.krystianwsul.checkme.utils

import com.jakewharton.rxrelay3.PublishRelay

class SingleParamObservableSource<PARAM, RESULT : Any> {

    private val relayMap = mutableMapOf<PARAM, PublishRelay<RESULT>>()

    fun getObservable(param: PARAM) = relayMap.getOrPut(param) { PublishRelay.create() }

    fun accept(param: PARAM, result: RESULT) = relayMap.getValue(param).accept(result)
}