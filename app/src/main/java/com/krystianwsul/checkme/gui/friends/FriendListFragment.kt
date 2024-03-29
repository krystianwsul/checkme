package com.krystianwsul.checkme.gui.friends


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.CustomItemAnimator
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentFriendListBinding
import com.krystianwsul.checkme.domainmodel.extensions.addFriends
import com.krystianwsul.checkme.domainmodel.extensions.removeFriends
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.gui.base.ActionModeListener
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.checkme.gui.friends.findfriend.FindFriendActivity
import com.krystianwsul.checkme.gui.main.FabUser
import com.krystianwsul.checkme.gui.main.MainActivity
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.AbstractModelNode
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.HolderType
import com.krystianwsul.checkme.gui.tree.delegates.avatar.AvatarDelegate
import com.krystianwsul.checkme.gui.tree.delegates.avatar.AvatarModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineRow
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.gui.utils.SelectionCallback
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.viewmodels.FriendListViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.utils.UserKey
import com.krystianwsul.treeadapter.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign
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

        override fun onFirstAdded(placeholder: TreeViewAdapter.Placeholder, initial: Boolean) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            updateFabVisibility()

            (activity as MainActivity).onCreateActionMode(actionMode!!)

            super.onFirstAdded(placeholder, initial)
        }

        override fun onLastRemoved(placeholder: TreeViewAdapter.Placeholder) {
            updateFabVisibility()

            (activity as MainActivity).onDestroyActionMode()
        }
    }

    private var friendListFabDelegate: BottomFabMenuDelegate.FabDelegate? = null

    private val isVisible = BehaviorRelay.createDefault(false)
    private lateinit var friendListViewModel: FriendListViewModel

    private val mainActivity get() = activity as MainActivity

    private val bindingProperty = ResettableProperty<FragmentFriendListBinding>()
    private var binding by bindingProperty

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            FragmentFriendListBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState?.containsKey(SELECTED_IDS_KEY) == true)
            selectedIds = savedInstanceState.getParcelableArrayList(SELECTED_IDS_KEY)!!

        friendListViewModel = getViewModel<FriendListViewModel>().apply {
            viewCreatedDisposable += isVisible.subscribe { if (it) start() else stop() }

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

                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it, false)
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
                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it, true)
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

    private fun updateSelectAll() =
        (activity as MainActivity).setUserSelectAllVisibility(treeViewAdapter.displayedNodes.isNotEmpty())

    override fun setFab(fabDelegate: BottomFabMenuDelegate.FabDelegate) {
        friendListFabDelegate = fabDelegate

        friendListFabDelegate!!.setOnClickListener { startActivity(FindFriendActivity.newIntent(requireActivity())) }

        updateFabVisibility()

        isVisible.accept(true)
    }

    private fun updateFabVisibility() {
        friendListFabDelegate?.run {
            if (data != null && !selectionCallback.hasActionMode) {
                show()
            } else {
                hide()
            }
        }
    }

    override fun clearFab() {
        isVisible.accept(false)

        friendListFabDelegate = null
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    private inner class FriendListAdapter : BaseAdapter(), ActionModeCallback by selectionCallback {

        lateinit var userDataWrappers: MutableList<FriendNode>

        val treeViewAdapter = TreeViewAdapter(
                this,
                TreeViewAdapter.PaddingData(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
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
            val userListDatas = selectedUserDataWrappers.map { it.userListData }
            val userPairs = userListDatas.associate { it.id to it.userWrapper }
            val friendIds = userPairs.map { it.key }.toSet()

            AndroidDomainUpdater.removeFriends(friendListViewModel.dataId.toFirst(), friendIds)
                    .observeOn(AndroidSchedulers.mainThread())
                    .andThen(mainActivity.showSnackbarRemovedMaybe(userListDatas.size))
                    .flatMapCompletable { AndroidDomainUpdater.addFriends(friendListViewModel.dataId.toFirst(), userPairs) }
                    .subscribe()
                    .addTo(createDisposable)
        }
    }

    private inner class FriendNode(val userListData: FriendListViewModel.UserListData) :
            AbstractModelNode(),
            AvatarModelNode,
            MultiLineModelNode {

        override val rowsDelegate = object : MultiLineModelNode.RowsDelegate {

            private val name = MultiLineRow.Visible(userListData.name)
            private val details = MultiLineRow.Visible(userListData.email, R.color.textSecondary)

            override fun getRows(isExpanded: Boolean, allChildren: List<TreeNode<*>>) = listOf(name, details)
        }

        public override lateinit var treeNode: TreeNode<AbstractHolder>
            private set

        override val holderType = HolderType.AVATAR

        override val isSelectable = true

        override val id = userListData.id

        override val parentNode: ModelNode<AbstractHolder>? = null

        override val avatarUrl = userListData.photoUrl

        override val delegates by lazy {
            listOf(
                AvatarDelegate(this),
                MultiLineDelegate(this), // this one always has to be last, because it depends on layout changes from prev
            )
        }

        override val widthKey
            get() = MultiLineDelegate.WidthKey(
                    0,
                    true,
                    false,
                    false
            )

        override fun compareTo(other: ModelNode<AbstractHolder>) = userListData.id.compareTo((other as FriendNode).userListData.id)

        fun initialize(treeNodeCollection: TreeNodeCollection<AbstractHolder>): TreeNode<AbstractHolder> {
            treeNode = TreeNode(
                    this,
                    treeNodeCollection,
                    selectedIds.contains(userListData.id),
            )

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
