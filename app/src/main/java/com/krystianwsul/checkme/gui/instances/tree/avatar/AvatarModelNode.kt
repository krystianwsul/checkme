package com.krystianwsul.checkme.gui.instances.tree.avatar

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.treeadapter.ModelNode

interface AvatarModelNode<T> : ModelNode<T> where T : RecyclerView.ViewHolder, T : AvatarHolder {

    val avatarUrl: String?
}