package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.domainmodel.GroupTypeFactory
import com.krystianwsul.checkme.domainmodel.MixedInstanceDataCollection
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.gui.tree.ImageNode
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class NodeCollection(
    private val indentation: Int,
    val groupAdapter: GroupListFragment.GroupAdapter,
    val nodeContainer: NodeContainer<AbstractHolder>,
    private val note: String?,
    val parentNode: DetailsNode.Parent?,
    private val projectInfo: DetailsNode.ProjectInfo?,
    private val unscheduledProjectKey: ProjectKey.Shared?,
    val doneBeforeNotDone: Boolean,
) {

    private lateinit var notDoneGroupCollection: NotDoneGroupCollection

    private lateinit var dividerNode: DividerNode

    private var unscheduledNode: UnscheduledNode? = null

    val unscheduledExpansionState get() = unscheduledNode?.expansionState

    val taskExpansionStates get() = unscheduledNode?.taskExpansionStates.orEmpty()

    val doneExpansionState get() = dividerNode.expansionState

    val unscheduledFirst by lazy { groupAdapter.groupListFragment.unscheduledFirst }

    fun initialize(
        mixedInstanceDataCollection: MixedInstanceDataCollection,
        doneSingleBridges: List<GroupTypeFactory.SingleBridge>,
        contentDelegateStates: Map<NotDoneNode.ContentDelegate.Id, NotDoneNode.ContentDelegate.State>,
        doneExpansionState: TreeNode.ExpansionState?,
        taskDatas: List<GroupListDataWrapper.TaskData>,
        unscheduledExpansionState: TreeNode.ExpansionState?,
        taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>,
        selectedTaskKeys: List<TaskKey>,
        imageData: ImageNode.ImageData?,
    ): List<TreeNode<AbstractHolder>> {
        val treeNodes = mutableListOf<TreeNode<AbstractHolder>>()

        treeNodes += DetailsNode(
            projectInfo,
            note,
            parentNode,
            indentation,
        ).initialize(nodeContainer)

        imageData?.let {
            check(indentation == 0)

            treeNodes += ImageNode(it, parentNode).initialize(nodeContainer)
        }

        notDoneGroupCollection = NotDoneGroupCollection(indentation, this, nodeContainer)

        treeNodes += notDoneGroupCollection.initialize(mixedInstanceDataCollection, contentDelegateStates)

        check(indentation == 0 || taskDatas.isEmpty())
        if (taskDatas.isNotEmpty()) {
            unscheduledNode = UnscheduledNode(this, unscheduledFirst, unscheduledProjectKey)

            treeNodes += unscheduledNode!!.initialize(
                unscheduledExpansionState,
                nodeContainer,
                taskDatas,
                taskExpansionStates,
                selectedTaskKeys,
            )
        }

        dividerNode = DividerNode(indentation, this, parentNode)

        treeNodes += dividerNode.initialize(
            doneExpansionState,
            nodeContainer,
            doneSingleBridges,
            contentDelegateStates,
        )

        return treeNodes
    }

    val contentDelegateStates: Map<NotDoneNode.ContentDelegate.Id, NotDoneNode.ContentDelegate.State>
        get() =
            notDoneGroupCollection.contentDelegateStates + dividerNode.contentDelegateStates

}
