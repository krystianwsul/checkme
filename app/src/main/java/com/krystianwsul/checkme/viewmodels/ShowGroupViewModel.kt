package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.time.TimeStamp

class ShowGroupViewModel : DomainViewModel<ShowGroupViewModel.Data>() {

    private lateinit var timeStamp: TimeStamp

    fun start(timeStamp: TimeStamp) {
        this.timeStamp = timeStamp

        internalStart()
    }

    override fun getData(domainFactory: DomainFactory) = domainFactory.getShowGroupData(timeStamp)

    data class Data(val displayText: String, val dataWrapper: GroupListFragment.DataWrapper?) : DomainData() {

        init {
            check(displayText.isNotEmpty())
        }
    }
}