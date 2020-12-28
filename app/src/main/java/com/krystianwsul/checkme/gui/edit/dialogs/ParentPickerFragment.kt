package com.krystianwsul.checkme.gui.edit.dialogs


import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.view.MotionEvent
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.rxbinding3.view.touches
import com.jakewharton.rxbinding3.widget.textChanges
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentParentPickerBinding
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.AbstractModelNode
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.HolderType
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineNameData
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.criteria.QueryMatchable
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
            arguments = Bundle().apply { putBoolean(SHOW_DELETE_KEY, showDelete) }
        }
    }

    private lateinit var searchChanges: Observable<String>

    lateinit var delegate: Delegate

    private var treeViewAdapter: TreeViewAdapter<AbstractHolder>? = null
    private var expandedParentKeys: List<Parcelable>? = null

    private val initializeDisposable = CompositeDisposable()

    private var filterCriteria = FilterCriteria.Full()

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

            filterCriteria = getParcelable(QUERY_KEY)!!
        }

        binding = FragmentParentPickerBinding.inflate(layoutInflater)

        searchChanges = binding.parentPickerSearch
                .textChanges()
                .map { it.toString().normalized() }

        return MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.parent_dialog_title)
                .setView(binding.root)
                .setPositiveButton(R.string.add_task) { _, _ ->
                    delegate.onNewParent(binding.parentPickerSearch.text?.toString())
                }
                .setNegativeButton(android.R.string.cancel, null)
                .apply {
                    if (requireArguments().getBoolean(SHOW_DELETE_KEY))
                        setNeutralButton(R.string.delete) { _, _ -> delegate.onTaskDeleted() }
                }
                .create()
    }

    fun initialize(delegate: Delegate) {
        this.delegate = delegate

        if (bindingProperty.isSet) initialize()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.parentPickerRecycler.layoutManager = LinearLayoutManager(activity)

        if (this::delegate.isInitialized) initialize()
    }

    private fun initialize() {
        check(activity != null)

        initializeDisposable.clear()

        if (treeViewAdapter != null) {
            val expanded = (treeViewAdapter!!.treeModelAdapter as TaskAdapter).expandedParentKeys

            expandedParentKeys = if (expanded.isEmpty()) null else expanded

            treeViewAdapter!!.updateDisplayedNodes {
                (treeViewAdapter!!.treeModelAdapter as TaskAdapter).initialize(delegate.entryDatas, expandedParentKeys)
            }
        } else {
            val taskAdapter = TaskAdapter(this)
            taskAdapter.initialize(delegate.entryDatas, expandedParentKeys)
            treeViewAdapter = taskAdapter.treeViewAdapter

            binding.parentPickerRecycler.apply {
                adapter = treeViewAdapter
                itemAnimator = CustomItemAnimator()
            }
        }

        initializeDisposable += searchChanges.subscribe { query ->
            treeViewAdapter!!.updateDisplayedNodes { search(FilterCriteria.Full(query), it) }
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

        outState.putParcelable(QUERY_KEY, filterCriteria)
    }

    override fun onDestroyView() {
        initializeDisposable.clear()

        bindingProperty.reset()

        super.onDestroyView()
    }

    private fun search(filterCriteria: FilterCriteria.Full, placeholder: TreeViewAdapter.Placeholder) {
        this.filterCriteria = filterCriteria

        treeViewAdapter!!.setFilterCriteria(filterCriteria, placeholder)
    }

    private inner class TaskAdapter(private val parentPickerFragment: ParentPickerFragment) :
            BaseAdapter(),
            TaskParent {

        private lateinit var taskWrappers: MutableList<TaskWrapper>

        val treeViewAdapter = TreeViewAdapter(
                this,
                null,
                viewCreatedDisposable,
                filterCriteria
        )

        override val taskAdapter = this

        val expandedParentKeys get() = taskWrappers.flatMap { it.expandedParentKeys }

        override lateinit var treeNodeCollection: TreeNodeCollection<AbstractHolder>
            private set

        fun initialize(
                entryDatas: Collection<EntryData>,
                expandedParentKeys: List<Parcelable>?,
        ) {
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            taskWrappers = ArrayList()

            val treeNodes = ArrayList<TreeNode<AbstractHolder>>()

            for (parentTreeData in entryDatas) {
                val taskWrapper = TaskWrapper(0, this, parentTreeData, null)

                treeNodes.add(taskWrapper.initialize(treeNodeCollection, expandedParentKeys))

                taskWrappers.add(taskWrapper)
            }

            treeNodeCollection.nodes = treeNodes
        }

        override val hasActionMode = false

        override fun incrementSelected(placeholder: TreeViewAdapter.Placeholder, initial: Boolean) =
                throw UnsupportedOperationException()

        override fun decrementSelected(placeholder: TreeViewAdapter.Placeholder) = throw UnsupportedOperationException()

        private inner class TaskWrapper(
                override val indentation: Int,
                private val taskParent: TaskParent,
                val entryData: EntryData,
                override val parentNode: ModelNode<AbstractHolder>?,
        ) : AbstractModelNode(), TaskParent, MultiLineModelNode, IndentationModelNode {

            override lateinit var treeNode: TreeNode<AbstractHolder>
                private set

            override val holderType = HolderType.DIALOG

            override val id = entryData.entryKey

            private lateinit var taskWrappers: MutableList<TaskWrapper>

            override val taskAdapter get() = taskParent.taskAdapter

            private val parentFragment get() = taskAdapter.parentPickerFragment

            val expandedParentKeys: List<Parcelable>
                get() {
                    val expandedParentKeys = ArrayList<Parcelable>()

                    val treeNode = this.treeNode

                    if (treeNode.isExpanded) {
                        expandedParentKeys.add(entryData.entryKey)

                        expandedParentKeys.addAll(taskWrappers.flatMap { it.expandedParentKeys })
                    }

                    return expandedParentKeys
                }

            override val delegates by lazy {
                listOf(
                        ExpandableDelegate(treeNode),
                        MultiLineDelegate(this),
                        IndentationDelegate(this)
                )
            }

            override val widthKey
                get() = MultiLineDelegate.WidthKey(
                        indentation,
                        false,
                        false,
                        true,
                        true
                )

            fun initialize(
                    nodeContainer: NodeContainer<AbstractHolder>,
                    expandedParentKeys: List<Parcelable>?,
            ): TreeNode<AbstractHolder> {
                var expanded = false
                if (expandedParentKeys != null) {
                    check(expandedParentKeys.isNotEmpty())
                    expanded = expandedParentKeys.contains(entryData.entryKey)
                }

                treeNode = TreeNode(this, nodeContainer, expanded, false)

                taskWrappers = ArrayList()

                val treeNodes = ArrayList<TreeNode<AbstractHolder>>()

                for (parentTreeData in entryData.childEntryDatas) {
                    val taskWrapper = TaskWrapper(indentation + 1, this, parentTreeData, this)

                    treeNodes.add(taskWrapper.initialize(treeNode, expandedParentKeys))

                    taskWrappers.add(taskWrapper)
                }

                treeNode.setChildTreeNodes(treeNodes)

                return treeNode
            }

            override val name get() = MultiLineNameData.Visible(entryData.name)

            override val details: Pair<String, Int>?
                get() = entryData.details.let {
                    if (it.isNullOrEmpty()) null else Pair(it, R.color.textSecondary)
                }

            override val children: Pair<String, Int>?
                get() {
                    val text = treeNode.takeIf { !it.isExpanded }
                            ?.allChildren
                            ?.filter { it.modelNode is TaskAdapter.TaskWrapper && it.canBeShown() }
                            ?.map { it.modelNode as TaskAdapter.TaskWrapper }
                            ?.takeIf { it.isNotEmpty() }
                            ?.joinToString(", ") { it.entryData.name }
                            ?: entryData.note.takeIf { !it.isNullOrEmpty() }

                    return text?.let { Pair(it, R.color.textSecondary) }
                }

            override fun onClick(holder: AbstractHolder) {
                val parentPickerFragment = parentFragment

                parentPickerFragment.dismiss()

                parentPickerFragment.delegate.onTaskSelected(entryData)
            }

            override fun compareTo(other: ModelNode<AbstractHolder>): Int {
                var comparison = entryData.sortKey.compareTo((other as TaskWrapper).entryData.sortKey)
                if (indentation == 0)
                    comparison = -comparison

                return comparison
            }

            override fun normalize() = entryData.normalize()

            override fun matchesFilterParams(filterParams: FilterCriteria.Full.FilterParams): Boolean {
                check(!filterParams.showDeleted)
                check(filterParams.showAssignedToOthers)

                return true
            }

            override fun getMatchResult(query: String) =
                    ModelNode.MatchResult.fromBoolean(entryData.matchesQuery(query))
        }
    }

    private interface TaskParent {

        val taskAdapter: TaskAdapter
    }

    interface Delegate {

        val entryDatas: Collection<EntryData>

        fun onTaskSelected(entryData: EntryData)

        fun onTaskDeleted()

        fun onNewParent(nameHint: String?)
    }

    interface EntryData : QueryMatchable {

        val name: String
        val childEntryDatas: Collection<EntryData>
        val entryKey: Parcelable
        val details: String?
        val note: String?
        val sortKey: EditViewModel.SortKey

        fun normalize()
    }
}
