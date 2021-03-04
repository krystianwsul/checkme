package com.krystianwsul.checkme.gui.instances.tree

import android.os.Parcelable
import com.krystianwsul.common.utils.Parcelize
import com.krystianwsul.treeadapter.TreeNode

@Parcelize
data class CollectionExpansionState(
        val collection: TreeNode.ExpansionState? = null,
        val done: TreeNode.ExpansionState? = null,
) : Parcelable