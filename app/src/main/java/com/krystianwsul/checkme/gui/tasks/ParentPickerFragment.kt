package com.krystianwsul.checkme.gui.tasks


import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.jakewharton.rxbinding3.view.touches
import com.jakewharton.rxbinding3.widget.textChanges
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractDialogFragment
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.treeadapter.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_parent_picker.view.*

class ParentPickerFragment : AbstractDialogFragment() {

    companion object {

        private const val EXPANDED_TASK_KEYS_KEY = "expandedTaskKeys"
        private const val QUERY_KEY = "query"

        private const val SHOW_DELETE_KEY = "showDelete"

        fun newInstance(showDelete: Boolean) = ParentPickerFragment().apply {
            arguments = Bundle().apply {
                putBoolean(SHOW_DELETE_KEY, showDelete)
            }
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchField: EditText
    private lateinit var searchChanges: Observable<String>

    private var taskDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>? = null
    lateinit var listener: Listener

    private var treeViewAdapter: TreeViewAdapter? = null
    private var expandedParentKeys: List<CreateTaskViewModel.ParentKey>? = null

    private val initializeDisposable = CompositeDisposable()

    private var query: String = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        check(arguments!!.containsKey(SHOW_DELETE_KEY))

        savedInstanceState?.apply {
            if (containsKey(EXPANDED_TASK_KEYS_KEY)) {
                expandedParentKeys = getParcelableArrayList(EXPANDED_TASK_KEYS_KEY)!!
                check(!expandedParentKeys!!.isEmpty())
            }

            query = getString(QUERY_KEY)!!
        }

        return MaterialDialog(requireActivity()).apply {
            title(R.string.parent_dialog_title)
            customView(R.layout.fragment_parent_picker)
            negativeButton(android.R.string.cancel)

            @Suppress("DEPRECATION")
            if (arguments!!.getBoolean(SHOW_DELETE_KEY))
                neutralButton(R.string.delete) { listener.onTaskDeleted() }

            getCustomView()!!.apply {
                recyclerView = parentPickerRecycler as RecyclerView

                searchField = parentPickerSearch as EditText
                searchChanges = searchField.textChanges().map { it.toString() }
            }
        }
    }

    fun initialize(taskDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>, listener: Listener) {
        this.taskDatas = taskDatas
        this.listener = listener

        if (this::recyclerView.isInitialized)
            initialize()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(activity)

        if (taskDatas != null)
            initialize()
    }

    private fun initialize() {
        check(taskDatas != null)
        check(activity != null)

        initializeDisposable.clear()

        if (treeViewAdapter != null) {
            val expanded = (treeViewAdapter!!.treeModelAdapter as TaskAdapter).expandedParentKeys

            expandedParentKeys = if (expanded.isEmpty()) null else expanded

            treeViewAdapter!!.updateDisplayedNodes(true) {
                (treeViewAdapter!!.treeModelAdapter as TaskAdapter).initialize(taskDatas!!, expandedParentKeys)

                query.takeIf { it.isNotEmpty() }?.let { search(it, TreeViewAdapter.Placeholder) }
            }
        } else {
            val taskAdapter = TaskAdapter(this)
            taskAdapter.initialize(taskDatas!!, expandedParentKeys)
            treeViewAdapter = taskAdapter.treeViewAdapter
            recyclerView.adapter = treeViewAdapter

            treeViewAdapter!!.updateDisplayedNodes {
                query.takeIf { it.isNotEmpty() }?.let { search(it, TreeViewAdapter.Placeholder) }
            }
        }

        initializeDisposable += searchChanges.subscribe {
            treeViewAdapter!!.updateDisplayedNodes {
                search(it, TreeViewAdapter.Placeholder)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        searchField.apply {
            startDisposable += searchChanges.subscribe {
                val drawable = if (it.isNullOrEmpty()) R.drawable.ic_close_white_24dp else R.drawable.ic_close_black_24dp
                setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0)
            }

            startDisposable += touches {
                if (it.x >= width - totalPaddingEnd) {
                    if (it.action == MotionEvent.ACTION_UP)
                        text.clear()

                    true
                } else {
                    false
                }
            }.subscribe()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (treeViewAdapter != null) {
            val expandedParentKeys = (treeViewAdapter!!.treeModelAdapter as TaskAdapter).expandedParentKeys

            if (!expandedParentKeys.isEmpty())
                outState.putParcelableArrayList(EXPANDED_TASK_KEYS_KEY, ArrayList(expandedParentKeys))
        }

        outState.putString(QUERY_KEY, query)
    }

    override fun onDestroyView() {
        initializeDisposable.clear()

        super.onDestroyView()
    }

    private fun search(query: String, @Suppress("UNUSED_PARAMETER") x: TreeViewAdapter.Placeholder) {
        this.query = query

        treeViewAdapter!!.query = query
    }

    private class TaskAdapter(private val parentPickerFragment: ParentPickerFragment) : TreeModelAdapter, TaskParent {

        private lateinit var taskWrappers: MutableList<TaskWrapper>

        val treeViewAdapter = TreeViewAdapter(this, null)

        override val taskAdapter = this

        val expandedParentKeys get() = taskWrappers.flatMap { it.expandedParentKeys }

        fun initialize(taskDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>, expandedParentKeys: List<CreateTaskViewModel.ParentKey>?) {
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
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(parentPickerFragment.requireActivity().layoutInflater.inflate(R.layout.row_list, parent, false))

        override val hasActionMode = false

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = throw UnsupportedOperationException()

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = throw UnsupportedOperationException()

        private class TaskWrapper(indentation: Int, private val taskParent: TaskParent, val parentTreeData: CreateTaskViewModel.ParentTreeData) : GroupHolderNode(indentation), TaskParent {

            companion object {

                private val background = getColor(android.R.color.white)
            }

            override lateinit var treeNode: TreeNode
                private set

            override val ripple = true

            override val id = parentTreeData.parentKey

            private lateinit var taskWrappers: MutableList<TaskWrapper>

            override val taskAdapter get() = taskParent.taskAdapter

            private val parentFragment get() = taskAdapter.parentPickerFragment

            override val itemViewType = 0

            override val isSelectable = false

            override val isVisibleWhenEmpty = true

            override val isVisibleDuringActionMode = true

            override val isSeparatorVisibleWhenNotExpanded = false

            override val colorBackground: Int
                get() = background

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

            override fun matchesSearch(query: String) = parentTreeData.matchesSearch(query)
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
