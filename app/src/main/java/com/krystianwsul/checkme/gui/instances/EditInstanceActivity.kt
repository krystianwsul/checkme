package com.krystianwsul.checkme.gui.instances

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.app.ActionBar
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.KotlinDomainFactory
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePairPersist
import com.krystianwsul.checkme.utils.time.TimeStamp
import com.krystianwsul.checkme.viewmodels.EditInstanceViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_edit_instance.*
import java.util.*

class EditInstanceActivity : AbstractActivity() {

    companion object {

        private const val INSTANCE_KEY = "instanceKey"

        private const val DATE_KEY = "date"
        private const val TIME_PAIR_PERSIST_KEY = "timePairPersist"

        private const val DATE_FRAGMENT_TAG = "dateFragment"
        private const val TIME_FRAGMENT_TAG = "timeFragment"
        private const val TIME_DIALOG_FRAGMENT_TAG = "timeDialogFragment"
        private const val DISCARD_TAG = "discard"

        fun getIntent(instanceKey: InstanceKey) = Intent(MyApplication.instance, EditInstanceActivity::class.java).apply {
            putExtra(INSTANCE_KEY, instanceKey as Parcelable)
        }
    }

    private var date: Date? = null
    private var data: EditInstanceViewModel.Data? = null

    private lateinit var actionBar: ActionBar

    private var savedInstanceState: Bundle? = null

    private var broadcastReceiver: BroadcastReceiver? = null

    private var timePairPersist: TimePairPersist? = null

    private var first = true

