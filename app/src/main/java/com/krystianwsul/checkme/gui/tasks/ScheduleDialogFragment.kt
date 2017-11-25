package com.krystianwsul.checkme.gui.tasks

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.design.widget.TextInputLayout
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.*
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.internal.MDButton
import com.codetroopers.betterpickers.numberpicker.NumberPickerBuilder
import com.codetroopers.betterpickers.numberpicker.NumberPickerDialogFragment
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.AbstractDialogFragment
import com.krystianwsul.checkme.gui.DatePickerDialogFragment
import com.krystianwsul.checkme.gui.TimeDialogFragment
import com.krystianwsul.checkme.gui.TimePickerDialogFragment
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity
import com.krystianwsul.checkme.loaders.CreateTaskLoader
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import junit.framework.Assert
import java.math.BigDecimal
import java.util.*

class ScheduleDialogFragment : AbstractDialogFragment() {

    companion object {

        private val SCHEDULE_DIALOG_DATA_KEY = "scheduleDialogData"
        private val SHOW_DELETE_KEY = "showDelete"

        private val DATE_FRAGMENT_TAG = "dateFragment"
        private val TIME_LIST_FRAGMENT_TAG = "timeListFragment"
        private val TIME_PICKER_TAG = "timePicker"

        private val DAY_NUMBER_PICKER_TAG = "day_number_dialog"

        fun newInstance(scheduleDialogData: ScheduleDialogData, showDelete: Boolean): ScheduleDialogFragment {
            val scheduleDialogFragment = ScheduleDialogFragment()

            val args = Bundle()
            args.putParcelable(SCHEDULE_DIALOG_DATA_KEY, scheduleDialogData)
            args.putBoolean(SHOW_DELETE_KEY, showDelete)
            scheduleDialogFragment.arguments = args

            return scheduleDialogFragment
        }
    }

    private var mScheduleType: Spinner? = null

    private var mScheduleDialogDateLayout: TextInputLayout? = null
    private var mScheduleDialogDate: TextView? = null

    private var mScheduleDialogDay: Spinner? = null

    private var mScheduleDialogMonthLayout: LinearLayout? = null

    private var mScheduleDialogMonthDayRadio: RadioButton? = null
    private var mScheduleDialogMonthDayNumber: TextView? = null
    private var mScheduleDialogMonthDayLabel: TextView? = null

    private var mScheduleDialogMonthWeekRadio: RadioButton? = null
    private var mScheduleDialogMonthWeekNumber: Spinner? = null
    private var mScheduleDialogMonthWeekDay: Spinner? = null

    private var mScheduleDialogMonthEnd: Spinner? = null

    private var mScheduleDialogDailyPadding: View? = null

    private var mScheduleDialogTimeLayout: TextInputLayout? = null
    private var mScheduleDialogTime: TextView? = null

    private var mButton: MDButton? = null

    private var mCustomTimeDatas: Map<CustomTimeKey, CreateTaskLoader.CustomTimeData>? = null
    private var mScheduleDialogListener: ScheduleDialogListener? = null

    private var mScheduleDialogData: ScheduleDialogData? = null

    private var mBroadcastReceiver: BroadcastReceiver? = null

