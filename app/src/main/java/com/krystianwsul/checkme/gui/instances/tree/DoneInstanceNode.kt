package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.treeadapter.ModelNode

class DoneInstanceNode(
    override val indentation: Int,
    private val instanceData: GroupListDataWrapper.InstanceData,
    override val parentNode: DividerNode,
) : NotDoneNode(ContentDelegate.Instance(parentNode.nodeCollection.groupAdapter, instanceData, indentation)) {

    override val groupAdapter by lazy { parentNode.nodeCollection.groupAdapter }

    override val id = Id(super.id)

    data class Id(val superId: Any)

    override fun compareTo(other: ModelNode<AbstractHolder>): Int {
        checkNotNull(instanceData.done)

        val doneInstanceNode = other as DoneInstanceNode
        checkNotNull(doneInstanceNode.instanceData.done)

        return -instanceData.done.compareTo(doneInstanceNode.instanceData.done) // negate
    }
}
