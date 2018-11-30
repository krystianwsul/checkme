package com.krystianwsul.checkme.gui.customtimes


import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.AbstractFragment
import com.krystianwsul.checkme.gui.FabUser
import com.krystianwsul.checkme.gui.SelectionCallback
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.persistencemodel.SaveService
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

    private var selectedCustomTimeIds: List<Int>? = null

    private val selectionCallback = object : SelectionCallback() {

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override fun onMenuClick(menuItem: MenuItem, x: TreeViewAdapter.Placeholder) {
            val customTimeIds = selectedIds
            check(!customTimeIds.isEmpty())

            when (menuItem.itemId) {
                R.id.action_custom_times_delete -> {
                    (treeViewAdapter.treeModelAdapter as CustomTimesAdapter).removeSelected(x)

                    updateSelectAll()
                }
                else -> throw UnsupportedOperationException()
            }
        }

        override fun onFirstAdded(x: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            actionMode!!.menuInflater.inflate(R.menu.menu_custom_times, actionMode!!.menu)

            updateFabVisibility()

            (activity as CustomTimesListListener).onCreateCustomTimesActionMode(actionMode!!)
        }

        override fun onSecondAdded() = Unit

        override fun onOtherAdded() = Unit

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) {
            updateFabVisibility()

            (activity as CustomTimesListListener).onDestroyCustomTimesActionMode()
        }

        override fun onSecondToLastRemoved() = Unit

        override fun onOtherRemoved() = Unit
    }

    private var showTimesFab: FloatingActionButton? = null

    private lateinit var data: ShowCustomTimesViewModel.Data

    private lateinit var showCustomTimesViewModel: ShowCustomTimesViewModel

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
            selectedCustomTimeIds = savedInstanceState.getIntegerArrayList(SELECTED_CUSTOM_TIME_IDS_KEY)!!
            check(!selectedCustomTimeIds!!.isEmpty())
        }

        showCustomTimesViewModel = getViewModel<ShowCustomTimesViewModel>().apply {
            start()

            viewCreatedDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    private fun onLoadFinished(data: ShowCustomTimesViewModel.Data) {
        this.data = data

        if (this::treeViewAdapter.isInitialized)
            selectedCustomTimeIds = treeViewAdapter.selectedNodes
                    .asSequence()
                    .map { (it.modelNode as CustomTimeNode).customTimeData.id }
                    .toList()
                    .takeIf { it.isNotEmpty() }

        treeViewAdapter = CustomTimesAdapter().initialize()
        showTimesList.adapter = treeViewAdapter

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
                outState.putIntegerArrayList(SELECTED_CUSTOM_TIME_IDS_KEY, ArrayList(selectedCustomTimeIds))
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
        showTimesFab?.setOnClickListener(null)
        showTimesFab = null
    }

    private inner class CustomTimesAdapter : TreeModelAdapter {

        private val dataId = data.dataId

        val customTimeWrappers = data.entries
                .asSequence()
                .map { CustomTimeNode(it) }
                .toMutableList()

        private lateinit var treeViewAdapter: TreeViewAdapter
        private lateinit var treeNodeCollection: TreeNodeCollection

        fun initialize(): TreeViewAdapter {
            treeViewAdapter = TreeViewAdapter(this)
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)
            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            treeNodeCollection.nodes = customTimeWrappers.map { it.initialize(treeNodeCollection) }

            return treeViewAdapter
        }

        override val hasActionMode get() = selectionCallback.hasActionMode

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = selectionCallback.incrementSelected(x)

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = selectionCallback.decrementSelected(x)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(layoutInflater.inflate(R.layout.row_list, parent, false)!!)

        fun removeSelected(@Suppress("UNUSED_PARAMETER") x: TreeViewAdapter.Placeholder) {
            val selectedCustomTimeWrappers = customTimeWrappers.filter { it.treeNode.isSelected }

            selectedCustomTimeWrappers.map { customTimeWrappers.indexOf(it) }.forEach { customTimeWrappers.removeAt(it) }

            val selectedCustomTimeIds = selectedCustomTimeWrappers.map { it.customTimeData.id }

            DomainFactory.getKotlinDomainFactory().setCustomTimeCurrent(dataId, SaveService.Source.GUI, selectedCustomTimeIds)
        }
    }

    private inner class CustomTimeNode(val customTimeData: ShowCustomTimesViewModel.CustomTimeData) : GroupHolderNode(0) {

        public override lateinit var treeNode: TreeNode
            private set

        override val id = customTimeData.id

        fun initialize(treeNodeCollection: TreeNodeCollection): TreeNode {
            treeNode = TreeNode(this, treeNodeCollection, false, selectedCustomTimeIds?.contains(customTimeData.id)
                    ?: false)
            treeNode.setChildTreeNodes(listOf())
            return treeNode
        }

        override val name get() = Triple(customTimeData.name, colorPrimary, true)

        override val backgroundColor get() = if (treeNode.isSelected) colorSelected else Color.TRANSPARENT

        override val isSelectable = true

        override val isSeparatorVisibleWhenNotExpanded = false

        override val isVisibleDuringActionMode = true

        override val isVisibleWhenEmpty = true

        override fun onClick() = requireActivity().startActivity(ShowCustomTimeActivity.getEditIntent(customTimeData.id, requireActivity()))

        override fun onLongClickListener(viewHolder: RecyclerView.ViewHolder) = treeNode.onLongClickListener()

        override fun compareTo(other: ModelNode) = customTimeData.id.compareTo((other as CustomTimeNode).customTimeData.id)
    }

    interface CustomTimesListListener {

        fun onCreateCustomTimesActionMode(actionMode: ActionMode)
        fun onDestroyCustomTimesActionMode()
        fun setCustomTimesSelectAllVisibility(selectAllVisible: Boolean)
    }
}
