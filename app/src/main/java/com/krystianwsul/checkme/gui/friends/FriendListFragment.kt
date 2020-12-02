package com.krystianwsul.checkme.gui.friends


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentFriendListBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.addFriends
import com.krystianwsul.checkme.domainmodel.extensions.removeFriends
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.gui.base.ActionModeListener
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.checkme.gui.main.FabUser
import com.krystianwsul.checkme.gui.main.MainActivity
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.GroupHolderNode
import com.krystianwsul.checkme.gui.tree.HolderType
import com.krystianwsul.checkme.gui.tree.delegates.avatar.AvatarDelegate
import com.krystianwsul.checkme.gui.tree.delegates.avatar.AvatarModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineNameData
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.gui.utils.SelectionCallback
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.FriendListViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.UserKey
import com.krystianwsul.treeadapter.*
import io.reactivex.rxkotlin.plusAssign
import java.util.*

class FriendListFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SELECTED_IDS_KEY = "selectedIds"

        fun newInstance() = FriendListFragment()
    }

    lateinit var treeViewAdapter: TreeViewAdapter<AbstractHolder>
        private set

    private var data: FriendListViewModel.Data? = null

    private var selectedIds = listOf<UserKey>()

    private val listener get() = activity as FriendListListener

    private val selectionCallback = object : SelectionCallback() {

        override val activity get() = requireActivity()

        override fun getTreeViewAdapter() = treeViewAdapter

        override fun unselect(placeholder: TreeViewAdapter.Placeholder) = treeViewAdapter.unselect(placeholder)

        override val bottomBarData by lazy { Triple(listener.getBottomBar(), R.menu.menu_friends, listener::initBottomBar) }

        override fun onMenuClick(itemId: Int, placeholder: TreeViewAdapter.Placeholder): Boolean {
            val selectedUserDataEmails = treeViewAdapter.selectedNodes
            check(selectedUserDataEmails.isNotEmpty())

            when (itemId) {
                R.id.action_friends_delete -> {
                    (treeViewAdapter.treeModelAdapter as FriendListAdapter).removeSelected(placeholder)

                    updateSelectAll()
                }
                else -> throw UnsupportedOperationException()
            }

            return true
        }

        override fun onFirstAdded(placeholder: TreeViewAdapter.Placeholder) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            updateFabVisibility()

            (activity as MainActivity).onCreateActionMode(actionMode!!)

            super.onFirstAdded(placeholder)
        }

        override fun onLastRemoved(placeholder: TreeViewAdapter.Placeholder) {
            updateFabVisibility()

            (activity as MainActivity).onDestroyActionMode()
        }
    }

    private var friendListFab: FloatingActionButton? = null

    private lateinit var friendListViewModel: FriendListViewModel

    private val mainActivity get() = activity as MainActivity

    private val bindingProperty = ResettableProperty<FragmentFriendListBinding>()
    private var binding by bindingProperty

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = FragmentFriendListBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.friendListRecycler.layoutManager = LinearLayoutManager(activity)

        if (savedInstanceState?.containsKey(SELECTED_IDS_KEY) == true)
            selectedIds = savedInstanceState.getParcelableArrayList(SELECTED_IDS_KEY)!!

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

            treeViewAdapter.updateDisplayedNodes {
                (treeViewAdapter.treeModelAdapter as FriendListAdapter).initialize()

                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it)
            }
        } else {
            val friendListAdapter = FriendListAdapter()
            friendListAdapter.initialize()
            treeViewAdapter = friendListAdapter.treeViewAdapter

            binding.friendListRecycler.apply {
                adapter = treeViewAdapter
                itemAnimator = CustomItemAnimator()
            }

            treeViewAdapter.updateDisplayedNodes {
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it)
            }
        }

        updateFabVisibility()

        val hide = mutableListOf<View>(binding.friendListProgress)
        val show: View

        if (data.userListDatas.isEmpty()) {
            hide += binding.friendListRecycler
            show = binding.friendListEmptyTextInclude.emptyTextLayout

            binding.friendListEmptyTextInclude
                    .emptyText
                    .setText(R.string.friends_empty)
        } else {
            show = binding.friendListRecycler
            hide += binding.friendListEmptyTextInclude.emptyTextLayout
        }

        animateVisibility(listOf(show), hide)

        updateSelectAll()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (this::treeViewAdapter.isInitialized)
            treeViewAdapter.let { outState.putParcelableArrayList(SELECTED_IDS_KEY, ArrayList(getSelectedIds())) }
    }

    private fun updateSelectAll() = (activity as MainActivity).setUserSelectAllVisibility(treeViewAdapter.displayedNodes.isNotEmpty())

    override fun setFab(floatingActionButton: FloatingActionButton) {
        friendListFab = floatingActionButton

        friendListFab!!.setOnClickListener { startActivity(FindFriendActivity.newIntent(requireActivity())) }

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

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    private inner class FriendListAdapter : BaseAdapter(), ActionModeCallback by selectionCallback {

        lateinit var userDataWrappers: MutableList<FriendNode>

        val treeViewAdapter = TreeViewAdapter(
                this,
                Pair(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
                viewCreatedDisposable
        )

        override lateinit var treeNodeCollection: TreeNodeCollection<AbstractHolder>
            private set

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

        fun removeSelected(@Suppress("UNUSED_PARAMETER") placeHolder: TreeViewAdapter.Placeholder) {
            val selectedUserDataWrappers = userDataWrappers.filter { it.treeNode.isSelected }

            selectedUserDataWrappers.forEach { treeNodeCollection.remove(it.treeNode, placeHolder) }

            selectedUserDataWrappers.forEach { userDataWrappers.remove(it) }

            val userListDatas = selectedUserDataWrappers.map { it.userListData }
            data!!.userListDatas.removeAll(userListDatas)

            val userPairs = userListDatas.associate { it.id to it.userWrapper }
            val friendIds = userPairs.map { it.key }.toSet()

            DomainFactory.instance.removeFriends(SaveService.Source.GUI, friendIds)

            mainActivity.showSnackbarRemoved(userListDatas.size) {
                onLoadFinished(data!!.copy(userListDatas = data!!.userListDatas
                        .toMutableSet()
                        .apply { addAll(userListDatas) }))

                DomainFactory.instance.addFriends(SaveService.Source.GUI, userPairs)
            }
        }
    }

    private inner class FriendNode(val userListData: FriendListViewModel.UserListData) :
            GroupHolderNode(0),
            AvatarModelNode,
            MultiLineModelNode {

        override val ripple = true

        override val name = MultiLineNameData.Visible(userListData.name)

        override val details = Pair(userListData.email, colorSecondary)

        public override lateinit var treeNode: TreeNode<AbstractHolder>
            private set

        override val showStartMargin = false

        override val holderType = HolderType.AVATAR

        override val isSelectable = true

        override val id = userListData.id

        override val parentNode: ModelNode<AbstractHolder>? = null

        override val avatarUrl = userListData.photoUrl

        override val delegates by lazy {
            listOf(
                    AvatarDelegate(this),
                    MultiLineDelegate(this)
            )
        }

        override val widthKey
            get() = MultiLineDelegate.WidthKey(
                    indentation,
                    true,
                    true,
                    thumbnail != null
            )

        override fun compareTo(other: ModelNode<AbstractHolder>) = userListData.id.compareTo((other as FriendNode).userListData.id)

        fun initialize(treeNodeCollection: TreeNodeCollection<AbstractHolder>): TreeNode<AbstractHolder> {
            treeNode = TreeNode(this, treeNodeCollection, false, selectedIds.contains(userListData.id))
            treeNode.setChildTreeNodes(listOf())
            return treeNode
        }

        override fun matches(filterCriteria: Any?) = false

        override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = true
    }

    interface FriendListListener : SnackbarListener, ActionModeListener {

        fun setUserSelectAllVisibility(selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()
    }
}
