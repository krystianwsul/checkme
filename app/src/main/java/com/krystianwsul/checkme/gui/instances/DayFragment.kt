package com.krystianwsul.checkme.gui.instances


import android.content.Context
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.LinearLayoutCompat
import android.util.AttributeSet
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.DayViewModel
import com.krystianwsul.treeadapter.TreeViewAdapter
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_day.view.*
import java.text.DateFormatSymbols
import java.util.*


class DayFragment @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    private var key: Pair<MainActivity.TimeRange, Int>? = null

    private var floatingActionButton: FloatingActionButton? = null

    private val activity = context as MainActivity

    private val dayViewModel = activity.dayViewModel
    private var entry: DayViewModel.Entry? = null

    private val compositeDisposable = CompositeDisposable()

    init {
        check(context is Host)

        View.inflate(context, R.layout.fragment_day, this)

        orientation = LinearLayoutCompat.VERTICAL
    }

    fun saveState() {
        activity.states[key!!] = groupListFragment.onSaveInstanceState()
    }

    fun setPosition(timeRange: MainActivity.TimeRange, position: Int) {
        entry?.stop()
        compositeDisposable.clear()

        key?.let { saveState() }

        key = Pair(timeRange, position)

        activity.states[key!!]?.let {
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

                startDate.toString() + " - " + endDate.toString()
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

        compositeDisposable += (context as Host).hostEvents.subscribe {
            if (it is Event.PageVisible && it.position == position)
                setFab(it.floatingActionButton)
            else {
                clearFab()
                saveState()
            }
        }

        floatingActionButton?.let { groupListFragment.setFab(it) }

        entry = dayViewModel.getEntry(timeRange, position).apply {
            start()

            compositeDisposable += data.subscribe { groupListFragment.setAll(timeRange, position, it.dataId, it.dataWrapper) }
        }
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
