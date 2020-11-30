package com.krystianwsul.checkme.gui.instances.tree

import android.view.View
import com.krystianwsul.checkme.databinding.RowListCheckableBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.setInstanceDone
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.tree.NodeType
import com.krystianwsul.checkme.gui.tree.checkable.CheckBoxState
import com.krystianwsul.checkme.gui.tree.checkable.CheckableDelegate
import com.krystianwsul.checkme.gui.tree.checkable.CheckableHolder
import com.krystianwsul.checkme.gui.tree.checkable.CheckableModelNode
import com.krystianwsul.checkme.gui.tree.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.expandable.ExpandableHolder
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineHolder
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineNameData
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeViewAdapter

class DoneInstanceNode(
        indentation: Int,
        val instanceData: GroupListDataWrapper.InstanceData,
        val dividerNode: DividerNode,
) : GroupHolderNode(indentation),
        NodeCollectionParent,
        CheckableModelNode,
        MultiLineModelNode {

    public override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    override val nodeType = NodeType.DONE

    override val ripple = true

    private lateinit var nodeCollection: NodeCollection

    private val parentNodeCollection get() = dividerNode.nodeCollection

    private val groupListFragment get() = groupAdapter.groupListFragment

    override val thumbnail = instanceData.imageState

    override val parentNode = dividerNode

    override val delegates by lazy {
        listOf(
                ExpandableDelegate(treeNode),
                CheckableDelegate(this),
                MultiLineDelegate(this)
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
                null,
                this,
                listOf()
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
                if (instanceData.colorEnabled) colorPrimary else colorDisabled
        )

    override val details
        get() = instanceData.displayText
                .takeUnless { it.isNullOrEmpty() }
                ?.let { Pair(it, if (instanceData.colorEnabled) colorSecondary else colorDisabled) }

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

    override val widthKey
        get() = MultiLineDelegate.WidthKey(
                indentation,
                checkBoxState.visibility == View.GONE,
                hasAvatar,
                thumbnail != null
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

    override fun matches(filterCriteria: Any?) = instanceData.matchesQuery((filterCriteria as? SearchData)?.query)

    override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = false

    class Holder(
            override val baseAdapter: BaseAdapter,
            binding: RowListCheckableBinding,
    ) : AbstractHolder(binding.root), ExpandableHolder, CheckableHolder, MultiLineHolder {

        override val rowContainer = binding.rowListCheckableContainer
        override val rowTextLayout = binding.rowListCheckableTextLayout
        override val rowName = binding.rowListCheckableName
        override val rowDetails = binding.rowListCheckableDetails
        override val rowChildren = binding.rowListCheckableChildren
        override val rowThumbnail = binding.rowListCheckableThumbnail
        override val rowExpand = binding.rowListCheckableExpand
        override val rowCheckBoxFrame = binding.rowListCheckableCheckboxInclude.rowCheckboxFrame
        override val rowCheckBox = binding.rowListCheckableCheckboxInclude.rowCheckbox
        override val rowMarginStart = binding.rowListCheckableMargin
        override val rowBigImage = binding.rowListCheckableBigImage
        override val rowBigImageLayout = binding.rowListCheckableBigImageLayout
        override val rowSeparator = binding.rowListCheckableSeparator
        override val rowChipGroup = binding.rowListCheckableChipGroup
        override val rowMarginEnd = binding.rowListCheckableMarginEnd
    }
}
