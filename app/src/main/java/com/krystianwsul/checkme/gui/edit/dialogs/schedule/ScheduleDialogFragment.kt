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
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.CompoundButton
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

            TimePickerDialogFragment.newInstance(
                scheduleDialogData.timePairPersist.hourMinute,
                SerializableUnit
            ).let {
                it.listener = timePickerDialogFragmentListener
                it.show(
                    childFragmentManager,
                    TIME_PICKER_TAG
                )
            }
        }

        override fun onAddSelected() {
            startActivityForResult(
                ShowCustomTimeActivity.getCreateIntent(activity!!),
                ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE
            )
        }
    }

    private val timePickerDialogFragmentListener = { hourMinute: HourMinute, _: SerializableUnit ->
        checkNotNull(customTimeDatas)

        scheduleDialogData.timePairPersist.setHourMinute(hourMinute)
        updateFields()
    }

    private val datePickerDialogFragmentListener = { date: Date ->
        check(delegate.hasDate)

        scheduleDialogData.date = delegate.fixDate(date)
        updateFields()
    }

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

        customView.run {
            errorData.run {
                scheduleDialogDateLayout.error = date
                scheduleDialogTimeLayout.error = time
                scheduleDialogFromLayout.error = from
                scheduleDialogUntilLayout.error = until
            }
        }

        return errorData.run {
            listOf(date, time, from, until).all { it.isNullOrEmpty() }
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

        customView = requireActivity().layoutInflater
            .inflate(R.layout.fragment_schedule_dialog, null)
            .apply {
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

                scheduleDialogMonthDayNumber.setDense()
                scheduleDialogMonthWeekNumber.setDense()
                scheduleDialogMonthWeekDay.setDense()
                scheduleDialogMonthEnd.setDense()
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
        updateDelegate()

        customView.scheduleType.run {
            setItems(resources.getStringArray(R.array.schedule_types).toList())

            setSelection(delegate.selection)

            addOneShotGlobalLayoutListener {
                addListener {
                    scheduleDialogData.scheduleType = when (it) {
                        0 -> ScheduleType.SINGLE
                        1 -> ScheduleType.WEEKLY
                        2 -> if (scheduleDialogData.monthlyDay) ScheduleType.MONTHLY_DAY else ScheduleType.MONTHLY_WEEK
                        3 -> ScheduleType.YEARLY
                        else -> throw UnsupportedOperationException()
                    }
                    updateDelegate()

                    if (activity != null && customTimeDatas != null)
                        updateScheduleTypeFields(true)
                }
            }
        }

        customView.scheduleDialogTime.setFixedOnClickListener {
            checkNotNull(customTimeDatas)

            val list = customTimeDatas!!.values.filter { it.customTimeKey is CustomTimeKey.Private }

            val customTimeDatas = delegate.getCustomTimeDatas(list)

            TimeDialogFragment.newInstance(ArrayList(customTimeDatas)).let {
                it.timeDialogListener = timeDialogListener
                it.show(
                    childFragmentManager,
                    TIME_LIST_FRAGMENT_TAG
                )
            }
        }

        childFragmentManager.apply {
            (findFragmentByTag(TIME_LIST_FRAGMENT_TAG) as? TimeDialogFragment)?.timeDialogListener =
                timeDialogListener

            @Suppress("UNCHECKED_CAST")
            (findFragmentByTag(TIME_PICKER_TAG) as? TimePickerDialogFragment<SerializableUnit>)?.listener =
                timePickerDialogFragmentListener
        }

        customView.scheduleDialogDate.setFixedOnClickListener {
            check(delegate.hasDate)

            delegate.getDatePicker().let {
                it.listener = datePickerDialogFragmentListener
                it.show(
                    childFragmentManager,
                    DATE_FRAGMENT_TAG
                )
            }
        }

        (childFragmentManager.findFragmentByTag(DATE_FRAGMENT_TAG) as? DatePickerDialogFragment)?.run {
            check(delegate.hasDate)

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
        val textDisabledSpinner =
            ContextCompat.getColor(requireContext(), R.color.textDisabledSpinner)

        customView.scheduleDialogMonthDayRadio.run {
            setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked)
                    return@setOnCheckedChangeListener

                if (delegate.isMonthly) {
                    scheduleDialogData.scheduleType = ScheduleType.MONTHLY_DAY
                    updateDelegate()
                }

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
            setItems((1..28).map { Utils.ordinal(it) })

            setSelection(scheduleDialogData.monthDayNumber - 1)

            addListener {
                check(it in (0 until 28))

                scheduleDialogData.monthDayNumber = it + 1
            }
        }

        customView.scheduleDialogMonthWeekRadio.run {
            setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                if (!isChecked)
                    return@setOnCheckedChangeListener

                if (delegate.isMonthly) {
                    scheduleDialogData.scheduleType = ScheduleType.MONTHLY_WEEK
                    updateDelegate()
                }

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
            setItems(listOf(1, 2, 3, 4).map { Utils.ordinal(it) })

            setSelection(scheduleDialogData.monthWeekNumber - 1)

            addListener {
                check(it in (0..3))

                scheduleDialogData.monthWeekNumber = it + 1
            }
        }

        customView.scheduleDialogMonthWeekDay.run {
            setItems(DayOfWeek.values().toList())

            setSelection(scheduleDialogData.monthWeekDay.ordinal)

            addListener {
                val dayOfWeek = DayOfWeek.values()[it]

                scheduleDialogData.monthWeekDay = dayOfWeek

                updateFields()
            }
        }

        customView.scheduleDialogMonthEnd.run {
            setItems(resources.getStringArray(R.array.month).toList())

            setSelection(if (scheduleDialogData.beginningOfMonth) 0 else 1)

            addListener {
                check(it in (0..1))

                scheduleDialogData.beginningOfMonth = it == 0
            }
        }

        if (customTimeDatas != null)
            initialize()
    }

    private fun updateDelegate() {
        delegate = when (scheduleDialogData.scheduleType) {
            ScheduleType.SINGLE -> singleDelegate
            ScheduleType.WEEKLY -> weeklyDelegate
            ScheduleType.MONTHLY_DAY -> monthlyDayDelegate
            ScheduleType.MONTHLY_WEEK -> monthlyWeekDelegate
            ScheduleType.YEARLY -> yearlyDelegate
        }
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

        customView.run {
            delegate.visibilities.run {
                scheduleDialogDateLayout.isVisible = date
                scheduleDialogDayLayout.isVisible = day
                scheduleDialogMonthLayout.isVisible = month
                scheduleDialogFromLayout.isVisible = from
                scheduleDialogUntilLayout.isVisible = until
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
                data!!.getParcelableExtra<CustomTimeKey.Private>(ShowCustomTimeActivity.CUSTOM_TIME_KEY)!!
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

        delegate.updateFields(customTimeData, hourMinuteString)

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

    private abstract inner class Delegate {

        abstract val selection: Int

        open val hasDate = false

        open val isMonthly = false

        abstract val visibilities: Visibilities

        open fun fixDate(date: Date): Date = throw IllegalStateException()

        abstract fun isValid(): ErrorData

        abstract fun getCustomTimeDatas(list: List<EditViewModel.CustomTimeData>): List<TimeDialogFragment.CustomTimeData>

        open fun getDatePicker(): DatePickerDialogFragment = throw IllegalStateException()

        abstract fun updateFields(
            customTimeData: EditViewModel.CustomTimeData?,
            hourMinuteString: String
        )
    }

    private val singleDelegate = object : Delegate() {

        override val selection = 0

        override val hasDate = true

        override val visibilities = Visibilities(date = true)

        override fun fixDate(date: Date) = date

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

        override fun getDatePicker() = DatePickerDialogFragment.newInstance(scheduleDialogData.date)

        override fun updateFields(
            customTimeData: EditViewModel.CustomTimeData?,
            hourMinuteString: String
        ) {
            customView.scheduleDialogDate.setText(scheduleDialogData.date.getDisplayText())

            customView.scheduleDialogTime.setText(customTimeData?.let {
                it.name + " (" + customTimeData.hourMinutes.getValue(scheduleDialogData.date.dayOfWeek) + ")"
            } ?: hourMinuteString)
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

        override fun updateFields(
            customTimeData: EditViewModel.CustomTimeData?,
            hourMinuteString: String
        ) {
            customView.scheduleDialogDate.setText(scheduleDialogData.date.run {
                ScheduleText.Yearly.getDateText(
                    month,
                    day
                )
            })

            customView.scheduleDialogTime.setText(customTimeData?.name ?: hourMinuteString)

            dateFieldDatas.forEach { data ->
                val date = data.property.get()

                val dropdown = date == null

                data.field.setText(date?.getDisplayText())

                data.layout.endIconMode =
                    if (dropdown) TextInputLayout.END_ICON_DROPDOWN_MENU else TextInputLayout.END_ICON_CLEAR_TEXT

                data.field.apply {
                    setFixedOnClickListener {
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
    }

    private val weeklyDelegate = object : Repeating() {

        override val selection = 1

        override val visibilities = repeatingVisibilities.copy(day = true)
    }

    private abstract inner class Monthly : Repeating() {

        override val selection = 2

        override val isMonthly = true

        override val visibilities = repeatingVisibilities.copy(month = true)
    }

    private val monthlyDayDelegate = object : Monthly() {

    }

    private val monthlyWeekDelegate = object : Monthly() {

    }

    private val yearlyDelegate = object : Repeating() {

        override val selection = 3

        override val hasDate = true

        override val visibilities = repeatingVisibilities.copy(date = true)

        override fun fixDate(date: Date): Date {
            return if (date.month == 2 && date.day == 29)
                Date(date.year, 2, 28)
            else
                date
        }

        override fun getDatePicker() =
            DatePickerDialogFragment.newYearInstance(scheduleDialogData.date)
    }

    private class ErrorData(
        val date: String? = null,
        val time: String? = null,
        val from: String? = null,
        val until: String? = null
    )

    private data class Visibilities(
        val date: Boolean = false,
        val day: Boolean = false,
        val month: Boolean = false,
        val from: Boolean = false,
        val until: Boolean = false
    )
}
