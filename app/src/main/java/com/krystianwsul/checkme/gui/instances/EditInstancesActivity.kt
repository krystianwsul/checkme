package com.krystianwsul.checkme.gui.instances

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.app.ActionBar
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity
import com.krystianwsul.checkme.loaders.EditInstancesLoader
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import junit.framework.Assert
import java.util.*

class EditInstancesActivity : AbstractActivity(), LoaderManager.LoaderCallbacks<EditInstancesLoader.Data> {

    private var mDate: Date? = null
    private var mData: EditInstancesLoader.Data? = null

    private var mActionBar: ActionBar? = null

    private var mEditInstanceLayout: LinearLayout? = null
    private var mEditInstanceDateLayout: TextInputLayout? = null
    private var mEditInstanceDate: TextView? = null
    private var mEditInstanceTimeLayout: TextInputLayout? = null
    private var mEditInstanceTime: TextView? = null

    private var mSavedInstanceState: Bundle? = null

    private var mBroadcastReceiver: BroadcastReceiver? = null

    private var mTimePairPersist: TimePairPersist? = null

    private var mFirst = true

    private val mTimeDialogListener = object : TimeDialogFragment.TimeDialogListener {
        override fun onCustomTimeSelected(customTimeKey: CustomTimeKey) {
            Assert.assertTrue(mData != null)

            mTimePairPersist!!.setCustomTimeKey(customTimeKey)

            updateTimeText()

            updateError()
        }

        override fun onOtherSelected() {
            Assert.assertTrue(mData != null)

            val timePickerDialogFragment = TimePickerDialogFragment.newInstance(mTimePairPersist!!.hourMinute)
            timePickerDialogFragment.setListener(mTimePickerDialogFragmentListener)
            timePickerDialogFragment.show(supportFragmentManager, TIME_FRAGMENT_TAG)
        }

        override fun onAddSelected() {
            startActivityForResult(ShowCustomTimeActivity.getCreateIntent(this@EditInstancesActivity), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        }
    }

    private val mTimePickerDialogFragmentListener = { hourMinute: HourMinute ->
        Assert.assertTrue(mData != null)

        mTimePairPersist!!.hourMinute = hourMinute
        updateTimeText()
        updateError()
    }

    private val mDiscardDialogListener = DiscardDialogFragment.DiscardDialogListener { this@EditInstancesActivity.finish() }

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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_edit_instance_save -> {
                Assert.assertTrue(mDate != null)
                Assert.assertTrue(mData != null)

                if (isValidDateTime) {
                    DomainFactory.getDomainFactory(this).setInstancesDateTime(this, mData!!.DataId, mData!!.InstanceDatas.keys, mDate!!, mTimePairPersist!!.timePair)

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
        Assert.assertTrue(toolbar != null)

        setSupportActionBar(toolbar)

        mActionBar = supportActionBar
        Assert.assertTrue(mActionBar != null)

        mActionBar!!.setDisplayHomeAsUpEnabled(true)
        mActionBar!!.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        mActionBar!!.title = null

        mSavedInstanceState = savedInstanceState

        mEditInstanceLayout = findViewById(R.id.editInstanceLayout)
        Assert.assertTrue(mEditInstanceLayout != null)

        mEditInstanceDateLayout = findViewById(R.id.editInstanceDateLayout)
        Assert.assertTrue(mEditInstanceDateLayout != null)

        mEditInstanceDate = findViewById(R.id.editInstanceDate)
        Assert.assertTrue(mEditInstanceDate != null)

        mEditInstanceDate!!.setOnClickListener {
            val datePickerDialogFragment = DatePickerDialogFragment.newInstance(mDate!!)
            datePickerDialogFragment.setListener(mDatePickerDialogFragmentListener)
            datePickerDialogFragment.show(supportFragmentManager, DATE_FRAGMENT_TAG)
        }
        val datePickerDialogFragment = supportFragmentManager.findFragmentByTag(DATE_FRAGMENT_TAG) as? DatePickerDialogFragment
        datePickerDialogFragment?.setListener(mDatePickerDialogFragmentListener)

        mEditInstanceTimeLayout = findViewById(R.id.editInstanceTimeLayout)
        Assert.assertTrue(mEditInstanceTimeLayout != null)

        mEditInstanceTime = findViewById(R.id.editInstanceTime)
        Assert.assertTrue(mEditInstanceTime != null)

        if (mSavedInstanceState != null && mSavedInstanceState!!.containsKey(DATE_KEY)) {
            mDate = mSavedInstanceState!!.getParcelable(DATE_KEY)
            Assert.assertTrue(mDate != null)

            Assert.assertTrue(mSavedInstanceState!!.containsKey(TIME_PAIR_PERSIST_KEY))
            mTimePairPersist = mSavedInstanceState!!.getParcelable(TIME_PAIR_PERSIST_KEY)
            Assert.assertTrue(mTimePairPersist != null)

            Assert.assertTrue(mSavedInstanceState!!.containsKey(INITIAL_HOUR_MINUTE_KEY))
            mInitialTimePair = mSavedInstanceState!!.getParcelable(INITIAL_HOUR_MINUTE_KEY)
            Assert.assertTrue(mInitialTimePair != null)

            Assert.assertTrue(mSavedInstanceState!!.containsKey(INITIAL_DATE_KEY))
            mInitialDate = mSavedInstanceState!!.getParcelable(INITIAL_DATE_KEY)
            Assert.assertTrue(mInitialDate != null)
        }

        supportLoaderManager.initLoader<EditInstancesLoader.Data>(0, null, this)

        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (mData != null)
                    updateError()
            }
        }

        val discardDialogFragment = supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment
        discardDialogFragment?.setDiscardDialogListener(mDiscardDialogListener)
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
            Assert.assertTrue(mDate != null)
            outState.putParcelable(DATE_KEY, mDate)

            Assert.assertTrue(mTimePairPersist != null)
            outState.putParcelable(TIME_PAIR_PERSIST_KEY, mTimePairPersist)

            Assert.assertTrue(mInitialTimePair != null)
            outState.putParcelable(INITIAL_HOUR_MINUTE_KEY, mInitialTimePair)

            Assert.assertTrue(mInitialDate != null)
            outState.putParcelable(INITIAL_DATE_KEY, mInitialDate)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<EditInstancesLoader.Data> {
        val intent = intent
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEYS))
        val instanceKeys = intent.getParcelableArrayListExtra<InstanceKey>(INSTANCE_KEYS)

