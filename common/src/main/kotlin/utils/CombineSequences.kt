package com.krystianwsul.common.utils

import com.krystianwsul.common.firebase.models.Instance
import com.krystianwsul.common.time.DateTime

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

fun combineInstanceSequences(instanceSequences: List<Sequence<Instance>>): Sequence<Instance> {
    data class InstanceInfo(val dateTime: DateTime, val ordinal: Double)
    data class Entry(val instanceInfo: InstanceInfo?, val index: Int)

    return combineSequences(instanceSequences) {
        val finalPair = it.mapIndexed { index, instance ->
            Entry(instance?.let { InstanceInfo(it.instanceDateTime, it.task.ordinal) }, index)
        }
            .filter { it.instanceInfo != null }
            .minWithOrNull(compareBy({ it.instanceInfo!!.dateTime }, { it.instanceInfo!!.ordinal }))!!

        finalPair.index
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