    private val mTimeDialogListener = object : TimeDialogFragment.TimeDialogListener {
        override fun onCustomTimeSelected(customTimeKey: CustomTimeKey) {
            Assert.assertTrue(mCustomTimeDatas != null)

            mScheduleDialogData!!.mTimePairPersist.setCustomTimeKey(customTimeKey)

            updateFields()
        }

        override fun onOtherSelected() {
            Assert.assertTrue(mCustomTimeDatas != null)

            val timePickerDialogFragment = TimePickerDialogFragment.newInstance(mScheduleDialogData!!.mTimePairPersist.hourMinute)
            timePickerDialogFragment.setListener(mTimePickerDialogFragmentListener)
            timePickerDialogFragment.show(childFragmentManager, TIME_PICKER_TAG)
        }

        override fun onAddSelected() {
            startActivityForResult(ShowCustomTimeActivity.getCreateIntent(activity), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        }
    }

    private val mTimePickerDialogFragmentListener = { hourMinute: HourMinute ->
        Assert.assertTrue(mCustomTimeDatas != null)

        mScheduleDialogData!!.mTimePairPersist.hourMinute = hourMinute
        updateFields()
    }

    private val mDayNumberPickerDialogHandlerV2 = NumberPickerDialogFragment.NumberPickerDialogHandlerV2 { _, _, _, _, fullNumber ->
        mScheduleDialogData!!.mMonthDayNumber = fullNumber.toInt()
        Assert.assertTrue(mScheduleDialogData!!.mMonthDayNumber > 0)
        Assert.assertTrue(mScheduleDialogData!!.mMonthDayNumber < 29)

        updateFields()
    }

    private val mDatePickerDialogFragmentListener = { date: Date ->
        Assert.assertTrue(mScheduleDialogData!!.mScheduleType == ScheduleType.SINGLE)

        mScheduleDialogData!!.mDate = date
        updateFields()
    }

    private//cached data doesn't contain new custom time
    val isValid: Boolean
        get() {
            if (mCustomTimeDatas == null)
                return false

            if (mScheduleDialogData == null)
                return false

            if (mScheduleDialogData!!.mScheduleType != ScheduleType.SINGLE)
                return true

            val hourMinute = if (mScheduleDialogData!!.mTimePairPersist.customTimeKey != null) {
                if (!mCustomTimeDatas!!.containsKey(mScheduleDialogData!!.mTimePairPersist.customTimeKey))
                    return false

                mCustomTimeDatas!![mScheduleDialogData!!.mTimePairPersist.customTimeKey]!!.HourMinutes[mScheduleDialogData!!.mDate.dayOfWeek]!!
            } else {
                mScheduleDialogData!!.mTimePairPersist.hourMinute
            }

            return TimeStamp(mScheduleDialogData!!.mDate, hourMinute) > TimeStamp.getNow()
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments
        Assert.assertTrue(args != null)
        Assert.assertTrue(args!!.containsKey(SHOW_DELETE_KEY))

        val showDelete = args.getBoolean(SHOW_DELETE_KEY)

        val builder = MaterialDialog.Builder(activity)
                .customView(R.layout.fragment_schedule_dialog, false)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .onNegative { dialog, _ -> dialog.cancel() }
                .onPositive { _, _ ->
                    Assert.assertTrue(mCustomTimeDatas != null)
                    Assert.assertTrue(mScheduleDialogListener != null)
                    Assert.assertTrue(isValid)

                    mScheduleDialogListener!!.onScheduleDialogResult(mScheduleDialogData!!)
                }

        if (showDelete)
            builder.neutralText(R.string.delete).onNeutral { _, _ -> mScheduleDialogListener!!.onScheduleDialogDelete() }

        val materialDialog = builder.build()

        val view = materialDialog.customView
        Assert.assertTrue(view != null)

        mScheduleType = view!!.findViewById<View>(R.id.schedule_type) as Spinner
        Assert.assertTrue(mScheduleType != null)

        mScheduleDialogDateLayout = view.findViewById<View>(R.id.schedule_dialog_date_layout) as TextInputLayout
        Assert.assertTrue(mScheduleDialogDateLayout != null)

        mScheduleDialogDate = view.findViewById<View>(R.id.schedule_dialog_date) as TextView
        Assert.assertTrue(mScheduleDialogDate != null)

        mScheduleDialogDay = view.findViewById<View>(R.id.schedule_dialog_day) as Spinner
        Assert.assertTrue(mScheduleDialogDay != null)

        mScheduleDialogMonthLayout = view.findViewById<View>(R.id.schedule_dialog_month_layout) as LinearLayout
        Assert.assertTrue(mScheduleDialogMonthLayout != null)

        mScheduleDialogMonthDayRadio = view.findViewById<View>(R.id.schedule_dialog_month_day_radio) as RadioButton
        Assert.assertTrue(mScheduleDialogMonthDayRadio != null)

        mScheduleDialogMonthDayNumber = view.findViewById<View>(R.id.schedule_dialog_month_day_number) as TextView
        Assert.assertTrue(mScheduleDialogMonthDayNumber != null)

        mScheduleDialogMonthDayLabel = view.findViewById<View>(R.id.schedule_dialog_month_day_label) as TextView
        Assert.assertTrue(mScheduleDialogMonthDayLabel != null)

        mScheduleDialogMonthWeekRadio = view.findViewById<View>(R.id.schedule_dialog_month_week_radio) as RadioButton
        Assert.assertTrue(mScheduleDialogMonthWeekRadio != null)

        mScheduleDialogMonthWeekNumber = view.findViewById<View>(R.id.schedule_dialog_month_week_number) as Spinner
        Assert.assertTrue(mScheduleDialogMonthWeekNumber != null)

        mScheduleDialogMonthWeekDay = view.findViewById<View>(R.id.schedule_dialog_month_week_day) as Spinner
        Assert.assertTrue(mScheduleDialogMonthWeekDay != null)

        mScheduleDialogMonthEnd = view.findViewById<View>(R.id.schedule_dialog_month_end) as Spinner
        Assert.assertTrue(mScheduleDialogMonthEnd != null)

        mScheduleDialogDailyPadding = view.findViewById(R.id.schedule_dialog_daily_padding)
        Assert.assertTrue(mScheduleDialogDailyPadding != null)

        mScheduleDialogTimeLayout = view.findViewById<View>(R.id.schedule_dialog_time_layout) as TextInputLayout
        Assert.assertTrue(mScheduleDialogTimeLayout != null)

        mScheduleDialogTime = view.findViewById<View>(R.id.schedule_dialog_time) as TextView
        Assert.assertTrue(mScheduleDialogTime != null)

        mButton = materialDialog.getActionButton(DialogAction.POSITIVE)
        Assert.assertTrue(mButton != null)

        return materialDialog
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(SCHEDULE_DIALOG_DATA_KEY))

            mScheduleDialogData = savedInstanceState.getParcelable(SCHEDULE_DIALOG_DATA_KEY)
        } else {
            val args = arguments
            Assert.assertTrue(args != null)
            Assert.assertTrue(args!!.containsKey(SCHEDULE_DIALOG_DATA_KEY))

            mScheduleDialogData = args.getParcelable(SCHEDULE_DIALOG_DATA_KEY)
            Assert.assertTrue(mScheduleDialogData != null)
        }

