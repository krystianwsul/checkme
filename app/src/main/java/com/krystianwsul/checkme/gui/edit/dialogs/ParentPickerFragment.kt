package com.krystianwsul.checkme.gui.edit.dialogs


import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.rxbinding3.view.touches
import com.jakewharton.rxbinding3.widget.textChanges
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import com.krystianwsul.checkme.gui.instances.tree.*
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.utils.normalized
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

        private val taskBackground by lazy {
            GroupHolderNode.getColor(android.R.color.white)
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchField: EditText
    private lateinit var searchChanges: Observable<String>

    private var taskDatas: Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData>? = null
    lateinit var listener: Listener

    private var treeViewAdapter: TreeViewAdapter<NodeHolder>? = null
    private var expandedParentKeys: List<EditViewModel.ParentKey>? = null

    private val initializeDisposable = CompositeDisposable()

    private var query: String = ""

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        check(requireArguments().containsKey(SHOW_DELETE_KEY))

        savedInstanceState?.apply {
            if (containsKey(EXPANDED_TASK_KEYS_KEY)) {
                expandedParentKeys = getParcelableArrayList(EXPANDED_TASK_KEYS_KEY)!!
                check(expandedParentKeys!!.isNotEmpty())
            }

            query = getString(QUERY_KEY)!!
        }

        val view = requireActivity().layoutInflater
                .inflate(R.layout.fragment_parent_picker, null)
                .apply {
                    recyclerView = parentPickerRecycler as RecyclerView

                    searchField = parentPickerSearch as EditText
                    searchChanges = searchField.textChanges().map { it.toString().normalized() }
                }

        return MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.parent_dialog_title)
                .setView(view)
                .setPositiveButton(R.string.add_task) { _, _ -> listener.onNewParent(searchField.text?.toString()) }
                .setNegativeButton(android.R.string.cancel, null)
                .apply {
                    if (requireArguments().getBoolean(SHOW_DELETE_KEY))
                        setNeutralButton(R.string.delete) { _, _ -> listener.onTaskDeleted() }
                }
                .create()
    }

    fun initialize(taskDatas: Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData>, listener: Listener) {
        this.taskDatas = taskDatas
        this.listener = listener

        if (this::recyclerView.isInitialized)
            initialize()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(activity)

        if (taskDatas != null) initialize()
    }

    private fun initialize() {
        check(taskDatas != null)
        check(activity != null)

        initializeDisposable.clear()

        if (treeViewAdapter != null) {
            val expanded = (treeViewAdapter!!.treeModelAdapter as TaskAdapter).expandedParentKeys

            expandedParentKeys = if (expanded.isEmpty()) null else expanded

            treeViewAdapter!!.updateDisplayedNodes {
                (treeViewAdapter!!.treeModelAdapter as TaskAdapter).initialize(taskDatas!!, expandedParentKeys)
            }
        } else {
            val taskAdapter = TaskAdapter(this)
            taskAdapter.initialize(taskDatas!!, expandedParentKeys)
            treeViewAdapter = taskAdapter.treeViewAdapter
            recyclerView.adapter = treeViewAdapter
            recyclerView.itemAnimator = CustomItemAnimator()

            if (query.isNotEmpty()) treeViewAdapter!!.updateDisplayedNodes { search(query, it) }
        }

        initializeDisposable += searchChanges.subscribe { query ->
            treeViewAdapter!!.updateDisplayedNodes { search(query, it) }
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

            if (expandedParentKeys.isNotEmpty())
                outState.putParcelableArrayList(EXPANDED_TASK_KEYS_KEY, ArrayList(expandedParentKeys))
        }

        outState.putString(QUERY_KEY, query)
    }

    override fun onDestroyView() {
        initializeDisposable.clear()

        super.onDestroyView()
    }

    private fun search(query: String, placeholder: TreeViewAdapter.Placeholder) {
        this.query = query

        treeViewAdapter!!.setFilterCriteria(
                query.takeIf { it.isNotEmpty() }?.let { SearchData(it, false) },
                placeholder
        )
    }

    private inner class TaskAdapter(private val parentPickerFragment: ParentPickerFragment) :
            GroupHolderAdapter(),
            TaskParent {

        private lateinit var taskWrappers: MutableList<TaskWrapper>

        val treeViewAdapter = TreeViewAdapter(this, null, viewCreatedDisposable)

        override val taskAdapter = this

        val expandedParentKeys get() = taskWrappers.flatMap { it.expandedParentKeys }

        override lateinit var treeNodeCollection: TreeNodeCollection<NodeHolder>
            private set

        fun initialize(
                taskDatas: Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData>,
                expandedParentKeys: List<EditViewModel.ParentKey>?
        ) {
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            taskWrappers = ArrayList()

            val treeNodes = ArrayList<TreeNode<NodeHolder>>()

            for (parentTreeData in taskDatas.values) {
                val taskWrapper = TaskWrapper(0, this, parentTreeData, null)

                treeNodes.add(taskWrapper.initialize(treeNodeCollection, expandedParentKeys))

                taskWrappers.add(taskWrapper)
            }

            treeNodeCollection.nodes = treeNodes
        }

        override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int,
        ) = DialogNodeHolder(parentPickerFragment.requireActivity().layoutInflater.inflate(
                R.layout.row_list_dialog,
                parent,
                false
        ))

        override val hasActionMode = false

        override fun incrementSelected(placeholder: TreeViewAdapter.Placeholder) = throw UnsupportedOperationException()

        override fun decrementSelected(placeholder: TreeViewAdapter.Placeholder) = throw UnsupportedOperationException()

        private inner class TaskWrapper(
                indentation: Int,
                private val taskParent: TaskParent,
                val parentTreeData: EditViewModel.ParentTreeData,
                override val parentNode: ModelNode<NodeHolder>?
        ) : GroupHolderNode(indentation), TaskParent {

            override lateinit var treeNode: TreeNode<NodeHolder>
                private set

            override val ripple = true

            override val id = parentTreeData.parentKey

            private lateinit var taskWrappers: MutableList<TaskWrapper>

            override val taskAdapter get() = taskParent.taskAdapter

            private val parentFragment get() = taskAdapter.parentPickerFragment

            override val itemViewType = 0

            override val colorBackground: Int
                get() = taskBackground

            val expandedParentKeys: List<EditViewModel.ParentKey>
                get() {
                    val expandedParentKeys = ArrayList<EditViewModel.ParentKey>()

                    val treeNode = this.treeNode

                    if (treeNode.isExpanded) {
                        expandedParentKeys.add(parentTreeData.parentKey)

                        expandedParentKeys.addAll(taskWrappers.flatMap { it.expandedParentKeys })
                    }

                    return expandedParentKeys
                }

            fun initialize(
                    nodeContainer: NodeContainer<NodeHolder>,
                    expandedParentKeys: List<EditViewModel.ParentKey>?
            ): TreeNode<NodeHolder> {
                var expanded = false
                if (expandedParentKeys != null) {
                    check(expandedParentKeys.isNotEmpty())
                    expanded = expandedParentKeys.contains(parentTreeData.parentKey)
                }

                treeNode = TreeNode(this, nodeContainer, expanded, false)

                taskWrappers = ArrayList()

                val treeNodes = ArrayList<TreeNode<NodeHolder>>()

                for (parentTreeData in parentTreeData.parentTreeDatas.values) {
                    val taskWrapper = TaskWrapper(indentation + 1, this, parentTreeData, this)

                    treeNodes.add(taskWrapper.initialize(treeNode, expandedParentKeys))

                    taskWrappers.add(taskWrapper)
                }

                treeNode.setChildTreeNodes(treeNodes)

                return treeNode
            }

            override val name get() = NameData(parentTreeData.name)

            override val details: Pair<String, Int>?
                get() = if (parentTreeData.scheduleText.isNullOrEmpty()) {
                    null
                } else {
                    Pair(parentTreeData.scheduleText, colorSecondary)
                }

            override val children: Pair<String, Int>?
                get() {
                    val text = treeNode.takeIf { !it.isExpanded }
                            ?.allChildren
                            ?.filter { it.modelNode is TaskAdapter.TaskWrapper && it.canBeShown() }
                            ?.map { it.modelNode as TaskAdapter.TaskWrapper }
                            ?.takeIf { it.isNotEmpty() }
                            ?.joinToString(", ") { it.parentTreeData.name }
                            ?: parentTreeData.note.takeIf { !it.isNullOrEmpty() }

                    return text?.let { Pair(it, colorSecondary) }
                }

            override fun onClick(holder: NodeHolder) {
                val parentPickerFragment = parentFragment

                parentPickerFragment.dismiss()

                parentPickerFragment.listener.onTaskSelected(parentTreeData)
            }

            override fun compareTo(other: ModelNode<NodeHolder>): Int {
                var comparison = parentTreeData.sortKey.compareTo((other as TaskWrapper).parentTreeData.sortKey)
                if (indentation == 0)
                    comparison = -comparison

                return comparison
            }

            override fun normalize() = parentTreeData.normalize()

            override fun matches(filterCriteria: Any?) = parentTreeData.matchesQuery(filterCriteria as? String)

            override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = false
        }
    }

    private interface TaskParent {

        val taskAdapter: TaskAdapter
    }

    interface Listener {

        fun onTaskSelected(parentTreeData: EditViewModel.ParentTreeData)

        fun onTaskDeleted()

        fun onNewParent(nameHint: String?)
    }
}
