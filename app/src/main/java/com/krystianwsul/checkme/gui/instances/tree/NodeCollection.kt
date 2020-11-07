package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.gui.instances.list.GroupListDataWrapper
import com.krystianwsul.checkme.gui.instances.list.GroupListFragment
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
        val nodeContainer: NodeContainer<NodeHolder>,
        private val note: String?,
        val parentNode: ModelNode<NodeHolder>?,
        val useDoneNode: Boolean = true
) {

    lateinit var notDoneGroupCollection: NotDoneGroupCollection
        private set

    lateinit var dividerNode: DividerNode
        private set

    private var unscheduledNode: UnscheduledNode? = null

    val expandedGroups: List<TimeStamp> get() = notDoneGroupCollection.expandedGroups

    val unscheduledExpanded get() = unscheduledNode?.expanded() ?: false

    val expandedTaskKeys: List<TaskKey> get() = unscheduledNode?.expandedTaskKeys ?: listOf()

    val doneExpanded get() = dividerNode.expanded()

    val searchResults by lazy { groupAdapter.groupListFragment.searchResults }

    fun initialize(
            instanceDatas: Collection<GroupListDataWrapper.InstanceData>,
            expandedGroups: List<TimeStamp>,
            expandedInstances: Map<InstanceKey, Boolean>,
            doneExpanded: Boolean,
            selectedInstances: List<InstanceKey>,
            selectedGroups: List<Long>,
            taskDatas: List<GroupListDataWrapper.TaskData>,
            unscheduledExpanded: Boolean,
            expandedTaskKeys: List<TaskKey>,
            selectedTaskKeys: List<TaskKey>,
            imageData: ImageNode.ImageData?
    ): List<TreeNode<NodeHolder>> {
        fun GroupListDataWrapper.InstanceData.filterNotDone() = done == null || !useDoneNode
        val notDoneInstanceDatas = instanceDatas.filter { it.filterNotDone() }
        val doneInstanceDatas = instanceDatas.filterNot { it.filterNotDone() }

        return mutableListOf<TreeNode<NodeHolder>>().apply {
            if (!note.isNullOrEmpty()) {
                check(indentation == 0)

                add(NoteNode(note, true, parentNode).initialize(nodeContainer))
            }

            imageData?.let {
                check(indentation == 0)

                add(ImageNode(it, parentNode).initialize(nodeContainer))
            }

            notDoneGroupCollection = NotDoneGroupCollection(
                    indentation,
                    this@NodeCollection,
                    nodeContainer
            )

            addAll(notDoneGroupCollection.initialize(
                    notDoneInstanceDatas,
                    expandedGroups,
                    expandedInstances,
                    selectedInstances,
                    selectedGroups
            ))

            check(indentation == 0 || taskDatas.isEmpty())
            if (taskDatas.isNotEmpty()) {
                unscheduledNode = UnscheduledNode(this@NodeCollection, searchResults)

                add(unscheduledNode!!.initialize(
                        unscheduledExpanded,
                        nodeContainer,
                        taskDatas,
                        expandedTaskKeys,
                        selectedTaskKeys
                ))
            }

            dividerNode = DividerNode(indentation, this@NodeCollection, parentNode)

            add(dividerNode.initialize(
                    doneExpanded && doneInstanceDatas.isNotEmpty(),
                    nodeContainer,
                    doneInstanceDatas,
                    expandedInstances,
                    selectedInstances
            ))
        }
    }

    fun addExpandedInstances(expandedInstances: MutableMap<InstanceKey, Boolean>) {
        notDoneGroupCollection.addExpandedInstances(expandedInstances)
        dividerNode.addExpandedInstances(expandedInstances)
    }
}
