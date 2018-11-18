package com.krystianwsul.checkme.gui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyCrashlytics
import io.reactivex.disposables.CompositeDisposable

abstract class AbstractActivity : AppCompatActivity() {

    protected val createDisposable = CompositeDisposable()

    val onPostCreate = BehaviorRelay.create<Unit>()

    override fun onPostCreate(savedInstanceState: Bundle?) {
        MyCrashlytics.logMethod(this)

        super.onPostCreate(savedInstanceState)

        onPostCreate.accept(Unit)
    }

    override fun onResume() {
        MyCrashlytics.log(javaClass.simpleName + ".onResume")

        super.onResume()
    }

    override fun onDestroy() {
        createDisposable.dispose()

        super.onDestroy()
    }
}
