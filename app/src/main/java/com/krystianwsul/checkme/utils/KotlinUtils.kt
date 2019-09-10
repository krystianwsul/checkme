package com.krystianwsul.checkme.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.Base64
import android.view.*
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.tabs.TabLayout
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.FirebaseWriteException
import com.krystianwsul.checkme.gui.MyBottomBar
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.json.OldestVisibleJson
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.Observable
import java.io.*
import java.util.*
import kotlin.collections.ArrayList

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

fun View.addOneShotScrollChangedListener(action: () -> Unit) = viewTreeObserver.addOnScrollChangedListener(object : ViewTreeObserver.OnScrollChangedListener {

    override fun onScrollChanged() {
        viewTreeObserver.removeOnScrollChangedListener(this)

        action()
    }
})

fun View.addOneShotPreDrawListener(action: () -> Unit) = viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {

    override fun onPreDraw(): Boolean {
        viewTreeObserver.removeOnPreDrawListener(this)

        action()

        return true
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

fun View.dpToPx(dp: Int): Float {
    val density = resources.displayMetrics.density
    return dp * density
}

fun Context.startTicks(receiver: BroadcastReceiver) {
    registerReceiver(receiver, IntentFilter(Intent.ACTION_TIME_TICK))
}

fun <T> Observable<NullableWrapper<T>>.filterNotNull() = filter { it.value != null }.map { it.value!! }!!

fun Task<Void>.checkError(domainFactory: DomainFactory, caller: String, values: Any? = null) {
    fun getTaskKeys() = Pair(ExactTimeStamp.now, domainFactory.remoteProjectFactory
            .remotePrivateProject
            .taskKeys)

    val taskKeysBefore = values?.let { getTaskKeys() }

    addOnCompleteListener {
        val message = "firebase write: $caller isCanceled: " + it.isCanceled + ", isComplete: " + it.isComplete + ", isSuccessful: " + it.isSuccessful + ", exception: " + it.exception + (values?.let { ", \nvalues: $values" }
                ?: "")
        if (it.isSuccessful) {
            MyCrashlytics.log(message)
        } else {
            val taskData = values?.let {
                val taskKeysAfter = getTaskKeys()
                ", \ntask keys before: $taskKeysBefore, \ntask keys after: $taskKeysAfter"
            } ?: ""
            MyCrashlytics.logException(FirebaseWriteException(message + taskData, it.exception))
        }
    }
}

val ViewGroup.children get() = ViewGroupChildrenIterable(this)

fun TabLayout.select(position: Int) = selectTab(getTabAt(position))

val Menu.items get() = MenuItemsIterable(this)

fun Toolbar.animateItems(itemVisibilities: List<Pair<Int, Boolean>>, replaceMenuHack: Boolean = false, onEnd: (() -> Unit)? = null) {
    if (replaceMenuHack) {
        fun getViews(ids: List<Int>) = ids.mapNotNull { findViewById<View>(it) }

        val hideItems = itemVisibilities.filterNot { it.second }.map { it.first }
        val hideViews = getViews(hideItems)

        animateVisibility(hide = hideViews, duration = MyBottomBar.duration) {
            hideItems.forEach { menu.findItem(it)?.isVisible = false }

            val showItems = itemVisibilities.filter { it.second }.map { it.first }.filter { menu.findItem(it)?.isVisible == false }
            showItems.forEach { menu.findItem(it)?.isVisible = true }

            //val showViews = getViews(showItems)
            //showViews.forEach { it.visibility = View.GONE }

            //animateVisibility(show = showViews, duration = MyBottomBar.duration, onEnd = onEnd)

            onEnd?.invoke()
        }
    } else {
        itemVisibilities.forEach { menu.findItem(it.first)?.isVisible = it.second }
        onEnd?.invoke()
    }
}

fun Context.normalizedId(@IdRes id: Int) = if (id == View.NO_ID) "NO_ID" else resources.getResourceName(id)!!

fun Calendar.toExactTimeStamp() = ExactTimeStamp(timeInMillis)

fun ImageView.loadPhoto(url: String?) = Glide.with(this)
        .load(url)
        .placeholder(R.drawable.ic_account_circle_black_24dp)
        .apply(RequestOptions.circleCropTransform())
        .into(this)

fun newUuid() = UUID.randomUUID().toString()

fun AutoCompleteTextView.fixClicks() = setOnTouchListener { _, _ -> false }

fun <T> serialize(obj: T): String {
    return ByteArrayOutputStream().let {
        ObjectOutputStream(it).run {
            writeObject(obj)
            close()
        }

        Base64.encodeToString(it.toByteArray(), Base64.DEFAULT).also { check(!it.isNullOrEmpty()) }
    }
}

fun <T> deserialize(serialized: String?): T? {
    if (serialized.isNullOrEmpty())
        return null

    return try {
        ObjectInputStream(ByteArrayInputStream(Base64.decode(serialized, Base64.DEFAULT))).run {
            @Suppress("UNCHECKED_CAST")
            val obj = readObject() as T
            close()

            obj
        }
    } catch (invalidClassException: InvalidClassException) {
        null
    } catch (e: Exception) {
        MyCrashlytics.logException(e)

        null
    }
}

fun Window.setTransparentNavigation(landscape: Boolean) {
    var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE

    if (landscape)
        navigationBarColor = ContextCompat.getColor(context, R.color.primaryColor12Solid)
    else
        flags = flags or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

    decorView.systemUiVisibility = decorView.systemUiVisibility or flags
}

val Resources.isLandscape get() = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

fun <T> RequestBuilder<T>.circle(circle: Boolean) = if (circle) apply(RequestOptions.circleCropTransform()) else this

fun OldestVisibleJson.Companion.fromDate(date: Date) = OldestVisibleJson(date.toJson(), date.year, date.month, date.day)

fun OldestVisibleJson.toDate() = date?.let { Date.fromJson(it) } ?: Date(year, month, day)