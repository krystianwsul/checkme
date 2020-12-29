package com.krystianwsul.checkme.gui.instances

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentEditInstancesBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesDateTime
import com.krystianwsul.checkme.gui.base.NoCollapseBottomSheetDialogFragment
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity
import com.krystianwsul.checkme.gui.dialogs.*
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.gui.utils.setFixedOnClickListener
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.SerializableUnit
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.viewmodels.EditInstancesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.*
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.rxkotlin.addTo
import java.util.*

class EditInstancesFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        private const val INSTANCE_KEYS = "instanceKeys"

        private const val DATE_KEY = "date"
        private const val TIME_PAIR_PERSIST_KEY = "timePairPersist"
        private const val INITIAL_HOUR_MINUTE_KEY = "initialHourMinute"
        private const val INITIAL_DATE_KEY = "initialDate"

        private const val DATE_FRAGMENT_TAG = "dateFragment"
        private const val TIME_FRAGMENT_TAG = "timeFragment"
        private const val TIME_DIALOG_FRAGMENT_TAG = "timeDialogFragment"

        fun newInstance(instanceKeys: List<InstanceKey>) = EditInstancesFragment().apply {
            check(instanceKeys.isNotEmpty())

            arguments = Bundle().apply { putParcelableArrayList(INSTANCE_KEYS, ArrayList(instanceKeys)) }
        }
    }

    override val dialogStyle = R.style.BottomSheetDialogTheme_ActionMode

    override val backgroundView get() = binding.editInstancesRoot
    override val contentView get() = binding.editInstancesBackground

    private lateinit var date: Date
    private var data: EditInstancesViewModel.Data? = null

    private var savedInstanceState: Bundle? = null

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (data != null) updateError()
        }
    }

    private var timePairPersist: TimePairPersist? = null

    private var first = true

    private val timeDialogListener = object : TimeDialogFragment.TimeDialogListener {

        override fun onCustomTimeSelected(customTimeKey: CustomTimeKey<*>) {
            checkNotNull(data)

            timePairPersist!!.customTimeKey = customTimeKey

            updateTimeText()

            updateError()
        }

        override fun onOtherSelected() {
            checkNotNull(data)

            TimePickerDialogFragment.newInstance(timePairPersist!!.hourMinute, SerializableUnit).also {
                it.listener = timePickerDialogFragmentListener
                it.show(childFragmentManager, TIME_FRAGMENT_TAG)
            }
        }

        override fun onAddSelected() = startActivityForResult(
                ShowCustomTimeActivity.getCreateIntent(requireContext()),
                ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE
        )
    }

    private val timePickerDialogFragmentListener = { hourMinute: HourMinute, _: SerializableUnit ->
        checkNotNull(data)

        timePairPersist!!.setHourMinute(hourMinute)
        updateTimeText()
        updateError()
    }

    private val materialDatePickerListener = { date: Date ->
        this.date = date
        updateDateText()
    }

    private var initialTimePair: TimePair? = null
    private var initialDate: Date? = null

    private lateinit var editInstancesViewModel: EditInstancesViewModel

    var listener: ((DomainFactory.EditInstancesUndoData) -> Unit)? = null

    private val bindingProperty = ResettableProperty<FragmentEditInstancesBinding>()
    private var binding by bindingProperty

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.savedInstanceState = savedInstanceState

        childFragmentManager.getMaterialDatePicker(DATE_FRAGMENT_TAG)?.addListener(materialDatePickerListener)

        if (savedInstanceState?.containsKey(DATE_KEY) == true) {
            date = savedInstanceState.getParcelable(DATE_KEY)!!

            check(savedInstanceState.containsKey(TIME_PAIR_PERSIST_KEY))
            timePairPersist = savedInstanceState.getParcelable(TIME_PAIR_PERSIST_KEY)!!

            check(savedInstanceState.containsKey(INITIAL_HOUR_MINUTE_KEY))
            initialTimePair = savedInstanceState.getParcelable(INITIAL_HOUR_MINUTE_KEY)!!

            check(savedInstanceState.containsKey(INITIAL_DATE_KEY))
            initialDate = savedInstanceState.getParcelable(INITIAL_DATE_KEY)!!
        }

        val instanceKeys = requireArguments().getParcelableArrayList<InstanceKey>(INSTANCE_KEYS)!!
        check(instanceKeys.isNotEmpty())

        editInstancesViewModel = getViewModel<EditInstancesViewModel>().apply { start(instanceKeys) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            FragmentEditInstancesBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editInstanceDate.setFixedOnClickListener {
            newMaterialDatePicker(date).let {
                it.addListener(materialDatePickerListener)
                it.show(childFragmentManager, DATE_FRAGMENT_TAG)
            }
        }

        binding.editInstanceSave.setOnClickListener {
            checkNotNull(data)
            check(isValidDate)
            check(isValidDateTime)

            editInstancesViewModel.stop()

            val editInstancesUndoData = DomainFactory.instance.setInstancesDateTime(
                    data!!.dataId,
                    SaveService.Source.GUI,
                    data!!.instanceKeys,
                    date,
                    timePairPersist!!.timePair
            )

            dismiss()

            listener?.invoke(editInstancesUndoData)
        }

        binding.editInstanceCancel.setOnClickListener { requireDialog().cancel() }

        editInstancesViewModel.data
                .subscribe(this::onLoadFinished)
                .addTo(viewCreatedDisposable)
    }

    override fun onResume() {
        super.onResume()

        requireActivity().registerReceiver(broadcastReceiver, IntentFilter(Intent.ACTION_TIME_TICK))

        if (data != null) updateError()
    }

    override fun onPause() {
        requireActivity().unregisterReceiver(broadcastReceiver)

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (data != null) {
            outState.putParcelable(DATE_KEY, date)

            checkNotNull(timePairPersist)
            outState.putParcelable(TIME_PAIR_PERSIST_KEY, timePairPersist)

            checkNotNull(initialTimePair)
            outState.putParcelable(INITIAL_HOUR_MINUTE_KEY, initialTimePair)

            checkNotNull(initialDate)
            outState.putParcelable(INITIAL_DATE_KEY, initialDate)
        }
    }

    private fun onLoadFinished(data: EditInstancesViewModel.Data) {
        this.data = data

        binding.editInstanceLayout.visibility = View.VISIBLE

        if (first && (savedInstanceState == null || !savedInstanceState!!.containsKey(DATE_KEY))) {
            check(!this::date.isInitialized)
            check(timePairPersist == null)
            check(initialTimePair == null)
            check(initialDate == null)

            first = false

            date = data.dateTime.date
            timePairPersist = TimePairPersist(data.dateTime.time.timePair)

            initialTimePair = timePairPersist!!.timePair
            initialDate = date
        }

        updateDateText()

        tryGetFragment<TimePickerDialogFragment<SerializableUnit>>(TIME_FRAGMENT_TAG)?.listener =
                timePickerDialogFragmentListener

        binding.editInstanceTime.setFixedOnClickListener {
            val customTimeDatas = ArrayList(data.customTimeDatas
                    .values
                    .filter { it.customTimeKey is CustomTimeKey.Private }
                    .sortedBy { it.hourMinutes[date.dayOfWeek] }
                    .map {
                        TimeDialogFragment.CustomTimeData(
                                it.customTimeKey,
                                it.name + " (" + it.hourMinutes[date.dayOfWeek] + ")"
                        )
                    }
            )

            TimeDialogFragment.newInstance(customTimeDatas).also {
                it.timeDialogListener = timeDialogListener
                it.show(childFragmentManager, TIME_DIALOG_FRAGMENT_TAG)
            }
        }

        tryGetFragment<TimeDialogFragment>(TIME_DIALOG_FRAGMENT_TAG)?.timeDialogListener = timeDialogListener
    }

    private fun updateDateText() {
        binding.editInstanceDate.setText(date.getDisplayText())

        updateTimeText()
        updateError()
    }

    @SuppressLint("SetTextI18n")
    private fun updateTimeText() {
        checkNotNull(timePairPersist)
        checkNotNull(data)

        if (timePairPersist!!.customTimeKey != null) {
            binding.editInstanceTime.setText(
                    data!!.customTimeDatas
                            .getValue(timePairPersist!!.customTimeKey!!)
                            .run { name + " (" + hourMinutes.getValue(date.dayOfWeek) + ")" }
            )
        } else {
            binding.editInstanceTime.setText(timePairPersist!!.hourMinute.toString())
        }
    }

    private val isValidDate get() = data?.let { date >= Date.today() } == true

    //cached data doesn't contain new custom time
    private val isValidDateTime: Boolean
        get() {
            if (data != null) {
                val hourMinute = if (timePairPersist!!.customTimeKey != null) {
                    if (!data!!.customTimeDatas.containsKey(timePairPersist!!.customTimeKey)) return false

                    data!!.customTimeDatas
                            .getValue(timePairPersist!!.customTimeKey!!)
                            .hourMinutes
                            .getValue(date.dayOfWeek)
                } else {
                    timePairPersist!!.hourMinute
                }

                return TimeStamp(date, hourMinute) > TimeStamp.now
            } else {
                return false
            }
        }

    private fun updateError() {
        val (dateError, timeError) = if (isValidDate) {
            null to if (isValidDateTime) null else getString(R.string.error_time)
        } else {
            getString(R.string.error_date) to null
        }

        binding.editInstanceDateLayout.error = dateError
        binding.editInstanceTimeLayout.error = timeError

        binding.editInstanceSave.isEnabled = isValidDateTime
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        check(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)
        checkNotNull(timePairPersist)

        if (resultCode == Activity.RESULT_OK) {
            timePairPersist!!.customTimeKey =
                    data!!.getParcelableExtra<CustomTimeKey.Private>(ShowCustomTimeActivity.CUSTOM_TIME_KEY)!!
        }
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }
}
