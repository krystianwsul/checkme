package com.krystianwsul.checkme.loaders

import android.content.Context

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.time.TimeStamp


class ShowGroupLoader(context: Context, private val timeStamp: TimeStamp) : DomainLoader<ShowGroupLoader.DomainData>(context, FirebaseLevel.WANT) {

    override val name = "ShowGroupLoader, timeStamp: " + timeStamp

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getShowGroupData(context, timeStamp)

    data class DomainData(val displayText: String, val dataWrapper: GroupListFragment.DataWrapper?) : com.krystianwsul.checkme.loaders.DomainData() {

        init {
            check(displayText.isNotEmpty())
        }
    }
}
