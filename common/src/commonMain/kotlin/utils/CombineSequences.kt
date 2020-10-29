package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.models.Instance

fun <T : Any> combineSequences(sequences: List<Sequence<T>>, selector: (List<T?>) -> Int): Sequence<T> {
    val sequenceHolders = sequences.map(::SequenceHolder)

    return generateSequence {
        val nextValues = sequenceHolders.map { it.nextValue }

        if (nextValues.filterNotNull().isEmpty()) return@generateSequence null

        val nextIndex = selector(nextValues)
        check(nextIndex in nextValues.indices)

        val selectedSequenceHolder = sequenceHolders[nextIndex]

        val nextValue = selectedSequenceHolder.nextValue!!
        selectedSequenceHolder.getNext()

        nextValue
    }
}

fun <T : ProjectType> combineInstanceSequences(
        instanceSequences: List<Sequence<Instance<out T>>>,
        bySchedule: Boolean
): Sequence<Instance<out T>> {
    return combineSequences(instanceSequences) {
        val finalPair = it.mapIndexed { index, instance -> instance?.getSequenceDate(bySchedule) to index }
                .filter { it.first != null }
                .minByOrNull { it.first!! }!!

        finalPair.second
    }
}

private data class SequenceHolder<T : Any>(
        private val sequence: Sequence<T>
) {

    private val iterator = sequence.iterator()

    var nextValue: T?

    init {
        nextValue = tryGetNextValue()
    }

    private fun tryGetNextValue(): T? = if (iterator.hasNext()) iterator.next() else null

    fun getNext() {
        checkNotNull(nextValue)

        nextValue = tryGetNextValue()
    }
}