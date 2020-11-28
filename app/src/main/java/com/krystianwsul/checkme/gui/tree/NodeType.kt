package com.krystianwsul.checkme.gui.tree

import android.view.LayoutInflater
import android.view.ViewGroup
import com.krystianwsul.checkme.databinding.RowListBinding
import com.krystianwsul.checkme.databinding.RowListDialogBinding
import com.krystianwsul.checkme.gui.edit.dialogs.DialogNodeHolder

enum class NodeType {

    CUSTOM_TIME {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    PARENT_PICKER_TASK {

        override fun onCreateViewHolder(parent: ViewGroup) = DialogNodeHolder(
                RowListDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    FRIEND {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    USER {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    DIVIDER {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    DONE {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOT_DONE_GROUP {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOT_DONE_INSTANCE {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    UNSCHEDULED_TASK {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    UNSCHEDULED {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    PROJECT {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    TASK_LIST_TASK {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    ASSIGNED {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    IMAGE {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    NOTE {

        override fun onCreateViewHolder(parent: ViewGroup) = RegularNodeHolder(
                RowListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    };

    abstract fun onCreateViewHolder(parent: ViewGroup): NodeHolder
}