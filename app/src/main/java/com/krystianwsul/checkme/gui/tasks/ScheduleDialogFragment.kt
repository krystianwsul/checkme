package com.krystianwsul.checkme.gui.tasks

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.os.Bundle
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
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel

import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_schedule_dialog.view.*
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

        fun newInstance(scheduleDialogData: ScheduleDialogData, showDelete: Boolean) = ScheduleDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(SCHEDULE_DIALOG_DATA_KEY, scheduleDialogData)
                putBoolean(SHOW_DELETE_KEY, showDelete)
            }
        }
    }

    private lateinit var mScheduleType: Spinner

    private lateinit var mScheduleDialogDateLayout: TextInputLayout
    private lateinit var mScheduleDialogDate: TextView

    private lateinit var mScheduleDialogDayLayout: LinearLayout
    private val mScheduleDialogDays = mutableMapOf<DayOfWeek, CheckBox>()

    private lateinit var mScheduleDialogMonthLayout: LinearLayout

    private lateinit var mScheduleDialogMonthDayRadio: RadioButton
    private lateinit var mScheduleDialogMonthDayNumber: TextView
    private lateinit var mScheduleDialogMonthDayLabel: TextView

    private lateinit var mScheduleDialogMonthWeekRadio: RadioButton
    private lateinit var mScheduleDialogMonthWeekNumber: Spinner
    private lateinit var mScheduleDialogMonthWeekDay: Spinner

    private lateinit var mScheduleDialogMonthEnd: Spinner

    private lateinit var mScheduleDialogDailyPadding: View

    private lateinit var mScheduleDialogTimeLayout: TextInputLayout
    private lateinit var mScheduleDialogTime: TextView

    private lateinit var mButton: MDButton

    private var mCustomTimeDatas: Map<CustomTimeKey, CreateTaskViewModel.CustomTimeData>? = null
    private var mScheduleDialogListener: ScheduleDialogListener? = null

    private lateinit var mScheduleDialogData: ScheduleDialogData

    private var mBroadcastReceiver: BroadcastReceiver? = null

    private val mTimeDialogListener = object : TimeDialogFragment.TimeDialogListener {
        override fun onCustomTimeSelected(customTimeKey: CustomTimeKey) {
            check(mCustomTimeDatas != null)

            mScheduleDialogData.timePairPersist.customTimeKey = customTimeKey

            updateFields()
        }

        override fun onOtherSelected() {
            check(mCustomTimeDatas != null)

            TimePickerDialogFragment.newInstance(mScheduleDialogData.timePairPersist.hourMinute).let {
                it.listener = mTimePickerDialogFragmentListener
                it.show(childFragmentManager, TIME_PICKER_TAG)
            }
        }

        override fun onAddSelected() {
            startActivityForResult(ShowCustomTimeActivity.getCreateIntent(activity!!), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        }
    }

    private val mTimePickerDialogFragmentListener = { hourMinute: HourMinute ->
        check(mCustomTimeDatas != null)

        mScheduleDialogData.timePairPersist.hourMinute = hourMinute
        updateFields()
    }

    private val mDayNumberPickerDialogHandlerV2 = NumberPickerDialogFragment.NumberPickerDialogHandlerV2 { _, _, _, _, fullNumber ->
        mScheduleDialogData.monthDayNumber = fullNumber.toInt()
        check(mScheduleDialogData.monthDayNumber > 0)
        check(mScheduleDialogData.monthDayNumber < 29)

        updateFields()
    }

    private val mDatePickerDialogFragmentListener = { date: Date ->
        check(mScheduleDialogData.scheduleType == ScheduleType.SINGLE)

        mScheduleDialogData.date = date
        updateFields()
    }

    //cached data doesn't contain new custom time
    private val isValid: Boolean
        get() {
            if (mCustomTimeDatas == null)
                return false

            if (mScheduleDialogData.scheduleType != ScheduleType.SINGLE)
                return true

            val hourMinute = if (mScheduleDialogData.timePairPersist.customTimeKey != null) {
                if (!mCustomTimeDatas!!.containsKey(mScheduleDialogData.timePairPersist.customTimeKey!!))
                    return false

                mCustomTimeDatas!![mScheduleDialogData.timePairPersist.customTimeKey!!]!!.hourMinutes[mScheduleDialogData.date.dayOfWeek]!!
            } else {
                mScheduleDialogData.timePairPersist.hourMinute
            }

            return TimeStamp(mScheduleDialogData.date, hourMinute) > TimeStamp.now
        }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        check(arguments!!.containsKey(SHOW_DELETE_KEY))

        val showDelete = arguments!!.getBoolean(SHOW_DELETE_KEY)

        val builder = MaterialDialog.Builder(activity!!)
                .customView(R.layout.fragment_schedule_dialog, false)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.ok)
                .onNegative { dialog, _ -> dialog.cancel() }
                .onPositive { _, _ ->
                    check(mCustomTimeDatas != null)
                    check(mScheduleDialogListener != null)
                    check(isValid)

                    mScheduleDialogListener!!.onScheduleDialogResult(mScheduleDialogData)
                }

        if (showDelete)
            builder.neutralText(R.string.delete).onNeutral { _, _ -> mScheduleDialogListener!!.onScheduleDialogDelete() }

        return builder.build().apply {
            customView!!.run {
                mScheduleType = scheduleType
                mScheduleDialogDateLayout = scheduleDialogDateLayout
                mScheduleDialogDate = scheduleDialogDate
                mScheduleDialogDayLayout = scheduleDialogDayLayout

                mScheduleDialogDays[DayOfWeek.SUNDAY] = scheduleDialogSunday
                mScheduleDialogDays[DayOfWeek.MONDAY] = scheduleDialogMonday
                mScheduleDialogDays[DayOfWeek.TUESDAY] = scheduleDialogTuesday
                mScheduleDialogDays[DayOfWeek.WEDNESDAY] = scheduleDialogWednesday
                mScheduleDialogDays[DayOfWeek.THURSDAY] = scheduleDialogThursday
                mScheduleDialogDays[DayOfWeek.FRIDAY] = scheduleDialogFriday
                mScheduleDialogDays[DayOfWeek.SATURDAY] = scheduleDialogSaturday

                mScheduleDialogDays.forEach { (day, view) -> view.text = day.toString() }

                mScheduleDialogMonthLayout = scheduleDialogMonthLayout
                mScheduleDialogMonthDayRadio = scheduleDialogMonthDayRadio
                mScheduleDialogMonthDayNumber = scheduleDialogMonthDayNumber
                mScheduleDialogMonthDayLabel = scheduleDialogMonthDayLabel
                mScheduleDialogMonthWeekRadio = scheduleDialogMonthWeekRadio
                mScheduleDialogMonthWeekNumber = scheduleDialogMonthWeekNumber
                mScheduleDialogMonthWeekDay = scheduleDialogMonthWeekDay
                mScheduleDialogMonthEnd = scheduleDialogMonthEnd
                mScheduleDialogDailyPadding = scheduleDialogDailyPadding
                mScheduleDialogTimeLayout = scheduleDialogTimeLayout
                mScheduleDialogTime = scheduleDialogTime
            }

            mButton = getActionButton(DialogAction.POSITIVE)!!
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mScheduleDialogData = (savedInstanceState ?: arguments!!).run { getParcelable(SCHEDULE_DIALOG_DATA_KEY)!! }

        mScheduleType.run {
            adapter = ArrayAdapter.createFromResource(requireContext(), R.array.schedule_types, R.layout.spinner_no_padding).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            setSelection(when (mScheduleDialogData.scheduleType) {
                ScheduleType.SINGLE -> 0
                ScheduleType.DAILY -> throw UnsupportedOperationException()
                ScheduleType.WEEKLY -> 1
                ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> 2
            })

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                    mScheduleDialogData.scheduleType = when (i) {
                        0 -> ScheduleType.SINGLE
                        1 -> ScheduleType.WEEKLY
                        2 -> if (mScheduleDialogData.monthlyDay) ScheduleType.MONTHLY_DAY else ScheduleType.MONTHLY_WEEK
                        else -> throw UnsupportedOperationException()
                    }

                    if (activity != null && mCustomTimeDatas != null)
                        initialize()
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) = Unit
            }
        }

        mScheduleDialogTime.setOnClickListener {
            check(mCustomTimeDatas != null)

            val customTimeDatas = when (mScheduleDialogData.scheduleType) {
                ScheduleType.SINGLE -> mCustomTimeDatas!!.values
                        .filter { it.customTimeKey.localCustomTimeId != null }
                        .sortedBy { it.hourMinutes[mScheduleDialogData.date.dayOfWeek] }
                        .map { customTimeData -> TimeDialogFragment.CustomTimeData(customTimeData.customTimeKey, customTimeData.name + " (" + customTimeData.hourMinutes[mScheduleDialogData.date.dayOfWeek] + ")") }
                ScheduleType.DAILY, ScheduleType.WEEKLY, ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> mCustomTimeDatas!!.values
                        .filter { it.customTimeKey.localCustomTimeId != null }
                        .sortedBy { it.hourMinutes.values.map { it.hour * 60 + it.minute }.sum() }
                        .map { TimeDialogFragment.CustomTimeData(it.customTimeKey, it.name) }
            }

            TimeDialogFragment.newInstance(ArrayList(customTimeDatas)).let {
                it.timeDialogListener = mTimeDialogListener
                it.show(childFragmentManager, TIME_LIST_FRAGMENT_TAG)
            }
        }

        (childFragmentManager.findFragmentByTag(TIME_LIST_FRAGMENT_TAG) as? TimeDialogFragment)?.timeDialogListener = mTimeDialogListener

        (childFragmentManager.findFragmentByTag(TIME_PICKER_TAG) as? TimePickerDialogFragment)?.listener = mTimePickerDialogFragmentListener

        mScheduleDialogDate.setOnClickListener {
            check(mScheduleDialogData.scheduleType == ScheduleType.SINGLE)

            DatePickerDialogFragment.newInstance(mScheduleDialogData.date).let {
                it.listener = mDatePickerDialogFragmentListener
                it.show(childFragmentManager, DATE_FRAGMENT_TAG)
            }
        }

        (childFragmentManager.findFragmentByTag(DATE_FRAGMENT_TAG) as? DatePickerDialogFragment)?.run {
            check(mScheduleDialogData.scheduleType == ScheduleType.SINGLE)

            listener = mDatePickerDialogFragmentListener
        }

        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (mCustomTimeDatas != null)
                    updateFields()
            }
        }

        val dayListener = { day: DayOfWeek, isChecked: Boolean ->
            mScheduleDialogData.daysOfWeek.run { if (isChecked) add(day) else remove(day) }

            updateFields()
        }

        mScheduleDialogData.daysOfWeek.forEach { mScheduleDialogDays[it]!!.isChecked = true }
        mScheduleDialogDays.forEach { (day, view) -> view.setOnCheckedChangeListener { _, isChecked -> dayListener(day, isChecked) } }

        val textPrimary = ContextCompat.getColor(activity!!, R.color.textPrimary)
        val textDisabledSpinner = ContextCompat.getColor(activity!!, R.color.textDisabledSpinner)

        mScheduleDialogMonthDayRadio.run {
            setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked)
                    return@setOnCheckedChangeListener

                mScheduleDialogData.scheduleType = ScheduleType.MONTHLY_DAY

                mScheduleDialogMonthWeekRadio.isChecked = false

                mScheduleDialogData.monthlyDay = true

                mScheduleDialogMonthDayNumber.isEnabled = true
                mScheduleDialogMonthDayLabel.setTextColor(textPrimary)

                mScheduleDialogMonthWeekNumber.isEnabled = false
                mScheduleDialogMonthWeekDay.isEnabled = false
            }

            isChecked = mScheduleDialogData.monthlyDay
        }

        mScheduleDialogMonthDayNumber.setOnClickListener {
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

        (childFragmentManager.findFragmentByTag(DAY_NUMBER_PICKER_TAG) as? NumberPickerDialogFragment)?.setNumberPickerDialogHandlersV2(Vector<NumberPickerDialogFragment.NumberPickerDialogHandlerV2>(listOf(mDayNumberPickerDialogHandlerV2)))

        mScheduleDialogMonthWeekRadio.run {
            setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                if (!isChecked)
                    return@setOnCheckedChangeListener

                mScheduleDialogData.scheduleType = ScheduleType.MONTHLY_WEEK

                mScheduleDialogMonthDayRadio.isChecked = false

                mScheduleDialogData.monthlyDay = false

                mScheduleDialogMonthDayNumber.isEnabled = false
                mScheduleDialogMonthDayLabel.setTextColor(textDisabledSpinner)

                mScheduleDialogMonthWeekNumber.isEnabled = true
                mScheduleDialogMonthWeekDay.isEnabled = true
            }

            isChecked = !mScheduleDialogData.monthlyDay
        }

        mScheduleDialogMonthWeekNumber.run {
            adapter = ArrayAdapter(requireContext(), R.layout.spinner_no_padding, listOf(1, 2, 3, 4).map { Utils.ordinal(it) }).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(mScheduleDialogData.monthWeekNumber - 1)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    check(position >= 0)
                    check(position <= 3)

                    mScheduleDialogData.monthWeekNumber = position + 1
                }

                override fun onNothingSelected(parent: AdapterView<*>) = Unit
            }
        }

        mScheduleDialogMonthWeekDay.run {
            val monthWeekDayAdapter = ArrayAdapter(context, R.layout.spinner_no_padding, DayOfWeek.values()).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            adapter = monthWeekDayAdapter
            setSelection(monthWeekDayAdapter.getPosition(mScheduleDialogData.monthWeekDay))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val dayOfWeek = monthWeekDayAdapter.getItem(position)!!

                    mScheduleDialogData.monthWeekDay = dayOfWeek

                    updateFields()
                }

                override fun onNothingSelected(parent: AdapterView<*>) = Unit
            }
        }

        mScheduleDialogMonthEnd.run {
            adapter = ArrayAdapter.createFromResource(requireContext(), R.array.month, R.layout.spinner_no_padding).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            setSelection(if (mScheduleDialogData.beginningOfMonth) 0 else 1)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    check(position == 0 || position == 1)

                    mScheduleDialogData.beginningOfMonth = position == 0
                }

                override fun onNothingSelected(parent: AdapterView<*>) = Unit
            }
        }

        if (mCustomTimeDatas != null)
            initialize()
    }

    override fun onResume() {
        super.onResume()

        activity!!.registerReceiver(mBroadcastReceiver, IntentFilter(Intent.ACTION_TIME_TICK))

        if (mCustomTimeDatas != null)
            updateFields()
    }

    fun initialize(customTimeDatas: Map<CustomTimeKey, CreateTaskViewModel.CustomTimeData>, scheduleDialogListener: ScheduleDialogListener) {
        mCustomTimeDatas = customTimeDatas
        mScheduleDialogListener = scheduleDialogListener

        if (activity != null)
            initialize()
    }

    private fun initialize() {
        check(mCustomTimeDatas != null)
        check(mScheduleDialogListener != null)
        check(activity != null)

        when (mScheduleDialogData.scheduleType) {
            ScheduleType.SINGLE -> {
                mScheduleDialogDateLayout.visibility = View.VISIBLE
                mScheduleDialogDayLayout.visibility = View.GONE
                mScheduleDialogMonthLayout.visibility = View.GONE
                mScheduleDialogDailyPadding.visibility = View.GONE
                mScheduleDialogTimeLayout.visibility = View.VISIBLE
                mScheduleDialogTimeLayout.isErrorEnabled = true
            }
            ScheduleType.DAILY -> {
                mScheduleDialogDateLayout.visibility = View.GONE
                mScheduleDialogDayLayout.visibility = View.GONE
                mScheduleDialogMonthLayout.visibility = View.GONE
                mScheduleDialogDailyPadding.visibility = View.VISIBLE
                mScheduleDialogTimeLayout.visibility = View.VISIBLE
                mScheduleDialogTimeLayout.isErrorEnabled = false
            }
            ScheduleType.WEEKLY -> {
                mScheduleDialogDateLayout.visibility = View.GONE
                mScheduleDialogDayLayout.visibility = View.VISIBLE
                mScheduleDialogMonthLayout.visibility = View.GONE
                mScheduleDialogDailyPadding.visibility = View.VISIBLE
                mScheduleDialogTimeLayout.visibility = View.VISIBLE
                mScheduleDialogTimeLayout.isErrorEnabled = false
            }
            ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> {
                mScheduleDialogDateLayout.visibility = View.GONE
                mScheduleDialogDayLayout.visibility = View.GONE
                mScheduleDialogMonthLayout.visibility = View.VISIBLE
                mScheduleDialogDailyPadding.visibility = View.GONE
                mScheduleDialogTimeLayout.visibility = View.VISIBLE
                mScheduleDialogTimeLayout.isErrorEnabled = false
            }
        }

        updateFields()
    }

    override fun onPause() {
        super.onPause()

        activity!!.unregisterReceiver(mBroadcastReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(SCHEDULE_DIALOG_DATA_KEY, mScheduleDialogData)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        check(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        check(resultCode >= 0)
        check(data == null)

        if (resultCode > 0) {
            mCustomTimeDatas = null
            mScheduleDialogData.timePairPersist.customTimeKey = CustomTimeKey(resultCode)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateFields() {
        check(mCustomTimeDatas != null)

        when (mScheduleDialogData.scheduleType) {
            ScheduleType.SINGLE -> {
                mScheduleDialogDate.text = mScheduleDialogData.date.getDisplayText()

                mScheduleDialogTime.text = if (mScheduleDialogData.timePairPersist.customTimeKey != null) {
                    val customTimeData = mCustomTimeDatas!![mScheduleDialogData.timePairPersist.customTimeKey!!]!!

                    customTimeData.name + " (" + customTimeData.hourMinutes[mScheduleDialogData.date.dayOfWeek] + ")"
                } else {
                    mScheduleDialogData.timePairPersist.hourMinute.toString()
                }
            }
            ScheduleType.DAILY -> mScheduleDialogTime.text = if (mScheduleDialogData.timePairPersist.customTimeKey != null) {
                mCustomTimeDatas!![mScheduleDialogData.timePairPersist.customTimeKey!!]!!.name
            } else {
                mScheduleDialogData.timePairPersist.hourMinute.toString()
            }
            ScheduleType.WEEKLY -> mScheduleDialogTime.text = if (mScheduleDialogData.timePairPersist.customTimeKey != null) {
                val customTimeData = mCustomTimeDatas!![mScheduleDialogData.timePairPersist.customTimeKey!!]!!

                customTimeData.name
            } else {
                mScheduleDialogData.timePairPersist.hourMinute.toString()
            }
            ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> {
                mScheduleDialogMonthDayNumber.text = Utils.ordinal(mScheduleDialogData.monthDayNumber)

                mScheduleDialogTime.text = if (mScheduleDialogData.timePairPersist.customTimeKey != null) {
                    mCustomTimeDatas!![mScheduleDialogData.timePairPersist.customTimeKey!!]!!.name
                } else {
                    mScheduleDialogData.timePairPersist.hourMinute.toString()
                }
            }
        }

        if (isValid) {
            mButton.isEnabled = true

            mScheduleDialogDateLayout.error = null
            mScheduleDialogTimeLayout.error = null
        } else {
            check(mScheduleDialogData.scheduleType == ScheduleType.SINGLE)
            mButton.isEnabled = false

            if (mScheduleDialogData.date >= Date.today()) {
                mScheduleDialogDateLayout.error = null
                mScheduleDialogTimeLayout.error = getString(R.string.error_time)
            } else {
                mScheduleDialogDateLayout.error = getString(R.string.error_date)
                mScheduleDialogTimeLayout.error = null
            }
        }
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)

        check(mScheduleDialogListener != null)

        mScheduleDialogListener!!.onScheduleDialogCancel()
    }

    @Parcelize
    class ScheduleDialogData(
            var date: Date,
            var daysOfWeek: MutableSet<DayOfWeek>,
            var monthlyDay: Boolean,
            var monthDayNumber: Int,
            var monthWeekNumber: Int,
            var monthWeekDay: DayOfWeek,
            var beginningOfMonth: Boolean,
            val timePairPersist: TimePairPersist,
            var scheduleType: ScheduleType) : Parcelable {

        init {
            check(monthDayNumber > 0)
            check(monthDayNumber < 29)
            check(monthWeekNumber > 0)
            check(monthWeekNumber < 5)
        }
    }

    interface ScheduleDialogListener {

        fun onScheduleDialogResult(scheduleDialogData: ScheduleDialogData)
        fun onScheduleDialogDelete()
        fun onScheduleDialogCancel()
    }
}
