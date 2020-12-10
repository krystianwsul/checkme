package com.krystianwsul.checkme.gui.tree

import android.view.LayoutInflater
import android.view.ViewGroup
import com.krystianwsul.checkme.databinding.*
import com.krystianwsul.checkme.gui.edit.dialogs.DialogNodeHolder
import com.krystianwsul.checkme.gui.tree.holders.*

enum class HolderType {

    MULTILINE { // MultilineHolder

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = MultilineHolder(
                baseAdapter,
                RowListMultilineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    DIALOG { // ExpandableHolder, MultiLineHolder, IndentationHolder

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = DialogNodeHolder(
                baseAdapter,
                RowListDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    AVATAR { // AvatarHolder, MultiLineHolder

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = AvatarHolder(
                baseAdapter,
                RowListAvatarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    EXPANDABLE_SINGLELINE { // ExpandableHolder, SingleLineHolder, IndentationHolder

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = ExpandableSinglelineHolder(
                baseAdapter,
                RowListExpandableSinglelineBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        )
    },

    CHECKABLE { // ExpandableHolder, CheckableHolder, MultiLineHolder, ThumbnailHolder, IndentationHolder

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = CheckableHolder(
                baseAdapter,
                RowListCheckableBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    EXPANDABLE_MULTILINE {
        // ExpandableHolder, MultiLineHolder, InvisibleCheckboxHolder, ThumbnailHolder, IndentationHolder

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = ExpandableMultilineHolder(
                baseAdapter,
                RowListExpandableMultilineBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                )
        )
    },

    ASSIGNED { // none

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

    NOTE { // none

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = NoteNode.Holder(
                baseAdapter,
                RowListNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    },

    DETAILS { // InvisibleCheckboxHolder, IndentationHolder

        override fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup) = DetailsNode.Holder(
                baseAdapter,
                RowListDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    };

    abstract fun onCreateViewHolder(baseAdapter: BaseAdapter, parent: ViewGroup): AbstractHolder
}