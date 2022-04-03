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

        val tracker = TimeLogger.startIfLogDone("combineSequences")

        check(nextIndex in nextValues.indices)

        val selectedSequenceHolder = sequenceHolders[nextIndex]

        val nextValue = selectedSequenceHolder.nextValue!!

        tracker?.stop()

        selectedSequenceHolder.getNext()

        nextValue
    }
}

data class InstanceInfo(val dateTime: DateTime, val instance: Instance?) {

    constructor(instance: Instance) : this(instance.instanceDateTime, instance)
}

fun Sequence<InstanceInfo>.toInstances() = mapNotNull { it.instance }

fun combineInstanceSequences(instanceSequences: List<Sequence<Instance>>): Sequence<Instance> = combineInstanceInfoSequences(
    instanceSequences.map {
        val tracker = TimeLogger.startIfLogDone("combineInstanceSequences.map 1")
        it.map { InstanceInfo(it.instanceDateTime, it) }.also { tracker?.stop() }
    }
).toInstances()

fun <T : Any> combineInstanceSequences(instanceSequences: List<Sequence<T>>, instanceGetter: (T) -> Instance): Sequence<T> {
    return combineInstanceInfoSequences(instanceSequences) {
        val tracker = TimeLogger.startIfLogDone("combineInstanceSequences.map 2")

        val instance = instanceGetter(it)
        InstanceInfo(instance.instanceDateTime, instance).also { tracker?.stop() }
    }
}

fun combineInstanceInfoSequences(instanceInfoSequences: List<Sequence<InstanceInfo>>): Sequence<InstanceInfo> =
    combineInstanceInfoSequences(instanceInfoSequences) { it }

fun <T : Any> combineInstanceInfoSequences(
    instanceInfoSequences: List<Sequence<T>>,
    instanceInfoGetter: (T) -> InstanceInfo,
): Sequence<T> {
    data class Entry(val element: T?, val index: Int)

    return combineSequences(instanceInfoSequences) {
        val tracker = TimeLogger.startIfLogDone("combineInstanceInfoSequences")

        val finalPair = it.mapIndexed { index, element -> Entry(element, index) }
            .filter { it.element != null }
            .map { it to instanceInfoGetter(it.element!!) }
            .minWithOrNull(
                compareBy(
                    { it.second.dateTime },
                    { it.second.instance?.ordinal },
                )
            )!!

        finalPair.first
            .index
            .also { tracker?.stop() }
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