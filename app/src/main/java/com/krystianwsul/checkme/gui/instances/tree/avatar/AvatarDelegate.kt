package com.krystianwsul.checkme.gui.instances.tree.avatar

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.gui.instances.tree.NodeDelegate
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.treeadapter.TreeNode

class AvatarDelegate<T>(
        private val treeNode: TreeNode<T>,
        private val avatarModelNode: AvatarModelNode<T>,
) : NodeDelegate
        where T : RecyclerView.ViewHolder,
              T : AvatarHolder {

    override val state get() = State(avatarModelNode.avatarUrl)

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder as AvatarHolder).rowImage!!.run {
            visibility = View.VISIBLE // todo delegate always visible
            loadPhoto(avatarModelNode.avatarUrl)
        }
    }

    data class State(val avatarUrl: String?)
}