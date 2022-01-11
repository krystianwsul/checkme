package com.krystianwsul.common.utils.flow

import com.krystianwsul.common.utils.singleOrEmpty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class BehaviorFlow<T> private constructor(private val flow: MutableSharedFlow<T>) : Flow<T> by flow {

    companion object {

        operator fun <T> invoke(initialValue: T? = null): BehaviorFlow<T> {
            val flow = MutableSharedFlow<T>(replay = 1)

            initialValue?.let(flow::tryEmit)

            return BehaviorFlow(flow)
        }
    }

    val valueOrNull get() = flow.replayCache.singleOrEmpty()

    val value get() = valueOrNull!!

    val hasValue get() = valueOrNull != null

    fun accept(value: T) {
        flow.tryEmit(value)
    }
}