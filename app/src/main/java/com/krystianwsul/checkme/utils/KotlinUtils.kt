package com.krystianwsul.checkme.utils

import android.view.View
import android.view.ViewTreeObserver
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.utils.time.DayOfWeek

fun Set<DayOfWeek>.prettyPrint(): String {
    check(isNotEmpty())

    if (size == 7)
        return MyApplication.instance.getString(R.string.daily) + ", "

    if (size == 1)
        return single().toString() + ", "

    val ranges = KotlinUtils.getRanges(this.toList())

    return ranges.joinToString(", ") {
        check(it.isNotEmpty())

        when (it.size) {
            1 -> it.single().toString()
            2 -> it.first().toString() + ", " + it.last().toString()
            else -> it.first().toString() + " - " + it.last().toString()
        }
    } + ": "
}

fun View.addOneShotGlobalLayoutListener(action: () -> Unit) = viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {

    override fun onGlobalLayout() {

        @Suppress("DEPRECATION")
        viewTreeObserver.removeGlobalOnLayoutListener(this)

        action()
    }
})


object KotlinUtils {

    fun getRanges(list: List<DayOfWeek>): List<List<DayOfWeek>> {
        return getRanges(list.sorted(), { x, y ->
            check(x.ordinal < y.ordinal)

            x.ordinal + 1 < y.ordinal
        })
    }

    fun <T> getRanges(list: List<T>, shouldSplit: (T, T) -> Boolean): List<List<T>> {
        val copy = ArrayList(list)

        val ranges = mutableListOf<List<T>>()
        while (copy.isNotEmpty()) {
            val nextRange = getNextRange(copy, shouldSplit)
            copy.removeAll(nextRange)
            ranges.add(nextRange)
        }

        return ranges
    }

    private fun <T> getNextRange(list: List<T>, shouldSplit: (T, T) -> Boolean): List<T> {
        if (list.size < 2)
            return ArrayList(list)

        var previous = list.first()

        var i = 1
        while (i < list.size) {
            val next = list[i]

            if (shouldSplit(previous, next))
                break

            previous = next

            i++
        }

        return ArrayList(list.subList(0, i))
    }
}
