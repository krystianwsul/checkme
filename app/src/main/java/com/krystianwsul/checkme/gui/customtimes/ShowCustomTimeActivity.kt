package com.krystianwsul.checkme.gui.customtimes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AutoCompleteTextView
import androidx.transition.TransitionManager
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.DiscardDialogFragment
import com.krystianwsul.checkme.gui.NavBarActivity
import com.krystianwsul.checkme.gui.TimePickerDialogFragment
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.SerializableUnit
import com.krystianwsul.checkme.utils.setFixedOnClickListener
import com.krystianwsul.checkme.viewmodels.ShowCustomTimeViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.RemoteCustomTimeId
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_show_custom_time.*
import kotlinx.android.synthetic.main.toolbar_edit_text.*
import java.util.*

class ShowCustomTimeActivity : NavBarActivity() {

    companion object {

        const val CUSTOM_TIME_ID_KEY = "customTimeId"
        private const val NEW_KEY = "new"

        private const val KEY_HOUR_MINUTES = "hourMinutes"
        private const val KEY_ALL_DAYS_EXPANDED = "allDaysExpanded"

        private const val TIME_PICKER_TAG = "timePicker"
        private const val TAG_TIME_PICKER_ALL_DAYS = "timePickerAllDays"
        private const val DISCARD_TAG = "discard"

        const val CREATE_CUSTOM_TIME_REQUEST_CODE = 1

        private val defaultHourMinute = HourMinute(9, 0)

        fun getEditIntent(
                customTimeId: RemoteCustomTimeId.Private,
                context: Context
        ) = Intent(context, ShowCustomTimeActivity::class.java).apply {
            @Suppress("CAST_NEVER_SUCCEEDS")
            putExtra(CUSTOM_TIME_ID_KEY, customTimeId)
        }

        fun getCreateIntent(context: Context) = Intent(context, ShowCustomTimeActivity::class.java).apply {
            putExtra(NEW_KEY, true)
        }
    }

    private var customTimeId: RemoteCustomTimeId.Private? = null

    private var data: ShowCustomTimeViewModel.Data? = null

    private lateinit var timeViews: Map<DayOfWeek, AutoCompleteTextView>
    private var hourMinutes = mutableMapOf<DayOfWeek, HourMinute>()

    private var savedInstanceState: Bundle? = null

    private val discardDialogListener = this@ShowCustomTimeActivity::finish

    private val timePickerDialogFragmentListener = { hourMinute: HourMinute, dayOfWeek: DayOfWeek ->
        check(timeViews.containsKey(dayOfWeek))
        check(hourMinutes.containsKey(dayOfWeek))

        hourMinutes[dayOfWeek] = hourMinute
        timeViews.getValue(dayOfWeek).setText(hourMinute.toString())

        if (dayOfWeek == DayOfWeek.SUNDAY)
            timeAllDaysTime.setText(hourMinute.toString())
    }

    private val allDaysListener = { hourMinute: HourMinute, _: SerializableUnit ->
        DayOfWeek.values().forEach { dayOfWeek ->
            hourMinutes[dayOfWeek] = hourMinute
            timeViews.getValue(dayOfWeek).setText(hourMinute.toString())
        }

        timeAllDaysTime.setText(hourMinute.toString())
    }

    private var showCustomTimeViewModel: ShowCustomTimeViewModel? = null

