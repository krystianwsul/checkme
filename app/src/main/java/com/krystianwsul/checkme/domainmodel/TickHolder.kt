package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.utils.time.ExactTimeStamp

object TickHolder {

    private var tickData: TickData? = null

    private fun mergeTickDatas(oldTickData: TickData, newTickData: TickData): TickData {
        val silent = oldTickData.silent && newTickData.silent

        val source = "merged (${oldTickData.source}, ${newTickData.source})"

        oldTickData.releaseWakelock()
        newTickData.releaseWakelock()

        val listeners = oldTickData.listeners + newTickData.listeners

        return TickData(silent, source, listeners, oldTickData.privateRefreshed || newTickData.privateRefreshed, oldTickData.sharedRefreshed || newTickData.sharedRefreshed)
    }

    private fun tryClearTickData() {
        Preferences.logLineHour("TickHolder.tryClearTickData: tickData $tickData")
        tickData?.let {
            if (it.expires < ExactTimeStamp.now || !it.wakelock.isHeld) {
                Preferences.logLineHour("TickHolder.tryClearTickData: tickData expired, clearing")
                tickData = null
            } else {
                Preferences.logLineHour("TickHolder.tryClearTickData: tickData not expired")
            }
        }
    }

    fun getTickData(): TickData? {
        tryClearTickData()
        return tickData
    }

    fun addTickData(newTickData: TickData) {
        tickData = if (tickData != null) {
            mergeTickDatas(tickData!!, newTickData)
        } else {
            newTickData
        }
    }

    val isHeld get() = tickData?.wakelock?.isHeld == true
}