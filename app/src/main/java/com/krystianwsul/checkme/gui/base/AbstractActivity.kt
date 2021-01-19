package com.krystianwsul.checkme.gui.base

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.akexorcist.localizationactivity.core.LocalizationActivityDelegate
import com.akexorcist.localizationactivity.core.OnLocaleChangedListener
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.TickData
import com.krystianwsul.checkme.gui.utils.SnackbarData
import com.krystianwsul.checkme.gui.utils.TaskSnackbarData
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.common.domain.TaskUndoData
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*

abstract class AbstractActivity : AppCompatActivity(), OnLocaleChangedListener {

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

    val started = BehaviorRelay.createDefault(false)!!

    private val localizationDelegate = LocalizationActivityDelegate(this)

    override fun attachBaseContext(newBase: Context) {
        applyOverrideConfiguration(localizationDelegate.updateConfigurationLocale(newBase))
        super.attachBaseContext(newBase)
    }

    override fun getApplicationContext(): Context {
        return localizationDelegate.getApplicationContext(super.getApplicationContext())
    }

    override fun getResources(): Resources {
        return localizationDelegate.getResources(super.getResources())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        MyCrashlytics.logMethod(this)

        localizationDelegate.addOnLocaleChangedListener(this)
        localizationDelegate.onCreate()

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

        localizationDelegate.onResume(this)

        snackbarData?.let {
            (this as? SnackbarListener)?.apply {
                anchor.addOneShotGlobalLayoutListener { it.show(this) }
            }
        }
        snackbarData = null // shouldn't this be moved into `apply` or `addOneShotGlobalLayoutListener`?

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

    fun setLanguage(language: String) {
        localizationDelegate.setLanguage(this, language)
    }

    fun setLanguage(language: String, country: String) {
        localizationDelegate.setLanguage(this, language, country)
    }

    fun setLanguage(locale: Locale) {
        localizationDelegate.setLanguage(this, locale)
    }

    fun getCurrentLanguage(): Locale {
        return localizationDelegate.getLanguage(this)
    }

    override fun onBeforeLocaleChanged() {}

    override fun onAfterLocaleChanged() {}
}
