package com.krystianwsul.checkme.loaders

import android.content.Context

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey


class ShowNotificationGroupLoader(context: Context, private val instanceKeys: Set<InstanceKey>) : DomainLoader<ShowNotificationGroupLoader.DomainData>(context, needsFirebase(instanceKeys)) {

    companion object {

        private fun needsFirebase(instanceKeys: Set<InstanceKey>): FirebaseLevel {
            return if (instanceKeys.any { it.type == TaskKey.Type.REMOTE })
                FirebaseLevel.NEED
            else
                FirebaseLevel.NOTHING
        }
    }

    init {
        check(!instanceKeys.isEmpty())
    }

    override val name = "ShowNotificationGroupLoader, instanceKeys: $instanceKeys"

    override fun loadDomain(domainFactory: DomainFactory): DomainData {
        check(!instanceKeys.isEmpty())

        return domainFactory.getShowNotificationGroupData(context, instanceKeys)
    }

    data class DomainData(val dataWrapper: GroupListFragment.DataWrapper) : com.krystianwsul.checkme.loaders.DomainData()
}
