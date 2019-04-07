package com.krystianwsul.checkme.gui.friends


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.instances.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.checkError
import com.krystianwsul.checkme.viewmodels.FriendListViewModel
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.treeadapter.*
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.empty_text.*
import java.util.*

class FriendListFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SELECTED_IDS_KEY = "selectedIds"

        fun newInstance() = FriendListFragment()
    }

    private lateinit var friendListProgress: ProgressBar
    private lateinit var friendListRecycler: RecyclerView

    lateinit var treeViewAdapter: TreeViewAdapter
        private set

    private var data: FriendListViewModel.Data? = null

    private var selectedIds = listOf<String>()

    private val listener get() = activity as FriendListListener

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(x: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(x)

        override val bottomBarData by lazy { Triple(listener.getBottomBar(), R.menu.menu_friends, listener::initBottomBar) }

        override fun onMenuClick(itemId: Int, x: TreeViewAdapter.Placeholder) {
            val selectedUserDataEmails = treeViewAdapter.selectedNodes
            check(!selectedUserDataEmails.isEmpty())

            when (itemId) {
                R.id.action_friends_delete -> {
                    (treeViewAdapter.treeModelAdapter as FriendListAdapter).removeSelected(x)

                    updateSelectAll()
                }
                else -> throw UnsupportedOperationException()
            }
        }

        override fun onFirstAdded(x: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            updateFabVisibility()

            (activity as MainActivity).onCreateActionMode(actionMode!!)

            super.onFirstAdded(x)
        }

        override fun onLastRemoved(x: TreeViewAdapter.Placeholder) {
            updateFabVisibility()

            (activity as MainActivity).onDestroyActionMode()
        }
    }

    private var friendListFab: FloatingActionButton? = null

    private lateinit var friendListViewModel: FriendListViewModel

    private val mainActivity get() = activity as MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_friend_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        friendListProgress = view.findViewById<View>(R.id.friendListProgress) as ProgressBar

        friendListRecycler = view.findViewById<View>(R.id.friendListRecycler) as RecyclerView

        friendListRecycler.layoutManager = LinearLayoutManager(activity)

        if (savedInstanceState?.containsKey(SELECTED_IDS_KEY) == true)
            selectedIds = savedInstanceState.getStringArrayList(SELECTED_IDS_KEY)!!

        friendListViewModel = getViewModel<FriendListViewModel>().apply {
            start()

            viewCreatedDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    private fun getSelectedIds() = treeViewAdapter.selectedNodes.map { (it.modelNode as FriendNode).userListData.id }

    private fun onLoadFinished(data: FriendListViewModel.Data) {
        this.data = data

        if (this::treeViewAdapter.isInitialized) {
            selectedIds = getSelectedIds()

            treeViewAdapter.updateDisplayedNodes(true) {
                (treeViewAdapter.treeModelAdapter as FriendListAdapter).initialize()
            }
        } else {
            val friendListAdapter = FriendListAdapter()
            friendListAdapter.initialize()
            treeViewAdapter = friendListAdapter.treeViewAdapter
            friendListRecycler.adapter = treeViewAdapter
        }

        selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, TreeViewAdapter.Placeholder)

        updateFabVisibility()

        val hide = mutableListOf<View>(friendListProgress)
        val show: View

        if (data.userListDatas.isEmpty()) {
            hide.add(friendListRecycler)
            show = emptyTextLayout

            emptyText.setText(R.string.friends_empty)
        } else {
            show = friendListRecycler
            hide.add(emptyTextLayout)
        }

        animateVisibility(listOf(show), hide)

        updateSelectAll()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (this::treeViewAdapter.isInitialized)
            treeViewAdapter.let { outState.putStringArrayList(SELECTED_IDS_KEY, ArrayList(getSelectedIds())) }
    }

    private fun updateSelectAll() {
        checkNotNull(treeViewAdapter)

        (activity as MainActivity).setUserSelectAllVisibility(treeViewAdapter.displayedNodes.isNotEmpty())
    }

    fun selectAll(x: TreeViewAdapter.Placeholder) = treeViewAdapter.selectAll(x)

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
        friendListFab = null
    }

    private inner class FriendListAdapter : TreeModelAdapter {

        lateinit var userDataWrappers: MutableList<FriendNode>

        val treeViewAdapter = TreeViewAdapter(this, R.layout.row_group_list_fab_padding)
        private lateinit var treeNodeCollection: TreeNodeCollection

        fun initialize() {
            treeNodeCollection = TreeNodeCollection(treeViewAdapter)
            treeViewAdapter.setTreeNodeCollection(treeNodeCollection)

            userDataWrappers = data!!.userListDatas
                    .asSequence()
                    .sortedBy { it.id }
                    .map { FriendNode(it) }
                    .toMutableList()

            treeNodeCollection.nodes = userDataWrappers.map { it.initialize(treeNodeCollection) }
        }

        override val hasActionMode get() = selectionCallback.hasActionMode

        override fun incrementSelected(x: TreeViewAdapter.Placeholder) = selectionCallback.incrementSelected(x)

        override fun decrementSelected(x: TreeViewAdapter.Placeholder) = selectionCallback.decrementSelected(x)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NodeHolder(layoutInflater.inflate(R.layout.row_list, parent, false)!!)

        fun removeSelected(@Suppress("UNUSED_PARAMETER") x: TreeViewAdapter.Placeholder) {
            val selectedUserDataWrappers = userDataWrappers.filter { it.treeNode.isSelected }

            selectedUserDataWrappers.forEach { treeNodeCollection.remove(it.treeNode, x) }

            selectedUserDataWrappers.forEach { userDataWrappers.remove(it) }

            val userListDatas = selectedUserDataWrappers.map { it.userListData }
            data!!.userListDatas.removeAll(userListDatas)

            val friendIds = userListDatas.map { it.id }.toSet()

            DomainFactory.instance.removeFriends(friendIds)

            mainActivity.showSnackbarRemoved(userListDatas.size) {
                onLoadFinished(data!!.copy(userListDatas = data!!.userListDatas
                        .toMutableSet()
                        .apply { addAll(userListDatas) }))

                DatabaseWrapper.addFriends(friendIds).checkError(DomainFactory.instance, "FriendListFragment.removeFriends.undo")
            }
        }
    }

    private inner class FriendNode(val userListData: FriendListViewModel.UserListData) : GroupHolderNode(0) {

        override val ripple = true

        override val name = Triple(userListData.name, colorPrimary, true)

        override val details = Pair(userListData.email, colorSecondary)

        public override lateinit var treeNode: TreeNode
            private set

        override val isSelectable = true

        override val isVisibleWhenEmpty = true

        override val id = userListData.id

        override val isSeparatorVisibleWhenNotExpanded = false

        override val isVisibleDuringActionMode = true

        override val image = NullableWrapper(userListData.photoUrl)

        override fun onClick() = Unit

        override fun compareTo(other: ModelNode) = userListData.id.compareTo((other as FriendNode).userListData.id)

        fun initialize(treeNodeCollection: TreeNodeCollection): TreeNode {
            treeNode = TreeNode(this, treeNodeCollection, false, selectedIds.contains(userListData.id))
            treeNode.setChildTreeNodes(listOf())
            return treeNode
        }
    }

    interface FriendListListener : SnackbarListener, ActionModeListener {

        fun setUserSelectAllVisibility(selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()
    }
}
