package com.krystianwsul.checkme.gui.customtimes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.DiscardDialogFragment
import com.krystianwsul.checkme.gui.TimePickerDialogFragment
import com.krystianwsul.checkme.loaders.ShowCustomTimeLoader
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.time.DayOfWeek
import com.krystianwsul.checkme.utils.time.HourMinute
import junit.framework.Assert
import kotlinx.android.synthetic.main.activity_show_custom_time.*
import kotlinx.android.synthetic.main.toolbar_edit_text.*
import java.util.*

class ShowCustomTimeActivity : AbstractActivity(), LoaderManager.LoaderCallbacks<ShowCustomTimeLoader.Data> {

    companion object {

        private const val CUSTOM_TIME_ID_KEY = "customTimeId"
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

        fun getEditIntent(customTimeId: Int, context: Context) = Intent(context, ShowCustomTimeActivity::class.java).apply { putExtra(CUSTOM_TIME_ID_KEY, customTimeId) }

        fun getCreateIntent(context: Context) = Intent(context, ShowCustomTimeActivity::class.java).apply { putExtra(NEW_KEY, true) }
    }

    private var customTimeId: Int? = null

    private var data: ShowCustomTimeLoader.Data? = null

    private val timeViews = HashMap<DayOfWeek, TextView>()
    private val hourMinutes = HashMap<DayOfWeek, HourMinute>()

    private var editedDayOfWeek: DayOfWeek? = null

    private var savedInstanceState: Bundle? = null

    private val discardDialogListener = this@ShowCustomTimeActivity::finish

    private val timePickerDialogFragmentListener = { hourMinute: HourMinute ->
        Assert.assertTrue(editedDayOfWeek != null)
        Assert.assertTrue(timeViews.containsKey(editedDayOfWeek))
        Assert.assertTrue(hourMinutes.containsKey(editedDayOfWeek))

        hourMinutes[editedDayOfWeek!!] = hourMinute
        timeViews[editedDayOfWeek!!]!!.text = hourMinute.toString()

        editedDayOfWeek = null
    }

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
                Assert.assertTrue(!hourMinutes.isEmpty())

                updateError()

                val name = toolbarEditText.text.toString().trim { it <= ' ' }
                if (name.isNotEmpty()) {
                    supportLoaderManager.destroyLoader(0)

                    if (data != null) {
                        DomainFactory.getDomainFactory().updateCustomTime(this@ShowCustomTimeActivity, data!!.dataId, SaveService.Source.GUI, data!!.id, name, hourMinutes)
                    } else {
                        val customTimeId = DomainFactory.getDomainFactory().createCustomTime(this@ShowCustomTimeActivity, SaveService.Source.GUI, name, hourMinutes)
                        Assert.assertTrue(customTimeId > 0)

                        setResult(customTimeId)
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
            Assert.assertTrue(savedInstanceState.containsKey(EDITED_DAY_OF_WEEK_KEY))
            Assert.assertTrue(hourMinutes.isEmpty())

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
                Assert.assertTrue(!intent.hasExtra(NEW_KEY))
            } else {
                Assert.assertTrue(intent.hasExtra(NEW_KEY))
                Assert.assertTrue(hourMinutes.isEmpty())

                for (dayOfWeek in DayOfWeek.values())
                    hourMinutes[dayOfWeek] = sDefaultHourMinute

                updateGui()
            }
        }

        if (intent.hasExtra(CUSTOM_TIME_ID_KEY)) {
            Assert.assertTrue(!intent.hasExtra(NEW_KEY))

            customTimeId = intent.getIntExtra(CUSTOM_TIME_ID_KEY, -1)
            Assert.assertTrue(customTimeId != -1)

            supportLoaderManager.initLoader(0, null, this)
        } else {
            Assert.assertTrue(intent.hasExtra(NEW_KEY))
        }

        (supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment)?.discardDialogListener = discardDialogListener
    }

    private fun extractKey(key: String, dayOfWeek: DayOfWeek) {
        Assert.assertTrue(savedInstanceState != null)
        Assert.assertTrue(key.isNotEmpty())

        Assert.assertTrue(savedInstanceState!!.containsKey(key))

        val hourMinute = savedInstanceState!!.getParcelable<HourMinute>(key)!!

        hourMinutes[dayOfWeek] = hourMinute
    }

    private fun initializeDay(dayOfWeek: DayOfWeek, nameId: Int, timeId: Int) {
        val timeName = findViewById<TextView>(nameId)!!

        timeName.text = dayOfWeek.toString()

        val timeView = findViewById<TextView>(timeId)!!

        timeViews[dayOfWeek] = timeView
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (!hourMinutes.isEmpty()) {
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

    override fun onCreateLoader(id: Int, args: Bundle?) = ShowCustomTimeLoader(this, customTimeId!!)

    private fun updateGui() {
        Assert.assertTrue(!hourMinutes.isEmpty())

        toolbarLayout.visibility = View.VISIBLE
        showCustomTimeContainer.visibility = View.VISIBLE

        for (dayOfWeek in DayOfWeek.values()) {
            val timeView = timeViews[dayOfWeek]!!

            val hourMinute = hourMinutes[dayOfWeek]!!

            timeView.text = hourMinute.toString()

            timeView.setOnClickListener {
                editedDayOfWeek = dayOfWeek

                val currHourMinute = hourMinutes[dayOfWeek]!!

                val timePickerDialogFragment = TimePickerDialogFragment.newInstance(currHourMinute)
                timePickerDialogFragment.setListener(timePickerDialogFragmentListener)
                timePickerDialogFragment.show(supportFragmentManager, TIME_PICKER_TAG)
            }
        }

        (supportFragmentManager.findFragmentByTag(TIME_PICKER_TAG) as? TimePickerDialogFragment)?.setListener(timePickerDialogFragmentListener)

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

    override fun onLoadFinished(loader: Loader<ShowCustomTimeLoader.Data>, data: ShowCustomTimeLoader.Data) {
        Log.e("asdf", "onLoadFinished")

        this.data = data

        if (savedInstanceState?.containsKey(HOUR_MINUTE_SUNDAY_KEY) != true) {
            Assert.assertTrue(hourMinutes.isEmpty())

            toolbarEditText.setText(data.name)

            for (dayOfWeek in DayOfWeek.values())
                hourMinutes[dayOfWeek] = data.hourMinutes[dayOfWeek]!!

            updateGui()
        }

        invalidateOptionsMenu()
    }

    override fun onLoaderReset(data: Loader<ShowCustomTimeLoader.Data>) {}

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
            Assert.assertTrue(data == null)

            if (toolbarEditText.text.isNotEmpty())
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