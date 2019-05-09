package com.krystianwsul.checkme.gui.tasks

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.gui.*
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.startTicks
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_schedule_dialog.view.*
import java.util.*

class ScheduleDialogFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        private const val SCHEDULE_DIALOG_DATA_KEY = "scheduleDialogData"
        private const val SHOW_DELETE_KEY = "showDelete"

        private const val DATE_FRAGMENT_TAG = "dateFragment"
        private const val TIME_LIST_FRAGMENT_TAG = "timeListFragment"
        private const val TIME_PICKER_TAG = "timePicker"

        fun newInstance(scheduleDialogData: ScheduleDialogData, showDelete: Boolean) = ScheduleDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(SCHEDULE_DIALOG_DATA_KEY, scheduleDialogData)
                putBoolean(SHOW_DELETE_KEY, showDelete)
            }
        }
    }

    private lateinit var mScheduleType: MySpinner

    private lateinit var mScheduleDialogDateLayout: TextInputLayout
    private lateinit var mScheduleDialogDate: TextView

    private lateinit var mScheduleDialogDayLayout: View
    private val mScheduleDialogDays = mutableMapOf<DayOfWeek, CheckBox>()

    private lateinit var mScheduleDialogMonthLayout: RadioGroup

    private lateinit var mScheduleDialogMonthDayRadio: RadioButton
    private lateinit var mScheduleDialogMonthDayNumber: Spinner
    private lateinit var mScheduleDialogMonthDayLabel: TextView

    private lateinit var mScheduleDialogMonthWeekRadio: RadioButton
    private lateinit var mScheduleDialogMonthWeekNumber: Spinner
    private lateinit var mScheduleDialogMonthWeekDay: Spinner

    private lateinit var mScheduleDialogMonthEnd: Spinner

    private lateinit var mScheduleDialogTimeLayout: TextInputLayout
    private lateinit var mScheduleDialogTime: TextView

    private lateinit var mScheduleDialogSave: Button
    private lateinit var mScheduleDialogRemove: Button

    private var customTimeDatas: Map<CustomTimeKey<*>, CreateTaskViewModel.CustomTimeData>? = null
    private var scheduleDialogListener: ScheduleDialogListener? = null

    private lateinit var scheduleDialogData: ScheduleDialogData

    private var broadcastReceiver: BroadcastReceiver? = null

    private val timeDialogListener = object : TimeDialogFragment.TimeDialogListener {

        override fun onCustomTimeSelected(customTimeKey: CustomTimeKey<*>) {
            check(customTimeDatas != null)

            scheduleDialogData.timePairPersist.customTimeKey = customTimeKey

            updateFields()
        }

        override fun onOtherSelected() {
            check(customTimeDatas != null)

            TimePickerDialogFragment.newInstance(scheduleDialogData.timePairPersist.hourMinute).let {
                it.listener = timePickerDialogFragmentListener
                it.show(childFragmentManager, TIME_PICKER_TAG)
            }
        }

        override fun onAddSelected() {
            startActivityForResult(ShowCustomTimeActivity.getCreateIntent(activity!!), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        }
    }

    private val timePickerDialogFragmentListener = { hourMinute: HourMinute ->
        check(customTimeDatas != null)

        scheduleDialogData.timePairPersist.hourMinute = hourMinute
        updateFields()
    }

    private val datePickerDialogFragmentListener = { date: Date ->
        check(scheduleDialogData.scheduleType == ScheduleType.SINGLE)

        scheduleDialogData.date = date
        updateFields()
    }

    //cached data doesn't contain new custom time
    private val isValid: Boolean
        get() {
            if (customTimeDatas == null)
                return false

            if (scheduleDialogData.scheduleType != ScheduleType.SINGLE)
                return true

            val hourMinute = if (scheduleDialogData.timePairPersist.customTimeKey != null) {
                if (!customTimeDatas!!.containsKey(scheduleDialogData.timePairPersist.customTimeKey!!))
                    return false

                customTimeDatas!!.getValue(scheduleDialogData.timePairPersist.customTimeKey!!).hourMinutes[scheduleDialogData.date.dayOfWeek]!!
            } else {
                scheduleDialogData.timePairPersist.hourMinute
            }

            return TimeStamp(scheduleDialogData.date, hourMinute) > TimeStamp.now
        }

    private fun AutoCompleteTextView.makeSpinner(items: List<*>) {
        setAdapter(ArrayAdapter(requireContext(), R.layout.cat_exposed_dropdown_popup_item, items))
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        check(arguments!!.containsKey(SHOW_DELETE_KEY))

        val view = requireActivity().layoutInflater.inflate(R.layout.fragment_schedule_dialog, null).apply {
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
            mScheduleDialogTimeLayout = scheduleDialogTimeLayout
            mScheduleDialogTime = scheduleDialogTime

            mScheduleDialogSave = scheduleDialogSave
            mScheduleDialogRemove = scheduleDialogRemove

            mScheduleDialogSave.setOnClickListener {
                check(customTimeDatas != null)
                check(scheduleDialogListener != null)
                check(isValid)

                scheduleDialogListener!!.onScheduleDialogResult(scheduleDialogData)

                dismiss()
            }

            if (arguments!!.getBoolean(SHOW_DELETE_KEY)) {
                mScheduleDialogRemove.apply {
                    visibility = View.VISIBLE

                    setOnClickListener {
                        scheduleDialogListener!!.onScheduleDialogDelete()

                        dismiss()
                    }
                }
            }

            scheduleDialogCancel.setOnClickListener {
                dialog!!.cancel()
            }
        }

        return BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme).apply {
            setCancelable(true)
            setContentView(view)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        scheduleDialogData = (savedInstanceState
                ?: arguments!!).run { getParcelable(SCHEDULE_DIALOG_DATA_KEY)!! }

        mScheduleType.run {
            text.makeSpinner(resources.getStringArray(R.array.schedule_types).toList())

            text.setSelection(when (scheduleDialogData.scheduleType) {
                ScheduleType.SINGLE -> 0
                ScheduleType.DAILY -> throw UnsupportedOperationException()
                ScheduleType.WEEKLY -> 1
                ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> 2
            })

            text.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {
                    scheduleDialogData.scheduleType = when (i) {
                        0 -> ScheduleType.SINGLE
                        1 -> ScheduleType.WEEKLY
                        2 -> if (scheduleDialogData.monthlyDay) ScheduleType.MONTHLY_DAY else ScheduleType.MONTHLY_WEEK
                        else -> throw UnsupportedOperationException()
                    }

                    if (activity != null && customTimeDatas != null)
                        initialize()
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) = Unit
            }
        }

        mScheduleDialogTime.setOnClickListener {
            check(customTimeDatas != null)

            val list = customTimeDatas!!.values.filter { it.customTimeKey is CustomTimeKey.Private }

            val customTimeDatas = when (scheduleDialogData.scheduleType) {
                ScheduleType.SINGLE -> list.sortedBy { it.hourMinutes[scheduleDialogData.date.dayOfWeek] }.map { customTimeData -> TimeDialogFragment.CustomTimeData(customTimeData.customTimeKey, customTimeData.name + " (" + customTimeData.hourMinutes[scheduleDialogData.date.dayOfWeek] + ")") }
                ScheduleType.DAILY, ScheduleType.WEEKLY, ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> list.sortedBy { it.hourMinutes.values.map { it.hour * 60 + it.minute }.sum() }.map { TimeDialogFragment.CustomTimeData(it.customTimeKey, it.name) }
            }

            TimeDialogFragment.newInstance(ArrayList(customTimeDatas)).let {
                it.timeDialogListener = timeDialogListener
                it.show(childFragmentManager, TIME_LIST_FRAGMENT_TAG)
            }
        }

        (childFragmentManager.findFragmentByTag(TIME_LIST_FRAGMENT_TAG) as? TimeDialogFragment)?.timeDialogListener = timeDialogListener

        (childFragmentManager.findFragmentByTag(TIME_PICKER_TAG) as? TimePickerDialogFragment)?.listener = timePickerDialogFragmentListener

        mScheduleDialogDate.setOnClickListener {
            check(scheduleDialogData.scheduleType == ScheduleType.SINGLE)

            DatePickerDialogFragment.newInstance(scheduleDialogData.date).let {
                it.listener = datePickerDialogFragmentListener
                it.show(childFragmentManager, DATE_FRAGMENT_TAG)
            }
        }

        (childFragmentManager.findFragmentByTag(DATE_FRAGMENT_TAG) as? DatePickerDialogFragment)?.run {
            check(scheduleDialogData.scheduleType == ScheduleType.SINGLE)

            listener = datePickerDialogFragmentListener
        }

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (customTimeDatas != null)
                    updateFields()
            }
        }

        val dayListener = { day: DayOfWeek, isChecked: Boolean ->
            scheduleDialogData.daysOfWeek.run { if (isChecked) add(day) else remove(day) }

            updateFields()
        }

        scheduleDialogData.daysOfWeek.forEach { mScheduleDialogDays[it]!!.isChecked = true }
        mScheduleDialogDays.forEach { (day, view) -> view.setOnCheckedChangeListener { _, isChecked -> dayListener(day, isChecked) } }

        val textPrimary = ContextCompat.getColor(activity!!, R.color.textPrimary)
        val textDisabledSpinner = ContextCompat.getColor(activity!!, R.color.textDisabledSpinner)

        mScheduleDialogMonthDayRadio.run {
            setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked)
                    return@setOnCheckedChangeListener

                scheduleDialogData.scheduleType = ScheduleType.MONTHLY_DAY

                mScheduleDialogMonthWeekRadio.isChecked = false

                scheduleDialogData.monthlyDay = true

                mScheduleDialogMonthDayNumber.isEnabled = true
                mScheduleDialogMonthDayLabel.setTextColor(textPrimary)

                mScheduleDialogMonthWeekNumber.isEnabled = false
                mScheduleDialogMonthWeekDay.isEnabled = false
            }

            isChecked = scheduleDialogData.monthlyDay
        }

        mScheduleDialogMonthDayNumber.apply {
            adapter = ArrayAdapter(requireContext(), R.layout.spinner_no_padding, (1..28).map { Utils.ordinal(it) }).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(scheduleDialogData.monthDayNumber - 1)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    check(position >= 0)
                    check(position < 28)

                    scheduleDialogData.monthDayNumber = position + 1
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }

        mScheduleDialogMonthWeekRadio.run {
            setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                if (!isChecked)
                    return@setOnCheckedChangeListener

                scheduleDialogData.scheduleType = ScheduleType.MONTHLY_WEEK

                mScheduleDialogMonthDayRadio.isChecked = false

                scheduleDialogData.monthlyDay = false

                mScheduleDialogMonthDayNumber.isEnabled = false
                mScheduleDialogMonthDayLabel.setTextColor(textDisabledSpinner)

                mScheduleDialogMonthWeekNumber.isEnabled = true
                mScheduleDialogMonthWeekDay.isEnabled = true
            }

            isChecked = !scheduleDialogData.monthlyDay
        }

        mScheduleDialogMonthWeekNumber.run {
            adapter = ArrayAdapter(requireContext(), R.layout.spinner_no_padding, listOf(1, 2, 3, 4).map { Utils.ordinal(it) }).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(scheduleDialogData.monthWeekNumber - 1)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    check(position >= 0)
                    check(position <= 3)

                    scheduleDialogData.monthWeekNumber = position + 1
                }

                override fun onNothingSelected(parent: AdapterView<*>) = Unit
            }
        }

        mScheduleDialogMonthWeekDay.run {
            val monthWeekDayAdapter = ArrayAdapter(context, R.layout.spinner_no_padding, DayOfWeek.values()).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            adapter = monthWeekDayAdapter
            setSelection(monthWeekDayAdapter.getPosition(scheduleDialogData.monthWeekDay))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val dayOfWeek = monthWeekDayAdapter.getItem(position)!!

                    scheduleDialogData.monthWeekDay = dayOfWeek

                    updateFields()
                }

                override fun onNothingSelected(parent: AdapterView<*>) = Unit
            }
        }

        mScheduleDialogMonthEnd.run {
            adapter = ArrayAdapter.createFromResource(requireContext(), R.array.month, R.layout.spinner_no_padding).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            setSelection(if (scheduleDialogData.beginningOfMonth) 0 else 1)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    check(position == 0 || position == 1)

                    scheduleDialogData.beginningOfMonth = position == 0
                }

                override fun onNothingSelected(parent: AdapterView<*>) = Unit
            }
        }

        if (customTimeDatas != null)
            initialize()
    }

    override fun onResume() {
        super.onResume()

        requireActivity().startTicks(broadcastReceiver!!)

        if (customTimeDatas != null)
            updateFields()
    }

    fun initialize(customTimeDatas: Map<CustomTimeKey<*>, CreateTaskViewModel.CustomTimeData>, scheduleDialogListener: ScheduleDialogListener) {
        this.customTimeDatas = customTimeDatas
        this.scheduleDialogListener = scheduleDialogListener

        if (this::scheduleDialogData.isInitialized)
            initialize()
    }

    private fun initialize() {
        check(customTimeDatas != null)
        check(scheduleDialogListener != null)
        check(activity != null)

        when (scheduleDialogData.scheduleType) {
            ScheduleType.SINGLE -> {
                mScheduleDialogDateLayout.visibility = View.VISIBLE
                mScheduleDialogDayLayout.visibility = View.GONE
                mScheduleDialogMonthLayout.visibility = View.GONE
                mScheduleDialogTimeLayout.visibility = View.VISIBLE
                mScheduleDialogTimeLayout.isErrorEnabled = true
            }
            ScheduleType.DAILY -> {
                mScheduleDialogDateLayout.visibility = View.GONE
                mScheduleDialogDayLayout.visibility = View.GONE
                mScheduleDialogMonthLayout.visibility = View.GONE
                mScheduleDialogTimeLayout.visibility = View.VISIBLE
                mScheduleDialogTimeLayout.isErrorEnabled = false
            }
            ScheduleType.WEEKLY -> {
                mScheduleDialogDateLayout.visibility = View.GONE
                mScheduleDialogDayLayout.visibility = View.VISIBLE
                mScheduleDialogMonthLayout.visibility = View.GONE
                mScheduleDialogTimeLayout.visibility = View.VISIBLE
                mScheduleDialogTimeLayout.isErrorEnabled = false
            }
            ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> {
                mScheduleDialogDateLayout.visibility = View.GONE
                mScheduleDialogDayLayout.visibility = View.GONE
                mScheduleDialogMonthLayout.visibility = View.VISIBLE
                mScheduleDialogTimeLayout.visibility = View.VISIBLE
                mScheduleDialogTimeLayout.isErrorEnabled = false
            }
        }

        updateFields()
    }

    override fun onPause() {
        super.onPause()

        activity!!.unregisterReceiver(broadcastReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(SCHEDULE_DIALOG_DATA_KEY, scheduleDialogData)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        check(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)

        if (resultCode == Activity.RESULT_OK)
            scheduleDialogData.timePairPersist.customTimeKey = data!!.getSerializableExtra(ShowCustomTimeActivity.CUSTOM_TIME_ID_KEY) as CustomTimeKey.Private
    }

    @SuppressLint("SetTextI18n")
    private fun updateFields() {
        check(customTimeDatas != null)

        when (scheduleDialogData.scheduleType) {
            ScheduleType.SINGLE -> {
                mScheduleDialogDate.text = scheduleDialogData.date.getDisplayText()

                mScheduleDialogTime.text = if (scheduleDialogData.timePairPersist.customTimeKey != null) {
                    val customTimeData = customTimeDatas!!.getValue(scheduleDialogData.timePairPersist.customTimeKey!!)

                    customTimeData.name + " (" + customTimeData.hourMinutes[scheduleDialogData.date.dayOfWeek] + ")"
                } else {
                    scheduleDialogData.timePairPersist.hourMinute.toString()
                }
            }
            ScheduleType.DAILY -> mScheduleDialogTime.text = if (scheduleDialogData.timePairPersist.customTimeKey != null) {
                customTimeDatas!!.getValue(scheduleDialogData.timePairPersist.customTimeKey!!).name
            } else {
                scheduleDialogData.timePairPersist.hourMinute.toString()
            }
            ScheduleType.WEEKLY -> mScheduleDialogTime.text = if (scheduleDialogData.timePairPersist.customTimeKey != null) {
                val customTimeData = customTimeDatas!!.getValue(scheduleDialogData.timePairPersist.customTimeKey!!)

                customTimeData.name
            } else {
                scheduleDialogData.timePairPersist.hourMinute.toString()
            }
            ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> {
                mScheduleDialogTime.text = if (scheduleDialogData.timePairPersist.customTimeKey != null) {
                    customTimeDatas!!.getValue(scheduleDialogData.timePairPersist.customTimeKey!!).name
                } else {
                    scheduleDialogData.timePairPersist.hourMinute.toString()
                }
            }
        }

        if (isValid) {
            mScheduleDialogSave.isEnabled = true

            mScheduleDialogDateLayout.error = null
            mScheduleDialogTimeLayout.error = null
        } else {
            check(scheduleDialogData.scheduleType == ScheduleType.SINGLE)
            mScheduleDialogSave.isEnabled = false

            if (scheduleDialogData.date >= Date.today()) {
                mScheduleDialogDateLayout.error = null
                mScheduleDialogTimeLayout.error = getString(R.string.error_time)
            } else {
                mScheduleDialogDateLayout.error = getString(R.string.error_date)
                mScheduleDialogTimeLayout.error = null
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        check(scheduleDialogListener != null)

        scheduleDialogListener!!.onScheduleDialogCancel()
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
