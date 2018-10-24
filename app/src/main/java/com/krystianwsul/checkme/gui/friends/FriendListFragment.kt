package com.krystianwsul.checkme.gui.friends


import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.gui.AbstractFragment
import com.krystianwsul.checkme.gui.FabUser
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.SelectionCallback
import com.krystianwsul.checkme.viewmodels.FriendListViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.row_friend.view.*
import java.util.*

class FriendListFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SELECTED_IDS_KEY = "selectedIds"

        fun newInstance() = FriendListFragment()
    }

    private lateinit var friendListProgress: ProgressBar
    private lateinit var friendListRecycler: RecyclerView
    private lateinit var emptyText: TextView

    private var friendListAdapter: FriendListAdapter? = null

    private var data: FriendListViewModel.Data? = null

    private var selectedIds: List<String>? = null

    private val selectionCallback = object : SelectionCallback() {

        override fun unselect() {
            friendListAdapter!!.unselect()
        }

        override fun onMenuClick(menuItem: MenuItem) {
            val selectedUserDataEmails = friendListAdapter!!.selected
            check(!selectedUserDataEmails.isEmpty())

            when (menuItem.itemId) {
                R.id.action_custom_times_delete -> {
                    friendListAdapter!!.removeSelected()

                    updateSelectAll()
                }
                else -> throw UnsupportedOperationException()
            }
        }

        override fun onFirstAdded() {
            (activity as AppCompatActivity).startSupportActionMode(this)

            actionMode!!.menuInflater.inflate(R.menu.menu_custom_times, actionMode!!.menu)

            updateFabVisibility()

            (activity as MainActivity).onCreateUserActionMode(actionMode!!)
        }

        override fun onSecondAdded() = Unit

        override fun onOtherAdded() = Unit

        override fun onLastRemoved(action: () -> Unit) {
            action()

            updateFabVisibility()

            (activity as MainActivity).onDestroyUserActionMode()
        }

        override fun onSecondToLastRemoved() = Unit

        override fun onOtherRemoved() = Unit
    }

    private var friendListFab: FloatingActionButton? = null

    private lateinit var friendListViewModel: FriendListViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_friend_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        friendListProgress = view.findViewById<View>(R.id.friendListProgress) as ProgressBar

        friendListRecycler = view.findViewById<View>(R.id.friendListRecycler) as RecyclerView

        friendListRecycler.layoutManager = LinearLayoutManager(activity)

        emptyText = view.findViewById<View>(R.id.emptyText) as TextView

        if (savedInstanceState?.containsKey(SELECTED_IDS_KEY) == true)
            selectedIds = savedInstanceState.getStringArrayList(SELECTED_IDS_KEY)!!

        friendListViewModel = getViewModel<FriendListViewModel>().apply {
            start()

            viewCreatedDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    private fun onLoadFinished(data: FriendListViewModel.Data) {
        this.data = data

        selectedIds = friendListAdapter?.selected ?: selectedIds ?: ArrayList()

        friendListAdapter = FriendListAdapter(data.userListDatas, selectedIds!!)
        friendListRecycler.adapter = friendListAdapter

        selectionCallback.setSelected(friendListAdapter!!.selected.size)

        friendListProgress.visibility = View.GONE

        updateFabVisibility()

        if (data.userListDatas.isEmpty()) {
            friendListRecycler.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
            emptyText.setText(R.string.friends_empty)
        } else {
            friendListRecycler.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
        }

        updateSelectAll()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        friendListAdapter?.let { outState.putStringArrayList(SELECTED_IDS_KEY, ArrayList(it.selected)) }
    }

    private fun updateSelectAll() {
        checkNotNull(friendListAdapter)

        (activity as MainActivity).setUserSelectAllVisibility(friendListAdapter!!.itemCount != 0)
    }

    fun selectAll() {
        friendListAdapter!!.selectAll()
    }

    override fun setFab(floatingActionButton: FloatingActionButton) {
        friendListFab = floatingActionButton

        friendListFab!!.setOnClickListener { startActivity(FindFriendActivity.newIntent(activity!!)) }

        updateFabVisibility()
    }

    private fun updateFabVisibility() {
        friendListFab?.run {
            if (data != null && !selectionCallback.hasActionMode) {
                show()
            } else {
                hide()
            }
        }
    }

    override fun clearFab() {
        friendListFab?.setOnClickListener(null)

        friendListFab = null
    }

    private inner class FriendListAdapter(userListDatas: Collection<FriendListViewModel.UserListData>, selectedIds: List<String>) : RecyclerView.Adapter<FriendListAdapter.FriendHolder>() {

        val userDataWrappers = userListDatas.sortedBy { it.id }
                .map { userListData -> UserDataWrapper(userListData, selectedIds) }
                .toMutableList()

        val selected
            get() = userDataWrappers.asSequence()
                    .filter { it.selected }
                    .map { it.userListData.id }
                    .toList()

        override fun getItemCount() = userDataWrappers.size

        fun unselect() {
            userDataWrappers.filter { it.selected }.forEach {
                it.selected = false
                notifyItemChanged(userDataWrappers.indexOf(it))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FriendHolder(activity!!.layoutInflater.inflate(R.layout.row_friend, parent, false))

        override fun onBindViewHolder(friendHolder: FriendHolder, position: Int) {
            val userDataWrapper = userDataWrappers[position]

            friendHolder.run {
                friendName.text = userDataWrapper.userListData.name
                friendEmail.text = userDataWrapper.userListData.email

                itemView.run {
                    setBackgroundColor(if (userDataWrapper.selected)
                        ContextCompat.getColor(activity!!, R.color.selected)
                    else
                        Color.TRANSPARENT)

                    setOnLongClickListener {
                        onLongClick()
                        true
                    }

                    setOnClickListener {
                        if (selectionCallback.hasActionMode)
                            onLongClick()
                    }
                }
            }
        }

        fun removeSelected() {
            val selectedUserDataWrappers = userDataWrappers.filter { it.selected }
            selectedUserDataWrappers.map { userDataWrappers.indexOf(it) }.forEach {
                userDataWrappers.removeAt(it)
                notifyItemRemoved(it)
            }

            KotlinDomainFactory.getKotlinDomainFactory().domainFactory.removeFriends(selectedUserDataWrappers.asSequence()
                    .map { it.userListData.id }
                    .toSet())
        }

        fun selectAll() {
            check(!selectionCallback.hasActionMode)

            userDataWrappers.filterNot { it.selected }.forEach { it.toggleSelect() }
        }

        private inner class FriendHolder(view: View) : RecyclerView.ViewHolder(view) {

            val friendName = itemView.friendName!!
            val friendEmail = itemView.friendEmail!!

            fun onLongClick() {
                userDataWrappers[adapterPosition].toggleSelect()
            }
        }
    }

    private inner class UserDataWrapper(val userListData: FriendListViewModel.UserListData, selectedIds: List<String>) {

        var selected = selectedIds.contains(userListData.id)

        fun toggleSelect() {
            selected = !selected

            if (selected) {
                selectionCallback.incrementSelected()
            } else {
                selectionCallback.decrementSelected()
            }

            val position = friendListAdapter!!.userDataWrappers.indexOf(this)
            check(position >= 0)

            friendListAdapter!!.notifyItemChanged(position)
        }
    }
}
