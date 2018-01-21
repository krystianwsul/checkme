package com.krystianwsul.checkme.loaders

import android.content.Context

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.time.TimeStamp

import junit.framework.Assert

class ShowGroupLoader(context: Context, private val timeStamp: TimeStamp) : DomainLoader<ShowGroupLoader.Data>(context, DomainLoader.FirebaseLevel.WANT) {

    override fun getName() = "ShowGroupLoader, timeStamp: " + timeStamp

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getShowGroupData(context, timeStamp)

    data class Data(val displayText: String, val dataWrapper: GroupListFragment.DataWrapper?) : DomainLoader.Data() {

        init {
            Assert.assertTrue(displayText.isNotEmpty())
        }
    }
}
