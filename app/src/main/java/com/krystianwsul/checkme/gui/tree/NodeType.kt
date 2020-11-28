package com.krystianwsul.checkme.gui.tree

import android.view.LayoutInflater
import android.view.ViewGroup
import com.krystianwsul.checkme.databinding.RowListBinding
import com.krystianwsul.checkme.databinding.RowListDialogBinding
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimesFragment
import com.krystianwsul.checkme.gui.edit.dialogs.DialogNodeHolder
import com.krystianwsul.checkme.gui.friends.FriendListFragment
import com.krystianwsul.checkme.gui.friends.UserListFragment
import com.krystianwsul.checkme.gui.instances.tree.*
import com.krystianwsul.checkme.gui.projects.ProjectListFragment
import com.krystianwsul.checkme.gui.tasks.TaskListFragment

enum class NodeType {

    CUSTOM_TIME {

        override fun onCreateViewHolder(parent: ViewGroup) = ShowCustomTimesFragment.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    PARENT_PICKER_TASK {

        override fun onCreateViewHolder(parent: ViewGroup) = DialogNodeHolder(
                RowListDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    FRIEND {

        override fun onCreateViewHolder(parent: ViewGroup) = FriendListFragment.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    USER {

        override fun onCreateViewHolder(parent: ViewGroup) = UserListFragment.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    DIVIDER {

        override fun onCreateViewHolder(parent: ViewGroup) = DividerNode.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    DONE {

        override fun onCreateViewHolder(parent: ViewGroup) = DoneInstanceNode.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOT_DONE_GROUP {

        override fun onCreateViewHolder(parent: ViewGroup) = NotDoneGroupNode.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOT_DONE_INSTANCE {

        override fun onCreateViewHolder(parent: ViewGroup) = NotDoneGroupNode.NotDoneInstanceNode.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    UNSCHEDULED_TASK {

        override fun onCreateViewHolder(parent: ViewGroup) = TaskNode.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    UNSCHEDULED {

        override fun onCreateViewHolder(parent: ViewGroup) = UnscheduledNode.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    PROJECT {

        override fun onCreateViewHolder(parent: ViewGroup) = ProjectListFragment.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    TASK_LIST_TASK {

        override fun onCreateViewHolder(parent: ViewGroup) = TaskListFragment.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    ASSIGNED {

        override fun onCreateViewHolder(parent: ViewGroup) = AssignedNode.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    IMAGE {

        override fun onCreateViewHolder(parent: ViewGroup) = ImageNode.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOTE {

        override fun onCreateViewHolder(parent: ViewGroup) = NoteNode.Holder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    };

    abstract fun onCreateViewHolder(parent: ViewGroup): BaseHolder
}