package com.krystianwsul.checkme.gui


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.krystianwsul.checkme.DataDiff
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
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
                val sharedPreferences = activity!!.getSharedPreferences(TickJobIntentService.TICK_PREFERENCES, Context.MODE_PRIVATE)
                val lastTick = sharedPreferences.getLong(TickJobIntentService.LAST_TICK_KEY, -1)
                val tickLog = sharedPreferences.getString(TickJobIntentService.TICK_LOG, "")

                val lastTickExactTimeStamp = ExactTimeStamp(lastTick)

                val kotlinDomainFactory = KotlinDomainFactory.getKotlinDomainFactory()

                append("last beeping tick: ")
                append(lastTickExactTimeStamp.toString())
                append("\ntick log:\n")
                append(tickLog)

                append("\ndata load time: ")
                append(kotlinDomainFactory.readMillis + kotlinDomainFactory.instantiateMillis)
                append("ms (")
                append(kotlinDomainFactory.readMillis)
                append(" + ")
                append(kotlinDomainFactory.instantiateMillis)
                append(")")

                append("\ntasks: ")
                append(kotlinDomainFactory.taskCount)
                append(", instances: ")
                append(kotlinDomainFactory.instanceCount)
                append(", custom times: ")
                append(kotlinDomainFactory.customTimeCount)

                val t1 = ExactTimeStamp.now
                KotlinDomainFactory.getKotlinDomainFactory().domainFactory.getGroupListData(ExactTimeStamp.now, 0, MainActivity.TimeRange.DAY)
                val t2 = ExactTimeStamp.now

                append("\ntoday: ")
                append(t2.long - t1.long)
                append(" ms")

                append("\ncrashlytics enabled: ")
                append(MyCrashlytics.enabled)
            }
        }

        debugDiffButton.setOnClickListener { debugDiffText.text = DataDiff.diff }
    }
}
