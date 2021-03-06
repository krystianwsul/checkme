package com.krystianwsul.checkme.gui.customtimes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.TextView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.DiscardDialogFragment
import com.krystianwsul.checkme.gui.NavBarActivity
import com.krystianwsul.checkme.gui.TimePickerDialogFragment
import com.krystianwsul.checkme.persistencemodel.SaveService
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

        private const val HOUR_MINUTE_SUNDAY_KEY = "hourMinuteSunday"
        private const val HOUR_MINUTE_MONDAY_KEY = "hourMinuteMonday"
        private const val HOUR_MINUTE_TUESDAY_KEY = "hourMinuteTuesday"
        private const val HOUR_MINUTE_WEDNESDAY_KEY = "hourMinuteWednesday"
        private const val HOUR_MINUTE_THURSDAY_KEY = "hourMinuteThursday"
        private const val HOUR_MINUTE_FRIDAY_KEY = "hourMinuteFriday"
        private const val HOUR_MINUTE_SATURDAY_KEY = "hourMinuteSaturday"

        private const val EDITED_DAY_OF_WEEK_KEY = "editedDayOfWeek"

        private const val TIME_PICKER_TAG = "timePicker"
        private const val DISCARD_TAG = "discard"

        const val CREATE_CUSTOM_TIME_REQUEST_CODE = 1

        private val sDefaultHourMinute = HourMinute(9, 0)

        fun getEditIntent(customTimeId: RemoteCustomTimeId.Private, context: Context) = Intent(context, ShowCustomTimeActivity::class.java).apply {
            @Suppress("CAST_NEVER_SUCCEEDS")
            putExtra(CUSTOM_TIME_ID_KEY, customTimeId as Parcelable)
        }

        fun getCreateIntent(context: Context) = Intent(context, ShowCustomTimeActivity::class.java).apply { putExtra(NEW_KEY, true) }
    }

    private var customTimeId: RemoteCustomTimeId.Private? = null

    private var data: ShowCustomTimeViewModel.Data? = null

    private val timeViews = HashMap<DayOfWeek, AutoCompleteTextView>()
    private val hourMinutes = HashMap<DayOfWeek, HourMinute>()

    private var editedDayOfWeek: DayOfWeek? = null

    private var savedInstanceState: Bundle? = null

    private val discardDialogListener = this@ShowCustomTimeActivity::finish

    private val timePickerDialogFragmentListener = { hourMinute: HourMinute ->
        editedDayOfWeek!!.let {
            check(timeViews.containsKey(it))
            check(hourMinutes.containsKey(it))

            hourMinutes[it] = hourMinute
            timeViews[it]!!.setText(hourMinute.toString())
        }

        editedDayOfWeek = null
    }

    private var showCustomTimeViewModel: ShowCustomTimeViewModel? = null

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
                        DomainFactory.instance.updateCustomTime(data!!.dataId, SaveService.Source.GUI, data!!.id, name, hourMinutes)
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

        initializeDay(DayOfWeek.SUNDAY, R.id.time_sunday_name, R.id.time_sunday_time)
        initializeDay(DayOfWeek.MONDAY, R.id.time_monday_name, R.id.time_monday_time)
        initializeDay(DayOfWeek.TUESDAY, R.id.time_tuesday_name, R.id.time_tuesday_time)
        initializeDay(DayOfWeek.WEDNESDAY, R.id.time_wednesday_name, R.id.time_wednesday_time)
        initializeDay(DayOfWeek.THURSDAY, R.id.time_thursday_name, R.id.time_thursday_time)
        initializeDay(DayOfWeek.FRIDAY, R.id.time_friday_name, R.id.time_friday_time)
        initializeDay(DayOfWeek.SATURDAY, R.id.time_saturday_name, R.id.time_saturday_time)

        if (savedInstanceState?.containsKey(HOUR_MINUTE_SUNDAY_KEY) == true) {
            check(savedInstanceState.containsKey(EDITED_DAY_OF_WEEK_KEY))
            check(hourMinutes.isEmpty())

            extractKey(HOUR_MINUTE_SUNDAY_KEY, DayOfWeek.SUNDAY)
            extractKey(HOUR_MINUTE_MONDAY_KEY, DayOfWeek.MONDAY)
            extractKey(HOUR_MINUTE_TUESDAY_KEY, DayOfWeek.TUESDAY)
            extractKey(HOUR_MINUTE_WEDNESDAY_KEY, DayOfWeek.WEDNESDAY)
            extractKey(HOUR_MINUTE_THURSDAY_KEY, DayOfWeek.THURSDAY)
            extractKey(HOUR_MINUTE_FRIDAY_KEY, DayOfWeek.FRIDAY)
            extractKey(HOUR_MINUTE_SATURDAY_KEY, DayOfWeek.SATURDAY)

            editedDayOfWeek = savedInstanceState.getSerializable(EDITED_DAY_OF_WEEK_KEY) as? DayOfWeek

            updateGui()
        } else {
            if (intent.hasExtra(CUSTOM_TIME_ID_KEY)) {
                check(!intent.hasExtra(NEW_KEY))
            } else {
                check(intent.hasExtra(NEW_KEY))
                check(hourMinutes.isEmpty())

                for (dayOfWeek in DayOfWeek.values())
                    hourMinutes[dayOfWeek] = sDefaultHourMinute

                updateGui()
            }
        }

        if (intent.hasExtra(CUSTOM_TIME_ID_KEY)) {
            check(!intent.hasExtra(NEW_KEY))

            @Suppress("CAST_NEVER_SUCCEEDS") // because the IDE isn't recognizing it's a subclass
            customTimeId = intent.getParcelableExtra<Parcelable>(CUSTOM_TIME_ID_KEY) as RemoteCustomTimeId.Private

            showCustomTimeViewModel = getViewModel<ShowCustomTimeViewModel>().apply {
                start(customTimeId!!)

                createDisposable += data.subscribe { onLoadFinished(it) }
            }
        } else {
            check(intent.hasExtra(NEW_KEY))
        }

        (supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment)?.discardDialogListener = discardDialogListener
    }

    private fun extractKey(key: String, dayOfWeek: DayOfWeek) {
        check(savedInstanceState != null)
        check(key.isNotEmpty())

        check(savedInstanceState!!.containsKey(key))

        val hourMinute = savedInstanceState!!.getParcelable<HourMinute>(key)!!

        hourMinutes[dayOfWeek] = hourMinute
    }

    private fun initializeDay(dayOfWeek: DayOfWeek, nameId: Int, timeId: Int) {
        val timeName = findViewById<TextView>(nameId)!!

        timeName.text = dayOfWeek.toString()

        timeViews[dayOfWeek] = findViewById(timeId)!!
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (hourMinutes.isNotEmpty()) {
            outState.run {
                putParcelable(HOUR_MINUTE_SUNDAY_KEY, hourMinutes[DayOfWeek.SUNDAY])
                putParcelable(HOUR_MINUTE_MONDAY_KEY, hourMinutes[DayOfWeek.MONDAY])
                putParcelable(HOUR_MINUTE_TUESDAY_KEY, hourMinutes[DayOfWeek.TUESDAY])
                putParcelable(HOUR_MINUTE_WEDNESDAY_KEY, hourMinutes[DayOfWeek.WEDNESDAY])
                putParcelable(HOUR_MINUTE_THURSDAY_KEY, hourMinutes[DayOfWeek.THURSDAY])
                putParcelable(HOUR_MINUTE_FRIDAY_KEY, hourMinutes[DayOfWeek.FRIDAY])
                putParcelable(HOUR_MINUTE_SATURDAY_KEY, hourMinutes[DayOfWeek.SATURDAY])

                putSerializable(EDITED_DAY_OF_WEEK_KEY, editedDayOfWeek)
            }
        }
    }

    private fun updateGui() {
        check(hourMinutes.isNotEmpty())

        toolbarLayout.visibility = View.VISIBLE
        showCustomTimeContainer.visibility = View.VISIBLE

        for (dayOfWeek in DayOfWeek.values()) {
            val timeView = timeViews[dayOfWeek]!!

            val hourMinute = hourMinutes[dayOfWeek]!!

            timeView.setText(hourMinute.toString())

            timeView.setFixedOnClickListener {
                editedDayOfWeek = dayOfWeek

                val currHourMinute = hourMinutes[dayOfWeek]!!

                val timePickerDialogFragment = TimePickerDialogFragment.newInstance(currHourMinute)
                timePickerDialogFragment.listener = timePickerDialogFragmentListener
                timePickerDialogFragment.show(supportFragmentManager, TIME_PICKER_TAG)
            }
        }

        (supportFragmentManager.findFragmentByTag(TIME_PICKER_TAG) as? TimePickerDialogFragment)?.listener = timePickerDialogFragmentListener

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

        if (savedInstanceState?.containsKey(HOUR_MINUTE_SUNDAY_KEY) != true) {
            check(hourMinutes.isEmpty())

            toolbarEditText.setText(data.name)

            for (dayOfWeek in DayOfWeek.values())
                hourMinutes[dayOfWeek] = data.hourMinutes.getValue(dayOfWeek)

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

            return DayOfWeek.values().any { hourMinutes[it] != sDefaultHourMinute }
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