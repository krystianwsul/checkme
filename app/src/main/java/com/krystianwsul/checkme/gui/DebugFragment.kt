package com.krystianwsul.checkme.gui


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.krystianwsul.checkme.DataDiff
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.notifications.TickJobIntentService
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import kotlinx.android.synthetic.main.fragment_debug.*

class DebugFragment : AbstractFragment() {

    companion object {

        fun newInstance() = DebugFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_debug, container, false)!!

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        debugException.setOnClickListener {
            val i = 0
            @Suppress("DIVISION_BY_ZERO", "UNUSED_VARIABLE")
            val j = 1 / i
        }

        debugTick.setOnClickListener { TickJobIntentService.startServiceDebug(activity!!, "DebugFragment: TickService.startServiceDebug") }

        debugLoad.setOnClickListener {
            debugData.text = StringBuilder().apply {
                val lastTick = Preferences.lastTick
                val tickLog = Preferences.tickLog

                val lastTickExactTimeStamp = ExactTimeStamp(lastTick)

                val kotlinDomainFactory = DomainFactory.instance

                append("local load time: ")
                append(kotlinDomainFactory.localReadTimes.readMillis + kotlinDomainFactory.localReadTimes.instantiateMillis)
                append("ms (")
                append(kotlinDomainFactory.localReadTimes.readMillis)
                append(" + ")
                append(kotlinDomainFactory.localReadTimes.instantiateMillis)
                append(")")

                append("\nremote load time: ")
                append(kotlinDomainFactory.remoteReadTimes.readMillis + kotlinDomainFactory.remoteReadTimes.instantiateMillis)
                append("ms (")
                append(kotlinDomainFactory.remoteReadTimes.readMillis)
                append(" + ")
                append(kotlinDomainFactory.remoteReadTimes.instantiateMillis)
                append(")")

                append("\n\ntasks: ")
                append(kotlinDomainFactory.localTaskCount)
                append("/")
                append(kotlinDomainFactory.remoteTaskCount)
                append("\ninstances: ")
                append(kotlinDomainFactory.localInstanceCount)
                append("/")
                append(kotlinDomainFactory.remoteInstanceCount)
                append("\ncustom times: ")
                append(kotlinDomainFactory.customTimeCount)
                append("\ninstance shown: ")
                append(kotlinDomainFactory.instanceShownCount)

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
