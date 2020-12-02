package com.krystianwsul.checkme.gui.tree.delegates.thumbnail

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.domainmodel.toImageLoader
import com.krystianwsul.checkme.gui.tree.NodeDelegate
import com.krystianwsul.common.firebase.models.ImageState

class ThumbnailDelegate(private val modelNode: ThumbnailModelNode) : NodeDelegate {

    override val state get() = State(modelNode.thumbnail)

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val thumbnail = modelNode.thumbnail

        (viewHolder as ThumbnailHolder).rowThumbnail.let {
            it.visibility = if (thumbnail != null) {
                thumbnail.toImageLoader().load(it, true)

                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    data class State(val thumbnail: ImageState?)
}