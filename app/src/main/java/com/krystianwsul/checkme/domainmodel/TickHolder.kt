package com.krystianwsul.checkme.domainmodel

object TickHolder {

    private var tickData: TickData? = null

    private fun mergeTickDatas(oldTickData: TickData, newTickData: TickData): TickData {
        oldTickData.release()
        newTickData.release()

        val silent = oldTickData.silent && newTickData.silent
        val source = "merged (${oldTickData.source}, ${newTickData.source})"

        val tickDatas = listOf(oldTickData, newTickData)

        return if (tickDatas.any { it is TickData.Lock }) {
            val lockTickDatas = tickDatas.filterIsInstance<TickData.Lock>()

            val expires = lockTickDatas.map { it.expires }.maxOrNull()!!
            val domainChanged = lockTickDatas.map { it.domainChanged }.maxOrNull()!!

            TickData.Lock(source, domainChanged, expires)
        } else {
            TickData.Normal(silent, source)
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