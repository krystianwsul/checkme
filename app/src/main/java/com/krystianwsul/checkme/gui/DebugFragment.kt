package com.krystianwsul.checkme.gui


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.widget.checkedChanges
import com.krystianwsul.checkme.*
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.notifications.TickJobIntentService
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_debug.*

class DebugFragment : AbstractFragment() {

    companion object {

        fun newInstance() = DebugFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_debug, container, false)!!

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        debugException.setOnClickListener {
            val i = 0
            @Suppress("DIVISION_BY_ZERO", "UNUSED_VARIABLE")
            val j = 1 / i
        }

        (if (MyApplication.instance.defaultRemote) debugDefaultRemote else debugDefaultLocal).isChecked = true

        debugDefaultRemote.checkedChanges()
                .subscribe { MyApplication.instance.defaultRemote = it }
                .addTo(viewCreatedDisposable)

        debugTick.setOnClickListener { TickJobIntentService.startServiceDebug(activity!!, "DebugFragment: TickService.startServiceDebug") }

        debugLoad.setOnClickListener {
            debugData.text = StringBuilder().apply {
                val lastTick = Preferences.lastTick
                val tickLog = Preferences.tickLog

                val lastTickExactTimeStamp = ExactTimeStamp(lastTick)

                val domainFactory = DomainFactory.instance

                append("local load time: ")
                append(domainFactory.localReadTimes.readMillis + domainFactory.localReadTimes.instantiateMillis)
                append("ms (")
                append(domainFactory.localReadTimes.readMillis)
                append(" + ")
                append(domainFactory.localReadTimes.instantiateMillis)
                append(")")

                append("\nremote load time: ")
                append(domainFactory.remoteReadTimes.readMillis + domainFactory.remoteReadTimes.instantiateMillis)
                append("ms (")
                append(domainFactory.remoteReadTimes.readMillis)
                append(" + ")
                append(domainFactory.remoteReadTimes.instantiateMillis)
                append(")")

                domainFactory.remoteUpdateTime?.let {
                    append("\nremote update time: ")
                    append(it)
                    append("ms")
                }

                append("\n\ntasks: ")
                append(domainFactory.localTaskCount)
                append("/")
                append(domainFactory.remoteTaskCount)
                append("\ninstances: ")
                append(domainFactory.localInstanceCount)
                append("/")
                append(domainFactory.remoteInstanceCount)
                append("\ncustom times: ")
                append(domainFactory.customTimeCount)
                append("\ninstance shown: ")
                append(domainFactory.instanceShownCount)

                val t1 = ExactTimeStamp.now
                DomainFactory.instance.getGroupListData(ExactTimeStamp.now, 0, MainActivity.TimeRange.DAY)
                val t2 = ExactTimeStamp.now

                append("\n\ntoday: ")
                append(t2.long - t1.long)
                append(" ms")

                append("\ncrashlytics enabled: ")
                append(MyCrashlytics.enabled)

                append("\n\nlast beeping tick: ")
                append(lastTickExactTimeStamp.toString())
                append("\n\ntick log:\n")
                append(tickLog)
            }
        }

        debugDiffButton.setOnClickListener { debugDiffText.text = DataDiff.diff }
    }
}
