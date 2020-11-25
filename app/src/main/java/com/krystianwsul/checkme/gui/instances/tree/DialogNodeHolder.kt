package com.krystianwsul.checkme.gui.instances.tree

import android.widget.ImageView
import android.widget.RelativeLayout
import com.google.android.material.chip.ChipGroup
import com.krystianwsul.checkme.databinding.RowListDialogBinding

class DialogNodeHolder(rowListDialogBinding: RowListDialogBinding) : NodeHolder(rowListDialogBinding.root) {

    override val rowContainer = rowListDialogBinding.rowContainer
    override val rowTextLayout = rowListDialogBinding.rowTextLayout
    override val rowName = rowListDialogBinding.rowName
    override val rowDetails = rowListDialogBinding.rowDetails
    override val rowChildren = rowListDialogBinding.rowChildren
    override val rowThumbnail = rowListDialogBinding.rowThumbnail
    override val rowExpand = rowListDialogBinding.rowExpand
    override val rowCheckBoxFrame = rowListDialogBinding.rowCheckboxFrame
    override val rowCheckBox = rowListDialogBinding.rowCheckbox
    override val rowMargin = rowListDialogBinding.rowMargin
    override val rowImage: ImageView? = null
    override val rowBigImage: ImageView? = null
    override val rowBigImageLayout: RelativeLayout? = null
    override val rowSeparator = rowListDialogBinding.rowSeparator
    override val rowChipGroup: ChipGroup? = null
}