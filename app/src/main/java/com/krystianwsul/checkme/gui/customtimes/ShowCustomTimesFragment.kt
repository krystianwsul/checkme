package com.krystianwsul.checkme.gui.customtimes


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.CustomItemAnimator
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentShowCustomTimesBinding
import com.krystianwsul.checkme.domainmodel.extensions.setCustomTimesCurrent
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.gui.base.ActionModeListener
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.checkme.gui.main.FabUser
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.AbstractModelNode
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.HolderType
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineRow
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.gui.utils.SelectionCallback
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.ShowCustomTimesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.treeadapter.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign
import java.util.*

class ShowCustomTimesFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SELECTED_CUSTOM_TIME_IDS_KEY = "selectedCustomTimeIds"

        fun newInstance() = ShowCustomTimesFragment()
    }

    lateinit var treeViewAdapter: TreeViewAdapter<AbstractHolder>
        private set

    private var selectedCustomTimeKeys: List<CustomTimeKey>? = null

    private val listener get() = activity as CustomTimesListListener

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(placeholder)

        override val bottomBarData by lazy { Triple(listener.getBottomBar(), R.menu.menu_custom_times, listener::initBottomBar) }

        override fun onMenuClick(itemId: Int, placeholder: TreeViewAdapter.Placeholder): Boolean {
            val customTimeIds = selectedIds
            check(customTimeIds.isNotEmpty())

            when (itemId) {
                R.id.action_custom_times_delete -> {
                    val selectedCustomTimeKeys =
                            (treeViewAdapter.treeModelAdapter as CustomTimesAdapter).customTimeWrappers
                                    .filter { it.treeNode.isSelected }
                                    .map { it.customTimeData.id }

                    fun setAreCurrent(current: Boolean) = AndroidDomainUpdater.setCustomTimesCurrent(
                            showCustomTimesViewModel.dataId.toFirst(),
                            selectedCustomTimeKeys,
                            current,
                    )

                    setAreCurrent(false).observeOn(AndroidSchedulers.mainThread())
                            .andThen(customTimesListListener.showSnackbarRemovedMaybe(selectedCustomTimeKeys.size))
                            .flatMapCompletable { setAreCurrent(true) }
                            .subscribe()
                            .addTo(createDisposable)
                }
                else -> throw UnsupportedOperationException()
            }

            return true
        }

        override fun onFirstAdded(placeholder: TreeViewAdapter.Placeholder, initial: Boolean) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            updateFabVisibility()

            (activity as CustomTimesListListener).onCreateActionMode(actionMode!!)

            super.onFirstAdded(placeholder, initial)
        }

        override fun onLastRemoved(placeholder: TreeViewAdapter.Placeholder) {
            updateFabVisibility()

            (activity as CustomTimesListListener).onDestroyActionMode()
        }
    }

    private var showTimesFabDelegate: BottomFabMenuDelegate.FabDelegate? = null

    private lateinit var data: ShowCustomTimesViewModel.Data

    private val isVisible = BehaviorRelay.createDefault(false)
    private lateinit var showCustomTimesViewModel: ShowCustomTimesViewModel

    private val customTimesListListener get() = activity as CustomTimesListListener

    private val bindingProperty = ResettableProperty<FragmentShowCustomTimesBinding>()
    private var binding by bindingProperty

    override fun onAttach(context: Context) {
        super.onAttach(context)

        check(context is CustomTimesListListener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = FragmentShowCustomTimesBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState?.containsKey(SELECTED_CUSTOM_TIME_IDS_KEY) == true) {
            selectedCustomTimeKeys = savedInstanceState.getParcelableArrayList(SELECTED_CUSTOM_TIME_IDS_KEY)!!
            check(selectedCustomTimeKeys!!.isNotEmpty())
        }

        showCustomTimesViewModel = getViewModel<ShowCustomTimesViewModel>().apply {
            viewCreatedDisposable += isVisible.subscribe { if (it) start() else stop() }

            viewCreatedDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    private fun onLoadFinished(data: ShowCustomTimesViewModel.Data) {
        this.data = data

        if (this::treeViewAdapter.isInitialized) {
            selectedCustomTimeKeys = treeViewAdapter.selectedNodes
                    .asSequence()
                    .map { (it.modelNode as CustomTimeNode).customTimeData.id }
                    .toList()
                    .takeIf { it.isNotEmpty() }

            treeViewAdapter.updateDisplayedNodes {
                (treeViewAdapter.treeModelAdapter as CustomTimesAdapter).initialize()

                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it, false)
            }
        } else {
            val customTimesAdapter = CustomTimesAdapter()
            customTimesAdapter.initialize()
            treeViewAdapter = customTimesAdapter.treeViewAdapter

            binding.showTimesList.apply {
                adapter = treeViewAdapter
                itemAnimator = CustomItemAnimator()
            }

            treeViewAdapter.updateDisplayedNodes {
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it, true)
            }
        }

        val (show, hide) = if (data.entries.isEmpty()) {
            binding.showCustomTimesEmptyTextInclude
                    .emptyText
                    .setText(R.string.custom_times_empty)

            binding.showCustomTimesEmptyTextInclude.emptyTextLayout to binding.showTimesList
        } else {
            binding.showTimesList to binding.showCustomTimesEmptyTextInclude.emptyTextLayout
        }

        animateVisibility(show, hide, immediate = data.immediate)

        updateSelectAll()
        updateFabVisibility()
    }

    private val selectedIds
        get() = treeViewAdapter.selectedNodes
                .asSequence()
                .map { (it.modelNode as CustomTimeNode).customTimeData.id }
                .toSet()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (this::treeViewAdapter.isInitialized) {
            selectedIds.takeIf { it.isNotEmpty() }?.let {
                outState.putParcelableArrayList(SELECTED_CUSTOM_TIME_IDS_KEY, ArrayList(it))
            }
        }
    }

    private fun updateSelectAll() =
        (activity as CustomTimesListListener).setCustomTimesSelectAllVisibility(treeViewAdapter.displayedNodes.isNotEmpty())

    override fun setFab(fabDelegate: BottomFabMenuDelegate.FabDelegate) {
        showTimesFabDelegate = fabDelegate

        showTimesFabDelegate!!.setOnClickListener { startActivity(ShowCustomTimeActivity.getCreateIntent(requireActivity())) }

        updateFabVisibility()

        isVisible.accept(true)
    }

    private fun updateFabVisibility() {
        showTimesFabDelegate?.let {
            if (this::data.isInitialized && !selectionCallback.hasActionMode) it.show() else it.hide()
        }
    }

    override fun clearFab() {
        isVisible.accept(false)

        showTimesFabDelegate = null
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    private inner class CustomTimesAdapter : BaseAdapter(), ActionModeCallback by selectionCallback {

        lateinit var customTimeWrappers: MutableList<CustomTimeNode>
            private set

        val treeViewAdapter = TreeViewAdapter(
                this,
                TreeViewAdapter.PaddingData(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
        )

        override lateinit var treeNodeCollection: TreeNodeCollection<AbstractHolder>
            private set

        fun initialize() {
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)
            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            customTimeWrappers = data.entries
                    .asSequence()
                    .map { CustomTimeNode(it) }
                    .toMutableList()

            treeNodeCollection.nodes = customTimeWrappers.map { it.initialize(treeNodeCollection) }
        }
    }

    private inner class CustomTimeNode(val customTimeData: ShowCustomTimesViewModel.CustomTimeData) :
            AbstractModelNode(),
            MultiLineModelNode {

        public override lateinit var treeNode: TreeNode<AbstractHolder>
            private set

        override val holderType = HolderType.MULTILINE

        override val id = customTimeData.id

        fun initialize(treeNodeCollection: TreeNodeCollection<AbstractHolder>) = TreeNode(
            this,
            treeNodeCollection,
            selectedCustomTimeKeys?.contains(customTimeData.id) ?: false,
        ).also {
            treeNode = it
            it.setChildTreeNodes(listOf())
        }

        override val rowsDelegate = object : MultiLineModelNode.RowsDelegate {

            private val name = MultiLineRow.Visible(customTimeData.name)
            private val details = MultiLineRow.Visible(customTimeData.details, R.color.textSecondary)

            override fun getRows(isExpanded: Boolean, allChildren: List<TreeNode<*>>) = listOf(name, details)
        }

        override val isSelectable = true

        override val parentNode: ModelNode<AbstractHolder>? = null

        override val delegates by lazy { listOf(MultiLineDelegate(this)) }

        override val widthKey
            get() = MultiLineDelegate.WidthKey(
                    0,
                    false,
                    false,
                    false,
            )

        override fun onClick(holder: AbstractHolder) = startActivity(ShowCustomTimeActivity.getEditIntent(customTimeData.id, requireActivity()))

        private val customTimeId get() = customTimeData.id.customTimeId.value

        override fun compareTo(other: ModelNode<AbstractHolder>) =
                customTimeId.compareTo((other as CustomTimeNode).customTimeId)
    }

    interface CustomTimesListListener : ActionModeListener, SnackbarListener {

        fun setCustomTimesSelectAllVisibility(selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()
    }
}
