package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.loaders.FirebaseLevel
import com.krystianwsul.checkme.utils.time.TimeStamp

class ShowGroupViewModel : DomainViewModel<ShowGroupViewModel.Data>() {

    private lateinit var timeStamp: TimeStamp

    fun start(timeStamp: TimeStamp) {
        this.timeStamp = timeStamp

        internalStart(FirebaseLevel.WANT)
    }

    override fun getData(domainFactory: DomainFactory) = domainFactory.getShowGroupData(MyApplication.instance, timeStamp)
    data class Data(val displayText: String, val dataWrapper: GroupListFragment.DataWrapper?) : DomainData() {

        init {
            check(displayText.isNotEmpty())
        }
    }
}