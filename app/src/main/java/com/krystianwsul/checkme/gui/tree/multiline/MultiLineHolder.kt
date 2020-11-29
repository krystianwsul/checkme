package com.krystianwsul.checkme.gui.tree.multiline

import android.widget.LinearLayout
import android.widget.TextView
import com.krystianwsul.checkme.gui.tree.BaseHolder

interface MultiLineHolder : BaseHolder {

    val rowTextLayout: LinearLayout
    val rowName: TextView
    val rowDetails: TextView
    val rowChildren: TextView
}