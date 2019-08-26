package com.krystianwsul.checkme.gui

import androidx.appcompat.app.AppCompatDialogFragment
import com.krystianwsul.checkme.MyCrashlytics
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

abstract class AbstractResultDialogFragment<T : Any> : AppCompatDialogFragment() {

    abstract val result: Observable<T>
}
