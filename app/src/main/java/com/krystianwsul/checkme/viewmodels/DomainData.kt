package com.krystianwsul.checkme.viewmodels

abstract class DomainData {

    private var _immediate = false
    val immediate: Boolean
        get() {
            val ret = _immediate
            _immediate = true
            return ret
        }
}