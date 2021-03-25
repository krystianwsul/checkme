package com.krystianwsul.checkme.gui.friends


import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentFriendListBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.createProject
import com.krystianwsul.checkme.domainmodel.extensions.updateProject
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.checkme.gui.main.FabUser
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.AbstractModelNode
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.HolderType
import com.krystianwsul.checkme.gui.tree.delegates.avatar.AvatarDelegate
import com.krystianwsul.checkme.gui.tree.delegates.avatar.AvatarModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineDelegate
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineModelNode
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineNameData
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.gui.utils.SelectionCallback
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.viewmodels.ShowProjectViewModel
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.UserKey
import com.krystianwsul.treeadapter.*
import io.reactivex.rxjava3.core.Completable
import kotlinx.parcelize.Parcelize
import java.util.*

class UserListFragment : AbstractFragment(), FabUser {

    companion object {

        private const val SAVE_STATE_KEY = "saveState"
        private const val FRIEND_PICKER_TAG = "friendPicker"

        fun newInstance() = UserListFragment()
    }

    private var projectId: ProjectKey.Shared? = null

    lateinit var treeViewAdapter: TreeViewAdapter<AbstractHolder>
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

        override fun onFirstAdded(placeholder: TreeViewAdapter.Placeholder, initial: Boolean) {
            (activity as AppCompatActivity).startSupportActionMode(this)

            updateFabVisibility()

            super.onFirstAdded(placeholder, initial)
        }

        override fun onLastRemoved(placeholder: TreeViewAdapter.Placeholder) = updateFabVisibility()
    }

    private var friendListFab: FloatingActionButton? = null

    private val listener get() = activity as UserListListener

    private val bindingProperty = ResettableProperty<FragmentFriendListBinding>()
    private var binding by bindingProperty

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = FragmentFriendListBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.friendListEmptyTextInclude
                .emptyTextPadding
                .visibility = View.VISIBLE
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.friendListRecycler.layoutManager = LinearLayoutManager(activity)

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
        if (!bindingProperty.isSet) return

        if (this::treeViewAdapter.isInitialized) {
            saveState = (treeViewAdapter.treeModelAdapter as FriendListAdapter).getSaveState()

            treeViewAdapter.updateDisplayedNodes {
                (treeViewAdapter.treeModelAdapter as FriendListAdapter).initialize(data!!.userListDatas, saveState)

                selectionCallback.setSelected(treeViewAdapter.selectedNodes.size, it, false)
            }
        } else {
            val friendListAdapter = FriendListAdapter()
            friendListAdapter.initialize(data!!.userListDatas, saveState)
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
        updateVisibility(data!!.immediate)

        tryGetFragment<FriendPickerFragment>(FRIEND_PICKER_TAG)?.let { initializeFriendPickerFragment(it) }

        updateSelectAll()
    }

    private fun updateVisibility(immediate: Boolean) {
        val hide = mutableListOf<View>(binding.friendListProgress)
        val show: View
        if ((treeViewAdapter.treeModelAdapter as FriendListAdapter).userNodes.isEmpty()) {
            binding.friendListEmptyTextInclude
                    .emptyText
                    .setText(R.string.friends_empty)

            show = binding.friendListEmptyTextInclude.emptyTextLayout
            hide += binding.friendListRecycler
        } else {
            show = binding.friendListRecycler
            hide += binding.friendListEmptyTextInclude.emptyTextLayout
        }

        animateVisibility(listOf(show), hide, immediate)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (this::treeViewAdapter.isInitialized) {
            (treeViewAdapter.treeModelAdapter as FriendListAdapter).let {
                outState.putParcelable(SAVE_STATE_KEY, it.getSaveState())
            }
        }
    }

    fun dataChanged(): Boolean {
        if (data == null) return false

        val saveState = (treeViewAdapter.treeModelAdapter as FriendListAdapter).getSaveState()

        if (saveState.addedIds.isNotEmpty()) return true

        return saveState.removedIds.isNotEmpty()
    }

    @CheckResult
    fun save(name: String): Completable {
        check(name.isNotEmpty())
        checkNotNull(data)

        val saveState = (treeViewAdapter.treeModelAdapter as FriendListAdapter).getSaveState()

        return DomainFactory.instance
                .run {
                    if (projectId == null) {
                        check(saveState.removedIds.isEmpty())

                        createProject(
                                DomainListenerManager.NotificationType.All,
                                name,
                                saveState.addedIds,
                        ).ignoreElement()
                    } else {
                        updateProject(
                                DomainListenerManager.NotificationType.All,
                                projectId!!,
                                name,
                                saveState.addedIds,
                                saveState.removedIds,
                        )
                    }
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
            if (data != null && !selectionCallback.hasActionMode) it.show() else it.hide()
        }
    }

    override fun clearFab() {
        checkNotNull(friendListFab)

        friendListFab = null
    }

    private fun getSelected() = treeViewAdapter.selectedNodes.map { (it.modelNode as UserNode) }

    private fun updateSelectAll() = listener.setUserSelectAllVisibility(treeViewAdapter.displayedNodes.isNotEmpty())

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    inner class FriendListAdapter : BaseAdapter(), ActionModeCallback by selectionCallback {

        lateinit var userNodes: MutableList<UserNode>
            private set

        val treeViewAdapter = TreeViewAdapter(
                this,
                TreeViewAdapter.PaddingData(R.layout.row_group_list_fab_padding, R.id.paddingProgress),
        )

        public override lateinit var treeNodeCollection: TreeNodeCollection<AbstractHolder>
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
            checkNotNull(data)

            val oldUserIds = data!!.userListDatas
                    .map { it.id }
                    .toSet()

            val newUserIds = userNodes.map { it.userListData.id }.toSet()

            val addedIds = newUserIds.minus(oldUserIds)
            val removedIds = oldUserIds.minus(newUserIds)

            val selectedIds = getSelected().map { it.userListData.id }.toSet()

            return SaveState(addedIds, removedIds, selectedIds)
        }

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
            private val selectedIds: Set<UserKey>,
    ) : AbstractModelNode(), AvatarModelNode, MultiLineModelNode {

        override val holderType = HolderType.AVATAR

        override val name = MultiLineNameData.Visible(userListData.name)

        override val details = Pair(userListData.email, R.color.textSecondary)

        public override lateinit var treeNode: TreeNode<AbstractHolder>
            private set

        override val id = userListData.id

        override val isSelectable = true

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
                    0,
                    true,
                    false,
                    false
            )

        override fun compareTo(other: ModelNode<AbstractHolder>) = userListData.id.compareTo((other as UserNode).userListData.id)

        fun initialize(treeNodeCollection: TreeNodeCollection<AbstractHolder>) = TreeNode(
                this,
                treeNodeCollection,
                selectedIds.contains(userListData.id),
        ).also {
            treeNode = it
            it.setChildTreeNodes(listOf())
        }
    }

    @Parcelize
    class SaveState(
            val addedIds: Set<UserKey>,
            val removedIds: Set<UserKey>,
            val selectedIds: Set<UserKey>,
    ) : Parcelable

    interface UserListListener : SnackbarListener {

        fun setUserSelectAllVisibility(selectAllVisible: Boolean)

        fun getBottomBar(): MyBottomBar

        fun initBottomBar()
    }
}
