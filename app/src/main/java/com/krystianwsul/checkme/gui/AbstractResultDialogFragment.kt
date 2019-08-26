package com.krystianwsul.checkme.gui

import io.reactivex.Observable

interface AbstractResultDialogFragment<T : Any> {

    val result: Observable<T>
}
