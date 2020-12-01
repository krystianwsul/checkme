package com.krystianwsul.checkme.gui.tree.holders

import com.krystianwsul.checkme.databinding.RowListCheckableBinding
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.delegates.checkable.CheckableHolder
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableHolder
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineHolder

class RowListCheckableHolder(
        override val baseAdapter: BaseAdapter,
        binding: RowListCheckableBinding,
) : AbstractHolder(binding.root), ExpandableHolder, CheckableHolder, MultiLineHolder {

    override val rowContainer = binding.rowListCheckableContainer
    override val rowTextLayout = binding.rowListCheckableTextLayout
    override val rowName = binding.rowListCheckableName
    override val rowDetails = binding.rowListCheckableDetails
    override val rowChildren = binding.rowListCheckableChildren
    override val rowThumbnail = binding.rowListCheckableThumbnail
    override val rowExpand = binding.rowListCheckableExpand
    override val rowCheckBoxFrame = binding.rowListCheckableCheckboxInclude.rowCheckboxFrame
    override val rowCheckBox = binding.rowListCheckableCheckboxInclude.rowCheckbox
    override val rowMarginStart = binding.rowListCheckableMargin
    override val rowSeparator = binding.rowListCheckableSeparator
    override val rowMarginEnd = binding.rowListCheckableMarginEnd

    override fun onViewAttachedToWindow() {
        super<AbstractHolder>.onViewAttachedToWindow()
        super<ExpandableHolder>.onViewAttachedToWindow()
        super<CheckableHolder>.onViewAttachedToWindow()
    }
}