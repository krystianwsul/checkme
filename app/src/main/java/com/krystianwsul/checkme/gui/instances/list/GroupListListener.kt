package com.krystianwsul.checkme.gui.instances.list

import androidx.appcompat.view.ActionMode
import com.krystianwsul.checkme.gui.base.ListItemAddedListener
import com.krystianwsul.checkme.gui.base.SnackbarListener
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.FilterCriteria
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.rxjava3.core.Observable

interface GroupListListener : SnackbarListener, ListItemAddedListener {

    val instanceSearch: Observable<FilterCriteria>

    fun onCreateGroupActionMode(
        actionMode: ActionMode,
        treeViewAdapter: TreeViewAdapter<AbstractHolder>,
        initial: Boolean,
    )

    fun onDestroyGroupActionMode()

    fun setGroupMenuItemVisibility(position: Int?, selectAllVisible: Boolean)

    fun getBottomBar(): MyBottomBar

    fun initBottomBar()

    fun deleteTasks(dataId: DataId, taskKeys: Set<TaskKey>)

    fun showFabMenu(menuDelegate: BottomFabMenuDelegate.MenuDelegate): Unit = throw UnsupportedOperationException()
}