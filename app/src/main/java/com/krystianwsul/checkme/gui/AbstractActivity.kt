package com.krystianwsul.checkme.gui

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.notifications.TickJobIntentService
import com.krystianwsul.checkme.persistencemodel.SaveService
import io.reactivex.disposables.CompositeDisposable

abstract class AbstractActivity : AppCompatActivity() {

    companion object {

        private var taskUndoData: DomainFactory.TaskUndoData? = null

        fun setSnackbar(taskUndoData: DomainFactory.TaskUndoData) {
            check(this.taskUndoData == null)

            this.taskUndoData = taskUndoData
        }
    }

    protected val createDisposable = CompositeDisposable()

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

    override fun onResume() {
        MyCrashlytics.logMethod(this)

        super.onResume()

        taskUndoData?.let {
            (this as SnackbarListener).showSnackbarRemoved(1) {
                DomainFactory.instance.clearTaskEndTimeStamps(0, SaveService.Source.GUI, it)
            }
        }
        taskUndoData = null

        TickJobIntentService.startServiceSilent(this, "AbstractActivity.onResume")
    }

    override fun onPause() {
        MyCrashlytics.logMethod(this)

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
}
