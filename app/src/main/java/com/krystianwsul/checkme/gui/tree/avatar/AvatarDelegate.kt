package com.krystianwsul.checkme.gui.tree.avatar

import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.tree.NodeDelegate
import com.krystianwsul.checkme.utils.loadPhoto

class AvatarDelegate(private val modelNode: AvatarModelNode) : NodeDelegate {

    override val state get() = State(modelNode.avatarUrl)

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as AvatarHolder).rowImage.loadPhoto(modelNode.avatarUrl)
    }

    data class State(val avatarUrl: String?)
}