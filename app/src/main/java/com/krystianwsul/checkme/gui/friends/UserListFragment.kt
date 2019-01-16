package com.krystianwsul.checkme.gui.friends


import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.AbstractFragment
import com.krystianwsul.checkme.gui.FabUser
import com.krystianwsul.checkme.gui.SelectionCallback
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.ShowProjectViewModel
import com.krystianwsul.treeadapter.*
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.empty_text.*
import kotlinx.android.synthetic.main.fragment_friend_list.*
import java.util.*

class UserListFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SAVE_STATE_KEY = "saveState"
        private const val FRIEND_PICKER_TAG = "friendPicker"

        fun newInstance() = UserListFragment()
    }

    private var projectId: String? = null

    private lateinit var treeViewAdapter: TreeViewAdapter

    private var data: ShowProjectViewModel.Data? = null

    private var saveState: SaveState? = null

    private val selectionCallback = object : SelectionCallback() {

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override fun onMenuClick(itemId: Int, x: TreeViewAdapter.Placeholder) {
            check(itemId == R.id.action_friends_delete)

            (treeViewAdapter.treeModelAdapter as FriendListAdapter).removeSelected(x)
        }

        override fun onFirstAdded(x: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            actionMode!!.menuInflater.inflate(R.menu.menu_friends, actionMode!!.menu)

            updateFabVisibility()
        }

        override fun onSecondAdded() = Unit

        override fun onOtherAdded() = Unit

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) = updateFabVisibility()

        override fun onSecondToLastRemoved() = Unit

        override fun onOtherRemoved() = Unit
    }

    private var friendListFab: FloatingActionButton? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_friend_list, container, false)!!

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        friendListRecycler.layoutManager = LinearLayoutManager(activity)

        if (savedInstanceState?.containsKey(SAVE_STATE_KEY) == true)
            saveState = savedInstanceState.getParcelable(SAVE_STATE_KEY)!!
    }

    private fun initializeFriendPickerFragment(friendPickerFragment: FriendPickerFragment) {
        check(data != null)

        val userIds = getSelected()
                .map { it.userListData.id }
                .toSet()

        val friendDatas = data!!.friendDatas
                .values
                .asSequence()
                .filterNot { userIds.contains(it.id) }
                .map { FriendPickerFragment.FriendData(it.id, it.name, it.email) }
                .toList()

        friendPickerFragment.initialize(friendDatas) { friendId ->
            check(data!!.friendDatas.containsKey(friendId))
            check(getSelected().none { it.userListData.id == friendId })

            val friendData = data!!.friendDatas[friendId]!!

            treeViewAdapter.updateDisplayedNodes {
                val userNode = UserNode(friendData, HashSet())

                (treeViewAdapter.treeModelAdapter as FriendListAdapter).apply {
                    userNodes.add(userNode)
                    treeNodeCollection.add(userNode.initialize(treeNodeCollection), TreeViewAdapter.Placeholder)
                }
            }

            updateVisibility()
        }
    }

    fun initialize(projectId: String?, data: ShowProjectViewModel.Data) {
        this.projectId = projectId
        this.data = data

        if (this::treeViewAdapter.isInitialized) {
            saveState = (treeViewAdapter.treeModelAdapter as FriendListAdapter).getSaveState()

            treeViewAdapter.updateDisplayedNodes(true) {
                (treeViewAdapter.treeModelAdapter as FriendListAdapter).initialize(data.userListDatas, saveState!!)
            }
        } else if (saveState == null) {
            saveState = SaveState(HashSet(), HashSet(), HashSet())

            val friendListAdapter = FriendListAdapter()
            friendListAdapter.initialize(data.userListDatas, saveState!!)
            treeViewAdapter = friendListAdapter.treeViewAdapter
            friendListRecycler.adapter = treeViewAdapter
        }

        selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)

        updateFabVisibility()
        updateVisibility()

        (childFragmentManager.findFragmentByTag(FRIEND_PICKER_TAG) as? FriendPickerFragment)?.let { initializeFriendPickerFragment(it) }
    }

    private fun updateVisibility() {
        val hide = mutableListOf<View>(friendListProgress)
        val show: View
        if ((treeViewAdapter.treeModelAdapter as FriendListAdapter).userNodes.isEmpty()) {
            hide.add(friendListRecycler)
            show = emptyText

            emptyText.setText(R.string.friends_empty)
        } else {
            show = friendListRecycler
            hide.add(emptyText)
        }

        animateVisibility(listOf(show), hide)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (this::treeViewAdapter.isInitialized)
            (treeViewAdapter.treeModelAdapter as FriendListAdapter).let { outState.putParcelable(SAVE_STATE_KEY, it.getSaveState()) }
    }

    fun dataChanged(): Boolean {
        if (data == null)
            return false

        val saveState = (treeViewAdapter.treeModelAdapter as FriendListAdapter).getSaveState()

        if (!saveState.addedIds.isEmpty())
            return true

        return !saveState.removedIds.isEmpty()
    }

    fun save(name: String) {
        check(name.isNotEmpty())
        check(data != null)

        val saveState = (treeViewAdapter.treeModelAdapter as FriendListAdapter).getSaveState()

        if (projectId.isNullOrEmpty()) {
            check(saveState.removedIds.isEmpty())

            DomainFactory.instance.createProject(data!!.dataId, SaveService.Source.GUI, name, saveState.addedIds)
        } else {
            DomainFactory.instance.updateProject(data!!.dataId, SaveService.Source.GUI, projectId!!, name, saveState.addedIds, saveState.removedIds)
        }
    }

    override fun setFab(floatingActionButton: FloatingActionButton) {
        check(friendListFab == null)

        friendListFab = floatingActionButton

        floatingActionButton.setOnClickListener {
            FriendPickerFragment.newInstance().also {
                initializeFriendPickerFragment(it)
                it.show(childFragmentManager, FRIEND_PICKER_TAG)
            }
        }

        updateFabVisibility()
    }

    private fun updateFabVisibility() {
        friendListFab?.let {
            if (data != null && !selectionCallback.hasActionMode) {
                it.show()
            } else {
                it.hide()
            }
        }
    }

    override fun clearFab() {
        check(friendListFab != null)

        friendListFab = null
    }

    private fun getSelected() = treeViewAdapter.selectedNodes.map { (it.modelNode as UserNode) }

    inner class FriendListAdapter : TreeModelAdapter {

        lateinit var userNodes: MutableList<UserNode>
            private set

        val treeViewAdapter = TreeViewAdapter(this)
        lateinit var treeNodeCollection: TreeNodeCollection
            private set

        fun initialize(userListDatas: Collection<ShowProjectViewModel.UserListData>, saveState: SaveState) {
            check(data != null)

            val userListMap = userListDatas.associateBy { it.id }.toMutableMap()

            saveState.removedIds.forEach { userListMap.remove(it) }

            if (!saveState.addedIds.isEmpty()) {
                userListMap.putAll(data!!.friendDatas.values
                        .filter { saveState.addedIds.contains(it.id) }
                        .associateBy { it.id })
            }

            userNodes = userListMap.values
                    .sortedBy { it.id }
                    .map { UserNode(it, saveState.selectedIds) }
                    .toMutableList()

            treeNodeCollection = TreeNodeCollection(treeViewAdapter)
            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            treeNodeCollection.nodes = userNodes.map { it.initialize(treeNodeCollection) }
        }

        fun getSaveState(): SaveState {
            check(data != null)

            val oldUserIds = data!!.userListDatas
                    .map { it.id }
                    .toSet()

            val newUserIds = userNodes.map { it.userListData.id }.toSet()

            val addedIds = newUserIds.minus(oldUserIds)
            val removedIds = oldUserIds.minus(newUserIds)

            val selectedIds = getSelected().map { it.userListData.id }.toSet()

            return SaveState(addedIds, removedIds, selectedIds)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(activity!!.layoutInflater.inflate(R.layout.row_list, parent, false))

        fun removeSelected(@Suppress("UNUSED_PARAMETER") x: TreeViewAdapter.Placeholder) {
            val selectedUserDataWrappers = getSelected()

            for (userDataWrapper in selectedUserDataWrappers) {
                userNodes.remove(userDataWrapper)
                (treeViewAdapter.treeModelAdapter as FriendListAdapter).treeNodeCollection.remove(userDataWrapper.treeNode, x)
            }
        }

        override val hasActionMode get() = selectionCallback.hasActionMode

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = selectionCallback.incrementSelected(x)

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = selectionCallback.decrementSelected(x)
    }

    inner class UserNode(val userListData: ShowProjectViewModel.UserListData, private val selectedIds: Set<String>) : GroupHolderNode(0) {

        override val ripple = true

        override val name = Triple(userListData.name, colorPrimary, true)

        override val details = Pair(userListData.email, colorSecondary)

        public override lateinit var treeNode: TreeNode
            private set

        override val id = userListData.id

        override val isSelectable = true

        override val isSeparatorVisibleWhenNotExpanded = false

        override val isVisibleDuringActionMode = true

        override val isVisibleWhenEmpty = true

        override fun onClick() = Unit

        override fun compareTo(other: ModelNode) = userListData.id.compareTo((other as UserNode).userListData.id)

        fun initialize(treeNodeCollection: TreeNodeCollection): TreeNode {
            treeNode = TreeNode(this, treeNodeCollection, false, selectedIds.contains(userListData.id))
            treeNode.setChildTreeNodes(listOf())
            return treeNode
        }
    }

    @Parcelize
    class SaveState(val addedIds: Set<String>, val removedIds: Set<String>, val selectedIds: Set<String>) : Parcelable
}
