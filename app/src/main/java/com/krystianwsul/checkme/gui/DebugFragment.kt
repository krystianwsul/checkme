package com.krystianwsul.checkme.gui


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.krystianwsul.checkme.DataDiff
import com.krystianwsul.checkme.MyCrashlytics
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
            val stringBuilder = StringBuilder()

            val sharedPreferences = activity!!.getSharedPreferences(TickJobIntentService.TICK_PREFERENCES, Context.MODE_PRIVATE)
            val lastTick = sharedPreferences.getLong(TickJobIntentService.LAST_TICK_KEY, -1)
            val tickLog = sharedPreferences.getString(TickJobIntentService.TICK_LOG, "")

            val lastTickExactTimeStamp = ExactTimeStamp(lastTick)

            stringBuilder.append("last beeping tick: ")
            stringBuilder.append(lastTickExactTimeStamp.toString())
            stringBuilder.append("\ntick log:\n")
            stringBuilder.append(tickLog)

            val domainFactory = DomainFactory.getDomainFactory()

            stringBuilder.append("\ndata load time: ")
            stringBuilder.append(domainFactory.readMillis + domainFactory.instantiateMillis)
            stringBuilder.append("ms (")
            stringBuilder.append(domainFactory.readMillis)
            stringBuilder.append(" + ")
            stringBuilder.append(domainFactory.instantiateMillis)
            stringBuilder.append(")")

            stringBuilder.append("\ntasks: ")
            stringBuilder.append(domainFactory.taskCount)
            stringBuilder.append(", instances: ")
            stringBuilder.append(domainFactory.instanceCount)
            stringBuilder.append(", custom times: ")
            stringBuilder.append(domainFactory.customTimeCount)

            val t1 = ExactTimeStamp.now
            DomainFactory.getDomainFactory().getGroupListData(activity!!, ExactTimeStamp.now, 0, MainActivity.TimeRange.DAY)
            val t2 = ExactTimeStamp.now

            stringBuilder.append("\ntoday: ")
            stringBuilder.append(t2.long - t1.long)
            stringBuilder.append(" ms")

            stringBuilder.append("\ncrashlytics enabled: ")
            stringBuilder.append(MyCrashlytics.enabled)

            debugData.text = stringBuilder
        }

        debugDiffButton.setOnClickListener { debugDiffText.text = DataDiff.diff }
    }
}
