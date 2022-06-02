package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.notifications.Notifier

object TickHolder {

    private var tickData: TickData? = null

    private fun mergeTickDatas(oldTickData: TickData, newTickData: TickData): TickData {
        oldTickData.release()
        newTickData.release()

        val notifierParams = Notifier.Params.merge(
            listOf(oldTickData, newTickData).map { it.notifierParams },
        )!!

        val tickDatas = listOf(oldTickData, newTickData)

        val lockTickDatas = tickDatas.filterIsInstance<TickData.Lock>()

        return if (lockTickDatas.isNotEmpty()) {
            val expires = lockTickDatas.maxOf { it.expires }
            val domainChanged = lockTickDatas.maxOf { it.domainChanged }

            TickData.Lock(notifierParams, domainChanged, expires)
        } else {
            TickData.Normal(notifierParams)
        }
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
        tryClearTickData()

        tickData = tickData?.let { mergeTickDatas(it, newTickData) } ?: newTickData
    }
}