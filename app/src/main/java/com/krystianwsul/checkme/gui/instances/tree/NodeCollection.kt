package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.DetailsNode
import com.krystianwsul.checkme.gui.tree.ImageNode
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class NodeCollection(
    private val indentation: Int,
    val groupAdapter: GroupListFragment.GroupAdapter,
    val useGroups: Boolean,
    val nodeContainer: NodeContainer<AbstractHolder>,
    private val note: String?,
    val parentNode: DetailsNode.Parent?,
    private val projectInfo: DetailsNode.ProjectInfo?,
    val useDoneNode: Boolean = true,
) {

    private lateinit var notDoneGroupCollection: NotDoneGroupCollection

    lateinit var dividerNode: DividerNode
        private set

    private var unscheduledNode: UnscheduledNode? = null

    val groupExpansionStates get() = notDoneGroupCollection.groupExpansionStates

    val unscheduledExpansionState get() = unscheduledNode?.expansionState

    val taskExpansionStates get() = unscheduledNode?.taskExpansionStates.orEmpty()

    val doneExpansionState get() = dividerNode.expansionState

    val searchResults by lazy { groupAdapter.groupListFragment.searchResults }

    fun initialize(
        instanceDatas: Collection<GroupListDataWrapper.InstanceData>,
        collectionState: CollectionState,
        doneExpansionState: TreeNode.ExpansionState?,
        selectedInstances: List<InstanceKey>,
        selectedGroups: List<Long>,
        taskDatas: List<GroupListDataWrapper.TaskData>,
        unscheduledExpansionState: TreeNode.ExpansionState?,
        taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>,
        selectedTaskKeys: List<TaskKey>,
        imageData: ImageNode.ImageData?,
    ): List<TreeNode<AbstractHolder>> {
        val (notDoneInstanceDatas, doneInstanceDatas) = instanceDatas.partition { it.done == null || !useDoneNode }

        val treeNodes = mutableListOf<TreeNode<AbstractHolder>>()

        treeNodes.add(
            DetailsNode(
                projectInfo,
                note,
                parentNode,
                indentation,
            ).initialize(nodeContainer)
        )

        imageData?.let {
            check(indentation == 0)

            treeNodes.add(ImageNode(it, parentNode).initialize(nodeContainer))
        }

        notDoneGroupCollection = NotDoneGroupCollection(indentation, this, nodeContainer)

        treeNodes.addAll(
            notDoneGroupCollection.initialize(
                notDoneInstanceDatas,
                collectionState,
                selectedInstances,
                selectedGroups,
            )
        )

        check(indentation == 0 || taskDatas.isEmpty())
        if (taskDatas.isNotEmpty()) {
            unscheduledNode = UnscheduledNode(this, searchResults)

            treeNodes.add(
                unscheduledNode!!.initialize(
                    unscheduledExpansionState,
                    nodeContainer,
                    taskDatas,
                    taskExpansionStates,
                    selectedTaskKeys,
                )
            )
        }

        dividerNode = DividerNode(indentation, this, parentNode)

        treeNodes.add(
            dividerNode.initialize(
                doneExpansionState,
                nodeContainer,
                doneInstanceDatas,
                collectionState,
                selectedInstances,
            )
        )

        return treeNodes
    }

    val instanceExpansionStates get() = notDoneGroupCollection.instanceExpansionStates + dividerNode.instanceExpansionStates
}
