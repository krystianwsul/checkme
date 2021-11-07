package com.krystianwsul.checkme.gui.instances


import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.widget.LinearLayoutCompat
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentDayBinding
import com.krystianwsul.checkme.gui.main.MainActivity
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.checkme.viewmodels.DayViewModel
import com.krystianwsul.common.time.Date
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import java.time.Month
import java.time.format.TextStyle
import java.util.*


class DayFragment @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {
    // todo consider subclass

    companion object {

        fun getTitle(timeRange: Preferences.TimeRange, position: Int): String {
            fun getString(@StringRes stringId: Int) = MyApplication.instance.getString(stringId)

            return if (timeRange == Preferences.TimeRange.DAY) {
                when (position) {
                    0 -> getString(R.string.today)
                    1 -> getString(R.string.tomorrow)
                    else -> {
                        Date(
                                Calendar.getInstance()
                                        .apply { add(Calendar.DATE, position) }
                                        .toDateTimeTz()
                        ).let { it.dayOfWeek.toString() + ", " + it.toString() }
                    }
                }
            } else {
                if (timeRange == Preferences.TimeRange.WEEK) {
                    val startDate = Date(
                            Calendar.getInstance()
                                    .apply {
                                        if (position > 0) {
                                            add(Calendar.WEEK_OF_YEAR, position)
                                            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                                        }
                                    }
                                    .toDateTimeTz()
                    )

                    val endDate = Date(
                            Calendar.getInstance()
                                    .apply {
                                        add(Calendar.WEEK_OF_YEAR, position + 1)
                                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                                        add(Calendar.DATE, -1)
                                    }
                                    .toDateTimeTz()
                    )

                    "$startDate - $endDate"
                } else {
                    check(timeRange == Preferences.TimeRange.MONTH)

                    val month = Calendar.getInstance().run {
                        add(Calendar.MONTH, position)
                        get(Calendar.MONTH) + 1
                    }

                    Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                }
            }
        }
    }

    private val key = BehaviorRelay.create<Pair<Preferences.TimeRange, Int>>()

    private var fabDelegate: BottomFabMenuDelegate.FabDelegate? = null

    private val activity = context as MainActivity

    private val dayViewModel = activity.dayViewModel
    private var entry: DayViewModel.Entry? = null

    private val attachedToWindowDisposable = CompositeDisposable()

    private val compositeDisposable = CompositeDisposable()

    private val binding: FragmentDayBinding

    init {
        check(context is Host)

        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        orientation = VERTICAL

        binding = FragmentDayBinding.inflate(LayoutInflater.from(context), this)

        binding.groupListFragment.apply {
            listener = (context as MainActivity).daysGroupListListener
            forceSaveStateListener = { saveState() }
        }
    }

    fun saveState() = activity.setState(key.value!!, binding.groupListFragment.onSaveInstanceState())

    fun setPosition(timeRange: Preferences.TimeRange, position: Int) {
        entry?.stop()
        compositeDisposable.clear()

        key.value?.let { saveState() }

        key.accept(Pair(timeRange, position))

        activity.getState(key.value!!)?.let {
            binding.groupListFragment.onRestoreInstanceState(it)
        }

        // this seems redundant/obsolete, but I'll leave it for now
        fabDelegate?.let(binding.groupListFragment::setVisible)

        entry = dayViewModel.getEntry(timeRange, position)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val hostEvents = key.switchMap { key -> (context as Host).hostEvents.map { Pair(key, it) } }

        hostEvents.subscribe { (key, event) ->
            if (event is Event.PageVisible && event.position == key.second) {
                setFab(event.fabDelegate)

                entry!!.start()

                activity.selectAllRelay
                    .subscribe {
                        binding.groupListFragment
                            .treeViewAdapter
                            .selectAll()
                    }
                    .addTo(compositeDisposable)
            } else {
                entry!!.stop()

                clearFab()
                saveState()
            }
        }.addTo(compositeDisposable)

        key.switchMap { key -> entry!!.data.map { Triple(key, it, entry!!.dataId) } }
                .subscribe { (key, data, dataId) ->
                    binding.groupListFragment.setAll(
                            key.first,
                            key.second,
                            dataId,
                            data.immediate,
                            data.groupListDataWrapper
                    )
                }
                .addTo(compositeDisposable)

        hostEvents.switchMap { (key, event) ->
            if (event is Event.PageVisible && event.position == key.second)
                activity.started
            else
                Observable.never()
        }
            .filter { it }
            .subscribe { binding.groupListFragment.checkCreatedTaskKey() }
            .addTo(compositeDisposable)

        // this should be inside the GroupListFragment
        activity.dateChangeRelay
            .switchMapMaybe {
                binding.groupListFragment
                    .treeViewAdapterNullable
                    ?.listUpdates
                    ?.firstElement()
                    ?: Maybe.empty()
            }
            .subscribe { binding.groupListFragment.scrollToTop() }
            .addTo(attachedToWindowDisposable)
    }

    override fun onDetachedFromWindow() {
        compositeDisposable.clear()
        attachedToWindowDisposable.clear()

        super.onDetachedFromWindow()
    }

    private fun setFab(fabDelegate: BottomFabMenuDelegate.FabDelegate) {
        this.fabDelegate = fabDelegate

        binding.groupListFragment.setVisible(fabDelegate)
    }

    private fun clearFab() {
        fabDelegate = null

        binding.groupListFragment.clearFab()
    }

    interface Host {

        val hostEvents: Observable<Event>
    }

    sealed class Event {

        data class PageVisible(val position: Int, val fabDelegate: BottomFabMenuDelegate.FabDelegate) : Event()

        object Invisible : Event()
    }
}
