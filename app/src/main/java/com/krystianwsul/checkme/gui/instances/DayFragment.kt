package com.krystianwsul.checkme.gui.instances


import android.content.Context
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.LinearLayoutCompat
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.FabUser
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.DayViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.fragment_day.view.*
import java.text.DateFormatSymbols
import java.util.*


class DayFragment @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayoutCompat(context, attrs, defStyleAttr), FabUser {

    private var position = 0
    private lateinit var timeRange: MainActivity.TimeRange

    private var floatingActionButton: FloatingActionButton? = null

    private val activity = context as MainActivity

    private val dayViewModel = activity.dayViewModel
    private var entry: DayViewModel.Entry? = null

    private val compositeDisposable = CompositeDisposable()

    init {
        View.inflate(context, R.layout.fragment_day, this)

        orientation = LinearLayoutCompat.VERTICAL
    }

    fun setPosition(timeRange: MainActivity.TimeRange, position: Int) {
        Log.e("asdf", "position: dayFragment.setPosition " + position)

        entry?.stop()
        compositeDisposable.clear()

        this.position = position
        this.timeRange = timeRange

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

        floatingActionButton?.let { groupListFragment.setFab(it) }

        entry = dayViewModel.getEntry(timeRange, position).apply {
            start()

            compositeDisposable += data.subscribe {
                Log.e("asdf", "position: data for $position")
                groupListFragment.setAll(timeRange, position, it.dataId, it.dataWrapper)
            }
        }
    }

    override fun onDetachedFromWindow() {
        compositeDisposable.clear()

        super.onDetachedFromWindow()
    }

    fun selectAll() = groupListFragment.selectAll()

    override fun setFab(floatingActionButton: FloatingActionButton) {
        if (this.floatingActionButton === floatingActionButton)
            return

        this.floatingActionButton = floatingActionButton

        groupListFragment?.setFab(floatingActionButton)
    }

    override fun clearFab() {
        floatingActionButton = null

        groupListFragment.clearFab()
    }
}
