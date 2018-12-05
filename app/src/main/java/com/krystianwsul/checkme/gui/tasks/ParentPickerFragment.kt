package com.krystianwsul.checkme.gui.tasks


import android.app.Dialog
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractDialogFragment
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.treeadapter.*
import java.util.*

class ParentPickerFragment : AbstractDialogFragment() {

    companion object {

        private const val EXPANDED_TASK_KEYS_KEY = "expandedTaskKeys"

        private const val SHOW_DELETE_KEY = "showDelete"

        fun newInstance(showDelete: Boolean) = ParentPickerFragment().apply {
            arguments = Bundle().apply {
                putBoolean(SHOW_DELETE_KEY, showDelete)
            }
        }
    }

    private lateinit var recyclerView: RecyclerView

    private var taskDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>? = null
    lateinit var listener: Listener

    private var treeViewAdapter: TreeViewAdapter? = null
    private var expandedParentKeys: List<CreateTaskViewModel.ParentKey>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXPANDED_TASK_KEYS_KEY)) {
                expandedParentKeys = savedInstanceState.getParcelableArrayList(EXPANDED_TASK_KEYS_KEY)!!
                check(!expandedParentKeys!!.isEmpty())
            }
        }

        val builder = MaterialDialog.Builder(requireActivity())
                .title(R.string.parent_dialog_title)
                .customView(R.layout.fragment_parent_picker, false)
                .negativeText(android.R.string.cancel)

        check(arguments!!.containsKey(SHOW_DELETE_KEY))

        val showDelete = arguments!!.getBoolean(SHOW_DELETE_KEY)

        if (showDelete)
            builder.neutralText(R.string.delete).onNeutral { _, _ -> listener.onTaskDeleted() }

        val materialDialog = builder.build()

        recyclerView = materialDialog.customView as RecyclerView

        return materialDialog
    }

    fun initialize(taskDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>, listener: Listener) {
        this.taskDatas = taskDatas
        this.listener = listener
        if (activity != null)
            initialize()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (taskDatas != null)
            initialize()
    }

    private fun initialize() {
        check(taskDatas != null)
        check(activity != null)

        recyclerView.layoutManager = LinearLayoutManager(activity)

        if (treeViewAdapter != null) {
            val expanded = (treeViewAdapter!!.treeModelAdapter as TaskAdapter).expandedParentKeys

            expandedParentKeys = if (expanded.isEmpty()) null else expanded
        }

        treeViewAdapter = TaskAdapter.getAdapter(this, taskDatas!!, expandedParentKeys)

        recyclerView.adapter = treeViewAdapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (treeViewAdapter != null) {
            val expandedParentKeys = (treeViewAdapter!!.treeModelAdapter as TaskAdapter).expandedParentKeys

            if (!expandedParentKeys.isEmpty())
                outState.putParcelableArrayList(EXPANDED_TASK_KEYS_KEY, ArrayList(expandedParentKeys))
        }
    }

    private class TaskAdapter private constructor(private val parentPickerFragment: ParentPickerFragment) : TreeModelAdapter, TaskParent {

        companion object {

            fun getAdapter(parentPickerFragment: ParentPickerFragment, taskDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>, expandedParentKeys: List<CreateTaskViewModel.ParentKey>?): TreeViewAdapter {
                val taskAdapter = TaskAdapter(parentPickerFragment)

                return taskAdapter.initialize(taskDatas, expandedParentKeys)
            }
        }

        private lateinit var taskWrappers: ArrayList<TaskWrapper>

        private lateinit var treeViewAdapter: TreeViewAdapter

        override val taskAdapter = this

        val expandedParentKeys get() = taskWrappers.flatMap { it.expandedParentKeys }

        private fun initialize(taskDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>, expandedParentKeys: List<CreateTaskViewModel.ParentKey>?): TreeViewAdapter {
            treeViewAdapter = TreeViewAdapter(this)

            val treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            taskWrappers = ArrayList()

            val treeNodes = ArrayList<TreeNode>()

            for (parentTreeData in taskDatas.values) {
                val taskWrapper = TaskWrapper(0, this, parentTreeData)

                treeNodes.add(taskWrapper.initialize(treeNodeCollection, expandedParentKeys))

                taskWrappers.add(taskWrapper)
            }

            treeNodeCollection.nodes = treeNodes

            return treeViewAdapter
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(parentPickerFragment.requireActivity().layoutInflater.inflate(R.layout.row_list, parent, false))

        override val hasActionMode = false

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = throw UnsupportedOperationException()

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = throw UnsupportedOperationException()

        private class TaskWrapper(indentation: Int, private val taskParent: TaskParent, val parentTreeData: CreateTaskViewModel.ParentTreeData) : GroupHolderNode(indentation), TaskParent {

            override lateinit var treeNode: TreeNode
                private set

            override val id = parentTreeData.parentKey

            private lateinit var taskWrappers: MutableList<TaskWrapper>

            override val taskAdapter get() = taskParent.taskAdapter

            private val parentFragment get() = taskAdapter.parentPickerFragment

            override val itemViewType = 0

            override val isSelectable = false

            override val isVisibleWhenEmpty = true

            override val isVisibleDuringActionMode = true

            override val isSeparatorVisibleWhenNotExpanded = false

            val expandedParentKeys: List<CreateTaskViewModel.ParentKey>
                get() {
                    val expandedParentKeys = ArrayList<CreateTaskViewModel.ParentKey>()

                    val treeNode = this.treeNode

                    if (treeNode.isExpanded) {
                        expandedParentKeys.add(parentTreeData.parentKey)

                        expandedParentKeys.addAll(taskWrappers.flatMap { it.expandedParentKeys })
                    }

                    return expandedParentKeys
                }

            fun initialize(nodeContainer: NodeContainer, expandedParentKeys: List<CreateTaskViewModel.ParentKey>?): TreeNode {
                var expanded = false
                if (expandedParentKeys != null) {
                    check(!expandedParentKeys.isEmpty())
                    expanded = expandedParentKeys.contains(parentTreeData.parentKey)
                }

                treeNode = TreeNode(this, nodeContainer, expanded, false)

                taskWrappers = ArrayList()

                val treeNodes = ArrayList<TreeNode>()

                for (parentTreeData in parentTreeData.parentTreeDatas.values) {
                    val taskWrapper = TaskWrapper(indentation + 1, this, parentTreeData)

                    treeNodes.add(taskWrapper.initialize(treeNode, expandedParentKeys))

                    taskWrappers.add(taskWrapper)
                }

                treeNode.setChildTreeNodes(treeNodes)

                return treeNode
            }

            override val name get() = Triple(parentTreeData.name, colorPrimary, true)

            override val details: Pair<String, Int>?
                get() = if (parentTreeData.scheduleText.isNullOrEmpty()) {
                    null
                } else {
                    Pair(parentTreeData.scheduleText, colorSecondary)
                }

            override val children: Pair<String, Int>?
                get() = if ((parentTreeData.parentTreeDatas.isEmpty() || treeNode.isExpanded) && TextUtils.isEmpty(parentTreeData.note)) {
                    null
                } else {
                    val text = if (!parentTreeData.parentTreeDatas.isEmpty() && !treeNode.isExpanded) {
                        parentTreeData.parentTreeDatas
                                .values
                                .joinToString(", ") { it.name }
                    } else {
                        check(!parentTreeData.note.isNullOrEmpty())

                        parentTreeData.note
                    }

                    Pair(text, colorSecondary)
                }

            override fun onClick() {
                val parentPickerFragment = parentFragment

                parentPickerFragment.dismiss()

                parentPickerFragment.listener.onTaskSelected(parentTreeData)
            }

            override fun compareTo(other: ModelNode): Int {
                var comparison = parentTreeData.sortKey.compareTo((other as TaskWrapper).parentTreeData.sortKey)
                if (indentation == 0)
                    comparison = -comparison

                return comparison
            }
        }
    }

    private interface TaskParent {

        val taskAdapter: TaskAdapter
    }

    interface Listener {
        fun onTaskSelected(parentTreeData: CreateTaskViewModel.ParentTreeData)

        fun onTaskDeleted()
    }
}
