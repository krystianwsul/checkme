package com.krystianwsul.checkme.gui.instances.edit

import android.animation.ValueAnimator
import android.annotation.SuppressLint
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
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.timepicker.MaterialTimePicker
import com.jakewharton.rxbinding4.view.clicks
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentEditInstancesBinding
import com.krystianwsul.checkme.domainmodel.DomainListenerManager
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesDateTime
import com.krystianwsul.checkme.domainmodel.extensions.setInstancesParent
import com.krystianwsul.checkme.domainmodel.undo.UndoData
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.gui.base.NoCollapseBottomSheetDialogFragment
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimeActivity
import com.krystianwsul.checkme.gui.dialogs.*
import com.krystianwsul.checkme.gui.edit.dialogs.parentpicker.ParentInstancePickerDelegate
import com.krystianwsul.checkme.gui.edit.dialogs.parentpicker.ParentInstanceViewModel
import com.krystianwsul.checkme.gui.edit.dialogs.parentpicker.ParentPickerFragment
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.gui.utils.measureVisibleHeight
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.animateVisibility
import com.krystianwsul.checkme.utils.cancelAnimations
import com.krystianwsul.checkme.utils.time.getDisplayText
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.checkme.viewmodels.DataId
import com.krystianwsul.checkme.viewmodels.EditInstancesViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePairPersist
import com.krystianwsul.common.time.TimeStamp
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.InstanceKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.parcelize.Parcelize

class EditInstancesFragment : NoCollapseBottomSheetDialogFragment() {

    companion object {

        private const val INSTANCE_KEYS = "instanceKeys"

        private const val KEY_STATE = "state"

        private const val DATE_FRAGMENT_TAG = "dateFragment"
        private const val TAG_TIME_FRAGMENT = "timeFragment"
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

        override fun onCustomTimeSelected(customTimeKey: CustomTimeKey) {
            state.timePairPersist.customTimeKey = customTimeKey
            updateFields()
        }

        override fun onOtherSelected() = newMaterialTimePicker(
            requireContext(),
            childFragmentManager,
            TAG_TIME_FRAGMENT,
            state.timePairPersist.hourMinute,
        ).setListener(timePickerDialogFragmentListener)

        @Suppress("DEPRECATION")
        override fun onAddSelected() = startActivityForResult(
            ShowCustomTimeActivity.getCreateIntent(requireContext()),
            ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE,
        )
    }

    private val timePickerDialogFragmentListener = { hourMinute: HourMinute ->
        state.timePairPersist.setHourMinute(hourMinute)
        updateFields()
    }

    private val materialDatePickerListener = { date: Date ->
        state.date = date
        updateFields()
    }

    private val editInstancesViewModel by lazy { getViewModel<EditInstancesViewModel>() }
    private val editInstancesSearchViewModel by lazy { getViewModel<ParentInstanceViewModel>() }

    private lateinit var listener: Listener

    private val bindingProperty = ResettableProperty<FragmentEditInstancesBinding>()
    private var binding by bindingProperty

    private val instanceKeys by lazy { requireArguments().getParcelableArrayList<InstanceKey>(INSTANCE_KEYS)!!.toSet() }

