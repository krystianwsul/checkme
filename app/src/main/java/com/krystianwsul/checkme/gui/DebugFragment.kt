package com.krystianwsul.checkme.gui


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getGroupListData
import com.krystianwsul.checkme.notifications.TickJobIntentService
import com.krystianwsul.common.time.ExactTimeStamp
import kotlinx.android.synthetic.main.fragment_debug.*

class DebugFragment : AbstractFragment() {

    companion object {

        fun newInstance() = DebugFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_debug, container, false)!!

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        debugTick.setOnClickListener { TickJobIntentService.startServiceNormal(requireContext(), "DebugFragment") }

        debugLoad.setOnClickListener {
            debugData.text = StringBuilder().apply {
                val lastTick = Preferences.lastTick
                val tickLog = Preferences.tickLog.log

                val lastTickExactTimeStamp = ExactTimeStamp(lastTick)

                val domainFactory = DomainFactory.instance

                append("\nload time: ")
                append(domainFactory.remoteReadTimes.readMillis + domainFactory.remoteReadTimes.instantiateMillis)
                append("ms (")
                append(domainFactory.remoteReadTimes.readMillis)
                append(" + ")
                append(domainFactory.remoteReadTimes.instantiateMillis)
                append(")")

                append("\n\ntasks: ")
                append(domainFactory.taskCount)
                append("\nall existing instances: ")
                append(domainFactory.instanceCount)
                append("\nfirst page root instances: existing ")
                append(domainFactory.instanceInfo.first)
                append(", virtual ")
                append(domainFactory.instanceInfo.second)
                append("\ncustom times: ")
                append(domainFactory.customTimeCount)
                append("\ninstance shown: ")
                append(domainFactory.instanceShownCount)

                append("\n\n")
                append(Preferences.mainTabsLog.log)

                append("\n\n")
                append(Preferences.saveLog.log)

                val t1 = ExactTimeStamp.now
                DomainFactory.instance.getGroupListData(ExactTimeStamp.now, 0, MainActivity.TimeRange.DAY)
                val t2 = ExactTimeStamp.now

                append("\n\ntoday: ")
                append(t2.long - t1.long)
                append(" ms")

                append("\ncrashlytics enabled: ")
                append(MyCrashlytics.enabled)

                append("\n\ntemporary notification log:")
                append(Preferences.temporaryNotificationLog.log)

                append("\n\nlast beeping tick: ")
                append(lastTickExactTimeStamp.toString())
                append("\n\ntick log:\n")
                append(tickLog)
            }
        }
    }
}
