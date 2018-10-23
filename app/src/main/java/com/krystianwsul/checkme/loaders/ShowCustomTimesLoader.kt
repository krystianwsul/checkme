package com.krystianwsul.checkme.loaders

import android.content.Context

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.viewmodels.DomainData


class ShowCustomTimesLoader(context: Context) : DomainLoader<ShowCustomTimesLoader.Data>(context, FirebaseLevel.NOTHING) {

    override val name = "ShowCustomTimesLoader"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.showCustomTimesData

    data class Data(val entries: List<CustomTimeData>) : DomainData()

    data class CustomTimeData(val id: Int, val name: String) {

        init {
            check(name.isNotEmpty())
        }
    }
}
