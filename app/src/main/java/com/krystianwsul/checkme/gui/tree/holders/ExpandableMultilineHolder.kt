package com.krystianwsul.checkme.gui.tree.holders

import com.krystianwsul.checkme.databinding.RowListExpandableMultilineBinding
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableHolder
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationHolder
import com.krystianwsul.checkme.gui.tree.delegates.invisible_checkbox.InvisibleCheckboxHolder
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineHolder
import com.krystianwsul.checkme.gui.tree.delegates.thumbnail.ThumbnailHolder

class ExpandableMultilineHolder(override val baseAdapter: BaseAdapter, binding: RowListExpandableMultilineBinding) :
        AbstractHolder(binding.root),
        ExpandableHolder,
        MultiLineHolder,
        InvisibleCheckboxHolder,
        ThumbnailHolder,
        IndentationHolder {

    override val rowContainer = binding.rowListExpandableMultilineContainer
    override val rowTextLayout = binding.rowListExpandableMultilineTextLayout
    override val rowName = binding.rowListExpandableMultilineName
    override val rowDetails = binding.rowListExpandableMultilineDetails
    override val rowChildren = binding.rowListExpandableMultilineChildren
    override val rowThumbnail = binding.rowListExpandableMultilineThumbnail
    override val rowExpand = binding.rowListExpandableMultilineExpand
    override val rowCheckBoxFrame = binding.rowListExpandableMultilineCheckboxInclude.rowCheckboxFrame
    override val rowMarginStart = binding.rowListExpandableMultilineMargin
    override val rowSeparator = binding.rowListExpandableMultilineSeparator

    override fun startRx() {
        super<AbstractHolder>.startRx()
        super<ExpandableHolder>.startRx()
    }
}