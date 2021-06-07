package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.domainmodel.GroupType
import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.gui.tree.ImageNode
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class NodeCollection(
    private val indentation: Int,
    val groupAdapter: GroupListFragment.GroupAdapter,
    val groupingMode: GroupType.GroupingMode,
    val nodeContainer: NodeContainer<AbstractHolder>,
    private val note: String?,
    val parentNode: DetailsNode.Parent?,
    private val projectInfo: DetailsNode.ProjectInfo?,
    private val useDoneNode: Boolean = true,
) {

    private lateinit var notDoneGroupCollection: NotDoneGroupCollection

    lateinit var dividerNode: DividerNode
        private set

    private var unscheduledNode: UnscheduledNode? = null

    val unscheduledExpansionState get() = unscheduledNode?.expansionState

    val taskExpansionStates get() = unscheduledNode?.taskExpansionStates.orEmpty()

    val doneExpansionState get() = dividerNode.expansionState

    val unscheduledFirst by lazy { groupAdapter.groupListFragment.unscheduledFirst }

    fun initialize(
        instanceDatas: Collection<GroupListDataWrapper.InstanceData>,
        contentDelegateStates: Map<NotDoneNode.ContentDelegate.Id, NotDoneNode.ContentDelegate.State>,
        doneExpansionState: TreeNode.ExpansionState?,
        taskDatas: List<GroupListDataWrapper.TaskData>,
        unscheduledExpansionState: TreeNode.ExpansionState?,
        taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>,
        selectedTaskKeys: List<TaskKey>,
        imageData: ImageNode.ImageData?,
    ): List<TreeNode<AbstractHolder>> {
        val (notDoneInstanceDatas, doneInstanceDatas) = instanceDatas.partition { it.done == null || !useDoneNode }

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

        treeNodes += notDoneGroupCollection.initialize(notDoneInstanceDatas, contentDelegateStates)

        check(indentation == 0 || taskDatas.isEmpty())
        if (taskDatas.isNotEmpty()) {
            unscheduledNode = UnscheduledNode(this, unscheduledFirst)

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
            doneInstanceDatas,
            contentDelegateStates,
        )

        return treeNodes
    }

    val contentDelegateStates: Map<NotDoneNode.ContentDelegate.Id, NotDoneNode.ContentDelegate.State>
        get() =
            notDoneGroupCollection.contentDelegateStates + dividerNode.contentDelegateStates

}
