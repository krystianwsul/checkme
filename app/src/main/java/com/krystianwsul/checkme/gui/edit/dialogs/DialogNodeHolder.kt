package com.krystianwsul.checkme.gui.edit.dialogs

import com.krystianwsul.checkme.databinding.RowListDialogBinding
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableHolder
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationHolder
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineHolder

class DialogNodeHolder(
        override val baseAdapter: BaseAdapter,
        rowListDialogBinding: RowListDialogBinding,
) : AbstractHolder(rowListDialogBinding.root), ExpandableHolder, MultiLineHolder, IndentationHolder {

    override val rowContainer = rowListDialogBinding.rowListDialogContainer
    override val rowTextLayout = rowListDialogBinding.rowListDialogTextLayout
    override val rowName = rowListDialogBinding.rowListDialogName
    override val rowDetails = rowListDialogBinding.rowListDialogDetails
    override val rowChildren = rowListDialogBinding.rowListDialogChildren
    override val rowExpand = rowListDialogBinding.rowListDialogExpand
    override val rowExpandMargin = rowListDialogBinding.rowListDialogExpandMargin
    override val rowSeparator = rowListDialogBinding.rowListDialogSeparator

    override fun startRx() {
        super<AbstractHolder>.startRx()
        super<ExpandableHolder>.startRx()
    }
}