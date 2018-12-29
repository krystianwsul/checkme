package com.krystianwsul.checkme.viewmodels

import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey

class ShowInstanceViewModel : DomainViewModel<ShowInstanceViewModel.Data>() {

    private lateinit var instanceKey: InstanceKey

    fun start(instanceKey: InstanceKey) {
        this.instanceKey = instanceKey

        internalStart(if (instanceKey.type == TaskKey.Type.REMOTE) FirebaseLevel.NEED else FirebaseLevel.NOTHING)
    }

    override fun getData() = kotlinDomainFactory.getShowInstanceData(instanceKey)

    data class Data(
            val name: String,
            val displayText: String?,
            var done: Boolean,
            var taskCurrent: Boolean,
            val isRootInstance: Boolean,
            var exists: Boolean,
            val dataWrapper: GroupListFragment.DataWrapper) : DomainData() {

        init {
            check(name.isNotEmpty())
        }
    }
}