package com.krystianwsul.checkme.loaders

import android.content.Context

import com.krystianwsul.checkme.domainmodel.DomainFactory


class ShowCustomTimesLoader(context: Context) : DomainLoader<ShowCustomTimesLoader.DomainData>(context, FirebaseLevel.NOTHING) {

    override val name = "ShowCustomTimesLoader"

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.showCustomTimesData

    data class DomainData(val entries: List<CustomTimeData>) : com.krystianwsul.checkme.loaders.DomainData()

    data class CustomTimeData(val id: Int, val name: String) {

        init {
            check(name.isNotEmpty())
        }
    }
}
