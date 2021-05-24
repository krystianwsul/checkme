package com.krystianwsul.checkme.gui.instances.list

import android.os.Parcelable
import com.krystianwsul.checkme.gui.instances.tree.NotDoneNode
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeNode
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupListState(
        val doneExpansionState: TreeNode.ExpansionState? = null,
        val contentDelegateStates: Map<NotDoneNode.ContentDelegate.Id, NotDoneNode.ContentDelegate.State> = mapOf(),
        val unscheduledExpansionState: TreeNode.ExpansionState? = null,
        val taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState> = mapOf(),
        val selectedTaskKeys: List<TaskKey> = listOf(),
) : Parcelable