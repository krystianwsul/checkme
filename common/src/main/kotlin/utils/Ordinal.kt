package com.krystianwsul.common.utils

import org.gciatto.kt.math.BigDecimal
import org.gciatto.kt.math.MathContext
import org.gciatto.kt.math.RoundingMode

class Ordinal(_bigDecimal: BigDecimal) : Comparable<Ordinal> {

    companion object {

        private val mathContext = MathContext(100, RoundingMode.HALF_UP)

        private fun BigDecimal.toOrdinal() = Ordinal(this)

        private const val PAD_LENGTH = 50

        val ZERO by lazy { Ordinal(0) }
        val ONE by lazy { Ordinal(1) }

        fun fromFields(ordinalDouble: Double?, ordinalString: String?): Ordinal? {
            return ordinalString?.let(::fromJson) ?: ordinalDouble?.let(::BigDecimal)?.toOrdinal()
        }

        fun fromJson(ordinalString: String) = BigDecimal(ordinalString, mathContext).toOrdinal()
    }

    private val bigDecimal = _bigDecimal.setScale(mathContext.precision, mathContext.roundingMode)

    constructor(int: Int) : this(BigDecimal.of(int, mathContext))
    constructor(long: Long) : this(BigDecimal.of(long, mathContext))

    override fun hashCode() = bigDecimal.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other !is Ordinal) return false

        return bigDecimal == other.bigDecimal
    }

    override fun toString() = bigDecimal.toString()
        .trimEnd('0')
        .trimEnd('.')

    override fun compareTo(other: Ordinal) = bigDecimal.compareTo(other.bigDecimal)

    operator fun plus(augend: Ordinal) = bigDecimal.plus(augend.bigDecimal, mathContext).toOrdinal()
    operator fun plus(augend: Int) = plus(augend.toOrdinal())

    operator fun minus(subtrahend: Ordinal) = bigDecimal.minus(subtrahend.bigDecimal, mathContext).toOrdinal()
    operator fun minus(subtrahend: Int) = minus(subtrahend.toOrdinal())

    operator fun times(multiplicand: Ordinal) = bigDecimal.times(multiplicand.bigDecimal, mathContext).toOrdinal()
    operator fun times(multiplicand: Int) = times(multiplicand.toOrdinal())

    operator fun div(divisor: Ordinal) = bigDecimal.div(divisor.bigDecimal, mathContext)!!.toOrdinal()
    operator fun div(divisor: Int) = div(divisor.toOrdinal())

    fun toFields() = Pair(bigDecimal.toDouble(), toString())

    fun pow(n: Int) = bigDecimal.pow(n, mathContext)!!.toOrdinal()

    fun padded() = toString().let {
        var index = it.indexOf('.')
        check(index < PAD_LENGTH)

        if (index == -1) index = it.length

        "0".repeat(PAD_LENGTH - index) + it
    }

    fun isInt() = bigDecimal.setScale(0, RoundingMode.DOWN).compareTo(bigDecimal) == 0

    fun ceiling() = bigDecimal.setScale(0, RoundingMode.CEILING).toOrdinal()
}

fun Int.toOrdinal() = Ordinal(this)
fun Long.toOrdinal() = Ordinal(this)

fun Iterable<Ordinal>.sum(): Ordinal {
    var sum = Ordinal.ZERO
    for (element in this) {
        sum += element
    }
    return sum
}

fun Ordinal?.toFields() = this?.toFields() ?: Pair(null, null)