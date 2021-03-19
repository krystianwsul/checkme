package com.krystianwsul.checkme.viewmodels

abstract class DomainData {

    companion object {

        private var dataId = 1

        private val nextId get() = dataId++
    }

    val dataId = DataId(nextId)

    private var _immediate = false
    val immediate: Boolean
        get() {
            val ret = _immediate
            _immediate = true
            return ret
        }
}