package com.krystianwsul.checkme.domainmodel

object TickHolder {

    private var tickData: TickData? = null

    private fun mergeTickDatas(oldTickData: TickData, newTickData: TickData): TickData {
        val silent = oldTickData.silent && newTickData.silent

        val source = "merged (${oldTickData.source}, ${newTickData.source})"

        oldTickData.release()
        newTickData.release()

        val listeners = oldTickData.listeners + newTickData.listeners

        return TickData(silent, source, listeners, oldTickData.waitingForPrivate, oldTickData.waitingForShared)
    }

    private fun tryClearTickData() {
        if (tickData?.shouldClear == true)
            tickData = null
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
}