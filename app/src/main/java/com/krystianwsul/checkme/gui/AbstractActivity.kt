package com.krystianwsul.checkme.gui

import android.support.v7.app.AppCompatActivity

import com.krystianwsul.checkme.MyCrashlytics

import io.reactivex.disposables.CompositeDisposable

abstract class AbstractActivity : AppCompatActivity() {

    protected var createDisposable = CompositeDisposable()

    override fun onResume() {
        MyCrashlytics.log(javaClass.simpleName + ".onResume")

        super.onResume()
    }

    override fun onDestroy() {
        createDisposable.dispose()

        super.onDestroy()
    }
}
