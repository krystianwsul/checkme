package com.krystianwsul.checkme.gui.customtimes


import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
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
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.ShowCustomTimesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxkotlin.plusAssign
import java.util.*

class ShowCustomTimesFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SELECTED_CUSTOM_TIME_IDS_KEY = "selectedCustomTimeIds"

        fun newInstance() = ShowCustomTimesFragment()
    }

    private lateinit var showTimesList: RecyclerView
    private var customTimesAdapter: CustomTimesAdapter? = null
    private lateinit var emptyText: TextView

    private var selectedCustomTimeIds: List<Int>? = null

    private val selectionCallback = object : SelectionCallback(null) {

        override fun unselect(x: TreeViewAdapter.Placeholder) = customTimesAdapter!!.unselect()

        override fun onMenuClick(menuItem: MenuItem, x: TreeViewAdapter.Placeholder) {
            val customTimeIds = customTimesAdapter!!.selected
            check(!customTimeIds.isEmpty())

            when (menuItem.itemId) {
                R.id.action_custom_times_delete -> {
                    customTimesAdapter!!.removeSelected()

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

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder, action: () -> Unit) {
            action()

            updateFabVisibility()

            (activity as CustomTimesListListener).onDestroyCustomTimesActionMode()
        }

        override fun onSecondToLastRemoved() = Unit

        override fun onOtherRemoved() = Unit
    }

    private var showTimesFab: FloatingActionButton? = null

    private var data: ShowCustomTimesViewModel.Data? = null

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

        customTimesAdapter?.let {
            val selectedCustomTimeIds = it.selected
            this.selectedCustomTimeIds = if (selectedCustomTimeIds.isEmpty()) null else selectedCustomTimeIds
        }

        customTimesAdapter = CustomTimesAdapter(data, this, selectedCustomTimeIds)
        showTimesList.adapter = customTimesAdapter

        selectionCallback.setSelected(customTimesAdapter!!.selected.size, TreeViewAdapter.Placeholder)

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        customTimesAdapter?.let {
            val selectedCustomTimeIds = it.selected
            if (!selectedCustomTimeIds.isEmpty())
                outState.putIntegerArrayList(SELECTED_CUSTOM_TIME_IDS_KEY, ArrayList(selectedCustomTimeIds))
        }
    }

    private fun updateSelectAll() {
        check(customTimesAdapter != null)

        (activity as CustomTimesListListener).setCustomTimesSelectAllVisibility(customTimesAdapter!!.itemCount != 0)
    }

    fun selectAll() {
        customTimesAdapter!!.selectAll()
    }

    override fun setFab(floatingActionButton: FloatingActionButton) {
        showTimesFab = floatingActionButton

        showTimesFab!!.setOnClickListener { startActivity(ShowCustomTimeActivity.getCreateIntent(activity!!)) }

        updateFabVisibility()
    }

    private fun updateFabVisibility() {
        showTimesFab?.let {
            if (data != null && !selectionCallback.hasActionMode) {
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

    private inner class CustomTimesAdapter(data: ShowCustomTimesViewModel.Data, private val showCustomTimesFragment: ShowCustomTimesFragment, selectedCustomTimeIds: List<Int>?) : RecyclerView.Adapter<CustomTimesAdapter.CustomTimeHolder>() {

        private val dataId = data.dataId

        val customTimeWrappers = data.entries
                .asSequence()
                .map { CustomTimeWrapper(it, selectedCustomTimeIds) }
                .toMutableList()

        val selected
            get() = customTimeWrappers.asSequence()
                    .filter { it.selected }
                    .map { it.customTimeData.id }
                    .toList()

        override fun getItemCount() = customTimeWrappers.size

        fun unselect() {
            customTimeWrappers.filter { it.selected }.forEach {
                it.selected = false
                notifyItemChanged(customTimeWrappers.indexOf(it))
            }
        }

        fun selectAll() {
            check(!selectionCallback.hasActionMode)

            customTimeWrappers.filterNot { it.selected }.forEach { it.toggleSelect(TreeViewAdapter.Placeholder) }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomTimeHolder {
            val layoutInflater = LayoutInflater.from(showCustomTimesFragment.activity)
            val showCustomTimesRow = layoutInflater.inflate(R.layout.row_show_custom_times, parent, false)

            val timesRowName = showCustomTimesRow.findViewById<TextView>(R.id.times_row_name)

            return CustomTimeHolder(showCustomTimesRow, timesRowName)
        }

        override fun onBindViewHolder(customTimeHolder: CustomTimeHolder, position: Int) {
            customTimeHolder.run {
                val customTimeWrapper = customTimeWrappers[position]

                timesRowName.text = customTimeWrapper.customTimeData.name

                showCustomTimeRow.run {
                    if (customTimeWrapper.selected)
                        setBackgroundColor(ContextCompat.getColor(showCustomTimesFragment.activity!!, R.color.selected))
                    else
                        setBackgroundColor(Color.TRANSPARENT)

                    setOnLongClickListener {
                        onLongClick()
                        true
                    }

                    setOnClickListener {
                        if (showCustomTimesFragment.selectionCallback.hasActionMode)
                            onLongClick()
                        else
                            onRowClick()
                    }
                }
            }
        }

        fun removeSelected() {
            val selectedCustomTimeWrappers = customTimeWrappers.filter { it.selected }

            selectedCustomTimeWrappers.map { customTimeWrappers.indexOf(it) }.forEach {
                customTimeWrappers.removeAt(it)
                notifyItemRemoved(it)

            }

            val selectedCustomTimeIds = selectedCustomTimeWrappers.map { it.customTimeData.id }

            DomainFactory.getKotlinDomainFactory().setCustomTimeCurrent(dataId, SaveService.Source.GUI, selectedCustomTimeIds)
        }

        private inner class CustomTimeHolder(val showCustomTimeRow: View, val timesRowName: TextView) : RecyclerView.ViewHolder(showCustomTimeRow) {

            fun onRowClick() {
                val customTimeWrapper = customTimeWrappers[adapterPosition]

                showCustomTimesFragment.activity!!.startActivity(ShowCustomTimeActivity.getEditIntent(customTimeWrapper.customTimeData.id, showCustomTimesFragment.activity!!))
            }

            fun onLongClick() {
                val customTimeWrapper = customTimeWrappers[adapterPosition]

                customTimeWrapper.toggleSelect(TreeViewAdapter.Placeholder)
            }
        }
    }

    private inner class CustomTimeWrapper(val customTimeData: ShowCustomTimesViewModel.CustomTimeData, selectedCustomTimeIds: List<Int>?) {
        var selected = false

        init {
            if (selectedCustomTimeIds != null) {
                check(!selectedCustomTimeIds.isEmpty())

                selected = selectedCustomTimeIds.contains(customTimeData.id)
            }
        }

        fun toggleSelect(x: TreeViewAdapter.Placeholder) {
            selected = !selected

            if (selected) {
                selectionCallback.incrementSelected(x)
            } else {
                selectionCallback.decrementSelected(x)
            }

            val position = customTimesAdapter!!.customTimeWrappers.indexOf(this)
            check(position >= 0)

            customTimesAdapter!!.notifyItemChanged(position)
        }
    }

    interface CustomTimesListListener {

        fun onCreateCustomTimesActionMode(actionMode: ActionMode)
        fun onDestroyCustomTimesActionMode()
        fun setCustomTimesSelectAllVisibility(selectAllVisible: Boolean)
    }
}
