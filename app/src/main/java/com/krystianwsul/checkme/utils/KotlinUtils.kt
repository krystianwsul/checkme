package com.krystianwsul.checkme.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.view.ViewTreeObserver
import com.google.android.gms.tasks.Task
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.firebase.FirebaseWriteException
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import io.reactivex.Observable

fun Set<DayOfWeek>.prettyPrint(): String {
    check(isNotEmpty())

    if (size == 7)
        return MyApplication.instance.getString(R.string.daily) + ", "

    if (size == 1)
        return single().toString() + ", "

    val ranges = getRanges(this.toList())

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
        viewTreeObserver.removeOnGlobalLayoutListener(this)

        action()
    }
})

fun getRanges(list: List<DayOfWeek>) = getRanges(list.sorted()) { x, y ->
    check(x.ordinal < y.ordinal)

    x.ordinal + 1 < y.ordinal
}

private fun <T> getRanges(list: List<T>, shouldSplit: (T, T) -> Boolean): List<List<T>> {
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

fun View.setIndent(indent: Int) = setPadding((indent * 48 * context.resources.displayMetrics.density + 0.5f).toInt(), 0, 0, 0)

fun <T> removeFromGetter(getter: () -> List<T>, action: (T) -> Unit) {
    var list = getter()

    do {
        action(list.first())
        list = getter()
    } while (list.isNotEmpty())
}

fun Context.dpToPx(dp: Int): Float {
    val density = resources.displayMetrics.density
    return dp * density
}

fun Context.startTicks(receiver: BroadcastReceiver) {
    registerReceiver(receiver, IntentFilter(Intent.ACTION_TIME_TICK))
}

fun <T> Observable<NullableWrapper<T>>.filterNotNull() = filter { it.value != null }.map { it.value!! }

fun Task<Void>.checkError(caller: String, values: Any? = null) {
    addOnCompleteListener {
        val message = "firebase write: $caller isCanceled: " + it.isCanceled + ", isComplete: " + it.isComplete + ", isSuccessful: " + it.isSuccessful + ", exception: " + it.exception + (values?.let { ", values: $values" }
                ?: "")
        if (it.isSuccessful)
            MyCrashlytics.log(message)
        else
            MyCrashlytics.logException(FirebaseWriteException(message, it.exception))
    }
}
