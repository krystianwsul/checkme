package com.krystianwsul.checkme.gui.tree

import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.krystianwsul.checkme.databinding.RowAssignedChipDetailsBinding
import com.krystianwsul.checkme.databinding.RowListDetailsBinding
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationDelegate
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationHolder
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationModelNode
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.common.criteria.QueryMatchable
import com.krystianwsul.common.firebase.models.ProjectUser
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
) : AbstractModelNode(), IndentationModelNode, QueryMatchable {

    override lateinit var treeNode: TreeNode<AbstractHolder>
        private set

    private lateinit var nodeContainer: NodeContainer<AbstractHolder>

    override val indentation = (indentation - 1).coerceAtLeast(0)

    override val holderType = HolderType.DETAILS

    override val id get() = Id(nodeContainer.id)

    override val state get() = State(super.state, projectInfo, note)

    data class Id(val id: Any)

    override val normalizedFields by lazy { listOfNotNull(note?.normalized()) }

    override val disableRipple = true

    override val showSeparatorWhenParentExpanded = false

    init {
        check(projectInfo != null || !note.isNullOrEmpty())
    }

    fun initialize(nodeContainer: NodeContainer<AbstractHolder>): TreeNode<AbstractHolder> {
        this.nodeContainer = nodeContainer

        treeNode = TreeNode(this, nodeContainer)
        treeNode.setChildTreeNodes(listOf())

        return treeNode
    }

    override val isVisibleDuringActionMode = false

    override val delegates by lazy { listOf(IndentationDelegate(this)) }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, startingDrag: Boolean) {
        super.onBindViewHolder(viewHolder, startingDrag)

        (viewHolder as Holder).apply {
            rowTopMargin.isVisible = treeNode.treeNodeCollection.getPosition(treeNode) == 0

            val projectRowVisible = projectInfo?.name?.isNotEmpty() == true
            rowProjectContainer.isVisible = projectRowVisible
            rowProject.text = projectInfo?.name

            val assignedTo = projectInfo?.assignedTo.orEmpty()
            val assignedToVisible = assignedTo.isNotEmpty()

            rowAssignedTo.apply {
                isVisible = assignedToVisible
                removeAllViews()
            }

            assignedTo.forEach { user ->
                RowAssignedChipDetailsBinding.inflate(
                        LayoutInflater.from(viewHolder.itemView.context),
                        rowAssignedTo,
                        true
                )
                        .root
                        .apply {
                            text = user.name
                            loadPhoto(user.photoUrl)
                            isCloseIconVisible = false
                            rippleColor = null
                            isClickable = false
                        }
            }

            val noteVisible = !note.isNullOrEmpty()
            rowNoteContainer.isVisible = noteVisible
            rowNote.text = note

            rowMargin1.isVisible = projectRowVisible && assignedToVisible
            rowMargin2.isVisible = noteVisible && (projectRowVisible || assignedToVisible)
        }
    }

    override fun onClick(holder: AbstractHolder) {
        parentNode?.onClick(holder)
    }

    override fun compareTo(other: ModelNode<AbstractHolder>) = -1

    override fun normalize() {
        normalizedFields
    }

    override fun getMatchResult(query: String) = ModelNode.MatchResult.fromBoolean(matchesQuery(query))

    data class State(val superState: ModelState, val projectInfo: ProjectInfo?, val note: String?) : ModelState {

        override val id = superState.id
    }

    class Holder(
            override val baseAdapter: BaseAdapter,
            binding: RowListDetailsBinding,
    ) : AbstractHolder(binding.root), IndentationHolder {

        val rowTopMargin = binding.rowListDetailsTopMargin
        override val rowContainer = binding.rowListDetailsContainer
        val rowProjectContainer = binding.rowListDetailsProjectContainer
        val rowProject = binding.rowListDetailsProject
        val rowAssignedTo = binding.rowListDetailsAssignedTo
        val rowMargin1 = binding.rowListDetailsMargin1
        val rowMargin2 = binding.rowListDetailsMargin2
        val rowNoteContainer = binding.rowListDetailsNoteContainer
        val rowNote = binding.rowListDetailsNote
        override val rowSeparator = binding.rowListDetailsSeparator
    }

    data class ProjectInfo(val name: String, val assignedTo: List<User>)

    data class User(val name: String, val photoUrl: String?) {

        companion object {

            fun fromProjectUsers(users: List<ProjectUser>) = users.map { User(it.name, it.photoUrl) }
        }
    }
}
