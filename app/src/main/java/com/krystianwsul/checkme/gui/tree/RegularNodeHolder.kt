package com.krystianwsul.checkme.gui.tree

import com.krystianwsul.checkme.databinding.RowListBinding

abstract class RegularNodeHolder(rowListBinding: RowListBinding) : NodeHolder(rowListBinding.root) {

    override val rowContainer = rowListBinding.rowContainer
    override val rowTextLayout = rowListBinding.rowTextLayout
    override val rowName = rowListBinding.rowName
    override val rowDetails = rowListBinding.rowDetails
    override val rowChildren = rowListBinding.rowChildren
    override val rowThumbnail = rowListBinding.rowThumbnail
    override val rowExpand = rowListBinding.rowExpand
    override val rowCheckBoxFrame = rowListBinding.rowCheckboxFrame
    override val rowCheckBox = rowListBinding.rowCheckbox
    override val rowMarginStart = rowListBinding.rowMargin
    override val rowImage = rowListBinding.rowImage
    override val rowBigImage = rowListBinding.rowBigImage
    override val rowBigImageLayout = rowListBinding.rowBigImageLayout
    override val rowSeparator = rowListBinding.rowSeparator
    override val rowChipGroup = rowListBinding.rowChipGroup
    override val rowMarginEnd = rowListBinding.rowMarginEnd
}