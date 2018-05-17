package com.krystianwsul.checkme.gui.instances

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.app.ActionBar
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity
import com.krystianwsul.checkme.loaders.EditInstanceLoader
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.InstanceKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePairPersist
import com.krystianwsul.checkme.utils.time.TimeStamp
import junit.framework.Assert
import kotlinx.android.synthetic.main.activity_edit_instance.*
import java.util.*

class EditInstanceActivity : AbstractActivity(), LoaderManager.LoaderCallbacks<EditInstanceLoader.Data> {

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

    private var mDate: Date? = null
    private var mData: EditInstanceLoader.Data? = null

    private lateinit var actionBar: ActionBar

    private var mSavedInstanceState: Bundle? = null

    private var mBroadcastReceiver: BroadcastReceiver? = null

    private var mTimePairPersist: TimePairPersist? = null

    private var mFirst = true

    private val mTimeDialogListener = object : TimeDialogFragment.TimeDialogListener {
        override fun onCustomTimeSelected(customTimeKey: CustomTimeKey) {
            Assert.assertTrue(mData != null)

            mTimePairPersist!!.customTimeKey = customTimeKey

            updateTimeText()

            updateError()
        }

        override fun onOtherSelected() {
            Assert.assertTrue(mData != null)

            val timePickerDialogFragment = TimePickerDialogFragment.newInstance(mTimePairPersist!!.hourMinute)
            timePickerDialogFragment.listener = mTimePickerDialogFragmentListener
            timePickerDialogFragment.show(supportFragmentManager, TIME_FRAGMENT_TAG)
        }

        override fun onAddSelected() {
            startActivityForResult(ShowCustomTimeActivity.getCreateIntent(this@EditInstanceActivity), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        }
    }

    private val mTimePickerDialogFragmentListener = { hourMinute: HourMinute ->
        Assert.assertTrue(mData != null)

        mTimePairPersist!!.hourMinute = hourMinute
        updateTimeText()
        updateError()
    }

    private val mDiscardDialogListener = this@EditInstanceActivity::finish

    private val mDatePickerDialogFragment = { date: Date ->
        mDate = date
        updateDateText()
    }

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
                Assert.assertTrue(mData != null)
                Assert.assertTrue(mData!!.showHour)

                supportLoaderManager.destroyLoader(0)

                DomainFactory.getDomainFactory().setInstanceAddHourActivity(this, mData!!.dataId, SaveService.Source.GUI, mData!!.instanceKey)

                finish()
            }
            R.id.action_edit_instance_save -> {
                Assert.assertTrue(mDate != null)
                Assert.assertTrue(mData != null)

                if (isValidDateTime) {
                    supportLoaderManager.destroyLoader(0)
                    DomainFactory.getDomainFactory().setInstanceDateTime(this, mData!!.dataId, SaveService.Source.GUI, mData!!.instanceKey, mDate!!, mTimePairPersist!!.timePair)
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

        actionBar = supportActionBar!!

        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        actionBar.title = null

        mSavedInstanceState = savedInstanceState

        editInstanceDate.setOnClickListener {
            val datePickerDialogFragment = DatePickerDialogFragment.newInstance(mDate!!)
            datePickerDialogFragment.listener = mDatePickerDialogFragment
            datePickerDialogFragment.show(supportFragmentManager, DATE_FRAGMENT_TAG)
        }
        val datePickerDialogFragment = supportFragmentManager.findFragmentByTag(DATE_FRAGMENT_TAG) as? DatePickerDialogFragment
        datePickerDialogFragment?.listener = mDatePickerDialogFragment

        if (mSavedInstanceState != null && mSavedInstanceState!!.containsKey(DATE_KEY)) {
            mDate = mSavedInstanceState!!.getParcelable(DATE_KEY)
            Assert.assertTrue(mDate != null)

            Assert.assertTrue(mSavedInstanceState!!.containsKey(TIME_PAIR_PERSIST_KEY))
            mTimePairPersist = mSavedInstanceState!!.getParcelable(TIME_PAIR_PERSIST_KEY)
            Assert.assertTrue(mTimePairPersist != null)
        }

        supportLoaderManager.initLoader<EditInstanceLoader.Data>(0, null, this)

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
            Assert.assertTrue(mDate != null)
            outState.putParcelable(DATE_KEY, mDate)

            Assert.assertTrue(mTimePairPersist != null)
            outState.putParcelable(TIME_PAIR_PERSIST_KEY, mTimePairPersist)
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<EditInstanceLoader.Data> {
        val intent = intent
        Assert.assertTrue(intent.hasExtra(INSTANCE_KEY))

        val instanceKey = intent.getParcelableExtra<InstanceKey>(INSTANCE_KEY)
        Assert.assertTrue(instanceKey != null)

        return EditInstanceLoader(this, instanceKey!!)
    }

    override fun onLoadFinished(loader: Loader<EditInstanceLoader.Data>, data: EditInstanceLoader.Data) {
        mData = data

        if (data.done) {
            Toast.makeText(this, R.string.instanceMarkedDone, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        editInstanceLayout.visibility = View.VISIBLE

        if (mFirst && (mSavedInstanceState == null || !mSavedInstanceState!!.containsKey(DATE_KEY))) {
            Assert.assertTrue(mDate == null)
            Assert.assertTrue(mTimePairPersist == null)

            mFirst = false

            mDate = mData!!.instanceDate
            mTimePairPersist = TimePairPersist(mData!!.instanceTimePair)
        }

        actionBar.title = data.name

        invalidateOptionsMenu()

        updateDateText()

        val timePickerDialogFragment = supportFragmentManager.findFragmentByTag(TIME_FRAGMENT_TAG) as? TimePickerDialogFragment
        timePickerDialogFragment?.listener = mTimePickerDialogFragmentListener

        editInstanceTime.setOnClickListener {
            Assert.assertTrue(mData != null)
            val customTimeDatas = ArrayList<TimeDialogFragment.CustomTimeData>(mData!!.customTimeDatas.values
                    .filter { it.customTimeKey.localCustomTimeId != null }
                    .sortedBy { it.hourMinutes[mDate!!.dayOfWeek] }
                    .map { TimeDialogFragment.CustomTimeData(it.customTimeKey, it.name + " (" + it.hourMinutes[mDate!!.dayOfWeek] + ")") })

            val timeDialogFragment = TimeDialogFragment.newInstance(customTimeDatas)

            timeDialogFragment.timeDialogListener = mTimeDialogListener

            timeDialogFragment.show(supportFragmentManager, TIME_DIALOG_FRAGMENT_TAG)
        }

        val timeDialogFragment = supportFragmentManager.findFragmentByTag(TIME_DIALOG_FRAGMENT_TAG) as? TimeDialogFragment
        timeDialogFragment?.timeDialogListener = mTimeDialogListener
    }

    override fun onLoaderReset(loader: Loader<EditInstanceLoader.Data>) {}

    private fun updateDateText() {
        Assert.assertTrue(mDate != null)

        editInstanceDate.setText(mDate!!.getDisplayText(this))

        updateTimeText()

        updateError()
    }

    @SuppressLint("SetTextI18n")
    private fun updateTimeText() {
        Assert.assertTrue(mTimePairPersist != null)
        Assert.assertTrue(mData != null)
        Assert.assertTrue(mDate != null)

        if (mTimePairPersist!!.customTimeKey != null) {
            val customTimeData = mData!!.customTimeDatas[mTimePairPersist!!.customTimeKey]
            Assert.assertTrue(customTimeData != null)

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

        if (mData!!.instanceDate != mDate)
            return true

        return (mData!!.instanceTimePair != mTimePairPersist!!.timePair)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        Assert.assertTrue(resultCode >= 0)
        Assert.assertTrue(data == null)
        Assert.assertTrue(mTimePairPersist != null)

        if (resultCode > 0)
            mTimePairPersist!!.customTimeKey = CustomTimeKey(resultCode)
    }
}
