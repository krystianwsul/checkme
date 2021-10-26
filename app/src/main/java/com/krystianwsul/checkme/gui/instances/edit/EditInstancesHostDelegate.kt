package com.krystianwsul.checkme.gui.instances.edit

import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.common.utils.InstanceKey

abstract class EditInstancesHostDelegate : EditInstancesFragment.Listener {

    companion object {

        private const val TAG_EDIT_INSTANCES = "editInstances"
    }

    protected abstract val activity: AbstractActivity

    protected open val tag = TAG_EDIT_INSTANCES

    fun onCreate() {
        activity.tryGetFragment<EditInstancesFragment>(tag)?.setListener(this)
    }

    fun show(instanceKeys: List<InstanceKey>) {
        EditInstancesFragment.newInstance(instanceKeys).also { it.setListener(this) }.show(
            activity.supportFragmentManager,
            tag,
        )
    }
}