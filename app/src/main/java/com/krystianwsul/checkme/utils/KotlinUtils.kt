package com.krystianwsul.checkme.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.SearchManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Base64
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.gms.tasks.Task
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.FirebaseWriteException
import com.krystianwsul.checkme.gui.base.AbstractActivity
import com.krystianwsul.checkme.gui.widgets.MyBottomBar
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.common.firebase.DatabaseCallback
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import java.io.*
import java.util.*
import kotlin.collections.ArrayList

fun Set<DayOfWeek>.prettyPrint(): String {
    check(isNotEmpty())

    if (size == 7)
        return MyApplication.instance.getString(R.string.daily) + ", "

    if (size == 1)
        return single().toString() + ", "

    val ranges = getRanges(toList())

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

fun View.setIndent(indent: Int) = updatePadding((indent * 48 * context.resources.displayMetrics.density + 0.5f).toInt())

fun <T> removeFromGetter(getter: () -> List<T>, action: (T) -> Unit) {
    var list = getter()

    do {
        action(list.first())
        list = getter()
    } while (list.isNotEmpty())
}

fun Resources.dpToPx(dp: Int): Float {
    val density = displayMetrics.density
    return dp * density
}

fun Context.dpToPx(dp: Int) = resources.dpToPx(dp)

fun View.dpToPx(dp: Int) = resources.dpToPx(dp)

fun Resources.pxToDp(px: Int): Float {
    val density = displayMetrics.density
    return px / density
}

fun View.pxToDp(px: Int) = resources.pxToDp(px)

fun Context.startTicks(receiver: BroadcastReceiver) {
    registerReceiver(receiver, IntentFilter(Intent.ACTION_TIME_TICK))
}

fun Context.startDate(receiver: BroadcastReceiver) {
    registerReceiver(receiver, IntentFilter(Intent.ACTION_DATE_CHANGED))
}

fun <T> Observable<NullableWrapper<T>>.filterNotNull() =
    filter { it.value != null }.map { it.value!! }!!

private typealias TaskKeys = Pair<ExactTimeStamp, Set<String>>

private fun DomainFactory.getTaskKeys(): TaskKeys = Pair(
        ExactTimeStamp.Local.now,
        projectsFactory.privateProject.taskIds
)

private fun onComplete(
        domainFactory: DomainFactory,
        caller: String,
        values: Any?,
        taskKeysBefore: TaskKeys?,
        databaseMessage: String?,
        successful: Boolean,
        exception: Exception?
) {
    val message = "firebase write: $caller $databaseMessage " + (values?.let { ", \nvalues: $values" }
            ?: "")
    if (successful) {
        MyCrashlytics.log(message)
    } else {
        val taskData = values?.let {
            val taskKeysAfter = domainFactory.getTaskKeys()
            ", \ntask keys before: $taskKeysBefore, \ntask keys after: $taskKeysAfter"
        } ?: ""
        MyCrashlytics.logException(FirebaseWriteException(message + taskData, exception))
    }
}

fun checkError(domainFactory: DomainFactory, caller: String, values: Map<String, Any?>? = null): DatabaseCallback {
    val taskKeysBefore = values?.let { domainFactory.getTaskKeys() }

    return { databaseMessage, successful, exception ->
        onComplete(domainFactory, caller, values, taskKeysBefore, databaseMessage, successful, exception)
    }
}

fun Task<Void>.getMessage() = "isCanceled: $isCanceled, isComplete: $isComplete, isSuccessful: $isSuccessful, exception: $exception"

val ViewGroup.children get() = ViewGroupChildrenIterable(this)

fun TabLayout.select(position: Int) = selectTab(getTabAt(position))

val Menu.items get() = MenuItemsIterable(this)

fun Toolbar.animateItems(itemVisibilities: List<Pair<Int, Boolean>>, replaceMenuHack: Boolean = false, onEnd: (() -> Unit)? = null) {
    if (replaceMenuHack) {
        fun getViews(ids: List<Int>) = ids.mapNotNull {
            @Suppress("RemoveExplicitTypeArguments")
            findViewById<View>(it)
        }

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

fun ImageView.loadPhoto(url: String?) = Glide.with(this)
        .load(url)
        .placeholder(R.drawable.ic_account_circle_black_24dp)
        .apply(RequestOptions.circleCropTransform())
        .into(this)

fun Chip.loadPhoto(url: String?) {
    setChipIconResource(R.drawable.ic_account_circle_black_24dp)

    Glide.with(this)
            .load(url)
            .apply(RequestOptions.circleCropTransform())
            .listener(object : RequestListener<Drawable> {

                override fun onResourceReady(
                        resource: Drawable,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean,
                ): Boolean {
                    chipIcon = resource

                    return false
                }

                override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean,
                ) = false
            })
            .preload()
}

fun newUuid() = UUID.randomUUID().toString()

fun <T : Serializable> serialize(obj: T): String {
    return ByteArrayOutputStream().let {
        ObjectOutputStream(it).run {
            writeObject(obj)
            close()
        }

        Base64.encodeToString(it.toByteArray(), Base64.DEFAULT).also { check(!it.isNullOrEmpty()) }
    }
}

fun <T : Serializable> deserialize(serialized: String?): T? {
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

val Resources.isLandscape get() = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

fun <T> RequestBuilder<T>.circle(circle: Boolean) = if (circle) apply(RequestOptions.circleCropTransform()) else this

fun <T : Any> List<Single<T>>.zipSingle() = if (isEmpty()) {
    Single.just(listOf())
} else {
    Single.zip(this) {
        it.map {
            @Suppress("UNCHECKED_CAST")
            it as T
        }
    }
}

inline fun <reified T, U> T.getPrivateField(name: String): U {
    return T::class.java.getDeclaredField(name).let {
        it.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        it.get(this) as U
    }
}

fun <T> Single<T>.tryGetCurrentValue(): T? {
    var value: T? = null
    subscribe { t -> value = t }.dispose()
    return value
}

fun <T> Single<T>.getCurrentValue() = tryGetCurrentValue()!!

fun <T : Any, U> Observable<T>.mapNotNull(mapper: (T) -> U?) =
        map { NullableWrapper(mapper(it)) }.filterNotNull()

fun <T> Observable<T>.publishImmediate(compositeDisposable: CompositeDisposable) =
    publish().apply { compositeDisposable += connect() }!!

fun <T> Single<T>.cacheImmediate(compositeDisposable: CompositeDisposable) =
    cache().apply { compositeDisposable += subscribe() }!!

@Suppress("unused")
fun Disposable.ignore() = Unit

fun webSearchIntent(query: String) =
    Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)

@SuppressLint("ClickableViewAccessibility")
fun hideKeyboardOnClickOutside(view: View) {
    if (view !is EditText) {
        view.setOnTouchListener { _, _ ->
            hideSoftKeyboard(view)
            false
        }
    }

    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            hideKeyboardOnClickOutside(view.getChildAt(i))
        }
    }
}

