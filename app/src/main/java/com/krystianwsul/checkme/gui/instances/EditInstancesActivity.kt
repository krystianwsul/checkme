package com.krystianwsul.checkme.gui.instances

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.app.ActionBar
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity
import com.krystianwsul.checkme.loaders.EditInstancesLoader
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import kotlinx.android.synthetic.main.activity_edit_instance.*
import java.util.*

class EditInstancesActivity : AbstractActivity(), LoaderManager.LoaderCallbacks<EditInstancesLoader.Data> {

    companion object {

        private const val INSTANCE_KEYS = "instanceKeys"

        private const val DATE_KEY = "date"
        private const val TIME_PAIR_PERSIST_KEY = "timePairPersist"
        private const val INITIAL_HOUR_MINUTE_KEY = "initialHourMinute"
        private const val INITIAL_DATE_KEY = "initialDate"

        private const val DATE_FRAGMENT_TAG = "dateFragment"
        private const val TIME_FRAGMENT_TAG = "timeFragment"
        private const val TIME_DIALOG_FRAGMENT_TAG = "timeDialogFragment"
        private const val DISCARD_TAG = "discard"

        fun getIntent(instanceKeys: ArrayList<InstanceKey>?): Intent {
            checkNotNull(instanceKeys)
            check(instanceKeys!!.size > 1)

            val intent = Intent(MyApplication.instance, EditInstancesActivity::class.java)
            intent.putParcelableArrayListExtra(INSTANCE_KEYS, instanceKeys)
            return intent
        }
    }

    private var mDate: Date? = null
    private var mData: EditInstancesLoader.Data? = null

    private lateinit var actionBar: ActionBar

    private var mSavedInstanceState: Bundle? = null

    private var mBroadcastReceiver: BroadcastReceiver? = null

    private var mTimePairPersist: TimePairPersist? = null

    private var mFirst = true

    private val mTimeDialogListener = object : TimeDialogFragment.TimeDialogListener {
        override fun onCustomTimeSelected(customTimeKey: CustomTimeKey) {
            checkNotNull(mData)

            mTimePairPersist!!.customTimeKey = customTimeKey

            updateTimeText()

            updateError()
        }

        override fun onOtherSelected() {
            checkNotNull(mData)

            val timePickerDialogFragment = TimePickerDialogFragment.newInstance(mTimePairPersist!!.hourMinute)
            timePickerDialogFragment.listener = mTimePickerDialogFragmentListener
            timePickerDialogFragment.show(supportFragmentManager, TIME_FRAGMENT_TAG)
        }

        override fun onAddSelected() {
            startActivityForResult(ShowCustomTimeActivity.getCreateIntent(this@EditInstancesActivity), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        }
    }

    private val mTimePickerDialogFragmentListener = { hourMinute: HourMinute ->
        checkNotNull(mData)

        mTimePairPersist!!.hourMinute = hourMinute
        updateTimeText()
        updateError()
    }

    private val mDiscardDialogListener = this@EditInstancesActivity::finish

    private val mDatePickerDialogFragmentListener = { date: Date ->
        mDate = date
        updateDateText()
    }

    private var mInitialTimePair: TimePair? = null
    private var mInitialDate: Date? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_instance, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_edit_instance_save).isVisible = mData != null
        menu.findItem(R.id.action_edit_instance_hour).isVisible = mData != null && mData!!.showHour
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_edit_instance_hour -> {
                checkNotNull(mData)
                check(mData!!.showHour)

                supportLoaderManager.destroyLoader(0)

                DomainFactory.getDomainFactory().setInstancesAddHourActivity(this, mData!!.dataId, SaveService.Source.GUI, mData!!.instanceDatas.keys)

                finish()
            }
            R.id.action_edit_instance_save -> {
                checkNotNull(mDate)
                checkNotNull(mData)

                if (isValidDateTime) {
                    DomainFactory.getDomainFactory().setInstancesDateTime(this, mData!!.dataId, SaveService.Source.GUI, mData!!.instanceDatas.keys, mDate!!, mTimePairPersist!!.timePair)

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
        checkNotNull(toolbar)

        setSupportActionBar(toolbar)

        actionBar = supportActionBar!!

        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        actionBar.title = null

        mSavedInstanceState = savedInstanceState

        editInstanceDate.setOnClickListener {
            val datePickerDialogFragment = DatePickerDialogFragment.newInstance(mDate!!)
            datePickerDialogFragment.listener = mDatePickerDialogFragmentListener
            datePickerDialogFragment.show(supportFragmentManager, DATE_FRAGMENT_TAG)
        }
        val datePickerDialogFragment = supportFragmentManager.findFragmentByTag(DATE_FRAGMENT_TAG) as? DatePickerDialogFragment
        datePickerDialogFragment?.listener = mDatePickerDialogFragmentListener

        if (mSavedInstanceState != null && mSavedInstanceState!!.containsKey(DATE_KEY)) {
            mDate = mSavedInstanceState!!.getParcelable(DATE_KEY)
            checkNotNull(mDate)

            check(mSavedInstanceState!!.containsKey(TIME_PAIR_PERSIST_KEY))
            mTimePairPersist = mSavedInstanceState!!.getParcelable(TIME_PAIR_PERSIST_KEY)
            checkNotNull(mTimePairPersist)

            check(mSavedInstanceState!!.containsKey(INITIAL_HOUR_MINUTE_KEY))
            mInitialTimePair = mSavedInstanceState!!.getParcelable(INITIAL_HOUR_MINUTE_KEY)
            checkNotNull(mInitialTimePair)

            check(mSavedInstanceState!!.containsKey(INITIAL_DATE_KEY))
            mInitialDate = mSavedInstanceState!!.getParcelable(INITIAL_DATE_KEY)
            checkNotNull(mInitialDate)
        }

        supportLoaderManager.initLoader<EditInstancesLoader.Data>(0, null, this)

        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (mData != null)
                    updateError()
            }
        }

