package com.krystianwsul.checkme.gui.instances.tree

import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.TreeNode

class CollectionState(
    val expandedGroups: Map<TimeStamp, TreeNode.ExpansionState>,
    val expandedInstances: Map<InstanceKey, CollectionExpansionState>,
    val selectedGroups: List<Long>,
    val selectedInstances: List<InstanceKey>,
)