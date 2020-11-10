package com.krystianwsul.checkme.gui.utils

import android.os.Parcelable
import com.krystianwsul.treeadapter.TreeViewAdapter
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SearchData(val query: String, val showDeleted: Boolean) : Parcelable, TreeViewAdapter.FilterCriteria {

    override val hasQuery get() = query.isNotEmpty()
}