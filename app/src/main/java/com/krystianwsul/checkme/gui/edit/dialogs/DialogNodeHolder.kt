package com.krystianwsul.checkme.gui.edit.dialogs

import android.view.View
import com.krystianwsul.checkme.databinding.RowListDialogBinding
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.expandable.ExpandableHolder
import com.krystianwsul.checkme.gui.tree.multiline.MultiLineHolder

class DialogNodeHolder(
        override val baseAdapter: BaseAdapter,
        rowListDialogBinding: RowListDialogBinding,
) : AbstractHolder(rowListDialogBinding.root), ExpandableHolder, MultiLineHolder {

    override val rowContainer = rowListDialogBinding.rowListDialogContainer
    override val rowTextLayout = rowListDialogBinding.rowListDialogTextLayout
    override val rowName = rowListDialogBinding.rowListDialogName
    override val rowDetails = rowListDialogBinding.rowListDialogDetails
    override val rowChildren = rowListDialogBinding.rowListDialogChildren
    override val rowThumbnail = rowListDialogBinding.rowListDialogThumbnail
    override val rowExpand = rowListDialogBinding.rowListDialogExpand
    override val rowMarginStart = rowListDialogBinding.rowListDialogMargin
    override val rowSeparator = rowListDialogBinding.rowListDialogSeparator
    override val rowMarginEnd: View? = null

    override fun onViewAttachedToWindow() {
        super<AbstractHolder>.onViewAttachedToWindow()
        super<ExpandableHolder>.onViewAttachedToWindow()
    }
}