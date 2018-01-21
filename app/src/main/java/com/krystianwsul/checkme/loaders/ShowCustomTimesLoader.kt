package com.krystianwsul.checkme.loaders

import android.content.Context

import com.krystianwsul.checkme.domainmodel.DomainFactory

import junit.framework.Assert

class ShowCustomTimesLoader(context: Context) : DomainLoader<ShowCustomTimesLoader.Data>(context, DomainLoader.FirebaseLevel.NOTHING) {

    override fun getName() = "ShowCustomTimesLoader"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.showCustomTimesData

    data class Data(val entries: List<CustomTimeData>) : DomainLoader.Data()

    data class CustomTimeData(val id: Int, val name: String) {

        init {
            Assert.assertTrue(name.isNotEmpty())
        }
    }
}
