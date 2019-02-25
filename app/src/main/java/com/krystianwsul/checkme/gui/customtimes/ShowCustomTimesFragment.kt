package com.krystianwsul.checkme.gui.customtimes


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.RemoteCustomTimeId
import com.krystianwsul.checkme.viewmodels.ShowCustomTimesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.*
import io.reactivex.rxkotlin.plusAssign
import java.util.*

class ShowCustomTimesFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SELECTED_CUSTOM_TIME_IDS_KEY = "selectedCustomTimeIds"

        fun newInstance() = ShowCustomTimesFragment()
    }

    private lateinit var showTimesList: RecyclerView

    lateinit var treeViewAdapter: TreeViewAdapter
        private set

    private lateinit var emptyText: TextView

    private var selectedCustomTimeIds: List<RemoteCustomTimeId.Private>? = null

    private val listener get() = activity as CustomTimesListListener

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override val bottomBarData by lazy { Triple(listener.getBottomBar(), R.menu.menu_custom_times, listener::initBottomBar) }

        override fun updateMenu() = Unit

        override fun onMenuClick(itemId: Int, x: TreeViewAdapter.Placeholder) {
            val customTimeIds = selectedIds
            check(!customTimeIds.isEmpty())

            when (itemId) {
                R.id.action_custom_times_delete -> {
                    (treeViewAdapter.treeModelAdapter as CustomTimesAdapter).removeSelected(x)

                    updateSelectAll()
                }
                else -> throw UnsupportedOperationException()
            }
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

        showTimesList = view.findViewById(R.id.show_times_list)
        showTimesList.layoutManager = LinearLayoutManager(activity)

        emptyText = view.findViewById(R.id.emptyText)!!

        if (savedInstanceState?.containsKey(SELECTED_CUSTOM_TIME_IDS_KEY) == true) {
            selectedCustomTimeIds = savedInstanceState.getParcelableArrayList(SELECTED_CUSTOM_TIME_IDS_KEY)!!
            check(!selectedCustomTimeIds!!.isEmpty())
        }

        showCustomTimesViewModel = getViewModel<ShowCustomTimesViewModel>().apply {
            start()

            viewCreatedDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    private fun onLoadFinished(data: ShowCustomTimesViewModel.Data) {
        this.data = data

        if (this::treeViewAdapter.isInitialized) {
            selectedCustomTimeIds = treeViewAdapter.selectedNodes
                    .asSequence()
                    .map { (it.modelNode as CustomTimeNode).customTimeData.id }
                    .toList()
                    .takeIf { it.isNotEmpty() }

            treeViewAdapter.updateDisplayedNodes(true) {
                (treeViewAdapter.treeModelAdapter as CustomTimesAdapter).initialize()
            }
        } else {
            val customTimesAdapter = CustomTimesAdapter()
            customTimesAdapter.initialize()
            treeViewAdapter = customTimesAdapter.treeViewAdapter
            showTimesList.adapter = treeViewAdapter
        }

        selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)

        if (data.entries.isEmpty()) {
            showTimesList.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
            emptyText.setText(R.string.custom_times_empty)
        } else {
            showTimesList.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
        }

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
            if (!selectedCustomTimeIds.isEmpty())
                outState.putParcelableArrayList(SELECTED_CUSTOM_TIME_IDS_KEY, ArrayList(selectedCustomTimeIds))
        }
    }

    private fun updateSelectAll() = (activity as CustomTimesListListener).setCustomTimesSelectAllVisibility(treeViewAdapter.itemCount != 0)

    fun selectAll(x: TreeViewAdapter.Placeholder) = treeViewAdapter.selectAll(x)

    override fun setFab(floatingActionButton: FloatingActionButton) {
        showTimesFab = floatingActionButton

        showTimesFab!!.setOnClickListener { startActivity(ShowCustomTimeActivity.getCreateIntent(activity!!)) }

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

    private inner class CustomTimesAdapter : TreeModelAdapter {

        private val dataId get() = data.dataId

        lateinit var customTimeWrappers: MutableList<CustomTimeNode>
            private set

        val treeViewAdapter = TreeViewAdapter(this, R.layout.row_group_list_fab_padding)
        private lateinit var treeNodeCollection: TreeNodeCollection

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

        fun removeSelected(@Suppress("UNUSED_PARAMETER") x: TreeViewAdapter.Placeholder) {
            val selectedCustomTimeWrappers = customTimeWrappers.filter { it.treeNode.isSelected }
            customTimeWrappers.removeAll(selectedCustomTimeWrappers)

            selectedCustomTimeWrappers.map { it.treeNode }.forEach { treeNodeCollection.remove(it, x) }

            val customTimeDatas = selectedCustomTimeWrappers.map { it.customTimeData }
            data.entries.removeAll(customTimeDatas)

            val selectedCustomTimeIds = customTimeDatas.map { it.id }

            DomainFactory.instance.setCustomTimesCurrent(dataId, SaveService.Source.GUI, selectedCustomTimeIds, false)

            customTimesListListener.showSnackbar(selectedCustomTimeIds.size) {
                onLoadFinished(data.apply {
                    entries.apply {
                        addAll(customTimeDatas)
                        sortBy { it.id }
                    }
                })

                DomainFactory.instance.setCustomTimesCurrent(dataId, SaveService.Source.GUI, selectedCustomTimeIds, true)
            }
        }
    }

    private inner class CustomTimeNode(val customTimeData: ShowCustomTimesViewModel.CustomTimeData) : GroupHolderNode(0) {

        public override lateinit var treeNode: TreeNode
            private set

        override val id = customTimeData.id

        override val ripple = true

        fun initialize(treeNodeCollection: TreeNodeCollection): TreeNode {
            treeNode = TreeNode(this, treeNodeCollection, false, selectedCustomTimeIds?.contains(customTimeData.id)
                    ?: false)
            treeNode.setChildTreeNodes(listOf())
            return treeNode
        }

        override val name get() = Triple(customTimeData.name, colorPrimary, true)

        override val isSelectable = true

        override val isSeparatorVisibleWhenNotExpanded = false

        override val isVisibleDuringActionMode = true

        override val isVisibleWhenEmpty = true

        override fun onClick() = requireActivity().startActivity(ShowCustomTimeActivity.getEditIntent(customTimeData.id, requireActivity()))

        override fun compareTo(other: ModelNode) = customTimeData.id.compareTo((other as CustomTimeNode).customTimeData.id)
    }

    interface CustomTimesListListener : ActionModeListener, SnackbarListener {

        fun setCustomTimesSelectAllVisibility(selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()
    }
}
