package com.krystianwsul.checkme.gui

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.TickData
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.merge
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

abstract class AbstractActivity : AppCompatActivity() {

    companion object {

        private var snackbarData: SnackbarData? = null

        fun setSnackbar(snackbarData: SnackbarData) {
            check(this.snackbarData == null)

            SnackbarListener.hashes.add(snackbarData.hashCode())
            this.snackbarData = snackbarData
        }

        fun setSnackbar(taskUndoData: DomainFactory.TaskUndoData) = setSnackbar(TaskSnackbarData(taskUndoData))
    }

    protected val createDisposable = CompositeDisposable()
    private val resumeDisposable = CompositeDisposable()

    val started = BehaviorRelay.createDefault(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        MyCrashlytics.logMethod(this)

        super.onCreate(savedInstanceState)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)

        findViewById<ViewGroup>(android.R.id.content).getChildAt(0).setBackgroundColor(ContextCompat.getColor(this, R.color.materialBackground))
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
            .subscribeBy { DomainFactory.setFirebaseTickListener(SaveService.Source.SERVICE, TickData.Normal(true, source)) }

    protected open val tickOnResume = true

    override fun onResume() {
        MyCrashlytics.logMethod(this)

        super.onResume()

        snackbarData?.let {
            (this as? SnackbarListener)?.apply {
                val hash = hashCode()

                SnackbarListener.hashes.add(hash)
                anchor.addOneShotGlobalLayoutListener {
                    it.show(this)
                    SnackbarListener.hashes.remove(hash)
                }
            }

            SnackbarListener.hashes.remove(it.hashCode())
        }
        snackbarData = null

        if (tickOnResume)
            resumeDisposable += tick("AbstractActivity.onResume")
    }

    override fun onPause() {
        MyCrashlytics.logMethod(this)

        resumeDisposable.clear()

        super.onPause()
    }

    override fun onStop() {
        MyCrashlytics.logMethod(this)

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

    protected fun <T : Any> Observable<out AbstractResultDialogFragment<out T>>.show(tag: String) = show(supportFragmentManager, tag)

    @Suppress("UNCHECKED_CAST")
    private fun <T> FragmentManager.getFragmentObservable(tag: String): Observable<T> where T : Fragment = (findFragmentByTag(tag) as? T)?.let { Observable.just(it) }
            ?: Observable.never()

    private fun <T : Any> Observable<out AbstractResultDialogFragment<out T>>.show(fragmentManager: FragmentManager, tag: String): Observable<T> = listOf(
            map { it.apply { show(fragmentManager, tag) } },
            fragmentManager.getFragmentObservable(tag)
    ).merge().switchMap { it.result }
}
