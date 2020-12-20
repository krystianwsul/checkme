package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceDone
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.AbstractModelNode
import com.krystianwsul.checkme.gui.tree.HolderType
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckableModelNode
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineNameData
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailDelegate
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailModelNode
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeViewAdapter

class DoneInstanceNode(
        override val indentation: Int,
        val instanceData: GroupListDataWrapper.InstanceData,
        val dividerNode: DividerNode,
) : AbstractModelNode(),
        NodeCollectionParent,
        CheckableModelNode,
        MultiLineModelNode,
        ThumbnailModelNode,
        IndentationModelNode {

    public override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    override val holderType = HolderType.CHECKABLE

    private lateinit var nodeCollection: NodeCollection

    private val parentNodeCollection get() = dividerNode.nodeCollection

    private val groupListFragment get() = groupAdapter.groupListFragment

    override val thumbnail = instanceData.imageState

    override val parentNode = dividerNode

    override val delegates by lazy {
        listOf(
                ExpandableDelegate(treeNode),
                CheckableDelegate(this),
                MultiLineDelegate(this),
                ThumbnailDelegate(this),
                IndentationDelegate(this)
        )
    }

    fun initialize(
            dividerTreeNode: TreeNode<AbstractHolder>,
            expandedInstances: Map<InstanceKey, Boolean>,
            selectedInstances: List<InstanceKey>,
    ): TreeNode<AbstractHolder> {
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
                instanceData.note,
                this,
                instanceData.projectInfo
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

    override val name
        get() = MultiLineNameData.Visible(
                instanceData.name,
                if (instanceData.taskCurrent) R.color.textPrimary else R.color.textDisabled
        )

    override val details
        get() = instanceData.displayText
                .takeUnless { it.isNullOrEmpty() }
                ?.let { Pair(it, if (instanceData.taskCurrent) R.color.textSecondary else R.color.textDisabled) }

    override val children get() = NotDoneGroupNode.NotDoneInstanceNode.getChildrenText(treeNode, instanceData)

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
                                    DomainListenerManager.NotificationType.Skip(groupAdapter.dataId),
                                    SaveService.Source.GUI,
                                    instanceData.instanceKey,
                                    false
                            )

                            dividerNode.remove(this, it)

                            nodeCollection.notDoneGroupCollection.add(instanceData, it)
                        }

                groupListFragment.listener.showSnackbarNotDone(1) {
                    DomainFactory.instance.setInstanceDone(
                            DomainListenerManager.NotificationType.First(groupAdapter.dataId),
                            SaveService.Source.GUI,
                            instanceData.instanceKey,
                            true
                    )
                }
            }
        }

    override val widthKey
        get() = MultiLineDelegate.WidthKey(
                indentation,
                true,
                thumbnail != null,
                true
        )

    override fun compareTo(other: ModelNode<AbstractHolder>): Int {
        checkNotNull(instanceData.done)

        val doneInstanceNode = other as DoneInstanceNode
        checkNotNull(doneInstanceNode.instanceData.done)

        return -instanceData.done!!.compareTo(doneInstanceNode.instanceData.done!!) // negate
    }

    override val isSelectable = true

    override fun onClick(holder: AbstractHolder) = groupListFragment.activity.startActivity(ShowInstanceActivity.getIntent(groupListFragment.activity, instanceData.instanceKey))

    fun removeFromParent(x: TreeViewAdapter.Placeholder) {
        dividerNode.remove(this, x)

        treeNode.deselect(x)
    }

    override val id = instanceData.instanceKey

    override fun normalize() = instanceData.normalize()

    override fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams) =
            instanceData.matchesFilterParams(filterParams)

    override fun getMatchResult(query: String) = ModelNode.MatchResult.fromBoolean(instanceData.matchesQuery(query))
}
