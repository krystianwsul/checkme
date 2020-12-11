package com.krystianwsul.checkme.gui.tree

import android.view.LayoutInflater
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.databinding.RowAssignedChipBinding
import com.krystianwsul.checkme.databinding.RowListDetailsBinding
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationHolder
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.gui.utils.SearchData
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.common.utils.normalized
import com.krystianwsul.treeadapter.ModelNode
import com.krystianwsul.treeadapter.ModelState
import com.krystianwsul.treeadapter.NodeContainer
import com.krystianwsul.treeadapter.TreeNode

class DetailsNode(
        private val projectInfo: ProjectInfo?,
        private val note: String?,
        override val parentNode: ModelNode<AbstractHolder>?,
        indentation: Int,
) : AbstractModelNode(), IndentationModelNode {

    override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    private lateinit var nodeContainer: NodeContainer<AbstractHolder>

    override val indentation = indentation - 1

    override val holderType = HolderType.DETAILS

    override val id get() = Id(nodeContainer.id)

    override val state get() = State(super.state, projectInfo, note)

    data class Id(val id: Any)

    private val normalizedNote by lazy { note?.normalized() }

    override val disableRipple = true

    init {
        check(projectInfo != null || !note.isNullOrEmpty())
    }

    fun initialize(nodeContainer: NodeContainer<AbstractHolder>): TreeNode<AbstractHolder> {
        this.nodeContainer = nodeContainer

        treeNode = TreeNode(this, nodeContainer, expanded = false, selected = false)
        treeNode.setChildTreeNodes(listOf())

        return treeNode
    }

    override val isVisibleDuringActionMode = false

    override val delegates by lazy { listOf(IndentationDelegate(this)) }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        super.onBindViewHolder(viewHolder, startingDrag)

        (viewHolder as Holder).apply {
            if (projectInfo != null) {
                rowProjectContainer.isVisible = true

                rowProject.text = projectInfo.name

                rowAssignedTo.removeAllViews()

                projectInfo.assignedTo.forEach { user ->
                    RowAssignedChipBinding.inflate(
                            LayoutInflater.from(viewHolder.itemView.context),
                            rowAssignedTo,
                            true
                    )
                            .root
                            .apply {
                                text = user.name
                                loadPhoto(user.photoUrl)
                                isCloseIconVisible = false
                            }
                }
            } else {
                rowProjectContainer.isGone = true
            }

            if (note.isNullOrEmpty()) {
                rowNote.isGone = true
            } else {
                rowNote.isVisible = true

                rowNote.text = note
            }
        }
    }

    override fun compareTo(other: ModelNode<AbstractHolder>) = -1

    override fun normalize() {
        normalizedNote
    }

    override fun matches(filterCriteria: Any?): Boolean {
        if (filterCriteria == null) return true

        val query = (filterCriteria as SearchData).query

        if (query.isEmpty()) return true

        return normalizedNote?.contains(query) == true
    }

    override fun canBeShownWithFilterCriteria(filterCriteria: Any?) = false

    data class State(val superState: ModelState, val projectInfo: ProjectInfo?, val note: String?) : ModelState {

        override val id = superState.id
    }

    class Holder(
            override val baseAdapter: BaseAdapter,
            binding: RowListDetailsBinding,
    ) : AbstractHolder(binding.root), IndentationHolder {

        override val rowContainer = binding.rowListDetailsContainer
        val rowProjectContainer = binding.rowListDetailsProjectContainer
        val rowProject = binding.rowListDetailsProject
        val rowAssignedTo = binding.rowListDetailsAssignedTo
        val rowNote = binding.rowListDetailsNote
        override val rowSeparator = binding.rowListDetailsSeparator
    }

    data class ProjectInfo(val name: String, val assignedTo: List<AssignedNode.User>)
}
