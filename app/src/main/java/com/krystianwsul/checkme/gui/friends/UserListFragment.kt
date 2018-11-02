package com.krystianwsul.checkme.gui.friends


import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
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
import android.widget.RelativeLayout
import android.widget.TextView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.gui.AbstractFragment
import com.krystianwsul.checkme.gui.FabUser
import com.krystianwsul.checkme.gui.SelectionCallback
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.viewmodels.ShowProjectViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.row_friend.view.*
import java.util.*

class UserListFragment : AbstractFragment(), FabUser {

    companion object {

        private const val PROJECT_ID_KEY = "projectId"

        private const val SAVE_STATE_KEY = "saveState"

        private const val FRIEND_PICKER_TAG = "friendPicker"

        fun newInstance() = UserListFragment()
    }

    private lateinit var friendListProgress: ProgressBar
    private lateinit var friendListRecycler: RecyclerView
    private lateinit var emptyText: TextView

    private var projectId: String? = null

    private var friendListAdapter: FriendListAdapter? = null

    private var data: ShowProjectViewModel.Data? = null

    private var saveState: SaveState? = null

    private val selectionCallback = object : SelectionCallback() {

        override fun unselect() {
            friendListAdapter!!.unselect()
        }

        override fun onMenuClick(menuItem: MenuItem) {
            val selectedUserDataEmails = friendListAdapter!!.selected
            check(!selectedUserDataEmails.isEmpty())

            when (menuItem.itemId) {
                R.id.action_custom_times_delete -> friendListAdapter!!.removeSelected()
                else -> throw UnsupportedOperationException()
            }
        }

        override fun onFirstAdded() {
            (activity as AppCompatActivity).startSupportActionMode(this)

            actionMode!!.menuInflater.inflate(R.menu.menu_custom_times, actionMode!!.menu)

            updateFabVisibility()
        }

        override fun onSecondAdded() = Unit

        override fun onOtherAdded() = Unit

        override fun onLastRemoved(action: () -> Unit) {
            action()

            updateFabVisibility()
        }

        override fun onSecondToLastRemoved() = Unit

        override fun onOtherRemoved() = Unit
    }

    private var friendListFab: FloatingActionButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            check(it.containsKey(PROJECT_ID_KEY))

