package com.krystianwsul.checkme.gui.base

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.TickData
import com.krystianwsul.checkme.gui.utils.SnackbarData
import com.krystianwsul.checkme.gui.utils.TaskSnackbarData
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.common.domain.TaskUndoData
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

abstract class AbstractActivity : AppCompatActivity() {

    companion object {

        private var snackbarData: SnackbarData? = null

        fun setSnackbar(snackbarData: SnackbarData) {
            check(Companion.snackbarData == null)

            Companion.snackbarData = snackbarData
        }

        fun setSnackbar(taskUndoData: TaskUndoData) = setSnackbar(TaskSnackbarData(taskUndoData))
    }

    protected val createDisposable = CompositeDisposable()
    protected val startDisposable = CompositeDisposable()
    private val resumeDisposable = CompositeDisposable()

    val started = BehaviorRelay.createDefault(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        MyCrashlytics.logMethod(this)

        super.onCreate(savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        MyCrashlytics.logMethod(this)

        super.onNewIntent(intent)
    }

    override fun onStart() {
        MyCrashlytics.logMethod(this)

        super.onStart()

        started.accept(true)
    }

    private fun tick(source: String) = Single.just(Unit)
            .observeOn(Schedulers.single())
            .subscribeBy {
                DomainFactory.setFirebaseTickListener(SaveService.Source.SERVICE, TickData.Normal(true, source))
            }
            .addTo(createDisposable)

    protected open val tickOnResume = true

    override fun onResume() {
        MyCrashlytics.logMethod(this)

        super.onResume()

        snackbarData?.let {
            (this as? SnackbarListener)?.apply {
                anchor.addOneShotGlobalLayoutListener { it.show(this) }
            }
        }
        snackbarData = null

        if (tickOnResume) resumeDisposable += tick("AbstractActivity.onResume")
    }

    override fun onPause() {
        MyCrashlytics.logMethod(this)

        resumeDisposable.clear()

        super.onPause()
    }

    override fun onStop() {
        MyCrashlytics.logMethod(this)

        startDisposable.clear()

        started.accept(false)

        super.onStop()
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
