package com.krystianwsul.checkme.gui.tree.avatar

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.treeadapter.ModelNode

interface AvatarModelNode<T> : ModelNode<T> where T : RecyclerView.ViewHolder, T : AvatarHolder {

    val avatarUrl: String?
}