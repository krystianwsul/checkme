package com.krystianwsul.checkme.gui.utils

import android.os.Parcelable
import com.krystianwsul.common.utils.QueryData
import com.krystianwsul.treeadapter.TreeViewAdapter
import kotlinx.parcelize.Parcelize

@Parcelize
data class SearchData(
        override val query: String = "",
        val showDeleted: Boolean = false,
) : Parcelable, TreeViewAdapter.FilterCriteria, QueryData {

    override val hasQuery get() = query.isNotEmpty()
}

fun SearchData?.orEmpty() = this ?: SearchData()