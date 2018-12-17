package com.krystianwsul.checkme.gui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyCrashlytics
import io.reactivex.disposables.CompositeDisposable

abstract class AbstractActivity : AppCompatActivity() {

    protected val createDisposable = CompositeDisposable()

    val onPostCreate = BehaviorRelay.create<Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        MyCrashlytics.logMethod(this)

        super.onCreate(savedInstanceState)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        MyCrashlytics.logMethod(this)

        super.onPostCreate(savedInstanceState)

        onPostCreate.accept(Unit)
    }

    override fun onResume() {
        MyCrashlytics.logMethod(this)

        super.onResume()
    }

    override fun onPause() {
        MyCrashlytics.logMethod(this)

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        MyCrashlytics.logMethod(this)

        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        MyCrashlytics.logMethod(this)

        createDisposable.dispose()

        super.onDestroy()
    }
}