        Assert.assertTrue(instanceKeys != null)
        Assert.assertTrue(instanceKeys!!.size > 1)

        return EditInstancesLoader(this, instanceKeys)
    }

    override fun onLoadFinished(loader: Loader<EditInstancesLoader.Data>, data: EditInstancesLoader.Data) {
        mData = data

        mEditInstanceLayout!!.visibility = View.VISIBLE

        if (mFirst && (mSavedInstanceState == null || !mSavedInstanceState!!.containsKey(DATE_KEY))) {
            Assert.assertTrue(mDate == null)
            Assert.assertTrue(mTimePairPersist == null)
            Assert.assertTrue(mInitialTimePair == null)
            Assert.assertTrue(mInitialDate == null)
            Assert.assertTrue(!mData!!.InstanceDatas.isEmpty())

            mFirst = false

            val dateTime = mData!!.InstanceDatas.values
                    .map { it.mInstanceDateTime }
                    .sorted()
                    .first()

            mDate = dateTime.date
            mTimePairPersist = TimePairPersist(dateTime.time.timePair)

            mInitialTimePair = mTimePairPersist!!.timePair
            mInitialDate = mDate
        }

        mActionBar!!.title = mData!!.InstanceDatas
                .values
                .joinToString(", ") { it.Name }

        invalidateOptionsMenu()

        updateDateText()

        val timePickerDialogFragment = supportFragmentManager.findFragmentByTag(TIME_FRAGMENT_TAG) as? TimePickerDialogFragment
        timePickerDialogFragment?.setListener(mTimePickerDialogFragmentListener)

        mEditInstanceTime!!.setOnClickListener {
            Assert.assertTrue(mData != null)
            val customTimeDatas = ArrayList<TimeDialogFragment.CustomTimeData>(mData!!.CustomTimeDatas.values
                    .filter { it.mCustomTimeKey.mLocalCustomTimeId != null }
                    .sortedBy { it.HourMinutes[mDate!!.dayOfWeek] }
                    .map { TimeDialogFragment.CustomTimeData(it.mCustomTimeKey, it.Name + " (" + it.HourMinutes[mDate!!.dayOfWeek] + ")") })

            val timeDialogFragment = TimeDialogFragment.newInstance(customTimeDatas)

            timeDialogFragment.setTimeDialogListener(mTimeDialogListener)

            timeDialogFragment.show(supportFragmentManager, TIME_DIALOG_FRAGMENT_TAG)
        }

        val timeDialogFragment = supportFragmentManager.findFragmentByTag(TIME_DIALOG_FRAGMENT_TAG) as? TimeDialogFragment
        timeDialogFragment?.setTimeDialogListener(mTimeDialogListener)
    }

    override fun onLoaderReset(loader: Loader<EditInstancesLoader.Data>) {}

    private fun updateDateText() {
        Assert.assertTrue(mDate != null)
        Assert.assertTrue(mEditInstanceDate != null)

        mEditInstanceDate!!.text = mDate!!.getDisplayText(this)

        updateTimeText()

        updateError()
    }

    @SuppressLint("SetTextI18n")
    private fun updateTimeText() {
        Assert.assertTrue(mTimePairPersist != null)
        Assert.assertTrue(mEditInstanceTime != null)
        Assert.assertTrue(mData != null)
        Assert.assertTrue(mDate != null)

        if (mTimePairPersist!!.customTimeKey != null) {
            val customTimeData = mData!!.CustomTimeDatas[mTimePairPersist!!.customTimeKey]
            Assert.assertTrue(customTimeData != null)

            mEditInstanceTime!!.text = customTimeData!!.Name + " (" + customTimeData.HourMinutes[mDate!!.dayOfWeek] + ")"
        } else {
            mEditInstanceTime!!.text = mTimePairPersist!!.hourMinute.toString()
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
                    if (!mData!!.CustomTimeDatas.containsKey(mTimePairPersist!!.customTimeKey))
                        return false

                    mData!!.CustomTimeDatas[mTimePairPersist!!.customTimeKey]!!.HourMinutes[mDate!!.dayOfWeek]!!
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
            mEditInstanceDateLayout!!.error = null
            mEditInstanceTimeLayout!!.error = if (isValidDateTime) null else getString(R.string.error_time)
        } else {
            mEditInstanceDateLayout!!.error = getString(R.string.error_date)
            mEditInstanceTimeLayout!!.error = null
        }
    }

    override fun onBackPressed() {
        if (tryClose())
            super.onBackPressed()
    }

    private fun tryClose(): Boolean {
        return if (dataChanged()) {
            val discardDialogFragment = DiscardDialogFragment.newInstance()
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener)
            discardDialogFragment.show(supportFragmentManager, DISCARD_TAG)

            false
        } else {
            true
        }
    }

    private fun dataChanged(): Boolean {
        if (mData == null)
            return false

        Assert.assertTrue(mInitialTimePair != null)
        Assert.assertTrue(mInitialDate != null)

        if (mTimePairPersist!!.timePair != mInitialTimePair)
            return true

        return (mInitialDate != mDate)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        Assert.assertTrue(resultCode >= 0)
        Assert.assertTrue(data == null)
        Assert.assertTrue(mTimePairPersist != null)

        if (resultCode > 0)
            mTimePairPersist!!.setCustomTimeKey(CustomTimeKey(resultCode))
    }

    companion object {
        private val INSTANCE_KEYS = "instanceKeys"

        private val DATE_KEY = "date"
        private val TIME_PAIR_PERSIST_KEY = "timePairPersist"
        private val INITIAL_HOUR_MINUTE_KEY = "initialHourMinute"
        private val INITIAL_DATE_KEY = "initialDate"

        private val DATE_FRAGMENT_TAG = "dateFragment"
        private val TIME_FRAGMENT_TAG = "timeFragment"
        private val TIME_DIALOG_FRAGMENT_TAG = "timeDialogFragment"
        private val DISCARD_TAG = "discard"

        fun getIntent(context: Context, instanceKeys: ArrayList<InstanceKey>?): Intent {
            Assert.assertTrue(instanceKeys != null)
            Assert.assertTrue(instanceKeys!!.size > 1)

            val intent = Intent(context, EditInstancesActivity::class.java)
            intent.putParcelableArrayListExtra(INSTANCE_KEYS, instanceKeys)
            return intent
        }
    }
}