fun hideSoftKeyboard(view: View) {
    val inputMethodManager =
            view.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

inline fun <reified T : Fragment> FragmentManager.getOrInitializeFragment(
        @IdRes id: Int,
        initializer: () -> T,
) = (findFragmentById(id) as? T) ?: initializer().also { beginTransaction().add(id, it).commit() }

inline fun <reified T : Fragment> AbstractActivity.getOrInitializeFragment(
        @IdRes id: Int,
        initializer: () -> T,
) = supportFragmentManager.getOrInitializeFragment(id, initializer)

inline fun <reified T : Fragment> FragmentManager.tryGetFragment(@IdRes id: Int) = findFragmentById(id) as? T

inline fun <reified T : Fragment> FragmentManager.tryGetFragment(tag: String) = findFragmentByTag(tag) as? T

inline fun <reified T : Fragment> FragmentManager.forceGetFragment(@IdRes id: Int) = tryGetFragment<T>(id)!!

inline fun <reified T : Fragment> AbstractActivity.forceGetFragment(@IdRes id: Int) =
        supportFragmentManager.forceGetFragment<T>(id)

inline fun <reified T : Fragment> AbstractActivity.tryGetFragment(tag: String) =
        supportFragmentManager.tryGetFragment<T>(tag)

inline fun <reified T : Fragment> Fragment.tryGetFragment(tag: String) = childFragmentManager.tryGetFragment<T>(tag)