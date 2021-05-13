package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
import com.krystianwsul.checkme.gui.tree.*
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class NodeCollection(
    private val indentation: Int,
    val groupAdapter: GroupListFragment.GroupAdapter,
    val useGroups: Boolean,
    val nodeContainer: NodeContainer<AbstractHolder>,
    private val note: String?,
    val parentNode: ModelNode<AbstractHolder>?,
    private val projectInfo: DetailsNode.ProjectInfo?,
    val useDoneNode: Boolean = true,
    private val projectNameShownInParent: () -> Boolean = { false },
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
        expandedGroups: Map<TimeStamp, TreeNode.ExpansionState>,
        expandedInstances: Map<InstanceKey, CollectionExpansionState>,
        doneExpansionState: TreeNode.ExpansionState?,
        selectedInstances: List<InstanceKey>,
        selectedGroups: List<Long>,
        taskDatas: List<GroupListDataWrapper.TaskData>,
        unscheduledExpansionState: TreeNode.ExpansionState?,
        taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState>,
        selectedTaskKeys: List<TaskKey>,
        imageData: ImageNode.ImageData?,
    ): List<TreeNode<AbstractHolder>> {
        fun GroupListDataWrapper.InstanceData.filterNotDone() = done == null || !useDoneNode
        val notDoneInstanceDatas = instanceDatas.filter { it.filterNotDone() }
        val doneInstanceDatas = instanceDatas.filterNot { it.filterNotDone() }

        return mutableListOf<TreeNode<AbstractHolder>>().apply {
            add(
                DetailsNode(
                    projectInfo,
                    note,
                    parentNode,
                    indentation,
                    projectNameShownInParent,
                ).initialize(nodeContainer)
            )

            imageData?.let {
                check(indentation == 0)

                add(ImageNode(it, parentNode).initialize(nodeContainer))
            }

            notDoneGroupCollection = NotDoneGroupCollection(
                indentation,
                this@NodeCollection,
                nodeContainer
            )

            addAll(
                notDoneGroupCollection.initialize(
                    notDoneInstanceDatas,
                    expandedGroups,
                    expandedInstances,
                    selectedInstances,
                    selectedGroups
                )
            )

            check(indentation == 0 || taskDatas.isEmpty())
            if (taskDatas.isNotEmpty()) {
                unscheduledNode = UnscheduledNode(this@NodeCollection, searchResults)

                add(
                    unscheduledNode!!.initialize(
                        unscheduledExpansionState,
                        nodeContainer,
                        taskDatas,
                        taskExpansionStates,
                        selectedTaskKeys
                    )
                )
            }

            dividerNode = DividerNode(indentation, this@NodeCollection, parentNode)

            add(
                dividerNode.initialize(
                    doneExpansionState,
                    nodeContainer,
                    doneInstanceDatas,
                    expandedInstances,
                    selectedInstances
                )
            )
        }
    }

    val instanceExpansionStates
        get() =
            notDoneGroupCollection.instanceExpansionStates + dividerNode.instanceExpansionStates
}
