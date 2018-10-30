package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.TimeStamp
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode


class NodeCollection(private val density: Float, private val indentation: Int, val groupAdapter: GroupListFragment.GroupAdapter, val useGroups: Boolean, val nodeContainer: NodeContainer, private val note: String?) {

    lateinit var notDoneGroupCollection: NotDoneGroupCollection
        private set

    lateinit var dividerNode: DividerNode
        private set

    private var unscheduledNode: UnscheduledNode? = null

    val expandedGroups: List<TimeStamp> get() = notDoneGroupCollection.expandedGroups

    val unscheduledExpanded get() = unscheduledNode?.expanded() ?: false

    val expandedTaskKeys: List<TaskKey>? get() = unscheduledNode?.expandedTaskKeys

    val doneExpanded get() = dividerNode.expanded()

    fun initialize(instanceDatas: Collection<GroupListFragment.InstanceData>, expandedGroups: List<TimeStamp>?, expandedInstances: Map<InstanceKey, Boolean>?, doneExpanded: Boolean, selectedNodes: List<InstanceKey>?, selectable: Boolean, taskDatas: List<GroupListFragment.TaskData>?, unscheduledExpanded: Boolean, expandedTaskKeys: List<TaskKey>?): List<TreeNode> {
        val notDoneInstanceDatas = instanceDatas.filter { it.Done == null }
        val doneInstanceDatas = instanceDatas.filter { it.Done != null }

        return mutableListOf<TreeNode>().apply {
            if (!note.isNullOrEmpty()) {
                check(indentation == 0)

                add(NoteNode(density, note, groupAdapter.mGroupListFragment).initialize(nodeContainer))
            }

            notDoneGroupCollection = NotDoneGroupCollection(density, indentation, this@NodeCollection, nodeContainer, selectable)

            addAll(notDoneGroupCollection.initialize(notDoneInstanceDatas, expandedGroups, expandedInstances, selectedNodes))

            check(indentation == 0 || taskDatas == null)
            if (taskDatas?.isEmpty() == false) {
                unscheduledNode = UnscheduledNode(density, this@NodeCollection)

                add(unscheduledNode!!.initialize(unscheduledExpanded, nodeContainer, taskDatas, expandedTaskKeys))
            }

            dividerNode = DividerNode(density, indentation, this@NodeCollection)

            add(dividerNode.initialize(doneExpanded && !doneInstanceDatas.isEmpty(), nodeContainer, doneInstanceDatas, expandedInstances))
        }
    }

    fun addExpandedInstances(expandedInstances: Map<InstanceKey, Boolean>) {
        notDoneGroupCollection.addExpandedInstances(expandedInstances)
        dividerNode.addExpandedInstances(expandedInstances)
    }
}
