package com.krystianwsul.checkme.gui.edit.dialogs


import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.rxbinding3.view.touches
import com.jakewharton.rxbinding3.widget.textChanges
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentParentPickerBinding
import com.krystianwsul.checkme.databinding.RowListDialogBinding
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import com.krystianwsul.checkme.gui.tree.GroupHolderAdapter
import com.krystianwsul.checkme.gui.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.tree.NodeHolder
import com.krystianwsul.checkme.gui.tree.NodeType
import com.krystianwsul.checkme.gui.tree.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineNameData
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign

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

    private lateinit var searchChanges: Observable<String>

    private var taskDatas: Map<EditViewModel.ParentKey, EditViewModel.ParentTreeData>? = null
    lateinit var listener: Listener

    private var treeViewAdapter: TreeViewAdapter<NodeHolder>? = null
    private var expandedParentKeys: List<EditViewModel.ParentKey>? = null

    private val initializeDisposable = CompositeDisposable()

    private var query: String = ""

    private val bindingProperty = ResettableProperty<FragmentParentPickerBinding>()
    private var binding by bindingProperty

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

        binding = FragmentParentPickerBinding.inflate(layoutInflater)

        searchChanges = binding.parentPickerSearch
                .textChanges()
                .map { it.toString().normalized() }

        return MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.parent_dialog_title)
                .setView(binding.root)
                .setPositiveButton(R.string.add_task) { _, _ ->
                    listener.onNewParent(binding.parentPickerSearch.text?.toString())
                }
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

        if (bindingProperty.isSet) initialize()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.parentPickerRecycler.layoutManager = LinearLayoutManager(activity)

        if (taskDatas != null) initialize()
    }

    private fun initialize() {
        checkNotNull(taskDatas)
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

            binding.parentPickerRecycler.apply {
                adapter = treeViewAdapter
                itemAnimator = CustomItemAnimator()
            }

            if (query.isNotEmpty()) treeViewAdapter!!.updateDisplayedNodes { search(query, it) }
        }

        initializeDisposable += searchChanges.subscribe { query ->
            treeViewAdapter!!.updateDisplayedNodes { search(query, it) }
        }
    }

    override fun onStart() {
        super.onStart()

        binding.parentPickerSearch.apply {
            startDisposable += searchChanges.subscribe {
                val drawable = if (it.isNullOrEmpty()) R.drawable.ic_close_white_24dp else R.drawable.ic_close_black_24dp
                setCompoundDrawablesWithIntrinsicBounds(0, 0, drawable, 0)
            }

            startDisposable += touches {
                if (it.x >= width - totalPaddingEnd) {
                    if (it.action == MotionEvent.ACTION_UP) text?.clear()

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

        bindingProperty.reset()

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
                expandedParentKeys: List<EditViewModel.ParentKey>?,
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = DialogNodeHolder(RowListDialogBinding.inflate(layoutInflater, parent, false))

        override val hasActionMode = false

        override fun incrementSelected(placeholder: TreeViewAdapter.Placeholder) = throw UnsupportedOperationException()

        override fun decrementSelected(placeholder: TreeViewAdapter.Placeholder) = throw UnsupportedOperationException()

        private inner class TaskWrapper(
                indentation: Int,
                private val taskParent: TaskParent,
                val parentTreeData: EditViewModel.ParentTreeData,
                override val parentNode: ModelNode<NodeHolder>?,
        ) : GroupHolderNode(indentation), TaskParent, MultiLineModelNode<NodeHolder> {

            override lateinit var treeNode: TreeNode<NodeHolder>
                private set

            override val nodeType = NodeType.PARENT_PICKER_TASK

            override val ripple = true

            override val id = parentTreeData.parentKey

            private lateinit var taskWrappers: MutableList<TaskWrapper>

            override val taskAdapter get() = taskParent.taskAdapter

            private val parentFragment get() = taskAdapter.parentPickerFragment

            override val colorBackground get() = taskBackground

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

            override val delegates by lazy {
                listOf(
                        ExpandableDelegate(treeNode),
                        MultiLineDelegate(this)
                )
            }

            override val widthKey
                get() = MultiLineDelegate.WidthKey(
                        indentation,
                        checkBoxState.visibility == View.GONE,
                        hasAvatar,
                        thumbnail != null
                )

            fun initialize(
                    nodeContainer: NodeContainer<NodeHolder>,
                    expandedParentKeys: List<EditViewModel.ParentKey>?,
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

            override val name get() = MultiLineNameData.Visible(parentTreeData.name)

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
