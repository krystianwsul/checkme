package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.models.Instance

fun <T : Any> combineSequencesGrouping(
        sequences: List<Sequence<T>>,
        selector: (List<T?>) -> List<Int>,
): Sequence<List<T>> {
    val sequenceHolders by lazy { sequences.map(::SequenceHolder) }

    return generateSequence {
        val nextValues = sequenceHolders.map { it.nextValue }

        if (nextValues.filterNotNull().isEmpty()) return@generateSequence null

        val nextIndices = selector(nextValues)
        check(nextValues.indices.toList().containsAll(nextIndices))

        val selectedSequenceHolders = nextIndices.map { sequenceHolders[it] }

        val nextValue = selectedSequenceHolders.map { it.nextValue!! }

        selectedSequenceHolders.forEach { it.getNext() }

        nextValue
    }
}

fun <T : Any> combineSequences(sequences: List<Sequence<T>>, selector: (List<T?>) -> Int): Sequence<T> {
    val sequenceHolders by lazy { sequences.map(::SequenceHolder) }

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
        bySchedule: Boolean = false
): Sequence<Instance<out T>> {
    return combineSequences(instanceSequences) {
        val finalPair = it.mapIndexed { index, instance ->
            instance?.let { Pair(it.getSequenceDate(bySchedule), it.task.ordinal) } to index
        }
                .filter { it.first != null }
                .minWithOrNull(compareBy({ it.first!!.first }, { it.first!!.second }))!!

        finalPair.second
    }
}

private data class SequenceHolder<T : Any>(private val sequence: Sequence<T>) {

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