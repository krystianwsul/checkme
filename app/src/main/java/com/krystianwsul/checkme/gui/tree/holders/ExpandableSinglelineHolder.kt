package com.krystianwsul.checkme.gui.tree.holders

import com.krystianwsul.checkme.databinding.RowListExpandableSinglelineBinding
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.delegates.expandable.ExpandableHolder
import com.krystianwsul.checkme.gui.tree.delegates.indentation.IndentationHolder
import com.krystianwsul.checkme.gui.tree.delegates.singleline.SingleLineHolder

class ExpandableSinglelineHolder(
        override val baseAdapter: BaseAdapter,
        binding: RowListExpandableSinglelineBinding,
) : AbstractHolder(binding.root), ExpandableHolder, SingleLineHolder, IndentationHolder {

    override val rowContainer = binding.rowListExpandableSingleLineContainer
    override val rowText = binding.rowListExpandableSingleLineName
    override val rowExpand = binding.rowListExpandableSingleLineExpand
    override val rowExpandMargin = binding.rowListExpandableSingleLineExpandMargin
    override val rowSeparator = binding.rowListExpandableSingleLineSeparator

    override fun startRx() {
        super<AbstractHolder>.startRx()
        super<ExpandableHolder>.startRx()
    }
}