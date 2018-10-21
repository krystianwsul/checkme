package com.krystianwsul.checkme.viewmodels

interface DomainObserver {

    fun onDomainChanged(dataIds: List<Int>)
}