        val discardDialogFragment = supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment
        discardDialogFragment?.discardDialogListener = mDiscardDialogListener
    }

    public override fun onResume() {
        super.onResume()

        registerReceiver(mBroadcastReceiver, IntentFilter(Intent.ACTION_TIME_TICK))

        if (mData != null)
            updateError()
    }

    public override fun onPause() {
        super.onPause()

        unregisterReceiver(mBroadcastReceiver)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (mData != null) {
            checkNotNull(mDate)
            outState.putParcelable(DATE_KEY, mDate)

            checkNotNull(mTimePairPersist)
            outState.putParcelable(TIME_PAIR_PERSIST_KEY, mTimePairPersist)

            checkNotNull(mInitialTimePair)
            outState.putParcelable(INITIAL_HOUR_MINUTE_KEY, mInitialTimePair)

            checkNotNull(mInitialDate)
            outState.putParcelable(INITIAL_DATE_KEY, mInitialDate)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<EditInstancesLoader.Data> {
        val intent = intent
        check(intent.hasExtra(INSTANCE_KEYS))
        val instanceKeys = intent.getParcelableArrayListExtra<InstanceKey>(INSTANCE_KEYS)

        checkNotNull(instanceKeys)
        check(instanceKeys!!.size > 1)

        return EditInstancesLoader(this, instanceKeys)
    }

    override fun onLoadFinished(loader: Loader<EditInstancesLoader.Data>, data: EditInstancesLoader.Data) {
        mData = data

        editInstanceLayout.visibility = View.VISIBLE

        if (mFirst && (mSavedInstanceState == null || !mSavedInstanceState!!.containsKey(DATE_KEY))) {
            check(mDate == null)
            check(mTimePairPersist == null)
            check(mInitialTimePair == null)
            check(mInitialDate == null)
            check(!mData!!.instanceDatas.isEmpty())

            mFirst = false

            val dateTime = mData!!.instanceDatas.values
                    .map { it.instanceDateTime }
                    .sorted()
                    .first()

            mDate = dateTime.date
            mTimePairPersist = TimePairPersist(dateTime.time.timePair)

            mInitialTimePair = mTimePairPersist!!.timePair
            mInitialDate = mDate
        }

        actionBar.title = mData!!.instanceDatas
                .values
                .joinToString(", ") { it.name }

        invalidateOptionsMenu()

        updateDateText()

        val timePickerDialogFragment = supportFragmentManager.findFragmentByTag(TIME_FRAGMENT_TAG) as? TimePickerDialogFragment
        timePickerDialogFragment?.listener = mTimePickerDialogFragmentListener

        editInstanceTime.setOnClickListener {
            checkNotNull(mData)
            val customTimeDatas = ArrayList<TimeDialogFragment.CustomTimeData>(mData!!.customTimeDatas.values
                    .filter { it.customTimeKey.mLocalCustomTimeId != null }
                    .sortedBy { it.hourMinutes[mDate!!.dayOfWeek] }
                    .map { TimeDialogFragment.CustomTimeData(it.customTimeKey, it.name + " (" + it.hourMinutes[mDate!!.dayOfWeek] + ")") })

            val timeDialogFragment = TimeDialogFragment.newInstance(customTimeDatas)

            timeDialogFragment.timeDialogListener = mTimeDialogListener

            timeDialogFragment.show(supportFragmentManager, TIME_DIALOG_FRAGMENT_TAG)
        }

        val timeDialogFragment = supportFragmentManager.findFragmentByTag(TIME_DIALOG_FRAGMENT_TAG) as? TimeDialogFragment
        timeDialogFragment?.timeDialogListener = mTimeDialogListener
    }

    override fun onLoaderReset(loader: Loader<EditInstancesLoader.Data>) {}

    private fun updateDateText() {
        checkNotNull(mDate)

        editInstanceDate.setText(mDate!!.getDisplayText(this))

        updateTimeText()

        updateError()
    }

    @SuppressLint("SetTextI18n")
    private fun updateTimeText() {
        checkNotNull(mTimePairPersist)
        checkNotNull(mData)
        checkNotNull(mDate)

        if (mTimePairPersist!!.customTimeKey != null) {
            val customTimeData = mData!!.customTimeDatas[mTimePairPersist!!.customTimeKey]
            checkNotNull(customTimeData)

            editInstanceTime.setText(customTimeData!!.name + " (" + customTimeData.hourMinutes[mDate!!.dayOfWeek] + ")")
        } else {
            editInstanceTime.setText(mTimePairPersist!!.hourMinute.toString())
        }
    }

    private val isValidDate: Boolean
        get() = if (mData != null) {
            mDate!! >= Date.today()
        } else {
            false
        }

    private //cached data doesn't contain new custom time
    val isValidDateTime: Boolean
        get() {
            if (mData != null) {
                val hourMinute = if (mTimePairPersist!!.customTimeKey != null) {
                    if (!mData!!.customTimeDatas.containsKey(mTimePairPersist!!.customTimeKey))
                        return false

                    mData!!.customTimeDatas[mTimePairPersist!!.customTimeKey]!!.hourMinutes[mDate!!.dayOfWeek]!!
                } else {
                    mTimePairPersist!!.hourMinute
                }

                return TimeStamp(mDate, hourMinute) > TimeStamp.getNow()
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
            discardDialogFragment.discardDialogListener = mDiscardDialogListener
            discardDialogFragment.show(supportFragmentManager, DISCARD_TAG)

            false
        } else {
            true
        }
    }

    private fun dataChanged(): Boolean {
        if (mData == null)
            return false

        checkNotNull(mInitialTimePair)
        checkNotNull(mInitialDate)

        if (mTimePairPersist!!.timePair != mInitialTimePair)
            return true

        return (mInitialDate != mDate)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        check(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        check(resultCode >= 0)
        check(data == null)
        checkNotNull(mTimePairPersist)

        if (resultCode > 0)
            mTimePairPersist!!.customTimeKey = CustomTimeKey(resultCode)
    }
}
