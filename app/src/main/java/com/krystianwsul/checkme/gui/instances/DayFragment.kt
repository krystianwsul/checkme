package com.krystianwsul.checkme.gui.instances


import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.view.LayoutInflater
import android.view.ViewGroup
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractFragment
import com.krystianwsul.checkme.gui.FabUser
import com.krystianwsul.checkme.gui.MainActivity
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment
import com.krystianwsul.checkme.loaders.DayLoader
import com.krystianwsul.checkme.utils.time.Date
import kotlinx.android.synthetic.main.fragment_day.*
import java.text.DateFormatSymbols
import java.util.*

class DayFragment : AbstractFragment(), LoaderManager.LoaderCallbacks<DayLoader.Data>, FabUser {

    companion object {

        private val POSITION_KEY = "position"
        private val TIME_RANGE_KEY = "timeRange"

        fun newInstance(timeRange: MainActivity.TimeRange, day: Int) = DayFragment().apply {
            check(day >= 0)

            arguments = Bundle().apply {
                putInt(POSITION_KEY, day)
                putSerializable(TIME_RANGE_KEY, timeRange)
            }
        }
    }

    private var position = 0
    private lateinit var timeRange: MainActivity.TimeRange

    private var groupListFragment: GroupListFragment? = null
    private var floatingActionButton: FloatingActionButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments!!.run {
            check(containsKey(POSITION_KEY))
            position = getInt(POSITION_KEY)
            check(position >= 0)

            check(containsKey(TIME_RANGE_KEY))
            timeRange = getSerializable(TIME_RANGE_KEY) as MainActivity.TimeRange
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = inflater.inflate(R.layout.fragment_day, container, false)!!

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val title = if (timeRange == MainActivity.TimeRange.DAY) {
            when (position) {
                0 -> activity!!.getString(R.string.today)
                1 -> activity!!.getString(R.string.tomorrow)
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

        dayTabLayout.addTab(dayTabLayout.newTab().setText(title))

        groupListFragment = childFragmentManager.findFragmentById(R.id.day_frame) as GroupListFragment

        floatingActionButton?.let { groupListFragment!!.setFab(it) }

        loaderManager.initLoader(0, null, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?) = DayLoader(activity!!, position, timeRange)

    override fun onLoadFinished(loader: Loader<DayLoader.Data>, data: DayLoader.Data) {
        groupListFragment!!.setAll(timeRange, position, data.dataId, data.dataWrapper)
    }

    override fun onLoaderReset(loader: Loader<DayLoader.Data>) = Unit

    fun selectAll() {
        groupListFragment!!.selectAll()
    }

    override fun setFab(floatingActionButton: FloatingActionButton) {
        if (this.floatingActionButton === floatingActionButton)
            return

        this.floatingActionButton = floatingActionButton

        groupListFragment?.setFab(floatingActionButton)
    }

    override fun clearFab() {
        floatingActionButton = null

        groupListFragment!!.clearFab()
    }
}
