package com.krystianwsul.checkme.gui.instances.tree

import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.gui.ListItemAddedListener
import com.krystianwsul.checkme.gui.MyBottomBar
import com.krystianwsul.checkme.gui.SnackbarListener
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeViewAdapter

interface GroupListListener : SnackbarListener, ListItemAddedListener {

    fun onCreateGroupActionMode(actionMode: ActionMode, treeViewAdapter: TreeViewAdapter<NodeHolder>)

    fun onDestroyGroupActionMode()

    fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean)

    fun getBottomBar(): MyBottomBar

    fun initBottomBar()

    fun deleteTasks(taskKeys: Set<TaskKey>)
}