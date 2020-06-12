package com.krystianwsul.checkme.gui.edit.dialogs.schedule

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.gui.DatePickerDialogFragment
import com.krystianwsul.checkme.gui.NoCollapseBottomSheetDialogFragment
import com.krystianwsul.checkme.gui.TimeDialogFragment
import com.krystianwsul.checkme.gui.TimePickerDialogFragment
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.viewmodels.EditViewModel
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ScheduleType
import kotlinx.android.synthetic.main.fragment_schedule_dialog.view.*
import java.util.*
import kotlin.reflect.KMutableProperty0

class ScheduleDialogFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        private const val KEY_POSITION = "position"
        private const val SCHEDULE_DIALOG_DATA_KEY = "scheduleDialogData"
        private const val SHOW_DELETE_KEY = "showDelete"

        private const val DATE_FRAGMENT_TAG = "dateFragment"
        private const val TAG_FROM_FRAGMENT = "fromFragment"
        private const val TAG_UNTIL_FRAGMENT = "untilFragment"
        private const val TIME_LIST_FRAGMENT_TAG = "timeListFragment"
        private const val TIME_PICKER_TAG = "timePicker"

        fun newInstance(parameters: ScheduleDialogParameters) = ScheduleDialogFragment()
            .apply {
                arguments = Bundle().apply {
                    parameters.position?.let { putInt(KEY_POSITION, it) }
                    putParcelable(SCHEDULE_DIALOG_DATA_KEY, parameters.scheduleDialogData)
                    putBoolean(SHOW_DELETE_KEY, parameters.showDelete)
                }
            }
    }

    private lateinit var customView: ViewGroup
    private lateinit var scheduleDialogDays: Map<DayOfWeek, CheckBox>

    private var customTimeDatas: Map<CustomTimeKey<*>, EditViewModel.CustomTimeData>? = null

    private lateinit var scheduleDialogData: ScheduleDialogData

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (customTimeDatas != null)
                updateFields()
        }
    }

    private val timeDialogListener = object : TimeDialogFragment.TimeDialogListener {

        override fun onCustomTimeSelected(customTimeKey: CustomTimeKey<*>) {
            check(customTimeDatas != null)

            scheduleDialogData.timePairPersist.customTimeKey = customTimeKey

            updateFields()
        }

        override fun onOtherSelected() {
            checkNotNull(customTimeDatas)

            TimePickerDialogFragment.newInstance(scheduleDialogData.timePairPersist.hourMinute, SerializableUnit).let {
                it.listener = timePickerDialogFragmentListener
                it.show(
                    childFragmentManager,
                    TIME_PICKER_TAG
                )
            }
        }

        override fun onAddSelected() {
            startActivityForResult(ShowCustomTimeActivity.getCreateIntent(activity!!), ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        }
    }

    private val timePickerDialogFragmentListener = { hourMinute: HourMinute, _: SerializableUnit ->
        checkNotNull(customTimeDatas)

        scheduleDialogData.timePairPersist.setHourMinute(hourMinute)
        updateFields()
    }

    private val datePickerDialogFragmentListener = { date: Date ->
        check(scheduleDialogData.scheduleType.hasDate)

        val fixedDate = when (scheduleDialogData.scheduleType) {
            ScheduleType.SINGLE -> date
            ScheduleType.YEARLY -> {
                if (date.month == 2 && date.day == 29)
                    Date(date.year, 2, 28)
                else
                    date
            }
            else -> throw IllegalArgumentException()
        }

        scheduleDialogData.date = fixedDate
        updateFields()
    }

    //cached data doesn't contain new custom time
    private fun isValid(): Boolean {
        if (customTimeDatas == null)
            return false

        if (scheduleDialogData.scheduleType == ScheduleType.SINGLE) {
            val today = Date.today()

            val (dateError, timeError) = if (scheduleDialogData.date < today) {
                Pair(getString(R.string.error_date), null)
            } else {
                val customTimeKey = scheduleDialogData.timePairPersist.customTimeKey

                val hourMinute = if (customTimeKey != null) {
                    customTimeDatas!![customTimeKey]
                            ?.hourMinutes
                            ?.get(scheduleDialogData.date.dayOfWeek)
                            ?: return false
                } else {
                    scheduleDialogData.timePairPersist.hourMinute
                }

                if (scheduleDialogData.date == today && hourMinute < HourMinute.now)
                    Pair(null, getString(R.string.error_time))
                else
                    Pair(null, null)
            }

            customView.scheduleDialogDateLayout.error = dateError
            customView.scheduleDialogTimeLayout.error = timeError

            return dateError.isNullOrEmpty() && timeError.isNullOrEmpty()
        } else {
            customView.scheduleDialogDateLayout.error = null
            customView.scheduleDialogTimeLayout.error = null

            var valid = true

            customView.scheduleDialogFromLayout.error = null
            scheduleDialogData.from?.let { from ->
                if (from < Date.today()) {
                    customView.scheduleDialogFromLayout.error = getString(R.string.error_date)
                    valid = false
                }
            }

            customView.scheduleDialogUntilLayout.error = null
            scheduleDialogData.until?.let { until ->
                if (until < Date.today()) {
                    customView.scheduleDialogUntilLayout.error = getString(R.string.error_date)
                    valid = false
                } else if (scheduleDialogData.from?.let { it > until } == true) {
                    customView.scheduleDialogUntilLayout.error = getString(R.string.endDate)
                    valid = false
                }
            }

            return valid
        }
    }

    private fun checkValid() = isValid().also { customView.scheduleDialogSave.isEnabled = it }

    private var position: Int? = null

    val result = PublishRelay.create<ScheduleDialogResult>()

    private val dateFieldDatas by lazy {
        listOf(
                DateFieldData(
                        customView.scheduleDialogFrom,
                        customView.scheduleDialogFromLayout,
                        scheduleDialogData::from,
                    TAG_FROM_FRAGMENT
                ),
                DateFieldData(
                    customView.scheduleDialogUntil,
                    customView.scheduleDialogUntilLayout,
                    scheduleDialogData::until,
                    TAG_UNTIL_FRAGMENT,
                    { listOfNotNull(scheduleDialogData.from, Date.today()).max()!! }
                )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        position = requireArguments().getInt(KEY_POSITION, -1).takeUnless { it == -1 }
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        check(requireArguments().containsKey(SHOW_DELETE_KEY))

        customView = requireActivity().layoutInflater.inflate(R.layout.fragment_schedule_dialog, null).apply {
            scheduleDialogDays = mapOf(
                    DayOfWeek.SUNDAY to scheduleDialogSunday,
                    DayOfWeek.MONDAY to scheduleDialogMonday,
                    DayOfWeek.TUESDAY to scheduleDialogTuesday,
                    DayOfWeek.WEDNESDAY to scheduleDialogWednesday,
                    DayOfWeek.THURSDAY to scheduleDialogThursday,
                    DayOfWeek.FRIDAY to scheduleDialogFriday,
                    DayOfWeek.SATURDAY to scheduleDialogSaturday
            )

            scheduleDialogDays.forEach { (day, view) -> view.text = day.toString() }

            scheduleDialogSave.setOnClickListener {
                check(customTimeDatas != null)
                check(checkValid())

                result.accept(
                    ScheduleDialogResult.Change(
                        position,
                        scheduleDialogData
                    )
                )

                dismiss()
            }

            if (requireArguments().getBoolean(SHOW_DELETE_KEY)) {
                checkNotNull(position)

                scheduleDialogRemove.apply {
                    visibility = View.VISIBLE

                    setOnClickListener {
                        result.accept(
                            ScheduleDialogResult.Delete(
                                position!!
                            )
                        )

                        dismiss()
                    }
                }
            }

            scheduleDialogCancel.setOnClickListener { dialog!!.cancel() }
        } as ViewGroup

        return TransparentNavigationDialog().apply {
            setCancelable(true)
            setContentView(customView)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        scheduleDialogData = (savedInstanceState ?: requireArguments()).run {
            getParcelable(SCHEDULE_DIALOG_DATA_KEY)!!
        }

        customView.scheduleType.run {
            setItems(resources.getStringArray(R.array.schedule_types).toList())

            setSelection(when (scheduleDialogData.scheduleType) {
                ScheduleType.SINGLE -> 0
                ScheduleType.WEEKLY -> 1
                ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> 2
                ScheduleType.YEARLY -> 3
            })

            addOneShotGlobalLayoutListener {
                addListener {
                    scheduleDialogData.scheduleType = when (it) {
                        0 -> ScheduleType.SINGLE
                        1 -> ScheduleType.WEEKLY
                        2 -> if (scheduleDialogData.monthlyDay) ScheduleType.MONTHLY_DAY else ScheduleType.MONTHLY_WEEK
                        3 -> ScheduleType.YEARLY
                        else -> throw UnsupportedOperationException()
                    }

                    if (activity != null && customTimeDatas != null)
                        updateScheduleTypeFields(true)
                }
            }
        }

        customView.scheduleDialogTime.setFixedOnClickListener {
            checkNotNull(customTimeDatas)

            val list = customTimeDatas!!.values.filter { it.customTimeKey is CustomTimeKey.Private }

            val customTimeDatas = if (scheduleDialogData.scheduleType == ScheduleType.SINGLE) {
                val dayOfWeek = scheduleDialogData.date.dayOfWeek
                list.sortedBy { it.hourMinutes[dayOfWeek] }.map { TimeDialogFragment.CustomTimeData(it.customTimeKey, it.name + " (" + it.hourMinutes[dayOfWeek] + ")") }
            } else {
                list.sortedBy { it.hourMinutes.values.map { it.hour * 60 + it.minute }.sum() }.map { TimeDialogFragment.CustomTimeData(it.customTimeKey, it.name) }
            }

            TimeDialogFragment.newInstance(ArrayList(customTimeDatas)).let {
                it.timeDialogListener = timeDialogListener
                it.show(
                    childFragmentManager,
                    TIME_LIST_FRAGMENT_TAG
                )
            }
        }

        childFragmentManager.apply {
            (findFragmentByTag(TIME_LIST_FRAGMENT_TAG) as? TimeDialogFragment)?.timeDialogListener = timeDialogListener

            @Suppress("UNCHECKED_CAST")
            (findFragmentByTag(TIME_PICKER_TAG) as? TimePickerDialogFragment<SerializableUnit>)?.listener = timePickerDialogFragmentListener
        }

        customView.scheduleDialogDate.setFixedOnClickListener {
            check(scheduleDialogData.scheduleType.hasDate)

            when (scheduleDialogData.scheduleType) {
                ScheduleType.SINGLE -> DatePickerDialogFragment.newInstance(scheduleDialogData.date)
                ScheduleType.YEARLY -> DatePickerDialogFragment.newYearInstance(scheduleDialogData.date)
                else -> throw IllegalArgumentException()
            }.let {
                it.listener = datePickerDialogFragmentListener
                it.show(
                    childFragmentManager,
                    DATE_FRAGMENT_TAG
                )
            }
        }

        (childFragmentManager.findFragmentByTag(DATE_FRAGMENT_TAG) as? DatePickerDialogFragment)?.run {
            check(scheduleDialogData.scheduleType.hasDate)

            listener = datePickerDialogFragmentListener
        }

        dateFieldDatas.forEach { data ->
            (childFragmentManager.findFragmentByTag(data.tag) as? DatePickerDialogFragment)?.let {
                check(scheduleDialogData.scheduleType != ScheduleType.SINGLE)

                it.listener = data.listener
            }
        }

        customView.scheduleDialogAllDays.apply {
            isChecked = scheduleDialogData.allDays

            setOnCheckedChangeListener { _, isChecked ->
                scheduleDialogData.allDays = isChecked

                updateDays(true)
            }
        }

        scheduleDialogData.daysOfWeek.forEach { scheduleDialogDays.getValue(it).isChecked = true }
        scheduleDialogDays.forEach { (day, view) ->
            view.setOnCheckedChangeListener { _, isChecked ->
                scheduleDialogData.daysOfWeek.run { if (isChecked) add(day) else remove(day) }

                updateFields()
            }
        }

        val textPrimary = ContextCompat.getColor(requireContext(), R.color.textPrimary)
        val textDisabledSpinner = ContextCompat.getColor(requireContext(), R.color.textDisabledSpinner)

        customView.scheduleDialogMonthDayRadio.run {
            setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked)
                    return@setOnCheckedChangeListener

                if (scheduleDialogData.scheduleType.isMonthly)
                    scheduleDialogData.scheduleType = ScheduleType.MONTHLY_DAY

                customView.scheduleDialogMonthWeekRadio.isChecked = false

                scheduleDialogData.monthlyDay = true

                customView.scheduleDialogMonthDayNumber.isEnabled = true
                customView.scheduleDialogMonthDayLabel.setTextColor(textPrimary)

                customView.scheduleDialogMonthWeekNumber.isEnabled = false
                customView.scheduleDialogMonthWeekDay.isEnabled = false
            }

            isChecked = scheduleDialogData.monthlyDay
        }

        customView.scheduleDialogMonthDayNumber.apply {
            adapter = ArrayAdapter(requireContext(), R.layout.spinner_no_padding, (1..28).map { Utils.ordinal(it) }).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(scheduleDialogData.monthDayNumber - 1)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    check(position in (0 until 28))

                    scheduleDialogData.monthDayNumber = position + 1
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }

        customView.scheduleDialogMonthWeekRadio.run {
            setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                if (!isChecked)
                    return@setOnCheckedChangeListener

                if (scheduleDialogData.scheduleType.isMonthly)
                    scheduleDialogData.scheduleType = ScheduleType.MONTHLY_WEEK

                customView.scheduleDialogMonthDayRadio.isChecked = false

                scheduleDialogData.monthlyDay = false

                customView.scheduleDialogMonthDayNumber.isEnabled = false
                customView.scheduleDialogMonthDayLabel.setTextColor(textDisabledSpinner)

                customView.scheduleDialogMonthWeekNumber.isEnabled = true
                customView.scheduleDialogMonthWeekDay.isEnabled = true
            }

            isChecked = !scheduleDialogData.monthlyDay
        }

        customView.scheduleDialogMonthWeekNumber.run {
            adapter = ArrayAdapter(requireContext(), R.layout.spinner_no_padding, listOf(1, 2, 3, 4).map { Utils.ordinal(it) }).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            setSelection(scheduleDialogData.monthWeekNumber - 1)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    check(position in (0..3))

                    scheduleDialogData.monthWeekNumber = position + 1
                }

                override fun onNothingSelected(parent: AdapterView<*>) = Unit
            }
        }

        customView.scheduleDialogMonthWeekDay.run {
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

        customView.scheduleDialogMonthEnd.run {
            adapter = ArrayAdapter.createFromResource(requireContext(), R.array.month, R.layout.spinner_no_padding).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            setSelection(if (scheduleDialogData.beginningOfMonth) 0 else 1)

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    check(position in (0..1))

                    scheduleDialogData.beginningOfMonth = position == 0
                }

                override fun onNothingSelected(parent: AdapterView<*>) = Unit
            }
        }

        if (customTimeDatas != null)
            initialize()
    }

    override fun onStart() {
        super.onStart()

        requireActivity().startTicks(broadcastReceiver)

        if (customTimeDatas != null)
            updateFields()
    }

    fun initialize(customTimeDatas: Map<CustomTimeKey<*>, EditViewModel.CustomTimeData>) {
        this.customTimeDatas = customTimeDatas

        if (this::scheduleDialogData.isInitialized)
            initialize()
    }

    private fun initialize() {
        customView.addOneShotGlobalLayoutListener { // needed so animations run correctly
            updateScheduleTypeFields()
            updateDays()
        }
    }

    private fun updateScheduleTypeFields(animate: Boolean = false) {
        check(customTimeDatas != null)
        check(activity != null)

        if (animate)
            TransitionManager.beginDelayedTransition(customView)

        when (scheduleDialogData.scheduleType) {
            ScheduleType.SINGLE -> {
                customView.scheduleDialogDateLayout.visibility = View.VISIBLE
                customView.scheduleDialogDayLayout.visibility = View.GONE
                customView.scheduleDialogMonthLayout.visibility = View.GONE
                customView.scheduleDialogFromLayout.visibility = View.GONE
                customView.scheduleDialogUntilLayout.visibility = View.GONE
            }
            ScheduleType.WEEKLY -> {
                customView.scheduleDialogDateLayout.visibility = View.GONE
                customView.scheduleDialogDayLayout.visibility = View.VISIBLE
                customView.scheduleDialogMonthLayout.visibility = View.GONE
                customView.scheduleDialogFromLayout.visibility = View.VISIBLE
                customView.scheduleDialogUntilLayout.visibility = View.VISIBLE
            }
            ScheduleType.MONTHLY_DAY, ScheduleType.MONTHLY_WEEK -> {
                customView.scheduleDialogDateLayout.visibility = View.GONE
                customView.scheduleDialogDayLayout.visibility = View.GONE
                customView.scheduleDialogMonthLayout.visibility = View.VISIBLE
                customView.scheduleDialogFromLayout.visibility = View.VISIBLE
                customView.scheduleDialogUntilLayout.visibility = View.VISIBLE
            }
            ScheduleType.YEARLY -> {
                customView.scheduleDialogDateLayout.visibility = View.VISIBLE
                customView.scheduleDialogDayLayout.visibility = View.GONE
                customView.scheduleDialogMonthLayout.visibility = View.GONE
                customView.scheduleDialogFromLayout.visibility = View.VISIBLE
                customView.scheduleDialogUntilLayout.visibility = View.VISIBLE
            }
        }

        updateFields()
    }

    override fun onStop() {
        requireActivity().unregisterReceiver(broadcastReceiver)

        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(SCHEDULE_DIALOG_DATA_KEY, scheduleDialogData)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        check(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)

        if (resultCode == Activity.RESULT_OK)
            scheduleDialogData.timePairPersist.customTimeKey = data!!.getParcelableExtra<CustomTimeKey.Private>(ShowCustomTimeActivity.CUSTOM_TIME_KEY)!!
    }

    private fun updateDays(animate: Boolean = false) {
        if (animate)
            TransitionManager.beginDelayedTransition(customView)

        scheduleDialogDays.values.forEach { it.isVisible = !scheduleDialogData.allDays }
    }

    @SuppressLint("SetTextI18n")
    private fun updateFields() {
        checkNotNull(customTimeDatas)

        val customTimeData = scheduleDialogData.timePairPersist
                .customTimeKey
                ?.let { customTimeDatas!!.getValue(it) }

        val hourMinuteString by lazy {
            scheduleDialogData.timePairPersist
                    .hourMinute
                    .toString()
        }

        if (scheduleDialogData.scheduleType == ScheduleType.SINGLE) {
            customView.scheduleDialogDate.setText(scheduleDialogData.date.getDisplayText())

            customView.scheduleDialogTime.setText(customTimeData?.let {
                it.name + " (" + customTimeData.hourMinutes.getValue(scheduleDialogData.date.dayOfWeek) + ")"
            } ?: hourMinuteString)
        } else {
            customView.scheduleDialogDate.setText(scheduleDialogData.date.run { ScheduleText.Yearly.getDateText(month, day) })

            customView.scheduleDialogTime.setText(customTimeData?.name ?: hourMinuteString)

            dateFieldDatas.forEach { data ->
                val date = data.property.get()

                val dropdown = date == null

                data.field.setText(date?.getDisplayText())

                data.layout.endIconMode = if (dropdown) TextInputLayout.END_ICON_DROPDOWN_MENU else TextInputLayout.END_ICON_CLEAR_TEXT

                data.field.apply {
                    setFixedOnClickListener {
                        check(scheduleDialogData.scheduleType != ScheduleType.SINGLE)

                        DatePickerDialogFragment.newInstance(
                                data.property.get() ?: Date.today(),
                                data.min?.invoke()
                        ).let {
                            it.listener = data.listener
                            it.show(childFragmentManager, data.tag)
                        }
                    }
                }

                if (!dropdown) {
                    data.layout.setEndIconOnClickListener {
                        data.property.set(null)
                        updateFields()
                    }
                }
            }
        }

        checkValid()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        result.accept(ScheduleDialogResult.Cancel)
    }

    private inner class DateFieldData(
            val field: AutoCompleteTextView,
            val layout: TextInputLayout,
            val property: KMutableProperty0<Date?>,
            val tag: String,
            val min: (() -> Date)? = null
    ) {

        val listener = { date: Date ->
            check(scheduleDialogData.scheduleType != ScheduleType.SINGLE)

            property.set(date)
            updateFields()
        }
    }
}
