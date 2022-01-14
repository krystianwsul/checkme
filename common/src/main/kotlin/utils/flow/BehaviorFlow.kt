package com.krystianwsul.common.utils.flow

import com.krystianwsul.common.utils.singleOrEmpty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/*
    Use StateFlow when initial value is present, and you'd be using a distinctUntilChange afterwards.
    Also, don't try changing this to nullable T; valueOrNull and hasValue would fall apart.
 */
class BehaviorFlow<T : Any> private constructor(private val flow: MutableSharedFlow<T>) : Flow<T> by flow {

    companion object {

        operator fun <T : Any> invoke(initialValue: T? = null): BehaviorFlow<T> {
            val flow = MutableSharedFlow<T>(replay = 1)

            initialValue?.let(flow::tryEmit)

            return BehaviorFlow(flow)
        }
    }

    val valueOrNull get() = flow.replayCache.singleOrEmpty()

    var value
        get() = valueOrNull!!
        set(value) {
            flow.tryEmit(value)
        }

    val hasValue get() = valueOrNull != null
}