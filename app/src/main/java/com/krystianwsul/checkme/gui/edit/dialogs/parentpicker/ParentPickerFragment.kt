package com.krystianwsul.checkme.gui.edit.dialogs.parentpicker


import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.view.MotionEvent
import androidx.recyclerview.widget.CustomItemAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jakewharton.rxbinding4.view.touches
import com.jakewharton.rxbinding4.widget.textChanges
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentParentPickerBinding
import com.krystianwsul.checkme.gui.base.AbstractDialogFragment
import com.krystianwsul.checkme.gui.tree.*
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineRow
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.gui.utils.flatten
import com.krystianwsul.checkme.utils.getMap
import com.krystianwsul.checkme.utils.putMap
import com.krystianwsul.common.utils.filterValuesNotNull
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.*
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.cast
import io.reactivex.rxjava3.kotlin.plusAssign

class ParentPickerFragment : AbstractDialogFragment() {

    companion object {

        private const val EXPANDED_TASK_KEYS_KEY = "expandedTaskKeys"

        private const val SHOW_DELETE_KEY = "showDelete"
        private const val KEY_SHOW_ADD = "showAdd"
        private const val KEY_SEARCH = "search"

        fun newInstance(
            showDelete: Boolean,
            showAdd: Boolean,
        ) = ParentPickerFragment().apply {
            arguments = Bundle().apply {
                putBoolean(SHOW_DELETE_KEY, showDelete)
                putBoolean(KEY_SHOW_ADD, showAdd)
            }
        }
    }

    private lateinit var searchChanges: BehaviorRelay<String>

    private val delegateRelay = BehaviorRelay.create<Delegate>()

    private val treeViewAdapterRelay = BehaviorRelay.create<TreeViewAdapter<AbstractHolder>>()
    private val treeViewAdapterSingle = treeViewAdapterRelay.firstOrError()
    private val treeViewAdapter get() = treeViewAdapterRelay.value

    private var expansionStates: Map<Parcelable, TreeNode.ExpansionState>? = null

    private val bindingProperty = ResettableProperty<FragmentParentPickerBinding>()
    private var binding by bindingProperty

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        searchChanges = savedInstanceState?.getString(KEY_SEARCH)
            ?.let { BehaviorRelay.createDefault(it) }
            ?: BehaviorRelay.create()

        Observables.combineLatest(startedRelay, delegateRelay)
            .subscribe { (started, delegate) -> delegate.startedRelay.accept(started) }
            .addTo(createDisposable)
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.apply {
            if (containsKey(EXPANDED_TASK_KEYS_KEY)) {
                expansionStates = getMap(EXPANDED_TASK_KEYS_KEY)!!
                check(expansionStates!!.isNotEmpty())
            }
        }

        binding = FragmentParentPickerBinding.inflate(layoutInflater)

        binding.parentPickerSearch
            .textChanges()
            .skipInitialValue()
            .map { it.toString().normalized() }
            .distinctUntilChanged()
            .subscribe(searchChanges)
            .addTo(viewCreatedDisposable)

        return MaterialAlertDialogBuilder(requireContext()).setView(binding.root)
            .apply {
                if (requireArguments().getBoolean(KEY_SHOW_ADD)) {
                    setPositiveButton(R.string.add_task) { _, _ ->
                        delegateRelay.value!!.onNewEntry(binding.parentPickerSearch.text?.toString())
                    }
                }

                if (requireArguments().getBoolean(SHOW_DELETE_KEY))
                    setNeutralButton(R.string.delete) { _, _ -> delegateRelay.value!!.onEntryDeleted() }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    fun initialize(delegate: Delegate) = delegateRelay.accept(delegate)

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)

        delegateRelay.switchMap { it.adapterDataObservable }
            .subscribe(::initialize)
            .addTo(viewCreatedDisposable)

        Observables.combineLatest(delegateRelay, searchChanges)
            .subscribe { (delegate, query) -> delegate.onSearch(query) }
            .addTo(viewCreatedDisposable)

