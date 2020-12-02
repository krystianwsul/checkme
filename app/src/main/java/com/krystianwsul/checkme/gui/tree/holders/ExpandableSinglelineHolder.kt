package com.krystianwsul.checkme.gui.tree.holders

import com.krystianwsul.checkme.databinding.RowListExpandableSinglelineBinding
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableHolder
import com.krystianwsul.checkme.gui.tree.delegates.singleline.SingleLineHolder

class ExpandableSinglelineHolder(
        override val baseAdapter: BaseAdapter,
        binding: RowListExpandableSinglelineBinding,
) : AbstractHolder(binding.root), ExpandableHolder, SingleLineHolder {

    override val rowContainer = binding.rowListExpandableSingleLineContainer
    override val rowText = binding.rowListExpandableSingleLineName
    override val rowThumbnail = binding.rowListExpandableSingleLineThumbnail
    override val rowExpand = binding.rowListExpandableSingleLineExpand
    override val rowSeparator = binding.rowListExpandableSingleLineSeparator

    override fun onViewAttachedToWindow() {
        super<AbstractHolder>.onViewAttachedToWindow()
        super<ExpandableHolder>.onViewAttachedToWindow()
    }
}