        val typeAdapter = ArrayAdapter.createFromResource(activity, R.array.schedule_types, R.layout.spinner_no_padding)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mScheduleType!!.adapter = typeAdapter

        when (mScheduleDialogData!!.mScheduleType) {
            ScheduleType.SINGLE -> mScheduleType!!.setSelection(0)
            ScheduleType.DAILY -> mScheduleType!!.setSelection(1)
            ScheduleType.WEEKLY -> mScheduleType!!.setSelection(2)
            ScheduleType.MONTHLY_DAY -> mScheduleType!!.setSelection(3)
            ScheduleType.MONTHLY_WEEK -> mScheduleType!!.setSelection(3)
        }

        mScheduleType!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                when (i) {
                    0 -> mScheduleDialogData!!.mScheduleType = ScheduleType.SINGLE
                    1 -> mScheduleDialogData!!.mScheduleType = ScheduleType.DAILY
                    2 -> mScheduleDialogData!!.mScheduleType = ScheduleType.WEEKLY
                    3 -> if (mScheduleDialogData!!.mMonthlyDay)
                        mScheduleDialogData!!.mScheduleType = ScheduleType.MONTHLY_DAY
                    else
                        mScheduleDialogData!!.mScheduleType = ScheduleType.MONTHLY_WEEK
                    else -> throw UnsupportedOperationException()
                }