        delegateRelay.switchMap { delegate ->
            getProgressShownObservable(binding.parentPickerRecycler, treeViewAdapterSingle.cast()).map { delegate }
        }
            .subscribe { it.onPaddingShown() }
            .addTo(viewCreatedDisposable)
    }

    private fun initialize(adapterData: AdapterData) {
        check(activity != null)

        if (treeViewAdapter != null) {
            val expanded = (treeViewAdapter!!.treeModelAdapter as TaskAdapter).expansionStates.toMap()

            expansionStates = expanded.ifEmpty { null }

            treeViewAdapter!!.updateDisplayedNodes {
                (treeViewAdapter!!.treeModelAdapter as TaskAdapter).initialize(
                    adapterData.entryDatas,
                    expansionStates,
                    adapterData.showProgress,
                )
            }
        } else {
            val taskAdapter = TaskAdapter()
            taskAdapter.initialize(adapterData.entryDatas, expansionStates, adapterData.showProgress)
            treeViewAdapterRelay.accept(taskAdapter.treeViewAdapter)

            binding.parentPickerRecycler.apply {
                adapter = treeViewAdapter
                itemAnimator = CustomItemAnimator()
            }

            delegateRelay.value!!
                .initialScrollMatcher
                ?.let { matcher ->
                    val position = treeViewAdapter!!.getTreeNodeCollection().getPosition(PositionMode.Displayed) {
                        it.modelNode
                            .let { it as? TaskAdapter.TaskWrapper }
                            ?.entryData
                            ?.let(matcher)
                            ?: false
                    }

                    binding.parentPickerRecycler.scrollToPosition(position)
                }
        }
    }

    private val startedRelay = BehaviorRelay.createDefault(false)

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

        startedRelay.accept(true)
    }

    override fun onStop() {
        startedRelay.accept(false)

        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (treeViewAdapter != null) {
            val expansionStates = (treeViewAdapter!!.treeModelAdapter as TaskAdapter).expansionStates.toMap()

            if (expansionStates.isNotEmpty()) outState.putMap(EXPANDED_TASK_KEYS_KEY, expansionStates)
        }

        outState.putString(KEY_SEARCH, searchChanges.value)
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    private inner class TaskAdapter : BaseAdapter(), TaskParent {

        private lateinit var taskWrappers: MutableList<TaskWrapper>

        val treeViewAdapter = TreeViewAdapter(
            this,
            TreeViewAdapter.PaddingData(
                R.layout.row_parent_picker_dialog_padding,
                R.id.paddingProgress,
                true,
            ),
        )

        override val taskAdapter = this

        val expansionStates get() = taskWrappers.map { it.expansionStates }.flatten()

        override lateinit var treeNodeCollection: TreeNodeCollection<AbstractHolder>
            private set

        fun initialize(
            entryDatas: Collection<EntryData>,
            expansionStates: Map<Parcelable, TreeNode.ExpansionState>?,
            showProgress: Boolean,
        ) {
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)
            treeViewAdapter.showProgress = showProgress

            taskWrappers = ArrayList()

            val treeNodes = ArrayList<TreeNode<AbstractHolder>>()

            for (parentTreeData in entryDatas) {
                val taskWrapper = TaskWrapper(0, this, parentTreeData, null)

                treeNodes.add(taskWrapper.initialize(treeNodeCollection, expansionStates))

                taskWrappers.add(taskWrapper)
            }

            treeNodeCollection.nodes = treeNodes
        }

        override val hasActionMode = false

        override fun incrementSelected(placeholder: TreeViewAdapter.Placeholder, initial: Boolean) =
            throw UnsupportedOperationException()

        override fun decrementSelected(placeholder: TreeViewAdapter.Placeholder) = throw UnsupportedOperationException()

        inner class TaskWrapper(
            override val indentation: Int,
            private val taskParent: TaskParent,
            val entryData: EntryData,
            override val parentNode: ModelNode<AbstractHolder>?,
        ) : AbstractModelNode(), TaskParent, MultiLineModelNode, IndentationModelNode, Matchable by entryData {

            override lateinit var treeNode: TreeNode<AbstractHolder>
                private set

            override val holderType = HolderType.DIALOG

            override val id = entryData.entryKey

            private lateinit var taskWrappers: MutableList<TaskWrapper>

            override val taskAdapter get() = taskParent.taskAdapter

            val expansionStates: Map<Parcelable, TreeNode.ExpansionState>
                get() = mapOf(entryData.entryKey to treeNode.getSaveExpansionState()).filterValuesNotNull() +
                        taskWrappers.map { it.expansionStates }.flatten()

            override val delegates by lazy {
                listOf(
                    ExpandableDelegate(treeNode),
                    IndentationDelegate(this),
                    MultiLineDelegate(this), // this one always has to be last, because it depends on layout changes from prev
                )
            }

            override val widthKey
                get() = MultiLineDelegate.WidthKey(
                    indentation,
                    false,
                    false,
                    treeNode.expandVisible,
                    true,
                )

            fun initialize(
                nodeContainer: NodeContainer<AbstractHolder>,
                expansionStates: Map<Parcelable, TreeNode.ExpansionState>?,
            ): TreeNode<AbstractHolder> {
                treeNode = TreeNode(
                    this,
                    nodeContainer,
                    initialExpansionState = expansionStates?.get(entryData.entryKey)
                )

                taskWrappers = ArrayList()

                val treeNodes = ArrayList<TreeNode<AbstractHolder>>()

                for (parentTreeData in entryData.childEntryDatas) {
                    val taskWrapper = TaskWrapper(indentation + 1, this, parentTreeData, this)

                    treeNodes.add(taskWrapper.initialize(treeNode, expansionStates))

                    taskWrappers.add(taskWrapper)
                }

                treeNode.setChildTreeNodes(treeNodes)

                return treeNode
            }

            override val rowsDelegate = object : MultiLineModelNode.RowsDelegate {

                private val name = MultiLineRow.Visible(entryData.name)

                private val details = entryData.details.let {
                    if (it.isNullOrEmpty()) null else MultiLineRow.Visible(it, R.color.textSecondary)
                }

                override fun getRows(isExpanded: Boolean, allChildren: List<TreeNode<*>>): List<MultiLineRow> {
                    val rows = listOfNotNull(name, details).toMutableList()

                    val childrenText = allChildren.takeIf { !isExpanded }
                        ?.filter { it.modelNode is TaskAdapter.TaskWrapper && it.canBeShown() }
                        ?.map { it.modelNode as TaskAdapter.TaskWrapper }
                        ?.takeIf { it.isNotEmpty() }
                        ?.joinToString(", ") { it.entryData.name }
                        ?: entryData.note.takeIf { !it.isNullOrEmpty() }

                    if (childrenText != null) {
                        rows += MultiLineRow.Visible(childrenText, R.color.textSecondary)
                    }

                    return rows
                }
            }

            override fun onClick(holder: AbstractHolder) {
                this@ParentPickerFragment.dismiss()

                delegateRelay.value!!.onEntrySelected(entryData)
            }

            override fun compareTo(other: ModelNode<AbstractHolder>): Int {
                return -entryData.sortKey.compareTo((other as TaskWrapper).entryData.sortKey)
            }
        }
    }

    private interface TaskParent {

        val taskAdapter: TaskAdapter
    }

    interface Delegate {

        val startedRelay: Consumer<Boolean>

        val adapterDataObservable: Observable<AdapterData>

        val initialScrollMatcher: ((EntryData) -> Boolean)?

        fun onEntrySelected(entryData: EntryData)

        fun onEntryDeleted()

        fun onNewEntry(nameHint: String?)

        fun onSearch(query: String)

        fun onPaddingShown()
    }

    data class AdapterData(val entryDatas: Collection<EntryData>, val showProgress: Boolean = false)

    interface EntryData : Matchable {

        val name: String
        val childEntryDatas: Collection<EntryData>
        val entryKey: Parcelable
        val details: String?
        val note: String?
        val sortKey: SortKey
    }

    interface SortKey : Comparable<SortKey>
}
