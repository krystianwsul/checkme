package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.checkme.domainmodel.update.DomainUpdater

object TickHolder {

    private var tickData: TickData? = null

    private fun mergeTickDatas(oldTickData: TickData, newTickData: TickData): TickData {
        oldTickData.release()
        newTickData.release()

        val notifierParams = DomainUpdater.NotifierParams.merge(
                listOf(oldTickData, newTickData).map { it.notifierParams },
        )!!

        val tickDatas = listOf(oldTickData, newTickData)

        return if (tickDatas.any { it is TickData.Lock }) {
            val lockTickDatas = tickDatas.filterIsInstance<TickData.Lock>()

            val expires = lockTickDatas.map { it.expires }.maxOrNull()!!
            val domainChanged = lockTickDatas.map { it.domainChanged }.maxOrNull()!!

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
        tickData = tickData?.let { mergeTickDatas(it, newTickData) } ?: newTickData
    }
}