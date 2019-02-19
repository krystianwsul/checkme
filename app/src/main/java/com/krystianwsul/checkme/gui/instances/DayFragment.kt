package com.krystianwsul.checkme.gui.instances


import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.DayViewModel
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_day.view.*
import java.text.DateFormatSymbols
import java.util.*


class DayFragment @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    private val key = BehaviorRelay.create<Pair<MainActivity.TimeRange, Int>>()

    private var floatingActionButton: FloatingActionButton? = null

    private val activity = context as MainActivity

    private val dayViewModel = activity.dayViewModel
    private var entry: DayViewModel.Entry? = null

    private val compositeDisposable = CompositeDisposable()

    init {
        check(context is Host)

        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        orientation = LinearLayoutCompat.VERTICAL

        View.inflate(context, R.layout.fragment_day, this)
    }

    fun saveState() {
        Log.e("asdf", "saving state for " + key.value)
        activity.states[key.value!!] = groupListFragment.onSaveInstanceState()
    }

    fun setPosition(timeRange: MainActivity.TimeRange, position: Int) {
        entry?.stop()
        compositeDisposable.clear()

        key.value?.let { saveState() }

        key.accept(Pair(timeRange, position))

        activity.states[key.value!!]?.let {
            groupListFragment.onRestoreInstanceState(it)
        }

        val title = if (timeRange == MainActivity.TimeRange.DAY) {
            when (position) {
                0 -> activity.getString(R.string.today)
                1 -> activity.getString(R.string.tomorrow)
                else -> {
                    Date(Calendar.getInstance().apply {
                        add(Calendar.DATE, position)
                    }).let {
                        it.dayOfWeek.toString() + ", " + it.toString()
                    }
                }
            }
        } else {
            if (timeRange == MainActivity.TimeRange.WEEK) {
                val startDate = Date(Calendar.getInstance().apply {
                    if (position > 0) {
                        add(Calendar.WEEK_OF_YEAR, position)
                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    }
                })

                val endDate = Date(Calendar.getInstance().apply {
                    add(Calendar.WEEK_OF_YEAR, position + 1)
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    add(Calendar.DATE, -1)
                })

                "$startDate - $endDate"
            } else {
                check(timeRange == MainActivity.TimeRange.MONTH)

                val month = Calendar.getInstance().run {
                    add(Calendar.MONTH, position)
                    get(Calendar.MONTH)
                }

                DateFormatSymbols.getInstance().months[month]
            }
        }

        dayTabLayout.removeAllTabs()
        dayTabLayout.addTab(dayTabLayout.newTab().setText(title))

        floatingActionButton?.let { groupListFragment.setFab(it) }

        entry = dayViewModel.getEntry(timeRange, position).apply { start() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        key.switchMap { key -> (context as Host).hostEvents.map { Pair(key, it) } }
                .subscribe { (key, event) ->
                    if (event is Event.PageVisible && event.position == key.second) {
                        setFab(event.floatingActionButton)

                        activity.selectAllRelay
                                .subscribe {
                                    groupListFragment
                                            .treeViewAdapter
                                            .updateDisplayedNodes {
                                                selectAll(TreeViewAdapter.Placeholder)
                                            }
                                }
                                .addTo(compositeDisposable)
                    } else {
                        clearFab()
                        saveState()
                    }
                }
                .addTo(compositeDisposable)

        key.switchMap { key -> entry!!.data.map { Pair(key, it) } }
                .subscribe { (key, data) -> groupListFragment.setAll(key.first, key.second, data.dataId, data.dataWrapper) }
                .addTo(compositeDisposable)
    }

    override fun onDetachedFromWindow() {
        compositeDisposable.clear()

        super.onDetachedFromWindow()
    }

    fun selectAll(x: TreeViewAdapter.Placeholder) = groupListFragment.selectAll(x)

    private fun setFab(floatingActionButton: FloatingActionButton) {
        if (this.floatingActionButton === floatingActionButton)
            return

        this.floatingActionButton = floatingActionButton

        groupListFragment?.setFab(floatingActionButton)
    }

    private fun clearFab() {
        floatingActionButton = null

        groupListFragment.clearFab()
    }

    interface Host {

        val hostEvents: Observable<Event>
    }

    sealed class Event {

        data class PageVisible(val position: Int, val floatingActionButton: FloatingActionButton) : Event()

        object Invisible : Event()
    }
}