    private var allDaysExpanded = false

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_save, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_save).isVisible = customTimeId == null || data != null

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                check(hourMinutes.isNotEmpty())

                updateError()

                val name = toolbarEditText.text.toString().trim { it <= ' ' }
                if (name.isNotEmpty()) {
                    showCustomTimeViewModel?.stop()

                    if (data != null) {
                        DomainFactory.instance.updateCustomTime(
                                data!!.dataId,
                                SaveService.Source.GUI,
                                data!!.id,
                                name,
                                hourMinutes
                        )
                    } else {
                        val customTimeKey = DomainFactory.instance.createCustomTime(SaveService.Source.GUI, name, hourMinutes)

                        setResult(Activity.RESULT_OK, Intent().apply { putExtra(CUSTOM_TIME_ID_KEY, customTimeKey) })
                    }

                    finish()
                }
            }
            android.R.id.home -> if (tryClose()) finish()
            else -> throw UnsupportedOperationException()
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_custom_time)

        setSupportActionBar(toolbar)

        supportActionBar!!.run {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        }

        this.savedInstanceState = savedInstanceState

        timeViews = listOf(
                Triple(DayOfWeek.SUNDAY, timeSundayName, timeSundayTime),
                Triple(DayOfWeek.MONDAY, timeMondayName, timeMondayTime),
                Triple(DayOfWeek.TUESDAY, timeTuesdayName, timeTuesdayTime),
                Triple(DayOfWeek.WEDNESDAY, timeWednesdayName, timeWednesdayTime),
                Triple(DayOfWeek.THURSDAY, timeThursdayName, timeThursdayTime),
                Triple(DayOfWeek.FRIDAY, timeFridayName, timeFridayTime),
                Triple(DayOfWeek.SATURDAY, timeSaturdayName, timeSaturdayTime)
        ).associate { (dayOfWeek, name, time) ->
            name.text = dayOfWeek.toString()

            dayOfWeek to time
        }

        if (savedInstanceState?.containsKey(KEY_HOUR_MINUTES) == true) {
            check(hourMinutes.isEmpty())

            @Suppress("UNCHECKED_CAST")
            hourMinutes = savedInstanceState.getSerializable(KEY_HOUR_MINUTES) as HashMap<DayOfWeek, HourMinute>

            allDaysExpanded = savedInstanceState.getBoolean(KEY_ALL_DAYS_EXPANDED)

            updateGui()
        } else {
            if (intent.hasExtra(CUSTOM_TIME_ID_KEY)) {
                check(!intent.hasExtra(NEW_KEY))
            } else {
                check(intent.hasExtra(NEW_KEY))
                check(hourMinutes.isEmpty())

                for (dayOfWeek in DayOfWeek.values())
                    hourMinutes[dayOfWeek] = defaultHourMinute

                updateGui()
            }
        }

        if (intent.hasExtra(CUSTOM_TIME_ID_KEY)) {
            check(!intent.hasExtra(NEW_KEY))

            customTimeId = intent.getParcelableExtra<RemoteCustomTimeId.Private>(CUSTOM_TIME_ID_KEY)

            showCustomTimeViewModel = getViewModel<ShowCustomTimeViewModel>().apply {
                start(customTimeId!!)

                createDisposable += data.subscribe { onLoadFinished(it) }
            }
        } else {
            check(intent.hasExtra(NEW_KEY))
        }

        (supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment)?.discardDialogListener = discardDialogListener
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (hourMinutes.isNotEmpty()) {
            outState.putSerializable(KEY_HOUR_MINUTES, HashMap(hourMinutes))
            outState.putBoolean(KEY_ALL_DAYS_EXPANDED, allDaysExpanded)
        }
    }

    private fun updateGui() {
        check(hourMinutes.isNotEmpty())

        toolbarLayout.visibility = View.VISIBLE
        showCustomTimeContainer.visibility = View.VISIBLE

        TransitionManager.beginDelayedTransition(showCustomTimeContainer)
        timeIndividualDaysLayout.visibility = if (allDaysExpanded) View.VISIBLE else View.GONE
        timeAllDaysTimeLayout.visibility = if (allDaysExpanded) View.GONE else View.VISIBLE

        timeAllDaysText.setFixedOnClickListener {
            allDaysExpanded = !allDaysExpanded

            if (!allDaysExpanded) {
                val hourMinute = hourMinutes.getValue(DayOfWeek.SUNDAY)

                hourMinutes = DayOfWeek.values()
                        .associate { it to hourMinute }
                        .toMutableMap()
            }

            updateGui()
        }

        val allDaysHourMinute = hourMinutes.getValue(DayOfWeek.SUNDAY)
        timeAllDaysTime.setText(allDaysHourMinute.toString())

        timeAllDaysTime.setFixedOnClickListener {
            TimePickerDialogFragment.newInstance(allDaysHourMinute, SerializableUnit).apply {
                listener = allDaysListener
                show(supportFragmentManager, TAG_TIME_PICKER_ALL_DAYS)
            }
        }

        @Suppress("UNCHECKED_CAST")
        (supportFragmentManager.findFragmentByTag(TAG_TIME_PICKER_ALL_DAYS) as? TimePickerDialogFragment<SerializableUnit>)?.listener = allDaysListener

        for (dayOfWeek in DayOfWeek.values()) {
            val timeView = timeViews.getValue(dayOfWeek)
            val hourMinute = hourMinutes.getValue(dayOfWeek)

            timeView.setText(hourMinute.toString())

            timeView.setFixedOnClickListener {
                val currHourMinute = hourMinutes.getValue(dayOfWeek)

                TimePickerDialogFragment.newInstance(currHourMinute, dayOfWeek).apply {
                    listener = timePickerDialogFragmentListener
                    show(supportFragmentManager, TIME_PICKER_TAG)
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        (supportFragmentManager.findFragmentByTag(TIME_PICKER_TAG) as? TimePickerDialogFragment<DayOfWeek>)?.listener = timePickerDialogFragmentListener

        toolbarEditText.addTextChangedListener(object : TextWatcher {
            private var skip = savedInstanceState != null

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable) {
                if (skip) {
                    skip = false
                    return
                }

                updateError()
            }
        })
    }

    private fun onLoadFinished(data: ShowCustomTimeViewModel.Data) {
        this.data = data

        if (savedInstanceState?.containsKey(KEY_HOUR_MINUTES) != true) {
            check(hourMinutes.isEmpty())

            toolbarEditText.setText(data.name)

            hourMinutes = HashMap(data.hourMinutes)

            allDaysExpanded = hourMinutes.values
                    .distinct()
                    .size > 1

            updateGui()
        }

        invalidateOptionsMenu()
    }

    override fun onBackPressed() {
        if (tryClose())
            super.onBackPressed()
    }

    private fun tryClose() = if (dataChanged()) {
        DiscardDialogFragment.newInstance().also {
            it.discardDialogListener = discardDialogListener
            it.show(supportFragmentManager, DISCARD_TAG)
        }

        false
    } else {
        true
    }

    private fun dataChanged(): Boolean {
        if (customTimeId == null) {
            check(data == null)

            if (!toolbarEditText.text.isNullOrEmpty())
                return true

            return DayOfWeek.values().any { hourMinutes[it] != defaultHourMinute }
        } else {
            if (data == null)
                return false

            if (toolbarEditText.text.toString() != data!!.name)
                return true

            return DayOfWeek.values().any { hourMinutes[it] != data!!.hourMinutes[it] }
        }
    }

    private fun updateError() {
        if (toolbarEditText.text.isNullOrEmpty()) {
            toolbarLayout.error = getString(R.string.nameError)
            toolbarLayout.setPadding(0, 0, 0, 0)
        } else {
            toolbarLayout.error = null
        }
    }
}