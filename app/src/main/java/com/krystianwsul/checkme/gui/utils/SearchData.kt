package com.krystianwsul.checkme.gui.utils

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SearchData(val query: String, val showDeleted: Boolean) : Parcelable