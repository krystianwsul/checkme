package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getShowGroupData
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper

class ShowGroupViewModel : DomainViewModel<ShowGroupViewModel.Data>() {

    override val domainListener = object : DomainListener<Data>() {

        override fun getData(domainFactory: DomainFactory) = domainFactory.getShowGroupData(parameters)
    }

    private lateinit var parameters: ShowGroupActivity.Parameters

    fun start(parameters: ShowGroupActivity.Parameters) {
        this.parameters = parameters

        internalStart()
    }

    data class Data(
        val title: String,
        val subTitle: String?,
        val groupListDataWrapper: GroupListDataWrapper?,
    ) : DomainData() {

        init {
            check(title.isNotEmpty())
        }
    }
}