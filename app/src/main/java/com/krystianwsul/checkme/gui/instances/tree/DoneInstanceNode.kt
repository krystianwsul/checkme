package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceDone
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeViewAdapter

class DoneInstanceNode(
        indentation: Int,
        val instanceData: GroupListDataWrapper.InstanceData,
        val dividerNode: DividerNode
) : GroupHolderNode(indentation), NodeCollectionParent {

    public override lateinit var treeNode: TreeNode<NodeHolder>
        private set

    override val ripple = true

    private lateinit var nodeCollection: NodeCollection

    private val parentNodeCollection get() = dividerNode.nodeCollection

    private val groupListFragment get() = groupAdapter.groupListFragment

    override val thumbnail = instanceData.imageState

    override val parentNode = dividerNode

    fun initialize(
            dividerTreeNode: TreeNode<NodeHolder>,
            expandedInstances: Map<InstanceKey, Boolean>,
            selectedInstances: List<InstanceKey>): TreeNode<NodeHolder> {
        val selected = selectedInstances.contains(instanceData.instanceKey)

        val expanded: Boolean
        val doneExpanded: Boolean
        if (expandedInstances.containsKey(instanceData.instanceKey) && instanceData.children.isNotEmpty()) {
            expanded = true
            doneExpanded = expandedInstances.getValue(instanceData.instanceKey)
        } else {
            expanded = false
            doneExpanded = false
        }

        treeNode = TreeNode(this, dividerTreeNode, expanded, selected)

        nodeCollection = NodeCollection(
                indentation + 1,
                groupAdapter,
                false,
                treeNode,
                null,
                this
        )

        treeNode.setChildTreeNodes(nodeCollection.initialize(
                instanceData.children.values,
                listOf(),
                expandedInstances,
                doneExpanded,
                listOf(),
                listOf(),
                listOf(),
                false,
                listOf(),
                listOf(),
                null))

        return treeNode
    }

    private fun expanded() = treeNode.isExpanded

    fun addExpandedInstances(expandedInstances: MutableMap<InstanceKey, Boolean>) {
        if (!expanded())
            return

        check(!expandedInstances.containsKey(instanceData.instanceKey))

        expandedInstances[instanceData.instanceKey] = nodeCollection.doneExpanded
        nodeCollection.addExpandedInstances(expandedInstances)
    }

    override val groupAdapter by lazy { parentNodeCollection.groupAdapter }

    override val name get() = NameData(instanceData.name, if (!instanceData.taskCurrent) colorDisabled else colorPrimary)

    override val details
        get() = instanceData.displayText
                .takeUnless { it.isNullOrEmpty() }
                ?.let { Pair(it, if (!instanceData.taskCurrent) colorDisabled else colorSecondary) }

    override val children get() = NotDoneGroupNode.NotDoneInstanceNode.getChildrenNew(treeNode, instanceData)

    override val checkBoxState
        get() = if (groupListFragment.selectionCallback.hasActionMode) {
            CheckBoxState.Invisible
        } else {
            CheckBoxState.Visible(true) {
                val nodeCollection = dividerNode.nodeCollection
                val groupAdapter = nodeCollection.groupAdapter

                groupAdapter.treeNodeCollection
                        .treeViewAdapter
                        .updateDisplayedNodes {
                            instanceData.done = DomainFactory.instance.setInstanceDone(
                                groupAdapter.dataId,
                                SaveService.Source.GUI,
                                instanceData.instanceKey,
                                false
                            )

                            dividerNode.remove(this, it)

                            nodeCollection.notDoneGroupCollection.add(instanceData, it)
                        }

                groupListFragment.listener.showSnackbarNotDone(1) {
                    DomainFactory.instance.setInstanceDone(
                        0,
                        SaveService.Source.GUI,
                        instanceData.instanceKey,
                        true
                    )
                }
            }
        }

    override fun compareTo(other: ModelNode<NodeHolder>): Int {
        checkNotNull(instanceData.done)

        val doneInstanceNode = other as DoneInstanceNode
        checkNotNull(doneInstanceNode.instanceData.done)

        return -instanceData.done!!.compareTo(doneInstanceNode.instanceData.done!!) // negate
    }

    override val isSelectable = true

    override fun onClick(holder: NodeHolder) = groupListFragment.activity.startActivity(ShowInstanceActivity.getIntent(groupListFragment.activity, instanceData.instanceKey))

    fun removeFromParent(x: TreeViewAdapter.Placeholder) {
        dividerNode.remove(this, x)

        treeNode.deselect(x)
    }

    override val id = instanceData.instanceKey

    override fun normalize() = instanceData.normalize()

    override fun filter(filterCriteria: Any?) = instanceData.matchesQuery((filterCriteria as? SearchData)?.query)
}
