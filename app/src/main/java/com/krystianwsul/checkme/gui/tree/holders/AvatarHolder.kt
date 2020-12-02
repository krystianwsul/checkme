package com.krystianwsul.checkme.gui.tree.holders

import com.krystianwsul.checkme.databinding.RowListAvatarBinding
import com.krystianwsul.checkme.gui.tree.AbstractHolder
import com.krystianwsul.checkme.gui.tree.BaseAdapter
import com.krystianwsul.checkme.gui.tree.delegates.avatar.AvatarHolder
import com.krystianwsul.checkme.gui.tree.delegates.multiline.MultiLineHolder

class AvatarHolder(override val baseAdapter: BaseAdapter, binding: RowListAvatarBinding) :
        AbstractHolder(binding.root),
        AvatarHolder,
        MultiLineHolder {

    override val rowTextLayout = binding.rowListAvatarTextLayout
    override val rowName = binding.rowListAvatarName
    override val rowDetails = binding.rowListAvatarDetails
    override val rowChildren = binding.rowListAvatarChildren
    override val rowImage = binding.rowListAvatarImage
    override val rowSeparator = binding.rowListAvatarSeparator
}