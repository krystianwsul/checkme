package com.krystianwsul.checkme.gui.tree.holders

import com.krystianwsul.checkme.databinding.RowListMultilineBinding
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineHolder

class MultilineHolder(
        override val baseAdapter: BaseAdapter,
        binding: RowListMultilineBinding,
) : AbstractHolder(binding.root), MultiLineHolder {

    override val rowContainer = binding.rowListMultilineContainer
    override val rowTextLayout = binding.rowListMultilineTextLayout
    override val rowName = binding.rowListMultilineName
    override val rowDetails = binding.rowListMultilineDetails
    override val rowChildren = binding.rowListMultilineChildren
    override val rowThumbnail = binding.rowListMultilineThumbnail
    override val rowSeparator = binding.rowListMultilineSeparator
    override val rowMarginEnd = binding.rowListMultilineMarginEnd
}