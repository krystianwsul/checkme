package com.krystianwsul.checkme.gui.instances

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentEditInstancesBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesDateTime
import com.krystianwsul.checkme.gui.base.NoCollapseBottomSheetDialogFragment
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity
import com.krystianwsul.checkme.gui.dialogs.*
import com.krystianwsul.checkme.gui.edit.dialogs.ParentPickerFragment
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.gui.utils.connectInstanceSearch
import com.krystianwsul.checkme.gui.utils.setFixedOnClickListener
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.SerializableUnit
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.viewmodels.EditInstancesSearchViewModel
import com.krystianwsul.checkme.viewmodels.EditInstancesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePairPersist
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.treeadapter.FilterCriteria
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import kotlinx.parcelize.Parcelize
import java.util.*

class EditInstancesFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        private const val INSTANCE_KEYS = "instanceKeys"

        private const val KEY_STATE = "state"

        private const val DATE_FRAGMENT_TAG = "dateFragment"
        private const val TIME_FRAGMENT_TAG = "timeFragment"
        private const val TIME_DIALOG_FRAGMENT_TAG = "timeDialogFragment"
        private const val TAG_PARENT_PICKER = "parentPicker"

        fun newInstance(instanceKeys: List<InstanceKey>) = EditInstancesFragment().apply {
            check(instanceKeys.isNotEmpty())

            arguments = Bundle().apply { putParcelableArrayList(INSTANCE_KEYS, ArrayList(instanceKeys)) }
        }
    }

    override val dialogStyle = R.style.BottomSheetDialogTheme_ActionMode

    override val backgroundView get() = binding.editInstancesRoot
    override val contentView get() = binding.editInstancesBackground

    private lateinit var data: EditInstancesViewModel.Data

    private lateinit var state: State

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (this@EditInstancesFragment::data.isInitialized) updateFields()
        }
    }

    private val timeDialogListener = object : TimeDialogFragment.TimeDialogListener {

        override fun onCustomTimeSelected(customTimeKey: CustomTimeKey<*>) {
            state.timePairPersist.customTimeKey = customTimeKey
            updateFields()
        }

        override fun onOtherSelected() {
            TimePickerDialogFragment.newInstance(state.timePairPersist.hourMinute, SerializableUnit).also {
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
        state.timePairPersist.setHourMinute(hourMinute)
        updateFields()
    }

    private val materialDatePickerListener = { date: Date ->
        state.date = date
        updateFields()
    }

    private val editInstancesViewModel by lazy { getViewModel<EditInstancesViewModel>() }
    private val editInstancesSearchViewModel by lazy { getViewModel<EditInstancesSearchViewModel>() }

    var listener: ((DomainFactory.EditInstancesUndoData) -> Unit)? = null

    private val bindingProperty = ResettableProperty<FragmentEditInstancesBinding>()
    private var binding by bindingProperty

    private val parentPickerDelegate by lazy {
        object : ParentPickerFragment.Delegate {

            private val queryRelay = BehaviorRelay.createDefault("")

            override val adapterDataObservable = BehaviorRelay.create<ParentPickerFragment.AdapterData>()

            override val filterCriteriaObservable = Observable.never<FilterCriteria.Full>()

            init {
                val onProgressShown = Observable.never<Unit>() // todo search

                connectInstanceSearch(
                        queryRelay.map { FilterCriteria.Full(it, showAssignedToOthers = Preferences.showAssigned) },
                        false,
                        { state.page },
                        { state.page = it },
                        onProgressShown,
                        viewCreatedDisposable,
                        editInstancesSearchViewModel,
                        {
                            adapterDataObservable.accept(ParentPickerFragment.AdapterData(
                                    it.instanceEntryDatas,
                                    FilterCriteria.ExpandOnly(it.searchCriteria.query),
                                    it.showLoader
                            ))
                        },
                        { searchCriteria, page -> editInstancesSearchViewModel.start(searchCriteria, page) }
                )

            }

            override fun onNewEntry(nameHint: String?) {
                TODO("Not yet implemented")
            }

            override fun onEntryDeleted() {
                TODO("Not yet implemented")
            }

            override fun onEntrySelected(entryData: ParentPickerFragment.EntryData) {
                TODO("Not yet implemented")
            }

            override fun onSearch(query: String) = queryRelay.accept(query)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.getMaterialDatePicker(DATE_FRAGMENT_TAG)?.addListener(materialDatePickerListener)

        if (savedInstanceState?.containsKey(KEY_STATE) == true) {
            state = savedInstanceState.getParcelable(KEY_STATE)!!
        }

        val instanceKeys = requireArguments().getParcelableArrayList<InstanceKey>(INSTANCE_KEYS)!!
        check(instanceKeys.isNotEmpty())

        editInstancesViewModel.start(instanceKeys)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            FragmentEditInstancesBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editInstanceDate.setFixedOnClickListener {
            newMaterialDatePicker(state.date).let {
                it.addListener(materialDatePickerListener)
                it.show(childFragmentManager, DATE_FRAGMENT_TAG)
            }
        }

        binding.editInstanceSave.setOnClickListener {
            check(isValidDate)
            check(isValidDateTime)

            editInstancesViewModel.stop()

            val editInstancesUndoData = DomainFactory.instance.setInstancesDateTime(
                    data.dataId,
                    SaveService.Source.GUI,
                    data.instanceKeys,
                    state.date,
                    state.timePairPersist.timePair
            )

            dismiss()

            listener?.invoke(editInstancesUndoData)
        }

        binding.editInstanceCancel.setOnClickListener { requireDialog().cancel() }

        editInstancesViewModel.data
                .subscribe(this::onLoadFinished)
                .addTo(viewCreatedDisposable)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        childFragmentManager.fragments

        tryGetFragment<ParentPickerFragment>(TAG_PARENT_PICKER)?.initialize(parentPickerDelegate)
    }

    override fun onResume() {
        super.onResume()

        requireActivity().registerReceiver(broadcastReceiver, IntentFilter(Intent.ACTION_TIME_TICK))

        if (this::data.isInitialized) updateFields()
    }

    override fun onPause() {
        requireActivity().unregisterReceiver(broadcastReceiver)

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (this::state.isInitialized) outState.putParcelable(KEY_STATE, state)
    }

    private fun onLoadFinished(data: EditInstancesViewModel.Data) {
        this.data = data

        binding.editInstanceLayout.visibility = View.VISIBLE

        if (!this::state.isInitialized) {
            state = data.run {
                State(
                        parentInstanceData,
                        dateTime.date,
                        TimePairPersist(dateTime.time.timePair),
                )
            }
        }

        updateFields()

        binding.editInstanceParentLayout.apply {
            addOneShotGlobalLayoutListener { isHintAnimationEnabled = true }
        }

        tryGetFragment<TimePickerDialogFragment<SerializableUnit>>(TIME_FRAGMENT_TAG)?.listener =
                timePickerDialogFragmentListener

        binding.editInstanceTime.setFixedOnClickListener {
            val customTimeDatas = ArrayList(data.customTimeDatas
                    .values
                    .filter { it.customTimeKey is CustomTimeKey.Private }
                    .sortedBy { it.hourMinutes[state.date.dayOfWeek] }
                    .map {
                        TimeDialogFragment.CustomTimeData(
                                it.customTimeKey,
                                it.name + " (" + it.hourMinutes[state.date.dayOfWeek] + ")"
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

    private val isValidDate get() = if (this::data.isInitialized) state.date >= Date.today() else false

    //cached data doesn't contain new custom time
    private val isValidDateTime: Boolean
        get() {
            if (this::data.isInitialized) {
                val hourMinute = if (state.timePairPersist.customTimeKey != null) {
                    if (!data.customTimeDatas.containsKey(state.timePairPersist.customTimeKey)) return false

                    data.customTimeDatas
                            .getValue(state.timePairPersist.customTimeKey!!)
                            .hourMinutes
                            .getValue(state.date.dayOfWeek)
                } else {
                    state.timePairPersist.hourMinute
                }

                return TimeStamp(state.date, hourMinute) > TimeStamp.now
            } else {
                return false
            }
        }

    private fun updateFields() {
        binding.editInstanceParentLayout.endIconMode = if (state.parentInstanceData != null)
            TextInputLayout.END_ICON_CLEAR_TEXT
        else
            TextInputLayout.END_ICON_DROPDOWN_MENU

        binding.editInstanceParentText.setText(state.parentInstanceData?.name)

        binding.editInstanceParentLayout.isEndIconVisible = true

        binding.editInstanceParentText.setFixedOnClickListener {
            ParentPickerFragment.newInstance(
                    data.parentInstanceData != null,
                    false,
                    R.string.parent_dialog_title_instance
            ).let {
                it.show(childFragmentManager, TAG_PARENT_PICKER)
                it.initialize(parentPickerDelegate)
            }
        }

        if (state.parentInstanceData != null) {
            binding.editInstanceParentLayout.setEndIconOnClickListener {
                state.parentInstanceData = null
                updateFields()
            }
        }

        binding.editInstanceDate.setText(state.date.getDisplayText())

        if (state.timePairPersist.customTimeKey != null) {
            binding.editInstanceTime.setText(
                    data.customTimeDatas
                            .getValue(state.timePairPersist.customTimeKey!!)
                            .run { name + " (" + hourMinutes.getValue(state.date.dayOfWeek) + ")" }
            )
        } else {
            binding.editInstanceTime.setText(state.timePairPersist.hourMinute.toString())
        }

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

        if (resultCode == Activity.RESULT_OK) {
            state.timePairPersist.customTimeKey =
                    data!!.getParcelableExtra<CustomTimeKey.Private>(ShowCustomTimeActivity.CUSTOM_TIME_KEY)!!
        }
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    @Parcelize
    private data class State(
            var parentInstanceData: EditInstancesViewModel.ParentInstanceData?,
            var date: Date,
            var timePairPersist: TimePairPersist,
            var page: Int = 0,
    ) : Parcelable
}
