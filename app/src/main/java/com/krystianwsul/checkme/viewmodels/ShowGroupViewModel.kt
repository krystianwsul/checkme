package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.tree.GroupListDataWrapper
import com.krystianwsul.common.time.TimeStamp

class ShowGroupViewModel : DomainViewModel<ShowGroupViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowGroupData(timeStamp)
    }

    private lateinit var timeStamp: TimeStamp

    fun start(timeStamp: TimeStamp) {
        this.timeStamp = timeStamp

        internalStart()
    }

    data class Data(val displayText: String, val groupListDataWrapper: GroupListDataWrapper?) : DomainData() {

        init {
            check(displayText.isNotEmpty())
        }
    }
}