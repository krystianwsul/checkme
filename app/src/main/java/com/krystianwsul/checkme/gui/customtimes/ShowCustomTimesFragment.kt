package com.krystianwsul.checkme.gui.customtimes


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderAdapter
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.instances.tree.NameData
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.ShowCustomTimesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.TreeNode
import com.krystianwsul.treeadapter.TreeNodeCollection
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.empty_text.*
import kotlinx.android.synthetic.main.fragment_show_custom_times.*
import java.util.*

class ShowCustomTimesFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SELECTED_CUSTOM_TIME_IDS_KEY = "selectedCustomTimeIds"

        fun newInstance() = ShowCustomTimesFragment()
    }

    lateinit var treeViewAdapter: TreeViewAdapter<NodeHolder>
        private set

    private var selectedCustomTimeKeys: List<CustomTimeKey.Private>? = null

    private val listener get() = activity as CustomTimesListListener

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override val bottomBarData by lazy { Triple(listener.getBottomBar(), R.menu.menu_custom_times, listener::initBottomBar) }

        override fun onMenuClick(itemId: Int, x: TreeViewAdapter.Placeholder): Boolean {
            val customTimeIds = selectedIds
            check(customTimeIds.isNotEmpty())

            when (itemId) {
                R.id.action_custom_times_delete -> {
                    val selectedCustomTimeKeys = (treeViewAdapter.treeModelAdapter as CustomTimesAdapter).customTimeWrappers
                            .filter { it.treeNode.isSelected }
                            .map { it.customTimeData.id }

                    DomainFactory.instance.setCustomTimesCurrent(0, SaveService.Source.GUI, selectedCustomTimeKeys, false)

                    customTimesListListener.showSnackbarRemoved(selectedCustomTimeKeys.size) {
                        DomainFactory.instance.setCustomTimesCurrent(0, SaveService.Source.GUI, selectedCustomTimeKeys, true)
                    }
                }
                else -> throw UnsupportedOperationException()
            }

            return true
        }

        override fun onFirstAdded(x: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            updateFabVisibility()

            (activity as CustomTimesListListener).onCreateActionMode(actionMode!!)

            super.onFirstAdded(x)
        }

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) {
            updateFabVisibility()

            (activity as CustomTimesListListener).onDestroyActionMode()
        }
    }

    private var showTimesFab: FloatingActionButton? = null

    private lateinit var data: ShowCustomTimesViewModel.Data

    private lateinit var showCustomTimesViewModel: ShowCustomTimesViewModel

    private val customTimesListListener get() = activity as CustomTimesListListener

    override fun onAttach(context: Context) {
        super.onAttach(context)

        check(context is CustomTimesListListener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_show_custom_times, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showTimesList.layoutManager = LinearLayoutManager(activity)

        if (savedInstanceState?.containsKey(SELECTED_CUSTOM_TIME_IDS_KEY) == true) {
            selectedCustomTimeKeys = savedInstanceState.getParcelableArrayList(SELECTED_CUSTOM_TIME_IDS_KEY)!!
            check(selectedCustomTimeKeys!!.isNotEmpty())
        }

        showCustomTimesViewModel = getViewModel<ShowCustomTimesViewModel>().apply {
            start()

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
            }
        } else {
            val customTimesAdapter = CustomTimesAdapter()
            customTimesAdapter.initialize()
            treeViewAdapter = customTimesAdapter.treeViewAdapter
            showTimesList.adapter = treeViewAdapter
            showTimesList.itemAnimator = CustomItemAnimator()
        }

        selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)

        val show: View
        val hide: View
        if (data.entries.isEmpty()) {
            show = emptyTextLayout
            hide = showTimesList
            emptyText.setText(R.string.custom_times_empty)
        } else {
            show = showTimesList
            hide = emptyTextLayout
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
            val selectedCustomTimeIds = selectedIds
            if (selectedCustomTimeIds.isNotEmpty())
                outState.putParcelableArrayList(SELECTED_CUSTOM_TIME_IDS_KEY, ArrayList(selectedCustomTimeIds))
        }
    }

    private fun updateSelectAll() = (activity as CustomTimesListListener).setCustomTimesSelectAllVisibility(treeViewAdapter.displayedNodes.isNotEmpty())

    fun selectAll(x: TreeViewAdapter.Placeholder) = treeViewAdapter.selectAll(x)

    override fun setFab(floatingActionButton: FloatingActionButton) {
        showTimesFab = floatingActionButton

        showTimesFab!!.setOnClickListener { startActivity(ShowCustomTimeActivity.getCreateIntent(requireActivity())) }

        updateFabVisibility()
    }

    private fun updateFabVisibility() {
        showTimesFab?.let {
            if (this::data.isInitialized && !selectionCallback.hasActionMode) {
                it.show()
            } else {
                it.hide()
            }
        }
    }

    override fun clearFab() {
        showTimesFab = null
    }

    private inner class CustomTimesAdapter : GroupHolderAdapter() {

        lateinit var customTimeWrappers: MutableList<CustomTimeNode>
            private set

        val treeViewAdapter = TreeViewAdapter(this, Pair(R.layout.row_group_list_fab_padding, R.id.paddingProgress))

        override lateinit var treeNodeCollection: TreeNodeCollection<NodeHolder>
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

        override val hasActionMode get() = selectionCallback.hasActionMode

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = selectionCallback.incrementSelected(x)

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = selectionCallback.decrementSelected(x)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(layoutInflater.inflate(R.layout.row_list, parent, false)!!)
    }

    private inner class CustomTimeNode(val customTimeData: ShowCustomTimesViewModel.CustomTimeData) : GroupHolderNode(0) {

        public override lateinit var treeNode: TreeNode<NodeHolder>
            private set

        override val id = customTimeData.id

        override val ripple = true

        fun initialize(treeNodeCollection: TreeNodeCollection<NodeHolder>): TreeNode<NodeHolder> {
            treeNode = TreeNode(this, treeNodeCollection, false, selectedCustomTimeKeys?.contains(customTimeData.id)
                    ?: false)
            treeNode.setChildTreeNodes(listOf())
            return treeNode
        }

        override val name = NameData(customTimeData.name)

        override val details = Pair(customTimeData.details, colorSecondary)

        override val isSelectable = true

        override val isSeparatorVisibleWhenNotExpanded = false

        override val isVisibleDuringActionMode = true

        override val isVisibleWhenEmpty = true

        override fun onClick(holder: NodeHolder) = requireActivity().startActivity(ShowCustomTimeActivity.getEditIntent(customTimeData.id, requireActivity()))

        override fun compareTo(other: ModelNode<NodeHolder>) = customTimeData.id.customTimeId.compareTo((other as CustomTimeNode).customTimeData.id.customTimeId)
    }

    interface CustomTimesListListener : ActionModeListener, SnackbarListener {

        fun setCustomTimesSelectAllVisibility(selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()
    }
}
