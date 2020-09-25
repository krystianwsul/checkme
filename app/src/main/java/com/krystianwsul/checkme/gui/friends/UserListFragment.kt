package com.krystianwsul.checkme.gui.friends


import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.createProject
import com.krystianwsul.checkme.domainmodel.extensions.updateProject
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderAdapter
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.instances.tree.NameData
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.checkme.viewmodels.ShowProjectViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
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

    private var projectId: ProjectKey.Shared? = null

    lateinit var treeViewAdapter: TreeViewAdapter<NodeHolder>
        private set

    private var data: ShowProjectViewModel.Data? = null

    private var saveState = SaveState(HashSet(), HashSet(), HashSet())

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(placeholder)

        override val bottomBarData by lazy {
            Triple(listener.getBottomBar(), R.menu.menu_friends, listener::initBottomBar)
        }

        override fun onMenuClick(itemId: Int, placeholder: TreeViewAdapter.Placeholder): Boolean {
            check(itemId == R.id.action_friends_delete)

            (treeViewAdapter.treeModelAdapter as FriendListAdapter).removeSelected(placeholder)

            updateSelectAll()

            return true
        }

        override fun onFirstAdded(placeholder: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            updateFabVisibility()

            super.onFirstAdded(placeholder)
        }

        override fun onLastRemoved(placeholder: TreeViewAdapter.Placeholder) = updateFabVisibility()
    }

    private var friendListFab: FloatingActionButton? = null

    private val listener get() = activity as UserListListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_friend_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyTextPadding.visibility = View.VISIBLE
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        friendListRecycler.layoutManager = LinearLayoutManager(activity)

        if (savedInstanceState?.containsKey(SAVE_STATE_KEY) == true)
            saveState = savedInstanceState.getParcelable(SAVE_STATE_KEY)!!

        initialize()
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
                .map { FriendPickerFragment.FriendData(it.id, it.name, it.email, it.photoUrl) }
                .toList()

        friendPickerFragment.initialize(FriendPickerFragment.Data(data!!.immediate, friendDatas)) { friendId ->
            check(data!!.friendDatas.containsKey(friendId))
            check(getSelected().none { it.userListData.id == friendId })

            val friendData = data!!.friendDatas.getValue(friendId)

            treeViewAdapter.updateDisplayedNodes {
                val userNode = UserNode(friendData, HashSet())

                (treeViewAdapter.treeModelAdapter as FriendListAdapter).apply {
                    userNodes.add(userNode)
                    treeNodeCollection.add(userNode.initialize(treeNodeCollection), it)
                }
            }

            updateVisibility(false)
        }
    }

    fun initialize(projectId: ProjectKey.Shared?, data: ShowProjectViewModel.Data) {
        this.projectId = projectId
        this.data = data

        initialize()
    }

    private fun initialize() {
        if (data == null) return
        if (friendListRecycler == null) return

        if (this::treeViewAdapter.isInitialized) {
            saveState = (treeViewAdapter.treeModelAdapter as FriendListAdapter).getSaveState()

            treeViewAdapter.updateDisplayedNodes {
                (treeViewAdapter.treeModelAdapter as FriendListAdapter).initialize(data!!.userListDatas, saveState)

                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it)
            }
        } else {
            val friendListAdapter = FriendListAdapter()
            friendListAdapter.initialize(data!!.userListDatas, saveState)
            treeViewAdapter = friendListAdapter.treeViewAdapter
            friendListRecycler.adapter = treeViewAdapter
            friendListRecycler.itemAnimator = CustomItemAnimator()

            treeViewAdapter.updateDisplayedNodes {
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it)
            }
        }

        updateFabVisibility()
        updateVisibility(data!!.immediate)

        (childFragmentManager.findFragmentByTag(FRIEND_PICKER_TAG) as? FriendPickerFragment)?.let { initializeFriendPickerFragment(it) }

        updateSelectAll()
    }

    private fun updateVisibility(immediate: Boolean) {
        val hide = mutableListOf<View>(friendListProgress)
        val show: View
        if ((treeViewAdapter.treeModelAdapter as FriendListAdapter).userNodes.isEmpty()) {
            hide.add(friendListRecycler)
            show = emptyTextLayout

            emptyText.setText(R.string.friends_empty)
        } else {
            show = friendListRecycler
            hide.add(emptyTextLayout)
        }

        animateVisibility(listOf(show), hide, immediate)
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

        if (saveState.addedIds.isNotEmpty())
            return true

        return saveState.removedIds.isNotEmpty()
    }

    fun save(name: String) {
        check(name.isNotEmpty())
        check(data != null)

        val saveState = (treeViewAdapter.treeModelAdapter as FriendListAdapter).getSaveState()

        if (projectId == null) {
            check(saveState.removedIds.isEmpty())

            DomainFactory.instance.createProject(
                    data!!.dataId,
                    SaveService.Source.GUI,
                    name,
                    saveState.addedIds
            )
        } else {
            DomainFactory.instance.updateProject(
                    data!!.dataId,
                    SaveService.Source.GUI,
                    projectId!!,
                    name,
                    saveState.addedIds,
                    saveState.removedIds
            )
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

    private fun updateSelectAll() = listener.setUserSelectAllVisibility(treeViewAdapter.displayedNodes.isNotEmpty())

    inner class FriendListAdapter : GroupHolderAdapter(), ActionModeCallback by selectionCallback {

        lateinit var userNodes: MutableList<UserNode>
            private set

        val treeViewAdapter = TreeViewAdapter(
                this,
                Pair(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
                viewCreatedDisposable
        )

        public override lateinit var treeNodeCollection: TreeNodeCollection<NodeHolder>
            private set

        fun initialize(userListDatas: Collection<ShowProjectViewModel.UserListData>, saveState: SaveState) {
            checkNotNull(data)

            val userListMap = userListDatas.associateBy { it.id }.toMutableMap()

            saveState.removedIds.forEach { userListMap.remove(it) }

            if (saveState.addedIds.isNotEmpty()) {
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
    }

    inner class UserNode(
            val userListData: ShowProjectViewModel.UserListData,
            private val selectedIds: Set<UserKey>
    ) : GroupHolderNode(0) {

        override val ripple = true

        override val name = NameData(userListData.name)

        override val details = Pair(userListData.email, colorSecondary)

        public override lateinit var treeNode: TreeNode<NodeHolder>
            private set

        override val id = userListData.id

        override val isSelectable = true

        override val avatarImage = NullableWrapper(userListData.photoUrl)

        override fun compareTo(other: ModelNode<NodeHolder>) = userListData.id.compareTo((other as UserNode).userListData.id)

        fun initialize(treeNodeCollection: TreeNodeCollection<NodeHolder>): TreeNode<NodeHolder> {
            treeNode = TreeNode(this, treeNodeCollection, false, selectedIds.contains(userListData.id))
            treeNode.setChildTreeNodes(listOf())
            return treeNode
        }
    }

    @Parcelize
    class SaveState(
            val addedIds: Set<UserKey>,
            val removedIds: Set<UserKey>,
            val selectedIds: Set<UserKey>
    ) : Parcelable

    interface UserListListener : SnackbarListener {

        fun setUserSelectAllVisibility(selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()
    }
}
