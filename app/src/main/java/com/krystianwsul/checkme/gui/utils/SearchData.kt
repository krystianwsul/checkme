package com.krystianwsul.checkme.gui.utils

import android.os.Parcelable
import com.krystianwsul.treeadapter.TreeViewAdapter
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchData(
        val query: String = "",
        val showDeleted: Boolean = false,
) : Parcelable, TreeViewAdapter.FilterCriteria {

    override val expandMatchingInstances get() = query.isNotEmpty()
}