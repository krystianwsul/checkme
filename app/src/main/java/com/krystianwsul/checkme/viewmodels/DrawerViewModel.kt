package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory

class DrawerViewModel : DomainViewModel<DrawerViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getDrawerData()
    }

    fun start() = internalStart()

    data class Data(val name: String, val email: String, val photoUrl: String?) : DomainData()
}