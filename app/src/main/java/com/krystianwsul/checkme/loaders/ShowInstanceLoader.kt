package com.krystianwsul.checkme.loaders

import android.content.Context
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import junit.framework.Assert

class ShowInstanceLoader(context: Context, private val instanceKey: InstanceKey) : DomainLoader<ShowInstanceLoader.Data>(context, if (instanceKey.type == TaskKey.Type.REMOTE) DomainLoader.FirebaseLevel.NEED else DomainLoader.FirebaseLevel.NOTHING) {

    override val name = "ShowInstanceLoader, instanceKey: " + instanceKey

    override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getShowInstanceData(context, instanceKey)

    data class Data(val instanceData: InstanceData?) : DomainLoader.Data()

    data class InstanceData(val name: String, val displayText: String?, var done: Boolean, val taskCurrent: Boolean, val isRootInstance: Boolean, val exists: Boolean, val dataWrapper: GroupListFragment.DataWrapper) {

        init {
            Assert.assertTrue(name.isNotEmpty())
        }
    }
}
