package com.krystianwsul.checkme.gui.instances.list

import android.os.Parcelable
import com.krystianwsul.checkme.gui.instances.tree.CollectionExpansionState
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.treeadapter.TreeNode
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupListState(
        val doneExpansionState: TreeNode.ExpansionState? = null,
        val groupExpansionStates: Map<TimeStamp, TreeNode.ExpansionState> = mapOf(),
        val instanceExpansionStates: Map<InstanceKey, CollectionExpansionState> = mapOf(),
        val unscheduledExpansionState: TreeNode.ExpansionState? = null,
        val taskExpansionStates: Map<TaskKey, TreeNode.ExpansionState> = mapOf(),
        val selectedInstances: List<InstanceKey> = listOf(),
        val selectedGroups: List<Long> = listOf(),
        val selectedTaskKeys: List<TaskKey> = listOf(),
) : Parcelable