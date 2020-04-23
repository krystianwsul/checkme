package com.krystianwsul.checkme.domainmodel

object TickHolder {

    private var tickData: TickData? = null

    private fun mergeTickDatas(oldTickData: TickData, newTickData: TickData): TickData {
        oldTickData.release()
        newTickData.release()

        val silent = oldTickData.silent && newTickData.silent
        val source = "merged (${oldTickData.source}, ${newTickData.source})"

        val expires = listOf(oldTickData, newTickData).filterIsInstance<TickData.Lock>()
                .map { it.expires }
                .max()

        return expires?.let { TickData.Lock(silent, source, it) } ?: TickData.Normal(silent, source)
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