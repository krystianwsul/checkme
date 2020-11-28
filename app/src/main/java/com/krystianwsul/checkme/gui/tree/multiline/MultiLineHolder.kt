package com.krystianwsul.checkme.gui.tree.multiline

import android.widget.LinearLayout
import android.widget.TextView
import io.reactivex.disposables.CompositeDisposable

interface MultiLineHolder {

    val rowTextLayout: LinearLayout
    val rowName: TextView
    val rowDetails: TextView
    val rowChildren: TextView

    val compositeDisposable: CompositeDisposable // todo delegate
}