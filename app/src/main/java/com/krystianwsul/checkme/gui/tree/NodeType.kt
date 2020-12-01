package com.krystianwsul.checkme.gui.tree

import android.view.LayoutInflater
import android.view.ViewGroup
import com.krystianwsul.checkme.databinding.*
import com.krystianwsul.checkme.gui.edit.dialogs.DialogNodeHolder
import com.krystianwsul.checkme.gui.instances.tree.TaskNode
import com.krystianwsul.checkme.gui.tasks.TaskListFragment
import com.krystianwsul.checkme.gui.tree.holders.ExpandableSinglelineHolder
import com.krystianwsul.checkme.gui.tree.holders.RowListAvatarHolder
import com.krystianwsul.checkme.gui.tree.holders.RowListCheckableHolder
import com.krystianwsul.checkme.gui.tree.holders.RowListMultilineHolder

enum class NodeType {

    MULTILINE { // MultilineHolder

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = RowListMultilineHolder(
                baseAdapter,
                RowListMultilineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    DIALOG { // ExpandableHolder, MultiLineHolder

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = DialogNodeHolder(
                baseAdapter,
                RowListDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    AVATAR { // AvatarHolder, MultiLineHolder

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = RowListAvatarHolder(
                baseAdapter,
                RowListAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    EXPANDABLE_SINGLELINE { // ExpandableHolder, SingleLineHolder

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = ExpandableSinglelineHolder(
                baseAdapter,
                RowListExpandableSinglelineBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        )
    },

    CHECKABLE {
        // ExpandableHolder, CheckableHolder, MultiLineHolder
        // RowListCheckableBinding

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = RowListCheckableHolder(
                baseAdapter,
                RowListCheckableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    UNSCHEDULED_TASK {
        // ExpandableHolder, MultiLineHolder, InvisibleCheckboxHolder
        // RowListExpandableMultilineBinding

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = TaskNode.Holder(
                baseAdapter,
                RowListExpandableMultilineBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        )
    },

    TASK_LIST_TASK {
        // ExpandableHolder, MultiLineHolder, InvisibleCheckboxHolder
        // RowListExpandableMultilineBinding

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = TaskListFragment.Holder(
                baseAdapter,
                RowListExpandableMultilineBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        )
    },

    ASSIGNED {
        // InvisibleCheckboxHolder
        // RowListAssignedBinding

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = AssignedNode.Holder(
                baseAdapter,
                RowListAssignedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    IMAGE {
        // none
        // RowListImageBinding

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = ImageNode.Holder(
                baseAdapter,
                RowListImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOTE {
        // InvisibleCheckboxHolder
        // RowListNoteBinding

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = NoteNode.Holder(
                baseAdapter,
                RowListNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    };

    abstract fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup): AbstractHolder
}