                if (activity != null && mCustomTimeDatas != null)
                    initialize()
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {

            }
        }

        mScheduleDialogTime!!.setOnClickListener {
            Assert.assertTrue(mCustomTimeDatas != null)

            val customTimeDatas = when (mScheduleDialogData!!.mScheduleType) {
                ScheduleType.SINGLE -> mCustomTimeDatas!!.values
                        .filter { it.mCustomTimeKey.mLocalCustomTimeId != null }
                        .sortedBy { it.HourMinutes[mScheduleDialogData!!.mDate.dayOfWeek] }
                        .map { customTimeData -> TimeDialogFragment.CustomTimeData(customTimeData.mCustomTimeKey, customTimeData.Name + " (" + customTimeData.HourMinutes[mScheduleDialogData!!.mDate.dayOfWeek] + ")") }
                ScheduleType.DAILY, ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> mCustomTimeDatas!!.values
                        .filter { it.mCustomTimeKey.mLocalCustomTimeId != null }
                        .sortedBy { it.HourMinutes.values.map { it.hour * 60 + it.minute }.sum() }
                        .map { TimeDialogFragment.CustomTimeData(it.mCustomTimeKey, it.Name) }
                ScheduleType.WEEKLY -> mCustomTimeDatas!!.values
                        .filter { it.mCustomTimeKey.mLocalCustomTimeId != null }
                        .sortedBy { it.HourMinutes[mScheduleDialogData!!.mDayOfWeek] }
                        .map { customTimeData -> TimeDialogFragment.CustomTimeData(customTimeData.mCustomTimeKey, customTimeData.Name + " (" + customTimeData.HourMinutes[mScheduleDialogData!!.mDayOfWeek] + ")") }
            }

            val timeDialogFragment = TimeDialogFragment.newInstance(ArrayList(customTimeDatas))

            timeDialogFragment.setTimeDialogListener(mTimeDialogListener)

            timeDialogFragment.show(childFragmentManager, TIME_LIST_FRAGMENT_TAG)
        }

        val timeDialogFragment = childFragmentManager.findFragmentByTag(TIME_LIST_FRAGMENT_TAG) as? TimeDialogFragment
        timeDialogFragment?.setTimeDialogListener(mTimeDialogListener)

        val timePickerDialogFragment = childFragmentManager.findFragmentByTag(TIME_PICKER_TAG) as? TimePickerDialogFragment
        timePickerDialogFragment?.setListener(mTimePickerDialogFragmentListener)

        mScheduleDialogDate!!.setOnClickListener {
            Assert.assertTrue(mScheduleDialogData!!.mScheduleType == ScheduleType.SINGLE)

            val datePickerDialogFragment = DatePickerDialogFragment.newInstance(mScheduleDialogData!!.mDate)
            datePickerDialogFragment.setListener(mDatePickerDialogFragmentListener)
            datePickerDialogFragment.show(childFragmentManager, DATE_FRAGMENT_TAG)
        }

        val datePickerDialogFragment = childFragmentManager.findFragmentByTag(DATE_FRAGMENT_TAG) as? DatePickerDialogFragment
        if (datePickerDialogFragment != null) {
            Assert.assertTrue(mScheduleDialogData!!.mScheduleType == ScheduleType.SINGLE)

            datePickerDialogFragment.setListener(mDatePickerDialogFragmentListener)
        }

        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (mCustomTimeDatas != null)
                    updateFields()
            }
        }

        val dayOfWeekAdapter = ArrayAdapter(context, R.layout.spinner_no_padding, DayOfWeek.values())
        dayOfWeekAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mScheduleDialogDay!!.adapter = dayOfWeekAdapter
        mScheduleDialogDay!!.setSelection(dayOfWeekAdapter.getPosition(mScheduleDialogData!!.mDayOfWeek))
        mScheduleDialogDay!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val dayOfWeek = dayOfWeekAdapter.getItem(position)
                Assert.assertTrue(dayOfWeek != null)

                mScheduleDialogData!!.mDayOfWeek = dayOfWeek

                updateFields()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        val textPrimary = ContextCompat.getColor(activity, R.color.textPrimary)
        val textDisabledSpinner = ContextCompat.getColor(activity, R.color.textDisabledSpinner)

        mScheduleDialogMonthDayRadio!!.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked)
                return@setOnCheckedChangeListener

            mScheduleDialogData!!.mScheduleType = ScheduleType.MONTHLY_DAY

            mScheduleDialogMonthWeekRadio!!.isChecked = false

            mScheduleDialogData!!.mMonthlyDay = true

            mScheduleDialogMonthDayNumber!!.isEnabled = true
            mScheduleDialogMonthDayLabel!!.setTextColor(textPrimary)

            mScheduleDialogMonthWeekNumber!!.isEnabled = false
            mScheduleDialogMonthWeekDay!!.isEnabled = false
        }

        mScheduleDialogMonthDayRadio!!.isChecked = mScheduleDialogData!!.mMonthlyDay

        mScheduleDialogMonthDayNumber!!.setOnClickListener {
            NumberPickerBuilder()
                    .setPlusMinusVisibility(View.GONE)
                    .setDecimalVisibility(View.GONE)
                    .setMinNumber(BigDecimal.ONE)
                    .setMaxNumber(BigDecimal(28))
                    .setStyleResId(R.style.BetterPickersDialogFragment_Light)
                    .setFragmentManager(childFragmentManager)
                    .addNumberPickerDialogHandler(mDayNumberPickerDialogHandlerV2)
                    .show()
        }

        val dayNumberPickerDialogFragment = childFragmentManager.findFragmentByTag(DAY_NUMBER_PICKER_TAG) as? NumberPickerDialogFragment
        dayNumberPickerDialogFragment?.setNumberPickerDialogHandlersV2(Vector<NumberPickerDialogFragment.NumberPickerDialogHandlerV2>(listOf(mDayNumberPickerDialogHandlerV2)))

        mScheduleDialogMonthWeekRadio!!.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            if (!isChecked)
                return@setOnCheckedChangeListener

            mScheduleDialogData!!.mScheduleType = ScheduleType.MONTHLY_WEEK

            mScheduleDialogMonthDayRadio!!.isChecked = false

            mScheduleDialogData!!.mMonthlyDay = false

            mScheduleDialogMonthDayNumber!!.isEnabled = false
            mScheduleDialogMonthDayLabel!!.setTextColor(textDisabledSpinner)

            mScheduleDialogMonthWeekNumber!!.isEnabled = true
            mScheduleDialogMonthWeekDay!!.isEnabled = true
        }

        mScheduleDialogMonthWeekRadio!!.isChecked = !mScheduleDialogData!!.mMonthlyDay

        val monthWeekNumberAdapter = ArrayAdapter(activity, R.layout.spinner_no_padding, listOf(1, 2, 3, 4).map { Utils.ordinal(it) })
        monthWeekNumberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mScheduleDialogMonthWeekNumber!!.adapter = monthWeekNumberAdapter
        mScheduleDialogMonthWeekNumber!!.setSelection(mScheduleDialogData!!.mMonthWeekNumber - 1)
        mScheduleDialogMonthWeekNumber!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                Assert.assertTrue(position >= 0)
                Assert.assertTrue(position <= 3)

                mScheduleDialogData!!.mMonthWeekNumber = position + 1
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        val monthWeekDayAdapter = ArrayAdapter(context, R.layout.spinner_no_padding, DayOfWeek.values())
        monthWeekDayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mScheduleDialogMonthWeekDay!!.adapter = monthWeekDayAdapter
        mScheduleDialogMonthWeekDay!!.setSelection(monthWeekDayAdapter.getPosition(mScheduleDialogData!!.mMonthWeekDay))
        mScheduleDialogMonthWeekDay!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val dayOfWeek = monthWeekDayAdapter.getItem(position)
                Assert.assertTrue(dayOfWeek != null)

                mScheduleDialogData!!.mMonthWeekDay = dayOfWeek

                updateFields()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        val monthEndAdapter = ArrayAdapter.createFromResource(activity, R.array.month, R.layout.spinner_no_padding)
        monthEndAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mScheduleDialogMonthEnd!!.adapter = monthEndAdapter

        mScheduleDialogMonthEnd!!.setSelection(if (mScheduleDialogData!!.mBeginningOfMonth) 0 else 1)

        mScheduleDialogMonthEnd!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                Assert.assertTrue(position == 0 || position == 1)

                mScheduleDialogData!!.mBeginningOfMonth = position == 0
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        if (mCustomTimeDatas != null)
            initialize()
    }

    override fun onResume() {
        super.onResume()

        activity.registerReceiver(mBroadcastReceiver, IntentFilter(Intent.ACTION_TIME_TICK))

        if (mCustomTimeDatas != null)
            updateFields()
    }

    fun initialize(customTimeDatas: Map<CustomTimeKey, CreateTaskLoader.CustomTimeData>, scheduleDialogListener: ScheduleDialogListener) {
        mCustomTimeDatas = customTimeDatas
        mScheduleDialogListener = scheduleDialogListener

        if (activity != null)
            initialize()
    }

    private fun initialize() {
        Assert.assertTrue(mCustomTimeDatas != null)
        Assert.assertTrue(mScheduleDialogListener != null)
        Assert.assertTrue(mScheduleDialogData != null)
        Assert.assertTrue(mScheduleDialogTime != null)
        Assert.assertTrue(activity != null)

        when (mScheduleDialogData!!.mScheduleType) {
            ScheduleType.SINGLE -> {
                mScheduleDialogDateLayout!!.visibility = View.VISIBLE
                mScheduleDialogDay!!.visibility = View.GONE
                mScheduleDialogMonthLayout!!.visibility = View.GONE
                mScheduleDialogDailyPadding!!.visibility = View.GONE
                mScheduleDialogTimeLayout!!.visibility = View.VISIBLE
                mScheduleDialogTimeLayout!!.isErrorEnabled = true
            }
            ScheduleType.DAILY -> {
                mScheduleDialogDateLayout!!.visibility = View.GONE
                mScheduleDialogDay!!.visibility = View.GONE
                mScheduleDialogMonthLayout!!.visibility = View.GONE
                mScheduleDialogDailyPadding!!.visibility = View.VISIBLE
                mScheduleDialogTimeLayout!!.visibility = View.VISIBLE
                mScheduleDialogTimeLayout!!.isErrorEnabled = false
            }
            ScheduleType.WEEKLY -> {
                mScheduleDialogDateLayout!!.visibility = View.GONE
                mScheduleDialogDay!!.visibility = View.VISIBLE
                mScheduleDialogMonthLayout!!.visibility = View.GONE
                mScheduleDialogDailyPadding!!.visibility = View.VISIBLE
                mScheduleDialogTimeLayout!!.visibility = View.VISIBLE
                mScheduleDialogTimeLayout!!.isErrorEnabled = false
            }
            ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> {
                mScheduleDialogDateLayout!!.visibility = View.GONE
                mScheduleDialogDay!!.visibility = View.GONE
                mScheduleDialogMonthLayout!!.visibility = View.VISIBLE
                mScheduleDialogDailyPadding!!.visibility = View.GONE
                mScheduleDialogTimeLayout!!.visibility = View.VISIBLE
                mScheduleDialogTimeLayout!!.isErrorEnabled = false
            }
        }

        updateFields()
    }

    override fun onPause() {
        super.onPause()

        activity.unregisterReceiver(mBroadcastReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        outState!!.putParcelable(SCHEDULE_DIALOG_DATA_KEY, mScheduleDialogData)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Assert.assertTrue(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        Assert.assertTrue(resultCode >= 0)
        Assert.assertTrue(data == null)

        if (resultCode > 0) {
            mCustomTimeDatas = null
            mScheduleDialogData!!.mTimePairPersist.setCustomTimeKey(CustomTimeKey(resultCode))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateFields() {
        Assert.assertTrue(mScheduleDialogData != null)
        Assert.assertTrue(mScheduleDialogDate != null)
        Assert.assertTrue(mScheduleDialogTime != null)
        Assert.assertTrue(mCustomTimeDatas != null)

        when (mScheduleDialogData!!.mScheduleType) {
            ScheduleType.SINGLE -> {
                mScheduleDialogDate!!.text = mScheduleDialogData!!.mDate.getDisplayText(context)

                if (mScheduleDialogData!!.mTimePairPersist.customTimeKey != null) {
                    val customTimeData = mCustomTimeDatas!![mScheduleDialogData!!.mTimePairPersist.customTimeKey]
                    Assert.assertTrue(customTimeData != null)

                    mScheduleDialogTime!!.text = customTimeData!!.Name + " (" + customTimeData.HourMinutes[mScheduleDialogData!!.mDate.dayOfWeek] + ")"
                } else {
                    mScheduleDialogTime!!.text = mScheduleDialogData!!.mTimePairPersist.hourMinute.toString()
                }
            }
            ScheduleType.DAILY -> if (mScheduleDialogData!!.mTimePairPersist.customTimeKey != null) {
                val customTimeData = mCustomTimeDatas!![mScheduleDialogData!!.mTimePairPersist.customTimeKey]
                Assert.assertTrue(customTimeData != null)

                mScheduleDialogTime!!.text = customTimeData!!.Name
            } else {
                mScheduleDialogTime!!.text = mScheduleDialogData!!.mTimePairPersist.hourMinute.toString()
            }
            ScheduleType.WEEKLY -> if (mScheduleDialogData!!.mTimePairPersist.customTimeKey != null) {
                val customTimeData = mCustomTimeDatas!![mScheduleDialogData!!.mTimePairPersist.customTimeKey]
                Assert.assertTrue(customTimeData != null)

                mScheduleDialogTime!!.text = customTimeData!!.Name + " (" + customTimeData.HourMinutes[mScheduleDialogData!!.mDayOfWeek] + ")"
            } else {
                mScheduleDialogTime!!.text = mScheduleDialogData!!.mTimePairPersist.hourMinute.toString()
            }
            ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> {
                mScheduleDialogMonthDayNumber!!.text = Utils.ordinal(mScheduleDialogData!!.mMonthDayNumber)

                if (mScheduleDialogData!!.mTimePairPersist.customTimeKey != null) {
                    val customTimeData = mCustomTimeDatas!![mScheduleDialogData!!.mTimePairPersist.customTimeKey]
                    Assert.assertTrue(customTimeData != null)

                    mScheduleDialogTime!!.text = customTimeData!!.Name
                } else {
                    mScheduleDialogTime!!.text = mScheduleDialogData!!.mTimePairPersist.hourMinute.toString()
                }
            }
        }

        if (isValid) {
            mButton!!.isEnabled = true

            mScheduleDialogDateLayout!!.error = null
            mScheduleDialogTimeLayout!!.error = null
        } else {
            Assert.assertTrue(mScheduleDialogData!!.mScheduleType == ScheduleType.SINGLE)
            mButton!!.isEnabled = false

            if (mScheduleDialogData!!.mDate >= Date.today()) {
                mScheduleDialogDateLayout!!.error = null
                mScheduleDialogTimeLayout!!.error = getString(R.string.error_time)
            } else {
                mScheduleDialogDateLayout!!.error = getString(R.string.error_date)
                mScheduleDialogTimeLayout!!.error = null
            }
        }
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)

        Assert.assertTrue(mScheduleDialogListener != null)

        mScheduleDialogListener!!.onScheduleDialogCancel()
    }

    class ScheduleDialogData(var mDate: Date, var mDayOfWeek: DayOfWeek, var mMonthlyDay: Boolean, var mMonthDayNumber: Int, var mMonthWeekNumber: Int, var mMonthWeekDay: DayOfWeek, var mBeginningOfMonth: Boolean, val mTimePairPersist: TimePairPersist, var mScheduleType: ScheduleType) : Parcelable {

        init {
            Assert.assertTrue(mMonthDayNumber > 0)
            Assert.assertTrue(mMonthDayNumber < 29)
            Assert.assertTrue(mMonthWeekNumber > 0)
            Assert.assertTrue(mMonthWeekNumber < 5)
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeParcelable(mDate, flags)
            dest.writeSerializable(mDayOfWeek)
            dest.writeInt(if (mMonthlyDay) 1 else 0)
            dest.writeInt(mMonthDayNumber)
            dest.writeInt(mMonthWeekNumber)
            dest.writeSerializable(mMonthWeekDay)
            dest.writeInt(if (mBeginningOfMonth) 1 else 0)
            dest.writeParcelable(mTimePairPersist, flags)
            dest.writeSerializable(mScheduleType)
        }

        override fun describeContents() = 0

        companion object {

            val CREATOR: Parcelable.Creator<ScheduleDialogData> = object : Parcelable.Creator<ScheduleDialogData> {
                override fun createFromParcel(parcel: Parcel): ScheduleDialogData {
                    val date = parcel.readParcelable<Date>(Date::class.java.classLoader)
                    val dayOfWeek = parcel.readSerializable() as DayOfWeek
                    val monthDay = parcel.readInt() == 1
                    val monthDayNumber = parcel.readInt()
                    val monthWeekNumber = parcel.readInt()
                    val monthWeekDay = parcel.readSerializable() as DayOfWeek
                    val beginningOfMonth = parcel.readInt() == 1
                    val timePairPersist = parcel.readParcelable<TimePairPersist>(TimePairPersist::class.java.classLoader)
                    val scheduleType = parcel.readSerializable() as ScheduleType

                    return ScheduleDialogData(date, dayOfWeek, monthDay, monthDayNumber, monthWeekNumber, monthWeekDay, beginningOfMonth, timePairPersist, scheduleType)
                }

                override fun newArray(size: Int) = arrayOfNulls<ScheduleDialogData>(size)
            }
        }
    }

    interface ScheduleDialogListener {
        fun onScheduleDialogResult(scheduleDialogData: ScheduleDialogData)

        fun onScheduleDialogDelete()

        fun onScheduleDialogCancel()
    }
}
