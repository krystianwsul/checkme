package com.krystianwsul.checkme.gui.tree

import android.view.LayoutInflater
import android.view.ViewGroup
import com.krystianwsul.checkme.databinding.*
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimesFragment
import com.krystianwsul.checkme.gui.edit.dialogs.DialogNodeHolder
import com.krystianwsul.checkme.gui.friends.FriendListFragment
import com.krystianwsul.checkme.gui.friends.UserListFragment
import com.krystianwsul.checkme.gui.instances.tree.*
import com.krystianwsul.checkme.gui.projects.ProjectListFragment
import com.krystianwsul.checkme.gui.tasks.TaskListFragment

enum class NodeType {

    CUSTOM_TIME { // MultiLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = ShowCustomTimesFragment.Holder(
                baseAdapter,
                RowListMultilineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    PARENT_PICKER_TASK { // ExpandableDelegate, MultiLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = DialogNodeHolder(
                baseAdapter,
                RowListDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    FRIEND { // AvatarDelegate, MultiLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = FriendListFragment.Holder(
                baseAdapter,
                RowListAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    USER { // AvatarDelegate, MultiLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = UserListFragment.Holder(
                baseAdapter,
                RowListAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    DIVIDER { // ExpandableDelegate, SingleLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = DividerNode.Holder(
                baseAdapter,
                RowListExpandableSinglelineBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        )
    },

    DONE { // ExpandableDelegate, CheckableDelegate, MultiLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = DoneInstanceNode.Holder(
                baseAdapter,
                RowListCheckableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOT_DONE_GROUP { // ExpandableDelegate, CheckableDelegate, MultiLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = NotDoneGroupNode.Holder(
                baseAdapter,
                RowListCheckableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOT_DONE_INSTANCE { // ExpandableDelegate, CheckableDelegate, MultiLineDelegate

        override fun onCreateViewHolder(
                baseAdapter: BaseAdapter,
                parent: ViewGroup,
        ) = NotDoneGroupNode.NotDoneInstanceNode.Holder(
                baseAdapter,
                RowListCheckableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    UNSCHEDULED_TASK { // ExpandableDelegate, MultiLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = TaskNode.Holder(
                baseAdapter,
                RowListExpandableMultilineBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        )
    },

    UNSCHEDULED { // ExpandableDelegate, SingleLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = UnscheduledNode.Holder(
                baseAdapter,
                RowListExpandableSinglelineBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        )
    },

    PROJECT { // MultiLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = ProjectListFragment.Holder(
                baseAdapter,
                RowListMultilineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    TASK_LIST_TASK { // ExpandableDelegate, MultiLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = TaskListFragment.Holder(
                baseAdapter,
                RowListExpandableMultilineBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        )
    },

    ASSIGNED { // InvisibleCheckboxDelegate, MultiLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = AssignedNode.Holder(
                baseAdapter,
                RowListAssignedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    IMAGE { // none

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = ImageNode.Holder(
                baseAdapter,
                RowListImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOTE { // InvisibleCheckboxDelegate, MultiLineDelegate

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = NoteNode.Holder(
                baseAdapter,
                RowListNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    };

    abstract fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup): AbstractHolder
}