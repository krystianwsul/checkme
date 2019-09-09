package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class NodeCollection(
        private val indentation: Int,
        val groupAdapter: GroupListFragment.GroupAdapter,
        val useGroups: Boolean,
        val nodeContainer: NodeContainer<NodeHolder>,
        private val note: String?) {

    lateinit var notDoneGroupCollection: NotDoneGroupCollection
        private set

    lateinit var dividerNode: DividerNode
        private set

    private var unscheduledNode: UnscheduledNode? = null

    val expandedGroups: List<TimeStamp> get() = notDoneGroupCollection.expandedGroups

    val unscheduledExpanded get() = unscheduledNode?.expanded() ?: false

    val expandedTaskKeys: List<TaskKey> get() = unscheduledNode?.expandedTaskKeys ?: listOf()

    val doneExpanded get() = dividerNode.expanded()

    fun initialize(
            instanceDatas: Collection<GroupListFragment.InstanceData>,
            expandedGroups: List<TimeStamp>,
            expandedInstances: Map<InstanceKey, Boolean>,
            doneExpanded: Boolean,
            selectedInstances: List<InstanceKey>,
            selectedGroups: List<Long>,
            taskDatas: List<GroupListFragment.TaskData>,
            unscheduledExpanded: Boolean,
            expandedTaskKeys: List<TaskKey>,
            selectedTaskKeys: List<TaskKey>,
            imageData: ImageNode.ImageData?): List<TreeNode<NodeHolder>> {
        val notDoneInstanceDatas = instanceDatas.filter { it.done == null }
        val doneInstanceDatas = instanceDatas.filter { it.done != null }

        return mutableListOf<TreeNode<NodeHolder>>().apply {
            if (!note.isNullOrEmpty()) {
                check(indentation == 0)

                add(NoteNode(note, true).initialize(nodeContainer))
            }

            imageData?.let {
                check(indentation == 0)

                add(ImageNode(it).initialize(nodeContainer))
            }

            notDoneGroupCollection = NotDoneGroupCollection(indentation, this@NodeCollection, nodeContainer)

            addAll(notDoneGroupCollection.initialize(notDoneInstanceDatas, expandedGroups, expandedInstances, selectedInstances, selectedGroups))

            check(indentation == 0 || taskDatas.isEmpty())
            if (taskDatas.isNotEmpty()) {
                unscheduledNode = UnscheduledNode(this@NodeCollection)

                add(unscheduledNode!!.initialize(unscheduledExpanded, nodeContainer, taskDatas, expandedTaskKeys, selectedTaskKeys))
            }

            dividerNode = DividerNode(indentation, this@NodeCollection)

            add(dividerNode.initialize(doneExpanded && doneInstanceDatas.isNotEmpty(), nodeContainer, doneInstanceDatas, expandedInstances, selectedInstances))
        }
    }

    fun addExpandedInstances(expandedInstances: MutableMap<InstanceKey, Boolean>) {
        notDoneGroupCollection.addExpandedInstances(expandedInstances)
        dividerNode.addExpandedInstances(expandedInstances)
    }
}
