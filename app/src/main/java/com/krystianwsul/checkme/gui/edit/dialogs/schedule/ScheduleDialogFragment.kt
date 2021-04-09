package com.krystianwsul.checkme.gui.edit.dialogs.schedule

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ca.antonious.materialdaypicker.MaterialDayPicker
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentScheduleDialogBinding
import com.krystianwsul.checkme.domainmodel.ScheduleText
import com.krystianwsul.checkme.gui.base.NoCollapseBottomSheetDialogFragment
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity
import com.krystianwsul.checkme.gui.dialogs.*
import com.krystianwsul.checkme.gui.edit.EditViewModel
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.gui.widgets.MyTextInputLayout
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.getDateInMonth
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
        private const val TAG_TIME_PICKER = "timePicker"

        private val daysOfWeekMap = mapOf(
                DayOfWeek.SUNDAY to MaterialDayPicker.Weekday.SUNDAY,
                DayOfWeek.MONDAY to MaterialDayPicker.Weekday.MONDAY,
                DayOfWeek.TUESDAY to MaterialDayPicker.Weekday.TUESDAY,
                DayOfWeek.WEDNESDAY to MaterialDayPicker.Weekday.WEDNESDAY,
                DayOfWeek.THURSDAY to MaterialDayPicker.Weekday.THURSDAY,
                DayOfWeek.FRIDAY to MaterialDayPicker.Weekday.FRIDAY,
                DayOfWeek.SATURDAY to MaterialDayPicker.Weekday.SATURDAY
        )

        fun newInstance(parameters: ScheduleDialogParameters) = ScheduleDialogFragment().apply {
            arguments = Bundle().apply {
                parameters.position?.let { putInt(KEY_POSITION, it) }
                putParcelable(SCHEDULE_DIALOG_DATA_KEY, parameters.scheduleDialogData)
                putBoolean(SHOW_DELETE_KEY, parameters.showDelete)
            }
        }
    }

    override val backgroundView get() = binding.scheduleDialogRoot
    override val contentView get() = binding.scheduleDialogContentWrapper

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

            newMaterialTimePicker(
                    requireContext(),
                    childFragmentManager,
                    TAG_TIME_PICKER,
                    scheduleDialogData.timePairPersist.hourMinute,
            ).setListener(timePickerDialogFragmentListener)
        }

        override fun onAddSelected() {
            startActivityForResult(
                    ShowCustomTimeActivity.getCreateIntent(activity!!),
                    ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE
            )
        }
    }

    private val timePickerDialogFragmentListener = { hourMinute: HourMinute ->
        checkNotNull(customTimeDatas)

        scheduleDialogData.timePairPersist.setHourMinute(hourMinute)
        updateFields()
    }

    private val datePickerDialogFragmentListener = { date: Date -> delegate.onDateChanged(date) }

    private lateinit var delegate: Delegate

    //cached data doesn't contain new custom time
    private fun isValid(): Boolean {
        scheduleDialogData.timePairPersist
                .customTimeKey
                ?.let {
                    if (customTimeDatas?.containsKey(it) == false)
                        return false
                }

        val errorData = delegate.isValid()

        errorData.run {
            binding.scheduleDialogDateLayout.error = date
            binding.scheduleDialogTimeLayout.error = time
            binding.scheduleDialogFromLayout.error = from
            binding.scheduleDialogUntilLayout.error = until
        }

        return errorData.isValid()
    }

    private fun checkValid() = isValid().also { binding.scheduleDialogSave.isEnabled = it }

    private var position: Int? = null

    val result = PublishRelay.create<ScheduleDialogResult>()!!

    private val dateFieldDatas by lazy {
        listOf(
                DateFieldData(
                        binding.scheduleDialogFrom,
                        binding.scheduleDialogFromLayout,
                        scheduleDialogData::from,
                        TAG_FROM_FRAGMENT
                ),
                DateFieldData(
                        binding.scheduleDialogUntil,
                        binding.scheduleDialogUntilLayout,
                        scheduleDialogData::until,
                        TAG_UNTIL_FRAGMENT,
                        { listOfNotNull(scheduleDialogData.from, Date.today()).maxOrNull()!! }
                )
        )
    }

    private val bindingProperty = ResettableProperty<FragmentScheduleDialogBinding>()
    private var binding by bindingProperty

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        position = requireArguments().getInt(KEY_POSITION, -1).takeUnless { it == -1 }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = FragmentScheduleDialogBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.scheduleDialogSave.setOnClickListener {
            check(customTimeDatas != null)

            if (checkValid()) {
                result.accept(ScheduleDialogResult.Change(position, scheduleDialogData))

                dismiss()
            }
        }

        if (requireArguments().getBoolean(SHOW_DELETE_KEY)) {
            checkNotNull(position)

            binding.scheduleDialogRemove.apply {
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

        binding.scheduleDialogCancel.setOnClickListener { dialog!!.cancel() }

        binding.scheduleDialogMonthDayNumber.setDense()
        binding.scheduleDialogMonthWeekNumber.setDense()
        binding.scheduleDialogMonthWeekDay.setDense()
        binding.scheduleDialogMonthEnd.setDense()

        hideKeyboardOnClickOutside(binding.scheduleDialogRoot)

        binding.scheduleDialogSelectAllDays.setOnClickListener {
            binding.scheduleDialogDayPicker.selectAllDays()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        scheduleDialogData = (savedInstanceState ?: requireArguments()).run {
            getParcelable(SCHEDULE_DIALOG_DATA_KEY)!!
        }
        updateDelegate()

        binding.scheduleType.run {
            setItems(resources.getStringArray(R.array.schedule_types).toList())

            setSelection(delegate.selection)

            addListener {
                scheduleDialogData.scheduleType = when (it) {
                    0 -> ScheduleType.SINGLE
                    1 -> ScheduleType.WEEKLY
                    2 -> if (scheduleDialogData.monthlyDay) ScheduleType.MONTHLY_DAY else ScheduleType.MONTHLY_WEEK
                    3 -> ScheduleType.YEARLY
                    else -> throw UnsupportedOperationException()
                }
                updateDelegate()

                if (activity != null && customTimeDatas != null) updateScheduleTypeFields(true)
            }
        }

        binding.scheduleDialogTimeLayout.setDropdown {
            checkNotNull(customTimeDatas)

            val list = customTimeDatas!!.values.filter { it.customTimeKey is CustomTimeKey.Project.Private }

            val customTimeDatas = delegate.getCustomTimeDatas(list)

            TimeDialogFragment.newInstance(ArrayList(customTimeDatas)).let {
                it.timeDialogListener = timeDialogListener
                it.show(
                        childFragmentManager,
                        TIME_LIST_FRAGMENT_TAG
                )
            }
        }

        tryGetFragment<TimeDialogFragment>(TIME_LIST_FRAGMENT_TAG)?.timeDialogListener = timeDialogListener
        tryGetFragment<MaterialTimePicker>(TAG_TIME_PICKER)?.setListener(timePickerDialogFragmentListener)

        binding.scheduleDialogDateLayout.setDropdown {
            delegate.getDatePicker().let {
                it.addListener(datePickerDialogFragmentListener)
                it.show(childFragmentManager, DATE_FRAGMENT_TAG)
            }
        }

        childFragmentManager.getMaterialDatePicker(DATE_FRAGMENT_TAG)?.run {
            addListener(datePickerDialogFragmentListener)
        }

        dateFieldDatas.forEach { data ->
            childFragmentManager.getMaterialDatePicker(data.tag)?.let {
                check(scheduleDialogData.scheduleType != ScheduleType.SINGLE)

                it.addListener(data.listener)
            }
        }

        val weekdaysMap = daysOfWeekMap.entries
                .map { it.value to it.key }
                .toMap()

        binding.scheduleDialogDayPicker.daySelectionChangedListener = object : MaterialDayPicker.DaySelectionChangedListener {

            override fun onDaySelectionChanged(selectedDays: List<MaterialDayPicker.Weekday>) = delegate.onDaysOfWeekChanged(selectedDays.map(weekdaysMap::getValue).toSet())
        }

        val textPrimary = ContextCompat.getColor(requireContext(), R.color.textPrimary)
        val textDisabledSpinner = ContextCompat.getColor(requireContext(), R.color.textDisabledSpinner)

        binding.scheduleDialogMonthDayRadio.run {
            setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked) return@setOnCheckedChangeListener

                if (delegate.isMonthly) {
                    scheduleDialogData.scheduleType = ScheduleType.MONTHLY_DAY
                    updateDelegate()
                }

                binding.scheduleDialogMonthWeekRadio.isChecked = false

                scheduleDialogData.monthlyDay = true

                binding.scheduleDialogMonthDayNumber.isEnabled = true
                binding.scheduleDialogMonthDayLabel.setTextColor(textPrimary)

                binding.scheduleDialogMonthWeekNumber.isEnabled = false
                binding.scheduleDialogMonthWeekDay.isEnabled = false
            }

            isChecked = scheduleDialogData.monthlyDay
        }

        binding.scheduleDialogMonthDayNumber.apply {
            setItems((1..28).map { Utils.ordinal(it) })

            addListener { delegate.onMonthDayNumberChanged(it + 1) }
        }

        binding.scheduleDialogMonthWeekRadio.run {
            setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                if (!isChecked) return@setOnCheckedChangeListener

                if (delegate.isMonthly) {
                    scheduleDialogData.scheduleType = ScheduleType.MONTHLY_WEEK
                    updateDelegate()
                }

                binding.scheduleDialogMonthDayRadio.isChecked = false

                scheduleDialogData.monthlyDay = false

                binding.scheduleDialogMonthDayNumber.isEnabled = false
                binding.scheduleDialogMonthDayLabel.setTextColor(textDisabledSpinner)

                binding.scheduleDialogMonthWeekNumber.isEnabled = true
                binding.scheduleDialogMonthWeekDay.isEnabled = true
            }

            isChecked = !scheduleDialogData.monthlyDay
        }

        binding.scheduleDialogMonthWeekNumber.run {
            setItems(listOf(1, 2, 3, 4).map { Utils.ordinal(it) })

            addListener { delegate.onMonthWeekNumberChanged(it + 1) }
        }

        binding.scheduleDialogMonthWeekDay.run {
            setItems(DayOfWeek.values().toList())

            addListener { delegate.onMonthWeekDayChanged(DayOfWeek.values()[it]) }
        }

        binding.scheduleDialogMonthEnd.run {
            setItems(resources.getStringArray(R.array.month).toList())

            addListener {
                check(it in (0..1))

                delegate.onBeginningOfMonthChanged(it == 0)
            }
        }

        if (customTimeDatas != null) initialize()

        binding.scheduleDialogEveryXWeeks.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable) {
                var value = s.toString().toIntOrNull() ?: 0

                if (value < 1) {
                    value = 1

                    if (s.isNotBlank()) {
                        binding.scheduleDialogEveryXWeeks.apply {
                            setText("1")
                            setSelection(1)
                        }
                    }
                }

                scheduleDialogData.interval = value

                if (s.isNotBlank()) checkValid()
            }
        })
    }

    private fun updateDelegate() {
        delegate = when (scheduleDialogData.scheduleType) {
            ScheduleType.SINGLE -> SingleDelegate()
            ScheduleType.WEEKLY -> WeeklyDelegate()
            ScheduleType.MONTHLY_DAY -> MonthlyDayDelegate()
            ScheduleType.MONTHLY_WEEK -> MonthlyWeekDelegate()
            ScheduleType.YEARLY -> YearlyDelegate()
        }
    }

    override fun onStart() {
        super.onStart()

        requireActivity().startTicks(broadcastReceiver)

        if (customTimeDatas != null) updateFields()
    }

    fun initialize(customTimeDatas: Map<CustomTimeKey<*>, EditViewModel.CustomTimeData>) {
        this.customTimeDatas = customTimeDatas

        if (this::scheduleDialogData.isInitialized)
            initialize()
    }

    private fun initialize() = binding.scheduleDialogRoot.addOneShotGlobalLayoutListener { // needed so animations run correctly
        updateScheduleTypeFields()
    }

    private fun updateScheduleTypeFields(animate: Boolean = false) {
        check(customTimeDatas != null)
        check(activity != null)

        if (animate) TransitionManager.beginDelayedTransition(binding.scheduleDialogContentLayout)

        run {
            delegate.visibilities.run {
                binding.scheduleDialogDateLayout.isVisible = date
                binding.scheduleDialogDayLayout.isVisible = day
                binding.scheduleDialogMonthLayout.isVisible = month
                binding.scheduleDialogFromLayout.isVisible = from
                binding.scheduleDialogUntilLayout.isVisible = until
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
            scheduleDialogData.timePairPersist.customTimeKey =
                    data!!.getParcelableExtra<CustomTimeKey.Project.Private>(ShowCustomTimeActivity.CUSTOM_TIME_KEY)!!
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

        delegate.updateFields(customTimeData, hourMinuteString)

        binding.scheduleDialogDayPicker.setSelectedDays(scheduleDialogData.daysOfWeek.map(daysOfWeekMap::getValue))

        binding.scheduleDialogMonthDayNumber.setSelection(scheduleDialogData.monthDayNumber - 1, true)

        binding.scheduleDialogMonthWeekNumber.setSelection(scheduleDialogData.monthWeekNumber - 1, true)

        binding.scheduleDialogMonthWeekDay.setSelection(scheduleDialogData.monthWeekDay.ordinal, true)

        binding.scheduleDialogMonthEnd.setSelection(if (scheduleDialogData.beginningOfMonth) 0 else 1, true)

        checkValid()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)

        result.accept(ScheduleDialogResult.Cancel)
    }

    private fun diffScheduleDialogData(applyChange: () -> Unit, updateOtherScheduleDialogDataFields: () -> Unit) {
        val oldScheduleDialogData = scheduleDialogData.copy()
        applyChange()
        val newScheduleDialogData = scheduleDialogData.copy()

        if (oldScheduleDialogData != newScheduleDialogData) {
            updateOtherScheduleDialogDataFields()

            updateFields()
        }
    }

    private fun recalculateMonthlyWeekFields() {
        val date = getDateInMonth(
                scheduleDialogData.date.year,
                scheduleDialogData.date.month,
                scheduleDialogData.monthWeekNumber,
                scheduleDialogData.monthWeekDay,
                scheduleDialogData.beginningOfMonth
        )

        val dayNumber = if (scheduleDialogData.beginningOfMonth)
            date.day
        else
            EditViewModel.ScheduleDataWrapper.dayFromEndOfMonth(date)

        scheduleDialogData.date = date
        scheduleDialogData.daysOfWeek = setOf(date.dayOfWeek)
        scheduleDialogData.monthDayNumber = dayNumber
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    private inner class DateFieldData(
            val field: AutoCompleteTextView,
            val layout: MyTextInputLayout,
            val property: KMutableProperty0<Date?>,
            val tag: String,
            val min: (() -> Date)? = null,
    ) {

        val listener = { date: Date ->
            check(scheduleDialogData.scheduleType != ScheduleType.SINGLE)

            property.set(date)
            updateFields()
        }
    }

    private abstract inner class Delegate : DateListener {

        abstract val selection: Int

        open val isMonthly = false

        abstract val visibilities: Visibilities

        abstract fun isValid(): ErrorData

        abstract fun getCustomTimeDatas(list: List<EditViewModel.CustomTimeData>): List<TimeDialogFragment.CustomTimeData>

        open fun getDatePicker(): MaterialDatePicker<Long> = throw IllegalStateException()

        abstract fun updateFields(customTimeData: EditViewModel.CustomTimeData?, hourMinuteString: String)

        open fun onDaysOfWeekChanged(daysOfWeek: Set<DayOfWeek>) = Unit

        open fun onMonthDayNumberChanged(monthDayNumber: Int) = Unit

        open fun onMonthWeekNumberChanged(monthWeekNumber: Int) = Unit

        open fun onMonthWeekDayChanged(dayOfWeek: DayOfWeek) = Unit

        open fun onBeginningOfMonthChanged(beginningOfMonth: Boolean) = Unit
    }

    private inner class SingleDelegate : Delegate(), DateListener by DateDelegate({ it }) {

        override val selection = 0

        override val visibilities = Visibilities(date = true)

        override fun isValid(): ErrorData {
            val today = Date.today()

            if (scheduleDialogData.date < today) {
                return ErrorData(getString(R.string.error_date))
            } else {
                val customTimeKey = scheduleDialogData.timePairPersist.customTimeKey

                val hourMinute = if (customTimeKey != null) {
                    customTimeDatas!!.getValue(customTimeKey)
                            .hourMinutes
                            .getValue(scheduleDialogData.date.dayOfWeek)
                } else {
                    scheduleDialogData.timePairPersist.hourMinute
                }

                return if (scheduleDialogData.date == today && hourMinute < HourMinute.now)
                    ErrorData(time = getString(R.string.error_time))
                else
                    ErrorData()
            }
        }

        override fun getCustomTimeDatas(list: List<EditViewModel.CustomTimeData>): List<TimeDialogFragment.CustomTimeData> {
            val dayOfWeek = scheduleDialogData.date.dayOfWeek

            return list.sortedBy { it.hourMinutes[dayOfWeek] }.map {
                TimeDialogFragment.CustomTimeData(
                        it.customTimeKey,
                        it.name + " (" + it.hourMinutes[dayOfWeek] + ")"
                )
            }
        }

        override fun getDatePicker() = newMaterialDatePicker(scheduleDialogData.date)

        override fun updateFields(customTimeData: EditViewModel.CustomTimeData?, hourMinuteString: String) {
            binding.scheduleDialogDate.setText(scheduleDialogData.date.getDisplayText())

            binding.scheduleDialogTime.setText(
                    customTimeData?.let {
                        it.name + " (" + customTimeData.hourMinutes.getValue(scheduleDialogData.date.dayOfWeek) + ")"
                    } ?: hourMinuteString
            )
        }
    }

    private abstract inner class Repeating : Delegate() {

        protected val repeatingVisibilities = Visibilities(from = true, until = true)

        override fun isValid(): ErrorData {
            var fromError: String? = null
            var untilError: String? = null

            scheduleDialogData.from?.let { from ->
                if (from < Date.today()) {
                    fromError = getString(R.string.error_date)
                }
            }

            scheduleDialogData.until?.let { until ->
                if (until < Date.today()) {
                    untilError = getString(R.string.error_date)
                } else if (scheduleDialogData.from?.let { it > until } == true) {
                    untilError = getString(R.string.endDate)
                }
            }

            return ErrorData(from = fromError, until = untilError)
        }

        override fun getCustomTimeDatas(list: List<EditViewModel.CustomTimeData>): List<TimeDialogFragment.CustomTimeData> {
            return list.sortedBy {
                it.hourMinutes.values.map { it.hour * 60 + it.minute }.sum()
            }.map {
                TimeDialogFragment.CustomTimeData(it.customTimeKey, it.name)
            }
        }

        override fun updateFields(customTimeData: EditViewModel.CustomTimeData?, hourMinuteString: String) {
            binding.scheduleDialogDate.setText(scheduleDialogData.date.run {
                ScheduleText.Yearly.getDateText(month, day)
            })

            binding.scheduleDialogTime.setText(customTimeData?.name ?: hourMinuteString)

            binding.scheduleDialogEveryXWeeks.setText(scheduleDialogData.interval.toString())

            dateFieldDatas.forEach { data ->
                val date = data.property.get()

                val dropdown = date == null

                data.field.setText(date?.getDisplayText())

                fun showDialog() = newMaterialDatePicker(
                        data.property.get() ?: Date.today(),
                        data.min?.invoke()
                ).let {
                    it.addListener(data.listener)
                    it.show(childFragmentManager, data.tag)
                }

                fun clearField() {
                    data.property.set(null)
                    updateFields()
                }

                data.layout.apply {
                    if (dropdown) setDropdown(::showDialog) else setClose(::showDialog, ::clearField)
                }
            }
        }
    }

    private inner class WeeklyDelegate : Repeating() {

        override val selection = 1

        override val visibilities = repeatingVisibilities.copy(day = true)

        override fun isValid(): ErrorData {
            val errorData = super.isValid()

            val from = errorData.from ?: getIntervalFromError()

            return errorData.copy(from = from, noDaysChosen = scheduleDialogData.daysOfWeek.isEmpty())
        }

        private fun getIntervalFromError(): String? {
            if (scheduleDialogData.interval == 1)
                return null

            if (scheduleDialogData.from != null)
                return null

            return getString(R.string.dateCannotBeEmpty)
        }

        override fun onDaysOfWeekChanged(daysOfWeek: Set<DayOfWeek>) = diffScheduleDialogData(
                { scheduleDialogData.daysOfWeek = daysOfWeek },
                {
                    daysOfWeek.singleOrNull()?.let {
                        scheduleDialogData.monthWeekDay = it
                        recalculateMonthlyWeekFields()
                    }
                }
        )
    }

    private abstract inner class Monthly : Repeating() {

        override val selection = 2

        override val isMonthly = true

        override val visibilities = repeatingVisibilities.copy(month = true)

        override fun onBeginningOfMonthChanged(beginningOfMonth: Boolean) = diffScheduleDialogData(
                { scheduleDialogData.beginningOfMonth = beginningOfMonth },
                ::recalculateFields
        )

        protected abstract fun recalculateFields()
    }

    private inner class MonthlyDayDelegate : Monthly() {

        override fun onMonthDayNumberChanged(monthDayNumber: Int) = diffScheduleDialogData(
                {
                    check(monthDayNumber in 1..28)

                    scheduleDialogData.monthDayNumber = monthDayNumber
                },
                ::recalculateFields
        )

        override fun recalculateFields() {
            val date = getDateInMonth(
                    scheduleDialogData.date.year,
                    scheduleDialogData.date.month,
                    scheduleDialogData.monthDayNumber,
                    scheduleDialogData.beginningOfMonth
            )

            scheduleDialogData.date = date
            scheduleDialogData.daysOfWeek = setOf(date.dayOfWeek)
            scheduleDialogData.monthWeekNumber =
                    EditViewModel.ScheduleDataWrapper.dayOfMonthToWeekOfMonth(scheduleDialogData.monthDayNumber)
            scheduleDialogData.monthWeekDay = date.dayOfWeek
        }
    }

    private inner class MonthlyWeekDelegate : Monthly() {

        override fun onMonthWeekNumberChanged(monthWeekNumber: Int) = diffScheduleDialogData(
                {
                    check(monthWeekNumber in 1..4)

                    scheduleDialogData.monthWeekNumber = monthWeekNumber
                },
                ::recalculateFields
        )

        override fun onMonthWeekDayChanged(dayOfWeek: DayOfWeek) = diffScheduleDialogData(
                { scheduleDialogData.monthWeekDay = dayOfWeek },
                ::recalculateFields
        )

        override fun recalculateFields() = recalculateMonthlyWeekFields()
    }

    private inner class YearlyDelegate : Repeating(), DateListener by DateDelegate({
        if (it.month == 2 && it.day == 29) Date(it.year, 2, 28) else it
    }) {

        override val selection = 3

        override val visibilities = repeatingVisibilities.copy(date = true)

        override fun getDatePicker() = newYearMaterialDatePicker(scheduleDialogData.date)
    }

    private data class ErrorData(
            val date: String? = null,
            val time: String? = null,
            val from: String? = null,
            val until: String? = null,
            val noDaysChosen: Boolean = false,
    ) {

        fun isValid() = listOf(date, time, from, until).all { it.isNullOrEmpty() } && !noDaysChosen
    }

    private data class Visibilities(
            val date: Boolean = false,
            val day: Boolean = false,
            val month: Boolean = false,
            val from: Boolean = false,
            val until: Boolean = false,
    )

    private interface DateListener {

        fun onDateChanged(date: Date): Unit = throw UnsupportedOperationException()
    }

    private inner class DateDelegate(val fixDate: (Date) -> Date) : DateListener {

        override fun onDateChanged(date: Date) {
            val fixedDate = fixDate(date)

            diffScheduleDialogData(
                    { scheduleDialogData.date = fixedDate },
                    {
                        val (monthDayNumber, beginningOfMonth)
                                = EditViewModel.ScheduleDataWrapper.dateToDayFromBeginningOrEnd(fixedDate)

                        val monthWeekNumber = EditViewModel.ScheduleDataWrapper.dayOfMonthToWeekOfMonth(monthDayNumber)

                        scheduleDialogData.daysOfWeek = setOf(fixedDate.dayOfWeek)
                        scheduleDialogData.monthDayNumber = monthDayNumber
                        scheduleDialogData.monthWeekNumber = monthWeekNumber
                        scheduleDialogData.monthWeekDay = fixedDate.dayOfWeek
                        scheduleDialogData.beginningOfMonth = beginningOfMonth
                    }
            )
        }
    }
}
