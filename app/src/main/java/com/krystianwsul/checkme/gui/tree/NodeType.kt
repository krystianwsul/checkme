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

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = ShowCustomTimesFragment.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    PARENT_PICKER_TASK {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = DialogNodeHolder(
                baseAdapter,
                RowListDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    FRIEND {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = FriendListFragment.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    USER {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = UserListFragment.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    DIVIDER {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = DividerNode.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    DONE {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = DoneInstanceNode.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOT_DONE_GROUP {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = NotDoneGroupNode.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOT_DONE_INSTANCE {

        override fun onCreateViewHolder(
                baseAdapter: BaseAdapter,
                parent: ViewGroup,
        ) = NotDoneGroupNode.NotDoneInstanceNode.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    UNSCHEDULED_TASK {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = TaskNode.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    UNSCHEDULED {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = UnscheduledNode.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    PROJECT {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = ProjectListFragment.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    TASK_LIST_TASK {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = TaskListFragment.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    ASSIGNED {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = AssignedNode.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    IMAGE {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = ImageNode.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOTE {

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = NoteNode.Holder(
                baseAdapter,
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    };

    abstract fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup): AbstractHolder
}