    private val timeDialogListener = object : TimeDialogFragment.TimeDialogListener {

        override fun onCustomTimeSelected(customTimeKey: CustomTimeKey) {
            check(data != null)

            timePairPersist!!.customTimeKey = customTimeKey

            updateTimeText()

            updateError()
        }

        override fun onOtherSelected() {
            check(data != null)

            TimePickerDialogFragment.newInstance(timePairPersist!!.hourMinute).also {
                it.listener = timePickerDialogFragmentListener
                it.show(supportFragmentManager, TIME_FRAGMENT_TAG)
            }
        }

        override fun onAddSelected() = startActivityForResult(ShowCustomTimeActivity.getCreateIntent(this@EditInstanceActivity), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
    }

    private val timePickerDialogFragmentListener = { hourMinute: HourMinute ->
        check(data != null)

        timePairPersist!!.hourMinute = hourMinute
        updateTimeText()
        updateError()
    }

    private val discardDialogListener = this::finish

    private val datePickerDialogFragment = { date: Date ->
        this.date = date
        updateDateText()
    }

    private lateinit var editInstanceViewModel: EditInstanceViewModel

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_instance, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_edit_instance_save).isVisible = data != null
        menu.findItem(R.id.action_edit_instance_hour).isVisible = data != null && data!!.showHour
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_edit_instance_hour -> {
                check(data != null)
                check(data!!.showHour)

                editInstanceViewModel.stop()

                KotlinDomainFactory.getKotlinDomainFactory().setInstanceAddHourActivity(data!!.dataId, SaveService.Source.GUI, data!!.instanceKey)

                finish()
            }
            R.id.action_edit_instance_save -> {
                check(date != null)
                check(data != null)

                if (isValidDateTime) {
                    editInstanceViewModel.stop()

                    KotlinDomainFactory.getKotlinDomainFactory().setInstanceDateTime(data!!.dataId, SaveService.Source.GUI, data!!.instanceKey, date!!, timePairPersist!!.timePair)
                    finish()
                }
            }
            android.R.id.home -> if (tryClose())
                finish()
            else -> throw UnsupportedOperationException()
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_instance)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        check(toolbar != null)

        setSupportActionBar(toolbar)

        actionBar = supportActionBar!!

        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        actionBar.title = null

        this.savedInstanceState = savedInstanceState

        editInstanceDate.setOnClickListener {
            val datePickerDialogFragment = DatePickerDialogFragment.newInstance(date!!)
            datePickerDialogFragment.listener = this.datePickerDialogFragment
            datePickerDialogFragment.show(supportFragmentManager, DATE_FRAGMENT_TAG)
        }
        val datePickerDialogFragment = supportFragmentManager.findFragmentByTag(DATE_FRAGMENT_TAG) as? DatePickerDialogFragment
        datePickerDialogFragment?.listener = this.datePickerDialogFragment

        if (this.savedInstanceState != null && this.savedInstanceState!!.containsKey(DATE_KEY)) {
            date = this.savedInstanceState!!.getParcelable(DATE_KEY)
            check(date != null)

            check(this.savedInstanceState!!.containsKey(TIME_PAIR_PERSIST_KEY))
            timePairPersist = this.savedInstanceState!!.getParcelable(TIME_PAIR_PERSIST_KEY)
            check(timePairPersist != null)
        }

        val instanceKey = intent.getParcelableExtra<InstanceKey>(INSTANCE_KEY)!!

        editInstanceViewModel = getViewModel<EditInstanceViewModel>().apply {
            start(instanceKey)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (data != null)
                    updateError()
            }
        }

        val discardDialogFragment = supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment
        discardDialogFragment?.discardDialogListener = discardDialogListener
    }

    public override fun onResume() {
        super.onResume()

        registerReceiver(broadcastReceiver, IntentFilter(Intent.ACTION_TIME_TICK))

        if (data != null)
            updateError()
    }

    public override fun onPause() {
        super.onPause()

        unregisterReceiver(broadcastReceiver)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (data != null) {
            check(date != null)
            outState.putParcelable(DATE_KEY, date)

            check(timePairPersist != null)
            outState.putParcelable(TIME_PAIR_PERSIST_KEY, timePairPersist)
        }
    }

    private fun onLoadFinished(data: EditInstanceViewModel.Data) {
        this.data = data

        if (data.done) {
            Toast.makeText(this, R.string.instanceMarkedDone, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        editInstanceLayout.visibility = View.VISIBLE

        if (first && (savedInstanceState == null || !savedInstanceState!!.containsKey(DATE_KEY))) {
            check(date == null)
            check(timePairPersist == null)

            first = false

            date = this.data!!.instanceDate
            timePairPersist = TimePairPersist(this.data!!.instanceTimePair)
        }

        actionBar.title = data.name

        invalidateOptionsMenu()

        updateDateText()

        val timePickerDialogFragment = supportFragmentManager.findFragmentByTag(TIME_FRAGMENT_TAG) as? TimePickerDialogFragment
        timePickerDialogFragment?.listener = timePickerDialogFragmentListener

        editInstanceTime.setOnClickListener {
            check(this.data != null)
            val customTimeDatas = ArrayList<TimeDialogFragment.CustomTimeData>(this.data!!.customTimeDatas.values
                    .filter { it.customTimeKey.localCustomTimeId != null }
                    .sortedBy { it.hourMinutes[date!!.dayOfWeek] }
                    .map { TimeDialogFragment.CustomTimeData(it.customTimeKey, it.name + " (" + it.hourMinutes[date!!.dayOfWeek] + ")") })

            val timeDialogFragment = TimeDialogFragment.newInstance(customTimeDatas)

            timeDialogFragment.timeDialogListener = timeDialogListener

            timeDialogFragment.show(supportFragmentManager, TIME_DIALOG_FRAGMENT_TAG)
        }

        val timeDialogFragment = supportFragmentManager.findFragmentByTag(TIME_DIALOG_FRAGMENT_TAG) as? TimeDialogFragment
        timeDialogFragment?.timeDialogListener = timeDialogListener
    }

    private fun updateDateText() {
        editInstanceDate.setText(date!!.getDisplayText())

        updateTimeText()

        updateError()
    }

    @SuppressLint("SetTextI18n")
    private fun updateTimeText() {
        check(timePairPersist != null)
        check(data != null)
        check(date != null)

        if (timePairPersist!!.customTimeKey != null) {
            val customTimeData = data!!.customTimeDatas[timePairPersist!!.customTimeKey]!!

            editInstanceTime.setText(customTimeData.name + " (" + customTimeData.hourMinutes[date!!.dayOfWeek] + ")")
        } else {
            editInstanceTime.setText(timePairPersist!!.hourMinute.toString())
        }
    }

    private val isValidDate: Boolean
        get() = if (data != null) {
            date!! >= Date.today()
        } else {
            false
        }

    private //cached data doesn't contain new custom time
    val isValidDateTime: Boolean
        get() {
            if (data != null) {
                val hourMinute = if (timePairPersist!!.customTimeKey != null) {
                    if (!data!!.customTimeDatas.containsKey(timePairPersist!!.customTimeKey))
                        return false

                    data!!.customTimeDatas[timePairPersist!!.customTimeKey]!!.hourMinutes[date!!.dayOfWeek]!!
                } else {
                    timePairPersist!!.hourMinute
                }

                return TimeStamp(date!!, hourMinute) > TimeStamp.now
            } else {
                return false
            }
        }

    private fun updateError() {
        if (isValidDate) {
            editInstanceDateLayout.error = null
            editInstanceTimeLayout.error = if (isValidDateTime) null else getString(R.string.error_time)
        } else {
            editInstanceDateLayout.error = getString(R.string.error_date)
            editInstanceTimeLayout.error = null
        }
    }

    override fun onBackPressed() {
        if (tryClose())
            super.onBackPressed()
    }

    private fun tryClose(): Boolean {
        return if (dataChanged()) {
            val discardDialogFragment = DiscardDialogFragment.newInstance()
            discardDialogFragment.discardDialogListener = discardDialogListener
            discardDialogFragment.show(supportFragmentManager, DISCARD_TAG)

            false
        } else {
            true
        }
    }

    private fun dataChanged(): Boolean {
        if (data == null)
            return false

        if (data!!.instanceDate != date)
            return true

        return (data!!.instanceTimePair != timePairPersist!!.timePair)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        check(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        check(resultCode >= 0)
        check(data == null)
        check(timePairPersist != null)

        if (resultCode > 0)
            timePairPersist!!.customTimeKey = CustomTimeKey(resultCode)
    }
}
