package com.krystianwsul.checkme.gui.instances.list

import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.gui.base.ListItemAddedListener
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.checkme.gui.instances.tree.NodeHolder
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable

interface GroupListListener : SnackbarListener, ListItemAddedListener {

    val instanceSearch: Observable<NullableWrapper<SearchData>>

    fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter<NodeHolder>)

    fun onDestroyGroupActionMode()

    fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean)

    fun getBottomBar(): MyBottomBar

    fun initBottomBar()

    fun deleteTasks(taskKeys: Set<TaskKey>)
}