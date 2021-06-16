package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.extensions.getDrawerData

class DrawerViewModel : DomainViewModel<DrawerViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override val domainResultFetcher = DomainResultFetcher.DomainFactoryData { it.getDrawerData() }
    }

    fun start() = internalStart()

    data class Data(val name: String, val email: String, val photoUrl: String?) : DomainData()
}