            projectId = it.getString(PROJECT_ID_KEY)!!
            check(projectId.isNullOrEmpty())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_friend_list, container, false)!!

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val friendListLayout = view as RelativeLayout

        friendListProgress = friendListLayout.findViewById(R.id.friendListProgress)

        friendListRecycler = friendListLayout.findViewById(R.id.friendListRecycler)

        friendListRecycler.layoutManager = LinearLayoutManager(activity)

        emptyText = friendListLayout.findViewById(R.id.emptyText)

        if (savedInstanceState?.containsKey(SAVE_STATE_KEY) == true) {
            saveState = savedInstanceState.getParcelable(SAVE_STATE_KEY)!!
        }
    }

    private fun initializeFriendPickerFragment(friendPickerFragment: FriendPickerFragment) {
        check(data != null)

        val userIds = friendListAdapter!!.userDataWrappers
                .map { it.userListData.id }
                .toSet()

        val friendDatas = data!!.friendDatas
                .values
                .filterNot { userIds.contains(it.id) }
                .map { FriendPickerFragment.FriendData(it.id, it.name, it.email) }

        friendPickerFragment.initialize(friendDatas) { friendId ->
            check(data!!.friendDatas.containsKey(friendId))
            check(friendListAdapter!!.userDataWrappers.none { it.userListData.id == friendId })

            val friendData = data!!.friendDatas[friendId]!!

            val position = friendListAdapter!!.itemCount

            friendListAdapter!!.userDataWrappers.add(UserDataWrapper(friendData, HashSet()))
            friendListAdapter!!.notifyItemChanged(position)

            if (data!!.userListDatas.isEmpty()) {
                emptyText.visibility = View.GONE
                friendListRecycler.visibility = View.VISIBLE
            }
        }
    }

    fun initialize(projectId: String?, data: ShowProjectViewModel.Data) {
        this.projectId = projectId
        this.data = data

        if (friendListAdapter != null)
            saveState = friendListAdapter!!.saveState
        else if (saveState == null)
            saveState = SaveState(HashSet(), HashSet(), HashSet())

        friendListAdapter = FriendListAdapter(data.userListDatas, saveState!!)
        friendListRecycler.adapter = friendListAdapter

        selectionCallback.setSelected(friendListAdapter!!.selected.size)

        friendListProgress.visibility = View.GONE

        updateFabVisibility()

        if (friendListAdapter!!.userDataWrappers.isEmpty()) {
            friendListRecycler.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
            emptyText.setText(R.string.friends_empty)
        } else {
            friendListRecycler.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
        }

        (childFragmentManager.findFragmentByTag(FRIEND_PICKER_TAG) as? FriendPickerFragment)?.let { initializeFriendPickerFragment(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        friendListAdapter?.let { outState.putParcelable(SAVE_STATE_KEY, it.saveState) }
    }

    fun dataChanged(): Boolean {
        if (data == null)
            return false

        val saveState = friendListAdapter!!.saveState

        if (!saveState.addedIds.isEmpty())
            return true

        return !saveState.removedIds.isEmpty()
    }

    fun save(name: String) {
        check(name.isNotEmpty())
        check(data != null)
        check(friendListAdapter != null)

        val saveState = friendListAdapter!!.saveState

        if (projectId.isNullOrEmpty()) {
            check(saveState.removedIds.isEmpty())

            KotlinDomainFactory.getKotlinDomainFactory().createProject(data!!.dataId, SaveService.Source.GUI, name, saveState.addedIds)
        } else {
            KotlinDomainFactory.getKotlinDomainFactory().updateProject(data!!.dataId, SaveService.Source.GUI, projectId!!, name, saveState.addedIds, saveState.removedIds)
        }
    }

    override fun setFab(floatingActionButton: FloatingActionButton) {
        check(friendListFab == null)

        friendListFab = floatingActionButton

        friendListFab!!.setOnClickListener {
            FriendPickerFragment.newInstance().also {
                initializeFriendPickerFragment(it)
                it.show(childFragmentManager, FRIEND_PICKER_TAG)
            }
        }

        updateFabVisibility()
    }

    private fun updateFabVisibility() {
        if (friendListFab == null)
            return

        if (data != null && !selectionCallback.hasActionMode) {
            friendListFab!!.show()
        } else {
            friendListFab!!.hide()
        }
    }

    override fun clearFab() {
        check(friendListFab != null)

        friendListFab!!.setOnClickListener(null)

        friendListFab = null
    }

    inner class FriendListAdapter(userListDatas: Collection<ShowProjectViewModel.UserListData>, saveState: SaveState) : RecyclerView.Adapter<FriendListAdapter.FriendHolder>() {

        val userDataWrappers: MutableList<UserDataWrapper>

        val selected get() = userDataWrappers.filter { it.selected }.map { it.userListData.email }

        val saveState: SaveState
            get() {
                check(data != null)

                val oldUserIds = data!!.userListDatas
                        .map { it.id }
                        .toSet()

                val newUserIds = userDataWrappers.map { it.userListData.id }.toSet()

                val addedIds = newUserIds.minus(oldUserIds)
                val removedIds = oldUserIds.minus(newUserIds)

                val selectedIds = userDataWrappers
                        .filter { it.selected }
                        .map { it.userListData.id }
                        .toSet()

                return SaveState(addedIds, removedIds, selectedIds)
            }

        init {
            check(data != null)

            val userListMap = userListDatas.associateBy { it.id }.toMutableMap()

            saveState.removedIds.forEach { userListMap.remove(it) }

            if (!saveState.addedIds.isEmpty()) {
                userListMap.putAll(data!!.friendDatas.values
                        .filter { saveState.addedIds.contains(it.id) }
                        .associateBy { it.id })
            }

            userDataWrappers = userListMap.values
                    .sortedBy { it.id }
                    .map { UserDataWrapper(it, saveState.selectedIds) }
                    .toMutableList()
        }

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

            friendHolder.friendName.text = userDataWrapper.userListData.name
            friendHolder.friendEmail.text = userDataWrapper.userListData.email

            if (userDataWrapper.selected)
                friendHolder.itemView.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.selected))
            else
                friendHolder.itemView.setBackgroundColor(Color.TRANSPARENT)

            friendHolder.itemView.setOnLongClickListener {
                friendHolder.onLongClick()
                true
            }

            friendHolder.itemView.setOnClickListener {
                if (selectionCallback.hasActionMode)
                    friendHolder.onLongClick()
            }
        }

        fun removeSelected() {
            val selectedUserDataWrappers = userDataWrappers.filter { it.selected }

            for (userDataWrapper in selectedUserDataWrappers) {
                val position = userDataWrappers.indexOf(userDataWrapper)
                userDataWrappers.removeAt(position)
                notifyItemRemoved(position)
            }
        }

        inner class FriendHolder(view: View) : RecyclerView.ViewHolder(view) {

            val friendName = itemView.friendName!!
            val friendEmail = itemView.friendEmail!!

            fun onLongClick() {
                userDataWrappers[adapterPosition].toggleSelect()
            }
        }
    }

    inner class UserDataWrapper(val userListData: ShowProjectViewModel.UserListData, selectedIds: Set<String>) {

        var selected = false

        init {
            selected = selectedIds.contains(userListData.id)
        }

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

    @Parcelize
    class SaveState(val addedIds: Set<String>, val removedIds: Set<String>, val selectedIds: Set<String>) : Parcelable
}
