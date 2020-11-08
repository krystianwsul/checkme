package com.krystianwsul.checkme.gui.main


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
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.ticks.Ticker
import com.krystianwsul.checkme.utils.filterNotNull
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_debug.*

class DebugFragment : AbstractFragment() {

    companion object {

        fun newInstance() = DebugFragment()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_debug, container, false)!!

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DomainFactory.instanceRelay
                .filterNotNull()
                .subscribe {
                    debugViewSwitch.apply {
                        isChecked = it.debugMode

                        setOnCheckedChangeListener { _, isChecked ->
                            it.debugMode = isChecked
                        }
                    }
                }
                .addTo(viewCreatedDisposable)

        debugTick.setOnClickListener { Ticker.tick("DebugFragment") }

        debugLoad.setOnClickListener {
            debugData.text = StringBuilder().apply {
                val lastTick = Preferences.lastTick
                val tickLog = Preferences.tickLog.log

                val lastTickExactTimeStamp = ExactTimeStamp.Local(lastTick)

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

                val t1 = ExactTimeStamp.Local.now
                DomainFactory.instance.getGroupListData(ExactTimeStamp.Local.now, 0, MainActivity.TimeRange.DAY)
                val t2 = ExactTimeStamp.Local.now

                append("\n\ntoday: ")
                append(t2.long - t1.long)
                append(" ms")

                append("\ncrashlytics enabled: ")
                append(MyCrashlytics.enabled)

                append("\n\ntemporary notification log:\n\n")
                append(Preferences.temporaryNotificationLog.log)

                append("\n\nlast beeping tick: ")
                append(lastTickExactTimeStamp.toString())
                append("\n\ntick log:\n\n")
                append(tickLog)
            }
        }
    }
}