    private val parentPickerDelegate by lazy {
        object : ParentInstancePickerDelegate(
            viewCreatedDisposable,
            editInstancesSearchViewModel,
            instanceKeys.map { it.taskKey }.toSet(),
        ) {

            override fun onNewEntry(nameHint: String?) = throw UnsupportedOperationException()

            override fun onEntryDeleted() {
                state.parentInstanceData = null
                updateFields()
            }

            override fun onEntrySelected(entryData: ParentPickerFragment.EntryData) {
                state.parentInstanceData = (entryData as ParentInstanceViewModel.InstanceEntryData).run {
                    EditInstancesViewModel.ParentInstanceData(instanceKey, name)
                }

                updateFields()
            }

            override fun getPage() = state.page

            override fun setPage(page: Int) {
                state.page = page
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.getMaterialDatePicker(DATE_FRAGMENT_TAG)?.addListener(materialDatePickerListener)

        if (savedInstanceState?.containsKey(KEY_STATE) == true)
            state = savedInstanceState.getParcelable(KEY_STATE)!!

        check(instanceKeys.isNotEmpty())

        editInstancesViewModel.start(instanceKeys)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentEditInstancesBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editInstanceDateLayout.setDropdown {
            newMaterialDatePicker(state.date).let {
                it.addListener(materialDatePickerListener)
                it.show(childFragmentManager, DATE_FRAGMENT_TAG)
            }
        }

        binding.editInstanceSave.setOnClickListener {
            editInstancesViewModel.stop()

            listener.beforeEditInstances(instanceKeys)

            AndroidDomainUpdater.run {
                if (state.parentInstanceData != null) {
                    setInstancesParent(listener.notificationType, instanceKeys, state.parentInstanceData!!.instanceKey)
                } else {
                    setInstancesDateTime(
                        listener.notificationType,
                        instanceKeys,
                        state.date,
                        state.timePairPersist.timePair,
                    )
                }
            }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy {
                    dismiss()

                    listener.afterEditInstances(it.undoData, instanceKeys.size, it.newTimeStamp)
                }
                .addTo(viewCreatedDisposable)
        }

        binding.editInstanceCancel.setOnClickListener { requireDialog().cancel() }

        editInstancesViewModel.data
            .subscribe(this::onLoadFinished)
            .addTo(viewCreatedDisposable)

        binding.editInstanceSetScheduleText
            .clicks()
            .subscribe {
                state.parentInstanceData = null
                updateFields()
            }
            .addTo(viewCreatedDisposable)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressLint("MissingSuperCall")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        super.onActivityCreated(savedInstanceState)

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

        tryGetFragment<MaterialTimePicker>(TAG_TIME_FRAGMENT)?.setListener(timePickerDialogFragmentListener)

        binding.editInstanceTimeLayout.setDropdown {
            val customTimeDatas = ArrayList(
                data.customTimeDatas
                    .values
                    .filter { it.isMine }
                    .sortedBy { it.hourMinutes[state.date.dayOfWeek] }
                    .map {
                        TimeDialogFragment.CustomTimeData(
                            it.customTimeKey,
                            it.name + " (" + it.hourMinutes[state.date.dayOfWeek] + ")",
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

    private var first = true

    private fun updateFields() {
        binding.editInstanceParentText.setText(state.parentInstanceData?.name)

        binding.editInstanceParentLayout.isEndIconVisible = true

        fun listener() = ParentPickerFragment.newInstance(data.parentInstanceData != null, false).let {
            it.show(childFragmentManager, TAG_PARENT_PICKER)
            it.initialize(parentPickerDelegate)
        }

        binding.editInstanceParentLayout.apply {
            if (state.parentInstanceData != null) {
                setClose(::listener) {
                    state.parentInstanceData = null
                    updateFields()
                }
            } else {
                setDropdown(::listener)
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

        val show: View
        val hide: View
        if (state.parentInstanceData != null) {
            show = binding.editInstanceSetScheduleLayout
            hide = binding.editInstanceScheduleContainer
        } else {
            hide = binding.editInstanceSetScheduleLayout
            show = binding.editInstanceScheduleContainer
        }
        animateVisibility(show, hide, immediate = first)

        if (!first) {
            binding.editInstanceScheduleFrame.apply {
                cancelAnimations()

                ValueAnimator.ofInt(height, show.measureVisibleHeight(binding.editInstanceScheduleFrame.width)).apply {
                    duration = context.resources.getInteger(android.R.integer.config_longAnimTime).toLong()

                    addUpdateListener {
                        updateLayoutParams<LinearLayout.LayoutParams> { height = it.animatedValue as Int }
                    }
                }.start()
            }
        }

        first = false
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        check(requestCode == ShowCustomTimeActivity.CREATE_CUSTOM_TIME_REQUEST_CODE)

        if (resultCode == Activity.RESULT_OK) {
            state.timePairPersist.customTimeKey = data!!.getParcelableExtra(ShowCustomTimeActivity.CUSTOM_TIME_KEY)!!
        }
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    fun setListener(listener: Listener) {
        check(!this::listener.isInitialized)

        this.listener = listener
    }

    @Parcelize
    private data class State(
        var parentInstanceData: EditInstancesViewModel.ParentInstanceData?,
        var date: Date,
        var timePairPersist: TimePairPersist,
        var page: Int = 0,
    ) : Parcelable

    interface Listener {

        val dataId: DataId?

        val notificationType get() = dataId?.toFirst() ?: DomainListenerManager.NotificationType.All

        fun beforeEditInstances(instanceKeys: Set<InstanceKey>) {}

        fun afterEditInstances(undoData: UndoData, count: Int, newTimeStamp: TimeStamp?)
    }

}
