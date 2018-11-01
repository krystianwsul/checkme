package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey

class ShowNotificationGroupViewModel : DomainViewModel<ShowNotificationGroupViewModel.Data>() {

    private lateinit var instanceKeys: Set<InstanceKey>

    fun start(instanceKeys: Set<InstanceKey>) {
        check(!instanceKeys.isEmpty())

        this.instanceKeys = instanceKeys

        val firebaseLevel = if (instanceKeys.any { it.type == TaskKey.Type.REMOTE })
            FirebaseLevel.NEED
        else
            FirebaseLevel.NOTHING

        internalStart(firebaseLevel)
    }

    override fun getData() = domainFactory.getShowNotificationGroupData(instanceKeys)

    data class Data(val dataWrapper: GroupListFragment.DataWrapper) : DomainData()
}